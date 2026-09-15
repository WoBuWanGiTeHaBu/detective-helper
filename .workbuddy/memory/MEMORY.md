# Detective Helper · 项目长期约定

## 技术栈与运行形态（2026-09 定型）

- **外壳：Electron**。JavaFX WebView 已废弃 —— 它是旧 WebKit，在 150% 缩放下有
  JDK-8252649 模糊缺陷且交互卡。换 Chromium 后画面质量由构造保证。
- **`desktop/` 模块已删除**（2026-09-15 清理）：根 pom 现在只有 `frontend` + `backend`
  两个模块，Maven 不再产出任何可执行产物，外壳完全由 `electron/` 承担。
- **后端**：Spring Boot 3.3.4 + MyBatis + SQLite，纯无界面 HTTP 服务，只监听 127.0.0.1 的随机端口。
- **运行形态**：`java -cp "app.jar;lib\*"`。
  - 不要嵌套 fat jar（BootLoader 解压偏移量最慢）；
  - 也不要展开 classes 目录 —— CDS 要求 classpath 上不能出现非空目录，
    且实测普通 jar 比展开目录还快 0.6 秒。三者顺序：嵌套 fat jar > 展开目录 > 普通 jar。
- **启动参数（勿回退）**：`-XX:TieredStopAtLevel=1`（省 2.9s）+ CDS 归档（再省 1.3s）。
  归档由 `scripts/make-cds-archive.ps1` 构建期训练，指纹用 **jar 的 size+mtime**
  （CDS 是按时间戳校验 jar 的，内容没改但文件被重写照样失效；失效时会**静默**回退）。

## 入口与构建

- 开发启动：仓库根 `run-dev.cmd`（可双击），或 `scripts\run-electron-dev.ps1`。
  `-File scripts\xxx.ps1` 是**相对路径**，工作目录不对就报"不存在"。
- 前端改了必须重建才生效；`run-electron-dev.ps1` 会比时间戳自动重建，
  并在重建后自动重训 CDS（`-SkipCds` 可跳过，代价是启动慢约 1.3 秒）。
- 端到端启动基准靠外壳的 `DHA_BENCH=1` 自测（窗口隐藏、渲染完成后打总耗时）。
  原来放在 `build/bench/` 的那套脚本已随 `build/` 一起清理掉了 —— `build/` 现在是纯产物目录，
  要重新测量就按 `electron/main.js` 里的 `DHA_BENCH` 机制写，别再往 `build/` 里放工具。

## 打包与发布（2026-09-15 打通，同日晚重写为一键脚本）

- **一键发布：`scripts\build-release.ps1`**（旧 jpackage 路线的 `make-installer.ps1` 已删除）。
  四步：Maven 构建 → `stage-runtime.ps1` → electron-builder → 报告。
  开关：`-NoBuild` / `-SkipStage` / `-Targets zip`。
- **发行形态只有两种，都不出 portable**：
  `build/release/Detective-Helper-<版本>-win-x64.zip`（便携版，含顶层目录）
  与 `-setup.exe`（NSIS 安装包）。
  portable 是自解压 exe，每次运行解 300MB 到 %TEMP%，冷启动 23 秒，已废弃。
- **zip 必须套一层顶层目录**（`Detective-Helper/`）。electron-builder 的 zip target
  默认把 win-unpacked 的内容直接压平，解压出 300 多个散文件。脚本里的
  `Add-ZipRootFolder` 负责重打包。
- 链路：`scripts/stage-runtime.ps1` → `electron-builder`。产物须为
  `resources/jre` + `resources/app/{app.jar, lib, app.jsa}`（与 main.js 的解析一一对应）。
- stage 四步顺序**不可换**：裁 JRE → 组装 → **实起验证** → 用裁剪后的 JRE 训 CDS。
  第 3 步不能省：jlink 少带模块的症状是**运行期** NoClassDefFoundError，打包时零报错。
- ⚠️ jlink 默认不生成基础 CDS 归档，**必须加 `--generate-cds-archive`**。
  缺了它只有一条 warning，训练进程照常打印就绪标记，但 `app.jsa` 永不生成。
- ⚠️ **`--generate-cds-archive` 产出的 `jre\bin\server\classes.jsa` 是只读文件。**
  用 .NET 删 jlink 出的 JRE 必须**先递归清只读位**，否则抛「访问被拒绝」且**完全不提只读**，
  极易误判成文件被占用。`stage-runtime.ps1` 的 `Remove-Hard` 已按此处理。
- electron-builder 的工具包从 GitHub 下载，国内常 502。脚本已挂
  `ELECTRON_BUILDER_BINARIES_MIRROR=https://npmmirror.com/mirrors/electron-builder-binaries/`。
- CDS 能进包靠的是 **jar 时间戳不变**：electron-builder 复制 extraResources 不重写 mtime。
  任何"把 jar 再写一遍"的步骤都会让归档失效，且失效是**静默**的。
  验收方法：比对 `build/stage-jvm/app/app.jar` 与包内
  `resources/app/app.jar` 的 mtime，一致才算 CDS 在包里生效。
- 卸载：数据在**安装目录\data**（不在 AppData，`deleteAppDataOnUninstall` 救不了），
  由 `electron/build/installer.nsh` 的 `customUnInstall` 询问。
  **「保留」必须原地不动** —— 升级时 NSIS 会先跑卸载流程，移走数据会让新版本找不到旧数据。

## 硬性约定

- `scripts/*.ps1` **必须 UTF-8 with BOM**。PowerShell 5.1 读无 BOM 的 UTF-8 中文脚本会按
  GBK 解析并报"字符串缺少终止符"，线索极少。用 Write 工具新建后要补 BOM。
- 原生程序调用统一走 `_common.ps1` 的 `Invoke-DhaNative`；否则 `ErrorActionPreference='Stop'`
  会把原生的 stderr 提示（如 javac 的一行"注:"）升级成空 Message 的 RemoteException。
- 读 `data/logs/*.log` 必须显式 `-Encoding UTF8`（默认按 ANSI 读会中文乱码，
  而正则匹配不上是**静默**的，不报错）。
- 数据目录：`{应用目录}/data`，不可写时降级 `%LOCALAPPDATA%/Detective Helper/data`。
  `main.js` 里仍保留 `PORTABLE_EXECUTABLE_DIR` 分支（历史 portable 目标用），
  现在的发行形态走不到它，但别删 —— 拿 portable 做临时测试时会用到。
- 冒烟判据：外壳日志里的 **`前端页面已渲染: {url}`**（只看窗口存在 + 标题非空盖不住白屏）。
  改外壳时这行语义不能动。
- 时间对账：`app.log` 是本地时间，外壳 `shell.log` 是 UTC，相差 8 小时。

## 前端

- antd 走**按需注册**（`app.component`），不要 `app.use(Antd)`：全量 1405KB → 按需 439KB。
- 配色/字体令牌集中在 `src/styles/tokens.css`；antd 主题在 `App.vue` 的
  `a-config-provider :theme`。
- 字体栈纯本地，不外链 Google Fonts（`<head>` 外链会阻塞首屏）。
- 时间与下拉框一律用 antd 组件，不用原生 `datetime-local` / `<select>`。

## 测量纪律

- A/B 对比**必须轮转**（round-robin）；逐组跑完会被机器负载漂移带出反向结论
  （已经因此误判过一次「提前 spawn 反而慢」）。
- 判定后端就绪用 HTTP `/api/health`，不用日志正则（会匹配到旧运行的遗留行）。
- 每次测量前清日志，测完确认没有残留 JVM。
