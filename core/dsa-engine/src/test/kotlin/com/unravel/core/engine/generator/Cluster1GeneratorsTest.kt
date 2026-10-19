package com.unravel.core.engine.generator

import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.VisualStructureState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class Cluster1GeneratorsTest {

    @ParameterizedTest
    @EnumSource(InputPreset::class)
    fun `TwoPointersGenerator produces valid frames across all presets`(preset: InputPreset) {
        val initialState = InputPresetGenerator.generateInitialState(preset, size = 12, seed = 42L)
        val generator = TwoPointersGenerator()

        assertEquals(AlgorithmId.TWO_POINTERS, generator.algorithmId)
        val frames = generator.generateFrames(initialState)

        assertTrue(frames.isNotEmpty())
        assertTrue(frames.size <= 1_500)

        // Frame indices are strictly sequential 0..frames.size - 1
        frames.forEachIndexed { index, frame ->
            assertEquals(index, frame.stepIndex)
            assertTrue(frame.codeSync.activeLineIndex in 1..10)
        }

        // Check metrics monotonically increase
        for (i in 0 until frames.size - 1) {
            assertTrue(frames[i].metrics.comparisons <= frames[i + 1].metrics.comparisons)
        }

        // Verify pointers exist in states
        val intermediateFrame = frames.firstOrNull {
            val state = it.structureState
            state is VisualStructureState.ArrayState && state.pointers.isNotEmpty()
        }
        assertNotNull(intermediateFrame)

        // At least one expected action or match
        val hasExpectedAction = frames.any { it.expectedUserAction != null }
        assertTrue(hasExpectedAction)
    }

    @ParameterizedTest
    @EnumSource(InputPreset::class)
    fun `SlidingWindowGenerator correctly finds max sum subarray`(preset: InputPreset) {
        val size = 15
        val k = 4
        val initialState = InputPresetGenerator.generateInitialState(preset, size = size, seed = 99L)
        val generator = SlidingWindowGenerator(windowSize = k)

        assertEquals(AlgorithmId.SLIDING_WINDOW, generator.algorithmId)
        val frames = generator.generateFrames(initialState)

        assertTrue(frames.isNotEmpty())

        // Calculate expected max sum by brute force
        val items = initialState.items.map { it.value }
        var expectedMax = Int.MIN_VALUE
        for (i in 0..size - k) {
            val sum = items.subList(i, i + k).sum()
            if (sum > expectedMax) expectedMax = sum
        }

        val lastFrame = frames.last()
        val reportedMax = lastFrame.codeSync.variableBindings["maxSum"]?.toInt()
        assertEquals(expectedMax, reportedMax)

        // Verify window ranges are populated
        val windowFrames = frames.filter {
            val st = it.structureState as VisualStructureState.ArrayState
            st.windowRange != null
        }
        assertTrue(windowFrames.isNotEmpty())
        windowFrames.forEach {
            val st = it.structureState as VisualStructureState.ArrayState
            val range = st.windowRange ?: error("windowRange should not be null")
            assertEquals(k, range.last - range.first + 1)
        }
    }

    @ParameterizedTest
    @EnumSource(InputPreset::class)
    fun `BinarySearchGenerator correctly finds target in sorted array`(preset: InputPreset) {
        val size = 16
        val initialState = InputPresetGenerator.generateInitialState(preset, size = size, seed = 1234L)

        // Sort items for binary search verification
        val sortedNodes = initialState.items.sortedBy { it.value }
        val target = sortedNodes[size / 2].value

        val generator = BinarySearchGenerator(customTarget = target)
        assertEquals(AlgorithmId.BINARY_SEARCH, generator.algorithmId)

        val frames = generator.generateFrames(initialState)
        assertTrue(frames.isNotEmpty())

        val lastFrame = frames.last()
        val foundIndex = lastFrame.codeSync.variableBindings["result"]?.toInt()
        assertNotNull(foundIndex)
        assertTrue(foundIndex!! >= 0)

        // Verify that target was indeed located at foundIndex in sorted array
        val arrayState = lastFrame.structureState as VisualStructureState.ArrayState
        assertEquals(target, arrayState.items[foundIndex].value)

        // Success lock haptic cue was emitted
        assertTrue(frames.any { it.hapticCue == HapticCue.SUCCESS_LOCK })
    }

    @Test
    fun `BinarySearchGenerator handles target not found gracefully`() {
        val initialState = InputPresetGenerator.generateInitialState(InputPreset.RANDOM, size = 10, seed = 55L)
        val generator = BinarySearchGenerator(customTarget = 99999) // guaranteed not found

        val frames = generator.generateFrames(initialState)
        val lastFrame = frames.last()
        val foundIndex = lastFrame.codeSync.variableBindings["result"]?.toInt()
        assertEquals(-1, foundIndex)
    }
}
