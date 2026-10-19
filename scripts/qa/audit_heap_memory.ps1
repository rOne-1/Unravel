# Unravel QA Automation: WP-52 Retained Heap Footprint & Memory Leak Audit
# Target: Pixel_9_Pro_Fold AVD (API 35/36)

param(
    [string]$PackageName = "com.unravel.app.debug",
    [string]$MainActivity = "com.unravel.app.MainActivity"
)

Write-Host "=========================================================================="
Write-Host " [Unravel QA] WP-52: Retained Heap Footprint & Memory Leak Audit          "
Write-Host "=========================================================================="

$logDir = "local-notes/audit/logs"
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
}

$rawLogPath = Join-Path $logDir "wp52_meminfo.txt"
$report = [System.Text.StringBuilder]::new()
$report.AppendLine("==========================================================================") | Out-Null
$report.AppendLine(" UNRAVEL MEMORY FOOTPRINT & LEAK AUDIT REPORT (NFR-02)") | Out-Null
$report.AppendLine(" Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')") | Out-Null
$report.AppendLine(" Target Package: $PackageName") | Out-Null
$report.AppendLine("==========================================================================`n") | Out-Null

function Trigger-GC {
    # am dumpheap forces full GC across all Dalvik/ART generations
    $tmpDump = "/data/local/tmp/qa_gc_trigger.hprof"
    adb shell am dumpheap -n $PackageName $tmpDump | Out-Null
    Start-Sleep -Milliseconds 800
    adb shell rm -f $tmpDump | Out-Null
}

function Get-MemInfoMetrics {
    $dump = adb shell dumpsys meminfo $PackageName
    $metrics = @{
        RawDump = $dump
        JavaHeapPss = 0
        NativeHeapPss = 0
        DalvikHeapAlloc = 0
        TotalPss = 0
        PrivateDirty = 0
        Activities = 0
        Views = 0
        ViewRootImpl = 0
    }

    foreach ($line in ($dump -split "`n")) {
        $line = $line.Trim()
        if ($line -match "Java Heap:\s+(\d+)") {
            $metrics.JavaHeapPss = [int]$matches[1]
        }
        if ($line -match "Native Heap:\s+(\d+)") {
            $metrics.NativeHeapPss = [int]$matches[1]
        }
        if ($line -match "Dalvik Heap\s+\d+\s+(\d+)\s+\d+\s+\d+\s+\d+\s+\d+\s+(\d+)") {
            $metrics.DalvikHeapAlloc = [int]$matches[2]
        }
        if ($line -match "TOTAL PSS:\s+(\d+)") {
            $metrics.TotalPss = [int]$matches[1]
        }
        if ($line -match "TOTAL\s+\d+\s+(\d+)") {
            $metrics.PrivateDirty = [int]$matches[1]
        }
        if ($line -match "Activities:\s+(\d+)") {
            $metrics.Activities = [int]$matches[1]
        }
        if ($line -match "Views:\s+(\d+)") {
            $metrics.Views = [int]$matches[1]
        }
        if ($line -match "ViewRootImpl:\s+(\d+)") {
            $metrics.ViewRootImpl = [int]$matches[1]
        }
    }
    return $metrics
}

# -------------------------------------------------------------------------
# Step 1: Baseline Memory Measurement
# -------------------------------------------------------------------------
Write-Host "`n--> [1/4] Establishing Baseline Idle Memory State (SkillMap)..."
adb shell am start -S -W -n "$PackageName/$MainActivity" | Out-Null
Start-Sleep -Milliseconds 1500
Trigger-GC

$baseline = Get-MemInfoMetrics
Write-Host ("  Baseline Java Heap PSS     : {0} KB ({1} MB)" -f $baseline.JavaHeapPss, [math]::Round($baseline.JavaHeapPss / 1024, 2))
Write-Host ("  Baseline Dalvik Heap Alloc : {0} KB ({1} MB)" -f $baseline.DalvikHeapAlloc, [math]::Round($baseline.DalvikHeapAlloc / 1024, 2))
Write-Host ("  Baseline Activities Count  : {0}" -f $baseline.Activities)
Write-Host ("  Baseline Views Count       : {0}" -f $baseline.Views)

$report.AppendLine("[Audit 1/4] Baseline Idle State (SkillMap)") | Out-Null
$report.AppendLine("  Java Heap Pss     : $($baseline.JavaHeapPss) KB") | Out-Null
$report.AppendLine("  Dalvik Heap Alloc : $($baseline.DalvikHeapAlloc) KB") | Out-Null
$report.AppendLine("  Activities        : $($baseline.Activities)") | Out-Null
$report.AppendLine("  Views             : $($baseline.Views)") | Out-Null
$report.AppendLine("  ViewRootImpl      : $($baseline.ViewRootImpl)`n") | Out-Null

# -------------------------------------------------------------------------
# Step 2: Worst-Case Dataset Footprint (QuickSort & Dijkstra/A*)
# -------------------------------------------------------------------------
Write-Host "`n--> [2/4] Measuring Worst-Case Algorithm Dataset Memory Footprint..."
# Load QuickSort worst-case frames
adb shell am start -W -n "$PackageName/$MainActivity" --es algorithmId quick_sort | Out-Null
Start-Sleep -Milliseconds 1200
# Step through frames
for ($i=0; $i -lt 10; $i++) {
    adb shell input tap 1239 1969
    Start-Sleep -Milliseconds 100
}

$quickSortMetrics = Get-MemInfoMetrics
$quickSortDelta = $quickSortMetrics.DalvikHeapAlloc - $baseline.DalvikHeapAlloc

Write-Host ("  QuickSort Dalvik Heap Alloc : {0} KB (Delta: +{1} KB)" -f $quickSortMetrics.DalvikHeapAlloc, $quickSortDelta)
$report.AppendLine("[Audit 2/4] Worst-Case Dataset Memory Footprint (QuickSort)") | Out-Null
$report.AppendLine("  QuickSort Heap Alloc: $($quickSortMetrics.DalvikHeapAlloc) KB") | Out-Null
$report.AppendLine("  Retained Delta      : +$quickSortDelta KB (Budget: < 2048 KB / 2 MB)`n") | Out-Null

# Load Dijkstra / A* dense graph frames
adb shell am start -W -n "$PackageName/$MainActivity" --es algorithmId dijkstra_astar | Out-Null
Start-Sleep -Milliseconds 1200
for ($i=0; $i -lt 10; $i++) {
    adb shell input tap 1239 1969
    Start-Sleep -Milliseconds 100
}

$dijkstraMetrics = Get-MemInfoMetrics
$dijkstraDelta = $dijkstraMetrics.DalvikHeapAlloc - $baseline.DalvikHeapAlloc
Write-Host ("  Dijkstra Dalvik Heap Alloc  : {0} KB (Delta: +{1} KB)" -f $dijkstraMetrics.DalvikHeapAlloc, $dijkstraDelta)
$report.AppendLine("[Audit 2/4 Continued] Worst-Case Graph Footprint (Dijkstra/A*)") | Out-Null
$report.AppendLine("  Dijkstra Heap Alloc : $($dijkstraMetrics.DalvikHeapAlloc) KB") | Out-Null
$report.AppendLine("  Retained Delta      : +$dijkstraDelta KB (Budget: < 2048 KB / 2 MB)`n") | Out-Null

# -------------------------------------------------------------------------
# Step 3: Multi-Cycle Full-App Navigation & Leak Audit
# -------------------------------------------------------------------------
Write-Host "`n--> [3/4] Executing 2 Complete Multi-Screen Navigation Cycles..."
$screens = @(
    "two_pointers", "sliding_window", "binary_search", "bubble_sort",
    "insertion_sort", "merge_sort", "quick_sort", "dutch_national_flag",
    "linked_list_reversal", "floyd_cycle", "stack_queue_lifo_fifo",
    "bst_ops", "avl_rotations", "graph_bfs_dfs", "dijkstra_astar"
)

for ($cycle = 1; $cycle -le 2; $cycle++) {
    Write-Host "  --> Navigation Cycle $cycle of 2..."
    foreach ($screen in $screens) {
        adb shell am start -W -n "$PackageName/$MainActivity" --es algorithmId $screen | Out-Null
        Start-Sleep -Milliseconds 350
    }
    # Race Mode in cycle
    adb shell am start -W -n "$PackageName/$MainActivity" --es route race_mode | Out-Null
    Start-Sleep -Milliseconds 500
}

# Return to Skill Map root
Write-Host "  --> Returning to Skill Map root destination..."
adb shell am start -W -n "$PackageName/$MainActivity" | Out-Null
Start-Sleep -Milliseconds 1000

# Force GC to collect destroyed activity/view references
Write-Host "  --> Triggering full garbage collection post-navigation..."
Trigger-GC

$finalMetrics = Get-MemInfoMetrics
Write-Host ("  Final Java Heap PSS        : {0} KB ({1} MB)" -f $finalMetrics.JavaHeapPss, [math]::Round($finalMetrics.JavaHeapPss / 1024, 2))
Write-Host ("  Final Dalvik Heap Alloc    : {0} KB ({1} MB)" -f $finalMetrics.DalvikHeapAlloc, [math]::Round($finalMetrics.DalvikHeapAlloc / 1024, 2))
Write-Host ("  Final Activities Count     : {0} (Strict 1 target)" -f $finalMetrics.Activities)
Write-Host ("  Final ViewRootImpl Count   : {0} (Strict 1 target)" -f $finalMetrics.ViewRootImpl)
Write-Host ("  Final Views Count          : {0}" -f $finalMetrics.Views)

$report.AppendLine("[Audit 3/4] Post-Navigation Memory State & Leak Verification") | Out-Null
$report.AppendLine("  Final Java Heap Pss : $($finalMetrics.JavaHeapPss) KB") | Out-Null
$report.AppendLine("  Final Dalvik Alloc  : $($finalMetrics.DalvikHeapAlloc) KB") | Out-Null
$report.AppendLine("  Activities Count    : $($finalMetrics.Activities) (Expected: 1)") | Out-Null
$report.AppendLine("  ViewRootImpl Count  : $($finalMetrics.ViewRootImpl) (Expected: 1)") | Out-Null
$report.AppendLine("  Views Count         : $($finalMetrics.Views)`n") | Out-Null

# -------------------------------------------------------------------------
# Step 4: Verification & Assertions
# -------------------------------------------------------------------------
Write-Host "`n--> [4/4] Evaluating Memory Budget Assertions (NFR-02)..."
$passed = $true

# Assertion 1: Retained dataset delta < 2 MB (2048 KB)
if ($quickSortDelta -ge 2048) {
    Write-Error "  [FAIL] QuickSort retained frame memory exceeded 2 MB budget ($quickSortDelta KB >= 2048 KB)!"
    $passed = $false
} else {
    Write-Host "  [PASS] QuickSort retained frame sequence consumes < 2 MB ($quickSortDelta KB < 2048 KB)."
}

if ($dijkstraDelta -ge 2048) {
    Write-Error "  [FAIL] Dijkstra retained frame memory exceeded 2 MB budget ($dijkstraDelta KB >= 2048 KB)!"
    $passed = $false
} else {
    Write-Host "  [PASS] Dijkstra retained frame sequence consumes < 2 MB ($dijkstraDelta KB < 2048 KB)."
}

# Assertion 2: Zero Activity leaks (Activities count == 1)
if ($finalMetrics.Activities -ne 1) {
    Write-Error "  [FAIL] Activity leak detected! $($finalMetrics.Activities) activities retained (expected 1)!"
    $passed = $false
} else {
    Write-Host "  [PASS] Zero Activity leaks verified (Activities count == 1)."
}

# Assertion 3: Zero Window/ViewRootImpl leaks
if ($finalMetrics.ViewRootImpl -ne 1) {
    Write-Error "  [FAIL] ViewRootImpl leak detected! $($finalMetrics.ViewRootImpl) ViewRootImpl retained (expected 1)!"
    $passed = $false
} else {
    Write-Host "  [PASS] Zero ViewRootImpl leaks verified (ViewRootImpl count == 1)."
}

# Append full raw dumpsys at bottom of report
$report.AppendLine("==========================================================================") | Out-Null
$report.AppendLine("RAW DUMPSYS MEMINFO AT AUDIT CONCLUSION:") | Out-Null
$report.AppendLine("==========================================================================") | Out-Null
$report.AppendLine($finalMetrics.RawDump) | Out-Null

Set-Content -Path $rawLogPath -Value $report.ToString() -Encoding UTF8
Write-Host "  --> Full meminfo report written to: $rawLogPath"

if ($passed) {
    Write-Host "`n=========================================================================="
    Write-Host " [Unravel QA] WP-52 Retained Heap & Memory Leak Audit: PASSED"
    Write-Host "=========================================================================="
    exit 0
} else {
    Write-Host "`n=========================================================================="
    Write-Host " [Unravel QA] WP-52 Retained Heap & Memory Leak Audit: FAILED"
    Write-Host "=========================================================================="
    exit 1
}
