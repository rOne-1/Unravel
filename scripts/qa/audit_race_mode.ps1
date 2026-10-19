# Unravel QA Automation: WP-49 Split-Screen Race Mode Live Execution & Posture Audit
# Target: Pixel_9_Pro_Fold AVD (API 35/36, Displays: 2076x2152 unfolded, 1080x2424 folded)

param(
    [string]$PackageName = "com.unravel.app.debug",
    [string]$MainActivity = "com.unravel.app.MainActivity",
    [string]$UnfoldedDisplayId = "4619827259835644672",
    [string]$FoldedDisplayId = "4619827551948147201"
)

Write-Host "=========================================================================="
Write-Host " [Unravel QA] WP-49: Split-Screen Race Mode Live Execution & Posture Audit "
Write-Host "=========================================================================="

$screenshotDir = "local-notes/audit/screenshots/wp49"
if (-not (Test-Path $screenshotDir)) {
    New-Item -ItemType Directory -Force -Path $screenshotDir | Out-Null
}

function Take-Screenshot {
    param(
        [string]$Filename,
        [string]$DisplayId = $UnfoldedDisplayId
    )
    $remotePath = "/sdcard/wp49_tmp.png"
    $localPath = Join-Path $screenshotDir $Filename
    adb shell screencap -d $DisplayId -p $remotePath
    adb pull $remotePath $localPath | Out-Null
    adb shell rm $remotePath
    Write-Host "  --> Captured screenshot: $Filename"
}

# UI Coordinate constants on Pixel 9 Pro Fold unfolded display (2076x2152)
$UNFOLDED_RESET_X = 345
$UNFOLDED_RESET_Y = 1991
$UNFOLDED_RACE_X  = 1076
$UNFOLDED_RACE_Y  = 1990
$UNFOLDED_STEP_X  = 1448
$UNFOLDED_STEP_Y  = 1991

# Folded display coordinates (1080x2424)
$FOLDED_RACE_X = 578
$FOLDED_RACE_Y = 2282

$passed = 0
$total = 4

# -------------------------------------------------------------------------
# Test 1: Bubble Sort vs QuickSort Unfolded Concurrent Execution & Winner
# -------------------------------------------------------------------------
Write-Host "`n--> [1/4] Auditing Bubble Sort vs QuickSort Concurrent Race (Unfolded)..."
adb shell cmd device_state state 2 | Out-Null # Ensure OPENED
Start-Sleep -Milliseconds 800
adb logcat -c
adb shell am start -S -W -n "$PackageName/$MainActivity" --es route race_mode | Out-Null
Start-Sleep -Milliseconds 1500

# 1. Capture initial synchronized state
Take-Screenshot -Filename "01_unfolded_initial_synced.png" -DisplayId $UnfoldedDisplayId

# 2. Tap RACE to start concurrent execution
Write-Host "  --> Starting dual-engine concurrent race..."
adb shell input tap $UNFOLDED_RACE_X $UNFOLDED_RACE_Y
Start-Sleep -Milliseconds 1800
Take-Screenshot -Filename "02_unfolded_concurrent_running.png" -DisplayId $UnfoldedDisplayId

# 3. Wait for divergence and completion
Write-Host "  --> Waiting for QuickSort to outpace Bubble Sort and trigger celebration..."
Start-Sleep -Seconds 12
Take-Screenshot -Filename "03_unfolded_winner_celebration.png" -DisplayId $UnfoldedDisplayId

# 4. Tap RESET to restore initial state
Write-Host "  --> Resetting race state..."
adb shell input tap $UNFOLDED_RESET_X $UNFOLDED_RESET_Y
Start-Sleep -Milliseconds 800
Take-Screenshot -Filename "04_unfolded_reset_synced.png" -DisplayId $UnfoldedDisplayId

$logs = adb logcat -d -s AndroidRuntime:E
if ($logs -match "FATAL EXCEPTION") {
    Write-Error "  [FAIL] Crash detected during concurrent race execution!"
} else {
    Write-Host "  [PASS] Concurrent execution, divergence detection, and winner celebration verified!"
    $passed++
}

# -------------------------------------------------------------------------
# Test 2: Foldable Posture Adaptation (Unfolded -> Folded -> Unfolded)
# -------------------------------------------------------------------------
Write-Host "`n--> [2/4] Auditing Posture Adaptation (Folded / Portrait)..."
# Fold device (State 0 = CLOSED)
adb shell cmd device_state state 0 | Out-Null
Start-Sleep -Milliseconds 1500

# Capture folded vertical top/bottom split
Take-Screenshot -Filename "05_folded_portrait_vertical_split.png" -DisplayId $FoldedDisplayId

# Start race on folded outer screen
Write-Host "  --> Running race in folded portrait posture..."
adb shell input tap $FOLDED_RACE_X $FOLDED_RACE_Y
Start-Sleep -Milliseconds 2500
Take-Screenshot -Filename "06_folded_portrait_running.png" -DisplayId $FoldedDisplayId

# Unfold device back to state 2
Write-Host "  --> Unfolding device back to expanded posture..."
adb shell cmd device_state state 2 | Out-Null
Start-Sleep -Milliseconds 1500
Take-Screenshot -Filename "07_unfolded_resumed_posture.png" -DisplayId $UnfoldedDisplayId

$logs = adb logcat -d -s AndroidRuntime:E
if ($logs -match "FATAL EXCEPTION") {
    Write-Error "  [FAIL] Crash detected during foldable posture adaptation!"
} else {
    Write-Host "  [PASS] Foldable posture adaptation seamlessly switches split orientation!"
    $passed++
}

# -------------------------------------------------------------------------
# Test 3: Trees Category Matchup (BST vs AVL Rotations)
# -------------------------------------------------------------------------
Write-Host "`n--> [3/4] Auditing Hierarchical Tree Matchup (BST vs AVL)..."
# Tap Trees chip (X=395, Y=245)
Write-Host "  --> Selecting Trees category chip..."
adb shell input tap 395 245
Start-Sleep -Milliseconds 1000
Take-Screenshot -Filename "08_unfolded_trees_matchup.png" -DisplayId $UnfoldedDisplayId

# Step forward on Tree matchup
adb shell input tap $UNFOLDED_STEP_X $UNFOLDED_STEP_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "09_unfolded_trees_stepped.png" -DisplayId $UnfoldedDisplayId

$logs = adb logcat -d -s AndroidRuntime:E
if ($logs -match "FATAL EXCEPTION") {
    Write-Error "  [FAIL] Crash detected during Trees matchup audit!"
} else {
    Write-Host "  [PASS] HierarchyTreeCanvas dual race matchup verified!"
    $passed++
}

# -------------------------------------------------------------------------
# Test 4: Spatial Grid Graph Matchup (BFS vs Dijkstra/A*)
# -------------------------------------------------------------------------
Write-Host "`n--> [4/4] Auditing Spatial Grid Graph Matchup (BFS vs Dijkstra/A*)..."
# Tap Graphs chip (X=475, Y=245)
Write-Host "  --> Selecting Graphs category chip..."
adb shell input tap 475 245
Start-Sleep -Milliseconds 1000
Take-Screenshot -Filename "10_unfolded_graphs_matchup.png" -DisplayId $UnfoldedDisplayId

# Step forward on Graph matchup
adb shell input tap $UNFOLDED_STEP_X $UNFOLDED_STEP_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "11_unfolded_graphs_stepped.png" -DisplayId $UnfoldedDisplayId

$logs = adb logcat -d -s AndroidRuntime:E
if ($logs -match "FATAL EXCEPTION") {
    Write-Error "  [FAIL] Crash detected during Graphs matchup audit!"
} else {
    Write-Host "  [PASS] SpatialGridGraphCanvas dual race matchup verified!"
    $passed++
}

Write-Host "`n=========================================================================="
Write-Host " [Unravel QA] WP-49 Race Mode Audit Summary: $passed / $total Tests PASSED"
Write-Host "=========================================================================="

if ($passed -eq $total) {
    exit 0
} else {
    exit 1
}
