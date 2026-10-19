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
 * Cluster 2: QuickSort Sequence Generator.
 * Lomuto partitioning, pivot badge tracking, recursion call-stack tracking, and parabolic swap markers.
 */
class QuickSortGenerator : AlgorithmGenerator<VisualStructureState.ArrayState> {

    override val algorithmId: AlgorithmId = AlgorithmId.QUICK_SORT

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

        quickSortRange(initialState, items, 0, n - 1, 0, callStack, sortedIndices, accumulator)

        // Lock all elements at completion
        for (idx in 0 until n) sortedIndices.add(idx)

        accumulator.emitFrame(
            structureState = initialState.copy(
                items = items.toImmutableList(),
                sortedIndices = sortedIndices.toImmutableSet()
            ),
            activeLineIndex = 8,
            variables = mapOf(
                "status" to "SORTED",
                "totalComparisons" to accumulator.currentComparisons.toString(),
                "totalSwaps" to accumulator.currentSwaps.toString()
            ),
            callStack = emptyList(),
            hapticCue = HapticCue.SUCCESS_LOCK
        )

        return accumulator.build()
    }

    private fun quickSortRange(
        initialState: VisualStructureState.ArrayState,
        items: MutableList<ArrayNode>,
        low: Int,
        high: Int,
        depth: Int,
        callStack: MutableList<CallStackFrame>,
        sortedIndices: MutableSet<Int>,
        accumulator: FrameAccumulator
    ) {
        if (low >= high) {
            if (low == high) sortedIndices.add(low)
            return
        }

        accumulator.updateMaxDepth(depth)
        val stackFrame = CallStackFrame(
            functionName = "quickSort",
            parameters = mapOf("low" to low.toString(), "high" to high.toString()).toImmutableMap(),
            depth = depth
        )
        callStack.add(0, stackFrame)

        // Line 1: Partitioning call setup
        if (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 2) {
            accumulator.emitFrame(
                structureState = initialState.copy(
                    items = items.toImmutableList(),
                    pointers = persistentMapOf("low" to low, "high" to high, "pivot" to high),
                    activeIndices = (low..high).toImmutableSet(),
                    sortedIndices = sortedIndices.toImmutableSet()
                ),
                activeLineIndex = 1,
                variables = mapOf("low" to low.toString(), "high" to high.toString(), "pivot" to items[high].value.toString()),
                callStack = callStack,
                hapticCue = HapticCue.TICK
            )
        }

        val pivotIndex = partitionRange(initialState, items, low, high, depth + 1, callStack, sortedIndices, accumulator)

        quickSortRange(initialState, items, low, pivotIndex - 1, depth + 1, callStack, sortedIndices, accumulator)
        quickSortRange(initialState, items, pivotIndex + 1, high, depth + 1, callStack, sortedIndices, accumulator)

        callStack.removeAt(0)
    }

    private fun partitionRange(
        initialState: VisualStructureState.ArrayState,
        items: MutableList<ArrayNode>,
        low: Int,
        high: Int,
        depth: Int,
        callStack: MutableList<CallStackFrame>,
        sortedIndices: MutableSet<Int>,
        accumulator: FrameAccumulator
    ): Int {
        val pivot = items[high]
        var i = low - 1

        for (j in low until high) {
            val compareVars = mapOf(
                "low" to low.toString(),
                "high" to high.toString(),
                "pivot" to pivot.value.toString(),
                "i" to i.toString(),
                "j" to j.toString(),
                "arr[j]" to items[j].value.toString()
            )

            accumulator.incComparisons() // compare items[j].value < pivot.value
            val needsSwap = items[j].value < pivot.value

            // Line 3: Compare arr[j] with pivot
            if (items.size <= 25 || accumulator.frameCount < 800) {
                val pointersMap = mutableMapOf("pivot" to high, "j" to j)
                if (i >= low) pointersMap["i"] = i

                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items.toImmutableList(),
                        pointers = pointersMap.toImmutableMap(),
                        activeIndices = persistentSetOf(j, high),
                        sortedIndices = sortedIndices.toImmutableSet()
                    ),
                    activeLineIndex = 3,
                    variables = compareVars,
                    callStack = callStack,
                    expectedUserAction = if (needsSwap) ExpectedAction.SwapIndices(i + 1, j) else ExpectedAction.KeepAndAdvance
                )
            }

            if (needsSwap) {
                i++
                if (i != j) {
                    val temp = items[i]
                    items[i] = items[j]
                    items[j] = temp
                    accumulator.incSwaps()
                    accumulator.incMutations()

                    // Line 4: Swap arr[i] with arr[j]
                    if (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 2) {
                        accumulator.emitFrame(
                            structureState = initialState.copy(
                                items = items.toImmutableList(),
                                pointers = persistentMapOf("pivot" to high, "i" to i, "j" to j),
                                activeIndices = persistentSetOf(i, j),
                                mutatingIndices = persistentSetOf(i, j),
                                sortedIndices = sortedIndices.toImmutableSet()
                            ),
                            activeLineIndex = 4,
                            variables = compareVars + ("action" to "swap(i, j)"),
                            callStack = callStack,
                            hapticCue = HapticCue.SWAP_THUD
                        )
                    }
                }
            }
        }

        // Final pivot swap: swap items[i + 1] with items[high]
        val finalPivotIndex = i + 1
        if (finalPivotIndex != high) {
            val temp = items[finalPivotIndex]
            items[finalPivotIndex] = items[high]
            items[high] = temp
            accumulator.incSwaps()
            accumulator.incMutations()
        }

        sortedIndices.add(finalPivotIndex)

        // Line 6: Pivot placed and locked in its permanent sorted position
        if (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 2) {
            accumulator.emitFrame(
                structureState = initialState.copy(
                    items = items.toImmutableList(),
                    pointers = persistentMapOf("pivotLocked" to finalPivotIndex),
                    activeIndices = persistentSetOf(finalPivotIndex),
                    mutatingIndices = if (finalPivotIndex != high) persistentSetOf(finalPivotIndex, high) else persistentSetOf(),
                    sortedIndices = sortedIndices.toImmutableSet()
                ),
                activeLineIndex = 6,
                variables = mapOf(
                    "pivotLockedIndex" to finalPivotIndex.toString(),
                    "pivotValue" to items[finalPivotIndex].value.toString()
                ),
                callStack = callStack,
                hapticCue = HapticCue.SUCCESS_LOCK
            )
        }

        return finalPivotIndex
    }
}
