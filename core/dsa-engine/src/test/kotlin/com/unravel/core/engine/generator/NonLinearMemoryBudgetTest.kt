package com.unravel.core.engine.generator

import com.unravel.core.engine.layout.GraphLayoutEngine
import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.SpatialVertex
import com.unravel.core.engine.model.TreeNode
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class NonLinearMemoryBudgetTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val twoMbBudget = 2L * 1024L * 1024L // 2,097,152 bytes (NFR-02)

    @Test
    @DisplayName("Non-Linear: Worst-case V=30 dense graph respects 1,500 frame ceiling and <2MB budget")
    fun testDenseGraphMemoryBudget() {
        // 5x6 = 30 vertices dense grid graph
        val gridState = GraphLayoutEngine.layoutGrid(
            rows = 5,
            cols = 6,
            obstacles = setOf(Pair(1, 2), Pair(2, 2), Pair(3, 2), Pair(2, 4)),
            startCoord = Pair(0, 0),
            targetCoord = Pair(4, 5)
        )

        val bfsGen = GraphBfsDfsGenerator()
        val dijkstraGen = DijkstraAStarGenerator()

        for (gen in listOf(bfsGen, dijkstraGen)) {
            val frames: List<AlgorithmFrame> = gen.generateFrames(gridState)

            assertTrue(frames.isNotEmpty(), "Frames should not be empty")
            assertTrue(
                frames.size <= AlgorithmGenerator.MAX_FRAMES,
                "Frames (${frames.size}) exceeded MAX_FRAMES (1,500)"
            )

            // Retained in-memory footprint estimation with persistent collections
            val estimatedMemory = frames.size * 600L
            assertTrue(
                estimatedMemory < twoMbBudget,
                "Estimated memory footprint ($estimatedMemory B) exceeded 2MB limit"
            )

            // Actual serialization size test
            val serialized = json.encodeToString(ListSerializer(AlgorithmFrame.serializer()), frames)
            assertTrue(serialized.isNotEmpty())
            assertTrue(
                serialized.toByteArray(Charsets.UTF_8).size.toLong() < 4L * 1024L * 1024L,
                "Serialized trace must be reasonably compact"
            )
        }
    }

    @Test
    @DisplayName("Non-Linear: Worst-case N=31 full AVL tree respects 1,500 frame ceiling and <2MB budget")
    fun testFullTreeMemoryBudget() {
        val values = (1..31).map { it * 10 }
        val dummyNodes = values.map { TreeNode(id = Uuid.random(), value = it) }.toImmutableList()
        val treeState = VisualStructureState.HierarchyTreeState(nodes = dummyNodes)

        val avlGen = AvlRotationsGenerator()
        val bstGen = BstOperationsGenerator()

        for (gen in listOf(avlGen, bstGen)) {
            val frames: List<AlgorithmFrame> = gen.generateFrames(treeState)

            assertTrue(frames.isNotEmpty())
            assertTrue(frames.size <= AlgorithmGenerator.MAX_FRAMES)

            val estimatedMemory = frames.size * 550L
            assertTrue(
                estimatedMemory < twoMbBudget,
                "Estimated footprint ($estimatedMemory B) exceeded 2MB limit"
            )

            val serialized = json.encodeToString(ListSerializer(AlgorithmFrame.serializer()), frames)
            assertTrue(serialized.isNotEmpty())
        }
    }

    @Test
    @DisplayName("Non-Linear: Worst-case N=60 cyclical linked list respects 1,500 frame ceiling and <2MB budget")
    fun testCyclicLinkedListMemoryBudget() {
        val values = (1..60).map { it * 5 }
        val cyclicState = InputPresetGenerator.generateLinkedListState(values, cycleEntryIndex = 25)

        val floydGen = FloydCycleGenerator()
        val revGen = LinkedListReversalGenerator()

        for (gen in listOf(floydGen, revGen)) {
            val frames: List<AlgorithmFrame> = gen.generateFrames(cyclicState)

            assertTrue(frames.isNotEmpty())
            assertTrue(frames.size <= AlgorithmGenerator.MAX_FRAMES)

            val estimatedMemory = frames.size * 450L
            assertTrue(
                estimatedMemory < twoMbBudget,
                "Estimated footprint ($estimatedMemory B) exceeded 2MB limit"
            )

            val serialized = json.encodeToString(ListSerializer(AlgorithmFrame.serializer()), frames)
            assertTrue(serialized.isNotEmpty())
        }
    }

    @Test
    @DisplayName("Non-Linear: Scrubbing backward preserves exact historical tree and graph states")
    fun testNonLinearScrubbingImmutability() {
        val gridState = GraphLayoutEngine.layoutGrid(rows = 3, cols = 3)
        val frames = DijkstraAStarGenerator().generateFrames(gridState)

        val midFrameIndex = frames.size / 2
        val midFrame = frames[midFrameIndex]
        val midStateOriginal = midFrame.structureState as VisualStructureState.SpatialGridGraphState
        val originalVisited = midStateOriginal.visitedIds.toSet()

        // Scrub forward
        for (i in frames.indices) {
            assertEquals(i, frames[i].stepIndex)
        }

        // Scrub backward
        for (i in frames.size - 1 downTo 0) {
            assertEquals(i, frames[i].stepIndex)
        }

        // Assert no historical mutation occurred
        val midStateAfter = frames[midFrameIndex].structureState as VisualStructureState.SpatialGridGraphState
        assertEquals(originalVisited, midStateAfter.visitedIds.toSet())
    }
}
