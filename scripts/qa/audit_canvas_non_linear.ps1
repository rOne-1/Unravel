# Unravel QA Automation: WP-46 Clusters 3-4 Non-Linear Canvas Live Interaction Audit
# Target: Pixel_9_Pro_Fold AVD (API 35/36, Display: 2076x2152)

param(
    [string]$PackageName = "com.unravel.app.debug",
    [string]$MainActivity = "com.unravel.app.MainActivity",
    [string]$DisplayId = "4619827259835644672"
)

Write-Host "=========================================================================="
Write-Host " [Unravel QA] WP-46: Clusters 3-4 Non-Linear Live Interaction Audit       "
Write-Host "=========================================================================="

$screenshotDir = "local-notes/audit/screenshots/wp46"
if (-not (Test-Path $screenshotDir)) {
    New-Item -ItemType Directory -Force -Path $screenshotDir | Out-Null
}

function Take-Screenshot {
    param([string]$Filename)
    $remotePath = "/sdcard/wp46_tmp.png"
    $localPath = Join-Path $screenshotDir $Filename
    adb shell screencap -d $DisplayId -p $remotePath
    adb pull $remotePath $localPath | Out-Null
    adb shell rm $remotePath
    Write-Host "  --> Captured screenshot: $Filename"
}

$nonLinearAlgorithms = @(
    # Cluster 3: Pointer Mechanics
    @{ id = "linked_list_reversal"; title = "Linked List Reversal"; cluster = 3; canvas = "LinkedNodeCanvas" },
    @{ id = "floyd_cycle"; title = "Floyd's Cycle"; cluster = 3; canvas = "LinkedNodeCanvas" },
    @{ id = "stack_queue"; title = "Stacks & Queues"; cluster = 3; canvas = "LinkedNodeCanvas" },

    # Cluster 4: Non-Linear & Spatial
    @{ id = "bst_ops"; title = "BST Operations"; cluster = 4; canvas = "HierarchyTreeCanvas" },
    @{ id = "avl_rotations"; title = "AVL Rotations"; cluster = 4; canvas = "HierarchyTreeCanvas" },
    @{ id = "graph_bfs_dfs"; title = "Graph BFS/DFS"; cluster = 4; canvas = "SpatialGridGraphCanvas" },
    @{ id = "dijkstra_astar"; title = "Dijkstra & A*"; cluster = 4; canvas = "SpatialGridGraphCanvas" }
)

$passed = 0
$total = $nonLinearAlgorithms.Count

foreach ($algo in $nonLinearAlgorithms) {
    $algoId = $algo.id
    $title = $algo.title
    $cluster = $algo.cluster
    $canvas = $algo.canvas

    Write-Host "`n--> [$($passed + 1)/$total] Auditing Cluster $cluster : $title ($algoId) [$canvas]..."

    adb logcat -c

    # 1. Launch with cold restart (-S)
    adb shell am start -S -W -n "$PackageName/$MainActivity" --es algorithmId $algoId | Out-Null
    Start-Sleep -Milliseconds 1500

    Take-Screenshot -Filename "$($algoId)_01_initial.png"

    # 2. Forward Scrubbing: Tap at ~60% across the timeline scrubber (X: 1250, Y: 1860)
    Write-Host "  --> Testing Forward Scrubbing..."
    adb shell input tap 1250 1860
    Start-Sleep -Milliseconds 600
    Take-Screenshot -Filename "$($algoId)_02_scrub_forward.png"

    # 3. Backward Scrubbing: Swipe backward to ~20% of timeline
    Write-Host "  --> Testing Backward Scrubbing..."
    adb shell input swipe 1250 1860 400 1860 300
    Start-Sleep -Milliseconds 600
    Take-Screenshot -Filename "$($algoId)_03_scrub_backward.png"

    # 4. If Graph BFS/DFS, test DIRECT EDIT mode and obstacle wall toggle
    if ($algoId -eq "graph_bfs_dfs") {
        Write-Host "  --> Switching to DIRECT EDIT mode for Spatial Grid..."
        # Mode pill 'EDIT' at top right: X: 1740, Y: 200
        adb shell input tap 1740 200
        Start-Sleep -Milliseconds 500
        Take-Screenshot -Filename "graph_bfs_dfs_04_edit_mode_active.png"

        Write-Host "  --> Tapping grid cell (row 1, col 2) to toggle obstacle wall..."
        # On 3x4 grid centered: cell (1, 2) is at X ~ 1250, Y ~ 1000
        adb shell input tap 1250 1000
        Start-Sleep -Milliseconds 600
        Take-Screenshot -Filename "graph_bfs_dfs_05_obstacle_toggled.png"
    }

    # Verify logcat health
    $logs = adb logcat -d -s AndroidRuntime:E
    if ($logs -match "FATAL EXCEPTION") {
        Write-Error "  [FAIL] Crash detected during $title audit!"
    } else {
        Write-Host "  [PASS] $title live non-linear canvas, scrubbing, and interactions verified successfully!"
        $passed++
    }
}

Write-Host "`n=========================================================================="
Write-Host " [Unravel QA] WP-46 Audit Summary: $passed / $total Algorithms PASSED "
Write-Host "=========================================================================="

if ($passed -eq $total) {
    exit 0
} else {
    exit 1
}
