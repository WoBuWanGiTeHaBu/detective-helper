# 把 electron/assets/icon.png 转成 Windows 的多尺寸 .ico。
#
# 为什么要单独这一步：electron-builder 在 Windows 上只接受 .ico，给 .png 会直接报错
# （见 electron/package.json 的 win.icon）。仓库里那份 icon.ico 就是本脚本产出的，
# 要换图标时改 icon.png 再跑一遍即可。
#
# 用 .NET 的 System.Drawing 缩放（Windows 自带，不需要装任何东西），
# 再把各尺寸以 PNG 形式打包进 ICO 容器（Vista 以后都支持 PNG 条目）。
#
# 输出：electron/assets/icon.ico
#
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File scripts\make-icon.ps1

[CmdletBinding()]
param(
    [string]$PngPath,
    [string]$OutputPath
)

$ScriptDir = $PSScriptRoot
if (-not $ScriptDir) {
    $ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
}

. (Join-Path $ScriptDir '_common.ps1')

$ProjectRoot = Get-DhaProjectRoot -ScriptDir $ScriptDir

if (-not $PngPath) {
    $PngPath = Join-Path $ProjectRoot 'electron\assets\icon.png'
}
if (-not $OutputPath) {
    $OutputPath = Join-Path $ProjectRoot 'electron\assets\icon.ico'
}

if (-not (Test-Path $PngPath)) {
    throw "找不到图标源文件：$PngPath"
}

Add-Type -AssemblyName System.Drawing

$outputDir = Split-Path -Parent $OutputPath
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}

$sizes = @(16, 24, 32, 48, 64, 128, 256)
$entries = New-Object System.Collections.ArrayList

$source = [System.Drawing.Image]::FromFile($PngPath)
try {
    foreach ($size in $sizes) {
        $bitmap = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
            try {
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)
                $graphics.DrawImage($source, $rect)
            } finally {
                $graphics.Dispose()
            }

            $stream = New-Object System.IO.MemoryStream
            try {
                $bitmap.Save($stream, [System.Drawing.Imaging.ImageFormat]::Png)
                $bytes = $stream.ToArray()
            } finally {
                $stream.Dispose()
            }

            [void]$entries.Add([PSCustomObject]@{ Size = $size; Bytes = $bytes })
        } finally {
            $bitmap.Dispose()
        }
    }
} finally {
    $source.Dispose()
}

# ---- 组装 ICO ----
# 结构：ICONDIR(6 字节) + ICONDIRENTRY(16 字节 * n) + 各条目数据
$outStream = New-Object System.IO.MemoryStream
$writer = New-Object System.IO.BinaryWriter($outStream)
try {
    $writer.Write([UInt16]0)                    # reserved
    $writer.Write([UInt16]1)                    # type = icon
    $writer.Write([UInt16]$entries.Count)       # image count

    $offset = 6 + 16 * $entries.Count
    foreach ($entry in $entries) {
        # 256 在 ICO 的宽高字节里用 0 表示
        $dimension = 0
        if ($entry.Size -lt 256) { $dimension = $entry.Size }

        $writer.Write([Byte]$dimension)             # width
        $writer.Write([Byte]$dimension)             # height
        $writer.Write([Byte]0)                      # color count（32bpp 用不到）
        $writer.Write([Byte]0)                      # reserved
        $writer.Write([UInt16]1)                    # color planes
        $writer.Write([UInt16]32)                   # bits per pixel
        $writer.Write([UInt32]$entry.Bytes.Length)  # size in bytes
        $writer.Write([UInt32]$offset)              # offset
        $offset += $entry.Bytes.Length
    }

    foreach ($entry in $entries) {
        $writer.Write($entry.Bytes)
    }

    $writer.Flush()
    [System.IO.File]::WriteAllBytes($OutputPath, $outStream.ToArray())
} finally {
    $writer.Dispose()
    $outStream.Dispose()
}

$info = Get-Item $OutputPath
Write-DhaOk ("已生成图标 {0}（{1} 个尺寸，{2:N0} 字节）" -f $info.FullName, $entries.Count, $info.Length)
