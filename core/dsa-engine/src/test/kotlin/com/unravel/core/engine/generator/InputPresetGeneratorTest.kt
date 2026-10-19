package com.unravel.core.engine.generator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class InputPresetGeneratorTest {

    @ParameterizedTest
    @EnumSource(InputPreset::class)
    fun `generateNodes produces exact requested size`(preset: InputPreset) {
        val size = 12
        val nodes = InputPresetGenerator.generateNodes(preset = preset, size = size, seed = 42L)
        assertEquals(size, nodes.size)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 61, 100])
    fun `generateNodes rejects out of bounds sizes`(invalidSize: Int) {
        assertThrows(IllegalArgumentException::class.java) {
            InputPresetGenerator.generateNodes(InputPreset.RANDOM, size = invalidSize)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [InputPresetGenerator.MIN_SIZE, 10, 30, InputPresetGenerator.MAX_SIZE])
    fun `boundary sizes generate successfully`(validSize: Int) {
        val nodes = InputPresetGenerator.generateNodes(InputPreset.RANDOM, size = validSize, seed = 123L)
        assertEquals(validSize, nodes.size)
    }

    @Test
    fun `every node has unique UUID`() {
        val nodes = InputPresetGenerator.generateNodes(InputPreset.RANDOM, size = 60, seed = 999L)
        val uniqueIds = nodes.map { it.id }.toSet()
        assertEquals(60, uniqueIds.size)
    }

    @Test
    fun `reversed preset is strictly monotonically decreasing`() {
        val sizes = listOf(4, 10, 30, 60)
        for (size in sizes) {
            val nodes = InputPresetGenerator.generateNodes(InputPreset.REVERSED, size = size, seed = 1L)
            for (i in 0 until nodes.size - 1) {
                assertTrue(
                    nodes[i].value > nodes[i + 1].value,
                    "Expected node[$i] (${nodes[i].value}) > node[${i + 1}] (${nodes[i + 1].value}) for size $size"
                )
            }
        }
    }

    @Test
    fun `duplicates preset contains recurring identical values`() {
        val size = 20
        val nodes = InputPresetGenerator.generateNodes(InputPreset.DUPLICATES, size = size, seed = 7L)
        val distinctValues = nodes.map { it.value }.toSet()
        assertTrue(distinctValues.size in 3..4, "Expected 3-4 distinct values, but was ${distinctValues.size}")
        assertTrue(distinctValues.size < size, "Expected cardinality < size")
    }

    @Test
    fun `nearly sorted preset contains inversions`() {
        val nodes = InputPresetGenerator.generateNodes(InputPreset.NEARLY_SORTED, size = 10, seed = 42L)
        var inversions = 0
        for (i in 0 until nodes.size - 1) {
            if (nodes[i].value > nodes[i + 1].value) inversions++
        }
        assertTrue(inversions in 1..4, "Expected 1-4 inversions in nearly sorted array, but found $inversions")
    }

    @Test
    fun `deterministic seeding produces identical sequences`() {
        val nodes1 = InputPresetGenerator.generateNodes(InputPreset.RANDOM, size = 15, seed = 1337L)
        val nodes2 = InputPresetGenerator.generateNodes(InputPreset.RANDOM, size = 15, seed = 1337L)
        val nodes3 = InputPresetGenerator.generateNodes(InputPreset.RANDOM, size = 15, seed = 9999L)

        assertEquals(nodes1.map { it.value }, nodes2.map { it.value })
        assertNotEquals(nodes1.map { it.value }, nodes3.map { it.value })
    }

    @Test
    fun `generateInitialState wraps nodes into ArrayState`() {
        val state = InputPresetGenerator.generateInitialState(InputPreset.RANDOM, size = 8, seed = 55L)
        assertEquals(8, state.items.size)
        assertTrue(state.activeIndices.isEmpty())
        assertTrue(state.mutatingIndices.isEmpty())
        assertTrue(state.sortedIndices.isEmpty())
    }
}
