<#
.SYNOPSIS
  把设计稿指定的 Noto 字体镜像到 frontend/public/fonts/，生成 fonts.css。

.DESCRIPTION
  默认**不需要**跑这个脚本。

  桌面版已经改成纯本地字体栈（见 src/styles/tokens.css 与 index.html 里的说明）：
  不请求任何外网，首屏不会被外链样式表阻塞，Windows 上退到华文中宋 / 微软雅黑 / Consolas，
  观感够用。这个脚本是为了「我就是想要设计稿里一模一样的思源/Noto 字体」这种情况准备的。

  它做的事：
    1. 取 Google Fonts 的 css2 接口（伪装成 Chrome，才会返回 woff2 + unicode-range 分片）
    2. 把 CSS 里引用的每个 woff2 分片下载到 frontend/public/fonts/
    3. 把 CSS 里的绝对 URL 改写成相对路径，落成 fonts/fonts.css
    4. 加 -Enable 的话，顺便把 index.html 里那行注释掉的 <link> 解开

  代价要知道：CJK 字体的分片很多（思源黑体一个字重就有 80+ 片），
  全部下完大概 15~25 MB，这些会一起进 dist、进 jar、进安装包。

.PARAMETER OutDir
  输出目录，默认 frontend\public\fonts。

.PARAMETER Enable
  下载完成后自动把 index.html 里的 <link rel="stylesheet" href="/fonts/fonts.css" /> 解注释。

.PARAMETER Force
  目标目录已存在也重新下载。

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\fetch-fonts.ps1 -Enable
#>

[CmdletBinding()]
param(
    [string]$OutDir,
    [switch]$Enable,
    [switch]$Force,
    [string]$CssUrl = 'https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700&family=Noto+Serif+SC:wght@700&family=Noto+Sans+Mono:wght@400;500&display=swap'
)

$ScriptDir = $PSScriptRoot
if (-not $ScriptDir) {
    $ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
}

. (Join-Path $ScriptDir '_common.ps1')

$ProjectRoot = Get-DhaProjectRoot -ScriptDir $ScriptDir
if (-not $OutDir) { $OutDir = Join-Path $ProjectRoot 'frontend\public\fonts' }

# Google 的 css2 接口按 User-Agent 决定返回什么格式：不带 UA 会拿到 ttf，
# 带现代 Chrome UA 才给 woff2 + unicode-range 分片（体积小一个量级）。
$userAgent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'

function Get-DhaWebBytes {
    # 按字节取回内容。不能用 $resp.Content —— 它会把二进制当文本按响应 charset 解码，图片/字体直接损坏。
    param([Parameter(Mandatory = $true)][string]$Url)

    $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 90 `
        -Headers @{ 'User-Agent' = $userAgent }
    $stream = $resp.RawContentStream
    $buffer = New-Object System.IO.MemoryStream
    try {
        $stream.CopyTo($buffer)
        return , $buffer.ToArray()
    } finally {
        $buffer.Dispose()
    }
}

Write-DhaStep '镜像 Google 字体（可选步骤）'
Write-DhaInfo "输出目录 : $OutDir"

if ((Test-Path $OutDir) -and -not $Force) {
    $existing = @(Get-ChildItem $OutDir -Filter '*.woff2' -File -ErrorAction SilentlyContinue)
    if ($existing.Count -gt 0) {
        Write-DhaWarn "目录里已经有 $($existing.Count) 个 woff2 分片，跳过。要重下请加 -Force。"
        exit 0
    }
}

# ---- 1. 取 CSS ----
Write-DhaInfo "拉取字体清单：$CssUrl"
try {
    $cssBytes = Get-DhaWebBytes -Url $CssUrl
} catch {
    throw "取不到 Google Fonts 的 CSS（检查网络/代理）：$($_.Exception.Message)"
}
$css = [System.Text.Encoding]::UTF8.GetString($cssBytes)

$blockRegex = [regex]'(?s)@font-face\s*\{(?<body>.*?)\}'
$urlRegex = [regex]'url\(\s*(?<url>https://fonts\.gstatic\.com/[^)\s]+)\s*\)'
$familyRegex = [regex]'font-family\s*:\s*''(?<v>[^'']+)'''
$weightRegex = [regex]'font-weight\s*:\s*(?<v>[\d]+)'

$blocks = $blockRegex.Matches($css)
if ($blocks.Count -eq 0) {
    throw 'CSS 里没有解析到任何 @font-face，接口返回的格式可能变了。'
}
Write-DhaInfo "@font-face 规则数：$($blocks.Count)"

if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Force -Path $OutDir | Out-Null }

# ---- 2. 逐条下载并改写 URL ----
$outStream = New-Object System.IO.MemoryStream
$outWriter = New-Object System.IO.StreamWriter($outStream, (New-Object System.Text.UTF8Encoding($false)))
$cache = @{}      # 原始 URL -> 本地文件名（同一分片会被多个字重复用）
$downloaded = 0
$failed = 0
$totalBytes = 0

try {
    $outWriter.WriteLine('/*')
    $outWriter.WriteLine('  由 scripts\fetch-fonts.ps1 自动生成，请勿手改。')
    $outWriter.WriteLine("  源：$CssUrl")
    $outWriter.WriteLine("  生成时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
    $outWriter.WriteLine('*/')
    $outWriter.WriteLine('')

    $index = 0
    foreach ($block in $blocks) {
        $index++
        $body = $block.Groups['body'].Value
        $newBody = $body

        foreach ($m in $urlRegex.Matches($body)) {
            $remoteUrl = $m.Groups['url'].Value

            if (-not $cache.ContainsKey($remoteUrl)) {
                $family = 'font'
                $weight = '400'
                $fm = $familyRegex.Match($body); if ($fm.Success) { $family = $fm.Groups['v'].Value }
                $wm = $weightRegex.Match($body); if ($wm.Success) { $weight = $wm.Groups['v'].Value }

                $slug = ($family.ToLowerInvariant() -replace '[^a-z0-9]+', '')
                # 原始文件名结尾的 .4.woff2 这类序号是分片编号，保留它避免重名
                $tail = ($remoteUrl -split '/')[-1]
                $seq = ''
                $sm = [regex]::Match($tail, '(\d+)\.woff2$')
                if ($sm.Success) { $seq = '-' + $sm.Groups[1].Value }
                $fileName = "$slug-$weight$seq.woff2"

                # 极端情况下仍有重名：加个后缀
                $n = 1
                while ($cache.Values -contains $fileName) {
                    $fileName = "$slug-$weight$seq-$n.woff2"
                    $n++
                }

                $dest = Join-Path $OutDir $fileName
                try {
                    $bytes = Get-DhaWebBytes -Url $remoteUrl
                    [System.IO.File]::WriteAllBytes($dest, $bytes)
                    $totalBytes += $bytes.Length
                    $downloaded++
                } catch {
                    Write-DhaWarn "下载失败，跳过：$tail（$($_.Exception.Message)）"
                    $failed++
                    $cache[$remoteUrl] = $null
                    continue
                }

                $cache[$remoteUrl] = $fileName

                if ($downloaded % 25 -eq 0) {
                    Write-DhaInfo ("已下载 {0} 个分片…（{1:N1} MB）" -f $downloaded, ($totalBytes / 1MB))
                }
            }

            $local = $cache[$remoteUrl]
            if ($local) {
                $newBody = $newBody.Replace($remoteUrl, "./$local")
            }
        }

        $outWriter.WriteLine('@font-face {' + $newBody.TrimEnd() + '}')
        $outWriter.WriteLine('')
    }
} finally {
    $outWriter.Flush()
    [System.IO.File]::WriteAllBytes((Join-Path $OutDir 'fonts.css'), $outStream.ToArray())
    $outWriter.Dispose()
    $outStream.Dispose()
}

Write-DhaOk ("分片 {0} 个，合计 {1:N1} MB（失败 {2} 个）" -f $downloaded, ($totalBytes / 1MB), $failed)
Write-DhaOk "样式表 frontend\public\fonts\fonts.css"

# ---- 3. 可选：解开 index.html 里的引用 ----
$indexHtml = Join-Path $ProjectRoot 'frontend\index.html'
$commented = '<!-- <link rel="stylesheet" href="/fonts/fonts.css" /> -->'
$live = '<link rel="stylesheet" href="/fonts/fonts.css" />'

if ($Enable) {
    if (-not (Test-Path $indexHtml)) {
        Write-DhaWarn "找不到 index.html，跳过启用步骤：$indexHtml"
    } else {
        $html = Get-Content $indexHtml -Raw -Encoding UTF8
        if ($html.Contains($live)) {
            Write-DhaInfo 'index.html 里 <link> 已经是启用状态，无需改动'
        } elseif ($html.Contains($commented)) {
            $html = $html.Replace($commented, $live)
            Set-Content -Path $indexHtml -Value $html -Encoding UTF8 -NoNewline
            Write-DhaOk 'index.html 已启用 /fonts/fonts.css'
        } else {
            Write-DhaWarn 'index.html 里找不到那行注释，请手动加：'
            Write-DhaInfo $live
        }
    }
} else {
    Write-DhaInfo ''
    Write-DhaInfo '下一步（二选一）：'
    Write-DhaInfo "  · 直接在 index.html 里启用：$live"
    Write-DhaInfo '  · 或重跑本脚本时加 -Enable，自动解注释'
}

if ($failed -gt 0) { exit 1 }
exit 0
