package com.unravel.core.engine.generator

import com.unravel.core.engine.layout.GraphLayoutEngine
import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.VisualStructureState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.system.measureTimeMillis

class All14AlgorithmsCurriculumTest {

    @ParameterizedTest(name = "Algorithm {0}: Generates deterministic, bounded frames")
    @EnumSource(AlgorithmId::class)
    @DisplayName("All 14 Algorithms: Deterministic sequence generation, frame ceiling, and code synchronization")
    fun testAll14AlgorithmsExecution(algorithmId: AlgorithmId) {
        val (generator, state) = createGeneratorAndState(algorithmId)

        @Suppress("UNCHECKED_CAST")
        val typedGen = generator as AlgorithmGenerator<VisualStructureState>

        val frames1: List<AlgorithmFrame> = typedGen.generateFrames(state)
        val frames2: List<AlgorithmFrame> = typedGen.generateFrames(state)

        // 1. Non-empty frames
        assertTrue(frames1.isNotEmpty(), "$algorithmId produced empty frames")

        // 2. Frame ceiling enforcement (MAX_FRAMES = 1,500)
        assertTrue(
            frames1.size <= AlgorithmGenerator.MAX_FRAMES,
            "$algorithmId produced ${frames1.size} frames, exceeding MAX_FRAMES (1,500)"
        )

        // 3. Step index sequential continuity
        frames1.forEachIndexed { index, frame ->
            assertEquals(index, frame.stepIndex, "Frame stepIndex mismatch at position $index in $algorithmId")
            assertTrue(frame.codeSync.activeLineIndex > 0, "activeLineIndex should be > 0 at frame $index in $algorithmId")
            assertNotNull(frame.structureState, "structureState should not be null at frame $index in $algorithmId")
        }

        // 4. Deterministic output
        assertEquals(frames1.size, frames2.size, "$algorithmId frame count non-deterministic")
        frames1.indices.forEach { i ->
            assertEquals(frames1[i].stepIndex, frames2[i].stepIndex)
            assertEquals(frames1[i].codeSync.activeLineIndex, frames2[i].codeSync.activeLineIndex)
            assertEquals(frames1[i].hapticCue, frames2[i].hapticCue)
        }

        // 5. Completion frame check
        val lastFrame = frames1.last()
        assertTrue(
            lastFrame.hapticCue in listOf(HapticCue.SUCCESS_LOCK, HapticCue.NONE, HapticCue.TICK, HapticCue.ERROR_BUZZ),
            "Final frame should have a valid terminal cue in $algorithmId"
        )
    }

    @Test
    @DisplayName("Execution Benchmark: All 14 algorithms complete execution in < 5 seconds")
    fun testAll14AlgorithmsExecutionSpeedBenchmark() {
        val elapsed = measureTimeMillis {
            for (id in AlgorithmId.entries) {
                val (generator, state) = createGeneratorAndState(id)
                @Suppress("UNCHECKED_CAST")
                val typedGen = generator as AlgorithmGenerator<VisualStructureState>
                val frames = typedGen.generateFrames(state)
                assertTrue(frames.isNotEmpty())
            }
        }

        assertTrue(elapsed < 5_000L, "Curriculum generation of all 14 algorithms took $elapsed ms, exceeding 5,000ms threshold")
    }

    @Test
    @DisplayName("Module Isolation Boundary: :core:dsa-engine contains zero Android dependencies")
    fun testPureJvmModuleBoundary() {
        // Assert that neither android.* nor androidx.* classes are loaded or accessible in this pure JVM module
        val forbiddenPrefixes = listOf("android.os.", "android.view.", "androidx.compose.")
        val loadedClasses = ClassLoader.getSystemClassLoader().definedPackages.map { it.name }

        for (prefix in forbiddenPrefixes) {
            val hasForbidden = loadedClasses.any { it.startsWith(prefix) }
            assertFalse(hasForbidden, "Forbidden Android package '$prefix' detected in pure JVM engine module")
        }
    }

    private fun createGeneratorAndState(algorithmId: AlgorithmId): Pair<AlgorithmGenerator<*>, VisualStructureState> {
        return when (algorithmId) {
            AlgorithmId.TWO_POINTERS -> Pair(
                TwoPointersGenerator(),
                InputPresetGenerator.generateInitialState(InputPreset.RANDOM, size = 10, seed = 1L)
            )
            AlgorithmId.SLIDING_WINDOW -> Pair(
                SlidingWindowGenerator(),
                InputPresetGenerator.generateInitialState(InputPreset.RANDOM, size = 10, seed = 2L)
            )
            AlgorithmId.BINARY_SEARCH -> Pair(
                BinarySearchGenerator(),
                InputPresetGenerator.generateInitialState(InputPreset.NEARLY_SORTED, size = 10, seed = 3L)
            )
            AlgorithmId.BUBBLE_SORT -> Pair(
                BubbleSortGenerator(),
                InputPresetGenerator.generateInitialState(InputPreset.RANDOM, size = 10, seed = 4L)
            )
            AlgorithmId.INSERTION_SORT -> Pair(
                InsertionSortGenerator(),
                InputPresetGenerator.generateInitialState(InputPreset.RANDOM, size = 10, seed = 5L)
            )
            AlgorithmId.MERGE_SORT -> Pair(
                MergeSortGenerator(),
                InputPresetGenerator.generateInitialState(InputPreset.RANDOM, size = 10, seed = 6L)
            )
            AlgorithmId.QUICK_SORT -> Pair(
                QuickSortGenerator(),
                InputPresetGenerator.generateInitialState(InputPreset.RANDOM, size = 10, seed = 7L)
            )
            AlgorithmId.LINKED_LIST_REVERSAL -> Pair(
                LinkedListReversalGenerator(),
                InputPresetGenerator.generateLinkedListState(listOf(12, 24, 36, 48, 60))
            )
            AlgorithmId.FLOYD_CYCLE -> Pair(
                FloydCycleGenerator(),
                InputPresetGenerator.generateLinkedListState(listOf(10, 20, 30, 40, 50, 60, 70), cycleEntryIndex = 3)
            )
            AlgorithmId.STACK_QUEUE_LIFO_FIFO -> Pair(
                StackQueueGenerator(),
                InputPresetGenerator.generateLinkedListState(listOf(5, 15, 25, 35))
            )
            AlgorithmId.BST_OPERATIONS -> Pair(
                BstOperationsGenerator(),
                VisualStructureState.HierarchyTreeState(nodes = kotlinx.collections.immutable.persistentListOf())
            )
            AlgorithmId.AVL_ROTATIONS -> Pair(
                AvlRotationsGenerator(),
                VisualStructureState.HierarchyTreeState(nodes = kotlinx.collections.immutable.persistentListOf())
            )
            AlgorithmId.GRAPH_BFS_DFS -> Pair(
                GraphBfsDfsGenerator(useDfs = false),
                GraphLayoutEngine.layoutGrid(rows = 3, cols = 4, obstacles = setOf(Pair(1, 1), Pair(1, 2)))
            )
            AlgorithmId.DIJKSTRA_ASTAR -> Pair(
                DijkstraAStarGenerator(useAStar = true),
                GraphLayoutEngine.layoutGrid(rows = 4, cols = 4, obstacles = setOf(Pair(1, 1), Pair(2, 2)))
            )
        }
    }
}
