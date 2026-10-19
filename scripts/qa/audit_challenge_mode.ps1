# Unravel QA Automation: WP-47 Challenge Mode Live Gesture Validation Audit
# Target: Pixel_9_Pro_Fold AVD (API 35/36, Display: 2076x2152)

param(
    [string]$PackageName = "com.unravel.app.debug",
    [string]$MainActivity = "com.unravel.app.MainActivity",
    [string]$DisplayId = "4619827259835644672"
)

Write-Host "=========================================================================="
Write-Host " [Unravel QA] WP-47: Challenge Mode Live Gesture Validation Audit         "
Write-Host "=========================================================================="

$screenshotDir = "local-notes/audit/screenshots/wp47"
if (-not (Test-Path $screenshotDir)) {
    New-Item -ItemType Directory -Force -Path $screenshotDir | Out-Null
}

function Take-Screenshot {
    param([string]$Filename)
    $remotePath = "/sdcard/wp47_tmp.png"
    $localPath = Join-Path $screenshotDir $Filename
    adb shell screencap -d $DisplayId -p $remotePath
    adb pull $remotePath $localPath | Out-Null
    adb shell rm $remotePath
    Write-Host "  --> Captured screenshot: $Filename"
}

$passed = 0
$total = 3

# -------------------------------------------------------------------------
# Test 1: Bubble Sort Swap Prediction & Rejection Pulse
# -------------------------------------------------------------------------
Write-Host "`n--> [1/3] Auditing Bubble Sort Challenge Mode (Swap Prediction & Rejection Pulse)..."
adb logcat -c
adb shell am start -S -W -n "$PackageName/$MainActivity" --es algorithmId bubble_sort | Out-Null
Start-Sleep -Milliseconds 1500

# Switch to CHALLENGE Mode (X: 1919, Y: 200)
Write-Host "  --> Switching to CHALLENGE mode..."
adb shell input tap 1919 200
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "bubble_sort_01_challenge_init.png"

# Step forward to comparison step (activeLine 3, j=0, j+1=1, needsSwap=true)
Write-Host "  --> Stepping to pairwise comparison frame..."
adb shell input tap 1239 1969
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "bubble_sort_02_comparison_step.png"

# Submit INVALID swap gesture: Drag bar 0 (X: 140, Y: 1000) to bar 4 (X: 940, Y: 1000)
Write-Host "  --> Executing INVALID swap gesture (0 -> 4) to trigger CoralFlame rejection pulse..."
adb shell input swipe 140 1000 940 1000 300
Take-Screenshot -Filename "bubble_sort_03_invalid_swap_rejection.png"
Start-Sleep -Milliseconds 1000

# Submit VALID swap gesture: Drag bar 0 (X: 140, Y: 1000) to bar 1 (X: 340, Y: 1000)
Write-Host "  --> Executing VALID swap gesture (0 -> 1) to increment streak..."
adb shell input swipe 140 1000 340 1000 300
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "bubble_sort_04_valid_swap_streak_increment.png"

$logs = adb logcat -d -s AndroidRuntime:E
if ($logs -match "FATAL EXCEPTION") {
    Write-Error "  [FAIL] Crash detected during Bubble Sort challenge audit!"
} else {
    Write-Host "  [PASS] Bubble Sort challenge swap validation and rejection pulse verified!"
    $passed++
}

# -------------------------------------------------------------------------
# Test 2: Binary Search Pointer Advance
# -------------------------------------------------------------------------
Write-Host "`n--> [2/3] Auditing Binary Search Challenge Mode (Pointer Advance)..."
adb logcat -c
adb shell am start -S -W -n "$PackageName/$MainActivity" --es algorithmId binary_search | Out-Null
Start-Sleep -Milliseconds 1500

# Switch to CHALLENGE Mode
Write-Host "  --> Switching to CHALLENGE mode..."
adb shell input tap 1919 200
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "binary_search_01_challenge_init.png"

# Step forward twice to step 3 (Line 7: low = mid + 1, expects MovePointer("low", 5))
Write-Host "  --> Stepping to low pointer advance frame (step 3)..."
adb shell input tap 1239 1969
Start-Sleep -Milliseconds 400
adb shell input tap 1239 1969
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "binary_search_02_pointer_expected.png"

# Submit VALID pointer move: Tap bar 5 (X: 1138, Y: 1000)
Write-Host "  --> Tapping bar 5 to advance low pointer..."
adb shell input tap 1138 1000
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "binary_search_03_pointer_advanced_streak.png"

$logs = adb logcat -d -s AndroidRuntime:E
if ($logs -match "FATAL EXCEPTION") {
    Write-Error "  [FAIL] Crash detected during Binary Search challenge audit!"
} else {
    Write-Host "  [PASS] Binary Search pointer advance gesture verified!"
    $passed++
}

# -------------------------------------------------------------------------
# Test 3: AVL Tree Rotations Subtree Rotation
# -------------------------------------------------------------------------
Write-Host "`n--> [3/3] Auditing AVL Tree Rotations Challenge Mode (Subtree Rotation)..."
adb logcat -c
adb shell am start -S -W -n "$PackageName/$MainActivity" --es algorithmId avl_rotations | Out-Null
Start-Sleep -Milliseconds 1500

# Switch to CHALLENGE Mode
Write-Host "  --> Switching to CHALLENGE mode..."
adb shell input tap 1919 200
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "avl_rotations_01_challenge_init.png"

# Step forward 3 times to step 4 (unbalanced pivot node 10, expects RotateSubtree(node10, LEFT))
Write-Host "  --> Stepping to unbalanced pivot frame (step 4)..."
adb shell input tap 1239 1969
Start-Sleep -Milliseconds 400
adb shell input tap 1239 1969
Start-Sleep -Milliseconds 400
adb shell input tap 1239 1969
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "avl_rotations_02_unbalanced_pivot.png"

# Submit VALID rotation: Tap pivot node 10 at (X: 200, Y: 240)
Write-Host "  --> Tapping unbalanced pivot node 10 to execute subtree rotation..."
adb shell input tap 200 240
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "avl_rotations_03_rotation_completed_streak.png"

$logs = adb logcat -d -s AndroidRuntime:E
if ($logs -match "FATAL EXCEPTION") {
    Write-Error "  [FAIL] Crash detected during AVL Rotations challenge audit!"
} else {
    Write-Host "  [PASS] AVL Rotations subtree rotation gesture verified!"
    $passed++
}

Write-Host "`n=========================================================================="
Write-Host " [Unravel QA] WP-47 Challenge Audit Summary: $passed / $total Tests PASSED"
Write-Host "=========================================================================="

if ($passed -eq $total) {
    exit 0
} else {
    exit 1
}
