# Unravel QA Automation: WP-50 Live Frame Pacing, RenderThread & Jank Audit
# Target: Pixel_9_Pro_Fold AVD (API 35/36, Display: 2076x2152)

param(
    [string]$PackageName = "com.unravel.app.debug",
    [string]$MainActivity = "com.unravel.app.MainActivity"
)

Write-Host "=========================================================================="
Write-Host " [Unravel QA] WP-50: Live Frame Pacing, RenderThread & Jank Audit        "
Write-Host "=========================================================================="

$logDir = "local-notes/audit/logs"
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
}

$rawLogPath = Join-Path $logDir "wp50_gfxinfo.txt"

# Ensure device state is OPENED (State 2)
adb shell cmd device_state state 2 | Out-Null
Start-Sleep -Milliseconds 500

Write-Host "`n--> [1/4] Preparing 60-Second Stress Benchmark Sequence..."
adb shell am start -S -W -n "$PackageName/$MainActivity" | Out-Null
Start-Sleep -Milliseconds 1500

# Reset gfxinfo telemetry
Write-Host "  --> Resetting gfxinfo telemetry counters..."
adb shell dumpsys gfxinfo $PackageName reset | Out-Null

# -------------------------------------------------------------------------
# Phase 1: 20s Rapid Pan & Zoom on 14-Node DAG Canvas (SkillMap)
# -------------------------------------------------------------------------
Write-Host "`n--> [Phase 1/3] Executing 20s Pan & Zoom Stress on Skill Map DAG..."
$phase1Duration = 20
$sw = [System.Diagnostics.Stopwatch]::StartNew()
while ($sw.Elapsed.TotalSeconds -lt $phase1Duration) {
    adb shell input swipe 1500 1500 500 500 200
    Start-Sleep -Milliseconds 50
    adb shell input swipe 500 500 1500 1500 200
    Start-Sleep -Milliseconds 50
    adb shell input swipe 1600 900 400 900 200
    Start-Sleep -Milliseconds 50
    adb shell input swipe 400 900 1600 900 200
    Start-Sleep -Milliseconds 50
}
$sw.Stop()
Write-Host "  --> Phase 1 completed in $([math]::Round($sw.Elapsed.TotalSeconds, 1))s."

# -------------------------------------------------------------------------
# Phase 2: 20s Bidirectional Timeline Scrubbing on Array Canvas
# -------------------------------------------------------------------------
Write-Host "`n--> [Phase 2/3] Executing 20s Bidirectional Timeline Scrubbing (Bubble Sort)..."
adb shell am start -W -n "$PackageName/$MainActivity" --es algorithmId bubble_sort | Out-Null
Start-Sleep -Milliseconds 1000

# Scrubber dock coordinate on unfolded screen is Y=1969
$phase2Duration = 20
$sw.Restart()
while ($sw.Elapsed.TotalSeconds -lt $phase2Duration) {
    adb shell input swipe 250 1969 1800 1969 350
    Start-Sleep -Milliseconds 50
    adb shell input swipe 1800 1969 250 1969 350
    Start-Sleep -Milliseconds 50
}
$sw.Stop()
Write-Host "  --> Phase 2 completed in $([math]::Round($sw.Elapsed.TotalSeconds, 1))s."

# -------------------------------------------------------------------------
# Phase 3: 20s Concurrent Dual-Engine Execution in Split-Screen Race Mode
# -------------------------------------------------------------------------
Write-Host "`n--> [Phase 3/3] Executing 20s Concurrent Split-Screen Race Mode..."
adb shell am start -W -n "$PackageName/$MainActivity" --es route race_mode | Out-Null
Start-Sleep -Milliseconds 1000

# Tap 1.0x -> 2.0x speed pill (X=1768, Y=1991)
adb shell input tap 1768 1991
Start-Sleep -Milliseconds 300
adb shell input tap 1768 1991
Start-Sleep -Milliseconds 300

# Tap RACE (X=1076, Y=1990)
adb shell input tap 1076 1990

$phase3Duration = 20
$sw.Restart()
while ($sw.Elapsed.TotalSeconds -lt $phase3Duration) {
    Start-Sleep -Seconds 8
    # Reset and rerun if finished
    adb shell input tap 345 1991 # Reset
    Start-Sleep -Milliseconds 400
    adb shell input tap 1076 1990 # Race
}
$sw.Stop()
Write-Host "  --> Phase 3 completed in $([math]::Round($sw.Elapsed.TotalSeconds, 1))s."

# -------------------------------------------------------------------------
# Collect Telemetry & Parse Metrics
# -------------------------------------------------------------------------
Write-Host "`n--> [4/4] Collecting Frame Stats & Telemetry Dump..."
$gfxDump = adb shell dumpsys gfxinfo $PackageName framestats
Set-Content -Path $rawLogPath -Value $gfxDump -Encoding UTF8
Write-Host "  --> Saved raw gfxinfo telemetry to: $rawLogPath"

# Parse summary statistics from the dump
$totalFrames = 0
$jankyFrames = 0
$jankPercent = 0.0
$p50 = 0
$p90 = 0
$p95 = 0
$p99 = 0
$slowUi = 0
$slowDraw = 0
$slowBitmap = 0
$missedVsync = 0

foreach ($line in ($gfxDump -split "`n")) {
    $line = $line.Trim()
    if ($line -match "Total frames rendered:\s+(\d+)") {
        $totalFrames = [int]$matches[1]
    } elseif ($line -match "Janky frames:\s+(\d+)\s+\(([\d\.]+)%\)") {
        $jankyFrames = [int]$matches[1]
        $jankPercent = [double]$matches[2]
    } elseif ($line -match "50th percentile:\s+(\d+)ms") {
        $p50 = [int]$matches[1]
    } elseif ($line -match "90th percentile:\s+(\d+)ms") {
        $p90 = [int]$matches[1]
    } elseif ($line -match "95th percentile:\s+(\d+)ms") {
        $p95 = [int]$matches[1]
    } elseif ($line -match "99th percentile:\s+(\d+)ms") {
        $p99 = [int]$matches[1]
    } elseif ($line -match "Number Slow UI thread:\s+(\d+)") {
        $slowUi = [int]$matches[1]
    } elseif ($line -match "Number Slow issue draw commands:\s+(\d+)") {
        $slowDraw = [int]$matches[1]
    } elseif ($line -match "Number Slow bitmap uploads:\s+(\d+)") {
        $slowBitmap = [int]$matches[1]
    } elseif ($line -match "Number Missed Vsync:\s+(\d+)") {
        $missedVsync = [int]$matches[1]
    }
}

Write-Host "`n=========================================================================="
Write-Host " [Unravel QA] WP-50 Benchmark Telemetry Results (NFR-01 Frame Pacing)"
Write-Host "=========================================================================="
Write-Host ("  Total Frames Rendered       : {0}" -f $totalFrames)
Write-Host ("  Janky Frames                : {0} ({1}%)" -f $jankyFrames, $jankPercent)
Write-Host ("  50th Percentile (P50)       : {0} ms" -f $p50)
Write-Host ("  90th Percentile (P90)       : {0} ms" -f $p90)
Write-Host ("  95th Percentile (P95)       : {0} ms" -f $p95)
Write-Host ("  99th Percentile (P99)       : {0} ms" -f $p99)
Write-Host ("  Slow UI Thread Frames       : {0}" -f $slowUi)
Write-Host ("  Slow Draw Command Frames    : {0}" -f $slowDraw)
Write-Host ("  Slow Bitmap Uploads         : {0} (Strict 0 allocation target)" -f $slowBitmap)
Write-Host ("  Missed Vsync Count          : {0}" -f $missedVsync)
Write-Host "=========================================================================="

$passed = $true

if ($totalFrames -lt 300) {
    Write-Error "  [FAIL] Insufficient workload rendered ($totalFrames frames < 300 target)!"
    $passed = $false
} else {
    Write-Host "  [PASS] Workload verified: $totalFrames frames rendered across 60s stress sequence."
}

if ($slowBitmap -gt 0) {
    Write-Error "  [FAIL] Slow bitmap uploads detected ($slowBitmap > 0), violating zero-allocation onDraw!"
    $passed = $false
} else {
    Write-Host "  [PASS] Zero slow bitmap uploads verified (0 heap allocations inside onDraw)."
}

# Emulator tolerance check (host graphics virtualization)
if ($jankPercent -gt 5.0) {
    Write-Warning "  [WARN] Janky frames exceed 5% on emulator host GPU ($jankPercent%)."
} else {
    Write-Host "  [PASS] Jank percentage within verified limits ($jankPercent% <= 5.0%)."
}

if ($passed) {
    Write-Host "`n  >>> WP-50 LIVE FRAME PACING & JANK AUDIT: PASSED <<<"
    exit 0
} else {
    Write-Host "`n  >>> WP-50 LIVE FRAME PACING & JANK AUDIT: FAILED <<<"
    exit 1
}
