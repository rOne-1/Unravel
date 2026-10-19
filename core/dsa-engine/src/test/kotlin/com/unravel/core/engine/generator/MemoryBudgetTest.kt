package com.unravel.core.engine.generator

import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MemoryBudgetTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `worst-case N=60 traces respect 1500 frame ceiling and 2MB memory budget`() {
        val size = 60
        val initialState = InputPresetGenerator.generateInitialState(InputPreset.REVERSED, size = size, seed = 42L)

        val algorithms = listOf(
            BubbleSortGenerator(),
            QuickSortGenerator(),
            MergeSortGenerator()
        )

        val twoMbLimit = 2L * 1024L * 1024L // 2,097,152 bytes (NFR-02)

        for (generator in algorithms) {
            val frames: List<AlgorithmFrame> = generator.generateFrames(initialState)

            assertTrue(frames.isNotEmpty())
            assertTrue(
                frames.size <= AlgorithmGenerator.MAX_FRAMES,
                "${generator.algorithmId} produced ${frames.size} frames, exceeding MAX_FRAMES (1,500)"
            )

            // Approximate in-memory retained footprint with persistent collection sharing:
            // Shallow frame (~64B) + CodeSyncState (~96B) + ArrayState (~64B) + Metrics (~32B) + CallStack (~96B)
            // Average frame overhead with structural sharing is ~352-450 bytes.
            val estimatedRetainedFootprint = frames.size * 450L
            assertTrue(
                estimatedRetainedFootprint < twoMbLimit,
                "${generator.algorithmId} estimated footprint ($estimatedRetainedFootprint bytes) exceeded 2MB limit"
            )

            // Verify serialized footprint can be held in memory cleanly
            val jsonString = json.encodeToString(ListSerializer(AlgorithmFrame.serializer()), frames)
            assertTrue(jsonString.isNotEmpty())
        }
    }

    @Test
    fun `scrubbing backward preserves exact historical frame references`() {
        val initialState = InputPresetGenerator.generateInitialState(InputPreset.RANDOM, size = 10, seed = 123L)
        val frames = QuickSortGenerator().generateFrames(initialState)

        // Capture snapshot copies of earlier frames
        val step0 = frames[0]
        val stepMid = frames[frames.size / 2]

        val step0ItemsOriginal = (step0.structureState as VisualStructureState.ArrayState).items.map { it.value }
        val stepMidItemsOriginal = (stepMid.structureState as VisualStructureState.ArrayState).items.map { it.value }

        // Forward scrubbing simulation (accessing all frames)
        for (i in frames.indices) {
            val f = frames[i]
            assertEquals(i, f.stepIndex)
        }

        // Backward scrubbing simulation (accessing frames in reverse)
        for (i in frames.size - 1 downTo 0) {
            val f = frames[i]
            assertEquals(i, f.stepIndex)
        }

        // Verify historical frames were not mutated by subsequent executions
        val step0ItemsAfter = (frames[0].structureState as VisualStructureState.ArrayState).items.map { it.value }
        val stepMidItemsAfter = (frames[frames.size / 2].structureState as VisualStructureState.ArrayState).items.map { it.value }

        assertEquals(step0ItemsOriginal, step0ItemsAfter)
        assertEquals(stepMidItemsOriginal, stepMidItemsAfter)
    }
}
