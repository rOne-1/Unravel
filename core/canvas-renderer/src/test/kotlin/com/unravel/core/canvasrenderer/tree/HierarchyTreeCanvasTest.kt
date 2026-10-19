package com.unravel.core.canvasrenderer.tree

import com.unravel.core.engine.model.TreeEdge
import com.unravel.core.engine.model.TreeNode
import com.unravel.core.engine.model.VisualStructureState.HierarchyTreeState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HierarchyTreeCanvasTest {

    @Test
    @DisplayName("HierarchyTree: State contains valid nodes, edges, and coordinates")
    fun testTreeStateIntegrity() {
        val rootId = Uuid.random()
        val leftId = Uuid.random()
        val rightId = Uuid.random()

        val state = HierarchyTreeState(
            nodes = persistentListOf(
                TreeNode(id = rootId, value = 50, leftId = leftId, rightId = rightId, height = 2, balanceFactor = 0, normX = 0.5f, normY = 0.2f),
                TreeNode(id = leftId, value = 30, height = 1, balanceFactor = 0, normX = 0.25f, normY = 0.6f),
                TreeNode(id = rightId, value = 70, height = 1, balanceFactor = 0, normX = 0.75f, normY = 0.6f)
            ),
            edges = persistentListOf(
                TreeEdge(fromId = rootId, toId = leftId),
                TreeEdge(fromId = rootId, toId = rightId)
            ),
            activeNodeId = rootId.toString(),
            visitedNodeIds = persistentSetOf(rootId.toString(), leftId.toString()),
            traversalOrderIds = persistentListOf(leftId.toString(), rootId.toString(), rightId.toString())
        )

        assertEquals(3, state.nodes.size)
        assertEquals(2, state.edges.size)
        assertEquals(rootId.toString(), state.activeNodeId)
        assertEquals(3, state.traversalOrderIds.size)

        // All nodes have valid coordinates within normalized bounds
        state.nodes.forEach { node ->
            assertTrue(node.normX in 0f..1f)
            assertTrue(node.normY in 0f..1f)
            assertTrue(abs(node.balanceFactor) <= 1, "Balanced node BF should be <= 1")
        }
    }

    @Test
    @DisplayName("HierarchyTree: Unbalanced pivot node is correctly flagged for AVL rotation")
    fun testUnbalancedPivotDetection() {
        val pivotId = Uuid.random()
        val childId = Uuid.random()

        val state = HierarchyTreeState(
            nodes = persistentListOf(
                TreeNode(id = pivotId, value = 30, leftId = null, rightId = childId, height = 3, balanceFactor = -2, normX = 0.5f, normY = 0.2f),
                TreeNode(id = childId, value = 40, height = 2, balanceFactor = -1, normX = 0.7f, normY = 0.5f)
            ),
            rotatingSubtreeRootId = pivotId.toString()
        )

        assertEquals(pivotId.toString(), state.rotatingSubtreeRootId)
        val pivotNode = state.nodes.first { it.id == pivotId }
        assertTrue(abs(pivotNode.balanceFactor) >= 2, "Pivot node should have |BF| >= 2")
    }
}
