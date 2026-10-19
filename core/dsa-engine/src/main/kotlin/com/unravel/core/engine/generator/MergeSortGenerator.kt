package com.unravel.core.engine.generator

import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ArrayNode
import com.unravel.core.engine.model.CallStackFrame
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet

/**
 * Cluster 2: Merge Sort Sequence Generator.
 * Divide-and-conquer recursion, CallStackFrame column push/pop, and auxiliary buffer merge.
 */
class MergeSortGenerator : AlgorithmGenerator<VisualStructureState.ArrayState> {

    override val algorithmId: AlgorithmId = AlgorithmId.MERGE_SORT

    override fun generateFrames(initialState: VisualStructureState.ArrayState): ImmutableList<AlgorithmFrame> {
        val accumulator = FrameAccumulator(algorithmId)
        val items = initialState.items.toMutableList()
        val n = items.size

        if (n < 2) {
            accumulator.emitFrame(initialState, activeLineIndex = 1)
            return accumulator.build()
        }

        val callStack = mutableListOf<CallStackFrame>()
        val sortedIndices = mutableSetOf<Int>()

        sortRange(initialState, items, 0, n - 1, 0, callStack, sortedIndices, accumulator)

        // Lock all elements at completion
        for (idx in 0 until n) sortedIndices.add(idx)

        // Final completion frame
        accumulator.emitFrame(
            structureState = initialState.copy(
                items = items.toImmutableList(),
                sortedIndices = sortedIndices.toImmutableSet()
            ),
            activeLineIndex = 8,
            variables = mapOf(
                "status" to "SORTED",
                "totalComparisons" to accumulator.currentComparisons.toString(),
                "totalMutations" to accumulator.currentMutations.toString()
            ),
            callStack = emptyList(),
            hapticCue = HapticCue.SUCCESS_LOCK
        )

        return accumulator.build()
    }

    private fun sortRange(
        initialState: VisualStructureState.ArrayState,
        items: MutableList<ArrayNode>,
        l: Int,
        r: Int,
        depth: Int,
        callStack: MutableList<CallStackFrame>,
        sortedIndices: MutableSet<Int>,
        accumulator: FrameAccumulator
    ) {
        if (l >= r) return

        accumulator.updateMaxDepth(depth)
        val stackFrame = CallStackFrame(
            functionName = "mergeSort",
            parameters = mapOf("l" to l.toString(), "r" to r.toString()).toImmutableMap(),
            depth = depth
        )
        callStack.add(0, stackFrame)

        val mid = l + (r - l) / 2

        // Line 1-2: Divide step
        if (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 2) {
            accumulator.emitFrame(
                structureState = initialState.copy(
                    items = items.toImmutableList(),
                    pointers = persistentMapOf("l" to l, "mid" to mid, "r" to r),
                    activeIndices = (l..r).toImmutableSet(),
                    sortedIndices = sortedIndices.toImmutableSet()
                ),
                activeLineIndex = 2,
                variables = mapOf("l" to l.toString(), "mid" to mid.toString(), "r" to r.toString()),
                callStack = callStack,
                hapticCue = HapticCue.TICK
            )
        }

        sortRange(initialState, items, l, mid, depth + 1, callStack, sortedIndices, accumulator)
        sortRange(initialState, items, mid + 1, r, depth + 1, callStack, sortedIndices, accumulator)

        // Merge step
        mergeRanges(initialState, items, l, mid, r, depth, callStack, sortedIndices, accumulator)

        callStack.removeAt(0)
    }

    private fun mergeRanges(
        initialState: VisualStructureState.ArrayState,
        items: MutableList<ArrayNode>,
        l: Int,
        mid: Int,
        r: Int,
        depth: Int,
        callStack: MutableList<CallStackFrame>,
        sortedIndices: MutableSet<Int>,
        accumulator: FrameAccumulator
    ) {
        val mergeStackFrame = CallStackFrame(
            functionName = "merge",
            parameters = mapOf("l" to l.toString(), "mid" to mid.toString(), "r" to r.toString()).toImmutableMap(),
            depth = depth
        )
        callStack.add(0, mergeStackFrame)

        val aux = mutableListOf<ArrayNode>()
        var p1 = l
        var p2 = mid + 1

        while (p1 <= mid && p2 <= r) {
            accumulator.incComparisons() // compare items[p1] vs items[p2]

            val compareVars = mapOf(
                "p1" to p1.toString(),
                "p2" to p2.toString(),
                "arr[p1]" to items[p1].value.toString(),
                "arr[p2]" to items[p2].value.toString()
            )

            val pickLeft = items[p1].value <= items[p2].value

            // Line 4: Comparison before adding to auxiliary buffer
            if (items.size <= 25 || accumulator.frameCount < 800) {
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items.toImmutableList(),
                        pointers = persistentMapOf("p1" to p1, "p2" to p2),
                        activeIndices = persistentSetOf(p1, p2),
                        auxiliaryItems = aux.toImmutableList(),
                        sortedIndices = sortedIndices.toImmutableSet()
                    ),
                    activeLineIndex = 4,
                    variables = compareVars,
                    callStack = callStack,
                    expectedUserAction = if (pickLeft) ExpectedAction.KeepAndAdvance else ExpectedAction.SwapIndices(p1, p2)
                )
            }

            if (pickLeft) {
                aux.add(items[p1])
                p1++
            } else {
                aux.add(items[p2])
                p2++
            }
        }

        while (p1 <= mid) {
            aux.add(items[p1])
            p1++
        }
        while (p2 <= r) {
            aux.add(items[p2])
            p2++
        }

        // Write back to primary array from auxiliary buffer
        for (k in aux.indices) {
            val targetIndex = l + k
            items[targetIndex] = aux[k]
            accumulator.incMutations()

            if (l == 0 && r == items.size - 1) {
                sortedIndices.add(targetIndex)
            }

            if (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 2) {
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items.toImmutableList(),
                        pointers = persistentMapOf("write" to targetIndex),
                        activeIndices = persistentSetOf(targetIndex),
                        mutatingIndices = persistentSetOf(targetIndex),
                        auxiliaryItems = aux.toImmutableList(),
                        sortedIndices = sortedIndices.toImmutableSet()
                    ),
                    activeLineIndex = 6,
                    variables = mapOf(
                        "writeIndex" to targetIndex.toString(),
                        "value" to aux[k].value.toString()
                    ),
                    callStack = callStack,
                    hapticCue = HapticCue.SWAP_THUD
                )
            }
        }

        callStack.removeAt(0)
    }
}
