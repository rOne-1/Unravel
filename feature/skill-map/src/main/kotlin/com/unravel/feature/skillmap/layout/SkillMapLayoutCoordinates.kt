package com.unravel.feature.skillmap.layout

import com.unravel.feature.skillmap.model.ClusterBounds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Standard spatial coordinates and cluster territory bounds for the 2400x1600 DAG map.
 */
object SkillMapLayoutCoordinates {
    const val MAP_WIDTH: Float = 2400f
    const val MAP_HEIGHT: Float = 1600f

    /** Default cluster root node IDs unlocked by default on fresh state. */
    val CLUSTER_ROOT_IDS: Set<String> = setOf(
        "two_pointers",
        "bubble_sort",
        "linked_list_reversal",
        "bst_ops"
    )

    /** 2D spatial coordinate mapping for all 14 curriculum algorithm nodes. */
    val NODE_COORDINATES: Map<String, Pair<Float, Float>> = mapOf(
        // Cluster 1: Linear Foundations
        "two_pointers" to Pair(300f, 350f),
        "sliding_window" to Pair(650f, 250f),
        "binary_search" to Pair(650f, 480f),

        // Cluster 2: Sorting & Recursion
        "bubble_sort" to Pair(1450f, 350f),
        "insertion_sort" to Pair(1700f, 350f),
        "merge_sort" to Pair(1950f, 250f),
        "quick_sort" to Pair(1950f, 480f),

        // Cluster 3: Pointer Mechanics
        "linked_list_reversal" to Pair(350f, 1150f),
        "floyd_cycle" to Pair(700f, 1050f),
        "stack_queue" to Pair(700f, 1280f),

        // Cluster 4: Non-Linear & Spatial
        "bst_ops" to Pair(1450f, 1080f),
        "avl_rotations" to Pair(1750f, 1080f),
        "graph_bfs_dfs" to Pair(1450f, 1320f),
        "dijkstra_astar" to Pair(1750f, 1320f)
    )

    /** The 4 distinct spatial cluster territories on the 2D plane. */
    val CLUSTER_BOUNDS: ImmutableList<ClusterBounds> = persistentListOf(
        ClusterBounds(
            clusterIndex = 1,
            title = "LINEAR FOUNDATIONS",
            minX = 100f,
            minY = 100f,
            maxX = 1100f,
            maxY = 750f
        ),
        ClusterBounds(
            clusterIndex = 2,
            title = "SORTING & RECURSION",
            minX = 1300f,
            minY = 100f,
            maxX = 2300f,
            maxY = 750f
        ),
        ClusterBounds(
            clusterIndex = 3,
            title = "POINTER MECHANICS",
            minX = 100f,
            minY = 850f,
            maxX = 1100f,
            maxY = 1500f
        ),
        ClusterBounds(
            clusterIndex = 4,
            title = "NON-LINEAR & SPATIAL",
            minX = 1300f,
            minY = 850f,
            maxX = 2300f,
            maxY = 1500f
        )
    )
}
