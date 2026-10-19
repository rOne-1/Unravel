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
 * Cluster 1: Two Pointers Sequence Generator.
 * Narrowing pointers (left, right) finding target pair sum in sorted array.
 */
class TwoPointersGenerator(
    private val customTarget: Int? = null
) : AlgorithmGenerator<VisualStructureState.ArrayState> {

    override val algorithmId: AlgorithmId = AlgorithmId.TWO_POINTERS

    override fun generateFrames(initialState: VisualStructureState.ArrayState): ImmutableList<AlgorithmFrame> {
        val accumulator = FrameAccumulator(algorithmId)
        if (initialState.items.size < 2) {
            accumulator.emitFrame(initialState, activeLineIndex = 1)
            return accumulator.build()
        }

        // Ensure array is sorted for two pointers pair search
        val isSorted = initialState.items.zipWithNext().all { it.first.value <= it.second.value }
        val items: ImmutableList<ArrayNode> = if (isSorted) {
            initialState.items
        } else {
            initialState.items.sortedBy { it.value }.toImmutableList()
        }

        val target = customTarget ?: (items[items.size / 4].value + items[3 * items.size / 4].value)

        var left = 0
        var right = items.size - 1

        // Line 1: Initialize pointers
        val initialVars = mapOf(
            "left" to left.toString(),
            "right" to right.toString(),
            "target" to target.toString()
        )
        accumulator.emitFrame(
            structureState = initialState.copy(
                items = items,
                pointers = persistentMapOf("left" to left, "right" to right),
                activeIndices = persistentSetOf(left, right)
            ),
            activeLineIndex = 1,
            variables = initialVars
        )

        var matchFound = false

        while (left < right && accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES) {
            accumulator.incComparisons() // while (left < right)

            val currentSum = items[left].value + items[right].value
            val vars = mapOf(
                "left" to left.toString(),
                "right" to right.toString(),
                "arr[left]" to items[left].value.toString(),
                "arr[right]" to items[right].value.toString(),
                "currentSum" to currentSum.toString(),
                "target" to target.toString()
            )

            // Line 3: Compute currentSum
            val currentArrayState = initialState.copy(
                items = items,
                pointers = persistentMapOf("left" to left, "right" to right),
                activeIndices = persistentSetOf(left, right)
            )
            accumulator.emitFrame(
                structureState = currentArrayState,
                activeLineIndex = 3,
                variables = vars
            )

            accumulator.incComparisons() // if (currentSum == target)
            if (currentSum == target) {
                // Line 4-5: Target match locked!
                matchFound = true
                accumulator.emitFrame(
                    structureState = currentArrayState.copy(
                        sortedIndices = persistentSetOf(left, right)
                    ),
                    activeLineIndex = 5,
                    variables = vars + ("status" to "MATCH_FOUND"),
                    hapticCue = HapticCue.SUCCESS_LOCK,
                    expectedUserAction = ExpectedAction.KeepAndAdvance
                )
                break
            } else if (currentSum < target) {
                // Line 6-7: currentSum < target -> advance left pointer
                val nextLeft = left + 1
                accumulator.emitFrame(
                    structureState = currentArrayState,
                    activeLineIndex = 7,
                    variables = vars + ("action" to "left++"),
                    expectedUserAction = ExpectedAction.MovePointer("left", nextLeft)
                )
                left = nextLeft
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items,
                        pointers = persistentMapOf("left" to left, "right" to right),
                        activeIndices = persistentSetOf(left, right)
                    ),
                    activeLineIndex = 7,
                    variables = vars + ("left" to left.toString()),
                    hapticCue = HapticCue.TICK
                )
            } else {
                // Line 8-9: currentSum > target -> advance right pointer
                val nextRight = right - 1
                accumulator.emitFrame(
                    structureState = currentArrayState,
                    activeLineIndex = 9,
                    variables = vars + ("action" to "right--"),
                    expectedUserAction = ExpectedAction.MovePointer("right", nextRight)
                )
                right = nextRight
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items,
                        pointers = persistentMapOf("left" to left, "right" to right),
                        activeIndices = persistentSetOf(left, right)
                    ),
                    activeLineIndex = 9,
                    variables = vars + ("right" to right.toString()),
                    hapticCue = HapticCue.TICK
                )
            }
        }

        // Line 10: Complete / termination frame
        accumulator.emitFrame(
            structureState = initialState.copy(
                items = items,
                pointers = persistentMapOf("left" to left, "right" to right),
                activeIndices = if (matchFound) persistentSetOf(left, right) else persistentSetOf(),
                sortedIndices = if (matchFound) persistentSetOf(left, right) else persistentSetOf()
            ),
            activeLineIndex = 10,
            variables = mapOf(
                "result" to if (matchFound) "Pair($left, $right)" else "null",
                "comparisons" to accumulator.currentComparisons.toString()
            )
        )

        return accumulator.build()
    }
}
