package com.unravel.core.engine.generator

import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.LinkedNode
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

/**
 * Cluster 3: Floyd's Cycle Detection Sequence Generator.
 * Implements Tortoise & Hare (two-pointer) cycle detection and cycle entrance localization.
 * Phase 1: Locates an intersection inside the cyclical loop.
 * Phase 2: Repositions slow pointer to head and advances both pointers at 1 step/tick to locate cycle entry node.
 */
class FloydCycleGenerator : AlgorithmGenerator<VisualStructureState.LinkedNodeState> {

    override val algorithmId: AlgorithmId = AlgorithmId.FLOYD_CYCLE

    override fun generateFrames(initialState: VisualStructureState.LinkedNodeState): ImmutableList<AlgorithmFrame> {
        val accumulator = FrameAccumulator(algorithmId)
        val nodes = initialState.nodes

        if (nodes.isEmpty()) {
            accumulator.emitFrame(
                structureState = initialState,
                activeLineIndex = 1,
                variables = mapOf("head" to "null", "hasCycle" to "false")
            )
            return accumulator.build()
        }

        val nodeMap = nodes.associateBy { it.id.toString() }
        val headId = nodes.first().id.toString()

        var slowId: String? = headId
        var fastId: String? = headId
        var slowIndex = 0
        var fastIndex = 0

        // Line 1: slow = head, fast = head
        accumulator.emitFrame(
            structureState = initialState.copy(
                pointers = persistentMapOf("head" to headId, "slow" to headId, "fast" to headId)
            ),
            activeLineIndex = 1,
            variables = mapOf(
                "phase" to "1: Detect Cycle",
                "slow" to headId,
                "fast" to headId,
                "comparisons" to "0"
            ),
            hapticCue = HapticCue.TICK
        )

        var cycleFound = false

        // Phase 1: Advance slow by 1, fast by 2 until they meet or fast reaches end
        while (fastId != null && accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 4) {
            accumulator.incComparisons()
            val fastNode = nodeMap[fastId]
            val fastNextUuid = fastNode?.nextId
            val fastNextNode = fastNextUuid?.let { nodeMap[it.toString()] }
            val fastNextNextUuid = fastNextNode?.nextId

            if (fastNextUuid == null || fastNextNextUuid == null) {
                // No cycle exists
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        pointers = persistentMapOf("head" to headId, "fast" to (fastNextUuid?.toString() ?: fastId)),
                        isCycleDetected = false
                    ),
                    activeLineIndex = 2,
                    variables = mapOf(
                        "phase" to "1: Detect Cycle",
                        "fast.next" to "null",
                        "hasCycle" to "false"
                    ),
                    hapticCue = HapticCue.TICK
                )
                break
            }

            // Advance slow by 1 step
            val slowNode = nodeMap[slowId]
            slowId = slowNode?.nextId?.toString()
            slowIndex++

            // Advance fast by 2 steps
            fastId = fastNextNextUuid.toString()
            fastIndex += 2

            val pointersMap = persistentMapOf(
                "head" to headId,
                "slow" to (slowId ?: "null"),
                "fast" to fastId
            )

            // Line 2: slow = slow.next, fast = fast.next.next
            accumulator.emitFrame(
                structureState = initialState.copy(
                    pointers = pointersMap,
                    activeLinkIds = persistentSetOf(
                        "${slowNode?.id}->${slowId ?: "null"}",
                        "${fastNode.id}->${fastNextUuid}",
                        "${fastNextUuid}->$fastId"
                    ),
                    isCycleDetected = false
                ),
                activeLineIndex = 2,
                variables = mapOf(
                    "phase" to "1: Detect Cycle",
                    "slow" to (slowId ?: "null"),
                    "fast" to fastId,
                    "comparisons" to accumulator.currentComparisons.toString()
                ),
                hapticCue = HapticCue.TICK,
                expectedUserAction = ExpectedAction.MovePointer("slow", slowIndex)
            )

            // Line 3: if (slow == fast)
            if (slowId != null && slowId == fastId) {
                cycleFound = true
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        pointers = pointersMap,
                        lockedNodeIds = persistentSetOf(slowId),
                        isCycleDetected = true
                    ),
                    activeLineIndex = 3,
                    variables = mapOf(
                        "phase" to "1: Intersection Found",
                        "intersectionNode" to slowId,
                        "hasCycle" to "true"
                    ),
                    hapticCue = HapticCue.SUCCESS_LOCK
                )
                break
            }
        }

        if (!cycleFound) {
            // Line 4: No cycle termination
            accumulator.emitFrame(
                structureState = initialState.copy(
                    pointers = persistentMapOf("head" to headId),
                    isCycleDetected = false
                ),
                activeLineIndex = 4,
                variables = mapOf(
                    "result" to "No cycle detected in list",
                    "hasCycle" to "false"
                ),
                hapticCue = HapticCue.NONE
            )
            return accumulator.build()
        }

        // Phase 2: Find cycle entrance node
        // Reposition slow to head, keep fast at intersection
        slowId = headId
        slowIndex = 0

        // Line 5: slow = head (Reset slow to head)
        accumulator.emitFrame(
            structureState = initialState.copy(
                pointers = persistentMapOf("head" to headId, "slow" to headId, "fast" to (fastId ?: headId)),
                isCycleDetected = true
            ),
            activeLineIndex = 5,
            variables = mapOf(
                "phase" to "2: Locate Cycle Entrance",
                "slow" to headId,
                "fast" to (fastId ?: headId)
            ),
            hapticCue = HapticCue.TICK
        )

        while (slowId != null && fastId != null && slowId != fastId && accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 2) {
            accumulator.incComparisons()
            // Advance both slow and fast by 1 step
            val slowNode = nodeMap[slowId]
            val fastNode = nodeMap[fastId]

            slowId = slowNode?.nextId?.toString()
            fastId = fastNode?.nextId?.toString()
            slowIndex++

            val pointersMap = persistentMapOf(
                "head" to headId,
                "slow" to (slowId ?: "null"),
                "fast" to (fastId ?: "null")
            )

            // Line 6: slow = slow.next, fast = fast.next
            accumulator.emitFrame(
                structureState = initialState.copy(
                    pointers = pointersMap,
                    isCycleDetected = true
                ),
                activeLineIndex = 6,
                variables = mapOf(
                    "phase" to "2: Locate Cycle Entrance",
                    "slow" to (slowId ?: "null"),
                    "fast" to (fastId ?: "null")
                ),
                hapticCue = HapticCue.TICK,
                expectedUserAction = ExpectedAction.MovePointer("slow", slowIndex)
            )
        }

        // Line 7: return slow (Cycle entrance located)
        val cycleEntryNodeId = slowId ?: headId
        accumulator.emitFrame(
            structureState = initialState.copy(
                pointers = persistentMapOf(
                    "head" to headId,
                    "cycleEntry" to cycleEntryNodeId,
                    "slow" to cycleEntryNodeId,
                    "fast" to cycleEntryNodeId
                ),
                lockedNodeIds = persistentSetOf(cycleEntryNodeId),
                isCycleDetected = true
            ),
            activeLineIndex = 7,
            variables = mapOf(
                "result" to "Cycle entrance node located",
                "cycleEntry" to cycleEntryNodeId,
                "hasCycle" to "true"
            ),
            hapticCue = HapticCue.SUCCESS_LOCK
        )

        return accumulator.build()
    }
}
