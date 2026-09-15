'use strict'

/*
  Detective Helper · Electron 外壳

  为什么从 JavaFX WebView 换到这里：
  JavaFX 内置的是旧版 WebKit，在 150% 缩放的 Windows 上既有 JDK-8252649 的模糊缺陷，
  又有交互卡顿，并且不支持 datetime-local 之类的原生控件。换成 Chromium 之后，
  「画面质量和浏览器一致」是由构造保证的 —— 因为参照物本身就是 Blink 渲染的。

  外壳的职责边界（刻意保持很窄）：
    1. 挑一个空闲端口；
    2. 拉起后端 JVM 子进程；
    3. 等 /api/health 真的应答；
    4. 把窗口指过去。
  页面内的导航一律交给前端自己的路由，外壳不重复实现。

  数据目录契约与后端 DataDirEnvironmentPostProcessor 对齐：
    {应用目录}/data  →  不可写时降级 %LOCALAPPDATA%/Detective Helper/data
*/

const { app, BrowserWindow, Menu, dialog, ipcMain, shell } = require('electron')
const { spawn } = require('node:child_process')
const fs = require('node:fs')
const net = require('node:net')
const http = require('node:http')
const path = require('node:path')

const APP_NAME = 'Detective Helper'
const SHELF_ROUTE = '/bookshelf'
const BACKEND_MAIN_CLASS = 'com.theos.detectivehelper.DetectiveHelperApplication'
/** CDS 归档文件名：构建期由 scripts/make-cds-archive.ps1 生成，运行期只读 */
const CDS_ARCHIVE_NAME = 'app.jsa'

/** 冷启动要扫 classpath、建库，给足余量；用户看到的是 loading 页而不是白屏 */
const STARTUP_TIMEOUT_MS = 120000
/**
 * 健康探测间隔。后端就绪后最多再等这么久就把窗口指过去，
 * 所以这个值直接变成启动耗时的一部分 —— 100ms 是"够快"和"别把 CPU 烧在 reject 上"的折中。
 */
const HEALTH_PROBE_INTERVAL_MS = 100
const HEALTH_PROBE_TIMEOUT_MS = 4000

/** 与 DataDirEnvironmentPostProcessor.SUB_DIRECTORIES / AppPaths.SUB_DIRECTORIES 保持一致 */
const DATA_SUBDIRS = ['covers', 'uploads', 'files', 'logs', 'backups']

const ZOOM_MIN = 0.6
const ZOOM_MAX = 2.0
const ZOOM_LEVELS = [0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.25, 1.5, 1.75, 2.0]

/** 只允许开一个实例：两个进程抢同一个 SQLite 库会制造 SQLITE_BUSY */
const gotSingleInstanceLock = app.requestSingleInstanceLock()
if (!gotSingleInstanceLock) {
  app.quit()
}

let mainWindow = null
let backend = null // { child, port, baseUrl }
let appDir = null
let dataDir = null
let shuttingDown = false
let booting = false
/**
 * 后端比窗口先就绪时（提前 spawn 之后这是常态）把目标 URL 寄存在这里，
 * 等 createWindow 建成后立刻过去。否则页面会永远停在加载页。
 */
let pendingAppUrl = null
/** 数据目录是否降级到了 LOCALAPPDATA —— 要如实告诉用户，不能藏着 */
let usedFallbackDataDir = false

/**
 * 最近一次状态。启动页可能在 boot() 推送完之后才加载完，
 * 那条消息就丢了 —— 启动页加载完主动拉一次这里兜住。
 */
let lastStatus = { text: '正在启动本地服务…', state: 'booting' }

/**
 * 基准模式：窗口不显示、页面渲染完成后打印总耗时并自动退出。
 *
 * 只影响「显示窗口」和「何时退出」，不改变任何启动路径本身，
 * 所以量出来的数字就是真实的端到端耗时，可以直接比较优化前后。
 *
 * 用法：$env:DHA_BENCH='1'; .\node_modules\electron\dist\electron.exe .
 */
const BENCH_MODE = process.env.DHA_BENCH === '1'

/** 自外壳进程启动到现在经过的毫秒数 —— 「双击 → 就绪」的度量尺 */
function sinceProcessStartMs () {
  return Math.round(process.uptime() * 1000)
}

/*
  ═══ 尽早拉起后端 ═══

  boot() 刻意在这里调用，而不是等 app.whenReady()。

  Electron/Chromium 自身要初始化几百毫秒，而这段与 JVM 启动毫无依赖关系；
  原来把 spawn 放在 whenReady 之后，等于把两段白串成 1+1。
  spawn 并不需要 app ready —— 它只用到 app.isPackaged / app.getPath
  和 node 的 net、child_process。

  两个必须守住的点：
    1. 抢不到单实例锁时【不能】spawn，否则两个进程会一起抢同一个 SQLite 库；
    2. 必须放在 `booting` 等 let 声明【之后】，否则会踩 TDZ。（boot 是函数声明，
       会提升，所以这里能直接调。）
*/
if (gotSingleInstanceLock && process.env.DHA_NO_EARLY_SPAWN !== '1') {
  void boot()
}

// ---------------------------------------------------------------- 路径解析

/**
 * 应用目录。
 *
 * - `dir` / NSIS 目标：exe 所在目录；
 * - `portable` 目标：那是个自解压包，进程跑在 %TEMP% 下，
 *   真正"用户放在哪"由 electron-builder 注入的 PORTABLE_EXECUTABLE_DIR 告诉你。
 *   不看这个变量的话，数据会落到临时目录里，重启一次就没了。
 * - 开发期（electron .）：回到仓库根目录，与 IDE 里跑后端时的 ./data 对齐。
 */
function resolveAppDir () {
  if (!app.isPackaged) {
    return path.resolve(__dirname, '..')
  }
  const portableDir = process.env.PORTABLE_EXECUTABLE_DIR
  if (portableDir && portableDir.trim()) {
    return path.resolve(portableDir)
  }
  return path.dirname(app.getPath('exe'))
}

/**
 * 真写一个探针文件再删掉。
 * 只判断权限位不够：装在 Program Files 下时目录看着可写，实际新建文件会被拦。
 */
function isWritable (dir) {
  try {
    fs.mkdirSync(dir, { recursive: true })
    const probe = path.join(dir, `.write-probe-${process.pid}.tmp`)
    fs.writeFileSync(probe, 'ok')
    fs.unlinkSync(probe)
    return true
  } catch {
    return false
  }
}

function fallbackDataDir () {
  const localAppData = process.env.LOCALAPPDATA
  if (localAppData && localAppData.trim()) {
    return path.join(localAppData, APP_NAME, 'data')
  }
  return path.join(app.getPath('home'), `.${APP_NAME}`, 'data')
}

/**
 * 数据目录（方案 A：数据跟着应用走 —— 整个目录拷到 U 盘就能带走全部案件）。
 * 顺带把子目录建好，后端的 logback 会在日志初始化时立刻写 logs/。
 */
function resolveDataDir (baseDir) {
  const preferred = path.join(baseDir, 'data')
  if (isWritable(preferred)) {
    return { dir: preferred, fallback: false }
  }
  const fallback = fallbackDataDir()
  if (isWritable(fallback)) {
    return { dir: fallback, fallback: true }
  }
  throw new Error(`数据目录不可用，两个候选位置都无法写入：\n${preferred}\n${fallback}`)
}

function ensureSubDirs (dir) {
  for (const sub of DATA_SUBDIRS) {
    fs.mkdirSync(path.join(dir, sub), { recursive: true })
  }
}

// ---------------------------------------------------------------- 后端进程

/** 让操作系统分配一个空闲的 loopback 端口，拿完立刻还回去 */
function pickFreePort () {
  return new Promise((resolve, reject) => {
    const server = net.createServer()
    server.unref()
    server.on('error', reject)
    server.listen(0, '127.0.0.1', () => {
      const { port } = server.address()
      server.close(() => resolve(port))
    })
  })
}

/**
 * Java 可执行文件。
 * 打包后固定用 resources/jre（自带、不依赖用户装没装 Java）；
 * 开发期按 DETECTIVE_HELPER_JAVA → JAVA_HOME → PATH 依次找。
 */
function resolveJavaExe () {
  const packaged = path.join(process.resourcesPath, 'jre', 'bin', 'java.exe')
  if (fs.existsSync(packaged)) {
    return packaged
  }

  const candidates = []
  if (process.env.DETECTIVE_HELPER_JAVA) {
    candidates.push(process.env.DETECTIVE_HELPER_JAVA)
  }
  if (process.env.JAVA_HOME) {
    candidates.push(path.join(process.env.JAVA_HOME, 'bin', 'java.exe'))
  }
  for (const dir of String(process.env.PATH || '').split(path.delimiter)) {
    if (dir.trim()) {
      candidates.push(path.join(dir.trim(), 'java.exe'))
    }
  }

  for (const candidate of candidates) {
    try {
      if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
        return candidate
      }
    } catch {
      /* 忽略不可访问的候选路径 */
    }
  }

  throw new Error(
    '找不到 Java 运行时。\n' +
      `已尝试：${packaged}\n` +
      '开发期可设置环境变量 DETECTIVE_HELPER_JAVA 指向本机 java.exe。'
  )
}

/**
 * 后端 classpath 与可选的 CDS 归档。
 *
 * 运行形态：一个应用 jar + 平铺的依赖 jar。
 *   打包后：resources/app/{app.jar, lib/*.jar, app.jsa?}
 *   开发期：backend/target/{app.jar, lib/*.jar, cds/app.jsa?}
 *
 * ═══ 为什么是 jar，不是展开的 classes 目录 ═══
 *
 * 这是实测结论，不是口味问题。三种形态的真实快慢（同一台机器、轮转 4 轮取中位数，
 * 「JVM 启动 → 应用就绪」）：
 *
 *     展开 classes 目录，无 CDS  ：3.97 秒
 *     普通 jar，      无 CDS  ：3.36 秒   光是换成 jar 就省 0.6 秒
 *     普通 jar，      有 CDS  ：2.06 秒   CDS 再省 1.3 秒
 *
 * 「jar 比展开目录快」看起来反直觉，但两件事可以同时成立：
 *   - 最慢的是 Spring Boot 的【嵌套 fat jar】—— 每个类都要经 BootLoader 解压偏移量，
 *     所以当初从 fat jar 换成展开目录确实是对的；
 *   - 而普通 jar 内部有索引、可以内存映射，比让 JVM 去挨个 stat/open 上百个 class
 *     文件更快（在 Windows 上还要过一遍杀毒软件的文件扫描）。
 * 真实顺序是：嵌套 fat jar > 展开目录 > 普通 jar。
 *
 * 换成 jar 还有第二个、也是更重要的理由：**CDS 要求 classpath 上不能有非空目录**。
 * 展开目录这个形态会把 CDS 彻底堵死，见 spawnBackend 里的说明。
 */
function resolveClasspath () {
  const candidates = []

  if (app.isPackaged) {
    candidates.push({
      jar: path.join(process.resourcesPath, 'app', 'app.jar'),
      lib: path.join(process.resourcesPath, 'app', 'lib'),
      cds: path.join(process.resourcesPath, 'app', CDS_ARCHIVE_NAME)
    })
  }
  candidates.push({
    jar: path.join(__dirname, '..', 'backend', 'target', 'app.jar'),
    lib: path.join(__dirname, '..', 'backend', 'target', 'lib'),
    cds: path.join(__dirname, '..', 'backend', 'target', 'cds', CDS_ARCHIVE_NAME)
  })
  // 兜底：只有展开目录时也能起来。会同时丢掉 jar 的收益和 CDS，
  // 所以下面会明着警告一句，而不是静默退化。
  candidates.push({
    classes: path.join(__dirname, '..', 'backend', 'target', 'classes'),
    lib: path.join(__dirname, '..', 'backend', 'target', 'lib'),
    cds: null
  })

  const tried = []
  for (const c of candidates) {
    tried.push(c.jar || c.classes)
    if (c.jar && fs.existsSync(c.jar) && fs.existsSync(c.lib) && listJars(c.lib).length) {
      return {
        classpath: buildClasspath(c.jar, c.lib),
        cdsArchive: c.cds && fs.existsSync(c.cds) ? c.cds : null,
        shape: 'jar'
      }
    }
    if (c.classes && fs.existsSync(c.classes) && fs.existsSync(c.lib) && listJars(c.lib).length) {
      log(`警告：没找到 ${c.jar || 'app.jar'}，退回展开 classes 目录启动 —— CDS 无法启用，启动会明显更慢。先跑 scripts\\run-electron-dev.ps1 构建。`)
      return {
        classpath: buildClasspath(c.classes, c.lib),
        cdsArchive: null,
        shape: 'classes'
      }
    }
  }

  throw new Error(
    '找不到后端 classpath。需要 app.jar（或 classes 目录）与非空 lib 目录，已尝试：\n' +
      tried.join('\n') +
      '\n（开发期先执行 `mvn -pl frontend,backend package`，或直接跑 scripts\\run-electron-dev.ps1）'
  )
}

/**
 * 用通配符 `lib\*` 而不是把每个 jar 逐个列进 -cp。
 *
 * 理由不是"少写几行"，而是 CDS：归档里存着 classpath 的条目列表，运行期加载归档时会
 * 逐项比对（不匹配就报 "shared class paths mismatch" 然后拒绝使用）。
 * `lib\*` 由 JVM 启动器统一展开，训练期和运行期走同一条展开逻辑，
 * 天然不会因为"谁先列出来"而不一致；自己 readdir 拼列表反而要操心顺序。
 */
function buildClasspath (appEntry, libDir) {
  return [appEntry, path.join(libDir, '*')].join(path.delimiter)
}

function listJars (libDir) {
  return fs
    .readdirSync(libDir)
    .filter((f) => f.endsWith('.jar'))
    .map((f) => path.join(libDir, f))
}

/**
 * 启动后端子进程。
 *
 * 用 java.exe（不是 javaw.exe）+ windowsHide：既有 stdio 管道能抓日志，
 * 又不会弹出控制台黑窗。
 *
 * JVM 参数：-XX:TieredStopAtLevel=1 恒定；CDS 只在归档真的存在时才加。理由见下面。
 */
function spawnBackend (port) {
  const javaExe = resolveJavaExe()
  const resolved = resolveClasspath()

  /*
    ═══ -XX:TieredStopAtLevel=1 ═══

    实测（同一台机器、同一份 classpath、每个组合各跑 3 轮取中位数，「进程启动 → 应用就绪」）：
        无标志                        ：6.94 秒
        -XX:TieredStopAtLevel=1       ：4.00 秒   ← 省 2.9 秒
        + -XX:+UseSerialGC            ：4.03 秒   没有收益
        + -Xms96m -Xmx512m            ：6.70 秒   没有收益
        + -Dspring.jmx.enabled=false  ：3.96 秒   落在噪声里
        + --spring.main.lazy-initialization=true
                                      ：3.69 秒   省 0.3 秒，但只是把代价搬到首次请求，
                                                 且会推迟 @PostConstruct 里的建表/迁移，不值

    TieredStopAtLevel=1 的语义是「只用 C1，而且不做 profiling」。Spring 启动阶段要跑掉
    几千个方法、其中绝大多数一辈子只执行一两次，为它们收集 profile 数据纯属白花时间 ——
    这才是省下那 2.9 秒的原因，而不是"编译得快"。

    代价：热点方法不会被 C2 优化到极致。本应用的重活是 SQLite 读写和 JSON 序列化，
    都是 I/O 与内存分配主导，实际感觉不出来。如果将来真遇到 CPU 密集的卡顿
    （例如超大关系图跑最短路），先把这一行去掉重新量一次再决定。

    ═══ CDS：能用，但必须先让 classpath 只剩 jar ═══

    这里原先写着"这条路走不通"，那个结论【错了一半】—— 错在把两件独立的事混成一件：

      1) 生成归档必须让 JVM【正常退出】。
         Windows 上 child.kill() 等于 TerminateProcess，JVM 没机会落盘，于是每一次
         启动都白付一遍生成归档的钱 —— 实测启动从 7.5 秒掉到 48 秒。
         这条是真的。所以训练【必须】是一个自己 System.exit(0) 的一次性进程，
         见 scripts/make-cds-archive.ps1 与 CdsTrainingEntry。

      2) 动态归档不允许 classpath 上有非空目录。
         当时用的正是展开目录 target/classes，JVM 直接拒绝：
           Error: non-empty directory '<...>/backend/target/classes'
         这条也是真的，但它【有解】：把应用类打成普通 jar（注意不是 Spring Boot 的
         嵌套胖 jar），classpath 上就只剩 jar 了。

    两条都解决之后实测（轮转 4 轮取中位数，「JVM 启动 → 就绪」）：
        展开目录 + 无 CDS ：3.97 秒
        jar      + 无 CDS ：3.36 秒
        jar      + CDS    ：2.06 秒    ← 相对原来的 3.97 秒快了一半

    ⚠️ 归档是按 jar 的【时间戳】校验的，不是按内容。JVM 的原话是
         "xxx.jar timestamp has changed."  →  "Unable to use shared archive."
       所以 Maven 每重写一次 jar，归档就失效、必须重训 —— 这也意味着
       一个"内容没改但文件被重写"的 jar 同样会让归档作废。
       重训由 scripts/make-cds-archive.ps1 负责（指纹一致时 1 秒跳过）。

    归档构建期生成、随包发布，运行期只读。开发期归档过期（改了后端代码）时，
    -Xshare:auto 会静默回退到普通启动 —— 慢一点，但绝不会因此起不来。
    重建后 run-electron-dev.ps1 会顺手重新训练一次。
  */
  const args = [
    '-XX:TieredStopAtLevel=1',
    '-Dfile.encoding=UTF-8',
    `-Ddetectivehelper.data.dir=${dataDir}`
  ]

  if (resolved.cdsArchive) {
    args.push(`-XX:SharedArchiveFile=${resolved.cdsArchive}`)
    // auto：归档不匹配时静默回退，不会因为"归档过期"让应用起不来
    args.push('-Xshare:auto')
  }

  args.push(
    '-cp',
    resolved.classpath,
    BACKEND_MAIN_CLASS,
    `--server.port=${port}`,
    '--server.address=127.0.0.1'
  )

  /*
    把「用哪个 java」和「CDS 到底生效了没有」写进日志。
    这两件事都必须可见：训练归档用的 JDK 和运行期这个 java 不是同一个的话，
    归档会被拒、-Xshare:auto 静默回退，启动悄悄慢 1.3 秒而没有任何报错。
  */
  log(`后端 JVM: ${javaExe}`)
  log(
    resolved.cdsArchive
      ? `CDS: 启用（${path.basename(resolved.cdsArchive)}）`
      : 'CDS: 未启用 —— 走普通启动，比正常慢约 1.3 秒'
  )

  const logStream = fs.createWriteStream(path.join(dataDir, 'logs', 'backend-stdout.log'), {
    flags: 'a'
  })

  const child = spawn(javaExe, args, {
    windowsHide: true,
    stdio: ['ignore', 'pipe', 'pipe'],
    env: { ...process.env }
  })

  child.stdout.pipe(logStream)
  child.stderr.pipe(logStream)

  child.on('error', (err) => {
    logStream.write(`[shell] 无法启动 Java 进程: ${err.message}\n`)
  })

  child.on('exit', (code, signal) => {
    if (shuttingDown) {
      return
    }
    logStream.write(`[shell] 后端进程意外退出 code=${code} signal=${signal}\n`)
    reportFailure(
      `本地服务意外退出（退出码 ${code}）。\n\n` +
        `日志：${path.join(dataDir, 'logs', 'backend-stdout.log')}`
    )
  })

  return { child, port, baseUrl: `http://127.0.0.1:${port}` }
}

function probeHealth (port) {
  return new Promise((resolve) => {
    const req = http.get(
      { host: '127.0.0.1', port, path: '/api/health', timeout: HEALTH_PROBE_TIMEOUT_MS },
      (res) => {
        res.resume()
        resolve(res.statusCode === 200)
      }
    )
    req.on('timeout', () => {
      req.destroy()
      resolve(false)
    })
    req.on('error', () => resolve(false))
  })
}

async function waitForHealth (port, timeoutMs) {
  const deadline = Date.now() + timeoutMs
  let attempt = 0
  while (Date.now() < deadline) {
    attempt++
    if (await probeHealth(port)) {
      log(
        `后端服务已就绪: http://127.0.0.1:${port}` +
          ` （第 ${attempt} 次探测，自外壳进程启动 ${sinceProcessStartMs()} ms）`
      )
      return true
    }
    if (backend && backend.child.exitCode !== null) {
      return false
    }
    await new Promise((r) => setTimeout(r, HEALTH_PROBE_INTERVAL_MS))
  }
  return false
}

// ---------------------------------------------------------------- 日志

/**
 * 外壳自己的日志，与后端 app.log 分开放，方便区分「页面没起来」和「后端没起来」。
 * 保留与旧 JavaFX 版相同的措辞「前端页面已渲染」，冒烟测试可以直接复用这个信号。
 */
function log (message) {
  if (!dataDir) {
    console.log(`[shell] ${message}`)
    return
  }
  const line = `${new Date().toISOString()} [shell] ${message}\n`
  try {
    fs.appendFileSync(path.join(dataDir, 'logs', 'shell.log'), line)
  } catch {
    /* 日志失败不影响主流程 */
  }
  console.log(`[shell] ${message}`)
}

// ---------------------------------------------------------------- 窗口

function sendStatus (text, state = lastStatus.state) {
  lastStatus = { text, state }
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('shell:status', lastStatus)
  }
}

function createWindow () {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 1024,
    minHeight: 700,
    // 基准模式不显示窗口：脚本量耗时不该在屏幕上闪一个窗口
    show: !BENCH_MODE,
    backgroundColor: '#F6F4EF',
    title: `${APP_NAME} · 推演录`,
    icon: path.join(__dirname, 'assets', 'icon.ico'),
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
      // 单机应用，不需要联网下载拼写词典
      spellcheck: false,
      // 允许隐藏滚动条之外的默认行为，保持与浏览器一致
      backgroundThrottling: false
    }
  })

  // 彻底去掉「文件 / 编辑 / 视图 / 帮助」这一整条菜单栏。
  // Windows 上 Blink 自己处理 Ctrl+C/V/A，不依赖菜单角色，所以编辑快捷键不会丢。
  Menu.setApplicationMenu(null)
  mainWindow.setMenuBarVisibility(false)

  installShortcuts(mainWindow)

  mainWindow.on('closed', () => {
    mainWindow = null
  })

  // 外部链接一律交给系统浏览器，不要在应用窗口里打开
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/i.test(url)) {
      shell.openExternal(url)
    }
    return { action: 'deny' }
  })

  mainWindow.webContents.on('will-navigate', (event, url) => {
    const allowed = backend && url.startsWith(backend.baseUrl)
    if (!allowed) {
      event.preventDefault()
      if (/^https?:/i.test(url)) {
        shell.openExternal(url)
      }
    }
  })

  // 「前端页面已渲染」是冒烟测试阶段二的关键信号：
  // 只看「窗口存在 + 标题非空」是盖不住白屏的。
  mainWindow.webContents.on('did-finish-load', () => {
    const url = mainWindow.webContents.getURL()
    if (url.startsWith('http://127.0.0.1:')) {
      log(`前端页面已渲染: ${url} （自外壳进程启动 ${sinceProcessStartMs()} ms）`)
      if (BENCH_MODE) {
        // 这行是给脚本正则抓的，格式别随便改
        log(`BENCH_MODE totalMs=${sinceProcessStartMs()}`)
        // 留一点时间让日志落盘，然后干净退出
        setTimeout(() => app.exit(0), 250)
      }
    }
  })

  mainWindow.webContents.on('did-fail-load', (_e, code, desc, url, isMainFrame) => {
    if (!isMainFrame) return
    /*
      -3 是 ERR_ABORTED。后端先就绪时我们会把还在加载的 loading.html 换成应用页，
      这次「失败」是我们自己叫停的，不是故障 —— 不排除掉会误报成启动失败。
    */
    if (code === -3) return
    log(`页面加载失败 code=${code} desc=${desc} url=${url}`)
    reportFailure(`页面加载失败：${desc}（${code}）\n${url}`)
  })

  mainWindow.webContents.on('render-process-gone', (_e, details) => {
    log(`渲染进程退出: ${JSON.stringify(details)}`)
    reportFailure(`界面渲染进程异常退出：${details.reason}`)
  })

  // 后端已经先就绪（提前 spawn 之后这是常态）就直接进应用页，
  // 省掉「加载页 → 应用页」这一次多余的导航
  if (pendingAppUrl) {
    const url = pendingAppUrl
    pendingAppUrl = null
    log(`后端已先就绪，直接加载前端页面: ${url}`)
    void mainWindow.loadURL(url)
    return
  }

  mainWindow.loadFile(path.join(__dirname, 'loading.html'))
}

function reportFailure (message) {
  log(`失败: ${message}`)

  /*
    基准模式没有窗口也没有人在看，必须用退出码表达失败 ——
    否则脚本会把「启动失败但进程正常退出」当成一次成功的测量。
  */
  if (BENCH_MODE) {
    log('BENCH_MODE failed=1')
    setTimeout(() => app.exit(1), 250)
    return
  }

  // 失败时把窗口切回加载页，否则用户只会看到一个空白页面无从下手
  if (mainWindow && !mainWindow.isDestroyed()) {
    const current = mainWindow.webContents.getURL()
    if (!current.startsWith('file:')) {
      mainWindow.loadFile(path.join(__dirname, 'loading.html'))
    }
  }
  sendStatus(message, 'failed')
}

/** 没有菜单栏之后，快捷键在渲染进程之前拦下来 */
function installShortcuts (win) {
  win.webContents.on('before-input-event', (event, input) => {
    if (input.type !== 'keyDown') return
    const ctrl = input.control || input.meta
    const key = (input.key || '').toLowerCase()

    if (input.key === 'F11') {
      event.preventDefault()
      win.setFullScreen(!win.isFullScreen())
      return
    }
    if (ctrl && key === 'r') {
      event.preventDefault()
      win.webContents.reload()
      return
    }
    if (ctrl && (key === '=' || key === '+')) {
      event.preventDefault()
      stepZoom(1)
      return
    }
    if (ctrl && key === '-') {
      event.preventDefault()
      stepZoom(-1)
      return
    }
    if (ctrl && key === '0') {
      event.preventDefault()
      win.webContents.setZoomFactor(1.0)
      return
    }
    if (ctrl && key === 'd') {
      event.preventDefault()
      void shell.openPath(dataDir)
      return
    }
    if (input.key === 'F1') {
      event.preventDefault()
      void showAbout()
      return
    }
  })
}

function stepZoom (direction) {
  if (!mainWindow) return
  const current = mainWindow.webContents.getZoomFactor()
  let idx = ZOOM_LEVELS.findIndex((z) => Math.abs(z - current) < 0.001)
  if (idx < 0) {
    idx = ZOOM_LEVELS.findIndex((z) => z >= current)
    if (idx < 0) idx = ZOOM_LEVELS.length - 1
  } else {
    idx += direction
  }
  idx = Math.max(0, Math.min(ZOOM_LEVELS.length - 1, idx))
  mainWindow.webContents.setZoomFactor(ZOOM_LEVELS[idx])
}

async function showAbout () {
  const info = buildInfo()
  await dialog.showMessageBox(mainWindow, {
    type: 'info',
    title: `关于 ${APP_NAME}`,
    message: `${APP_NAME} · 推演录`,
    detail:
      `版本：v${info.version}\n` +
      `数据目录：${info.dataDir}\n` +
      `运行环境：${info.os} / Electron ${info.electron} / Chromium ${info.chrome}\n\n` +
      '一个帮助推理游戏玩家整理案件信息的单机笔记工具。',
    buttons: ['好'],
    noLink: true
  })
}

function buildInfo () {
  return {
    name: APP_NAME,
    version: app.getVersion(),
    appDir,
    dataDir,
    fallbackDataDir: usedFallbackDataDir,
    backendRunning: !!(backend && backend.child.exitCode === null),
    backendUrl: backend ? backend.baseUrl : null,
    electron: process.versions.electron,
    chrome: process.versions.chrome,
    node: process.versions.node,
    os: `${process.platform} ${process.getSystemVersion ? process.getSystemVersion() : ''}`.trim()
  }
}

// ---------------------------------------------------------------- 启动流程

async function boot () {
  if (booting) {
    return
  }
  booting = true

  try {
    // 重试时先把上一轮的后端收掉，否则会攒下孤儿 JVM 一起抢 SQLite 的写锁
    stopBackend()

    appDir = resolveAppDir()
    const resolved = resolveDataDir(appDir)
    dataDir = resolved.dir
    usedFallbackDataDir = resolved.fallback
    ensureSubDirs(dataDir)

    if (resolved.fallback) {
      log(`应用目录不可写，数据目录降级为: ${dataDir}`)
    }
    log(`应用目录: ${appDir}`)
    log(`数据目录: ${dataDir}`)
    // Chromium 自身初始化的开销 —— 提前 spawn 之后这段与 JVM 启动并行，不再计入串行路径
    log(`外壳进程启动到此: ${sinceProcessStartMs()} ms`)

    const isFirstRun = !fs.existsSync(path.join(dataDir, 'detective-helper.db'))
    sendStatus(
      isFirstRun
        ? '首次启动需要建库建表，稍等几秒就好。'
        : '正在初始化运行环境，请稍候。',
      'booting'
    )

    const port = await pickFreePort()
    backend = spawnBackend(port)
    log(`启动后端服务，端口 ${port}`)

    const ready = await waitForHealth(port, STARTUP_TIMEOUT_MS)
    if (!ready) {
      const hint =
        `本地服务在 ${Math.round(STARTUP_TIMEOUT_MS / 1000)} 秒内没有应答 /api/health。\n\n` +
        `日志目录：${path.join(dataDir, 'logs')}`
      reportFailure(hint)
      return
    }

    if (mainWindow && !mainWindow.isDestroyed()) {
      navigateToApp(`${backend.baseUrl}${SHELF_ROUTE}`)
    } else {
      // 窗口还没建出来：寄存起来，createWindow 建完立刻过去。
      // 这条分支在「提前 spawn」之后会成为常态，不能省。
      pendingAppUrl = `${backend.baseUrl}${SHELF_ROUTE}`
      log(`后端先于窗口就绪，先寄存目标地址: ${pendingAppUrl}`)
    }
  } catch (err) {
    reportFailure(`启动失败：${err && err.message ? err.message : String(err)}`)
  } finally {
    booting = false
  }
}

/** 把窗口导航到应用页。窗口不存在时不报错，由 createWindow 收尾。 */
function navigateToApp (url) {
  if (!mainWindow || mainWindow.isDestroyed()) {
    pendingAppUrl = url
    log(`窗口尚未就绪，先寄存目标地址: ${url}`)
    return
  }
  log(`加载前端页面: ${url}`)
  void mainWindow.loadURL(url)
}

function stopBackend () {
  if (!backend || !backend.child) return
  const child = backend.child
  backend = null
  if (child.exitCode !== null) return
  try {
    // Windows 上 kill 只终止父进程，Spring Boot 没有派生进程，够用
    child.kill()
    log('已请求停止后端服务')
  } catch (err) {
    log(`停止后端服务失败: ${err.message}`)
  }
}

// ---------------------------------------------------------------- IPC

ipcMain.handle('shell:get-info', () => buildInfo())

/**
 * 启动页加载完之后主动拉一次状态。
 * 必须有这个：boot() 里的推送可能早于 loading.html 完成加载，那条消息就丢了，
 * 页面会一直停在默认文案上。
 */
ipcMain.handle('shell:get-status', () => lastStatus)

/** 启动失败后的「重试」——重新走完整 boot 流程，不是 reload 页面 */
ipcMain.handle('shell:retry-boot', async () => {
  sendStatus('正在重新启动本地服务…', 'booting')
  await boot()
  return buildInfo()
})

ipcMain.handle('shell:open-data-dir', async () => {
  await shell.openPath(dataDir)
  return true
})

ipcMain.handle('shell:open-logs', async () => {
  const dir = path.join(dataDir, 'logs')
  const target = fs.existsSync(path.join(dir, 'app.log')) ? dir : dataDir
  await shell.openPath(target)
  return true
})

ipcMain.handle('shell:about', async () => {
  await showAbout()
  return true
})

ipcMain.handle('shell:reload', () => {
  if (mainWindow) mainWindow.webContents.reload()
  return true
})

ipcMain.handle('shell:set-zoom', (_e, factor) => {
  if (!mainWindow) return 1
  const clamped = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, Number(factor) || 1))
  mainWindow.webContents.setZoomFactor(clamped)
  return clamped
})

ipcMain.handle('shell:get-zoom', () => (mainWindow ? mainWindow.webContents.getZoomFactor() : 1))

ipcMain.handle('shell:toggle-fullscreen', () => {
  if (!mainWindow) return false
  const next = !mainWindow.isFullScreen()
  mainWindow.setFullScreen(next)
  return next
})

// ---------------------------------------------------------------- 生命周期

app.on('second-instance', () => {
  if (mainWindow) {
    if (mainWindow.isMinimized()) mainWindow.restore()
    mainWindow.focus()
  }
})

app.whenReady().then(() => {
  app.setAppUserModelId('com.theos.detectivehelper')
  // 注意：boot() 已经在模块加载阶段就调过了（为了和 Chromium 初始化并行），
  // 这里只负责把窗口建出来。
  createWindow()
  if (process.env.DHA_NO_EARLY_SPAWN === '1') { void boot() }

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
      void boot()
    }
  })
})

app.on('window-all-closed', () => {
  app.quit()
})

app.on('before-quit', () => {
  shuttingDown = true
  stopBackend()
})

app.on('will-quit', () => {
  shuttingDown = true
  stopBackend()
})
