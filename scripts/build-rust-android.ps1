param(
    [switch]$DebugBuild
)

$ErrorActionPreference = "Stop"

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$RustCoreDir = Join-Path $RepoRoot "rust-core"
$ManifestPath = Join-Path $RustCoreDir "Cargo.toml"
$JniLibsDir = Join-Path $RepoRoot "android-app\app\src\main\jniLibs"
$KotlinOutDir = Join-Path $RepoRoot "android-app\app\src\main\java"

# Resolver ANDROID_NDK_HOME automáticamente
$NdkCandidates = @($env:ANDROID_NDK_HOME, $env:NDK_HOME) | Where-Object { $_ -and (Test-Path $_) }
$ResolvedNdk = $NdkCandidates | Select-Object -First 1

if (-not $ResolvedNdk) {
    $SdkRoot = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, "$env:LOCALAPPDATA\Android\Sdk") |
        Where-Object { $_ -and (Test-Path $_) } |
        Select-Object -First 1
    if ($SdkRoot -and (Test-Path (Join-Path $SdkRoot "ndk"))) {
        $LatestNdk = Get-ChildItem (Join-Path $SdkRoot "ndk") -Directory |
            Sort-Object Name -Descending |
            Select-Object -First 1
        if ($LatestNdk) {
            $ResolvedNdk = $LatestNdk.FullName
        }
    }
}

if (-not $ResolvedNdk) {
    throw "No se encontró el Android NDK. Define ANDROID_NDK_HOME o NDK_HOME."
}

$env:ANDROID_NDK_HOME = $ResolvedNdk
$env:NDK_HOME = $ResolvedNdk
Write-Host "[build-rust-android] Usando NDK: $env:ANDROID_NDK_HOME"

# Verificar cargo-ndk
& cargo ndk --version | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "cargo-ndk no está instalado. Ejecuta: cargo install cargo-ndk --locked"
}

New-Item -ItemType Directory -Force -Path $JniLibsDir | Out-Null
New-Item -ItemType Directory -Force -Path $KotlinOutDir | Out-Null

$BuildArgs = @(
    "ndk",
    "-t", "arm64-v8a",
    "-t", "x86_64",
    "--platform", "26",
    "--manifest-path", $ManifestPath,
    "-o", $JniLibsDir,
    "build"
)
if (-not $DebugBuild) {
    $BuildArgs += "--release"
}

Write-Host "[build-rust-android] Compilando librerías nativas (.so) para arm64-v8a y x86_64..."
Push-Location $RustCoreDir
try {
    & cargo @BuildArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Falló la compilación cruzada con cargo-ndk (exit code $LASTEXITCODE)."
    }
} finally {
    Pop-Location
}

# Verificar DT_NEEDED y copiar libc++_shared.so si es requerido
$PrebuiltDir = Join-Path $env:ANDROID_NDK_HOME "toolchains\llvm\prebuilt\windows-x86_64"
$ReadElf = Join-Path $PrebuiltDir "bin\llvm-readelf.exe"
$AbiTriples = @{
    "arm64-v8a" = "aarch64-linux-android"
    "x86_64"    = "x86_64-linux-android"
}

foreach ($Abi in $AbiTriples.Keys) {
    $SoPath = Join-Path $JniLibsDir "$Abi\libappnotas_core.so"
    if (-not (Test-Path $SoPath)) {
        throw "No se generó $SoPath"
    }
    $SizeMb = [Math]::Round((Get-Item $SoPath).Length / 1MB, 2)
    Write-Host "[build-rust-android] Generado $SoPath ($SizeMb MB)"

    if (Test-Path $ReadElf) {
        $Needed = & $ReadElf -d $SoPath | Select-String "NEEDED"
        Write-Host "[build-rust-android] Dependencias dinámicas ($Abi):"
        $Needed | ForEach-Object { Write-Host "  $($_.Line.Trim())" }

        if ($Needed -match "libc\+\+_shared\.so") {
            $Triple = $AbiTriples[$Abi]
            $LibCppSrc = Join-Path $PrebuiltDir "sysroot\usr\lib\$Triple\libc++_shared.so"
            if (Test-Path $LibCppSrc) {
                Copy-Item $LibCppSrc (Join-Path $JniLibsDir "$Abi\libc++_shared.so") -Force
                Write-Host "[build-rust-android] Copiado libc++_shared.so para $Abi"
            } else {
                Write-Warning "Se requiere libc++_shared.so para $Abi pero no se encontró en $LibCppSrc"
            }
        }
    }
}

# Generar bindings Kotlin con uniffi-bindgen integrado (misma versión exacta de uniffi).
# Se ejecuta dentro de $RustCoreDir para que cargo metadata encuentre Cargo.toml y cargue rust-core/uniffi.toml automáticamente.
$RefSo = Join-Path $JniLibsDir "arm64-v8a\libappnotas_core.so"
Write-Host "[build-rust-android] Generando bindings Kotlin con UniFFI..."
Push-Location $RustCoreDir
try {
    & cargo run --features bindgen-cli --bin uniffi-bindgen -- `
        generate $RefSo --language kotlin --out-dir $KotlinOutDir
    if ($LASTEXITCODE -ne 0) {
        throw "Falló la generación de bindings Kotlin con uniffi-bindgen (exit code $LASTEXITCODE)."
    }
} finally {
    Pop-Location
}

Write-Host "[build-rust-android] ¡Compilación cruzada y generación de bindings completadas!"
