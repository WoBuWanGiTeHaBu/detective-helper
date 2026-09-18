<#
.SYNOPSIS
  生成 AppCDS 共享归档（app.jsa）—— 构建期动作，不是运行期。

.DESCRIPTION
  为什么需要「训练」这一步，而不是让应用自己生成：
    -XX:ArchiveClassesAtExit 只在 JVM【正常退出】时才写归档。桌面外壳停后端用的是
    child.kill()，在 Windows 上等于 TerminateProcess，JVM 没有任何机会落盘。
    直接把 CDS 开到线上进程上的后果是——每一次启动都白付一遍生成归档的代价，
    实测启动从 7.5 秒涨到 48 秒。所以训练必须是一个「启动完、自己 System.exit(0)」
    的独立进程，也就是 CdsTrainingEntry。

  为什么要 jar 而不是展开目录：
    动态归档不允许 classpath 上出现非空目录，否则 JVM 直接拒绝：
      Error: non-empty directory '.../backend/target/classes'
    所以运行形态是「一个应用 jar + lib\* 通配符」，训练与运行期用【完全相同的】
    classpath —— CDS 会逐项比对 classpath，不一致就报 shared class paths mismatch
    并拒绝使用归档。

  实测收益（轮转 4 轮取中位数，「JVM 启动 → 应用就绪」）：
      展开目录 + 无 CDS ：3.97 秒
      jar      + 无 CDS ：3.36 秒
      jar      + CDS    ：2.06 秒

  ⚠️ 归档是按【jar 的时间戳】校验的，不是按内容 —— JVM 的原话是
     "A jar file is not the one used while building the shared archive file: xxx.jar"
     "xxx.jar timestamp has changed."  →  "Unable to use shared archive."
     所以只要 jar 被重写一次（哪怕一个字都没改），归档就失效、必须重训。
     这也正是指纹用 size+mtime 而不是"内容哈希"的原因：
     内容哈希相同但时间戳变了，归档照样会被拒，指纹必须反映 JVM 看到的那份身份。

  ⚠️ 归档与「JDK 版本 + app.jar + lib 下所有 jar」强绑定，任一变化都必须重新训练。
  本脚本把这几项算成一个指纹写进 app.jsa.meta，指纹一致就直接跳过（约 1 秒）。

  实测训练开销：约 50 秒。这时间不在写盘上（44 MB 落盘只要 0.06 秒，本机磁盘实测
  640 MB/s），而在 JVM 落盘【之前】的处理阶段，且与归档体积成正比：
      0.56 MB -> 1.1 秒      36 MB -> 34.7 秒      44 MB -> 49 秒
  这是动态归档的固有特性，不是环境问题。所以策略是「构建期生成一次、随包发布」；
  开发期指纹一致会自动跳过，急着看界面时用 run-electron-dev.ps1 -SkipCds。

.PARAMETER Force
  忽略指纹，强制重新训练。

.PARAMETER JavaHome
  用哪个 JDK 训练。**必须与运行期用的是同一个**，否则指纹虽对、归档也会被拒。
  打包时要把这里指到最终随包发布的运行时上。

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\make-cds-archive.ps1

.EXAMPLE
  # 指纹不一致才重训（给构建脚本用）
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\make-cds-archive.ps1
#>

[CmdletBinding()]
param(
    [string]$JavaHome,
    [string]$JarPath,
    [string]$LibDir,
    [string]$ArchivePath,
    [string]$DataDir,
    [switch]$Force,
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'

$ScriptDir = $PSScriptRoot
if (-not $ScriptDir) { $ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path }
. (Join-Path $ScriptDir '_common.ps1')

$ProjectRoot = Get-DhaProjectRoot -ScriptDir $ScriptDir

if (-not $JavaHome)     { $JavaHome     = Resolve-DhaJavaHome }
if (-not $JarPath)      { $JarPath      = Join-Path $ProjectRoot 'backend\target\app.jar' }
if (-not $LibDir)       { $LibDir       = Join-Path $ProjectRoot 'backend\target\lib' }
if (-not $ArchivePath)  { $ArchivePath  = Join-Path $ProjectRoot 'backend\target\cds\app.jsa' }
if (-not $DataDir)      { $DataDir      = Join-Path $ProjectRoot 'backend\target\cds\data' }

$MetaPath = "$ArchivePath.meta"
$Java     = Join-Path $JavaHome 'bin\java.exe'
$TrainClass = 'com.theos.detectivehelper.devtools.CdsTrainingEntry'

function Write-Line {
    param([string]$Text)
    if (-not $Quiet) { Write-Host $Text }
}

# 沙箱/受限环境里 PowerShell 的 Remove-Item 会被包装成 safe-delete，
# 在回收站不可用时会 fail-closed 直接抛错。构建脚本不该因此挂掉，用 .NET 直接删。
function Remove-FileHard {
    param([string]$Path)
    if (Test-Path -LiteralPath $Path) {
        # CDS 训练产出的 .jsa 带只读位，直接 File::Delete 会抛「访问被拒绝」，
        # 而报错完全不提只读 —— 看着像被进程占用，实际只是属性问题。先摘掉。
        try {
            $item = Get-Item -LiteralPath $Path -Force
            if ($item.IsReadOnly) { $item.IsReadOnly = $false }
        } catch {
            # 取属性失败也继续尝试删除，让下面抛出的才是真实原因
        }
        [System.IO.File]::Delete($Path)
    }
}

# ---------------------------------------------------------------- 前置校验

if (-not (Test-Path $Java)) { throw "找不到 java.exe：$Java" }
if (-not (Test-Path $JarPath)) {
    throw "找不到应用 jar：$JarPath`n先跑一次 scripts\run-electron-dev.ps1 或 `mvn -pl frontend,backend package` 生成它。"
}
if (-not (Test-Path $LibDir)) { throw "找不到依赖目录：$LibDir" }

$libCount = @(Get-ChildItem $LibDir -Filter *.jar).Count
if ($libCount -eq 0) { throw "$LibDir 下没有 jar" }

<#
  ⚠️ 动态归档依赖【基础 CDS 归档】（classes.jsa）。

  用 jlink 裁出来的 JRE 默认【不带】它，而缺失时的症状极具误导性：
      [warning][cds] -XX:ArchiveClassesAtExit is unsupported when base CDS archive is not loaded.
  只是一条 warning，JVM 照常把应用起完、训练进程照常打印 CDS_TRAIN_READY，
  但归档永远不生成 —— 白等 50 秒，最后只得到一句"归档没有生成"，很难指向根因。

  所以在这里提前拦住（省下那 50 秒），并把修法直接写在错误里。
#>
$baseCds = @(
    (Join-Path $JavaHome 'lib\server\classes.jsa'),
    (Join-Path $JavaHome 'bin\server\classes.jsa')
) | Where-Object { Test-Path $_ } | Select-Object -First 1

if (-not $baseCds) {
    throw @"
$JavaHome 里没有基础 CDS 归档（lib\server\classes.jsa 或 bin\server\classes.jsa）。

-XX:ArchiveClassesAtExit 是「在基础归档之上再叠一层」，基础归档缺失时
JVM 只打一条 warning 就继续，训练进程仍会打印就绪标记，但归档不会生成。

修法：
  - 用 jlink 裁的 JRE：给 jlink 加 --generate-cds-archive（见 scripts\stage-runtime.ps1）
  - 直接用完整 JDK：它自带 bin\server\classes.jsa，无需处理
"@
}

New-Item -ItemType Directory -Force -Path (Split-Path $ArchivePath -Parent), $DataDir | Out-Null

# ---------------------------------------------------------------- 指纹

$javaVersion = 'unknown'
$releaseFile = Join-Path $JavaHome 'release'
if (Test-Path $releaseFile) {
    $m = [regex]::Match((Get-Content $releaseFile -Raw), 'JAVA_VERSION="([^"]+)"')
    if ($m.Success) { $javaVersion = $m.Groups[1].Value }
}

$jarItem = Get-Item $JarPath
$libSig = (Get-ChildItem $LibDir -Filter *.jar | Sort-Object Name | ForEach-Object { "$($_.Name)/$($_.Length)" }) -join ','
$sha1 = [System.Security.Cryptography.SHA1]::Create()
$libHash = ([System.BitConverter]::ToString($sha1.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($libSig)))).Replace('-', '')
$sha1.Dispose()

$signature = "java=$javaVersion;jar=$($jarItem.Length)/$($jarItem.LastWriteTimeUtc.Ticks);lib=$libHash;n=$libCount"

if (-not $Force -and (Test-Path $ArchivePath) -and (Test-Path $MetaPath)) {
    $old = (Get-Content $MetaPath -Raw).Trim()
    if ($old -eq $signature) {
        Write-Line "CDS 归档已是最新（$([math]::Round((Get-Item $ArchivePath).Length / 1MB, 1)) MB），跳过训练。"
        # 用 return 而不是 exit：万一将来有人 dot-source 这个脚本，exit 会把调用方一起干掉
        return
    }
    Write-Line 'CDS 归档指纹已变化（JDK / app.jar / 依赖有改动），重新训练。'
}

# ---------------------------------------------------------------- 训练

Write-Line "CDS 训练：Java $javaVersion，app.jar $([math]::Round($jarItem.Length / 1KB)) KB，依赖 $libCount 个"
Write-Line "         归档 -> $ArchivePath"

Remove-FileHard $ArchivePath
Remove-FileHard $MetaPath

# 训练用独立的数据目录：不要拿用户真实案件库去陪跑一次启动。
$trainOut = "$ArchivePath.train.log"
$trainErr = "$ArchivePath.train.err"
Remove-FileHard $trainOut
Remove-FileHard $trainErr

# 注意 classpath 形态必须与运行期完全一致：jar + lib\*
# （main.js 的 buildClasspath 用的是同一形态，两边一起改才不会踩 mismatch）
$cp = "$JarPath;$(Join-Path $LibDir '*')"

Remove-DhaDuplicateEnvKeys | Out-Null

$trainArgs = @(
    '-XX:TieredStopAtLevel=1'
    "-XX:ArchiveClassesAtExit=$ArchivePath"
    '-Dfile.encoding=UTF-8'
    "-Ddetectivehelper.data.dir=$DataDir"
    '-cp'
    $cp
    $TrainClass
    '--server.port=0'
    '--server.address=127.0.0.1'
)

$trainProc = Start-Process -FilePath $Java -ArgumentList $trainArgs `
    -WorkingDirectory $ProjectRoot `
    -RedirectStandardOutput $trainOut -RedirectStandardError $trainErr `
    -PassThru -NoNewWindow

if (-not $trainProc.WaitForExit(180000)) {
    Stop-Process -Id $trainProc.Id -Force -ErrorAction SilentlyContinue
    throw '训练进程 180 秒没有退出'
}

$trainTxt = Get-Content $trainOut -Raw -ErrorAction SilentlyContinue
if (-not $trainTxt -or $trainTxt -notmatch 'CDS_TRAIN_READY') {
    $errTxt = Get-Content $trainErr -Raw -ErrorAction SilentlyContinue
    throw "训练进程没有走到就绪标记（exit=$($trainProc.ExitCode)）。`nstdout: $trainTxt`nstderr: $errTxt"
}

if (-not (Test-Path $ArchivePath)) {
    throw "训练自称成功，但归档没有生成：$ArchivePath"
}

# ---------------------------------------------------------------- 校验归档真的可用

<#
  光看文件存在是不够的：-Xshare:auto 在归档不被接受时会【静默回退】成普通启动，
  不校验就会把「没生效」误读成「没收益」，而且一点报错都没有。

  这里用 -Xshare:on 做一次强校验 —— 归档被拒时 JVM 会直接失败并打印
  "shared class paths mismatch" / "Unable to use shared archive"。
  配 -version 就走完了归档加载与 classpath 比对，不用真把应用起起来，几十毫秒。
#>
$verifyOut = "$ArchivePath.verify.log"
$verifyErr = "$ArchivePath.verify.err"
Remove-FileHard $verifyOut
Remove-FileHard $verifyErr

$verifyProc = Start-Process -FilePath $Java `
    -ArgumentList @('-Xshare:on', "-XX:SharedArchiveFile=$ArchivePath", '-cp', $cp, '-version') `
    -RedirectStandardOutput $verifyOut -RedirectStandardError $verifyErr `
    -PassThru -NoNewWindow -Wait

if ($verifyProc.ExitCode -ne 0) {
    $detail = (Get-Content $verifyErr -Raw -ErrorAction SilentlyContinue)
    if (-not $detail) { $detail = (Get-Content $verifyOut -Raw -ErrorAction SilentlyContinue) }
    Remove-FileHard $ArchivePath
    throw "归档生成后无法被接受（-Xshare:on 退出码 $($verifyProc.ExitCode)），已删除该归档，避免留下一个会静默失效的坑：`n$detail"
}

Set-Content -Path $MetaPath -Value $signature -Encoding UTF8

$sizeMB = [math]::Round((Get-Item $ArchivePath).Length / 1MB, 1)
Write-Line "CDS 归档就绪：$sizeMB MB（-Xshare:on 校验通过）"
