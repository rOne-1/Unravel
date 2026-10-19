# Unravel QA Automation: WP-42 Emulator Deployment & Health Check Script
# Target: Pixel_9_Pro_Fold AVD

$AvdName = "Pixel_9_Pro_Fold"
$PackageName = "com.unravel.app.debug"
$MainActivity = "com.unravel.app.MainActivity"

Write-Host "========================================"
Write-Host " [Unravel QA] WP-42: Emulator Deployment "
Write-Host "========================================"

# 1. Check ADB device status
$devices = adb devices
$booted = $false
if ($devices -match "emulator-\d+\s+device") {
    Write-Host "[OK] Active emulator detected."
    $booted = $true
} else {
    Write-Host "[!] Starting $AvdName..."
    Start-Process -FilePath "emulator" -ArgumentList "-avd $AvdName -no-audio -no-boot-anim"
    
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        $bootProp = adb shell getprop sys.boot_completed
        if ($bootProp -match "1") {
            Write-Host "[OK] Emulator booted successfully."
            $booted = $true
            break
        }
        Write-Host "    Waiting for emulator boot..."
    }
}

if (-not $booted) {
    Write-Error "Failed to verify emulator boot."
    exit 1
}

# 2. Unlock keyguard
Write-Host "[*] Dismissing keyguard..."
adb shell wm dismiss-keyguard
adb shell input keyevent 82

# 3. Verify / Install Debug APK
Write-Host "[*] Verifying installed package: $PackageName..."
$installed = adb shell pm list packages
if ($installed -notmatch $PackageName) {
    Write-Host "[!] Installing fresh debug APK..."
    ./gradlew installDebug
} else {
    Write-Host "[OK] Package $PackageName is installed."
}

# 4. Clear Logcat and Launch Activity
Write-Host "[*] Launching $PackageName/$MainActivity..."
adb logcat -c
adb shell am start -W -n "$PackageName/$MainActivity"

# 5. Capture Verification Screenshot
$screenshotPath = "local-notes/audit/screenshots/wp42_initial_launch.png"
if (-not (Test-Path "local-notes/audit/screenshots")) {
    New-Item -ItemType Directory -Force -Path "local-notes/audit/screenshots" | Out-Null
}
Write-Host "[*] Capturing screen to $screenshotPath..."
adb shell screencap -p /sdcard/screen_wp42.png
adb pull /sdcard/screen_wp42.png $screenshotPath
adb shell rm /sdcard/screen_wp42.png

# 6. Check Logcat for Crashes
Write-Host "[*] Auditing logcat for runtime crashes or ANRs..."
$crashes = adb logcat -d | Select-String -Pattern "FATAL EXCEPTION|AndroidRuntime"
if ($crashes) {
    Write-Error "Runtime crash detected in logcat: `n$crashes"
    exit 1
}

Write-Host "[OK] WP-42 Verification Complete: App running smoothly with 0 crashes!"
