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
import kotlinx.collections.immutable.toImmutableSet

/**
 * Cluster 2: Bubble Sort Sequence Generator.
 * Adjacent pairwise comparisons, parabolic arc markers on swap, and expanding sorted suffix.
 */
class BubbleSortGenerator : AlgorithmGenerator<VisualStructureState.ArrayState> {

    override val algorithmId: AlgorithmId = AlgorithmId.BUBBLE_SORT

    override fun generateFrames(initialState: VisualStructureState.ArrayState): ImmutableList<AlgorithmFrame> {
        val accumulator = FrameAccumulator(algorithmId)
        val items = initialState.items.toMutableList()
        val n = items.size

        if (n < 2) {
            accumulator.emitFrame(initialState, activeLineIndex = 1)
            return accumulator.build()
        }

        val sortedIndices = mutableSetOf<Int>()

        // Line 1: Outer loop setup
        accumulator.emitFrame(
            structureState = initialState.copy(items = items.toImmutableList()),
            activeLineIndex = 1,
            variables = mapOf("n" to n.toString(), "comparisons" to "0", "swaps" to "0")
        )

        for (i in 0 until n - 1) {
            var swappedInPass = false

            for (j in 0 until n - 1 - i) {
                accumulator.incComparisons()
                val needsSwap = items[j].value > items[j + 1].value

                // Emit comparison frame if size allows rich stepping
                if (n <= 25 || accumulator.frameCount < 800) {
                    val vars = mapOf(
                        "i" to i.toString(),
                        "j" to j.toString(),
                        "arr[j]" to items[j].value.toString(),
                        "arr[j+1]" to items[j + 1].value.toString(),
                        "comparisons" to accumulator.currentComparisons.toString(),
                        "swaps" to accumulator.currentSwaps.toString()
                    )

                    accumulator.emitFrame(
                        structureState = initialState.copy(
                            items = items.toImmutableList(),
                            pointers = persistentMapOf("j" to j, "j+1" to j + 1),
                            activeIndices = persistentSetOf(j, j + 1),
                            sortedIndices = sortedIndices.toImmutableSet()
                        ),
                        activeLineIndex = 3,
                        variables = vars,
                        hapticCue = HapticCue.TICK,
                        expectedUserAction = if (needsSwap) ExpectedAction.SwapIndices(j, j + 1) else ExpectedAction.KeepAndAdvance
                    )
                }

                if (needsSwap) {
                    // Line 4-5: Swap elements
                    val temp = items[j]
                    items[j] = items[j + 1]
                    items[j + 1] = temp
                    swappedInPass = true

                    accumulator.incSwaps()
                    accumulator.incMutations()

                    if (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 2) {
                        accumulator.emitFrame(
                            structureState = initialState.copy(
                                items = items.toImmutableList(),
                                pointers = persistentMapOf("j" to j, "j+1" to j + 1),
                                activeIndices = persistentSetOf(j, j + 1),
                                mutatingIndices = persistentSetOf(j, j + 1),
                                sortedIndices = sortedIndices.toImmutableSet()
                            ),
                            activeLineIndex = 5,
                            variables = mapOf(
                                "action" to "swap(j, j+1)",
                                "swaps" to accumulator.currentSwaps.toString()
                            ),
                            hapticCue = HapticCue.SWAP_THUD
                        )
                    }
                }
            }

            // Lock suffix element (n - 1 - i) into sorted set
            sortedIndices.add(n - 1 - i)

            if (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 2) {
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items.toImmutableList(),
                        sortedIndices = sortedIndices.toImmutableSet()
                    ),
                    activeLineIndex = 6,
                    variables = mapOf("lockedIndex" to (n - 1 - i).toString()),
                    hapticCue = HapticCue.SUCCESS_LOCK
                )
            }

            // Early exit optimization if array is fully sorted
            if (!swappedInPass) {
                for (k in 0 until n - 1 - i) sortedIndices.add(k)
                break
            }
        }

        // Lock all elements at completion
        for (idx in 0 until n) sortedIndices.add(idx)

        // Line 7: Final sorted frame
        accumulator.emitFrame(
            structureState = initialState.copy(
                items = items.toImmutableList(),
                sortedIndices = sortedIndices.toImmutableSet()
            ),
            activeLineIndex = 7,
            variables = mapOf(
                "status" to "SORTED",
                "totalComparisons" to accumulator.currentComparisons.toString(),
                "totalSwaps" to accumulator.currentSwaps.toString()
            ),
            hapticCue = HapticCue.SUCCESS_LOCK
        )

        return accumulator.build()
    }
}
