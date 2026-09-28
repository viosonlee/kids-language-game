# Build Android launcher resources from the existing mascot artwork.
# No external image API is used. Keep the source asset unchanged.
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path $PSScriptRoot -Parent
$sourcePath = Join-Path $projectRoot 'app/src/main/assets/mascot/png/bunny_idle.png'
$resRoot = Join-Path $projectRoot 'app/src/main/res'
$source = [Drawing.Image]::FromFile($sourcePath)
$foreground = New-Object Drawing.Bitmap 1080,1080
$graphics = [Drawing.Graphics]::FromImage($foreground)
$graphics.Clear([Drawing.Color]::Transparent)
$graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
# Crop above the Idle badge. Keep the full rabbit silhouette inside the mask-safe area.
$cropHeight = 503
$width = 460
$height = [int]($width * $cropHeight / $source.Width)
$target = New-Object Drawing.Rectangle ([int]((1080-$width)/2)),([int]((1080-$height)/2)),$width,$height
$crop = New-Object Drawing.Rectangle 0,0,$source.Width,$cropHeight
$graphics.DrawImage($source,$target,$crop,[Drawing.GraphicsUnit]::Pixel)
$graphics.Dispose()
$drawable = Join-Path $resRoot 'drawable-nodpi'
New-Item -ItemType Directory -Force $drawable | Out-Null
$foreground.Save((Join-Path $drawable 'ic_launcher_foreground.png'),[Drawing.Imaging.ImageFormat]::Png)
$legacy = New-Object Drawing.Bitmap 1080,1080
$graphics = [Drawing.Graphics]::FromImage($legacy)
$graphics.Clear([Drawing.ColorTranslator]::FromHtml('#DCEEFF'))
$graphics.DrawImage($foreground,0,0)
$graphics.Dispose()
$densities = @{mdpi=48; hdpi=72; xhdpi=96; xxhdpi=144; xxxhdpi=192}
foreach ($density in $densities.Keys) {
    $dir = Join-Path $resRoot ('mipmap-' + $density)
    New-Item -ItemType Directory -Force $dir | Out-Null
    $size = $densities[$density]
    $bitmap = New-Object Drawing.Bitmap $size,$size
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    # Legacy uses the same launcher-visible region as the adaptive mask.
    $graphics.DrawImage($legacy,(New-Object Drawing.Rectangle 0,0,$size,$size),(New-Object Drawing.Rectangle 180,180,720,720),[Drawing.GraphicsUnit]::Pixel)
    $graphics.Dispose()
    $bitmap.Save((Join-Path $dir 'ic_launcher.png'),[Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}
$previewDir = Join-Path $projectRoot 'app/build/icon-preview'
New-Item -ItemType Directory -Force $previewDir | Out-Null
$legacy.Save((Join-Path $previewDir 'rabbit-icon.png'),[Drawing.Imaging.ImageFormat]::Png)
$foreground.Dispose()
$legacy.Dispose()
$source.Dispose()
