package com.unravel.feature.skillmap.ui

import com.unravel.core.database.model.ActiveSessionPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JumpBackInBannerTest {

    @Test
    fun formatAlgorithmTitle_formatsKnownAndFallbackIdentifiers() {
        assertEquals("Two Pointers", formatAlgorithmTitle("two_pointers"))
        assertEquals("Sliding Window", formatAlgorithmTitle("sliding_window"))
        assertEquals("Binary Search", formatAlgorithmTitle("binary_search"))
        assertEquals("Bubble Sort", formatAlgorithmTitle("bubble_sort"))
        assertEquals("Merge Sort", formatAlgorithmTitle("merge_sort"))
        assertEquals("Floyd's Cycle Detection", formatAlgorithmTitle("floyd_cycle"))
        assertEquals("BST Operations", formatAlgorithmTitle("bst_ops"))
        assertEquals("Graph BFS & DFS", formatAlgorithmTitle("graph_bfs_dfs"))
        assertEquals("Dijkstra & A* Pathfinding", formatAlgorithmTitle("dijkstra_astar"))

        // Fallback title-casing
        assertEquals("Custom Dynamic Programming", formatAlgorithmTitle("custom_dynamic_programming"))
    }

    @Test
    fun formatElapsedTime_formatsRelativeTimeWindows() {
        val baseTime = 1_000_000_000L

        // Paused if invalid/zero timestamp
        assertEquals("Paused", formatElapsedTime(0L, baseTime))
        assertEquals("Paused", formatElapsedTime(-100L, baseTime))

        // Just now (< 60s)
        assertEquals("Just now", formatElapsedTime(baseTime - 30_000L, baseTime))

        // Minutes ago (< 1h)
        assertEquals("5m ago", formatElapsedTime(baseTime - 300_000L, baseTime))
        assertEquals("45m ago", formatElapsedTime(baseTime - 2_700_000L, baseTime))

        // Hours ago (< 24h)
        assertEquals("3h ago", formatElapsedTime(baseTime - 10_800_000L, baseTime))

        // Days ago (>= 24h)
        assertEquals("2d ago", formatElapsedTime(baseTime - 172_800_000L, baseTime))
    }

    @Test
    fun sessionVisibility_evaluatesCorrectly() {
        val nullSession: ActiveSessionPreferences? = null
        val emptySession = ActiveSessionPreferences(algorithmId = "")
        val activeSession = ActiveSessionPreferences(algorithmId = "bubble_sort", stepIndex = 5)

        assertFalse(nullSession != null && nullSession.algorithmId.isNotBlank())
        assertFalse(emptySession.algorithmId.isNotBlank())
        assertTrue(activeSession.algorithmId.isNotBlank())
    }
}
