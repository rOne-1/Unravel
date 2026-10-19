package com.unravel.core.engine.generator

import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.VisualStructureState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class Cluster2GeneratorsTest {

    private val generators: List<AlgorithmGenerator<VisualStructureState.ArrayState>> = listOf(
        BubbleSortGenerator(),
        InsertionSortGenerator(),
        MergeSortGenerator(),
        QuickSortGenerator()
    )

    @ParameterizedTest
    @EnumSource(InputPreset::class)
    fun `all 4 sorting generators sort all presets correctly`(preset: InputPreset) {
        val size = 12
        val initialState = InputPresetGenerator.generateInitialState(preset, size = size, seed = 777L)

        for (generator in generators) {
            val frames = generator.generateFrames(initialState)
            assertTrue(frames.isNotEmpty(), "${generator.algorithmId} produced empty frames")
            assertTrue(frames.size <= AlgorithmGenerator.MAX_FRAMES, "${generator.algorithmId} exceeded max frames")

            // Verify final frame is sorted
            val finalState = frames.last().structureState as VisualStructureState.ArrayState
            assertEquals(size, finalState.items.size)

            val values = finalState.items.map { it.value }
            for (i in 0 until values.size - 1) {
                assertTrue(
                    values[i] <= values[i + 1],
                    "${generator.algorithmId} on preset $preset failed to sort: $values"
                )
            }

            // Verify all indices are marked sorted
            assertEquals((0 until size).toSet(), finalState.sortedIndices)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [4, 10, 30, 60])
    fun `sorting generators handle boundary and large sizes`(size: Int) {
        val initialState = InputPresetGenerator.generateInitialState(InputPreset.REVERSED, size = size, seed = 42L)

        for (generator in generators) {
            val frames = generator.generateFrames(initialState)
            assertTrue(frames.isNotEmpty())
            assertTrue(frames.size <= AlgorithmGenerator.MAX_FRAMES)

            val finalState = frames.last().structureState as VisualStructureState.ArrayState
            val values = finalState.items.map { it.value }
            for (i in 0 until values.size - 1) {
                assertTrue(values[i] <= values[i + 1], "${generator.algorithmId} failed on size $size")
            }
        }
    }

    @Test
    fun `BubbleSort emits SWAP_THUD and populates mutatingIndices on swaps`() {
        val initialState = InputPresetGenerator.generateInitialState(InputPreset.REVERSED, size = 6, seed = 1L)
        val frames = BubbleSortGenerator().generateFrames(initialState)

        val swapFrames = frames.filter { it.hapticCue == HapticCue.SWAP_THUD }
        assertTrue(swapFrames.isNotEmpty())
        swapFrames.forEach { frame ->
            val state = frame.structureState as VisualStructureState.ArrayState
            assertTrue(state.mutatingIndices.isNotEmpty(), "Swap frame must have non-empty mutatingIndices")
        }
    }

    @Test
    fun `MergeSort and QuickSort populate callStack frames and track recursion depth`() {
        val initialState = InputPresetGenerator.generateInitialState(InputPreset.RANDOM, size = 16, seed = 99L)

        val mergeFrames = MergeSortGenerator().generateFrames(initialState)
        assertTrue(mergeFrames.any { it.callStack.isNotEmpty() })
        val mergeMaxDepth = mergeFrames.last().metrics.maxRecursionDepth
        assertTrue(mergeMaxDepth >= 3, "Expected MergeSort recursion depth >= 3, but was $mergeMaxDepth")

        val quickFrames = QuickSortGenerator().generateFrames(initialState)
        assertTrue(quickFrames.any { it.callStack.isNotEmpty() })
        val quickMaxDepth = quickFrames.last().metrics.maxRecursionDepth
        assertTrue(quickMaxDepth >= 2, "Expected QuickSort recursion depth >= 2, but was $quickMaxDepth")
    }

    @Test
    fun `MergeSort utilizes auxiliaryItems buffer during merge`() {
        val initialState = InputPresetGenerator.generateInitialState(InputPreset.RANDOM, size = 10, seed = 123L)
        val frames = MergeSortGenerator().generateFrames(initialState)

        val auxFrames = frames.filter {
            val state = it.structureState as VisualStructureState.ArrayState
            state.auxiliaryItems.isNotEmpty()
        }
        assertTrue(auxFrames.isNotEmpty(), "MergeSort must utilize auxiliaryItems during merging")
    }
}
