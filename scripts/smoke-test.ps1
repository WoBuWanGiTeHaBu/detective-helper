<#
.SYNOPSIS
  Detective Helper 的两阶段冒烟测试。

.DESCRIPTION
  打包产物"能生成"和"能跑"是两件事。这个脚本回答第二个问题。

  ⚠️ 当前状态：本脚本仍按旧 JavaFX/jpackage 的 app-image 布局找 JRE
     （runtime\bin\java.exe）与 launcher exe。Electron 包的结构是
     resources\{jre,app}，直接跑会找不到路径 —— 用之前先适配这一层。
     43 项接口断言本身仍然有效，改的只是"怎么把后端单独拉起来"。

  ── 阶段一：无界面，全接口 ────────────────────────────────────────────
  拿 app-image 里**裁剪过的 JRE**（runtime\bin\java.exe）去跑内嵌后端，逐个打真实接口。
  用裁剪 JRE 而不是 JDK 是刻意的 —— 这样顺带验证了 jlink 的模块表够用，
  模块少一个就会在这里炸出来，而不是等用户双击时才发现。

  后端怎么单独跑起来：fat jar 的 Start-Class 是 DesktopApplication（JavaFX），
  直接 java -jar 会弹窗口。所以要换成 PropertiesLauncher + -Dloader.main 指到
  后端主类，绕过 JavaFX 入口。

  覆盖：健康检查 / 资料 upsert / 案件书 CRUD / 事件 CRUD / 页面 CRUD /
        画布读写回环（真正验证持久化，不只是 200）/ 工作空间聚合 /
        SPA 托管与回退语义 / api 与静态资源的 404 边界。

  ── 阶段二：真实窗口 ─────────────────────────────────────────────────
  启动 app-image 的 launcher exe，然后：
    · 从 data\logs\app.log 里等"后端服务已就绪"，并取出它随机分配的端口
    · 用那个端口确认 /api/health 与 /（index.html）都能应答
    · 确认窗口真的出现了（MainWindowTitle 非空）
  最后把进程收干净。

.PARAMETER AppImageDir
  app-image 目录。默认 build\release\Detective Helper。

.PARAMETER DataDir
  临时数据目录。默认 build\smoke\data（每次跑都清空重建）。
  这个参数存在的意义是：测试绝不能碰用户的真实案件库。

.PARAMETER Port
  阶段一用的端口。默认自动挑一个空闲回环端口。

.PARAMETER SkipPhase1 / -SkipPhase2
  只跑其中一阶段。

.PARAMETER KeepData
  保留临时数据目录，便于事后翻日志。

.PARAMETER ReportPath
  报告输出位置，默认 build\smoke-report.txt。

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\smoke-test.ps1
#>

[CmdletBinding()]
param(
    [string]$AppImageDir,
    [string]$DataDir,
    [int]$Port = 0,
    [switch]$SkipPhase1,
    [switch]$SkipPhase2,
    [switch]$KeepData,
    [string]$ReportPath,
    [int]$StartupTimeoutSeconds = 120
)

$ScriptDir = $PSScriptRoot
if (-not $ScriptDir) {
    $ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
}

. (Join-Path $ScriptDir '_common.ps1')

$ProjectRoot = Get-DhaProjectRoot -ScriptDir $ScriptDir
$BuildDir    = Join-Path $ProjectRoot 'build'
$AppName     = 'Detective Helper'

if (-not $AppImageDir) { $AppImageDir = Join-Path $BuildDir ('release\' + $AppName) }
if (-not $DataDir)     { $DataDir     = Join-Path $BuildDir 'smoke\data' }
if (-not $ReportPath)  { $ReportPath  = Join-Path $BuildDir 'smoke-report.txt' }

$LauncherExe = Join-Path $AppImageDir ($AppName + '.exe')
$RuntimeJava = Join-Path $AppImageDir 'runtime\bin\java.exe'
$FatJar      = Join-Path $AppImageDir ('app\DetectiveHelper-desktop.jar')

# ---------------------------------------------------------------- 结果收集

$script:Results = New-Object System.Collections.ArrayList
$script:Section = '（未命名）'

function Add-Result {
    param(
        [string]$Name,
        [bool]$Pass,
        [string]$Detail = ''
    )
    [void]$script:Results.Add([PSCustomObject]@{
        Section = $script:Section
        Name    = $Name
        Pass    = $Pass
        Detail  = $Detail
    })
    if ($Pass) {
        Write-Host ("  [OK]   {0}" -f $Name) -ForegroundColor Green
    } else {
        Write-Host ("  [FAIL] {0}  -- {1}" -f $Name, $Detail) -ForegroundColor Red
    }
}

function Start-Section {
    param([string]$Name)
    $script:Section = $Name
    Write-Host ''
    Write-Host ("── {0}" -f $Name) -ForegroundColor Cyan
}

# ---------------------------------------------------------------- HTTP 帮手

<#
  为什么要自己包一层：PowerShell 5.1 的 Invoke-RestMethod 在响应头没写 charset 时
  会拿 Latin-1 解 UTF-8，中文全变问号；而 Invoke-WebRequest 遇到 4xx/5xx 直接抛异常，
  拿不到 body 也就没法断言 404 语义。这里统一用 -UseBasicParsing + 自己按 UTF-8 解码。
#>
function Invoke-Probe {
    param(
        [string]$Method = 'GET',
        [string]$Url,
        $Body,
        [int]$TimeoutSec = 30
    )

    $params = @{
        Uri             = $Url
        Method          = $Method
        UseBasicParsing = $true
        TimeoutSec      = $TimeoutSec
        Headers         = @{ 'Accept' = 'application/json, text/html' }
    }

    if ($null -ne $Body) {
        if ($Body -is [string]) {
            $json = $Body
        } else {
            $json = $Body | ConvertTo-Json -Depth 30 -Compress
        }
        # 传字节而不是字符串：彻底绕开 PS 5.1 在 Body 编码上的不确定性
        $params.Body = [System.Text.Encoding]::UTF8.GetBytes($json)
        $params.ContentType = 'application/json; charset=utf-8'
    }

    $status = 0
    $text = ''
    $errorMessage = ''

    try {
        $resp = Invoke-WebRequest @params
        $status = [int]$resp.StatusCode
        $text = [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
    } catch {
        $webResp = $_.Exception.Response
        if ($null -eq $webResp) {
            $errorMessage = $_.Exception.Message
        } else {
            $status = [int]$webResp.StatusCode
            try {
                $stream = $webResp.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
                $text = $reader.ReadToEnd()
                $reader.Dispose()
                $stream.Dispose()
            } catch {
                $text = ''
            }
        }
    }

    $parsed = $null
    if ($text -and ($text.TrimStart().StartsWith('{'))) {
        try { $parsed = $text | ConvertFrom-Json } catch { $parsed = $null }
    }

    return [PSCustomObject]@{
        Status       = $status
        Text         = $text
        Json         = $parsed
        ErrorMessage = $errorMessage
    }
}

function Wait-Health {
    param(
        [string]$BaseUrl,
        [int]$TimeoutSeconds = 120
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $probe = Invoke-Probe -Url "$BaseUrl/api/health" -TimeoutSec 5
        if ($probe.Status -eq 200) { return $true }
        Start-Sleep -Milliseconds 300
    }
    return $false
}

# ---------------------------------------------------------------- 准备工作

Write-DhaStep '冒烟测试 · 准备'

# 宿主环境块里有 "HTTPS_PROXY"/"https_proxy" 这类只有大小写不同的重复项，
# 不清掉的话 Start-Process 在参数绑定阶段就抛"字典中的关键字已添加"，脚本直接死。
$dupFixed = Remove-DhaDuplicateEnvKeys
if ($dupFixed -gt 0) {
    Write-DhaInfo "环境       : 清理了 $dupFixed 组大小写重复变量（Start-Process 的已知坑）"
}

if (-not (Test-Path $LauncherExe)) {
    throw "找不到启动器：$LauncherExe（先跑 scripts\build-release.ps1）"
}
if (-not (Test-Path $FatJar)) {
    throw "找不到 fat jar：$FatJar"
}

$useTrimmedRuntime = Test-Path $RuntimeJava
$javaExe = $null
$injectedRuntimeLauncher = $false

function Get-DhaJavaVersionFromRelease {
    param([string]$ReleaseFile)
    if (-not (Test-Path $ReleaseFile)) { return '' }
    $m = [regex]::Match((Get-Content $ReleaseFile -Raw -ErrorAction SilentlyContinue), 'JAVA_VERSION="([^"]+)"')
    if ($m.Success) { return $m.Groups[1].Value }
    return ''
}

if ($useTrimmedRuntime) {
    $javaExe = $RuntimeJava
    Write-DhaInfo "JRE        : $RuntimeJava（app-image 自带，顺便验证裁剪后的模块表）"
} else {
    <#
      jpackage 的 runtime 是 jlink 的「运行时镜像」，**按设计不含 java.exe / javaw.exe**：
      bin 下只有 jli.dll / server\jvm.dll，启动器直接吃这些。
      所以想用这份裁剪过的 JRE 跑后端，得先把同版本的 java.exe 临时放回去。

      版本必须严格一致 —— java.exe 是 jli.dll 的薄壳，版本错配会直接崩。
      对不上就老老实实用 JDK 跑，把「模块表够不够」交给阶段二覆盖验证。
    #>
    $javaHome = Resolve-DhaJavaHome
    $jdkJava = Join-Path $javaHome 'bin\java.exe'

    $runtimeVersion = Get-DhaJavaVersionFromRelease (Join-Path $AppImageDir 'runtime\release')
    $jdkVersion = Get-DhaJavaVersionFromRelease (Join-Path $javaHome 'release')

    if ($runtimeVersion -and $jdkVersion -and ($runtimeVersion -eq $jdkVersion)) {
        Copy-Item (Join-Path $javaHome 'bin\java.exe') $RuntimeJava -Force
        $injectedRuntimeLauncher = $true
        $javaExe = $RuntimeJava
        Write-DhaInfo "JRE        : $RuntimeJava（临时放回同版本启动器 $runtimeVersion，用来验证裁剪模块表）"
    } else {
        $javaExe = $jdkJava
        Write-DhaWarn "runtime 版本($runtimeVersion) 与 JDK($jdkVersion) 不一致，阶段一改用 JDK 跑"
        Write-DhaWarn "裁剪模块表是否够用，由阶段二（真正启动 app-image）覆盖验证"
    }
}

if ($Port -le 0) {
    $listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $Port = ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
    $listener.Stop()
}
$baseUrl = "http://127.0.0.1:$Port"
Write-DhaInfo "测试端口   : $Port"

# 数据目录：每次全新，绝不碰用户的真实库
Remove-DhaPath -Path $DataDir -Quiet | Out-Null
New-Item -ItemType Directory -Force -Path $DataDir | Out-Null
Write-DhaInfo "临时数据   : $DataDir"

$startedAt = Get-Date
$failedCount = 0

# ================================================================ 阶段一

if ($SkipPhase1) {
    Write-DhaStep '跳过阶段一（-SkipPhase1）'
} else {
    Write-DhaStep '阶段一 · 无界面跑全接口'

    $payload = @(
        '-cp', $FatJar
        "-Dloader.main=com.theos.detectivehelper.DetectiveHelperApplication"
        "-Ddetectivehelper.data.dir=$DataDir"
        '-Dspring.main.banner-mode=off'
        'org.springframework.boot.loader.launch.PropertiesLauncher'
        "--server.port=$Port"
        '--server.address=127.0.0.1'
        '--logging.level.root=WARN'
        '--logging.level.com.theos.detectivehelper=INFO'
    )

    Write-DhaInfo ("java " + ($payload -join ' '))

    # Start-Process 的 -ArgumentList 传数组时，PS 5.1 只用空格拼接、不补引号。
    # fat jar 的路径里有空格（"Detective Helper" 目录），不自己加引号的话
    # -cp 会被截断成两段参数，JVM 直接报"找不到主类"。
    $quotedArgs = @($payload | ForEach-Object {
        if ($_ -match '\s') { '"' + $_ + '"' } else { $_ }
    })
    $argLine = $quotedArgs -join ' '

    $stdoutLog = Join-Path $BuildDir 'smoke\phase1-stdout.log'
    $stderrLog = Join-Path $BuildDir 'smoke\phase1-stderr.log'
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $stdoutLog) | Out-Null

    $proc = $null
    try {
        # Start-Process 必须待在 try 里面：它一旦抛错，finally 里的
        # "撤掉临时启动器" 才会执行，否则 app-image 里会残留一个 java.exe，
        # 下次打包就被当产物一起塞进 zip。
        $proc = Start-Process -FilePath $javaExe -ArgumentList $argLine -PassThru -NoNewWindow `
            -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog

        if (-not (Wait-Health -BaseUrl $baseUrl -TimeoutSeconds $StartupTimeoutSeconds)) {
            Write-DhaFail "后端在 $StartupTimeoutSeconds 秒内没有应答 /api/health"
            if (Test-Path $stdoutLog) {
                Write-DhaInfo '--- 标准输出尾部 ---'
                Get-Content $stdoutLog -Tail 40 -Encoding UTF8 | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
            }
            if (Test-Path $stderrLog) {
                Write-DhaInfo '--- 标准错误尾部 ---'
                Get-Content $stderrLog -Tail 40 -Encoding UTF8 | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
            }
            Add-Result -Name '后端启动' -Pass $false -Detail '健康检查超时'
            throw '阶段一失败：后端没起来'
        }
        Add-Result -Name '后端启动成功' -Pass $true -Detail $baseUrl

        # ---------------- 健康检查
        Start-Section '健康检查'
        $r = Invoke-Probe -Url "$baseUrl/api/health"
        Add-Result -Name 'GET /api/health → 200' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"
        if ($r.Json) {
            Add-Result -Name 'health.status = UP' -Pass ($r.Json.data.status -eq 'UP') -Detail "实际 $($r.Json.data.status)"
            Add-Result -Name 'health.database = UP（真实查了一次库）' -Pass ($r.Json.data.database -eq 'UP') -Detail "实际 $($r.Json.data.database)"
            $healthDataDir = [string]$r.Json.data.dataDir
            Add-Result -Name 'health.dataDir 指向临时目录' -Pass ($healthDataDir -like "$DataDir*") -Detail "实际 $healthDataDir"
        }

        $dbFile = Join-Path $DataDir 'detective-helper.db'
        Add-Result -Name 'SQLite 库文件已生成' -Pass (Test-Path $dbFile) -Detail $dbFile

        $missingSubDirs = @(
            @('covers', 'uploads', 'files', 'logs', 'backups') |
                Where-Object { -not (Test-Path (Join-Path $DataDir $_)) }
        )
        $subDirDetail = if ($missingSubDirs.Count -eq 0) { '全部就位' } else { '缺: ' + ($missingSubDirs -join ',') }
        Add-Result -Name 'data 子目录齐备（covers/uploads/files/logs/backups）' `
            -Pass ($missingSubDirs.Count -eq 0) -Detail $subDirDetail

        # ---------------- 用户资料 upsert
        Start-Section '用户资料'
        $r = Invoke-Probe -Url "$baseUrl/api/profile"
        Add-Result -Name 'GET /api/profile → 200（无记录时给默认值）' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"

        $r = Invoke-Probe -Method PUT -Url "$baseUrl/api/profile" -Body @{ displayName = '冒烟测试员' }
        Add-Result -Name 'PUT /api/profile → 200' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"

        $r = Invoke-Probe -Url "$baseUrl/api/profile"
        $nameOk = $r.Json -and ($r.Json.data.displayName -eq '冒烟测试员')
        Add-Result -Name '资料回读一致（中文未乱码）' -Pass $nameOk -Detail "实际 '$($r.Json.data.displayName)'"

        # ---------------- 案件书 CRUD
        Start-Section '案件书'
        $r = Invoke-Probe -Method POST -Url "$baseUrl/api/books" -Body @{
            name = '冒烟案件·雾山别墅'; coverType = 'text'; coverText = '雾山'
        }
        Add-Result -Name 'POST /api/books → 200' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"
        $bookId = $null
        if ($r.Json) { $bookId = $r.Json.data.id }
        Add-Result -Name '返回了 bookId' -Pass ($null -ne $bookId) -Detail "bookId=$bookId"

        if ($null -ne $bookId) {
            $r = Invoke-Probe -Url "$baseUrl/api/books/$bookId"
            Add-Result -Name 'GET /api/books/{id} 名称一致' -Pass ($r.Json -and $r.Json.data.name -eq '冒烟案件·雾山别墅') -Detail "实际 '$($r.Json.data.name)'"

            $r = Invoke-Probe -Url "$baseUrl/api/books"
            $listHasBook = $false
            if ($r.Json -and $r.Json.data) {
                $listHasBook = (@($r.Json.data | Where-Object { $_.id -eq $bookId }).Count -ge 1)
            }
            Add-Result -Name 'GET /api/books 列表含新建案件' -Pass $listHasBook

            # ---------------- 事件 CRUD
            Start-Section '事件'
            $r = Invoke-Probe -Method POST -Url "$baseUrl/api/books/$bookId/events" -Body @{ name = '第一幕·雨夜' }
            Add-Result -Name 'POST /api/books/{id}/events → 200' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"
            $eventId = $null
            if ($r.Json) { $eventId = $r.Json.data.id }
            Add-Result -Name '返回了 eventId' -Pass ($null -ne $eventId) -Detail "eventId=$eventId"

            if ($null -ne $eventId) {
                $r = Invoke-Probe -Url "$baseUrl/api/books/$bookId/events"
                $listHasEvent = $false
                if ($r.Json -and $r.Json.data) {
                    $listHasEvent = (@($r.Json.data | Where-Object { $_.id -eq $eventId }).Count -ge 1)
                }
                Add-Result -Name 'GET 事件列表含新事件' -Pass $listHasEvent

                # ---------------- 页面 CRUD
                Start-Section '页面'
                $r = Invoke-Probe -Method POST -Url "$baseUrl/api/events/$eventId/pages" -Body @{ name = '案发现场' }
                Add-Result -Name 'POST /api/events/{id}/pages → 200' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"
                $pageId = $null
                if ($r.Json) { $pageId = $r.Json.data.id }
                Add-Result -Name '返回了 pageId' -Pass ($null -ne $pageId) -Detail "pageId=$pageId"

                if ($null -ne $pageId) {
                    # ---------------- 画布读写回环（真正验证持久化）
                    Start-Section '画布读写回环'

                    $r = Invoke-Probe -Url "$baseUrl/api/pages/$pageId/canvas"
                    $canvasOk = $false
                    if ($r.Json -and $r.Json.data) {
                        $d = $r.Json.data
                        $canvasOk = ($null -ne $d.objects) -and ($null -ne $d.relationships) -and
                                    ($null -ne $d.annotations) -and ($null -ne $d.timelines)
                    }
                    Add-Result -Name 'GET 空画布四个数组均非 null（前端不用判空）' -Pass $canvasOk

                    $canvasBody = @{
                        objects = @(
                            @{
                                id = 'obj-smoke-1'
                                type = 'note'
                                x = 111.5
                                y = 222.5
                                width = 200
                                height = 120
                                text = '冒烟对象·中文也要能存'
                            }
                        )
                        relationships = @()
                        annotations = @()
                        timelines = @()
                    }
                    $r = Invoke-Probe -Method PUT -Url "$baseUrl/api/pages/$pageId/canvas" -Body $canvasBody
                    Add-Result -Name 'PUT 画布 → 200' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"

                    $r = Invoke-Probe -Url "$baseUrl/api/pages/$pageId/canvas"
                    $roundTripOk = $false
                    $objCount = -1
                    if ($r.Json -and $r.Json.data -and $r.Json.data.objects) {
                        $objCount = @($r.Json.data.objects).Count
                        $first = @($r.Json.data.objects)[0]
                        $roundTripOk = ($objCount -eq 1) -and ($first.id -eq 'obj-smoke-1') -and ($first.text -eq '冒烟对象·中文也要能存')
                    }
                    Add-Result -Name '画布回读：对象数量=1 且内容/中文一致' -Pass $roundTripOk -Detail "实际对象数=$objCount"

                    # 清空验证：传 [] 应当真的清掉，而不是被合并
                    $r = Invoke-Probe -Method PUT -Url "$baseUrl/api/pages/$pageId/canvas" -Body @{
                        objects = @(); relationships = @(); annotations = @(); timelines = @()
                    }
                    $r = Invoke-Probe -Url "$baseUrl/api/pages/$pageId/canvas"
                    $clearedOk = $r.Json -and (@($r.Json.data.objects).Count -eq 0)
                    Add-Result -Name '画布传空数组即清空（不是合并语义）' -Pass $clearedOk

                    # ---------------- 工作空间聚合
                    Start-Section '工作空间聚合'
                    $r = Invoke-Probe -Url "$baseUrl/api/books/$bookId/workspace"
                    Add-Result -Name 'GET /api/books/{id}/workspace → 200' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"
                    $wsOk = $false
                    if ($r.Json -and $r.Json.data) {
                        $wsOk = ($null -ne $r.Json.data.book) -and ($null -ne $r.Json.data.events)
                    }
                    Add-Result -Name 'workspace 同时带 book 与 events' -Pass $wsOk

                    # ---------------- SPA 托管与回退
                    Start-Section 'SPA 托管与回退语义'
                    $r = Invoke-Probe -Url "$baseUrl/"
                    $indexOk = ($r.Status -eq 200) -and ($r.Text -match '<div id="app">')
                    Add-Result -Name 'GET / 返回 index.html（含 #app 挂载点）' -Pass $indexOk -Detail "实际 $($r.Status)"

                    $r = Invoke-Probe -Url "$baseUrl/bookshelf/whatever/deep/route"
                    $fallbackOk = ($r.Status -eq 200) -and ($r.Text -match '<div id="app">')
                    Add-Result -Name '未知前端路由回退到 index.html（刷新不 404）' -Pass $fallbackOk -Detail "实际 $($r.Status)"

                    $r = Invoke-Probe -Url "$baseUrl/api/definitely-not-a-route"
                    Add-Result -Name '/api/** 未知路由 → 404（不回退成 HTML）' -Pass ($r.Status -eq 404) -Detail "实际 $($r.Status)"

                    $r = Invoke-Probe -Url "$baseUrl/assets/definitely-missing.js"
                    Add-Result -Name '带扩展名的静态资源缺失 → 404（不回退）' -Pass ($r.Status -eq 404) -Detail "实际 $($r.Status)"

                    $r = Invoke-Probe -Url "$baseUrl/favicon.svg"
                    Add-Result -Name '/favicon.svg 可访问' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"

                    # ---------------- 级联删除
                    Start-Section '级联删除'
                    $r = Invoke-Probe -Method DELETE -Url "$baseUrl/api/pages/$pageId"
                    Add-Result -Name 'DELETE 页面 → 200' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"
                    $r = Invoke-Probe -Url "$baseUrl/api/pages/$pageId"
                    Add-Result -Name '删除后查页面 → 业务错误（非 200）' -Pass ($r.Json -and $r.Json.code -ne 200) -Detail "HTTP $($r.Status) code=$($r.Json.code)"

                    $r = Invoke-Probe -Method DELETE -Url "$baseUrl/api/events/$eventId"
                    Add-Result -Name 'DELETE 事件 → 200' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"
                }
            }

            $r = Invoke-Probe -Method DELETE -Url "$baseUrl/api/books/$bookId"
            Add-Result -Name 'DELETE 案件书 → 200' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"
        }

        # ---------------- 参数校验
        Start-Section '参数校验'
        $r = Invoke-Probe -Method POST -Url "$baseUrl/api/books" -Body @{ name = '' }
        Add-Result -Name '空名称创建案件 → 被校验拦下（非 200）' -Pass ($r.Json -and $r.Json.code -ne 200) -Detail "HTTP $($r.Status) code=$($r.Json.code)"

    } finally {
        if ($proc -and -not $proc.HasExited) {
            Write-DhaInfo "停止阶段一进程（PID $($proc.Id)）"
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            Start-Sleep -Milliseconds 800
        }

        # 把临时放回的启动器撤掉，让 app-image 回到出厂的"无 java.exe"状态
        if ($injectedRuntimeLauncher -and (Test-Path $RuntimeJava)) {
            Remove-DhaPath -Path $RuntimeJava -Quiet | Out-Null
            Write-DhaInfo '已撤掉临时启动器，app-image 恢复出厂状态'
        }
    }
}

# ================================================================ 阶段二

if ($SkipPhase2) {
    Write-DhaStep '跳过阶段二（-SkipPhase2）'
} else {
    Write-DhaStep '阶段二 · 真实窗口'

    # 阶段二用 app-image 自己的 data 目录，且同样先清空 —— 不能污染用户案件
    $appDataDir = Join-Path $AppImageDir 'data'
    Remove-DhaPath -Path $appDataDir -Quiet | Out-Null

    $before = @(Get-Process -ErrorAction SilentlyContinue | Where-Object {
        $_.Path -and $_.Path.StartsWith($AppImageDir, [System.StringComparison]::OrdinalIgnoreCase)
    } | Select-Object -ExpandProperty Id)

    Write-DhaInfo "启动：$LauncherExe"
    $launcher = Start-Process -FilePath $LauncherExe -WorkingDirectory $AppImageDir -PassThru

    $appProcIds = @()
    $readyPort = 0

    try {
        # 等日志里出现"后端服务已就绪"，同时把端口抠出来
        $logFile = Join-Path $appDataDir 'logs\app.log'
        $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)

        while ((Get-Date) -lt $deadline) {
            if (Test-Path $logFile) {
                $content = Get-Content $logFile -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
                if ($content) {
                    $m = [regex]::Match($content, '后端服务已就绪:\s*http://127\.0\.0\.1:(\d+)')
                    if ($m.Success) {
                        $readyPort = [int]$m.Groups[1].Value
                        break
                    }
                }
            }
            Start-Sleep -Milliseconds 500
        }

        Add-Result -Name '日志出现"后端服务已就绪"' -Pass ($readyPort -gt 0) -Detail "端口 $readyPort"

        # 再等一条 WebView 的日志：这条能证明页面真的渲染出来了，
        # 而不是"窗口开了但 WebView 一片空白"。缺 jdk.jsobject / jdk.xml.dom 时就会卡在这里。
        $renderedOk = $false
        $renderDeadline = (Get-Date).AddSeconds(60)
        while ((Get-Date) -lt $renderDeadline) {
            if (Test-Path $logFile) {
                $content = Get-Content $logFile -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
                if ($content -and ($content -match '前端页面已渲染')) { $renderedOk = $true; break }
                if ($content -and ($content -match '前端页面渲染失败|页面加载失败')) { break }
            }
            Start-Sleep -Milliseconds 500
        }
        Add-Result -Name 'WebView 日志确认前端页面已渲染（模块表够用）' -Pass $renderedOk

        if ($readyPort -gt 0) {
            $appBaseUrl = "http://127.0.0.1:$readyPort"
            $r = Invoke-Probe -Url "$appBaseUrl/api/health"
            Add-Result -Name '打包后的随机端口能应答 /api/health' -Pass ($r.Status -eq 200) -Detail "实际 $($r.Status)"

            $r = Invoke-Probe -Url "$appBaseUrl/"
            Add-Result -Name 'WebView 目标地址返回 index.html' -Pass (($r.Status -eq 200) -and ($r.Text -match '<div id="app">')) -Detail "实际 $($r.Status)"

            $healthDataDir = ''
            $r = Invoke-Probe -Url "$appBaseUrl/api/health"
            if ($r.Json) { $healthDataDir = [string]$r.Json.data.dataDir }
            Add-Result -Name 'data 落在安装目录下（便携策略生效）' -Pass ($healthDataDir -like "$appDataDir*") -Detail "实际 $healthDataDir"
        }

        # 窗口真的出来了吗
        Start-Sleep -Seconds 2
        $appProcs = @(Get-Process -ErrorAction SilentlyContinue | Where-Object {
            $_.Path -and $_.Path.StartsWith($AppImageDir, [System.StringComparison]::OrdinalIgnoreCase)
        })
        $appProcIds = @($appProcs | Select-Object -ExpandProperty Id)

        Add-Result -Name 'app-image 里有存活的进程' -Pass ($appProcs.Count -gt 0) -Detail "进程数 $($appProcs.Count)"

        $titled = @($appProcs | Where-Object { $_.MainWindowTitle -and $_.MainWindowTitle.Length -gt 0 })
        $titleText = ''
        if ($titled.Count -gt 0) { $titleText = $titled[0].MainWindowTitle }
        Add-Result -Name 'JavaFX 窗口已显示（窗口标题非空）' -Pass ($titled.Count -gt 0) -Detail "标题 '$titleText'"

        # 崩溃文件不该出现
        $crashLog = Join-Path $appDataDir 'logs\crash.log'
        Add-Result -Name '没有产生 crash.log' -Pass (-not (Test-Path $crashLog)) -Detail $crashLog

    } finally {
        $toKill = @($appProcIds)
        if ($launcher -and -not $launcher.HasExited) { $toKill += $launcher.Id }
        $toKill = $toKill | Sort-Object -Unique
        foreach ($id in $toKill) {
            Write-DhaInfo "停止进程 PID $id"
            Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Milliseconds 1200
    }
}

# ---------------------------------------------------------------- 收尾与报告

$failed = @($script:Results | Where-Object { -not $_.Pass })
$failedCount = $failed.Count
$total = $script:Results.Count

if (-not $KeepData) {
    # 阶段一的临时数据清理；阶段二的 data 留在 app-image 里也顺手清掉，
    # 免得下次打包把它当"产物"一起塞进 zip
    foreach ($dir in @($DataDir, (Join-Path $AppImageDir 'data'))) {
        Remove-DhaPath -Path $dir -Quiet | Out-Null
    }
}

$lines = New-Object System.Collections.ArrayList
[void]$lines.Add('Detective Helper 冒烟测试报告')
[void]$lines.Add('=' * 60)
[void]$lines.Add("时间        : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')")
[void]$lines.Add("app-image   : $AppImageDir")
[void]$lines.Add("使用 JRE    : $javaExe")
[void]$lines.Add("结果        : $($total - $failedCount) / $total 通过")
[void]$lines.Add('')

$lastSection = ''
foreach ($res in $script:Results) {
    if ($res.Section -ne $lastSection) {
        $lastSection = $res.Section
        [void]$lines.Add('')
        [void]$lines.Add("── $lastSection")
    }
    $mark = if ($res.Pass) { '[OK]  ' } else { '[FAIL]' }
    $detail = if ($res.Detail) { "  ($($res.Detail))" } else { '' }
    [void]$lines.Add("$mark $($res.Name)$detail")
}

if ($failedCount -gt 0) {
    [void]$lines.Add('')
    [void]$lines.Add('失败项：')
    foreach ($f in $failed) {
        [void]$lines.Add("  - $($f.Section) / $($f.Name) -- $($f.Detail)")
    }
}

Set-Content -Path $ReportPath -Value $lines -Encoding UTF8

Write-Host ''
Write-Host ('=' * 60) -ForegroundColor Cyan
if ($failedCount -eq 0) {
    Write-Host ("全部通过：$total / $total") -ForegroundColor Green
} else {
    Write-Host ("通过 $($total - $failedCount) / $total —— 失败 $failedCount 项") -ForegroundColor Red
}
Write-Host "报告：$ReportPath" -ForegroundColor Gray

if ($failedCount -gt 0) { exit 1 }
exit 0
