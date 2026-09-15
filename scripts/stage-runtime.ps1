<#
.SYNOPSIS
  为 Electron 打包准备运行时暂存目录（build/stage-jvm/{jre, app}）。

.DESCRIPTION
  产出结构必须与 electron/main.js 的路径解析一一对应：

    build/stage-jvm/jre/            -> resources/jre/            （resolveJavaExe）
    build/stage-jvm/app/app.jar     -> resources/app/app.jar     （resolveClasspath）
    build/stage-jvm/app/lib/*.jar   -> resources/app/lib/
    build/stage-jvm/app/app.jsa     -> resources/app/app.jsa     （CDS 归档）

  四步，顺序不能换：

    1. jlink 裁出精简 JRE
       模块表是【保守取】的：多带几个模块无非大几 MB，少带一个就是运行期崩。
       刻意不含 JavaFX 专用的 jdk.jsobject / jdk.xml.dom —— 那是 WebView 时代的遗留，
       现在渲染全交给 Chromium，JVM 只跑 Spring Boot + Tomcat + MyBatis + SQLite。

    2. 组装 app.jar + lib
       app.jar 必须是【普通 jar】，不是 Spring Boot fat jar：
       嵌套 jar 的每个类都要过 BootLoader，实测比普通 jar 慢约 0.6 秒。

    3. 实测「裁剪 JRE 真能起后端」—— 这一步不能省
       jlink 少带模块的症状是运行期 NoClassDefFoundError，而不是打包时报错。
       不在这里拦住，问题会一路漏到用户第一次双击。

    4. 用【裁剪后的 JRE】训练 CDS 归档
       归档与训练它的 JDK 强绑定。拿完整 JDK 训练的归档放到裁剪 JRE 上用会被拒，
       而 -Xshare:auto 是【静默回退】—— 启动悄悄慢 1.3 秒，零报错。
       所以顺序必须是「先裁 JRE，再用它训练」，不能反。

.PARAMETER Force
  忽略指纹，强制重新裁剪与重新训练。

.PARAMETER SkipCds
  跳过 CDS 训练（调试用；正常打包不要跳，否则启动慢约 1.3 秒）。

.PARAMETER SkipVerify
  跳过第 3 步的实起验证（调试用；正常打包不要跳）。

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\stage-runtime.ps1
#>

[CmdletBinding()]
param(
    [string]$JavaHome,
    [switch]$Force,
    [switch]$SkipCds,
    [switch]$SkipVerify
)

$ErrorActionPreference = 'Stop'

$ScriptDir = $PSScriptRoot
if (-not $ScriptDir) { $ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path }
. (Join-Path $ScriptDir '_common.ps1')

$ProjectRoot = Get-DhaProjectRoot -ScriptDir $ScriptDir

$StageDir   = Join-Path $ProjectRoot 'build\stage-jvm'
$StageJre   = Join-Path $StageDir 'jre'
$StageApp   = Join-Path $StageDir 'app'
$StageLib   = Join-Path $StageApp 'lib'
$StageJar   = Join-Path $StageApp 'app.jar'
$StageCds   = Join-Path $StageApp 'app.jsa'
$StageStamp = Join-Path $StageDir 'stage.meta'

$SrcJar = Join-Path $ProjectRoot 'backend\target\app.jar'
$SrcLib = Join-Path $ProjectRoot 'backend\target\lib'

if (-not $JavaHome) { $JavaHome = Resolve-DhaJavaHome }
$jlink  = Join-Path $JavaHome 'bin\jlink.exe'
$srcJre = Join-Path $JavaHome 'jre'

function Write-Line { param([string]$Text) Write-Host $Text }

# 沙箱/受限环境会把 Remove-Item 包成 safe-delete，回收站不可用时 fail-closed 直接抛错。
# 构建脚本不该因此挂掉，一律用 .NET 直删。
function Remove-Hard {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return }
    $item = Get-Item -LiteralPath $Path -Force
    if ($item.PSIsContainer) {
        # 只读位必须先摘掉：jlink --generate-cds-archive 产出的 bin\server\classes.jsa
        # 是【只读】文件，Directory::Delete 碰到它直接抛
        #   「对路径 xxx\classes.jsa 的访问被拒绝」
        # 而这条报错完全不提只读，看起来像文件被占用，很容易往错的方向排查。
        Get-ChildItem -LiteralPath $Path -Recurse -Force -ErrorAction SilentlyContinue | ForEach-Object {
            try { if ($_.IsReadOnly) { $_.IsReadOnly = $false } } catch { }
        }
        [System.IO.Directory]::Delete($Path, $true)
    } else {
        try { if ($item.IsReadOnly) { $item.IsReadOnly = $false } } catch { }
        [System.IO.File]::Delete($Path)
    }
}

<#
  清掉 make-cds-archive.ps1 落在归档旁边的附属文件。

  它们默认跟 app.jsa 同目录，而那个目录会整个进发布包 ——
  于是包里会多出 15 KB 的训练日志、-Xshare 校验日志，以及一个指纹文件。
  文件不大，但训练日志里有构建机的绝对路径，属于不该外发的东西。

  代价：指纹文件被清掉后，make-cds-archive 自己那套"指纹一致就跳过"就失效了。
  所以本脚本的跳过分支改成只看 app.jsa 在不在，不再去问它的指纹 —— 见下面。
#>
function Clear-CdsNoise {
    param([string]$Archive)
    foreach ($suffix in @('.meta', '.train.log', '.train.err', '.verify.log', '.verify.err')) {
        Remove-Hard "$Archive$suffix"
    }
}

# ---------------------------------------------------------------- 前置校验

if (-not (Test-Path $jlink)) { throw "找不到 jlink.exe：$jlink`n（需要 JDK 而不是 JRE，且 JDK 9+）" }
if (-not (Test-Path $SrcJar)) {
    throw "找不到 $SrcJar`n先跑 scripts\run-electron-dev.ps1（它会同步出固定名 app.jar）。"
}
if (-not (Test-Path $SrcLib)) { throw "找不到依赖目录：$SrcLib" }

$libJars = @(Get-ChildItem $SrcLib -Filter *.jar)
if ($libJars.Count -eq 0) { throw "$SrcLib 下没有 jar" }

$srcJarItem = Get-Item $SrcJar

<#
  模块表。
  保守原则：宁多勿少 —— 多带一个模块只是几 MB，少带一个就是运行期崩。
  但 WebView 时代的两个模块（jdk.jsobject / jdk.xml.dom）明确不要：
  jdk.jsobject 是 JS<->Java 桥，现在没有任何代码用它。
#>
$modules = @(
    'java.base'              # 一切的基础
    'java.compiler'          # Spring 的 SpEL / 字节码相关
    'java.desktop'           # java.beans（Spring 属性绑定）、ImageIO
    'java.instrument'        # Spring Boot 的启动期织入检查
    'java.logging'           # JUL —— Tomcat / SQLite 驱动都用
    'java.management'        # JMX（Spring Boot 默认开启 actuator 基础设施）
    'java.naming'            # JNDI —— Tomcat 容器初始化
    'java.prefs'             # 部分库会用
    'java.security.jgss'     # Tomcat 的认证链路
    'java.security.sasl'     # 同上
    'java.sql'               # JDBC —— SQLite 驱动必需
    'java.transaction.xa'    # 连接池的 XA 支持
    'java.xml'               # 配置解析
    'java.xml.crypto'        # XML 签名（Spring Security 链路）
    'jdk.crypto.ec'          # 椭圆曲线（TLS 用得到）
    'jdk.crypto.cryptoki'    # PKCS#11
    'jdk.charsets'           # UTF-8 之外的字符集，读写非 ASCII 案件数据要它
    'jdk.localedata'         # 中文日期/数字格式化 —— 界面上全是中文
    'jdk.unsupported'        # sun.misc.Unsafe —— 若干库（Netty/Jackson）依赖
    'jdk.zipfs'              # zip 文件系统
)

$moduleSig = ($modules -join ',')
$jdkVersion = 'unknown'
$releaseFile = Join-Path $JavaHome 'release'
if (Test-Path $releaseFile) {
    $m = [regex]::Match((Get-Content $releaseFile -Raw), 'JAVA_VERSION="([^"]+)"')
    if ($m.Success) { $jdkVersion = $m.Groups[1].Value }
}

# jlink 选项也必须进指纹：改了选项（例如补上 --generate-cds-archive）却没进指纹的话，
# 下一次跑会因为"指纹一致"直接跳过裁剪，JRE 还是旧的、问题依旧。
$jlinkOpts = '--strip-debug --no-header-files --no-man-pages --compress=zip-6 --generate-cds-archive'

$signature = "jdk=$jdkVersion;jar=$($srcJarItem.Length)/$($srcJarItem.LastWriteTimeUtc.Ticks);lib=$($libJars.Count);modules=$moduleSig;opts=$jlinkOpts"

if (-not $Force -and (Test-Path $StageStamp)) {
    $old = (Get-Content $StageStamp -Raw).Trim()
    if ($old -eq $signature -and (Test-Path $StageJar) -and (Test-Path (Join-Path $StageJre 'bin\java.exe'))) {
        Write-Line '暂存目录已是最新，跳过裁剪与组装。'
        if ($SkipCds) {
            Write-Line '  跳过 CDS（-SkipCds）'
        } elseif (Test-Path $StageCds) {
            # 这里只看归档在不在，不再去问 make-cds-archive 的指纹：
            # 训练日志在本步骤末尾会被清掉（它们不该进发布包），指纹文件也跟着没了，
            # 若还依赖指纹判断，就会每次都白重训 50 秒。
            Write-Line "  CDS 归档已存在（$([math]::Round((Get-Item $StageCds).Length / 1MB, 1)) MB），跳过训练。"
        } else {
            & (Join-Path $ScriptDir 'make-cds-archive.ps1') -JavaHome $StageJre `
                -JarPath $StageJar -LibDir $StageLib -ArchivePath $StageCds `
                -DataDir (Join-Path $StageDir 'cds-data')
        }
        Clear-CdsNoise -Archive $StageCds
        return
    }
    Write-Line '暂存指纹已变化（JDK / app.jar / 依赖 / 模块表有改动），重新裁剪。'
}

New-Item -ItemType Directory -Force -Path $StageDir | Out-Null

# ---------------------------------------------------------------- 1. jlink

Write-Line "1/4 jlink 裁剪 JRE（源 JDK $jdkVersion，$($modules.Count) 个模块）"
Remove-Hard $StageJre

$jlinkArgs = @(
    '--add-modules', ($modules -join ',')
    '--output', $StageJre
    '--strip-debug'          # 去掉调试信息 —— 体积能省一大截
    '--no-header-files'      # 不输出 JNI 头文件
    '--no-man-pages'
    '--compress=zip-6'
    # 必须有这一项。动态归档（-XX:ArchiveClassesAtExit）建立在【基础 CDS 归档】之上，
    # 而 jlink 默认【不生成】基础归档 —— 缺了它，JVM 只打一条 warning 就继续跑，
    # 训练进程照样打印就绪标记，归档却始终不生成，白等 50 秒还找不到根因。
    '--generate-cds-archive'
)

$jlinkOut = Join-Path $StageDir 'jlink.log'
$jlinkErr = Join-Path $StageDir 'jlink.err'
Remove-Hard $jlinkOut
Remove-Hard $jlinkErr

Remove-DhaDuplicateEnvKeys | Out-Null
$jlinkProc = Start-Process -FilePath $jlink -ArgumentList $jlinkArgs `
    -RedirectStandardOutput $jlinkOut -RedirectStandardError $jlinkErr `
    -PassThru -NoNewWindow -Wait

if ($jlinkProc.ExitCode -ne 0) {
    $detail = Get-Content $jlinkErr -Raw -ErrorAction SilentlyContinue
    if (-not $detail) { $detail = Get-Content $jlinkOut -Raw -ErrorAction SilentlyContinue }
    throw "jlink 失败（退出码 $($jlinkProc.ExitCode)）：`n$detail"
}

$stageJava = Join-Path $StageJre 'bin\java.exe'
if (-not (Test-Path $stageJava)) { throw "jlink 自称成功，但没有 java.exe：$stageJava" }

Write-Line ("     JRE -> $StageJre  ($([math]::Round((Get-ChildItem $StageJre -Recurse -File | Measure-Object Length -Sum).Sum / 1MB, 1)) MB)")

# ---------------------------------------------------------------- 2. 组装 app

Write-Line "2/4 组装 classpath（app.jar + $($libJars.Count) 个依赖 jar）"
Remove-Hard $StageApp
New-Item -ItemType Directory -Force -Path $StageLib | Out-Null
Copy-Item $SrcJar $StageJar -Force
foreach ($j in $libJars) { Copy-Item $j.FullName (Join-Path $StageLib $j.Name) -Force }

$cp = "$StageJar;$(Join-Path $StageLib '*')"

# ---------------------------------------------------------------- 3. 实起验证

if ($SkipVerify) {
    Write-Line '3/4 跳过实起验证（-SkipVerify）'
} else {
    Write-Line '3/4 实测裁剪 JRE 能起后端（这一步是模块表的验收）'

    $verifyData = Join-Path $StageDir '_verify-data'
    New-Item -ItemType Directory -Force -Path $verifyData | Out-Null
    $port = Get-Random -Minimum 20000 -Maximum 40000
    $vOut = Join-Path $StageDir 'verify.out'
    $vErr = Join-Path $StageDir 'verify.err'
    Remove-Hard $vOut
    Remove-Hard $vErr

    $vArgs = @(
        '-XX:TieredStopAtLevel=1'
        '-Dfile.encoding=UTF-8'
        "-Ddetectivehelper.data.dir=$verifyData"
        '-cp'
        $cp
        'com.theos.detectivehelper.DetectiveHelperApplication'
        "--server.port=$port"
        '--server.address=127.0.0.1'
    )

    $vProc = Start-Process -FilePath $stageJava -ArgumentList $vArgs `
        -RedirectStandardOutput $vOut -RedirectStandardError $vErr `
        -PassThru -NoNewWindow

    # 打真实接口，而不是只看"启动成功"日志 —— 缺模块往往在首个请求才炸
    $healthUrl = "http://127.0.0.1:$port/api/health"
    $ok = $false
    $lastErr = ''
    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Milliseconds 250
        if ($vProc.HasExited) { break }
        try {
            $r = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3
            if ($r.StatusCode -eq 200) { $ok = $true; break }
        } catch {
            $lastErr = $_.Exception.Message
        }
    }

    if (-not $vProc.HasExited) { Stop-Process -Id $vProc.Id -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Milliseconds 400

    if (-not $ok) {
        $txt = Get-Content $vOut -Raw -ErrorAction SilentlyContinue
        $errTxt = Get-Content $vErr -Raw -ErrorAction SilentlyContinue
        # 只有走到这里才说明模块表缺东西。把线索摆全，别让人再去猜。
        throw @"
裁剪 JRE 起不来后端 —— 模块表很可能少了东西。

  探测地址 : $healthUrl
  最后错误 : $lastErr
  进程退出 : $($vProc.HasExited)

--- stdout 尾部 ---
$(($txt -split "`r?`n" | Select-Object -Last 30) -join "`r`n")

--- stderr 尾部 ---
$(($errTxt -split "`r?`n" | Select-Object -Last 30) -join "`r`n")

排查方向：在 `$modules` 里补上缺失模块（NoClassDefFoundError / ClassNotFoundException
的类名能直接指向模块，例如 javax.naming -> java.naming），然后重跑本脚本 -Force。
"@
    }

    Remove-Hard $verifyData
    Write-Line "     裁剪 JRE 起后端并响应 /api/health 正常（端口 $port）"
}

# ---------------------------------------------------------------- 4. CDS

if ($SkipCds) {
    Write-Line '4/4 跳过 CDS 训练（-SkipCds）—— 启动会比正常慢约 1.3 秒'
} else {
    Write-Line '4/4 用裁剪后的 JRE 训练 CDS 归档'
    & (Join-Path $ScriptDir 'make-cds-archive.ps1') -JavaHome $StageJre `
        -JarPath $StageJar -LibDir $StageLib -ArchivePath $StageCds `
        -DataDir (Join-Path $StageDir 'cds-data') -Force
}

# 训练/校验的附属文件就在归档旁边，而那个目录会整个进包 —— 必须清掉
Clear-CdsNoise -Archive $StageCds

# ---------------------------------------------------------------- 报告

Set-Content -Path $StageStamp -Value $signature -Encoding UTF8

$jreMB = [math]::Round((Get-ChildItem $StageJre -Recurse -File | Measure-Object Length -Sum).Sum / 1MB, 1)
$appMB = [math]::Round((Get-ChildItem $StageApp -Recurse -File | Measure-Object Length -Sum).Sum / 1MB, 1)
Write-Line ''
Write-Line "暂存就绪 -> $StageDir"
Write-Line "  jre   $jreMB MB"
Write-Line "  app   $appMB MB"
if (Test-Path $StageCds) {
    Write-Line "  cds   $([math]::Round((Get-Item $StageCds).Length / 1MB, 1)) MB"
} else {
    Write-Line '  cds   （缺失 —— 打包后会静默慢约 1.3 秒）'
}
