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
 * Cluster 2: Insertion Sort Sequence Generator.
 * Key extraction, shifting elements in sorted prefix, and insertion lock.
 */
class InsertionSortGenerator : AlgorithmGenerator<VisualStructureState.ArrayState> {

    override val algorithmId: AlgorithmId = AlgorithmId.INSERTION_SORT

    override fun generateFrames(initialState: VisualStructureState.ArrayState): ImmutableList<AlgorithmFrame> {
        val accumulator = FrameAccumulator(algorithmId)
        val items = initialState.items.toMutableList()
        val n = items.size

        if (n < 2) {
            accumulator.emitFrame(initialState, activeLineIndex = 1)
            return accumulator.build()
        }

        val sortedIndices = mutableSetOf(0)

        // Line 1: Initial state
        accumulator.emitFrame(
            structureState = initialState.copy(
                items = items.toImmutableList(),
                sortedIndices = sortedIndices.toImmutableSet()
            ),
            activeLineIndex = 1,
            variables = mapOf("n" to n.toString(), "comparisons" to "0", "mutations" to "0")
        )

        for (i in 1 until n) {
            val key = items[i]
            var j = i - 1

            val outerVars = mapOf(
                "i" to i.toString(),
                "key" to key.value.toString(),
                "sortedPrefix" to "0..${i - 1}"
            )

            // Line 2: Extract key element
            if (n <= 25 || accumulator.frameCount < 800) {
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items.toImmutableList(),
                        pointers = persistentMapOf("i" to i, "key" to i),
                        activeIndices = persistentSetOf(i),
                        sortedIndices = sortedIndices.toImmutableSet()
                    ),
                    activeLineIndex = 2,
                    variables = outerVars,
                    hapticCue = HapticCue.TICK
                )
            }

            var shifted = false
            while (j >= 0) {
                accumulator.incComparisons() // items[j].value > key.value

                val needsShift = items[j].value > key.value

                // Line 3: Compare arr[j] with key
                if (n <= 25 || accumulator.frameCount < 800) {
                    val compareVars = mapOf(
                        "i" to i.toString(),
                        "j" to j.toString(),
                        "arr[j]" to items[j].value.toString(),
                        "key" to key.value.toString()
                    )

                    accumulator.emitFrame(
                        structureState = initialState.copy(
                            items = items.toImmutableList(),
                            pointers = persistentMapOf("j" to j, "key" to i),
                            activeIndices = persistentSetOf(j, i),
                            sortedIndices = sortedIndices.toImmutableSet()
                        ),
                        activeLineIndex = 3,
                        variables = compareVars,
                        expectedUserAction = if (needsShift) ExpectedAction.SwapIndices(j, j + 1) else ExpectedAction.KeepAndAdvance
                    )
                }

                if (!needsShift) break

                // Line 4-5: Shift element right (items[j + 1] = items[j])
                items[j + 1] = items[j]
                shifted = true
                accumulator.incMutations()

                if (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 2) {
                    accumulator.emitFrame(
                        structureState = initialState.copy(
                            items = items.toImmutableList(),
                            pointers = persistentMapOf("j" to j, "target" to j + 1),
                            activeIndices = persistentSetOf(j, j + 1),
                            mutatingIndices = persistentSetOf(j, j + 1),
                            sortedIndices = sortedIndices.toImmutableSet()
                        ),
                        activeLineIndex = 5,
                        variables = mapOf("action" to "shift arr[$j] -> arr[${j + 1}]"),
                        hapticCue = HapticCue.SWAP_THUD
                    )
                }

                j--
            }

            // Line 6: Insert key into final resting position j + 1
            items[j + 1] = key
            if (shifted) accumulator.incMutations()

            for (k in 0..i) sortedIndices.add(k)

            if (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 2) {
                accumulator.emitFrame(
                    structureState = initialState.copy(
                        items = items.toImmutableList(),
                        pointers = persistentMapOf("inserted" to j + 1),
                        activeIndices = persistentSetOf(j + 1),
                        sortedIndices = sortedIndices.toImmutableSet()
                    ),
                    activeLineIndex = 6,
                    variables = outerVars + ("insertedAt" to (j + 1).toString()),
                    hapticCue = HapticCue.SUCCESS_LOCK
                )
            }
        }

        // Line 7: Final sorted array
        for (idx in 0 until n) sortedIndices.add(idx)

        accumulator.emitFrame(
            structureState = initialState.copy(
                items = items.toImmutableList(),
                sortedIndices = sortedIndices.toImmutableSet()
            ),
            activeLineIndex = 7,
            variables = mapOf(
                "status" to "SORTED",
                "totalComparisons" to accumulator.currentComparisons.toString(),
                "totalMutations" to accumulator.currentMutations.toString()
            ),
            hapticCue = HapticCue.SUCCESS_LOCK
        )

        return accumulator.build()
    }
}
