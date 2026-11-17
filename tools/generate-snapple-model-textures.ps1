Add-Type -AssemblyName System.Drawing
$target = Join-Path $PSScriptRoot '..\src\main\resources\assets\mobilecellnetwork\textures\item\snapple_realistic'
New-Item -ItemType Directory -Force -Path $target | Out-Null
$colors = @{
    metal = '#62636a'; rim = '#a0a0a2'; back = '#25282e'; glass = '#050609'
    camera_base = '#31343a'; lens = '#0c111a'; lens_inner = '#172538'
    highlight = '#819cb4'; flash = '#e5e0c3'
}
foreach ($name in $colors.Keys) {
    $bitmap = New-Object System.Drawing.Bitmap(16, 16)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.ColorTranslator]::FromHtml($colors[$name]))
        $bitmap.Save((Join-Path $target "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}
$bitmap = New-Object System.Drawing.Bitmap(512, 1024)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$wallpaperPath = Join-Path $PSScriptRoot '..\src\main\resources\assets\mobilecellnetwork\textures\gui\snapple_wallpaper.png'
$wallpaper = [System.Drawing.Image]::FromFile((Resolve-Path $wallpaperPath).Path)
$brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 4, 5, 8))
try {
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.DrawImage($wallpaper, 0, 0, 512, 1024)
    $graphics.FillEllipse($brush, 177, 24, 50, 50)
    $graphics.FillRectangle($brush, 202, 24, 108, 50)
    $graphics.FillEllipse($brush, 285, 24, 50, 50)
    $bitmap.Save((Join-Path $target 'screen.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $brush.Dispose()
    $wallpaper.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}
