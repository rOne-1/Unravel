package com.unravel.core.engine.layout

import com.unravel.core.engine.model.TreeEdge
import com.unravel.core.engine.model.TreeNode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.max
import kotlin.uuid.Uuid

/**
 * Reingold-Tilford aesthetic tree layout engine.
 * Computes non-overlapping normalized coordinates (normX, normY in [0.05, 0.95]) for binary trees.
 *
 * Guarantees:
 * 1. Parent nodes are centered horizontally above their subtrees.
 * 2. Child nodes are positioned vertically below parents.
 * 3. Left and right subtrees do not overlap at any depth level.
 * 4. Height and balance factor (height(left) - height(right)) are accurately computed.
 * 5. Deterministic and pure JVM (no Android dependencies).
 */
object TreeLayoutEngine {

    data class LayoutResult(
        val nodes: ImmutableList<TreeNode>,
        val edges: ImmutableList<TreeEdge>
    )

    private class LayoutNode(
        val node: TreeNode,
        var left: LayoutNode? = null,
        var right: LayoutNode? = null,
        var x: Float = 0f,
        var y: Float = 0f,
        var mod: Float = 0f,
        var depth: Int = 0,
        var height: Int = 1,
        var balanceFactor: Int = 0
    )

    fun layout(
        nodes: List<TreeNode>,
        rootId: Uuid? = null
    ): LayoutResult {
        if (nodes.isEmpty()) {
            return LayoutResult(persistentListOf(), persistentListOf())
        }

        val nodeMap = nodes.associateBy { it.id }

        // Find root: either explicitly provided or the node that is not any other node's child
        val childIds = mutableSetOf<Uuid>()
        for (n in nodes) {
            n.leftId?.let { childIds.add(it) }
            n.rightId?.let { childIds.add(it) }
        }
        val actualRootId = rootId?.takeIf { nodeMap.containsKey(it) }
            ?: nodes.firstOrNull { !childIds.contains(it.id) }?.id
            ?: nodes.first().id

        // Build temporary LayoutNode tree
        fun buildTree(id: Uuid, depth: Int): LayoutNode {
            val original = nodeMap.getValue(id)
            val layoutNode = LayoutNode(node = original, depth = depth)
            original.leftId?.let { if (nodeMap.containsKey(it)) layoutNode.left = buildTree(it, depth + 1) }
            original.rightId?.let { if (nodeMap.containsKey(it)) layoutNode.right = buildTree(it, depth + 1) }

            val leftHeight = layoutNode.left?.height ?: 0
            val rightHeight = layoutNode.right?.height ?: 0
            layoutNode.height = 1 + max(leftHeight, rightHeight)
            layoutNode.balanceFactor = leftHeight - rightHeight

            return layoutNode
        }

        val root = buildTree(actualRootId, depth = 0)

        // Pass 1: Initial relative positioning (bottom-up)
        initializePositions(root)

        // Pass 2: Second pass to resolve conflicts between sibling subtrees
        var maxDepth = 0
        fun findMaxDepth(node: LayoutNode) {
            if (node.depth > maxDepth) maxDepth = node.depth
            node.left?.let { findMaxDepth(it) }
            node.right?.let { findMaxDepth(it) }
        }
        findMaxDepth(root)

        // Pass 3: Accumulate modifiers to get final absolute X positions
        val positionedNodes = mutableListOf<LayoutNode>()
        fun accumulatePositions(node: LayoutNode, accumulatedMod: Float) {
            node.x += accumulatedMod
            node.y = node.depth.toFloat()
            positionedNodes.add(node)
            node.left?.let { accumulatePositions(it, accumulatedMod + node.mod) }
            node.right?.let { accumulatePositions(it, accumulatedMod + node.mod) }
        }
        accumulatePositions(root, 0f)

        // Pass 4: Normalize coordinates to [0.05f, 0.95f]
        val minX = positionedNodes.minOf { it.x }
        val maxX = positionedNodes.maxOf { it.x }
        val xRange = maxX - minX

        val updatedNodes = positionedNodes.map { ln ->
            val normX = if (xRange > 0.0001f) {
                0.08f + 0.84f * ((ln.x - minX) / xRange)
            } else {
                0.5f
            }

            val normY = if (maxDepth > 0) {
                0.12f + 0.76f * (ln.depth.toFloat() / maxDepth.toFloat())
            } else {
                0.3f
            }

            ln.node.copy(
                normX = normX,
                normY = normY,
                height = ln.height,
                balanceFactor = ln.balanceFactor
            )
        }

        // Build edges
        val updatedNodeMap = updatedNodes.associateBy { it.id }
        val edges = mutableListOf<TreeEdge>()
        for (un in updatedNodes) {
            un.leftId?.let { leftChildId ->
                if (updatedNodeMap.containsKey(leftChildId)) {
                    edges.add(TreeEdge(fromId = un.id, toId = leftChildId))
                }
            }
            un.rightId?.let { rightChildId ->
                if (updatedNodeMap.containsKey(rightChildId)) {
                    edges.add(TreeEdge(fromId = un.id, toId = rightChildId))
                }
            }
        }

        return LayoutResult(
            nodes = updatedNodes.toImmutableList(),
            edges = edges.toImmutableList()
        )
    }

    private fun initializePositions(node: LayoutNode) {
        node.left?.let { initializePositions(it) }
        node.right?.let { initializePositions(it) }

        val left = node.left
        val right = node.right

        when {
            left == null && right == null -> {
                node.x = 0f
            }
            left != null && right == null -> {
                node.x = left.x + 1f
            }
            left == null && right != null -> {
                node.x = right.x - 1f
            }
            left != null && right != null -> {
                // Sibling separation
                val separation = separateSubtrees(left, right)
                right.x += separation
                right.mod += separation
                node.x = (left.x + right.x) / 2f
            }
        }
    }

    private fun separateSubtrees(leftRoot: LayoutNode, rightRoot: LayoutNode): Float {
        // Collect contours per depth level
        val leftContour = mutableMapOf<Int, Float>()
        val rightContour = mutableMapOf<Int, Float>()

        fun computeContour(node: LayoutNode, curX: Float, depth: Int, isLeft: Boolean) {
            val contour = if (isLeft) leftContour else rightContour
            val existing = contour[depth]
            if (existing == null) {
                contour[depth] = curX
            } else {
                contour[depth] = if (isLeft) maxOf(existing, curX) else minOf(existing, curX)
            }
            node.left?.let { computeContour(it, curX + it.x + node.mod, depth + 1, isLeft) }
            node.right?.let { computeContour(it, curX + it.x + node.mod, depth + 1, isLeft) }
        }

        computeContour(leftRoot, leftRoot.x, 0, isLeft = true)
        computeContour(rightRoot, rightRoot.x, 0, isLeft = false)

        val minSeparation = 1.8f
        var maxShift = 0f

        val commonDepths = leftContour.keys.intersect(rightContour.keys)
        for (d in commonDepths) {
            val lX = leftContour.getValue(d)
            val rX = rightContour.getValue(d)
            val overlap = (lX + minSeparation) - rX
            if (overlap > maxShift) {
                maxShift = overlap
            }
        }

        return max(0f, maxShift)
    }
}
