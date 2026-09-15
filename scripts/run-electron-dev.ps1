<#
  开发期启动 Electron 外壳（不打包）。

  这个脚本存在的意义是把几个「不写下来就一定会踩」的坑挡在前面：

  1. ELECTRON_RUN_AS_NODE
     本机（以及不少 IDE / CI 环境）会设置这个环境变量，
     它会让 electron.exe 退化成普通 Node —— require('electron') 返回的是可执行文件路径
     而不是 API，于是报 `Cannot read properties of undefined (reading 'requestSingleInstanceLock')`。
     报错信息完全看不出去向，所以必须在启动前摘掉。

  2. Electron 二进制可能没下下来
     `npm install` 期间 electron 的 postinstall 可能静默失败：node_modules/electron 里
     有 index.js 却没有 dist/electron.exe，启动时报一堆看不懂的错。这里会检测并补跑 install.js。

  3. 后端运行产物还没产出
     外壳需要 backend/target/{app.jar, lib\*, cds\app.jsa}。
     为什么是 jar 而不是展开的 classes 目录：CDS 要求 classpath 上不能有非空目录，
     而且实测 jar 本身也更快（见 electron/main.js 里的实测表）。
     缺了就先跑一次 Maven，并把 Maven 的 jar 同步成固定名 app.jar。

  4. 产物是旧的（改动没生效）
     前端源码改了但没重新构建，启动后看到的还是上一版 bundle。
     现象是"页面没有任何报错，就是没变" —— 靠肉眼极难定位，所以这里比时间戳：
     源码比产物新就自动重建一次。重建后还会顺手重训 CDS 归档 ——
     归档过期只会静默退回普通启动（慢约 1.3 秒）而不报错，忘掉这一步损失很隐蔽。

  用法（先把工作目录切到项目根，或用绝对路径 —— 脚本自身不依赖当前目录，
       它按 $PSScriptRoot 定位项目根，所以 cd 到哪里都能跑）：

    cd E:\IdeaProjects\detective-helper
    powershell -ExecutionPolicy Bypass -File scripts\run-electron-dev.ps1

  或者一步到位（在任意目录下都有效，推荐）：

    powershell -ExecutionPolicy Bypass -File E:\IdeaProjects\detective-helper\scripts\run-electron-dev.ps1

  也可以直接双击项目根目录的 run-dev.cmd。

  加 -SkipBuild 可跳过 Maven 构建（产物已就绪时用，省 30 秒）。
#>
[CmdletBinding()]
param(
    # 跳过 Maven 构建（产物已就绪时用，能省 30 秒）
    [switch]$SkipBuild,

    # 跳过 CDS 归档训练。
    # 什么时候用：改了后端一行代码、只想赶紧看一眼界面。
    # 训练本身要 50 秒左右（JVM 动态归档的固有开销，与归档体积成正比，不是磁盘慢），
    # 代价是这次启动退回普通模式、慢约 1.3 秒。指纹没变时本来就会自动跳过，不算成本。
    [switch]$SkipCds
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '_common.ps1')

$root = Get-DhaProjectRoot -ScriptDir (Get-DhaScriptDir)
$electronDir = Join-Path $root 'electron'
$nodeModules = Join-Path $electronDir 'node_modules'

Write-DhaStep 'Electron 开发模式启动'

# ---------- 1. 摘掉 ELECTRON_RUN_AS_NODE ----------
# 大小写两种写法都清，Windows 下环境变量名不区分大小写，但枚举出来可能是任意一种
foreach ($name in @('ELECTRON_RUN_AS_NODE', 'electron_run_as_node')) {
    if (Test-Path "env:$name") {
        Remove-Item "env:$name" -Force -ErrorAction SilentlyContinue
        Write-DhaWarn "已摘掉环境变量 $name（它会让 electron.exe 退化成 Node）"
    }
}

# 这台机器的环境块里有「只有大小写不同」的重复变量，会让 Start-Process 直接崩，
# 顺手统一处理掉（详见 _common.ps1 的说明）
Remove-DhaDuplicateEnvKeys | Out-Null

# ---------- 2. 准备好 node_modules 与 Electron 二进制 ----------
# 优先用 PATH 上的 node（用户自己装的那份），找不到才退回到托管运行时
$nodeExe = $null
$cmd = Get-Command node -ErrorAction SilentlyContinue
if ($cmd) { $nodeExe = $cmd.Source }
if (-not $nodeExe) {
    $managed = 'C:\Users\shengqi\.workbuddy\binaries\node\versions\22.22.2-3\node.exe'
    if (Test-Path $managed) { $nodeExe = $managed }
}
if (-not $nodeExe) { Write-DhaFail '找不到 node.exe，请先安装 Node.js'; exit 1 }

# npm-cli.js 在 node 安装目录下，位置随发行方式略有不同，两个候选都试
$npmCli = $null
$nodeParent = Split-Path $nodeExe
foreach ($candidate in @(
        (Join-Path $nodeParent 'node_modules\npm\bin\npm-cli.js'),
        (Join-Path $nodeParent '..\node_modules\npm\bin\npm-cli.js'))) {
    if (Test-Path $candidate) { $npmCli = (Resolve-Path $candidate).Path; break }
}

if (-not (Test-Path $nodeModules)) {
    if (-not $npmCli) { Write-DhaFail '缺少 node_modules，且找不到 npm-cli.js，无法自动安装'; exit 1 }
    Write-DhaStep '安装 Electron 依赖（首次约 4 分钟）'
    Invoke-DhaNative -FilePath $nodeExe -Arguments @($npmCli, 'install', '--no-audit', '--no-fund') -WorkingDirectory $electronDir
    if ($LASTEXITCODE -ne 0) { Write-DhaFail 'npm install 失败'; exit 1 }
}

$electronExe = Join-Path $nodeModules 'electron\dist\electron.exe'
if (-not (Test-Path $electronExe)) {
    Write-DhaWarn 'Electron 二进制缺失（npm install 的 postinstall 静默失败了），补跑 install.js'
    $installJs = Join-Path $nodeModules 'electron\install.js'
    if (-not (Test-Path $installJs)) { Write-DhaFail "找不到 $installJs"; exit 1 }
    Invoke-DhaNative -FilePath $nodeExe -Arguments @('install.js') -WorkingDirectory (Join-Path $nodeModules 'electron')
    if (-not (Test-Path $electronExe)) { Write-DhaFail 'Electron 二进制仍然缺失，请检查网络后重试'; exit 1 }
    Write-DhaOk 'Electron 二进制已补齐'
}

# ---------- 3. 准备好后端运行产物（app.jar + lib\*） ----------
#
# 为什么是 jar 而不是展开的 classes 目录：CDS 要求 classpath 上不能出现非空目录，
# 而且实测 jar 本身也比展开目录快（3.36s vs 3.97s，见 electron/main.js 里的实测表）。
# app.jar 是 Maven 那个普通 jar 的固定名副本 —— 固定名让开发态与打包态走同一条路径。
$targetDir = Join-Path $root 'backend\target'
$libDir = Join-Path $targetDir 'lib'
$appJar = Join-Path $targetDir 'app.jar'
$needBuild = (-not (Test-Path $appJar)) -or (-not (Test-Path $libDir))
$buildReason = if ($needBuild) { '运行产物还没产出' } else { '' }

# 只看"存在"不够：改了源码再启动，看到的可能还是上一版 bundle，
# 而这类"改动没生效"的现象没有报错，只能靠时间戳发现。
if (-not $needBuild) {
    $sources = @()
    $sources += Get-ChildItem (Join-Path $root 'frontend\src') -Recurse -File -ErrorAction SilentlyContinue
    $sources += Get-ChildItem (Join-Path $root 'backend\src') -Recurse -File -ErrorAction SilentlyContinue
    foreach ($f in @('frontend\index.html', 'frontend\vite.config.ts', 'frontend\package.json')) {
        $item = Get-Item (Join-Path $root $f) -ErrorAction SilentlyContinue
        if ($item) { $sources += $item }
    }

    $newestSource = $sources | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($newestSource -and $newestSource.LastWriteTime -gt (Get-Item $appJar).LastWriteTime) {
        $needBuild = $true
        $buildReason = "源码比产物新（$($newestSource.Name)）"
    }
}

if ($needBuild -and -not $SkipBuild) {
    Write-DhaStep "需要重建：$buildReason"
    $javaHome = Resolve-DhaJavaHome
    $mavenCmd = Resolve-DhaMavenCmd -ProjectRoot $root
    Invoke-DhaMaven -MavenCmd $mavenCmd -JavaHome $javaHome -WorkingDirectory $root `
        -MavenArgs @('-B', '-pl', 'frontend,backend', 'package', '-DskipTests')
    Write-DhaOk '构建完成'
} elseif ($needBuild) {
    Write-DhaFail "产物缺失或过期（$buildReason），但指定了 -SkipBuild：$appJar"
    exit 1
}

# ---------- 3b. 把 Maven 的普通 jar 同步成固定名 app.jar ----------
$mavenJars = @(Get-ChildItem $targetDir -Filter '*.jar' -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -ne 'app.jar' -and $_.Name -notmatch '-(sources|javadoc)\.jar$' })
$newestJar = $mavenJars | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($newestJar) {
    $appJarItem = Get-Item $appJar -ErrorAction SilentlyContinue
    if ((-not $appJarItem) -or ($newestJar.LastWriteTime -gt $appJarItem.LastWriteTime)) {
        Copy-Item $newestJar.FullName $appJar -Force
        Write-DhaInfo "app.jar  <- $($newestJar.Name)"
    }
}
if (-not (Test-Path $appJar)) {
    Write-DhaFail "没能产出 $appJar（Maven 的 jar 产物没找到？）"
    exit 1
}

# ---------- 3c. CDS 归档 ----------
# 指纹一致时脚本会直接跳过（1 秒），所以每次都调用是廉价的；
# 而 JDK / app.jar / 依赖任一变化都会自动重新训练 —— 忘了这一步的代价是
# 静默退回普通启动（慢约 1.3 秒，且没有任何报错）。
#
# 注意：CDS 是按【jar 的时间戳】校验的（JVM 原话 "xxx.jar timestamp has changed"），
# 所以 Maven 每次重写 jar 都会让归档失效，必须重训。想跳过就用 -SkipCds。
if ($SkipCds) {
    Write-DhaWarn '已跳过 CDS 训练（-SkipCds）—— 本次启动会比正常慢约 1.3 秒'
} else {
    & (Join-Path $root 'scripts\make-cds-archive.ps1')
}

$jarCount = @(Get-ChildItem $libDir -Filter *.jar -ErrorAction SilentlyContinue).Count
$cdsJar = Join-Path $targetDir 'cds\app.jsa'
Write-DhaInfo "运行产物: app.jar + $jarCount 个依赖 jar"
if (Test-Path $cdsJar) {
    Write-DhaInfo "CDS 归档: $cdsJar"
} else {
    Write-DhaWarn 'CDS 归档缺失 —— 会用普通启动，比正常慢约 1.3 秒'
}

# ---------- 4. 启动 ----------
$shellLog = Join-Path $root 'data\logs\shell.log'
Write-DhaInfo "应用日志（外壳）: $shellLog"
Write-DhaInfo "应用日志（后端）: $(Join-Path $root 'data\logs\app.log')"
Write-DhaOk '正在启动，窗口出现后即可操作；关闭窗口即退出'

$proc = Start-Process -FilePath $electronExe -ArgumentList @('.') -WorkingDirectory $electronDir -PassThru
$proc.WaitForExit()
Write-DhaInfo "Electron 已退出（退出码 $($proc.ExitCode)）"
