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
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet

/**
 * Cluster 3: Linked List Reversal Sequence Generator.
 * Simulates in-place reversal of singly and doubly linked list pointer chains.
 * Emits fine-grained pointer tracking (`prev`, `curr`, `next`), pointer rewire cues,
 * and synchronized code execution frames.
 */
class LinkedListReversalGenerator : AlgorithmGenerator<VisualStructureState.LinkedNodeState> {

    override val algorithmId: AlgorithmId = AlgorithmId.LINKED_LIST_REVERSAL

    override fun generateFrames(initialState: VisualStructureState.LinkedNodeState): ImmutableList<AlgorithmFrame> {
        val accumulator = FrameAccumulator(algorithmId)
        val originalNodes = initialState.nodes

        if (originalNodes.isEmpty()) {
            accumulator.emitFrame(
                structureState = initialState,
                activeLineIndex = 1,
                variables = mapOf("head" to "null", "curr" to "null")
            )
            return accumulator.build()
        }

        val nodeMap = originalNodes.associateBy { it.id.toString() }.toMutableMap()
        var prevId: String? = null
        var currId: String? = originalNodes.first().id.toString()
        var nodeIndex = 0

        // Line 1: Initialize prev = null, curr = head
        accumulator.emitFrame(
            structureState = initialState.copy(
                pointers = persistentMapOf("head" to currId!!, "curr" to currId)
            ),
            activeLineIndex = 1,
            variables = mapOf(
                "prev" to "null",
                "curr" to currId,
                "mutations" to "0"
            ),
            hapticCue = HapticCue.TICK
        )

        while (currId != null && accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 2) {
            accumulator.incComparisons()
            val currNode = nodeMap[currId] ?: break
            val nextUuid = currNode.nextId
            val nextId = nextUuid?.toString()

            val basePointers = mutableMapOf<String, String>("curr" to currId)
            prevId?.let { basePointers["prev"] = it }
            nextId?.let { basePointers["next"] = it }

            // Line 2: next = curr.next
            val activeLinks = if (nextId != null) persistentSetOf("$currId->$nextId") else persistentSetOf()
            accumulator.emitFrame(
                structureState = VisualStructureState.LinkedNodeState(
                    nodes = nodeMap.values.toImmutableList(),
                    pointers = basePointers.toImmutableMap(),
                    activeLinkIds = activeLinks,
                    isCycleDetected = false
                ),
                activeLineIndex = 2,
                variables = mapOf(
                    "prev" to (prevId ?: "null"),
                    "curr" to currId,
                    "next" to (nextId ?: "null")
                ),
                hapticCue = HapticCue.TICK,
                expectedUserAction = ExpectedAction.RewirePointer(
                    fromNodeId = currId,
                    newTargetNodeId = prevId
                )
            )

            // Line 3: curr.next = prev (Rewire pointer)
            accumulator.incMutations()
            val prevUuid = prevId?.let { idStr -> nodeMap[idStr]?.id }
            val updatedCurrNode = currNode.copy(
                nextId = prevUuid,
                prevId = nextUuid
            )
            nodeMap[currId] = updatedCurrNode

            val mutatingLinks = persistentSetOf("$currId->${prevId ?: "null"}")
            accumulator.emitFrame(
                structureState = VisualStructureState.LinkedNodeState(
                    nodes = nodeMap.values.toImmutableList(),
                    pointers = basePointers.toImmutableMap(),
                    mutatingLinkIds = mutatingLinks,
                    isCycleDetected = false
                ),
                activeLineIndex = 3,
                variables = mapOf(
                    "action" to "curr.next = prev",
                    "curr.next" to (prevId ?: "null"),
                    "mutations" to accumulator.currentMutations.toString()
                ),
                hapticCue = HapticCue.SWAP_THUD
            )

            // Line 4: prev = curr
            val afterPrevPointers = basePointers.toMutableMap()
            afterPrevPointers["prev"] = currId
            accumulator.emitFrame(
                structureState = VisualStructureState.LinkedNodeState(
                    nodes = nodeMap.values.toImmutableList(),
                    pointers = afterPrevPointers.toImmutableMap(),
                    isCycleDetected = false
                ),
                activeLineIndex = 4,
                variables = mapOf(
                    "prev" to currId,
                    "curr" to currId,
                    "next" to (nextId ?: "null")
                ),
                hapticCue = HapticCue.TICK
            )

            // Line 5: curr = next
            nodeIndex++
            val afterCurrPointers = mutableMapOf<String, String>("prev" to currId)
            nextId?.let { afterCurrPointers["curr"] = it }
            accumulator.emitFrame(
                structureState = VisualStructureState.LinkedNodeState(
                    nodes = nodeMap.values.toImmutableList(),
                    pointers = afterCurrPointers.toImmutableMap(),
                    isCycleDetected = false
                ),
                activeLineIndex = 5,
                variables = mapOf(
                    "prev" to currId,
                    "curr" to (nextId ?: "null")
                ),
                hapticCue = HapticCue.TICK,
                expectedUserAction = ExpectedAction.MovePointer("curr", nodeIndex)
            )

            prevId = currId
            currId = nextId
        }

        // Line 6: return prev (new head)
        val finalHeadId = prevId ?: originalNodes.first().id.toString()
        val allNodeIds = originalNodes.map { it.id.toString() }.toImmutableSet()
        accumulator.emitFrame(
            structureState = VisualStructureState.LinkedNodeState(
                nodes = nodeMap.values.toImmutableList(),
                pointers = persistentMapOf("head" to finalHeadId),
                lockedNodeIds = allNodeIds,
                isCycleDetected = false
            ),
            activeLineIndex = 6,
            variables = mapOf(
                "newHead" to finalHeadId,
                "totalMutations" to accumulator.currentMutations.toString()
            ),
            hapticCue = HapticCue.SUCCESS_LOCK
        )

        return accumulator.build()
    }
}
