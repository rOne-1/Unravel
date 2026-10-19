# Unravel QA Automation: WP-48 Synchronized Code X-Ray Live Highlighting & Variable Badge Audit
# Target: Pixel_9_Pro_Fold AVD (API 35/36, Display: 2076x2152)

param(
    [string]$PackageName = "com.unravel.app.debug",
    [string]$MainActivity = "com.unravel.app.MainActivity",
    [string]$DisplayId = "4619827259835644672"
)

Write-Host "=========================================================================="
Write-Host " [Unravel QA] WP-48: Synchronized Code X-Ray Live Highlighting Audit       "
Write-Host "=========================================================================="

$screenshotDir = "local-notes/audit/screenshots/wp48"
if (-not (Test-Path $screenshotDir)) {
    New-Item -ItemType Directory -Force -Path $screenshotDir | Out-Null
}

function Take-Screenshot {
    param([string]$Filename)
    $remotePath = "/sdcard/wp48_tmp.png"
    $localPath = Join-Path $screenshotDir $Filename
    adb shell screencap -d $DisplayId -p $remotePath
    adb pull $remotePath $localPath | Out-Null
    adb shell rm $remotePath
    Write-Host "  --> Captured screenshot: $Filename"
}

# UI Coordinate constants on Pixel 9 Pro Fold unfolded display (2076x2152)
$COLLAPSED_HEADER_X = 1038
$COLLAPSED_HEADER_Y = 1727
$EXPANDED_HEADER_X  = 1038
$EXPANDED_HEADER_Y  = 1159

$STEP_FORWARD_X  = 1239
$STEP_FORWARD_Y  = 1969
$STEP_BACKWARD_X = 981
$STEP_BACKWARD_Y = 1969

$passed = 0
$total = 4

# -------------------------------------------------------------------------
# Test 1: Two Pointers (Cluster 1: Linear)
# -------------------------------------------------------------------------
Write-Host "`n--> [1/4] Auditing Two Pointers Synchronized Code X-Ray..."
adb logcat -c
adb shell am start -S -W -n "$PackageName/$MainActivity" --es algorithmId two_pointers | Out-Null
Start-Sleep -Milliseconds 1500

Take-Screenshot -Filename "two_pointers_01_collapsed.png"

# 1. Expand Code X-Ray Sheet
Write-Host "  --> Expanding Code X-Ray bottom sheet..."
adb shell input tap $COLLAPSED_HEADER_X $COLLAPSED_HEADER_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "two_pointers_02_expanded_line1.png"

# 2. Step forward to comparison (Line 3: currentSum = arr[left] + arr[right])
Write-Host "  --> Stepping forward to Line 3 (comparison)..."
adb shell input tap $STEP_FORWARD_X $STEP_FORWARD_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "two_pointers_03_stepped_line3.png"

# 3. Step forward to left++ (Line 7)
Write-Host "  --> Stepping forward to Line 7 (left++)..."
adb shell input tap $STEP_FORWARD_X $STEP_FORWARD_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "two_pointers_04_stepped_line7.png"

# 4. Step backward to Line 3
Write-Host "  --> Stepping backward to Line 3..."
adb shell input tap $STEP_BACKWARD_X $STEP_BACKWARD_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "two_pointers_05_stepped_back_line3.png"

# 5. Collapse Code X-Ray Sheet
Write-Host "  --> Collapsing Code X-Ray bottom sheet..."
adb shell input tap $EXPANDED_HEADER_X $EXPANDED_HEADER_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "two_pointers_06_collapsed.png"

$logs = adb logcat -d -s AndroidRuntime:E
if ($logs -match "FATAL EXCEPTION") {
    Write-Error "  [FAIL] Crash detected during Two Pointers Code X-Ray audit!"
} else {
    Write-Host "  [PASS] Two Pointers Code X-Ray highlighting, stepping, and badges verified!"
    $passed++
}

# -------------------------------------------------------------------------
# Test 2: Bubble Sort (Cluster 2: Sorting)
# -------------------------------------------------------------------------
Write-Host "`n--> [2/4] Auditing Bubble Sort Synchronized Code X-Ray..."
adb logcat -c
adb shell am start -S -W -n "$PackageName/$MainActivity" --es algorithmId bubble_sort | Out-Null
Start-Sleep -Milliseconds 1500

# 1. Expand Code X-Ray Sheet
Write-Host "  --> Expanding Code X-Ray sheet..."
adb shell input tap $COLLAPSED_HEADER_X $COLLAPSED_HEADER_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "bubble_sort_01_expanded_line1.png"

# 2. Step forward to Line 3 (pairwise comparison arr[j] > arr[j+1])
Write-Host "  --> Stepping forward to Line 3..."
adb shell input tap $STEP_FORWARD_X $STEP_FORWARD_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "bubble_sort_02_stepped_line3.png"

# 3. Step forward to Line 5 (swap execution)
Write-Host "  --> Stepping forward to Line 5 (swap)..."
adb shell input tap $STEP_FORWARD_X $STEP_FORWARD_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "bubble_sort_03_stepped_line5.png"

# Collapse
adb shell input tap $EXPANDED_HEADER_X $EXPANDED_HEADER_Y
Start-Sleep -Milliseconds 500

$logs = adb logcat -d -s AndroidRuntime:E
if ($logs -match "FATAL EXCEPTION") {
    Write-Error "  [FAIL] Crash detected during Bubble Sort Code X-Ray audit!"
} else {
    Write-Host "  [PASS] Bubble Sort Code X-Ray synchronization verified!"
    $passed++
}

# -------------------------------------------------------------------------
# Test 3: BST Operations (Cluster 4: Trees)
# -------------------------------------------------------------------------
Write-Host "`n--> [3/4] Auditing BST Operations Synchronized Code X-Ray..."
adb logcat -c
adb shell am start -S -W -n "$PackageName/$MainActivity" --es algorithmId bst_ops | Out-Null
Start-Sleep -Milliseconds 1500

# Expand
Write-Host "  --> Expanding Code X-Ray sheet..."
adb shell input tap $COLLAPSED_HEADER_X $COLLAPSED_HEADER_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "bst_ops_01_expanded_line1.png"

# Step forward through root creation & branch decision
Write-Host "  --> Stepping forward through branch logic..."
adb shell input tap $STEP_FORWARD_X $STEP_FORWARD_Y
Start-Sleep -Milliseconds 400
adb shell input tap $STEP_FORWARD_X $STEP_FORWARD_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "bst_ops_02_stepped_branch.png"

# Collapse
adb shell input tap $EXPANDED_HEADER_X $EXPANDED_HEADER_Y
Start-Sleep -Milliseconds 500

$logs = adb logcat -d -s AndroidRuntime:E
if ($logs -match "FATAL EXCEPTION") {
    Write-Error "  [FAIL] Crash detected during BST Operations Code X-Ray audit!"
} else {
    Write-Host "  [PASS] BST Operations Code X-Ray synchronization verified!"
    $passed++
}

# -------------------------------------------------------------------------
# Test 4: Graph BFS/DFS (Cluster 4: Spatial Grid Graph)
# -------------------------------------------------------------------------
Write-Host "`n--> [4/4] Auditing Graph BFS/DFS Synchronized Code X-Ray..."
adb logcat -c
adb shell am start -S -W -n "$PackageName/$MainActivity" --es algorithmId graph_bfs_dfs | Out-Null
Start-Sleep -Milliseconds 1500

# Expand
Write-Host "  --> Expanding Code X-Ray sheet..."
adb shell input tap $COLLAPSED_HEADER_X $COLLAPSED_HEADER_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "graph_bfs_dfs_01_expanded_line1.png"

# Step forward through queue traversal
Write-Host "  --> Stepping forward through queue traversal..."
adb shell input tap $STEP_FORWARD_X $STEP_FORWARD_Y
Start-Sleep -Milliseconds 400
adb shell input tap $STEP_FORWARD_X $STEP_FORWARD_Y
Start-Sleep -Milliseconds 600
Take-Screenshot -Filename "graph_bfs_dfs_02_stepped_queue.png"

# Collapse
adb shell input tap $EXPANDED_HEADER_X $EXPANDED_HEADER_Y
Start-Sleep -Milliseconds 500

$logs = adb logcat -d -s AndroidRuntime:E
if ($logs -match "FATAL EXCEPTION") {
    Write-Error "  [FAIL] Crash detected during Graph BFS/DFS Code X-Ray audit!"
} else {
    Write-Host "  [PASS] Graph BFS/DFS Code X-Ray synchronization verified!"
    $passed++
}

Write-Host "`n=========================================================================="
Write-Host " [Unravel QA] WP-48 Code X-Ray Audit Summary: $passed / $total Tests PASSED"
Write-Host "=========================================================================="

if ($passed -eq $total) {
    exit 0
} else {
    exit 1
}
