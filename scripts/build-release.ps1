<#
.SYNOPSIS
  一键产出 Detective Helper 的 Windows 发行包：便携 zip + 安装 exe。

.DESCRIPTION
  完整链路（4 步，任一步失败即中止）：

    1/4  Maven 构建
         frontend(npm run build → dist)
           → backend(把 dist 收进 classpath:static，产出普通 jar)
         并把 Maven 的普通 jar 同步成固定名 backend\target\app.jar。

         为什么是普通 jar 而不是 Spring Boot fat jar：外壳按
         `java -cp "app.jar;lib\*"` 启动后端，fat jar 的 BOOT-INF 布局会让每个类
         都走一遍 BootLoader 偏移量解压，冷启动明显更慢，而且 CDS 归档也复用不了。

    2/4  准备运行时暂存（scripts\stage-runtime.ps1）
         jlink 裁 JRE → 组装 app\{app.jar,lib\} → 实起验证 → 用裁剪 JRE 训 CDS 归档
         产物 build\stage-jvm\{jre,app} 正是 electron-builder 的 extraResources 输入，
         两边的目录结构是一一对应的，改任何一边都要同步改另一边。

    3/4  electron-builder 打包
         产物 build\release\ ：
           Detective-Helper-<版本>-win-x64.zip          ← 便携版，解压即用
           Detective-Helper-<版本>-win-x64-setup.exe    ← 安装包（NSIS 向导）

    4/4  报告产物、体积与用法

  为什么值得多做一个 zip（而不是用 portable 目标）：
    electron-builder 的 portable 是「自解压 exe」，每次运行都要把约 300 MB 解到
    %TEMP% 再启动，实测冷启动 23 秒。zip 只需要解压一次，之后双击就是正常启动。
    所以分发优先给 zip；要给不懂解压的人，再给 setup.exe。

.PARAMETER NoBuild
  跳过 Maven 构建，直接复用 backend\target 下的现有产物。只调打包参数时用。

.PARAMETER SkipStage
  跳过运行时暂存（jlink 裁剪 + CDS 训练），复用现有 build\stage-jvm。
  只在确认 JRE / 依赖都没变时用；盲目复用会让包里带着旧 JRE。

.PARAMETER SkipPackage
  跳过 electron-builder，只补做「给 zip 套顶层目录」那一步。
  用途：electron-builder 已经跑完、只有套目录这步挂了时，不必再等那几分钟。

.PARAMETER Targets
  只出某一种格式，逗号分隔：zip / nsis。默认两种都出。

.PARAMETER RunTests
  默认 -DskipTests；加这个开关才跑单元测试。

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1

.EXAMPLE
  # 只改了 Electron 侧代码，Java 产物没动
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1 -NoBuild -SkipStage

.EXAMPLE
  # 只要便携版
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1 -Targets zip
#>

[CmdletBinding()]
param(
    [switch]$NoBuild,
    [switch]$SkipStage,
    [switch]$SkipPackage,
    [string]$Targets = 'zip,nsis',
    [switch]$RunTests
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '_common.ps1')

$root         = Get-DhaProjectRoot -ScriptDir (Get-DhaScriptDir)
$electronDir  = Join-Path $root 'electron'
$releaseDir   = Join-Path $root 'build\release'
$stageDir     = Join-Path $root 'build\stage-jvm'
$stageJre     = Join-Path $stageDir 'jre'
$stageApp     = Join-Path $stageDir 'app'
$stageCds     = Join-Path $stageApp 'app.jsa'
$backendTarget = Join-Path $root 'backend\target'
$appJar       = Join-Path $backendTarget 'app.jar'

Write-DhaStep 'Detective Helper 发行打包'

# ---------------------------------------------------------------- 环境自检

function Resolve-DhaNode {
    $cmd = Get-Command node -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $managed = 'C:\Users\shengqi\.workbuddy\binaries\node\versions\22.22.2-3\node.exe'
    if (Test-Path $managed) { return $managed }
    return $null
}

function Resolve-DhaNpmCli {
    param([string]$NodeExe)
    $nodeParent = Split-Path $NodeExe
    foreach ($candidate in @(
            (Join-Path $nodeParent 'node_modules\npm\bin\npm-cli.js'),
            (Join-Path $nodeParent '..\node_modules\npm\bin\npm-cli.js'))) {
        if (Test-Path $candidate) { return (Resolve-Path $candidate).Path }
    }
    return $null
}

$nodeExe = Resolve-DhaNode
if (-not $nodeExe) { Write-DhaFail '找不到 node.exe，请先安装 Node.js'; exit 1 }

$npmCli = Resolve-DhaNpmCli -NodeExe $nodeExe

# 本机环境块里有「只有大小写不同」的重复变量，会让 Start-Process 直接崩
Remove-DhaDuplicateEnvKeys | Out-Null

# electron-builder 的 winCodeSign / nsis 工具包从 GitHub 拉，国内经常 502。
# 挂 npmmirror 镜像；缓存已有时这里只是空转。
$env:ELECTRON_BUILDER_BINARIES_MIRROR = 'https://npmmirror.com/mirrors/electron-builder-binaries/'

$electronNodeModules = Join-Path $electronDir 'node_modules'
if (-not (Test-Path $electronNodeModules)) {
    Write-DhaFail "缺少 $electronNodeModules。先跑一次 scripts\run-electron-dev.ps1 把依赖装上。"
    exit 1
}
$builderCli = Join-Path $electronNodeModules 'electron-builder\out\cli\cli.js'
if (-not (Test-Path $builderCli)) {
    Write-DhaFail "找不到 electron-builder：$builderCli"
    exit 1
}
if (-not (Test-Path (Join-Path $electronNodeModules 'electron\dist\electron.exe'))) {
    Write-DhaFail 'Electron 二进制缺失（npm install 的 postinstall 可能静默失败了），先跑 scripts\run-electron-dev.ps1'
    exit 1
}

$targetList = @($Targets -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
if ($targetList.Count -eq 0) { Write-DhaFail '-Targets 不能为空'; exit 1 }

function Add-ZipRootFolder {
    <#
      把 electron-builder 的 zip 重打一层顶层目录。

      它的 zip target 是直接压缩 win-unpacked 的内容，解压后 300 多个文件
      散在同一个文件夹里 —— 用户拖到桌面就收拾不回去了。这里统一塞进
      名为 "Detective Helper" 的顶层目录，解压即得一个干净的文件夹。

      不用 ZipFile::CreateFromDirectory(includeBaseDirectory) 是因为那会用
      win-unpacked 当目录名，而改名 win-unpacked 会让下次构建多解压一次 Electron。
    #>
    param(
        [Parameter(Mandatory = $true)][string]$SourceDir,
        [Parameter(Mandatory = $true)][string]$ZipPath,
        [Parameter(Mandatory = $true)][string]$RootName
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    # ⚠ ZipArchive / ZipArchiveMode / CompressionLevel 都住在 System.IO.Compression 里，
    #   只加载 .FileSystem 是不够的（它只带 ZipFile / ZipFileExtensions）。
    #   以前只写上面那一行也能过，纯属运气 —— 那个会话恰好已经把 System.IO.Compression
    #   加载进来了（别的模块或前一条命令干的）。换成干净的会话就报
    #   「找不到类型 [System.IO.Compression.ZipArchiveMode]」。两行都要留着。
    Add-Type -AssemblyName System.IO.Compression

    # 幂等：已套过顶层目录的 zip 不再重复套，否则会变成 Detective Helper/Detective Helper/...
    $probe = [System.IO.Compression.ZipFile]::OpenRead($ZipPath)
    try {
        $firstEntry = $probe.Entries | Select-Object -First 1
        if ($firstEntry -and $firstEntry.FullName.StartsWith("$RootName/")) {
            Write-DhaInfo "  $([System.IO.Path]::GetFileName($ZipPath)) 已带顶层目录，跳过"
            return
        }
    } finally {
        $probe.Dispose()
    }

    $srcFull = (Resolve-Path -LiteralPath $SourceDir).Path.TrimEnd('\')
    $tmp = "$ZipPath.tmp"
    if (Test-Path $tmp) { [System.IO.File]::Delete($tmp) }

    $stream = [System.IO.File]::Open($tmp, [System.IO.FileMode]::Create)
    try {
        $archive = New-Object System.IO.Compression.ZipArchive($stream, [System.IO.Compression.ZipArchiveMode]::Create)
        try {
            foreach ($f in Get-ChildItem -LiteralPath $srcFull -Recurse -File -Force) {
                $rel = $f.FullName.Substring($srcFull.Length + 1).Replace('\', '/')
                $entry = $archive.CreateEntry("$RootName/$rel", [System.IO.Compression.CompressionLevel]::Optimal)
                $entryStream = $entry.Open()
                try {
                    $fileStream = [System.IO.File]::OpenRead($f.FullName)
                    try { $fileStream.CopyTo($entryStream) } finally { $fileStream.Dispose() }
                } finally { $entryStream.Dispose() }
            }
        } finally { $archive.Dispose() }
    } finally { $stream.Dispose() }

    if (Test-Path $ZipPath) { [System.IO.File]::Delete($ZipPath) }
    Move-Item -LiteralPath $tmp -Destination $ZipPath -Force
}

# ---------------------------------------------------------------- 1/4 Maven

if ($NoBuild) {
    Write-DhaStep '1/4 跳过 Maven 构建（-NoBuild）'
    if (-not (Test-Path $appJar)) {
        Write-DhaFail "指定了 -NoBuild，但 $appJar 不存在"
        exit 1
    }
} else {
    Write-DhaStep '1/4 Maven 构建（frontend → backend）'
    $javaHome = Resolve-DhaJavaHome
    $mavenCmd = Resolve-DhaMavenCmd -ProjectRoot $root

    $mavenArgs = @('-B', 'package')
    if (-not $RunTests) { $mavenArgs += '-DskipTests' }

    Invoke-DhaMaven -MavenCmd $mavenCmd -JavaHome $javaHome -WorkingDirectory $root -MavenArgs $mavenArgs

    # 把 Maven 的普通 jar 同步成固定名 app.jar。
    # 固定名让开发态、暂存态、包内路径三处一致，electron/main.js 只认这一个名字。
    $mavenJars = @(Get-ChildItem $backendTarget -Filter '*.jar' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -ne 'app.jar' -and $_.Name -notmatch '-(sources|javadoc)\.jar$' })
    $newestJar = $mavenJars | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $newestJar) {
        Write-DhaFail "Maven 没产出 jar：$backendTarget"
        exit 1
    }
    Copy-Item $newestJar.FullName $appJar -Force
    Write-DhaOk "构建完成，app.jar <- $($newestJar.Name)"
}

# ---------------------------------------------------------------- 2/4 运行时暂存

if ($SkipStage) {
    Write-DhaStep '2/4 跳过运行时暂存（-SkipStage）'
    if (-not (Test-Path (Join-Path $stageJre 'bin\java.exe'))) {
        Write-DhaFail "指定了 -SkipStage，但 $stageJre\bin\java.exe 不存在"
        exit 1
    }
    if (-not (Test-Path (Join-Path $stageApp 'app.jar'))) {
        Write-DhaFail "指定了 -SkipStage，但 $stageApp\app.jar 不存在"
        exit 1
    }
} else {
    Write-DhaStep '2/4 准备运行时暂存（jlink 裁 JRE + 组装 + 实起验证 + CDS）'
    & (Join-Path $PSScriptRoot 'stage-runtime.ps1')
    if ($LASTEXITCODE -ne 0 -and $null -ne $LASTEXITCODE) { exit $LASTEXITCODE }
}

# 包里的 jar 必须和暂存目录里的是同一份 —— CDS 归档是按 jar 的【时间戳】校验的，
# 归档和 jar 不配套时 JVM 会静默退回普通启动（慢约 1.3 秒，零报错）。
# electron-builder 复制 extraResources 不会重写 mtime，所以只要这之后别再动 jar 就安全。
if (-not (Test-Path $stageCds)) {
    Write-DhaWarn 'CDS 归档缺失 —— 包里的应用会以普通模式启动，比正常慢约 1.3 秒'
} else {
    Write-DhaInfo ("CDS 归档: {0} MB" -f [math]::Round((Get-Item $stageCds).Length / 1MB, 1))
}

# ---------------------------------------------------------------- 3/4 electron-builder

if ($SkipPackage) {
    Write-DhaStep '3/4 跳过 electron-builder（-SkipPackage）—— 只补做下面的套顶层目录'
} else {
    Write-DhaStep "3/4 electron-builder 打包（$($targetList -join ', ')）"
    Write-DhaInfo "产物目录：$releaseDir"

    $builderArgs = @($builderCli, '--win') + $targetList + @('--x64')
    Invoke-DhaNative -FilePath $nodeExe -Arguments $builderArgs -WorkingDirectory $electronDir
    if ($LASTEXITCODE -ne 0) {
        Write-DhaFail "electron-builder 失败（退出码 $LASTEXITCODE）"
        exit 1
    }
}

# ---------------------------------------------------------------- 3b. 给 zip 套顶层目录

$version = Get-DhaAppVersion -ProjectRoot $root

# 产物文件名是 electron-builder 按 electron/package.json 的 version 拼的，而
# Get-DhaAppVersion 读的是根 pom.xml。两个来源不一致时，按后者筛会一个 zip 都筛不到，
# 于是**静默跳过**套目录（包照发，但解压出来是一堆散文件）。所以这里按"产物实际用的
# 那个版本"来筛，并且一旦发现两处不一致就直说。
$pkgVersion = $null
$pkgJson = Join-Path $electronDir 'package.json'
if (Test-Path $pkgJson) {
    $mv = [regex]::Match((Get-Content $pkgJson -Raw -Encoding UTF8), '"version"\s*:\s*"([^"]+)"')
    if ($mv.Success) { $pkgVersion = $mv.Groups[1].Value.Trim() }
}
$zipVersion = $version
if ($pkgVersion) {
    $zipVersion = $pkgVersion
    if ($pkgVersion -ne $version) {
        Write-DhaWarn "版本号两处不一致：pom.xml=$version，electron/package.json=$pkgVersion（按后者筛产物，记得两处一起改）"
    }
}

# 只处理【本次版本】的 zip。以前这里是 '*.zip'，会顺手把 build\release 里遗留的旧版本
# zip 一起捞进来重打 —— 旧 zip 排在前面先被处理，一旦那步出错就中断整个循环，
# 反倒把本次刚打好的新 zip 漏掉（报错信息还会指向一个跟本次构建无关的旧文件）。
$zipArtifacts = @(Get-ChildItem $releaseDir -Filter "*-$zipVersion-*.zip" -File -ErrorAction SilentlyContinue)
if ($zipArtifacts.Count -eq 0) {
    if (-not $SkipPackage -and $targetList -contains 'zip') {
        Write-DhaWarn "刚出了 zip，却没找到文件名含 $zipVersion 的 zip —— 套顶层目录被跳过了，检查版本号"
    } else {
        Write-DhaInfo '  （没有需要处理的 zip，跳过套顶层目录）'
    }
}
foreach ($z in $zipArtifacts) {
    Write-DhaInfo "重打包 $($z.Name)：套上顶层目录 'Detective Helper'"
    Add-ZipRootFolder -SourceDir (Join-Path $releaseDir 'win-unpacked') -ZipPath $z.FullName -RootName 'Detective Helper'
}

# ---------------------------------------------------------------- 4/4 报告

Write-DhaStep '4/4 产物'

$artifacts = @()
if (Test-Path $releaseDir) {
    $artifacts = @(Get-ChildItem $releaseDir -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Extension -in @('.zip', '.exe') } |
        Sort-Object Name)
}

if ($artifacts.Count -eq 0) {
    Write-DhaFail "没有找到产物：$releaseDir"
    exit 1
}

Write-Host ''
foreach ($a in $artifacts) {
    Write-Host ("  {0,-52} {1,8} MB" -f $a.Name, [math]::Round($a.Length / 1MB, 1))
}
Write-Host ''
Write-DhaOk "发行包就绪（$version）-> $releaseDir"
Write-Host ''
Write-DhaInfo '怎么用：'
Write-DhaInfo '  zip  解压到任意目录，双击 Detective Helper.exe 即可（首次启动约 3 秒）'
Write-DhaInfo '  exe  双击进入安装向导；卸载时会问是否保留 data 目录里的笔记数据'
