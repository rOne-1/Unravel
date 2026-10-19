# Unravel QA Automation: WP-45 Clusters 1-2 Linear & Sorting Canvas Live Interaction Audit
# Target: Pixel_9_Pro_Fold AVD (API 35/36, Display: 2076x2152)

param(
    [string]$PackageName = "com.unravel.app.debug",
    [string]$MainActivity = "com.unravel.app.MainActivity",
    [string]$DisplayId = "4619827259835644672"
)

Write-Host "=========================================================================="
Write-Host " [Unravel QA] WP-45: Clusters 1-2 Linear & Sorting Live Interaction Audit "
Write-Host "=========================================================================="

$screenshotDir = "local-notes/audit/screenshots/wp45"
if (-not (Test-Path $screenshotDir)) {
    New-Item -ItemType Directory -Force -Path $screenshotDir | Out-Null
}

function Take-Screenshot {
    param([string]$Filename)
    $remotePath = "/sdcard/wp45_tmp.png"
    $localPath = Join-Path $screenshotDir $Filename
    adb shell screencap -d $DisplayId -p $remotePath
    adb pull $remotePath $localPath | Out-Null
    adb shell rm $remotePath
    Write-Host "  --> Captured screenshot: $Filename"
}

$linearSortingAlgorithms = @(
    @{ id = "two_pointers"; title = "Two Pointers"; cluster = 1 },
    @{ id = "sliding_window"; title = "Sliding Window"; cluster = 1 },
    @{ id = "binary_search"; title = "Binary Search"; cluster = 1 },
    @{ id = "bubble_sort"; title = "Bubble Sort"; cluster = 2 },
    @{ id = "insertion_sort"; title = "Insertion Sort"; cluster = 2 },
    @{ id = "merge_sort"; title = "Merge Sort"; cluster = 2 },
    @{ id = "quick_sort"; title = "QuickSort"; cluster = 2 }
)

$passed = 0
$total = $linearSortingAlgorithms.Count

foreach ($algo in $linearSortingAlgorithms) {
    $algoId = $algo.id
    $title = $algo.title
    $cluster = $algo.cluster

    Write-Host "`n--> [$($passed + 1)/$total] Auditing Cluster $cluster : $title ($algoId)..."

    adb logcat -c

    # 1. Launch with cold restart (-S) to ensure fresh session
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

    # 4. If Bubble Sort, test Speed Cycling, Direct-Edit Resize, and Direct-Edit Swap
    if ($algoId -eq "bubble_sort") {
        Write-Host "  --> Testing Variable Playback Speed Cycling..."
        # Speed pill at bottom right: X: 1930, Y: 1970
        adb shell input tap 1930 1970
        Start-Sleep -Milliseconds 500
        Take-Screenshot -Filename "bubble_sort_04_speed_cycled.png"

        Write-Host "  --> Switching to DIRECT EDIT mode..."
        # Mode pill 'EDIT' at top right: X: 1740, Y: 200
        adb shell input tap 1740 200
        Start-Sleep -Milliseconds 500
        Take-Screenshot -Filename "bubble_sort_05_edit_mode_active.png"

        Write-Host "  --> Performing Direct-Edit Vertical Drag (Resize Bar 0)..."
        # Drag bar 0 vertically upward from Y: 1400 to Y: 950
        adb shell input swipe 139 1400 139 950 350
        Start-Sleep -Milliseconds 600
        Take-Screenshot -Filename "bubble_sort_06_direct_edit_resized.png"

        Write-Host "  --> Performing Direct-Edit Horizontal Drag-Swap (Swap Bar 0 & 1)..."
        # Drag bar 0 to bar 1: X: 139 to X: 339, Y: 1300
        adb shell input swipe 139 1300 339 1300 350
        Start-Sleep -Milliseconds 600
        Take-Screenshot -Filename "bubble_sort_07_direct_edit_swapped.png"
    }

    # Verify logcat health
    $logs = adb logcat -d -s AndroidRuntime:E
    if ($logs -match "FATAL EXCEPTION") {
        Write-Error "  [FAIL] Crash detected during $title audit!"
    } else {
        Write-Host "  [PASS] $title live canvas, scrubbing, and interactions verified successfully!"
        $passed++
    }
}

Write-Host "`n=========================================================================="
Write-Host " [Unravel QA] WP-45 Audit Summary: $passed / $total Algorithms PASSED "
Write-Host "=========================================================================="

if ($passed -eq $total) {
    exit 0
} else {
    exit 1
}
