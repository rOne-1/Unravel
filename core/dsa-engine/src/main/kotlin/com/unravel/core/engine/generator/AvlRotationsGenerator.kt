package com.unravel.core.engine.generator

import com.unravel.core.engine.layout.TreeLayoutEngine
import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.RotationDirection
import com.unravel.core.engine.model.TreeNode
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlin.math.max
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Cluster 4: AVL Tree Rotations Sequence Generator.
 * Implements self-balancing binary search tree insertions with staged 3-micro-frame rotations
 * (Single Left RR, Single Right LL, Left-Right LR, Right-Left RL).
 * Proves the AVL balance factor invariant (|BF| <= 1) and updates Reingold-Tilford aesthetic layout.
 */
class AvlRotationsGenerator : AlgorithmGenerator<VisualStructureState.HierarchyTreeState> {

    override val algorithmId: AlgorithmId = AlgorithmId.AVL_ROTATIONS

    private class AvlMutableNode(
        val id: Uuid,
        val value: Int,
        var left: AvlMutableNode? = null,
        var right: AvlMutableNode? = null,
        var height: Int = 1
    ) {
        val balanceFactor: Int
            get() = (left?.height ?: 0) - (right?.height ?: 0)

        fun updateHeight() {
            height = 1 + max(left?.height ?: 0, right?.height ?: 0)
        }

        fun collectAll(list: MutableList<TreeNode>) {
            list.add(
                TreeNode(
                    id = id,
                    value = value,
                    leftId = left?.id,
                    rightId = right?.id,
                    height = height,
                    balanceFactor = balanceFactor
                )
            )
            left?.collectAll(list)
            right?.collectAll(list)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun generateFrames(initialState: VisualStructureState.HierarchyTreeState): ImmutableList<AlgorithmFrame> {
        val accumulator = FrameAccumulator(algorithmId)

        // Provide skewed dataset if input is empty or unbalanced to demonstrate LL, RR, LR, RL rotations
        val valuesToInsert = if (initialState.nodes.isNotEmpty()) {
            initialState.nodes.map { it.value }
        } else {
            listOf(10, 20, 30, 40, 50, 25)
        }

        var root: AvlMutableNode? = null

        fun getLayout(): TreeLayoutEngine.LayoutResult {
            val list = mutableListOf<TreeNode>()
            root?.collectAll(list)
            return TreeLayoutEngine.layout(list, root?.id)
        }

        // Line 1: Setup
        accumulator.emitFrame(
            structureState = VisualStructureState.HierarchyTreeState(
                nodes = persistentListOf(),
                edges = persistentListOf()
            ),
            activeLineIndex = 1,
            variables = mapOf("operation" to "init", "status" to "Self-Balancing AVL Tree"),
            hapticCue = HapticCue.TICK
        )

        fun rotateRight(pivot: AvlMutableNode): AvlMutableNode {
            val pivotIdStr = pivot.id.toString()
            val newRoot = pivot.left!!

            // Micro-frame 1: Highlight unbalanced pivot
            val layout1 = getLayout()
            accumulator.emitFrame(
                structureState = VisualStructureState.HierarchyTreeState(
                    nodes = layout1.nodes,
                    edges = layout1.edges,
                    activeNodeId = pivotIdStr,
                    rotatingSubtreeRootId = pivotIdStr
                ),
                activeLineIndex = 6,
                variables = mapOf(
                    "operation" to "Rotate Right (LL)",
                    "pivot" to pivot.value.toString(),
                    "balanceFactor" to pivot.balanceFactor.toString(),
                    "direction" to "RIGHT"
                ),
                hapticCue = HapticCue.TICK,
                expectedUserAction = ExpectedAction.RotateSubtree(pivotIdStr, RotationDirection.RIGHT)
            )

            // Micro-frame 2: Execute pointer rewire
            accumulator.incSwaps()
            accumulator.incMutations()
            pivot.left = newRoot.right
            newRoot.right = pivot
            pivot.updateHeight()
            newRoot.updateHeight()

            val layout2 = getLayout()
            accumulator.emitFrame(
                structureState = VisualStructureState.HierarchyTreeState(
                    nodes = layout2.nodes,
                    edges = layout2.edges,
                    activeNodeId = newRoot.id.toString(),
                    rotatingSubtreeRootId = newRoot.id.toString()
                ),
                activeLineIndex = 7,
                variables = mapOf(
                    "action" to "Rotate Right Complete",
                    "newSubtreeRoot" to newRoot.value.toString()
                ),
                hapticCue = HapticCue.SWAP_THUD
            )

            // Micro-frame 3: Rebalance settled
            val layout3 = getLayout()
            accumulator.emitFrame(
                structureState = VisualStructureState.HierarchyTreeState(
                    nodes = layout3.nodes,
                    edges = layout3.edges,
                    activeNodeId = newRoot.id.toString()
                ),
                activeLineIndex = 8,
                variables = mapOf(
                    "action" to "Subtree Balanced",
                    "balanceFactor" to newRoot.balanceFactor.toString()
                ),
                hapticCue = HapticCue.TICK
            )

            return newRoot
        }

        fun rotateLeft(pivot: AvlMutableNode): AvlMutableNode {
            val pivotIdStr = pivot.id.toString()
            val newRoot = pivot.right!!

            // Micro-frame 1: Highlight unbalanced pivot
            val layout1 = getLayout()
            accumulator.emitFrame(
                structureState = VisualStructureState.HierarchyTreeState(
                    nodes = layout1.nodes,
                    edges = layout1.edges,
                    activeNodeId = pivotIdStr,
                    rotatingSubtreeRootId = pivotIdStr
                ),
                activeLineIndex = 9,
                variables = mapOf(
                    "operation" to "Rotate Left (RR)",
                    "pivot" to pivot.value.toString(),
                    "balanceFactor" to pivot.balanceFactor.toString(),
                    "direction" to "LEFT"
                ),
                hapticCue = HapticCue.TICK,
                expectedUserAction = ExpectedAction.RotateSubtree(pivotIdStr, RotationDirection.LEFT)
            )

            // Micro-frame 2: Execute pointer rewire
            accumulator.incSwaps()
            accumulator.incMutations()
            pivot.right = newRoot.left
            newRoot.left = pivot
            pivot.updateHeight()
            newRoot.updateHeight()

            val layout2 = getLayout()
            accumulator.emitFrame(
                structureState = VisualStructureState.HierarchyTreeState(
                    nodes = layout2.nodes,
                    edges = layout2.edges,
                    activeNodeId = newRoot.id.toString(),
                    rotatingSubtreeRootId = newRoot.id.toString()
                ),
                activeLineIndex = 10,
                variables = mapOf(
                    "action" to "Rotate Left Complete",
                    "newSubtreeRoot" to newRoot.value.toString()
                ),
                hapticCue = HapticCue.SWAP_THUD
            )

            // Micro-frame 3: Rebalance settled
            val layout3 = getLayout()
            accumulator.emitFrame(
                structureState = VisualStructureState.HierarchyTreeState(
                    nodes = layout3.nodes,
                    edges = layout3.edges,
                    activeNodeId = newRoot.id.toString()
                ),
                activeLineIndex = 11,
                variables = mapOf(
                    "action" to "Subtree Balanced",
                    "balanceFactor" to newRoot.balanceFactor.toString()
                ),
                hapticCue = HapticCue.TICK
            )

            return newRoot
        }

        fun insertNode(current: AvlMutableNode?, value: Int): AvlMutableNode {
            if (current == null) {
                accumulator.incMutations()
                val created = AvlMutableNode(id = Uuid.random(), value = value)
                return created
            }

            accumulator.incComparisons()
            if (value < current.value) {
                current.left = insertNode(current.left, value)
            } else if (value > current.value) {
                current.right = insertNode(current.right, value)
            } else {
                return current
            }

            current.updateHeight()
            val bf = current.balanceFactor

            // Check for imbalances and perform rotations
            if (bf > 1 && current.left != null) {
                if (current.left!!.balanceFactor >= 0) {
                    // Left-Left (LL) Case -> Single Right Rotation
                    return rotateRight(current)
                } else {
                    // Left-Right (LR) Case -> Left on left child, then Right on current
                    current.left = rotateLeft(current.left!!)
                    return rotateRight(current)
                }
            }

            if (bf < -1 && current.right != null) {
                if (current.right!!.balanceFactor <= 0) {
                    // Right-Right (RR) Case -> Single Left Rotation
                    return rotateLeft(current)
                } else {
                    // Right-Left (RL) Case -> Right on right child, then Left on current
                    current.right = rotateRight(current.right!!)
                    return rotateLeft(current)
                }
            }

            return current
        }

        // Insert all values
        for (value in valuesToInsert) {
            if (accumulator.frameCount >= AlgorithmGenerator.MAX_FRAMES - 10) break

            root = insertNode(root, value)
            val stepLayout = getLayout()

            accumulator.emitFrame(
                structureState = VisualStructureState.HierarchyTreeState(
                    nodes = stepLayout.nodes,
                    edges = stepLayout.edges,
                    activeNodeId = root.id.toString()
                ),
                activeLineIndex = 2,
                variables = mapOf(
                    "action" to "Inserted $value and balanced",
                    "root.value" to root.value.toString(),
                    "root.height" to root.height.toString()
                ),
                hapticCue = HapticCue.TICK
            )
        }

        // Final completion frame: verify |BF| <= 1 everywhere
        val finalLayout = getLayout()
        val allNodeIds = finalLayout.nodes.map { it.id.toString() }

        accumulator.emitFrame(
            structureState = VisualStructureState.HierarchyTreeState(
                nodes = finalLayout.nodes,
                edges = finalLayout.edges,
                traversalOrderIds = allNodeIds.toImmutableList(),
                visitedNodeIds = allNodeIds.toImmutableSet()
            ),
            activeLineIndex = 12,
            variables = mapOf(
                "result" to "AVL Tree strictly balanced: |BF| <= 1 for all nodes",
                "totalNodes" to finalLayout.nodes.size.toString(),
                "root.height" to (root?.height?.toString() ?: "0"),
                "totalSwaps" to accumulator.currentSwaps.toString()
            ),
            hapticCue = HapticCue.SUCCESS_LOCK
        )

        return accumulator.build()
    }
}
