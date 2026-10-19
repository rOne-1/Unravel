package com.unravel.core.engine.generator

import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

/**
 * Cluster 1: Sliding Window Sequence Generator.
 * Tracks max sum subarray across a sliding window of size K.
 */
class SlidingWindowGenerator(
    private val windowSize: Int? = null
) : AlgorithmGenerator<VisualStructureState.ArrayState> {

    override val algorithmId: AlgorithmId = AlgorithmId.SLIDING_WINDOW

    override fun generateFrames(initialState: VisualStructureState.ArrayState): ImmutableList<AlgorithmFrame> {
        val accumulator = FrameAccumulator(algorithmId)
        val items = initialState.items
        if (items.isEmpty()) {
            accumulator.emitFrame(initialState, activeLineIndex = 1)
            return accumulator.build()
        }

        val n = items.size
        val k = windowSize ?: minOf(n, maxOf(2, n / 3))

        if (k > n) {
            accumulator.emitFrame(initialState, activeLineIndex = 1)
            return accumulator.build()
        }

        // Line 1: Initialize window size K
        var left = 0
        var right = k - 1
        var windowSum = (0 until k).sumOf { items[it].value }
        var maxSum = windowSum
        var bestRange = left..right

        val initialVars = mapOf(
            "k" to k.toString(),
            "left" to left.toString(),
            "right" to right.toString(),
            "windowSum" to windowSum.toString(),
            "maxSum" to maxSum.toString()
        )

        accumulator.emitFrame(
            structureState = initialState.copy(
                windowRange = left..right,
                pointers = persistentMapOf("left" to left, "right" to right),
                activeIndices = (left..right).toImmutableSet()
            ),
            activeLineIndex = 2,
            variables = initialVars,
            hapticCue = HapticCue.SUCCESS_LOCK
        )

        while (right < n - 1 && accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES) {
            accumulator.incComparisons() // while (right < n - 1)

            val currentVars = mapOf(
                "left" to left.toString(),
                "right" to right.toString(),
                "windowSum" to windowSum.toString(),
                "maxSum" to maxSum.toString()
            )

            // Line 3: Prepare to advance window
            accumulator.emitFrame(
                structureState = initialState.copy(
                    windowRange = left..right,
                    pointers = persistentMapOf("left" to left, "right" to right),
                    activeIndices = (left..right).toImmutableSet()
                ),
                activeLineIndex = 3,
                variables = currentVars,
                expectedUserAction = ExpectedAction.MovePointer("right", right + 1)
            )

            // Line 4-6: Slide window right
            val incoming = items[right + 1].value
            val outgoing = items[left].value
            right++
            windowSum += incoming - outgoing
            left++

            val slideVars = mapOf(
                "left" to left.toString(),
                "right" to right.toString(),
                "incoming" to incoming.toString(),
                "outgoing" to outgoing.toString(),
                "windowSum" to windowSum.toString(),
                "maxSum" to maxSum.toString()
            )

            accumulator.emitFrame(
                structureState = initialState.copy(
                    windowRange = left..right,
                    pointers = persistentMapOf("left" to left, "right" to right),
                    activeIndices = (left..right).toImmutableSet()
                ),
                activeLineIndex = 6,
                variables = slideVars,
                hapticCue = HapticCue.TICK
            )

            // Line 7-8: Compare windowSum > maxSum
            accumulator.incComparisons()
            if (windowSum > maxSum) {
                maxSum = windowSum
                bestRange = left..right

                accumulator.emitFrame(
                    structureState = initialState.copy(
                        windowRange = left..right,
                        pointers = persistentMapOf("left" to left, "right" to right),
                        activeIndices = (left..right).toImmutableSet(),
                        sortedIndices = bestRange.toImmutableSet()
                    ),
                    activeLineIndex = 8,
                    variables = slideVars + ("maxSum" to maxSum.toString()) + ("newMax" to "true"),
                    hapticCue = HapticCue.SUCCESS_LOCK
                )
            }
        }

        // Line 9: Final result frame
        accumulator.emitFrame(
            structureState = initialState.copy(
                windowRange = bestRange,
                pointers = persistentMapOf("left" to bestRange.first, "right" to bestRange.last),
                activeIndices = bestRange.toImmutableSet(),
                sortedIndices = bestRange.toImmutableSet()
            ),
            activeLineIndex = 9,
            variables = mapOf(
                "maxSum" to maxSum.toString(),
                "bestWindow" to "${bestRange.first}..${bestRange.last}",
                "comparisons" to accumulator.currentComparisons.toString()
            ),
            hapticCue = HapticCue.SUCCESS_LOCK
        )

        return accumulator.build()
    }
}
