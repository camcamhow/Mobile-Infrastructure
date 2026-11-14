Add-Type -AssemblyName System.Drawing

$icons = @{
    phone = @('#53E575', '#09A74B'); sms = @('#56E77A', '#0CAC4D')
    contacts = @('#F2EEE7', '#C9C6C1'); map = @('#58B7E2', '#3479B3')
    store = @('#36BFF5', '#1966CD'); socials = @('#CB5BE5', '#8B3CA3')
    bank = @('#667CF4', '#3646A1'); settings = @('#D6D7DB', '#8A8D95')
    plan = @('#6DB6F7', '#326EC8'); voicemail = @('#708FE8', '#3857A2')
    camera = @('#555C68', '#20242C'); gallery = @('#F2A65D', '#C64E8D')
    flashlight = @('#8997A5', '#3E4C5A')
}
$target = Join-Path $PSScriptRoot '..\src\main\resources\assets\mobilecellnetwork\textures\gui\snapple_apps'
New-Item -ItemType Directory -Force $target | Out-Null
foreach ($name in $icons.Keys) {
    $bitmap = New-Object System.Drawing.Bitmap(256, 256, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
    $g = [System.Drawing.Graphics]::FromImage($bitmap)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    try {
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $path.AddArc(0, 0, 112, 112, 180, 90)
        $path.AddArc(144, 0, 112, 112, 270, 90)
        $path.AddArc(144, 144, 112, 112, 0, 90)
        $path.AddArc(0, 144, 112, 112, 90, 90)
        $path.CloseFigure()
        $top = [System.Drawing.ColorTranslator]::FromHtml($icons[$name][0])
        $bottom = [System.Drawing.ColorTranslator]::FromHtml($icons[$name][1])
        $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
            (New-Object System.Drawing.Point(0, 0)), (New-Object System.Drawing.Point(0, 256)), $top, $bottom)
        $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, 12)
        $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
        $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
        $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
        $white = [System.Drawing.Brushes]::White
        try {
            $g.FillPath($brush, $path)
            switch ($name) {
                phone {
                    $handset = New-Object System.Drawing.Drawing2D.GraphicsPath
                    try {
                        $handset.AddBezier(70, 50, 55, 55, 47, 70, 51, 91)
                        $handset.AddBezier(51, 91, 61, 138, 116, 190, 163, 203)
                        $handset.AddBezier(163, 203, 185, 208, 200, 197, 205, 182)
                        $handset.AddBezier(205, 182, 207, 176, 202, 170, 197, 167)
                        $handset.AddLine(197, 167, 168, 151)
                        $handset.AddBezier(168, 151, 162, 148, 157, 150, 153, 154)
                        $handset.AddLine(153, 154, 143, 165)
                        $handset.AddBezier(143, 165, 121, 155, 99, 133, 89, 111)
                        $handset.AddLine(89, 111, 100, 101)
                        $handset.AddBezier(100, 101, 104, 97, 106, 92, 103, 86)
                        $handset.AddLine(103, 86, 87, 57)
                        $handset.AddBezier(87, 57, 84, 51, 78, 48, 70, 50)
                        $handset.CloseFigure()
                        $g.FillPath($white, $handset)
                    } finally { $handset.Dispose() }
                }
                sms {
                    $bubble = New-Object System.Drawing.Drawing2D.GraphicsPath
                    try {
                        $bubble.AddBezier(46, 116, 46, 72, 81, 52, 127, 52)
                        $bubble.AddBezier(127, 52, 174, 52, 210, 75, 210, 116)
                        $bubble.AddBezier(210, 116, 210, 157, 174, 179, 125, 179)
                        $bubble.AddBezier(125, 179, 116, 179, 107, 178, 98, 175)
                        $bubble.AddBezier(98, 175, 86, 187, 69, 192, 57, 192)
                        $bubble.AddBezier(57, 192, 68, 182, 73, 173, 74, 164)
                        $bubble.AddBezier(74, 164, 56, 153, 46, 137, 46, 116)
                        $bubble.CloseFigure()
                        $g.FillPath($white, $bubble)
                    } finally { $bubble.Dispose() }
                }
                contacts {
                    $avatar = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(112, 109, 111))
                    $tabBlue = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(73, 164, 204))
                    $tabOrange = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(231, 147, 63))
                    $tabGreen = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(82, 178, 113))
                    $avatarPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(112, 109, 111), 8)
                    try {
                        $g.FillRectangle($tabBlue, 216, 50, 30, 48)
                        $g.FillRectangle($tabOrange, 216, 104, 30, 48)
                        $g.FillRectangle($tabGreen, 216, 158, 30, 48)
                        $g.DrawEllipse($avatarPen, 45, 50, 150, 150)
                        $g.FillEllipse($avatar, 95, 78, 50, 50)
                        $g.FillEllipse($avatar, 70, 134, 100, 48)
                    } finally {
                        $avatar.Dispose(); $tabBlue.Dispose(); $tabOrange.Dispose(); $tabGreen.Dispose(); $avatarPen.Dispose()
                    }
                }
                map {
                    $g.DrawPolygon($pen, [System.Drawing.Point[]]@((New-Object System.Drawing.Point(49, 70)), (New-Object System.Drawing.Point(102, 52)), (New-Object System.Drawing.Point(154, 71)), (New-Object System.Drawing.Point(207, 53)), (New-Object System.Drawing.Point(207, 180)), (New-Object System.Drawing.Point(154, 198)), (New-Object System.Drawing.Point(102, 179)), (New-Object System.Drawing.Point(49, 197))))
                    $g.DrawLine($pen, 102, 55, 102, 177); $g.DrawLine($pen, 154, 73, 154, 194)
                }
                store {
                    $g.DrawRectangle($pen, 62, 92, 132, 105)
                    $g.DrawArc($pen, 92, 50, 72, 84, 180, 180)
                    $g.DrawLine($pen, 91, 145, 165, 145)
                    $g.DrawLine($pen, 128, 115, 128, 175)
                }
                socials {
                    $g.DrawEllipse($pen, 51, 51, 154, 154); $g.DrawEllipse($pen, 101, 101, 54, 54)
                    $g.FillEllipse($white, 164, 73, 17, 17)
                }
                bank {
                    $g.DrawLines($pen, [System.Drawing.Point[]]@((New-Object System.Drawing.Point(49, 99)), (New-Object System.Drawing.Point(128, 54)), (New-Object System.Drawing.Point(207, 99))))
                    $g.DrawLine($pen, 57, 110, 199, 110); $g.DrawLine($pen, 57, 194, 199, 194)
                    foreach ($x in @(77, 128, 179)) { $g.DrawLine($pen, $x, 120, $x, 183) }
                }
                settings {
                    $gear = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(55, 57, 64))
                    $gearPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(55, 57, 64), 17)
                    $gearPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
                    $gearPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
                    try {
                        foreach ($angle in 0..11) {
                            $a = $angle * [Math]::PI / 6
                            $g.DrawLine($gearPen, [int](128 + 65 * [Math]::Cos($a)), [int](128 + 65 * [Math]::Sin($a)), [int](128 + 84 * [Math]::Cos($a)), [int](128 + 84 * [Math]::Sin($a)))
                        }
                        $g.FillEllipse($gear, 56, 56, 144, 144)
                        $g.FillEllipse($brush, 99, 99, 58, 58)
                    } finally { $gear.Dispose(); $gearPen.Dispose() }
                }
                plan {
                    $g.DrawRectangle($pen, 67, 48, 122, 160)
                    $g.DrawLine($pen, 88, 92, 167, 92); $g.DrawLine($pen, 88, 125, 167, 125); $g.DrawLine($pen, 88, 158, 145, 158)
                }
                voicemail {
                    $g.DrawEllipse($pen, 52, 94, 62, 62); $g.DrawEllipse($pen, 142, 94, 62, 62)
                    $g.DrawLine($pen, 83, 156, 173, 156)
                }
                camera {
                    $g.FillRectangle($white, 48, 83, 160, 105); $g.FillRectangle($white, 81, 69, 52, 18)
                    $lens = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(55, 61, 71))
                    try { $g.FillEllipse($lens, 98, 102, 60, 60) } finally { $lens.Dispose() }
                }
                gallery {
                    $g.DrawRectangle($pen, 51, 62, 154, 132); $g.DrawEllipse($pen, 150, 81, 23, 23)
                    $g.DrawLines($pen, [System.Drawing.Point[]]@((New-Object System.Drawing.Point(62, 176)), (New-Object System.Drawing.Point(105, 127)), (New-Object System.Drawing.Point(133, 154)), (New-Object System.Drawing.Point(153, 136)), (New-Object System.Drawing.Point(194, 177))))
                }
                flashlight {
                    $g.DrawPolygon($pen, [System.Drawing.Point[]]@((New-Object System.Drawing.Point(76, 52)), (New-Object System.Drawing.Point(180, 52)), (New-Object System.Drawing.Point(158, 106)), (New-Object System.Drawing.Point(153, 201)), (New-Object System.Drawing.Point(103, 201)), (New-Object System.Drawing.Point(98, 106))))
                    $g.DrawLine($pen, 102, 119, 154, 119)
                }
            }
        } finally {
            $brush.Dispose()
            $pen.Dispose()
        }
        $bitmap.Save((Join-Path $target "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $path.Dispose()
        $g.Dispose()
        $bitmap.Dispose()
    }
}
