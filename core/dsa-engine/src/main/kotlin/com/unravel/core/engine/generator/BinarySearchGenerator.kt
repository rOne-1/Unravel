package com.unravel.core.engine.generator

import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ArrayNode
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Cluster 1: Binary Search Sequence Generator.
 * Divide-and-conquer search on sorted array marking discarded ranges and low/mid/high pointers.
 */
class BinarySearchGenerator(
    private val customTarget: Int? = null
) : AlgorithmGenerator<VisualStructureState.ArrayState> {

    override val algorithmId: AlgorithmId = AlgorithmId.BINARY_SEARCH

    override fun generateFrames(initialState: VisualStructureState.ArrayState): ImmutableList<AlgorithmFrame> {
        val accumulator = FrameAccumulator(algorithmId)
        if (initialState.items.isEmpty()) {
            accumulator.emitFrame(initialState, activeLineIndex = 1)
            return accumulator.build()
        }

        // Ensure array is sorted for binary search
        val isSorted = initialState.items.zipWithNext().all { it.first.value <= it.second.value }
        val items: ImmutableList<ArrayNode> = if (isSorted) {
            initialState.items
        } else {
            initialState.items.sortedBy { it.value }.toImmutableList()
        }

        val n = items.size
        val target = customTarget ?: items[minOf(n - 1, n * 3 / 4)].value

        var low = 0
        var high = n - 1
        var discarded: IntRange? = null
        var foundIndex = -1

        // Line 1: Initialize low and high
        val initialVars = mapOf(
            "low" to low.toString(),
            "high" to high.toString(),
            "target" to target.toString()
        )
        accumulator.emitFrame(
            structureState = initialState.copy(
                items = items,
                pointers = persistentMapOf("low" to low, "high" to high),
                activeIndices = persistentSetOf(low, high)
            ),
            activeLineIndex = 1,
            variables = initialVars
        )

        while (low <= high && accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES) {
            accumulator.incComparisons() // while (low <= high)

            val mid = low + (high - low) / 2
            val midVal = items[mid].value

            val stepVars = mapOf(
                "low" to low.toString(),
                "mid" to mid.toString(),
                "high" to high.toString(),
                "arr[mid]" to midVal.toString(),
                "target" to target.toString()
            )

            // Line 3: Compute mid
            accumulator.emitFrame(
                structureState = initialState.copy(
                    items = items,
                    pointers = persistentMapOf("low" to low, "mid" to mid, "high" to high),
                    activeIndices = persistentSetOf(mid),
                    discardedRange = discarded
                ),
                activeLineIndex = 3,
                variables = stepVars
            )

            // Line 4-5: Check arr[mid] == target
            accumulator.incComparisons()
            if (midVal == target) {
                foundIndex = mid
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items,
                        pointers = persistentMapOf("mid" to mid),
                        activeIndices = persistentSetOf(mid),
                        sortedIndices = persistentSetOf(mid),
                        discardedRange = discarded
                    ),
                    activeLineIndex = 5,
                    variables = stepVars + ("status" to "FOUND"),
                    hapticCue = HapticCue.SUCCESS_LOCK,
                    expectedUserAction = ExpectedAction.KeepAndAdvance
                )
                break
            } else if (midVal < target) {
                // Line 6-7: arr[mid] < target -> discard left half and search right
                val nextLow = mid + 1
                discarded = 0..mid
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items,
                        pointers = persistentMapOf("low" to low, "mid" to mid, "high" to high),
                        activeIndices = persistentSetOf(mid),
                        discardedRange = discarded
                    ),
                    activeLineIndex = 7,
                    variables = stepVars + ("action" to "low = mid + 1"),
                    expectedUserAction = ExpectedAction.MovePointer("low", nextLow)
                )

                low = nextLow
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items,
                        pointers = if (low <= high) persistentMapOf("low" to low, "high" to high) else persistentMapOf("low" to low),
                        activeIndices = if (low <= high) persistentSetOf(low, high) else persistentSetOf(low),
                        discardedRange = discarded
                    ),
                    activeLineIndex = 7,
                    variables = stepVars + ("low" to low.toString()),
                    hapticCue = HapticCue.TICK
                )
            } else {
                // Line 8-9: arr[mid] > target -> discard right half and search left
                val nextHigh = mid - 1
                discarded = mid until n
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items,
                        pointers = persistentMapOf("low" to low, "mid" to mid, "high" to high),
                        activeIndices = persistentSetOf(mid),
                        discardedRange = discarded
                    ),
                    activeLineIndex = 9,
                    variables = stepVars + ("action" to "high = mid - 1"),
                    expectedUserAction = ExpectedAction.MovePointer("high", nextHigh)
                )

                high = nextHigh
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items,
                        pointers = if (low <= high) persistentMapOf("low" to low, "high" to high) else persistentMapOf("high" to high),
                        activeIndices = if (low <= high) persistentSetOf(low, high) else persistentSetOf(high),
                        discardedRange = discarded
                    ),
                    activeLineIndex = 9,
                    variables = stepVars + ("high" to high.toString()),
                    hapticCue = HapticCue.TICK
                )
            }
        }

        // Line 10: Final frame
        accumulator.emitFrame(
            structureState = initialState.copy(
                items = items,
                pointers = if (foundIndex != -1) persistentMapOf("mid" to foundIndex) else persistentMapOf(),
                activeIndices = if (foundIndex != -1) persistentSetOf(foundIndex) else persistentSetOf(),
                sortedIndices = if (foundIndex != -1) persistentSetOf(foundIndex) else persistentSetOf(),
                discardedRange = discarded
            ),
            activeLineIndex = 10,
            variables = mapOf(
                "result" to foundIndex.toString(),
                "comparisons" to accumulator.currentComparisons.toString()
            )
        )

        return accumulator.build()
    }
}
