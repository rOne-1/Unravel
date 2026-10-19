package com.unravel.core.engine.generator

import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.VisualStructureState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ComprehensiveCurriculumTest {

    companion object {
        @JvmStatic
        fun curriculumCombinations(): Stream<Arguments> {
            val algorithms: List<AlgorithmGenerator<VisualStructureState.ArrayState>> = listOf(
                TwoPointersGenerator(),
                SlidingWindowGenerator(),
                BinarySearchGenerator(),
                BubbleSortGenerator(),
                InsertionSortGenerator(),
                MergeSortGenerator(),
                QuickSortGenerator()
            )
            val presets = InputPreset.entries

            return algorithms.flatMap { algo ->
                presets.map { preset -> Arguments.of(algo, preset) }
            }.stream()
        }
    }

    @ParameterizedTest(name = "{0} with preset {1}")
    @MethodSource("curriculumCombinations")
    fun `curriculum combination produces valid frame trace`(
        generator: AlgorithmGenerator<VisualStructureState.ArrayState>,
        preset: InputPreset
    ) {
        val size = 10
        val initialState = InputPresetGenerator.generateInitialState(preset, size = size, seed = 555L)

        val frames = generator.generateFrames(initialState)

        assertTrue(frames.isNotEmpty(), "${generator.algorithmId} produced empty frames for preset $preset")
        assertTrue(frames.size <= AlgorithmGenerator.MAX_FRAMES, "${generator.algorithmId} exceeded max frames for preset $preset")

        val lastFrame = frames.last()
        assertEquals(frames.size - 1, lastFrame.stepIndex)

        // For sorting algorithms, assert final array is strictly sorted
        if (generator.algorithmId in listOf(
                AlgorithmId.BUBBLE_SORT,
                AlgorithmId.INSERTION_SORT,
                AlgorithmId.MERGE_SORT,
                AlgorithmId.QUICK_SORT
            )
        ) {
            val finalState = lastFrame.structureState as VisualStructureState.ArrayState
            assertEquals(size, finalState.items.size)
            val values = finalState.items.map { it.value }
            for (i in 0 until values.size - 1) {
                assertTrue(
                    values[i] <= values[i + 1],
                    "${generator.algorithmId} failed to sort $preset: $values"
                )
            }
        }
    }
}
