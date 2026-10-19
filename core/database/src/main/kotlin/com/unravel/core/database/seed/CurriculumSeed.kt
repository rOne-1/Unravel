package com.unravel.core.database.seed

import com.unravel.core.database.entity.SkillNodeEntity
import com.unravel.core.engine.model.AlgorithmId

/**
 * Pre-populates the 14 interactive curriculum skill nodes upon first database creation.
 */
object CurriculumSeed {

    val INITIAL_SKILL_NODES: List<SkillNodeEntity> = listOf(
        // Cluster 1: Linear Foundations
        SkillNodeEntity(
            id = AlgorithmId.TWO_POINTERS.id,
            title = AlgorithmId.TWO_POINTERS.title,
            category = AlgorithmId.TWO_POINTERS.category.name,
            clusterIndex = 1,
            prerequisiteIds = emptyList(),
            masteryState = "UNEXPLORED",
            isUnlocked = true // Default unlocked curriculum entry point
        ),
        SkillNodeEntity(
            id = AlgorithmId.SLIDING_WINDOW.id,
            title = AlgorithmId.SLIDING_WINDOW.title,
            category = AlgorithmId.SLIDING_WINDOW.category.name,
            clusterIndex = 1,
            prerequisiteIds = listOf(AlgorithmId.TWO_POINTERS.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        ),
        SkillNodeEntity(
            id = AlgorithmId.BINARY_SEARCH.id,
            title = AlgorithmId.BINARY_SEARCH.title,
            category = AlgorithmId.BINARY_SEARCH.category.name,
            clusterIndex = 1,
            prerequisiteIds = listOf(AlgorithmId.TWO_POINTERS.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        ),

        // Cluster 2: Sorting & Recursion
        SkillNodeEntity(
            id = AlgorithmId.BUBBLE_SORT.id,
            title = AlgorithmId.BUBBLE_SORT.title,
            category = AlgorithmId.BUBBLE_SORT.category.name,
            clusterIndex = 2,
            prerequisiteIds = listOf(AlgorithmId.TWO_POINTERS.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        ),
        SkillNodeEntity(
            id = AlgorithmId.INSERTION_SORT.id,
            title = AlgorithmId.INSERTION_SORT.title,
            category = AlgorithmId.INSERTION_SORT.category.name,
            clusterIndex = 2,
            prerequisiteIds = listOf(AlgorithmId.BUBBLE_SORT.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        ),
        SkillNodeEntity(
            id = AlgorithmId.MERGE_SORT.id,
            title = AlgorithmId.MERGE_SORT.title,
            category = AlgorithmId.MERGE_SORT.category.name,
            clusterIndex = 2,
            prerequisiteIds = listOf(AlgorithmId.INSERTION_SORT.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        ),
        SkillNodeEntity(
            id = AlgorithmId.QUICK_SORT.id,
            title = AlgorithmId.QUICK_SORT.title,
            category = AlgorithmId.QUICK_SORT.category.name,
            clusterIndex = 2,
            prerequisiteIds = listOf(AlgorithmId.MERGE_SORT.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        ),

        // Cluster 3: Pointer Mechanics
        SkillNodeEntity(
            id = AlgorithmId.LINKED_LIST_REVERSAL.id,
            title = AlgorithmId.LINKED_LIST_REVERSAL.title,
            category = AlgorithmId.LINKED_LIST_REVERSAL.category.name,
            clusterIndex = 3,
            prerequisiteIds = listOf(AlgorithmId.TWO_POINTERS.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        ),
        SkillNodeEntity(
            id = AlgorithmId.FLOYD_CYCLE.id,
            title = AlgorithmId.FLOYD_CYCLE.title,
            category = AlgorithmId.FLOYD_CYCLE.category.name,
            clusterIndex = 3,
            prerequisiteIds = listOf(AlgorithmId.LINKED_LIST_REVERSAL.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        ),
        SkillNodeEntity(
            id = AlgorithmId.STACK_QUEUE_LIFO_FIFO.id,
            title = AlgorithmId.STACK_QUEUE_LIFO_FIFO.title,
            category = AlgorithmId.STACK_QUEUE_LIFO_FIFO.category.name,
            clusterIndex = 3,
            prerequisiteIds = listOf(AlgorithmId.LINKED_LIST_REVERSAL.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        ),

        // Cluster 4: Non-Linear & Spatial
        SkillNodeEntity(
            id = AlgorithmId.BST_OPERATIONS.id,
            title = AlgorithmId.BST_OPERATIONS.title,
            category = AlgorithmId.BST_OPERATIONS.category.name,
            clusterIndex = 4,
            prerequisiteIds = listOf(AlgorithmId.BINARY_SEARCH.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        ),
        SkillNodeEntity(
            id = AlgorithmId.AVL_ROTATIONS.id,
            title = AlgorithmId.AVL_ROTATIONS.title,
            category = AlgorithmId.AVL_ROTATIONS.category.name,
            clusterIndex = 4,
            prerequisiteIds = listOf(AlgorithmId.BST_OPERATIONS.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        ),
        SkillNodeEntity(
            id = AlgorithmId.GRAPH_BFS_DFS.id,
            title = AlgorithmId.GRAPH_BFS_DFS.title,
            category = AlgorithmId.GRAPH_BFS_DFS.category.name,
            clusterIndex = 4,
            prerequisiteIds = listOf(AlgorithmId.STACK_QUEUE_LIFO_FIFO.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        ),
        SkillNodeEntity(
            id = AlgorithmId.DIJKSTRA_ASTAR.id,
            title = AlgorithmId.DIJKSTRA_ASTAR.title,
            category = AlgorithmId.DIJKSTRA_ASTAR.category.name,
            clusterIndex = 4,
            prerequisiteIds = listOf(AlgorithmId.GRAPH_BFS_DFS.id),
            masteryState = "UNEXPLORED",
            isUnlocked = false
        )
    )
}
