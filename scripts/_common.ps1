# 公共辅助函数 —— 被 scripts 下的其它脚本 dot-source 引入。
# 约定：本文件里的函数名统一带 Dha 前缀（Detective Helper App），避免和内置 cmdlet 撞名。
#
# 注意：PowerShell 5.1 对含中文的 .ps1 要求 UTF-8 with BOM，
# 否则解析会报「字符串缺少终止符」之类的错。仓库里的 .ps1 都已按此保存。

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

function Get-DhaScriptDir {
    # param() 默认值里不要用 $PSScriptRoot，-File 启动时可能取不到
    $dir = $PSScriptRoot
    if (-not $dir) {
        $dir = Split-Path -Parent $MyInvocation.MyCommand.Path
    }
    return $dir
}

function Get-DhaProjectRoot {
    param([string]$ScriptDir)
    if (-not $ScriptDir) { $ScriptDir = Get-DhaScriptDir }
    $root = Split-Path -Parent $ScriptDir
    if (-not (Test-Path (Join-Path $root 'pom.xml'))) {
        throw "找不到项目根目录（$root 下没有 pom.xml）"
    }
    return $root
}

function Write-DhaStep {
    param([string]$Message)
    Write-Host ''
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-DhaInfo {
    param([string]$Message)
    Write-Host "    $Message" -ForegroundColor Gray
}

function Write-DhaOk {
    param([string]$Message)
    Write-Host "    [OK] $Message" -ForegroundColor Green
}

function Write-DhaWarn {
    param([string]$Message)
    Write-Host "    [!] $Message" -ForegroundColor Yellow
}

function Write-DhaFail {
    param([string]$Message)
    Write-Host "    [X] $Message" -ForegroundColor Red
}

function Resolve-DhaJavaHome {
    # 1) 已有 JAVA_HOME 且有效 → 直接用
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $candidate) { return $env:JAVA_HOME }
    }

    # 2) PATH 里的 java → 反推 JDK 目录
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $jdk = Split-Path -Parent (Split-Path -Parent $javaCmd.Source)
        if (Test-Path (Join-Path $jdk 'bin\jpackage.exe')) { return $jdk }
    }

    # 3) 常见安装位置
    foreach ($guess in @('D:\JAVA\jdk', 'C:\Program Files\Java\jdk-21', 'C:\Program Files\Eclipse Adoptium\jdk-21*')) {
        $expanded = Get-Item $guess -ErrorAction SilentlyContinue
        if ($expanded -and (Test-Path (Join-Path $expanded.FullName 'bin\jpackage.exe'))) {
            return $expanded.FullName
        }
    }

    throw '找不到可用的 JDK 21（需要 bin\jpackage.exe）。请设置 JAVA_HOME。'
}

function Resolve-DhaMavenCmd {
    param([string]$ProjectRoot)

    # 1) 仓库自带的 wrapper
    foreach ($name in @('mvnw.cmd', 'mvnw')) {
        $wrapper = Join-Path $ProjectRoot $name
        if (Test-Path $wrapper) { return $wrapper }
    }

    # 2) PATH 里的 mvn
    $mvnCmd = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if (-not $mvnCmd) { $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue }
    if ($mvnCmd) { return $mvnCmd.Source }

    # 3) ~/.m2/wrapper/dists 下已经解压好的发行版（本机就属于这种情况：
    #    仓库只有 Unix mvnw 且已被删除，PATH 里也没有 mvn）
    $dists = Join-Path $env:USERPROFILE '.m2\wrapper\dists'
    if (Test-Path $dists) {
        $candidates = Get-ChildItem -Path $dists -Recurse -Filter 'mvn.cmd' -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match '\\bin\\mvn\.cmd$' } |
            Sort-Object FullName -Descending
        if ($candidates.Count -gt 0) { return $candidates[0].FullName }
    }

    throw '找不到 Maven。请安装 Maven 并加入 PATH，或恢复仓库根目录的 mvnw.cmd。'
}

function Remove-DhaPath {
    <#
      删除文件/目录树的统一入口。

      为什么不直接 Remove-Item：宿主的 safe-delete 钩子会把删除改道到回收站，
      删成功之后仍然抛一个
        [safe-delete][SAFE_DELETE_FAIL_CLOSED] {"target":..., "reason":"trash-failed", "detail":"OK <path>"}
      —— 注意 detail 里是 "OK"，东西其实已经进回收站了，是钩子在误报。
      这个错误是 throw 出来的终止性错误，-ErrorAction 压不住，会把整个构建打断。

      而且这个搬运是**异步**的：Remove-Item 立刻返回并抛错，真正的搬完要等一会儿
      （实测 200 个文件、250MB 的 app-image 大约 3 秒）。所以不能只试一次就断言失败 ——
      必须轮询等路径真的消失，否则紧接着的 jpackage 会报「目标目录已存在」。

      这里不绕开回收站（那是宿主的安全策略，该尊重），只是把误报和异步性都消化掉。

      副作用要知道：app-image 两三百 MB，每次重建都会把上一份推进回收站，
      回收站会变大，隔一阵清一次。
    #>
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [switch]$Quiet,
        [int]$TimeoutSeconds = 30
    )

    if (-not (Test-Path -LiteralPath $Path)) { return $true }

    $isDirectory = (Get-Item -LiteralPath $Path -Force).PSIsContainer

    function Invoke-DhaDeleteOnce {
        try {
            if ($isDirectory) {
                Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction SilentlyContinue
            } else {
                Remove-Item -LiteralPath $Path -Force -ErrorAction SilentlyContinue
            }
        } catch {
            # safe-delete 钩子的误报，忽略；用轮询的实际结果判断
        }
    }

    Invoke-DhaDeleteOnce

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (-not (Test-Path -LiteralPath $Path)) { return $true }
        Start-Sleep -Milliseconds 400
    }

    # 等到超时还没消失，再补一刀（可能是第一次压根没触发钩子）
    Invoke-DhaDeleteOnce
    Start-Sleep -Milliseconds 1000

    if (-not (Test-Path -LiteralPath $Path)) { return $true }

    if (-not $Quiet) {
        Write-DhaWarn "删除超时（$TimeoutSeconds 秒）仍未生效：$Path"
    }
    return $false
}

function Invoke-DhaNative {
    <#
      跑原生程序的统一入口。

      为什么不能直接 `& cmd args`：本文件顶部把 $ErrorActionPreference 设成了 'Stop'，
      而 PowerShell 5.1 在「native 程序往 stderr 写东西 + 输出被重定向或进了管道」时，
      会把普通的 stderr 输出升级成终止性错误，抛 System.Management.Automation.RemoteException。
      Maven 打一行警告、jpackage 打一行进度、tar 打一行提示，都会让整个脚本莫名中断
      （而且异常 Message 还是空的，非常难查）。所以这里必须临时降级成 'Continue'。

      输出走函数自己的成功流，调用方 `$x = Invoke-DhaNative ...` 就能接住；
      退出码不通过返回值传 —— 调用方直接读 $LASTEXITCODE（native 调用会写它，不受函数边界影响）。
    #>
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [string[]]$Arguments = @(),
        [string]$WorkingDirectory
    )

    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        if ($WorkingDirectory) { Push-Location $WorkingDirectory }
        try {
            & $FilePath @Arguments
        } finally {
            if ($WorkingDirectory) { Pop-Location }
        }
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Invoke-DhaMaven {
    # 统一起见，所有 maven 调用都经过这里：显式喂 JAVA_HOME，避免 mvn.cmd 自己去猜
    param(
        [string]$MavenCmd,
        [string]$JavaHome,
        [string]$WorkingDirectory,
        [string[]]$MavenArgs
    )

    $previousJavaHome = $env:JAVA_HOME
    $env:JAVA_HOME = $JavaHome
    try {
        Invoke-DhaNative -FilePath $MavenCmd -Arguments $MavenArgs -WorkingDirectory $WorkingDirectory
        $code = $LASTEXITCODE
    } finally {
        $env:JAVA_HOME = $previousJavaHome
    }

    if ($code -ne 0) {
        throw "Maven 执行失败（退出码 $code）：$($MavenArgs -join ' ')"
    }
}

function Get-DhaAppVersion {
    param([string]$ProjectRoot)
    # 从根 pom 的 <version> 里取，避免脚本里再写一份版本号
    $pom = Get-Content (Join-Path $ProjectRoot 'pom.xml') -Raw -Encoding UTF8
    $m = [regex]::Match($pom, '(?s)<artifactId>detective-helper</artifactId>\s*<version>([^<]+)</version>')
    if ($m.Success) { return $m.Groups[1].Value.Trim() }
    return '1.0.0'
}

function Test-DhaJarEntry {
    # 用 JDK 自带的 jar 工具检查条目；比在 PowerShell 里解 zip 更直观
    param(
        [string]$JarPath,
        [string]$EntryPattern,
        [string]$JavaHome
    )
    $jarExe = Join-Path $JavaHome 'bin\jar.exe'
    $listing = Invoke-DhaNative -FilePath $jarExe -Arguments @('tf', $JarPath)
    return (@($listing | Where-Object { $_ -match $EntryPattern }).Count -gt 0)
}

function Remove-DhaDuplicateEnvKeys {
    <#
      清掉"只有大小写不同"的重复环境变量。

      为什么会踩到：这台机器的父进程（宿主）环境块里同时存在
          HTTPS_PROXY / https_proxy、HTTP_PROXY / http_proxy、Path / PATH
      三对重复项。Windows 自己不在乎 —— 它查找环境变量从来不分大小写。
      但 PowerShell 5.1 的 Start-Process 会把当前环境往一个**大小写不敏感的**
      StringDictionary 里塞，塞第二项时必然撞键，直接抛：

          已添加项。字典中的关键字:"HTTPS_PROXY"所添加的关键字:"https_proxy"

      而且它是在 cmdlet 参数绑定阶段炸的，既不是 terminating error 也来不及被
      调用方的 try/catch 接住 —— 现象就是"脚本跑到 Start-Process 就没了"。

      处理：把每种大小写写法都置空再写回一份规范副本。Windows 下
      SetEnvironmentVariable 按名查找不分大小写，同一个"名字"的多个变体会被一起清掉。
      只影响本进程，动不到系统环境，跑完就随进程消失。

      返回被修复的组数，调用方可以据此打日志。
    #>
    [CmdletBinding()]
    param()

    $rawKeys = @([System.Environment]::GetEnvironmentVariables().Keys)

    $duplicates = @(
        $rawKeys |
            Group-Object { $_.ToLowerInvariant() } |
            Where-Object { $_.Count -gt 1 }
    )

    if ($duplicates.Count -eq 0) { return 0 }

    # 优先保留这些规范写法，其余变体一律丢掉
    $canonical = @{
        'path'        = 'PATH'
        'http_proxy'  = 'HTTP_PROXY'
        'https_proxy' = 'HTTPS_PROXY'
        'no_proxy'    = 'NO_PROXY'
        'all_proxy'   = 'ALL_PROXY'
    }

    $fixed = 0
    foreach ($group in $duplicates) {
        $keepName = $canonical[$group.Name]
        $source = $group.Group | Where-Object { $_ -ceq $keepName } | Select-Object -First 1
        if (-not $source) { $source = $group.Group[0] }
        if (-not $keepName) { $keepName = $source }

        # 一定要先把值捞出来，再动手清
        $value = [System.Environment]::GetEnvironmentVariable($source)

        foreach ($variant in $group.Group) {
            [System.Environment]::SetEnvironmentVariable($variant, $null)
        }
        [System.Environment]::SetEnvironmentVariable($keepName, $value)
        $fixed++
    }

    return $fixed
}
