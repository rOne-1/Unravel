package com.unravel.core.engine.generator

import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.VisualStructureState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class Cluster3GeneratorsTest {

    @Test
    @DisplayName("LinkedListReversal: Reverses 5-node list, inverts nextId links, and updates head")
    fun testLinkedListReversalCorrectness() {
        val generator = LinkedListReversalGenerator()
        val values = listOf(10, 20, 30, 40, 50)
        val initialState = InputPresetGenerator.generateLinkedListState(values)

        val frames = generator.generateFrames(initialState)

        assertTrue(frames.isNotEmpty(), "Frames should not be empty")
        assertTrue(frames.size <= AlgorithmGenerator.MAX_FRAMES, "Frame count should obey MAX_FRAMES ceiling")

        // First frame: initial state
        val firstFrame = frames.first()
        assertEquals(1, firstFrame.codeSync.activeLineIndex)
        assertEquals(initialState.nodes.first().id.toString(), firstFrame.structureState.let { (it as VisualStructureState.LinkedNodeState).pointers["head"] })

        // Check for rewire actions and haptic cues
        val rewireFrames = frames.filter { it.expectedUserAction is ExpectedAction.RewirePointer }
        assertEquals(5, rewireFrames.size, "Should have 5 pointer rewire actions for 5 nodes")
        assertTrue(frames.any { it.hapticCue == HapticCue.SWAP_THUD }, "Should emit SWAP_THUD on rewiring")

        // Final frame: head points to original tail (node 50)
        val finalFrame = frames.last()
        val finalState = finalFrame.structureState as VisualStructureState.LinkedNodeState
        val originalTailId = initialState.nodes.last().id.toString()
        val originalHeadId = initialState.nodes.first().id.toString()

        assertEquals(originalTailId, finalState.pointers["head"], "New head must be original tail")
        assertEquals(HapticCue.SUCCESS_LOCK, finalFrame.hapticCue)

        // Verify pointers inversion in the final node map
        val finalNodeMap = finalState.nodes.associateBy { it.id.toString() }
        val reversedHeadNode = finalNodeMap[originalTailId]
        assertNotNull(reversedHeadNode)
        assertEquals(initialState.nodes[3].id, reversedHeadNode?.nextId, "Tail's next should be node 40")

        val reversedTailNode = finalNodeMap[originalHeadId]
        assertNotNull(reversedTailNode)
        assertNull(reversedTailNode?.nextId, "Original head's next should now be null")
    }

    @Test
    @DisplayName("LinkedListReversal: Handles empty list gracefully")
    fun testLinkedListReversalEmptyList() {
        val generator = LinkedListReversalGenerator()
        val initialState = InputPresetGenerator.generateLinkedListState(emptyList())

        val frames = generator.generateFrames(initialState)
        assertEquals(1, frames.size)
    }

    @ParameterizedTest(name = "FloydCycle: Detects cycle with loop length C = {0}")
    @ValueSource(ints = [3, 5, 8])
    @DisplayName("FloydCycle: Detects cycle and accurately localizes cycle entrance node")
    fun testFloydCycleDetection(cycleLength: Int) {
        val generator = FloydCycleGenerator()
        val prefixLength = 4
        val totalLength = prefixLength + cycleLength
        val values = (1..totalLength).map { it * 10 }
        val cycleEntryIndex = prefixLength // Node at index 4 is the cycle entry

        val initialState = InputPresetGenerator.generateLinkedListState(values, cycleEntryIndex = cycleEntryIndex)
        val expectedCycleEntryNodeId = initialState.nodes[cycleEntryIndex].id.toString()

        val frames = generator.generateFrames(initialState)

        assertTrue(frames.isNotEmpty())
        assertTrue(frames.size <= AlgorithmGenerator.MAX_FRAMES)

        // Verify cycle is detected
        val finalFrame = frames.last()
        val finalState = finalFrame.structureState as VisualStructureState.LinkedNodeState
        assertTrue(finalState.isCycleDetected, "Cycle must be flagged as detected")
        assertEquals(expectedCycleEntryNodeId, finalState.pointers["cycleEntry"], "Must locate exact cycle entry node")
        assertEquals(HapticCue.SUCCESS_LOCK, finalFrame.hapticCue)

        // Check phase 1 intersection detection occurs
        assertTrue(frames.any { frame ->
            val state = frame.structureState as VisualStructureState.LinkedNodeState
            state.isCycleDetected && frame.codeSync.activeLineIndex == 3
        }, "Phase 1 intersection frame must exist")

        // Check phase 2 entrance search occurs
        assertTrue(frames.any { frame ->
            frame.codeSync.activeLineIndex == 5
        }, "Phase 2 reset slow to head frame must exist")
    }

    @Test
    @DisplayName("FloydCycle: Correctly terminates when list has no cycle")
    fun testFloydCycleNoCycle() {
        val generator = FloydCycleGenerator()
        val values = (1..6).map { it * 10 }
        val initialState = InputPresetGenerator.generateLinkedListState(values, cycleEntryIndex = null)

        val frames = generator.generateFrames(initialState)
        assertTrue(frames.isNotEmpty())

        val finalFrame = frames.last()
        val finalState = finalFrame.structureState as VisualStructureState.LinkedNodeState
        assertFalse(finalState.isCycleDetected, "No cycle should be detected")
        assertNull(finalState.pointers["cycleEntry"])
    }

    @Test
    @DisplayName("StackQueue: Demonstrates LIFO and FIFO invariants and pointer tracking")
    fun testStackQueueInvariants() {
        val generator = StackQueueGenerator()
        val values = listOf(10, 20, 30, 40)
        val initialState = InputPresetGenerator.generateLinkedListState(values)

        val frames = generator.generateFrames(initialState)

        assertTrue(frames.isNotEmpty())
        assertTrue(frames.size <= AlgorithmGenerator.MAX_FRAMES)

        // Verify Stack LIFO frames exist
        val stackPushFrames = frames.filter { it.codeSync.activeLineIndex == 2 }
        assertEquals(4, stackPushFrames.size, "Should push 4 elements to stack")

        val stackPopFrames = frames.filter { it.codeSync.activeLineIndex == 4 }
        assertEquals(1, stackPopFrames.size, "Should pop 1 element from stack")
        val popFrame = stackPopFrames.first()
        assertEquals(HapticCue.SWAP_THUD, popFrame.hapticCue)
        assertTrue(popFrame.codeSync.variableBindings["invariant"]?.contains("LIFO") == true)

        // Verify Queue FIFO frames exist
        val queueEnqueueFrames = frames.filter { it.codeSync.activeLineIndex == 6 }
        assertEquals(4, queueEnqueueFrames.size, "Should enqueue 4 elements to queue")

        val queueDequeueFrames = frames.filter { it.codeSync.activeLineIndex == 8 }
        assertEquals(1, queueDequeueFrames.size, "Should dequeue 1 element from queue")
        val deqFrame = queueDequeueFrames.first()
        assertEquals(HapticCue.SWAP_THUD, deqFrame.hapticCue)
        assertTrue(deqFrame.codeSync.variableBindings["invariant"]?.contains("FIFO") == true)

        // Final frame verifies success lock
        val finalFrame = frames.last()
        assertEquals(HapticCue.SUCCESS_LOCK, finalFrame.hapticCue)
    }

    @Test
    @DisplayName("Cluster 3: Generators produce 100% deterministic frame sequences")
    fun testCluster3Determinism() {
        val revGen = LinkedListReversalGenerator()
        val floydGen = FloydCycleGenerator()
        val sqGen = StackQueueGenerator()

        val listState = InputPresetGenerator.generateLinkedListState(listOf(1, 2, 3, 4, 5))
        val cycleState = InputPresetGenerator.generateLinkedListState(listOf(1, 2, 3, 4, 5, 6), cycleEntryIndex = 2)

        val run1Rev = revGen.generateFrames(listState)
        val run2Rev = revGen.generateFrames(listState)
        assertEquals(run1Rev.size, run2Rev.size)
        run1Rev.indices.forEach { i ->
            assertEquals(run1Rev[i].stepIndex, run2Rev[i].stepIndex)
            assertEquals(run1Rev[i].codeSync.activeLineIndex, run2Rev[i].codeSync.activeLineIndex)
            assertEquals(run1Rev[i].hapticCue, run2Rev[i].hapticCue)
        }

        val run1Floyd = floydGen.generateFrames(cycleState)
        val run2Floyd = floydGen.generateFrames(cycleState)
        assertEquals(run1Floyd.size, run2Floyd.size)
        run1Floyd.indices.forEach { i ->
            assertEquals(run1Floyd[i].stepIndex, run2Floyd[i].stepIndex)
            assertEquals(run1Floyd[i].codeSync.activeLineIndex, run2Floyd[i].codeSync.activeLineIndex)
        }

        val run1Sq = sqGen.generateFrames(listState)
        val run2Sq = sqGen.generateFrames(listState)
        assertEquals(run1Sq.size, run2Sq.size)
    }
}
