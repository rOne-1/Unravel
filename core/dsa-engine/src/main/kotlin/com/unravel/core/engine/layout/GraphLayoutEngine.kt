package com.unravel.core.engine.layout

import com.unravel.core.engine.model.SpatialEdge
import com.unravel.core.engine.model.SpatialMode
import com.unravel.core.engine.model.SpatialVertex
import com.unravel.core.engine.model.VisualStructureState.SpatialGridGraphState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.cos
import kotlin.math.sin
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Spatial Grid & Graph layout engine.
 * Computes non-overlapping normalized coordinates (normX, normY in [0.05, 0.95]) for:
 * 1. 2D Orthogonal Grids (GRID_2D)
 * 2. Arbitrary Free Graphs (FREE_GRAPH) via circular or distributed layout.
 * Pure Kotlin JVM only.
 */
object GraphLayoutEngine {

    @OptIn(ExperimentalUuidApi::class)
    fun layoutGrid(
        rows: Int,
        cols: Int,
        obstacles: Set<Pair<Int, Int>> = emptySet(),
        startCoord: Pair<Int, Int> = Pair(0, 0),
        targetCoord: Pair<Int, Int> = Pair(rows - 1, cols - 1)
    ): SpatialGridGraphState {
        require(rows > 0 && cols > 0) { "Grid dimensions must be positive, got ${rows}x${cols}" }

        val vertexGrid = Array(rows) { r ->
            Array(cols) { c ->
                val isObstacle = obstacles.contains(Pair(r, c))
                val normX = if (cols > 1) 0.08f + 0.84f * (c.toFloat() / (cols - 1).toFloat()) else 0.5f
                val normY = if (rows > 1) 0.08f + 0.84f * (r.toFloat() / (rows - 1).toFloat()) else 0.5f
                SpatialVertex(
                    id = Uuid.random(),
                    label = "($r,$c)",
                    gridRow = r,
                    gridCol = c,
                    normX = normX,
                    normY = normY,
                    weight = 1,
                    isObstacle = isObstacle
                )
            }
        }

        val vertices = mutableListOf<SpatialVertex>()
        val edges = mutableListOf<SpatialEdge>()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val v = vertexGrid[r][c]
                vertices.add(v)

                // Add edges to right and down neighbors
                if (c + 1 < cols) {
                    val right = vertexGrid[r][c + 1]
                    edges.add(SpatialEdge(fromId = v.id, toId = right.id, weight = 1, isDirected = false))
                }
                if (r + 1 < rows) {
                    val down = vertexGrid[r + 1][c]
                    edges.add(SpatialEdge(fromId = v.id, toId = down.id, weight = 1, isDirected = false))
                }
            }
        }

        val startVertex = vertexGrid[startCoord.first.coerceIn(0, rows - 1)][startCoord.second.coerceIn(0, cols - 1)]
        val targetVertex = vertexGrid[targetCoord.first.coerceIn(0, rows - 1)][targetCoord.second.coerceIn(0, cols - 1)]

        return SpatialGridGraphState(
            mode = SpatialMode.GRID_2D,
            vertices = vertices.toImmutableList(),
            edges = edges.toImmutableList(),
            startVertexId = startVertex.id.toString(),
            targetVertexId = targetVertex.id.toString()
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    fun layoutFreeGraph(
        vertices: List<SpatialVertex>,
        edges: List<SpatialEdge>,
        startId: String? = null,
        targetId: String? = null
    ): SpatialGridGraphState {
        if (vertices.isEmpty()) {
            return SpatialGridGraphState(
                mode = SpatialMode.FREE_GRAPH,
                vertices = persistentListOf(),
                edges = persistentListOf(),
                startVertexId = "",
                targetVertexId = ""
            )
        }

        val n = vertices.size
        val positionedVertices = vertices.mapIndexed { index, vertex ->
            if (vertex.normX > 0.001f || vertex.normY > 0.001f) {
                vertex
            } else {
                // Distribute on circular layout centered at (0.5, 0.5)
                val angle = (2.0 * Math.PI * index / n) - (Math.PI / 2.0)
                val nx = (0.5 + 0.38 * cos(angle)).toFloat()
                val ny = (0.5 + 0.38 * sin(angle)).toFloat()
                vertex.copy(normX = nx, normY = ny)
            }
        }

        val sId = startId ?: positionedVertices.first().id.toString()
        val tId = targetId ?: positionedVertices.last().id.toString()

        return SpatialGridGraphState(
            mode = SpatialMode.FREE_GRAPH,
            vertices = positionedVertices.toImmutableList(),
            edges = edges.toImmutableList(),
            startVertexId = sId,
            targetVertexId = tId
        )
    }
}
