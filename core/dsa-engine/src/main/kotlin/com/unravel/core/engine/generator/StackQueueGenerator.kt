package com.unravel.core.engine.generator

import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.LinkedNode
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Cluster 3: Stack & Queue Sequence Generator.
 * Visualizes LIFO (Last-In-First-Out) stack mechanics (push, pop, peek)
 * and FIFO (First-In-First-Out) queue mechanics (enqueue, dequeue, front).
 * Tracks `top`, `head`, and `tail` pointer tags, emits haptic pulses, and proves container invariants.
 */
class StackQueueGenerator : AlgorithmGenerator<VisualStructureState.LinkedNodeState> {

    override val algorithmId: AlgorithmId = AlgorithmId.STACK_QUEUE_LIFO_FIFO

    @OptIn(ExperimentalUuidApi::class)
    override fun generateFrames(initialState: VisualStructureState.LinkedNodeState): ImmutableList<AlgorithmFrame> {
        val accumulator = FrameAccumulator(algorithmId)
        val inputValues = if (initialState.nodes.isNotEmpty()) {
            initialState.nodes.map { it.value }
        } else {
            listOf(10, 20, 30, 40)
        }

        // ==========================================
        // Phase 1: Stack LIFO Demonstration
        // ==========================================
        var stackNodes = mutableListOf<LinkedNode>()

        // Line 1: Initialize Stack
        accumulator.emitFrame(
            structureState = VisualStructureState.LinkedNodeState(
                nodes = persistentListOf(),
                pointers = persistentMapOf()
            ),
            activeLineIndex = 1,
            variables = mapOf(
                "structure" to "Stack (LIFO)",
                "action" to "init",
                "stack.size" to "0"
            ),
            hapticCue = HapticCue.TICK
        )

        // Push elements to stack
        for (i in 0 until minOf(inputValues.size, 4)) {
            val value = inputValues[i]
            val newNodeId = Uuid.random()
            val oldTopId = stackNodes.firstOrNull()?.id
            val newNode = LinkedNode(
                id = newNodeId,
                value = value,
                nextId = oldTopId,
                prevId = null
            )
            // Update old top's prevId to point to new node
            if (stackNodes.isNotEmpty()) {
                stackNodes[0] = stackNodes[0].copy(prevId = newNodeId)
            }
            stackNodes.add(0, newNode)
            accumulator.incMutations()

            val topIdStr = newNodeId.toString()
            val pointers = persistentMapOf("top" to topIdStr)

            // Line 2: stack.push(value)
            accumulator.emitFrame(
                structureState = VisualStructureState.LinkedNodeState(
                    nodes = stackNodes.toImmutableList(),
                    pointers = pointers,
                    activeLinkIds = if (oldTopId != null) persistentSetOf("$topIdStr->${oldTopId}") else persistentSetOf()
                ),
                activeLineIndex = 2,
                variables = mapOf(
                    "structure" to "Stack (LIFO)",
                    "action" to "push($value)",
                    "top" to value.toString(),
                    "stack.size" to stackNodes.size.toString()
                ),
                hapticCue = HapticCue.TICK,
                expectedUserAction = ExpectedAction.MovePointer("top", stackNodes.size - 1)
            )
        }

        // Line 3: stack.peek()
        val topNode = stackNodes.first()
        accumulator.emitFrame(
            structureState = VisualStructureState.LinkedNodeState(
                nodes = stackNodes.toImmutableList(),
                pointers = persistentMapOf("top" to topNode.id.toString()),
                lockedNodeIds = persistentSetOf(topNode.id.toString())
            ),
            activeLineIndex = 3,
            variables = mapOf(
                "structure" to "Stack (LIFO)",
                "action" to "peek()",
                "top.value" to topNode.value.toString()
            ),
            hapticCue = HapticCue.TICK
        )

        // Line 4: stack.pop()
        accumulator.incMutations()
        val poppedNode = stackNodes.removeAt(0)
        val newTopNode = stackNodes.firstOrNull()
        if (newTopNode != null) {
            stackNodes[0] = newTopNode.copy(prevId = null)
        }
        val popPointers = if (newTopNode != null) persistentMapOf("top" to newTopNode.id.toString()) else persistentMapOf()

        accumulator.emitFrame(
            structureState = VisualStructureState.LinkedNodeState(
                nodes = stackNodes.toImmutableList(),
                pointers = popPointers,
                mutatingLinkIds = persistentSetOf("${poppedNode.id}->${newTopNode?.id ?: "null"}")
            ),
            activeLineIndex = 4,
            variables = mapOf(
                "structure" to "Stack (LIFO)",
                "action" to "pop() -> ${poppedNode.value}",
                "invariant" to "Popped element is most recently pushed (LIFO)"
            ),
            hapticCue = HapticCue.SWAP_THUD,
            expectedUserAction = ExpectedAction.RewirePointer(
                fromNodeId = poppedNode.id.toString(),
                newTargetNodeId = newTopNode?.id?.toString()
            )
        )

        // ==========================================
        // Phase 2: Queue FIFO Demonstration
        // ==========================================
        val queueNodes = mutableListOf<LinkedNode>()

        // Line 5: Initialize Queue
        accumulator.emitFrame(
            structureState = VisualStructureState.LinkedNodeState(
                nodes = persistentListOf(),
                pointers = persistentMapOf()
            ),
            activeLineIndex = 5,
            variables = mapOf(
                "structure" to "Queue (FIFO)",
                "action" to "init",
                "queue.size" to "0"
            ),
            hapticCue = HapticCue.TICK
        )

        // Enqueue elements to queue
        for (i in 0 until minOf(inputValues.size, 4)) {
            val value = inputValues[i]
            val newNodeId = Uuid.random()
            val oldTail = queueNodes.lastOrNull()
            val newNode = LinkedNode(
                id = newNodeId,
                value = value,
                nextId = null,
                prevId = oldTail?.id
            )
            if (oldTail != null) {
                queueNodes[queueNodes.lastIndex] = oldTail.copy(nextId = newNodeId)
            }
            queueNodes.add(newNode)
            accumulator.incMutations()

            val headIdStr = queueNodes.first().id.toString()
            val tailIdStr = newNodeId.toString()
            val qPointers = persistentMapOf("head" to headIdStr, "tail" to tailIdStr)

            // Line 6: queue.enqueue(value)
            accumulator.emitFrame(
                structureState = VisualStructureState.LinkedNodeState(
                    nodes = queueNodes.toImmutableList(),
                    pointers = qPointers,
                    activeLinkIds = if (oldTail != null) persistentSetOf("${oldTail.id}->$tailIdStr") else persistentSetOf()
                ),
                activeLineIndex = 6,
                variables = mapOf(
                    "structure" to "Queue (FIFO)",
                    "action" to "enqueue($value)",
                    "head" to queueNodes.first().value.toString(),
                    "tail" to value.toString(),
                    "queue.size" to queueNodes.size.toString()
                ),
                hapticCue = HapticCue.TICK,
                expectedUserAction = ExpectedAction.MovePointer("tail", queueNodes.size - 1)
            )
        }

        // Line 7: queue.front()
        val frontNode = queueNodes.first()
        accumulator.emitFrame(
            structureState = VisualStructureState.LinkedNodeState(
                nodes = queueNodes.toImmutableList(),
                pointers = persistentMapOf(
                    "head" to frontNode.id.toString(),
                    "tail" to queueNodes.last().id.toString()
                ),
                lockedNodeIds = persistentSetOf(frontNode.id.toString())
            ),
            activeLineIndex = 7,
            variables = mapOf(
                "structure" to "Queue (FIFO)",
                "action" to "front()",
                "front.value" to frontNode.value.toString()
            ),
            hapticCue = HapticCue.TICK
        )

        // Line 8: queue.dequeue()
        accumulator.incMutations()
        val dequeuedNode = queueNodes.removeAt(0)
        val newHeadNode = queueNodes.firstOrNull()
        if (newHeadNode != null) {
            queueNodes[0] = newHeadNode.copy(prevId = null)
        }
        val deqPointers = if (newHeadNode != null) {
            persistentMapOf("head" to newHeadNode.id.toString(), "tail" to queueNodes.last().id.toString())
        } else {
            persistentMapOf()
        }

        accumulator.emitFrame(
            structureState = VisualStructureState.LinkedNodeState(
                nodes = queueNodes.toImmutableList(),
                pointers = deqPointers,
                mutatingLinkIds = persistentSetOf("${dequeuedNode.id}->${newHeadNode?.id ?: "null"}")
            ),
            activeLineIndex = 8,
            variables = mapOf(
                "structure" to "Queue (FIFO)",
                "action" to "dequeue() -> ${dequeuedNode.value}",
                "invariant" to "Dequeued element is earliest enqueued (FIFO)"
            ),
            hapticCue = HapticCue.SWAP_THUD,
            expectedUserAction = ExpectedAction.RewirePointer(
                fromNodeId = dequeuedNode.id.toString(),
                newTargetNodeId = newHeadNode?.id?.toString()
            )
        )

        // Line 9: Summary & Invariants verified
        accumulator.emitFrame(
            structureState = VisualStructureState.LinkedNodeState(
                nodes = queueNodes.toImmutableList(),
                pointers = deqPointers,
                lockedNodeIds = queueNodes.map { it.id.toString() }.toImmutableSet()
            ),
            activeLineIndex = 9,
            variables = mapOf(
                "result" to "LIFO and FIFO container invariants verified",
                "totalMutations" to accumulator.currentMutations.toString()
            ),
            hapticCue = HapticCue.SUCCESS_LOCK
        )

        return accumulator.build()
    }
}
