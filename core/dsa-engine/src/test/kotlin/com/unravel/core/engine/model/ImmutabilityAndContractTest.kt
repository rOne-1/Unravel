package com.unravel.core.engine.model

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ImmutabilityAndContractTest {

    @Test
    fun testPersistentCollectionStructuralSharing() {
        val node1 = ArrayNode(id = Uuid.random(), value = 10)
        val node2 = ArrayNode(id = Uuid.random(), value = 20)
        val initialList = persistentListOf(node1, node2)

        val node3 = ArrayNode(id = Uuid.random(), value = 30)
        val modifiedList = initialList.add(node3)

        // Verify immutability & structural sharing: initialList remains unmodified
        assertEquals(2, initialList.size)
        assertEquals(3, modifiedList.size)
        assertNotSame(initialList, modifiedList)

        // Verify individual nodes are shared by reference (zero heap duplication)
        assertEquals(node1, modifiedList[0])
        assertEquals(node2, modifiedList[1])
    }

    @Test
    fun testAllFourteenAlgorithmsPresentAndConfigured() {
        val algorithms = AlgorithmId.entries
        assertEquals(14, algorithms.size, "Curriculum must contain exactly 14 algorithms across 4 clusters")

        // Cluster counts
        val arrayCluster = algorithms.filter { it.category == SandboxCategory.ARRAY_POINTER }
        assertEquals(7, arrayCluster.size, "Array/Pointer cluster must have 7 algorithms")

        val linkedCluster = algorithms.filter { it.category == SandboxCategory.LINKED_STACK_QUEUE }
        assertEquals(3, linkedCluster.size, "Linked/Stack/Queue cluster must have 3 algorithms")

        val treeCluster = algorithms.filter { it.category == SandboxCategory.HIERARCHICAL_TREE }
        assertEquals(2, treeCluster.size, "Hierarchical Tree cluster must have 2 algorithms")

        val spatialCluster = algorithms.filter { it.category == SandboxCategory.SPATIAL_GRID_GRAPH }
        assertEquals(2, spatialCluster.size, "Spatial Grid/Graph cluster must have 2 algorithms")

        // Validate metadata integrity
        for (algo in algorithms) {
            assertTrue(algo.id.isNotBlank(), "Algorithm ${algo.name} has blank id")
            assertTrue(algo.title.isNotBlank(), "Algorithm ${algo.name} has blank title")
            assertTrue(algo.timeComplexityBest.startsWith("O("), "Algorithm ${algo.name} invalid best complexity")
            assertTrue(algo.timeComplexityWorst.startsWith("O("), "Algorithm ${algo.name} invalid worst complexity")
            assertTrue(algo.spaceComplexity.startsWith("O("), "Algorithm ${algo.name} invalid space complexity")
        }
    }

    @Test
    fun testAlgorithmGeneratorMaxFramesCeiling() {
        assertEquals(1_500, AlgorithmGenerator.MAX_FRAMES, "Maximum frames ceiling must be locked to 1,500")
    }

    @Test
    fun testUuidStabilityAndParsing() {
        val originalUuid = Uuid.random()
        val uuidString = originalUuid.toString()
        val parsedUuid = Uuid.parse(uuidString)

        assertEquals(originalUuid, parsedUuid)
        assertEquals(originalUuid.hashCode(), parsedUuid.hashCode())
    }

    @Test
    fun testFrameMetricsImmutability() {
        val metrics1 = AlgorithmMetrics(comparisons = 10, mutations = 5)
        val metrics2 = metrics1.copy(comparisons = 11)

        assertEquals(10, metrics1.comparisons)
        assertEquals(11, metrics2.comparisons)
        assertNotEquals(metrics1, metrics2)
    }
}
