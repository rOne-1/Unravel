# ==============================================================================
# Unravel (com.unravel.app) — Standalone Beta Release Packaging & Distribution
# Target Release: v0.6.0-beta.0 | Signed Distribution Verification
# ==============================================================================

param(
    [string]$ApkPath = "release-artifacts/unravel-v0.6.0-beta.0.apk",
    [string]$Sha256Path = "release-artifacts/unravel-v0.6.0-beta.0.apk.sha256",
    [string]$BuildToolsVersion = "36.0.0",
    [bool]$InstallOnEmulator = $true
)

Write-Host "=========================================================================="
Write-Host " [Unravel Packaging] Beta Release Packaging & Distribution Audit          "
Write-Host " Target Release: v0.6.0-beta.0 | Signed APK & SHA-256 Checksum            "
Write-Host "=========================================================================="

# 1. Execute Gradle Beta Distribution Task
Write-Host "`n[*] Step 1: Executing Gradle packageBetaDistribution task..."
./gradlew :app:packageBetaDistribution --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle packageBetaDistribution task failed!"
    exit 1
}

# 2. Verify Output Artifacts
Write-Host "`n[*] Step 2: Validating Distribution Output Files..."
if (-not (Test-Path $ApkPath)) {
    Write-Error "Target APK not found at: $ApkPath"
    exit 1
}
if (-not (Test-Path $Sha256Path)) {
    Write-Error "Target checksum file not found at: $Sha256Path"
    exit 1
}

$apkItem = Get-Item $ApkPath
$apkSizeBytes = $apkItem.Length
$apkSizeMb = [math]::Round($apkSizeBytes / 1MB, 2)
Write-Host "    APK Location: $($apkItem.FullName)"
Write-Host "    APK File Size: $apkSizeBytes bytes ($apkSizeMb MB)"

if ($apkSizeBytes -gt 15MB) {
    Write-Error "APK file size ($apkSizeMb MB) exceeds the 15 MB budget ceiling!"
    exit 1
}
Write-Host "    [OK] APK size budget compliant (< 15 MB)."

# 3. Locate Android SDK Build Tools
Write-Host "`n[*] Step 3: Locating Android SDK Build Tools ($BuildToolsVersion)..."
$buildToolsDir = "$env:LOCALAPPDATA\Android\Sdk\build-tools\$BuildToolsVersion"
if (-not (Test-Path $buildToolsDir)) {
    # Fallback search
    $available = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" | Sort-Object Name -Descending | Select-Object -First 1
    if ($available) {
        $buildToolsDir = $available.FullName
    } else {
        Write-Error "Android SDK build-tools not found!"
        exit 1
    }
}
$zipalignPath = Join-Path $buildToolsDir "zipalign.exe"
$apksignerPath = Join-Path $buildToolsDir "apksigner.bat"
Write-Host "    Using zipalign:  $zipalignPath"
Write-Host "    Using apksigner: $apksignerPath"

# 4. Verify 4-Byte Zip Alignment
Write-Host "`n[*] Step 4: Validating 4-byte zip alignment (zipalign -c 4)..."
& $zipalignPath -c 4 $ApkPath
if ($LASTEXITCODE -ne 0) {
    Write-Error "APK is NOT 4-byte zipaligned!"
    exit 1
}
Write-Host "    [OK] APK passes 4-byte zip alignment."

# 5. Verify Cryptographic Signatures (v2 / v3)
Write-Host "`n[*] Step 5: Validating APK signature scheme (apksigner verify)..."
$signerOutput = & $apksignerPath verify --verbose --print-certs $ApkPath
$signerText = $signerOutput -join "`n"
Write-Host $signerText

if ($signerText -notmatch "Verified using v2 scheme \(APK Signature Scheme v2\): true") {
    Write-Error "APK lacks valid v2 signature!"
    exit 1
}
if ($signerText -notmatch "Verified using v3 scheme \(APK Signature Scheme v3\): true") {
    Write-Error "APK lacks valid v3 signature!"
    exit 1
}
if ($signerText -notmatch "CN=Unravel Beta") {
    Write-Error "APK is not signed by Unravel Beta certificate!"
    exit 1
}
Write-Host "    [OK] APK signed with valid v2/v3 signatures by Unravel Beta certificate."

# 6. Verify SHA-256 Checksum
Write-Host "`n[*] Step 6: Verifying SHA-256 Checksum Integrity..."
$computedHash = (Get-FileHash -Path $ApkPath -Algorithm SHA256).Hash.ToLower()
$recordedContent = Get-Content $Sha256Path -Raw
$recordedHash = ($recordedContent.Trim().Split(" ")[0]).ToLower()

Write-Host "    Computed Hash: $computedHash"
Write-Host "    Recorded Hash: $recordedHash"

if ($computedHash -ne $recordedHash) {
    Write-Error "SHA-256 hash mismatch! Computed: $computedHash vs Recorded: $recordedHash"
    exit 1
}
Write-Host "    [OK] SHA-256 checksum matches recorded hash."

# 7. Sideload & Cold Boot Verification on Emulator
if ($InstallOnEmulator) {
    Write-Host "`n[*] Step 7: Performing Live Emulator Sideload Test..."
    $devicesText = (adb devices) -join "`n"
    if ($devicesText -notmatch "\tdevice") {
        Write-Warning "No active ADB device detected. Skipping live sideload."
    } else {
        Write-Host "    Installing APK to active device/emulator..."
        # Clean reinstall to handle signature transition
        adb uninstall com.unravel.app 2>$null | Out-Null
        $installResult = adb install -r $ApkPath
        $installStr = $installResult -join " "
        Write-Host "    ADB Install: $installStr"
        if ($installStr -notmatch "Success") {
            Write-Error "Failed to install beta APK onto device: $installStr"
            exit 1
        }

        # Clear logcat and cold launch
        adb logcat -c
        $launchOutput = adb shell am start -S -W -n com.unravel.app/com.unravel.app.MainActivity
        Write-Host "    Cold Launch: $($launchOutput -join ' ')"

        Start-Sleep -Milliseconds 1500
        $crashes = adb logcat -d | Select-String -Pattern "FATAL EXCEPTION|AndroidRuntime|ClassNotFoundException|NoSuchMethodError"
        if ($crashes) {
            Write-Error "Runtime crash detected upon cold boot!`n$crashes"
            exit 1
        }
        Write-Host "    [OK] Live cold launch verified with 0 crashes."
    }
}

Write-Host "`n=========================================================================="
Write-Host " Beta Distribution Packaging Certified Complete!                         "
Write-Host " Release Artifact: $ApkPath ($apkSizeMb MB)                               "
Write-Host " SHA-256:          $computedHash                                          "
Write-Host "=========================================================================="
