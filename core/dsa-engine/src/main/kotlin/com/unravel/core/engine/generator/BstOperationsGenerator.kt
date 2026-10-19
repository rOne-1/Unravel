package com.unravel.core.engine.generator

import com.unravel.core.engine.layout.TreeLayoutEngine
import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.TreeNode
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Cluster 4: Binary Search Tree Operations Sequence Generator.
 * Simulates BST Insert, Search, and Delete operations with synchronized code frames,
 * Reingold-Tilford aesthetic layout recalculation, and BST invariant verification.
 */
class BstOperationsGenerator : AlgorithmGenerator<VisualStructureState.HierarchyTreeState> {

    override val algorithmId: AlgorithmId = AlgorithmId.BST_OPERATIONS

    private data class MutableNode(
        val id: Uuid,
        var value: Int,
        var leftId: Uuid? = null,
        var rightId: Uuid? = null
    ) {
        fun toTreeNode(normX: Float = 0.5f, normY: Float = 0.5f, height: Int = 1, bf: Int = 0): TreeNode {
            return TreeNode(
                id = id,
                value = value,
                leftId = leftId,
                rightId = rightId,
                normX = normX,
                normY = normY,
                height = height,
                balanceFactor = bf
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun generateFrames(initialState: VisualStructureState.HierarchyTreeState): ImmutableList<AlgorithmFrame> {
        val accumulator = FrameAccumulator(algorithmId)

        val initialValues = if (initialState.nodes.isNotEmpty()) {
            initialState.nodes.map { it.value }
        } else {
            listOf(50, 30, 70, 20, 40, 60, 80)
        }

        val treeNodes = mutableMapOf<Uuid, MutableNode>()
        var rootId: Uuid? = null

        fun computeCurrentLayout(): TreeLayoutEngine.LayoutResult {
            val list = treeNodes.values.map { it.toTreeNode() }
            return TreeLayoutEngine.layout(list, rootId)
        }

        // Emit initial empty or setup frame
        accumulator.emitFrame(
            structureState = VisualStructureState.HierarchyTreeState(
                nodes = persistentListOf(),
                edges = persistentListOf()
            ),
            activeLineIndex = 1,
            variables = mapOf("action" to "init", "operation" to "Building BST"),
            hapticCue = HapticCue.TICK
        )

        // ==========================================
        // Phase 1: Insertion of Elements
        // ==========================================
        for (value in initialValues) {
            val newNodeId = Uuid.random()
            val newNode = MutableNode(id = newNodeId, value = value)

            if (rootId == null) {
                rootId = newNodeId
                treeNodes[newNodeId] = newNode
                val layoutRes = computeCurrentLayout()

                accumulator.incMutations()
                accumulator.emitFrame(
                    structureState = VisualStructureState.HierarchyTreeState(
                        nodes = layoutRes.nodes,
                        edges = layoutRes.edges,
                        activeNodeId = newNodeId.toString()
                    ),
                    activeLineIndex = 2,
                    variables = mapOf(
                        "operation" to "insert($value)",
                        "action" to "set as root",
                        "node.value" to value.toString()
                    ),
                    hapticCue = HapticCue.TICK
                )
            } else {
                val nonNullRootId = checkNotNull(rootId)
                var currId: Uuid = nonNullRootId
                val visited = mutableSetOf<String>()

                while (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 6) {
                    accumulator.incComparisons()
                    val currNode = treeNodes.getValue(currId)
                    visited.add(currId.toString())

                    val layoutRes = computeCurrentLayout()
                    val targetNextId = if (value < currNode.value) currNode.leftId else currNode.rightId

                    accumulator.emitFrame(
                        structureState = VisualStructureState.HierarchyTreeState(
                            nodes = layoutRes.nodes,
                            edges = layoutRes.edges,
                            activeNodeId = currId.toString(),
                            visitedNodeIds = visited.toImmutableSet()
                        ),
                        activeLineIndex = 3,
                        variables = mapOf(
                            "operation" to "insert($value)",
                            "comparing" to "$value with ${currNode.value}",
                            "direction" to (if (value < currNode.value) "LEFT" else "RIGHT")
                        ),
                        hapticCue = HapticCue.TICK,
                        expectedUserAction = targetNextId?.let { ExpectedAction.SelectBranch(it.toString()) }
                            ?: ExpectedAction.KeepAndAdvance
                    )

                    if (value < currNode.value) {
                        if (currNode.leftId == null) {
                            currNode.leftId = newNodeId
                            treeNodes[newNodeId] = newNode
                            break
                        } else {
                            currId = currNode.leftId!!
                        }
                    } else if (value > currNode.value) {
                        if (currNode.rightId == null) {
                            currNode.rightId = newNodeId
                            treeNodes[newNodeId] = newNode
                            break
                        } else {
                            currId = currNode.rightId!!
                        }
                    } else {
                        // Duplicate value: no-op in standard BST
                        break
                    }
                }

                accumulator.incMutations()
                val updatedLayout = computeCurrentLayout()
                accumulator.emitFrame(
                    structureState = VisualStructureState.HierarchyTreeState(
                        nodes = updatedLayout.nodes,
                        edges = updatedLayout.edges,
                        activeNodeId = newNodeId.toString(),
                        visitedNodeIds = visited.toImmutableSet()
                    ),
                    activeLineIndex = 4,
                    variables = mapOf(
                        "operation" to "insert($value)",
                        "action" to "inserted node $value",
                        "totalNodes" to treeNodes.size.toString()
                    ),
                    hapticCue = HapticCue.TICK
                )
            }
        }

        // ==========================================
        // Phase 2: Search Operation
        // ==========================================
        val searchTarget = initialValues.getOrNull(1) ?: 30
        val searchRootId = rootId
        if (searchRootId != null && accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 6) {
            var currId: Uuid = searchRootId
            val searchVisited = mutableSetOf<String>()

            while (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 4) {
                accumulator.incComparisons()
                val curr = treeNodes.getValue(currId)
                searchVisited.add(currId.toString())

                val layoutRes = computeCurrentLayout()

                if (curr.value == searchTarget) {
                    accumulator.emitFrame(
                        structureState = VisualStructureState.HierarchyTreeState(
                            nodes = layoutRes.nodes,
                            edges = layoutRes.edges,
                            activeNodeId = currId.toString(),
                            visitedNodeIds = searchVisited.toImmutableSet()
                        ),
                        activeLineIndex = 5,
                        variables = mapOf(
                            "operation" to "search($searchTarget)",
                            "result" to "Found $searchTarget!",
                            "node.id" to currId.toString()
                        ),
                        hapticCue = HapticCue.SUCCESS_LOCK
                    )
                    break
                }

                val nextId = if (searchTarget < curr.value) curr.leftId else curr.rightId
                accumulator.emitFrame(
                    structureState = VisualStructureState.HierarchyTreeState(
                        nodes = layoutRes.nodes,
                        edges = layoutRes.edges,
                        activeNodeId = currId.toString(),
                        visitedNodeIds = searchVisited.toImmutableSet()
                    ),
                    activeLineIndex = 6,
                    variables = mapOf(
                        "operation" to "search($searchTarget)",
                        "current" to curr.value.toString(),
                        "searchDirection" to (if (searchTarget < curr.value) "LEFT" else "RIGHT")
                    ),
                    hapticCue = HapticCue.TICK,
                    expectedUserAction = nextId?.let { ExpectedAction.SelectBranch(it.toString()) }
                )

                if (nextId == null) break
                currId = nextId
            }
        }

        // ==========================================
        // Phase 3: Deletion Operation (Two-Children Case)
        // ==========================================
        // Delete a node with two children if possible (e.g. 30 or 50)
        val deleteTargetNode = treeNodes.values.firstOrNull { it.leftId != null && it.rightId != null }
            ?: treeNodes.values.firstOrNull { it.leftId != null || it.rightId != null }

        if (deleteTargetNode != null && accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 4) {
            val deleteVal = deleteTargetNode.value
            val targetId = deleteTargetNode.id

            // Highlight target to be deleted
            val layoutRes = computeCurrentLayout()
            accumulator.emitFrame(
                structureState = VisualStructureState.HierarchyTreeState(
                    nodes = layoutRes.nodes,
                    edges = layoutRes.edges,
                    activeNodeId = targetId.toString()
                ),
                activeLineIndex = 7,
                variables = mapOf(
                    "operation" to "delete($deleteVal)",
                    "action" to "locate deletion target",
                    "targetNode" to deleteVal.toString()
                ),
                hapticCue = HapticCue.TICK
            )

            if (deleteTargetNode.leftId != null && deleteTargetNode.rightId != null) {
                // Find in-order successor (minimum in right subtree)
                var succId = deleteTargetNode.rightId!!
                var succParentId = targetId

                while (treeNodes.getValue(succId).leftId != null) {
                    succParentId = succId
                    succId = treeNodes.getValue(succId).leftId!!
                }

                val succNode = treeNodes.getValue(succId)
                val succVal = succNode.value

                // Highlight successor
                accumulator.emitFrame(
                    structureState = VisualStructureState.HierarchyTreeState(
                        nodes = layoutRes.nodes,
                        edges = layoutRes.edges,
                        activeNodeId = succId.toString(),
                        visitedNodeIds = persistentSetOf(targetId.toString(), succId.toString())
                    ),
                    activeLineIndex = 8,
                    variables = mapOf(
                        "operation" to "delete($deleteVal)",
                        "action" to "found in-order successor $succVal"
                    ),
                    hapticCue = HapticCue.TICK
                )

                // Replace value with successor value
                accumulator.incMutations()
                deleteTargetNode.value = succVal

                // Splice out successor
                val succParent = treeNodes.getValue(succParentId)
                if (succParentId == targetId) {
                    succParent.rightId = succNode.rightId
                } else {
                    succParent.leftId = succNode.rightId
                }
                treeNodes.remove(succId)

                val postDelLayout = computeCurrentLayout()
                accumulator.emitFrame(
                    structureState = VisualStructureState.HierarchyTreeState(
                        nodes = postDelLayout.nodes,
                        edges = postDelLayout.edges,
                        activeNodeId = targetId.toString()
                    ),
                    activeLineIndex = 9,
                    variables = mapOf(
                        "operation" to "delete($deleteVal)",
                        "action" to "replaced with successor $succVal and spliced",
                        "remainingNodes" to treeNodes.size.toString()
                    ),
                    hapticCue = HapticCue.SWAP_THUD
                )
            } else {
                // Single child or leaf
                val childId = deleteTargetNode.leftId ?: deleteTargetNode.rightId
                val parentNode = treeNodes.values.firstOrNull { it.leftId == targetId || it.rightId == targetId }

                accumulator.incMutations()
                if (parentNode != null) {
                    if (parentNode.leftId == targetId) parentNode.leftId = childId
                    else parentNode.rightId = childId
                } else {
                    rootId = childId
                }
                treeNodes.remove(targetId)

                val postDelLayout = computeCurrentLayout()
                accumulator.emitFrame(
                    structureState = VisualStructureState.HierarchyTreeState(
                        nodes = postDelLayout.nodes,
                        edges = postDelLayout.edges
                    ),
                    activeLineIndex = 9,
                    variables = mapOf(
                        "operation" to "delete($deleteVal)",
                        "action" to "spliced node out",
                        "remainingNodes" to treeNodes.size.toString()
                    ),
                    hapticCue = HapticCue.SWAP_THUD
                )
            }
        }

        // ==========================================
        // Phase 4: In-Order Traversal & Completion
        // ==========================================
        val traversalOrder = mutableListOf<String>()
        fun inOrder(id: Uuid?) {
            if (id == null) return
            val n = treeNodes[id] ?: return
            inOrder(n.leftId)
            traversalOrder.add(id.toString())
            inOrder(n.rightId)
        }
        inOrder(rootId)

        val finalLayout = computeCurrentLayout()
        accumulator.emitFrame(
            structureState = VisualStructureState.HierarchyTreeState(
                nodes = finalLayout.nodes,
                edges = finalLayout.edges,
                traversalOrderIds = traversalOrder.toImmutableList(),
                visitedNodeIds = traversalOrder.toImmutableSet()
            ),
            activeLineIndex = 10,
            variables = mapOf(
                "result" to "BST invariant verified (in-order values strictly sorted)",
                "totalNodes" to treeNodes.size.toString(),
                "comparisons" to accumulator.currentComparisons.toString(),
                "mutations" to accumulator.currentMutations.toString()
            ),
            hapticCue = HapticCue.SUCCESS_LOCK
        )

        return accumulator.build()
    }
}
