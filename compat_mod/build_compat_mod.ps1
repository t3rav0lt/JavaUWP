$ErrorActionPreference = "Stop"

. (Join-Path (Split-Path $PSScriptRoot -Parent) "scripts\common.ps1")

$root = Resolve-RepoRoot
$srcJava = Join-Path $PSScriptRoot "src\main\java"
$srcResources = Join-Path $PSScriptRoot "src\main\resources"
$buildRoot = Join-Path (Get-ConfigPath "BuildDir") "compat_mod"
$classesDir = Join-Path $buildRoot "classes"
$compatJarName = "$($ProjectConfig.CompatModId)-$($ProjectConfig.CompatModVersion).jar"
$jarPath = Join-Path $buildRoot $compatJarName
$gameDir = Get-ConfigPath "GameDir"
$modsDir = Join-Path $gameDir "mods"

$javaHome = Resolve-JavaHome
$javac = Join-Path $javaHome "bin\javac.exe"
$jar = Join-Path $javaHome "bin\jar.exe"
$mixinVersion = $ProjectConfig.MixinVersion
$minecraftVersion = $ProjectConfig.MinecraftVersion
$loaderVersion = $ProjectConfig.FabricLoaderVersion
$mixinJar = Join-Path $gameDir "libraries\net\fabricmc\sponge-mixin\$mixinVersion\sponge-mixin-$mixinVersion.jar"
$oshiJar = Get-ChildItem (Join-Path $gameDir "libraries\com\github\oshi") -Recurse -Filter "oshi-core-*.jar" |
    Sort-Object FullName -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $oshiJar) {
    throw "oshi-core jar not found under $gameDir\libraries\com\github\oshi (run scripts/download-libs.ps1)."
}
# 26.1+ ships unobfuscated; compile against the vanilla client jar. Older versions used
# Fabric's intermediary remapped jar.
$clientJar = Join-Path $gameDir "versions\$minecraftVersion\$minecraftVersion.jar"
if (-not (Test-Path $clientJar)) {
    $clientJar = Join-Path $gameDir ".fabric\remappedJars\minecraft-$minecraftVersion-$loaderVersion\client-intermediary.jar"
}
if (-not (Test-Path $clientJar)) {
    throw "Minecraft client jar not found. Run scripts/download-libs.ps1 and install Fabric for $minecraftVersion."
}

Remove-Item -Recurse -Force $buildRoot -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
New-Item -ItemType Directory -Force -Path $modsDir | Out-Null

$sources = Get-ChildItem $srcJava -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
if (-not $sources) { throw "No compatibility mod sources found" }

$cp = @($clientJar, $mixinJar, $oshiJar) -join ";"
& $javac --release $ProjectConfig.JavaRelease -proc:none -cp $cp -d $classesDir $sources
if ($LASTEXITCODE -ne 0) { throw "compatibility mod compile failed" }

Copy-Item -Recurse "$srcResources\*" $classesDir -Force

Push-Location $classesDir
& $jar cf $jarPath .
if ($LASTEXITCODE -ne 0) {
    Pop-Location
    throw "compatibility mod jar failed"
}
Pop-Location

Copy-Item $jarPath (Join-Path $modsDir $compatJarName) -Force
Write-Host "Compatibility mod built -> $jarPath"
