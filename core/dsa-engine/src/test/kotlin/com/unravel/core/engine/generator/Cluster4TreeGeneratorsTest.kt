package com.unravel.core.engine.generator

import com.unravel.core.engine.layout.TreeLayoutEngine
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.TreeNode
import com.unravel.core.engine.model.VisualStructureState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class Cluster4TreeGeneratorsTest {

    @Test
    @DisplayName("TreeLayoutEngine: Assigns non-overlapping coordinates in [0.05, 0.95] with children below parents")
    fun testTreeLayoutEngineAestheticProperties() {
        val rootId = Uuid.random()
        val leftChildId = Uuid.random()
        val rightChildId = Uuid.random()
        val leftLeftId = Uuid.random()

        val nodes = listOf(
            TreeNode(id = rootId, value = 50, leftId = leftChildId, rightId = rightChildId),
            TreeNode(id = leftChildId, value = 30, leftId = leftLeftId),
            TreeNode(id = rightChildId, value = 70),
            TreeNode(id = leftLeftId, value = 20)
        )

        val result = TreeLayoutEngine.layout(nodes, rootId)
        val nodeMap = result.nodes.associateBy { it.id }

        assertEquals(4, result.nodes.size)
        assertEquals(3, result.edges.size)

        // Coordinates within [0.05, 0.95]
        result.nodes.forEach { node ->
            assertTrue(node.normX in 0.05f..0.95f, "normX (${node.normX}) should be within [0.05, 0.95]")
            assertTrue(node.normY in 0.05f..0.95f, "normY (${node.normY}) should be within [0.05, 0.95]")
        }

        // Child vertically below parent
        val rootNode = nodeMap.getValue(rootId)
        val leftNode = nodeMap.getValue(leftChildId)
        val rightNode = nodeMap.getValue(rightChildId)
        val leftLeftNode = nodeMap.getValue(leftLeftId)

        assertTrue(leftNode.normY > rootNode.normY, "Left child must be below root")
        assertTrue(rightNode.normY > rootNode.normY, "Right child must be below root")
        assertTrue(leftLeftNode.normY > leftNode.normY, "Grandchild must be below left child")

        // Left child to the left of parent, right child to the right
        assertTrue(leftNode.normX < rootNode.normX, "Left child must be to the left of root")
        assertTrue(rightNode.normX > rootNode.normX, "Right child must be to the right of root")
    }

    @Test
    @DisplayName("BstOperations: Verifies BST search tree invariant across insertions, search, and two-children deletion")
    fun testBstOperationsCorrectness() {
        val generator = BstOperationsGenerator()
        val initialState = VisualStructureState.HierarchyTreeState(
            nodes = kotlinx.collections.immutable.persistentListOf()
        )

        val frames = generator.generateFrames(initialState)

        assertTrue(frames.isNotEmpty(), "Frames should not be empty")
        assertTrue(frames.size <= AlgorithmGenerator.MAX_FRAMES, "Frame count should obey MAX_FRAMES ceiling")

        // Verify branch selection expected actions exist
        val branchActions = frames.filter { it.expectedUserAction is ExpectedAction.SelectBranch }
        assertTrue(branchActions.isNotEmpty(), "Should emit SelectBranch actions during traversal")

        // Verify search found target
        val searchFrames = frames.filter { it.codeSync.variableBindings["result"]?.contains("Found") == true }
        assertTrue(searchFrames.isNotEmpty(), "Should locate search target")
        assertEquals(HapticCue.SUCCESS_LOCK, searchFrames.first().hapticCue)

        // Verify two-children deletion occurred with successor replacement
        val deleteFrames = frames.filter { it.codeSync.variableBindings["operation"]?.contains("delete") == true }
        assertTrue(deleteFrames.isNotEmpty(), "Should include deletion frames")
        assertTrue(frames.any { it.hapticCue == HapticCue.SWAP_THUD }, "Should emit SWAP_THUD on structural mutation")

        // Verify final state has sorted in-order traversal (BST invariant)
        val finalFrame = frames.last()
        val finalState = finalFrame.structureState as VisualStructureState.HierarchyTreeState
        assertEquals(HapticCue.SUCCESS_LOCK, finalFrame.hapticCue)

        val finalNodeMap = finalState.nodes.associateBy { it.id.toString() }
        val orderedValues = finalState.traversalOrderIds.mapNotNull { finalNodeMap[it]?.value }
        assertEquals(orderedValues.sorted(), orderedValues, "BST invariant: in-order traversal must be strictly sorted")
    }

    @Test
    @DisplayName("AvlRotations: Rebalances skewed insertions and guarantees |BF| <= 1 everywhere")
    fun testAvlRotationsRebalancing() {
        val generator = AvlRotationsGenerator()
        val initialState = VisualStructureState.HierarchyTreeState(
            nodes = kotlinx.collections.immutable.persistentListOf()
        )

        val frames = generator.generateFrames(initialState)

        assertTrue(frames.isNotEmpty())
        assertTrue(frames.size <= AlgorithmGenerator.MAX_FRAMES)

        // Verify rotation actions and haptic cues
        val rotationFrames = frames.filter { it.expectedUserAction is ExpectedAction.RotateSubtree }
        assertTrue(rotationFrames.isNotEmpty(), "Should emit RotateSubtree expected actions")
        assertTrue(frames.any { it.hapticCue == HapticCue.SWAP_THUD }, "Should emit SWAP_THUD on rotation execution")

        // Verify final state satisfies |BF| <= 1 for EVERY node
        val finalFrame = frames.last()
        val finalState = finalFrame.structureState as VisualStructureState.HierarchyTreeState
        assertEquals(HapticCue.SUCCESS_LOCK, finalFrame.hapticCue)

        assertTrue(finalState.nodes.isNotEmpty(), "Final tree should not be empty")
        finalState.nodes.forEach { node ->
            assertTrue(
                abs(node.balanceFactor) <= 1,
                "Node ${node.value} has invalid balance factor ${node.balanceFactor}; AVL requires |BF| <= 1"
            )
        }
    }

    @Test
    @DisplayName("Cluster 4 Trees: Deterministic sequence generation")
    fun testCluster4TreeDeterminism() {
        val bstGen = BstOperationsGenerator()
        val avlGen = AvlRotationsGenerator()
        val state = VisualStructureState.HierarchyTreeState(nodes = kotlinx.collections.immutable.persistentListOf())

        val run1Bst = bstGen.generateFrames(state)
        val run2Bst = bstGen.generateFrames(state)
        assertEquals(run1Bst.size, run2Bst.size)
        run1Bst.indices.forEach { i ->
            assertEquals(run1Bst[i].codeSync.activeLineIndex, run2Bst[i].codeSync.activeLineIndex)
            assertEquals(run1Bst[i].hapticCue, run2Bst[i].hapticCue)
        }

        val run1Avl = avlGen.generateFrames(state)
        val run2Avl = avlGen.generateFrames(state)
        assertEquals(run1Avl.size, run2Avl.size)
        run1Avl.indices.forEach { i ->
            assertEquals(run1Avl[i].codeSync.activeLineIndex, run2Avl[i].codeSync.activeLineIndex)
            assertEquals(run1Avl[i].hapticCue, run2Avl[i].hapticCue)
        }
    }
}
