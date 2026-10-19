package com.unravel.core.engine.generator

import com.unravel.core.engine.layout.GraphLayoutEngine
import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.SpatialMode
import com.unravel.core.engine.model.SpatialVertex
import com.unravel.core.engine.model.VisualStructureState
import com.unravel.core.engine.model.VisualStructureState.SpatialGridGraphState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Cluster 4: Dijkstra & A* Shortest Path Sequence Generator.
 * Implements weighted shortest pathfinding on 2D grids and free graphs with obstacle avoidance.
 * Supports Dijkstra (uniform cost search) and A* (Manhattan / Euclidean heuristic).
 */
class DijkstraAStarGenerator(
    val useAStar: Boolean = false
) : AlgorithmGenerator<VisualStructureState.SpatialGridGraphState> {

    override val algorithmId: AlgorithmId = AlgorithmId.DIJKSTRA_ASTAR

    private data class PQNode(
        val vertexId: String,
        val fScore: Int,
        val gScore: Int
    )

    override fun generateFrames(initialState: VisualStructureState.SpatialGridGraphState): ImmutableList<AlgorithmFrame> {
        val state = if (initialState.vertices.isNotEmpty()) {
            initialState
        } else {
            // Default 5x5 grid with obstacle bar
            GraphLayoutEngine.layoutGrid(
                rows = 5,
                cols = 5,
                obstacles = setOf(Pair(2, 1), Pair(2, 2), Pair(2, 3))
            )
        }

        val accumulator = FrameAccumulator(algorithmId)
        val vertices = state.vertices
        val vertexMap = vertices.associateBy { it.id.toString() }

        val startId = state.startVertexId.ifEmpty { vertices.first().id.toString() }
        val targetId = state.targetVertexId.ifEmpty { vertices.last().id.toString() }
        val targetVertex = vertexMap[targetId]

        fun heuristic(v: SpatialVertex): Int {
            if (!useAStar || targetVertex == null) return 0
            return when (state.mode) {
                SpatialMode.GRID_2D -> {
                    abs(v.gridRow - targetVertex.gridRow) + abs(v.gridCol - targetVertex.gridCol)
                }
                SpatialMode.FREE_GRAPH -> {
                    val dist = hypot(v.normX - targetVertex.normX, v.normY - targetVertex.normY)
                    (dist * 10).toInt()
                }
            }
        }

        // Build adjacency graph
        val adjacency = mutableMapOf<String, MutableList<Pair<String, Int>>>()
        for (v in vertices) {
            adjacency[v.id.toString()] = mutableListOf()
        }
        for (edge in state.edges) {
            val fromStr = edge.fromId.toString()
            val toStr = edge.toId.toString()
            val fromV = vertexMap[fromStr]
            val toV = vertexMap[toStr]
            if (fromV != null && toV != null && !fromV.isObstacle && !toV.isObstacle) {
                adjacency[fromStr]?.add(Pair(toStr, edge.weight))
                if (!edge.isDirected) {
                    adjacency[toStr]?.add(Pair(fromStr, edge.weight))
                }
            }
        }

        val gScoreMap = mutableMapOf<String, Int>()
        val parentMap = mutableMapOf<String, String>()
        val visited = mutableSetOf<String>()
        val frontier = mutableSetOf<String>()

        val pq = PriorityQueue<PQNode>(compareBy { it.fScore })

        val startVertex = vertexMap[startId]
        val startH = if (startVertex != null) heuristic(startVertex) else 0
        gScoreMap[startId] = 0
        pq.add(PQNode(startId, fScore = startH, gScore = 0))
        frontier.add(startId)

        // Line 1: Setup pathfinding
        accumulator.emitFrame(
            structureState = state.copy(
                frontierIds = persistentSetOf(startId),
                visitedIds = persistentSetOf(),
                distanceMap = persistentMapOf(startId to 0),
                shortestPathIds = persistentListOf()
            ),
            activeLineIndex = 1,
            variables = mapOf(
                "algorithm" to (if (useAStar) "A* Pathfinding" else "Dijkstra's Algorithm"),
                "start" to (startVertex?.label ?: startId),
                "target" to (targetVertex?.label ?: targetId),
                "heuristic" to (if (useAStar) "Admissible heuristic active" else "Uniform cost (h=0)")
            ),
            hapticCue = HapticCue.TICK
        )

        var targetFound = false

        while (pq.isNotEmpty() && accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 4) {
            accumulator.incComparisons()
            val current = pq.poll()
            val currId = current.vertexId

            if (visited.contains(currId)) continue

            frontier.remove(currId)
            visited.add(currId)

            val currVertex = vertexMap[currId]

            // Line 2: Expand lowest fScore node from priority queue
            accumulator.emitFrame(
                structureState = state.copy(
                    frontierIds = frontier.toImmutableSet(),
                    visitedIds = visited.toImmutableSet(),
                    distanceMap = gScoreMap.toImmutableMap(),
                    shortestPathIds = persistentListOf(currId)
                ),
                activeLineIndex = 2,
                variables = mapOf(
                    "action" to "expandMinNode",
                    "current" to (currVertex?.label ?: currId),
                    "gScore" to current.gScore.toString(),
                    "fScore" to current.fScore.toString(),
                    "frontier.size" to frontier.size.toString()
                ),
                hapticCue = HapticCue.TICK,
                expectedUserAction = ExpectedAction.ExpandFrontierNode(currId)
            )

            if (currId == targetId) {
                targetFound = true
                break
            }

            val neighbors = adjacency[currId] ?: emptyList()
            for ((neighborId, weight) in neighbors) {
                val neighborVertex = vertexMap[neighborId] ?: continue
                if (neighborVertex.isObstacle) continue // Strictly skip obstacles
                if (visited.contains(neighborId)) continue

                val tentativeG = current.gScore + weight
                val currentG = gScoreMap.getOrDefault(neighborId, Int.MAX_VALUE)

                if (tentativeG < currentG) {
                    accumulator.incMutations()
                    gScoreMap[neighborId] = tentativeG
                    parentMap[neighborId] = currId

                    val fScore = tentativeG + heuristic(neighborVertex)
                    pq.add(PQNode(neighborId, fScore = fScore, gScore = tentativeG))
                    frontier.add(neighborId)

                    // Line 3: Relax edge (u, v) and update distance
                    if (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 6) {
                        accumulator.emitFrame(
                            structureState = state.copy(
                                frontierIds = frontier.toImmutableSet(),
                                visitedIds = visited.toImmutableSet(),
                                distanceMap = gScoreMap.toImmutableMap()
                            ),
                            activeLineIndex = 3,
                            variables = mapOf(
                                "action" to "relaxEdge",
                                "relaxedNode" to neighborVertex.label,
                                "newDistance" to tentativeG.toString(),
                                "fScore" to fScore.toString()
                            ),
                            hapticCue = HapticCue.TICK
                        )
                    }
                }
            }
        }

        // ==========================================
        // Path Reconstruction
        // ==========================================
        val path = mutableListOf<String>()
        if (targetFound) {
            var curr: String? = targetId
            while (curr != null) {
                path.add(0, curr)
                curr = if (curr == startId) null else parentMap[curr]
            }
        }

        val totalCost = if (targetFound) gScoreMap[targetId] ?: 0 else -1

        // Final completion frame
        accumulator.emitFrame(
            structureState = state.copy(
                frontierIds = persistentSetOf(),
                visitedIds = visited.toImmutableSet(),
                distanceMap = gScoreMap.toImmutableMap(),
                shortestPathIds = path.toImmutableList()
            ),
            activeLineIndex = 4,
            variables = mapOf(
                "result" to (if (targetFound) "Optimal shortest path discovered!" else "Path blocked by obstacles"),
                "pathLength" to path.size.toString(),
                "totalCost" to totalCost.toString(),
                "nodesVisited" to visited.size.toString(),
                "totalMutations" to accumulator.currentMutations.toString()
            ),
            hapticCue = if (targetFound) HapticCue.SUCCESS_LOCK else HapticCue.ERROR_BUZZ
        )

        return accumulator.build()
    }
}
