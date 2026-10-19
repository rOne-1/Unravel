package com.unravel.feature.racemode.engine

import androidx.compose.runtime.Immutable
import com.unravel.core.engine.generator.AvlRotationsGenerator
import com.unravel.core.engine.generator.BinarySearchGenerator
import com.unravel.core.engine.generator.BstOperationsGenerator
import com.unravel.core.engine.generator.BubbleSortGenerator
import com.unravel.core.engine.generator.DijkstraAStarGenerator
import com.unravel.core.engine.generator.FloydCycleGenerator
import com.unravel.core.engine.generator.GraphBfsDfsGenerator
import com.unravel.core.engine.generator.InputPreset
import com.unravel.core.engine.generator.InputPresetGenerator
import com.unravel.core.engine.generator.InsertionSortGenerator
import com.unravel.core.engine.generator.LinkedListReversalGenerator
import com.unravel.core.engine.generator.MergeSortGenerator
import com.unravel.core.engine.generator.QuickSortGenerator
import com.unravel.core.engine.generator.SlidingWindowGenerator
import com.unravel.core.engine.generator.StackQueueGenerator
import com.unravel.core.engine.generator.TwoPointersGenerator
import com.unravel.core.engine.layout.GraphLayoutEngine
import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.SandboxCategory
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Educational head-to-head algorithm matchup specification.
 */
@Immutable
data class RaceMatchup(
    val id: String,
    val title: String,
    val category: SandboxCategory,
    val algorithmA: AlgorithmId,
    val algorithmB: AlgorithmId,
    val description: String
)

/**
 * Registry providing standardized race matchups and shared-dataset frame generation.
 */
object RaceAlgorithmCatalog {

    val MATCHUPS: ImmutableList<RaceMatchup> = persistentListOf(
        RaceMatchup(
            id = "bubble_vs_quick",
            title = "Bubble Sort vs QuickSort",
            category = SandboxCategory.ARRAY_POINTER,
            algorithmA = AlgorithmId.BUBBLE_SORT,
            algorithmB = AlgorithmId.QUICK_SORT,
            description = "O(n²) Quadratic vs O(n log n) Divide & Conquer"
        ),
        RaceMatchup(
            id = "insertion_vs_merge",
            title = "Insertion Sort vs Merge Sort",
            category = SandboxCategory.ARRAY_POINTER,
            algorithmA = AlgorithmId.INSERTION_SORT,
            algorithmB = AlgorithmId.MERGE_SORT,
            description = "Adaptive Linear vs Stable O(n log n)"
        ),
        RaceMatchup(
            id = "two_pointers_vs_binary_search",
            title = "Two Pointers vs Binary Search",
            category = SandboxCategory.ARRAY_POINTER,
            algorithmA = AlgorithmId.TWO_POINTERS,
            algorithmB = AlgorithmId.BINARY_SEARCH,
            description = "O(n) Converging Bounds vs O(log n) Halving"
        ),
        RaceMatchup(
            id = "linked_reversal_vs_floyd",
            title = "Reversal vs Floyd's Cycle",
            category = SandboxCategory.LINKED_STACK_QUEUE,
            algorithmA = AlgorithmId.LINKED_LIST_REVERSAL,
            algorithmB = AlgorithmId.FLOYD_CYCLE,
            description = "In-Place Pointer Swap vs Fast & Slow Runner"
        ),
        RaceMatchup(
            id = "bst_vs_avl",
            title = "Binary Search Tree vs AVL Tree",
            category = SandboxCategory.HIERARCHICAL_TREE,
            algorithmA = AlgorithmId.BST_OPERATIONS,
            algorithmB = AlgorithmId.AVL_ROTATIONS,
            description = "Unbalanced Insertion vs Height-Balanced Rotations"
        ),
        RaceMatchup(
            id = "bfs_vs_dijkstra",
            title = "BFS Traversal vs Dijkstra / A*",
            category = SandboxCategory.SPATIAL_GRID_GRAPH,
            algorithmA = AlgorithmId.GRAPH_BFS_DFS,
            algorithmB = AlgorithmId.DIJKSTRA_ASTAR,
            description = "Unweighted Wavefront vs Cost-Directed Search"
        )
    )

    fun getMatchupById(id: String): RaceMatchup {
        return MATCHUPS.firstOrNull { it.id == id } ?: MATCHUPS.first()
    }

    fun getMatchupsByCategory(category: SandboxCategory): List<RaceMatchup> {
        return MATCHUPS.filter { it.category == category }
    }

    /**
     * Generates synchronized frame sequences for two competing algorithms from an identical seeded input dataset.
     */
    fun generateMatchupFrames(
        algorithmA: AlgorithmId,
        algorithmB: AlgorithmId,
        category: SandboxCategory,
        seed: Long = 42L,
        arraySize: Int = 10
    ): Pair<ImmutableList<AlgorithmFrame>, ImmutableList<AlgorithmFrame>> {
        return when (category) {
            SandboxCategory.ARRAY_POINTER -> {
                // Ensure identical initial array state for unbiased comparison
                val sharedInitialState = InputPresetGenerator.generateInitialState(
                    preset = InputPreset.RANDOM,
                    size = arraySize,
                    seed = seed
                )
                val framesA = generateArrayFrames(algorithmA, sharedInitialState)
                val framesB = generateArrayFrames(algorithmB, sharedInitialState)
                Pair(framesA, framesB)
            }
            SandboxCategory.LINKED_STACK_QUEUE -> {
                val sharedListState = InputPresetGenerator.generateLinkedListState(
                    listOf(10, 25, 40, 55, 70, 85),
                    cycleEntryIndex = if (algorithmB == AlgorithmId.FLOYD_CYCLE) 2 else null
                )
                val framesA = generateLinkedFrames(algorithmA, sharedListState)
                val framesB = generateLinkedFrames(algorithmB, sharedListState)
                Pair(framesA, framesB)
            }
            SandboxCategory.HIERARCHICAL_TREE -> {
                val initialTreeState = VisualStructureState.HierarchyTreeState(nodes = persistentListOf())
                val framesA = BstOperationsGenerator().generateFrames(initialTreeState)
                val framesB = AvlRotationsGenerator().generateFrames(initialTreeState)
                Pair(framesA, framesB)
            }
            SandboxCategory.SPATIAL_GRID_GRAPH -> {
                val sharedGridState = GraphLayoutEngine.layoutGrid(
                    rows = 4,
                    cols = 4,
                    obstacles = setOf(Pair(1, 1), Pair(2, 2))
                )
                val framesA = GraphBfsDfsGenerator(useDfs = false).generateFrames(sharedGridState)
                val framesB = DijkstraAStarGenerator(useAStar = true).generateFrames(sharedGridState)
                Pair(framesA, framesB)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateArrayFrames(
        algorithmId: AlgorithmId,
        initialState: VisualStructureState.ArrayState
    ): ImmutableList<AlgorithmFrame> {
        val generator: AlgorithmGenerator<VisualStructureState.ArrayState> = when (algorithmId) {
            AlgorithmId.BUBBLE_SORT -> BubbleSortGenerator()
            AlgorithmId.QUICK_SORT -> QuickSortGenerator()
            AlgorithmId.INSERTION_SORT -> InsertionSortGenerator()
            AlgorithmId.MERGE_SORT -> MergeSortGenerator()
            AlgorithmId.TWO_POINTERS -> TwoPointersGenerator()
            AlgorithmId.BINARY_SEARCH -> BinarySearchGenerator()
            AlgorithmId.SLIDING_WINDOW -> SlidingWindowGenerator()
            else -> BubbleSortGenerator()
        }
        return generator.generateFrames(initialState)
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateLinkedFrames(
        algorithmId: AlgorithmId,
        initialState: VisualStructureState.LinkedNodeState
    ): ImmutableList<AlgorithmFrame> {
        return when (algorithmId) {
            AlgorithmId.LINKED_LIST_REVERSAL -> LinkedListReversalGenerator().generateFrames(initialState)
            AlgorithmId.FLOYD_CYCLE -> FloydCycleGenerator().generateFrames(initialState)
            AlgorithmId.STACK_QUEUE_LIFO_FIFO -> StackQueueGenerator().generateFrames(initialState)
            else -> LinkedListReversalGenerator().generateFrames(initialState)
        }
    }
}
