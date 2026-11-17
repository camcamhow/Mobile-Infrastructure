Add-Type -AssemblyName System.Drawing

function New-RoundedPath([single]$x, [single]$y, [single]$width, [single]$height, [single]$radius) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $diameter = $radius * 2
    $path.AddArc($x, $y, $diameter, $diameter, 180, 90)
    $path.AddArc($x + $width - $diameter, $y, $diameter, $diameter, 270, 90)
    $path.AddArc($x + $width - $diameter, $y + $height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($x, $y + $height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

$bitmap = New-Object System.Drawing.Bitmap(880, 1600, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
try {
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $outer = New-RoundedPath 2 2 876 1596 94
    $inner = New-RoundedPath 13 13 854 1574 83
    $screen = New-RoundedPath 32 32 816 1536 64
    $outerGradient = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Point(0, 0)), (New-Object System.Drawing.Point(880, 0)),
        [System.Drawing.Color]::FromArgb(255, 108, 110, 117),
        [System.Drawing.Color]::FromArgb(255, 64, 66, 72))
    $bezel = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 8, 9, 13))
    $clear = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::Transparent)
    $outerShine = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(210, 170, 172, 180), 3)
    $innerShine = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(190, 45, 48, 56), 3)
    $key = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 112, 114, 122))
    $keyShine = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(190, 190, 192, 198), 2)
    try {
        $graphics.FillPath($outerGradient, $outer)
        $graphics.FillPath($bezel, $inner)
        $graphics.DrawPath($outerShine, $outer)
        $graphics.DrawPath($innerShine, $inner)
        foreach ($bounds in @(@(2, 376, 8, 80), @(2, 485, 8, 112), @(2, 618, 8, 112), @(870, 465, 8, 160))) {
            $button = New-RoundedPath $bounds[0] $bounds[1] $bounds[2] $bounds[3] 4
            try {
                $graphics.FillPath($key, $button)
                $graphics.DrawPath($keyShine, $button)
            } finally {
                $button.Dispose()
            }
        }
        $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
        $graphics.FillPath($clear, $screen)
    } finally {
        $outerGradient.Dispose()
        $bezel.Dispose()
        $clear.Dispose()
        $outerShine.Dispose()
        $innerShine.Dispose()
        $key.Dispose()
        $keyShine.Dispose()
        $outer.Dispose()
        $inner.Dispose()
        $screen.Dispose()
    }
    $target = Join-Path $PSScriptRoot '..\src\main\resources\assets\mobilecellnetwork\textures\gui\snapple_frame.png'
    $bitmap.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $graphics.Dispose()
    $bitmap.Dispose()
}
