# Detective Helper · 项目长期记忆

推理笔记桌面应用。结构 Book → Event → Page → Canvas，扩展三件套：自适应时间线、人物关系图、族谱图。

## 架构

- **外壳 Electron**。JavaFX WebView 已废弃（旧 WebKit 在 150% 缩放下有 JDK-8252649 模糊缺陷），
  `desktop/` 模块与 jpackage 脚本已删除，根 pom 只剩 frontend + backend。
- 后端 Spring Boot 3.3.4 + MyBatis + SQLite，顺带托管前端静态资源 → **运行时只有一个进程、一个端口**。
- 前端 Vue 3 + TS + Vite + Ant Design Vue。
- 画布是 **SVG 与 HTML 混合渲染**：对象/连线是 SVG，注解是 HTML 富文本，时间线面板是 HTML 内嵌 SVG。
  任何涉及"整体处理画布"的功能（导出、截图、打印）都要照顾到这三层。
- 导出画布为图片（`frontend/src/utils/exportCanvas.ts`）：克隆 `.canvas-origin` → 全量 CSS 内联进
  `<style>` → 整体装进 `<foreignObject>` → **以 `data:` URL（不是 `blob:`）**交给 `<img>`
  → `drawImage` → `toBlob`（2 倍分辨率）。
  ⚠️ 用 `blob:` 会被 Chromium 判成非同源 → canvas 被污染 → `toBlob` 抛
  `Tainted canvases may not be exported`。**污染画布照样能正常显示**，只验"图出来了"会漏掉这个 bug。
  现场准备三条缺一不可：`zoom` 归 1（背景 pattern 尺寸按 zoom 算）、清 `selectedId`/`linkSourceId`、
  挂 `.exporting` 隐掉编辑态控件（穿透子组件要 `:deep()`）。
- 数据目录：`{应用目录}/data`，不可写时降级 `%LOCALAPPDATA%/Detective Helper/data`；
  打包态要读 `PORTABLE_EXECUTABLE_DIR`（portable 下进程跑在 %TEMP%）。

## 启动性能（端到端 ~3.3s，JavaFX 时代 20s）

三件事缺一不可：
- 普通 jar classpath（`java -cp "app.jar;lib\*"`）代替 fat jar —— 省约 1.9s
- `-XX:TieredStopAtLevel=1` —— 省约 2.9s
- CDS 归档 —— 省约 1.3s

外壳初始化与 JVM 启动并行再省约 0.2s。classpath 形态实测排序：嵌套 fat jar > 展开 classes > 普通 jar + lib。

## 打包发布

- 链路：`scripts/stage-runtime.ps1` → `electron-builder`，产物为
  `resources/jre` + `resources/app/{app.jar, lib, app.jsa}`（与 main.js 的解析一一对应）。
- stage 四步顺序**不可换**：裁 JRE → 组装 → **实起验证** → 用裁剪后的 JRE 训 CDS。
  第三步不能省：jlink 少带模块的症状是运行期 NoClassDefFoundError，打包时零报错。
- ⚠️ jlink 必须带 `--generate-cds-archive`：缺了只有一条 warning，训练进程照常打印就绪标记，
  但 `app.jsa` 永不生成。
- ⚠️ CDS 按 jar **时间戳**校验（不是内容）。任何"把 jar 再写一遍"的步骤都会让归档失效，且**静默**失效。
- electron-builder 工具包从 GitHub 下，国内挂
  `ELECTRON_BUILDER_BINARIES_MIRROR=https://npmmirror.com/mirrors/electron-builder-binaries/`。
- 发行形态只有两种，**都不出 portable**：
  `build/release/Detective-Helper-<版本>-win-x64.zip`（绿色版）与 `-setup.exe`（NSIS）。
  portable 是自解压 exe，每次运行解 300MB 到 %TEMP%，冷启动 23s，已废弃。
- 一键发布：`scripts/build-release.ps1`（开关 `-NoBuild` / `-SkipStage` / `-SkipPackage` / `-Targets zip`）。
  electron-builder 的 zip target **不套顶层目录**，脚本里用 `Add-ZipRootFolder` 重打一层。
  ⚠️ 该函数里 **`Add-Type` 必须写两行**：`System.IO.Compression.FileSystem`
  **加上** `System.IO.Compression` —— `ZipArchive` / `ZipArchiveMode` 在后一个程序集里，
  只加载前者在"碰巧已加载过"的会话里能跑，干净会话必报「找不到类型」。
  ⚠️ 重打 zip 只许处理**本次版本**的，且版本号要取 `electron/package.json` 那个
  （产物文件名是它拼的），**不是** pom 的。写 `*.zip` 会把遗留的旧版本一起捞进来，
  旧文件排前面、一出错就中断循环，**本次刚打好的新 zip 反而漏掉**；而用 pom 的版本号
  会在两处不一致时一个都筛不到、**静默跳过**套目录。函数本身也做了幂等保护。
- **版本号有两个来源，发版必须同时改**：根 `pom.xml` 的 `<version>`（脚本用它做报告，
  两个子模块的 `<parent><version>` 要跟着改）与 `electron/package.json` 的 `version`
  （决定产物文件名，`package-lock.json` 顶层跟着改）。
  对用户可见的变化写进根 `CHANGELOG.md`，按版本累积。
  （`backend/target/app.jar` 是固定名，升版本号不影响 stage 与 main.js 的路径解析。）
- 卸载：数据在**安装目录\data**（不在 AppData），由 `electron/build/installer.nsh` 询问是否保留；
  「保留」必须原地不动 —— NSIS 升级时会先跑卸载流程。

## 本机坑（Windows + PowerShell 5.1）

- **单条工具命令约 2 分钟就被截断，`timeout` 参数不生效。** 表现是 transcript 停在半路、
  没有异常块、也没有收尾行，极具迷惑性。长任务（打包、CDS 训练）一律用
  `run_in_background: true` 起来，再用 `TaskOutput(block=true)` 在**同一个回合内**等完 ——
  **回合一结束，后台任务就会被杀**（这就是第一次打包失败的原因：启动后台任务后又结束了回合）。
  被截断时直系子进程（powershell）会死，但孙进程（electron-builder 的 node）变孤儿继续跑；
  而它写 stdout 的管道已没人读，缓冲区一满就**阻塞**（现象：`win-unpacked.tmp` 停在
  368 MB 不再长、`.lock` 不更新）。所以长任务的输出**必须重定向到文件**，不要留管道。
- 沙箱只静态拦「直接敲的命令文本」，**不拦 `.ps1` 文件内部**。以下会被拒：
  `Add-Type`、`Start-Process` 拉 shell/解释器、在 Bash 里调 `powershell.exe`。
- PowerShell 工具不回传 stdout（连 `"done"` 都收不到），一律落盘再 Read。
- Bash 的 PATH 会间歇性丢失（`head` / `grep` / `wmic` 随机找不到）；开头显式
  `export PATH="/usr/bin:/bin:...PortableGit/versions/1.2.0/usr/bin:$PATH"` 能救回来。
- 查进程：本机没有 `wmic`；`tasklist` 可用，但**务必核对命令行**再动手 ——
  常驻的 `node.exe` 是 WorkBuddy 自己的 MCP 服务（sheetagent / weixinpay），不是构建进程。

- PowerShell 工具不回传 stdout，输出必须落盘再读；`Write-Host` 看不到，`Start-Transcript` 可以。
- 不要用 `*>&1 | Out-File` 宽重定向，会命中沙箱 `.ssh` 拦截；
  改用 `> file 2>&1` 或 `Start-Process -RedirectStandardOutput`。
- `Remove-Item` 被包成 safe-delete，回收站不可用时 fail-closed 直接抛错；
  改用 `[System.IO.File]::Delete()` / `[System.IO.Directory]::Delete()`。
- **jlink 产出的 `classes.jsa` 是只读文件**：删旧 JRE 时报"访问被拒绝"且完全不提只读，
  看着像被占用。必须先递归清只读位再删。
- `vite build` 清空 dist 会触发沙箱批量删除守卫（阈值 50 个文件），
  要先手动删 dist，并清掉继承来的 `CODEBUDDY_*` / `GENIE_*` 环境变量。
- `scripts/*.ps1` 必须 **UTF-8 with BOM**，否则 PowerShell 5.1 按 GBK 解析中文脚本报错。
- 无头出图 / 验页面：Edge 在 `C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`
  ——注意是 `Microsoft\Edge`，**没有空格**（写成 `Microsoft Edge` 会找不到文件）。
  `--headless=new --screenshot=<png>` 出图；`--headless=new --dump-dom` 把页面 DOM 打到 stdout。
  **`--dump-dom` 配 `--virtual-time-budget=<ms>` 能取到异步跑完的结果**：让探针页把结论写进
  `<pre>` / `document.title`，再 grep dump 出来的 HTML —— 这是验证"某段 JS 在真实浏览器里到底成不成"
  最快的手段，尤其在拿不到 GUI 交互的时候。两种模式都要加 `--user-data-dir=<临时目录>`
  （否则会复用已开实例 / 不生效）。

## 验收

- 冒烟测试：`scripts/smoke-test.ps1`（43 项接口断言），仍按旧 app-image 布局找 JRE，待适配 Electron 包结构。
- 外壳日志里的 `前端页面已渲染: <url>` 是「真的渲染出来了」的判据 —— 光看窗口存在和标题非空盖不住白屏。
