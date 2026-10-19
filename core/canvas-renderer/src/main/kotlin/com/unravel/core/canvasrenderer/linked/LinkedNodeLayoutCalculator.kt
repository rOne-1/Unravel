package com.unravel.core.canvasrenderer.linked

import androidx.compose.ui.geometry.Offset
import com.unravel.core.engine.model.LinkedNode
import com.unravel.core.engine.model.VisualStructureState.LinkedNodeState
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Geometric layout calculator for linked lists, Floyd orbital cycle geometries,
 * and vertical/horizontal stack and queue containers.
 *
 * Fully decoupled from drawing passes for clean unit testability and zero-allocation rendering.
 */
object LinkedNodeLayoutCalculator {

    enum class ContainerMode {
        LINEAR_LIST,
        FLOYD_ORBITAL,
        VERTICAL_STACK,
        HORIZONTAL_QUEUE
    }

    data class LinkGeometry(
        val fromId: String,
        val toId: String,
        val start: Offset,
        val control: Offset,
        val end: Offset,
        val isCycleLoop: Boolean = false,
        val isActive: Boolean = false,
        val isMutating: Boolean = false
    )

    data class LayoutResult(
        val mode: ContainerMode,
        val nodePositions: Map<String, Offset>,
        val nodeRadius: Float,
        val links: List<LinkGeometry>,
        val orbitCenter: Offset? = null,
        val orbitRadius: Float? = null
    )

    fun calculateLayout(
        state: LinkedNodeState,
        width: Float,
        height: Float
    ): LayoutResult {
        val nodes = state.nodes
        if (nodes.isEmpty() || width <= 0f || height <= 0f) {
            return LayoutResult(
                mode = ContainerMode.LINEAR_LIST,
                nodePositions = emptyMap(),
                nodeRadius = 24f,
                links = emptyList()
            )
        }

        // Detect container mode
        val hasCycle = state.isCycleDetected || detectCycleInNodes(nodes) != null
        val mode = when {
            hasCycle -> ContainerMode.FLOYD_ORBITAL
            state.pointers.containsKey("top") -> ContainerMode.VERTICAL_STACK
            state.pointers.containsKey("tail") && !state.pointers.containsKey("curr") -> ContainerMode.HORIZONTAL_QUEUE
            else -> ContainerMode.LINEAR_LIST
        }

        val nodeRadius = calculateNodeRadius(nodes.size, width, height, mode)
        val positions = mutableMapOf<String, Offset>()
        var orbitCenter: Offset? = null
        var orbitRadius: Float? = null

        when (mode) {
            ContainerMode.FLOYD_ORBITAL -> {
                val cycleEntryIdx = detectCycleInNodes(nodes) ?: max(1, nodes.size / 2)
                val prefixCount = cycleEntryIdx
                val cycleCount = nodes.size - prefixCount

                val prefixWidth = width * 0.45f
                val orbitAreaWidth = width * 0.55f
                val cy = height * 0.5f

                // Prefix spine nodes
                val prefixStep = if (prefixCount > 0) prefixWidth / (prefixCount + 1) else prefixWidth
                for (i in 0 until prefixCount) {
                    val nx = 32f + (i + 1) * prefixStep
                    positions[nodes[i].id.toString()] = Offset(nx, cy)
                }

                // Orbital ring nodes
                val cx = prefixWidth + (orbitAreaWidth * 0.5f)
                val rad = minOf(orbitAreaWidth * 0.35f, height * 0.35f)
                orbitCenter = Offset(cx, cy)
                orbitRadius = rad

                for (j in 0 until cycleCount) {
                    val nodeIdx = prefixCount + j
                    val angle = (2.0 * Math.PI * j / cycleCount) - (Math.PI / 2.0)
                    val nx = (cx + rad * cos(angle)).toFloat()
                    val ny = (cy + rad * sin(angle)).toFloat()
                    positions[nodes[nodeIdx].id.toString()] = Offset(nx, ny)
                }
            }

            ContainerMode.VERTICAL_STACK -> {
                val cx = width * 0.5f
                val baseY = height - 48f
                val vStep = (nodeRadius * 2f) + 16f
                for (i in nodes.indices) {
                    val ny = baseY - (i * vStep)
                    positions[nodes[i].id.toString()] = Offset(cx, ny)
                }
            }

            ContainerMode.HORIZONTAL_QUEUE, ContainerMode.LINEAR_LIST -> {
                val cy = height * 0.5f
                val totalNodes = nodes.size
                val hStep = (width - 64f) / max(1, totalNodes + 1)
                for (i in nodes.indices) {
                    val nx = 32f + (i + 1) * hStep
                    positions[nodes[i].id.toString()] = Offset(nx, cy)
                }
            }
        }

        // Calculate Bézier spline links
        val links = mutableListOf<LinkGeometry>()
        val nodeMap = nodes.associateBy { it.id.toString() }

        for (node in nodes) {
            val fromId = node.id.toString()
            val toUuid = node.nextId ?: continue
            val toId = toUuid.toString()

            val pStart = positions[fromId] ?: continue
            val pEnd = positions[toId] ?: continue

            val linkKey = "$fromId->$toId"
            val isActive = state.activeLinkIds.contains(linkKey)
            val isMutating = state.mutatingLinkIds.contains(linkKey)

            val fromIndex = nodes.indexOfFirst { it.id.toString() == fromId }
            val toIndex = nodes.indexOfFirst { it.id.toString() == toId }
            val isCycleClosure = mode == ContainerMode.FLOYD_ORBITAL && (node == nodes.last() || (fromIndex >= 0 && toIndex >= 0 && toIndex <= fromIndex))

            // Calculate directional offset on circle boundary
            val dx = pEnd.x - pStart.x
            val dy = pEnd.y - pStart.y
            val dist = kotlin.math.hypot(dx, dy)
            if (dist < 0.001f) continue

            val startOffset = Offset(
                pStart.x + (dx / dist) * nodeRadius,
                pStart.y + (dy / dist) * nodeRadius
            )
            val endOffset = Offset(
                pEnd.x - (dx / dist) * nodeRadius,
                pEnd.y - (dy / dist) * nodeRadius
            )

            // Tensioned control point: unvisited has slack (curved arc), active/mutating pulls taut
            val midX = (startOffset.x + endOffset.x) * 0.5f
            val midY = (startOffset.y + endOffset.y) * 0.5f
            val slackOffset = if (isActive || isMutating) 4f else 22f
            val control = if (isCycleClosure) {
                // Curved loop arc for cycle closure
                Offset(midX, midY + (orbitRadius ?: 60f) * 0.8f)
            } else {
                Offset(midX, midY - slackOffset)
            }

            links.add(
                LinkGeometry(
                    fromId = fromId,
                    toId = toId,
                    start = startOffset,
                    control = control,
                    end = endOffset,
                    isCycleLoop = isCycleClosure,
                    isActive = isActive,
                    isMutating = isMutating
                )
            )
        }

        return LayoutResult(
            mode = mode,
            nodePositions = positions,
            nodeRadius = nodeRadius,
            links = links,
            orbitCenter = orbitCenter,
            orbitRadius = orbitRadius
        )
    }

    private fun detectCycleInNodes(nodes: List<LinkedNode>): Int? {
        val seen = mutableMapOf<String, Int>()
        for (i in nodes.indices) {
            val id = nodes[i].id.toString()
            seen[id] = i
            val next = nodes[i].nextId?.toString()
            if (next != null && seen.containsKey(next)) {
                return seen[next]
            }
        }
        return null
    }

    private fun calculateNodeRadius(count: Int, width: Float, height: Float, mode: ContainerMode): Float {
        return when (mode) {
            ContainerMode.VERTICAL_STACK -> minOf(28f, (height * 0.8f) / (count * 2.5f + 1f))
            ContainerMode.FLOYD_ORBITAL -> 24f
            else -> minOf(26f, (width * 0.8f) / (count * 2.8f + 1f))
        }.coerceIn(16f, 32f)
    }
}
