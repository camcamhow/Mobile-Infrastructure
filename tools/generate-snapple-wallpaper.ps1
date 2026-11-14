Add-Type -AssemblyName System.Drawing

$width = 512
$height = 960
$bitmap = New-Object System.Drawing.Bitmap($width, $height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
try {
    for ($y = 0; $y -lt $height; $y++) {
        $v = $y / ($height - 1.0)
        for ($x = 0; $x -lt $width; $x++) {
            $u = $x / ($width - 1.0)
            $dx = [Math]::Max(0, [Math]::Max(40 - ($x + 0.5), ($x + 0.5) - ($width - 40)))
            $dy = [Math]::Max(0, [Math]::Max(40 - ($y + 0.5), ($y + 0.5) - ($height - 40)))
            $coverage = [Math]::Min(1.0, [Math]::Max(0.0, 40.5 - [Math]::Sqrt($dx * $dx + $dy * $dy)))
            $alpha = [int](255 * $coverage)
            if ($alpha -eq 0) {
                continue
            }
            $glow = [Math]::Exp(-((($u - 0.60) * ($u - 0.60) / 0.18) + (($v - 0.42) * ($v - 0.42) / 0.10)))
            $curve = 0.39 + 0.21 * [Math]::Sin($u * 3.0 - 1.15)
            $ribbon = [Math]::Exp(-[Math]::Pow(($v - $curve) / 0.095, 2))
            $edge = [Math]::Exp(-[Math]::Pow(($v - $curve - 0.025) / 0.009, 2))
            $secondCurve = 0.67 - 0.13 * [Math]::Sin($u * 3.6 + 0.2)
            $second = [Math]::Exp(-[Math]::Pow(($v - $secondCurve) / 0.08, 2))
            $secondEdge = [Math]::Exp(-[Math]::Pow(($v - $secondCurve + 0.03) / 0.012, 2))
            $orangeCurve = 0.91 - 0.06 * [Math]::Sin($u * 4.0)
            $orange = [Math]::Exp(-[Math]::Pow(($v - $orangeCurve) / 0.055, 2))
            $red = [int][Math]::Min(255, 8 + 34 * $glow + 73 * $ribbon + 48 * $edge + 37 * $second + 30 * $secondEdge + 190 * $orange)
            $green = [int][Math]::Min(255, 8 + 12 * $glow + 12 * $ribbon + 22 * $edge + 8 * $second + 10 * $secondEdge + 68 * $orange)
            $blueChannel = [int][Math]::Min(255, 27 + 56 * $glow + 110 * $ribbon + 87 * $edge + 65 * $second + 54 * $secondEdge + 10 * $orange)
            $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($alpha, $red, $green, $blueChannel))
        }
    }
    $target = Join-Path $PSScriptRoot '..\src\main\resources\assets\mobilecellnetwork\textures\gui\snapple_wallpaper.png'
    $bitmap.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $bitmap.Dispose()
}
