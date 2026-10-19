package com.unravel.core.engine.generator

import com.unravel.core.engine.model.ArrayNode
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Deterministic input dataset generator providing array presets (RANDOM, NEARLY_SORTED, REVERSED, DUPLICATES)
 * for linear array algorithms.
 *
 * Configurable size parameter N in [4, 60] with optional deterministic seeding.
 * Pure Kotlin JVM only.
 */
object InputPresetGenerator {
    const val MIN_SIZE: Int = 4
    const val MAX_SIZE: Int = 60
    const val DEFAULT_SIZE: Int = 10

    @OptIn(ExperimentalUuidApi::class)
    fun generateNodes(
        preset: InputPreset,
        size: Int = DEFAULT_SIZE,
        seed: Long? = null
    ): ImmutableList<ArrayNode> {
        require(size in MIN_SIZE..MAX_SIZE) {
            "Array size must be between $MIN_SIZE and $MAX_SIZE, but was $size"
        }
        val random = if (seed != null) Random(seed) else Random.Default

        val values: List<Int> = when (preset) {
            InputPreset.RANDOM -> {
                // Unique integers in range 10..99
                (10..99).shuffled(random).take(size)
            }
            InputPreset.NEARLY_SORTED -> {
                // Strictly sorted distinct values with 1-2 guaranteed inversions
                val sortedBase = (10..99).shuffled(random).take(size).sorted().toMutableList()
                val swapCount = if (size >= 8) 2 else 1
                // First swap: pick an index and swap with adjacent
                val idx1 = random.nextInt(size - 1)
                val temp1 = sortedBase[idx1]
                sortedBase[idx1] = sortedBase[idx1 + 1]
                sortedBase[idx1 + 1] = temp1

                if (swapCount > 1) {
                    // Second swap: pick distant index if possible
                    val idx2 = (0 until size - 2).filter { it != idx1 && it != idx1 + 1 }.randomOrNull(random) ?: 0
                    val targetIdx = minOf(size - 1, idx2 + 2)
                    val temp2 = sortedBase[idx2]
                    sortedBase[idx2] = sortedBase[targetIdx]
                    sortedBase[targetIdx] = temp2
                }
                sortedBase
            }
            InputPreset.REVERSED -> {
                // Strictly monotonically decreasing integers
                val step = maxOf(1, 80 / size)
                (0 until size).map { 95 - it * step }
            }
            InputPreset.DUPLICATES -> {
                // Restricted palette of 3-4 distinct values ensuring recurring identical elements
                val palette = if (size < 6) listOf(24, 52, 78) else listOf(18, 42, 65, 88)
                // Fill array and ensure all palette elements are present
                val list = ArrayList<Int>(size)
                palette.forEach { list.add(it) }
                while (list.size < size) {
                    list.add(palette[random.nextInt(palette.size)])
                }
                list.shuffled(random)
            }
        }

        return values.map { ArrayNode(id = Uuid.random(), value = it) }.toImmutableList()
    }

    fun generateInitialState(
        preset: InputPreset,
        size: Int = DEFAULT_SIZE,
        seed: Long? = null
    ): VisualStructureState.ArrayState {
        return VisualStructureState.ArrayState(items = generateNodes(preset, size, seed))
    }

    @OptIn(ExperimentalUuidApi::class)
    fun generateLinkedListState(
        values: List<Int>,
        cycleEntryIndex: Int? = null
    ): VisualStructureState.LinkedNodeState {
        if (values.isEmpty()) {
            return VisualStructureState.LinkedNodeState(nodes = kotlinx.collections.immutable.persistentListOf())
        }
        val ids = values.map { Uuid.random() }
        val nodes = values.indices.map { i ->
            val nextId = when {
                i == values.lastIndex -> cycleEntryIndex?.let { ids[it] }
                else -> ids[i + 1]
            }
            val prevId = if (i > 0) ids[i - 1] else null
            com.unravel.core.engine.model.LinkedNode(
                id = ids[i],
                value = values[i],
                nextId = nextId,
                prevId = prevId
            )
        }.toImmutableList()

        val pointers = kotlinx.collections.immutable.persistentMapOf("head" to ids.first().toString())
        return VisualStructureState.LinkedNodeState(
            nodes = nodes,
            pointers = pointers,
            isCycleDetected = false
        )
    }
}
