package com.unravel.core.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class SandboxCategory {
    ARRAY_POINTER,
    LINKED_STACK_QUEUE,
    HIERARCHICAL_TREE,
    SPATIAL_GRID_GRAPH
}

@Serializable
enum class AlgorithmId(
    val id: String,
    val title: String,
    val category: SandboxCategory,
    val timeComplexityBest: String,
    val timeComplexityWorst: String,
    val spaceComplexity: String
) {
    // Cluster 1: Linear Foundations
    TWO_POINTERS("two_pointers", "Two Pointers", SandboxCategory.ARRAY_POINTER, "O(n)", "O(n)", "O(1)"),
    SLIDING_WINDOW("sliding_window", "Sliding Window", SandboxCategory.ARRAY_POINTER, "O(n)", "O(n)", "O(1)"),
    BINARY_SEARCH("binary_search", "Binary Search", SandboxCategory.ARRAY_POINTER, "O(1)", "O(log n)", "O(1)"),

    // Cluster 2: Sorting & Recursion
    BUBBLE_SORT("bubble_sort", "Bubble Sort", SandboxCategory.ARRAY_POINTER, "O(n)", "O(n²)", "O(1)"),
    INSERTION_SORT("insertion_sort", "Insertion Sort", SandboxCategory.ARRAY_POINTER, "O(n)", "O(n²)", "O(1)"),
    MERGE_SORT("merge_sort", "Merge Sort", SandboxCategory.ARRAY_POINTER, "O(n log n)", "O(n log n)", "O(n)"),
    QUICK_SORT("quick_sort", "QuickSort", SandboxCategory.ARRAY_POINTER, "O(n log n)", "O(n²)", "O(log n)"),

    // Cluster 3: Pointer Mechanics
    LINKED_LIST_REVERSAL("linked_list_reversal", "Linked List Reversal", SandboxCategory.LINKED_STACK_QUEUE, "O(n)", "O(n)", "O(1)"),
    FLOYD_CYCLE("floyd_cycle", "Floyd's Cycle Detection", SandboxCategory.LINKED_STACK_QUEUE, "O(n)", "O(n)", "O(1)"),
    STACK_QUEUE_LIFO_FIFO("stack_queue", "Stacks & Queues", SandboxCategory.LINKED_STACK_QUEUE, "O(1)", "O(1)", "O(n)"),

    // Cluster 4: Non-Linear & Spatial
    BST_OPERATIONS("bst_ops", "Binary Search Tree", SandboxCategory.HIERARCHICAL_TREE, "O(log n)", "O(n)", "O(h)"),
    AVL_ROTATIONS("avl_rotations", "AVL Tree Rotations", SandboxCategory.HIERARCHICAL_TREE, "O(log n)", "O(log n)", "O(log n)"),
    GRAPH_BFS_DFS("graph_bfs_dfs", "BFS & DFS Traversal", SandboxCategory.SPATIAL_GRID_GRAPH, "O(V + E)", "O(V + E)", "O(V)"),
    DIJKSTRA_ASTAR("dijkstra_astar", "Dijkstra & A* Pathfinding", SandboxCategory.SPATIAL_GRID_GRAPH, "O(E + V log V)", "O(E + V log V)", "O(V)")
}

@Serializable
enum class HapticCue {
    NONE,
    TICK,
    SWAP_THUD,
    SUCCESS_LOCK,
    ERROR_BUZZ
}
