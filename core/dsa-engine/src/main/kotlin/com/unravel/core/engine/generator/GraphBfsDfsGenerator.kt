package com.unravel.core.engine.generator

import com.unravel.core.engine.layout.GraphLayoutEngine
import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.SpatialVertex
import com.unravel.core.engine.model.VisualStructureState
import com.unravel.core.engine.model.VisualStructureState.SpatialGridGraphState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

/**
 * Cluster 4: Graph BFS & DFS Traversal Sequence Generator.
 * Implements Breadth-First Search (queue wavefront expansion) and Depth-First Search (stack backtracking).
 * Supports 2D grid matrix mazes with obstacles and arbitrary free graphs.
 */
class GraphBfsDfsGenerator(
    val useDfs: Boolean = false
) : AlgorithmGenerator<VisualStructureState.SpatialGridGraphState> {

    override val algorithmId: AlgorithmId = AlgorithmId.GRAPH_BFS_DFS

    override fun generateFrames(initialState: VisualStructureState.SpatialGridGraphState): ImmutableList<AlgorithmFrame> {
        val state = if (initialState.vertices.isNotEmpty()) {
            initialState
        } else {
            // Default 4x5 grid with obstacle wall
            GraphLayoutEngine.layoutGrid(
                rows = 4,
                cols = 5,
                obstacles = setOf(Pair(1, 2), Pair(2, 2), Pair(3, 2))
            )
        }

        val accumulator = FrameAccumulator(algorithmId)
        val vertices = state.vertices
        val vertexMap = vertices.associateBy { it.id.toString() }

        val startId = state.startVertexId.ifEmpty { vertices.first().id.toString() }
        val targetId = state.targetVertexId.ifEmpty { vertices.last().id.toString() }

        // Build adjacency list (ignoring obstacle nodes)
        val adjacency = mutableMapOf<String, MutableList<String>>()
        for (v in vertices) {
            adjacency[v.id.toString()] = mutableListOf()
        }
        for (edge in state.edges) {
            val fromStr = edge.fromId.toString()
            val toStr = edge.toId.toString()
            val fromV = vertexMap[fromStr]
            val toV = vertexMap[toStr]
            if (fromV != null && toV != null && !fromV.isObstacle && !toV.isObstacle) {
                adjacency[fromStr]?.add(toStr)
                if (!edge.isDirected) {
                    adjacency[toStr]?.add(fromStr)
                }
            }
        }

        // Line 1: Setup graph traversal
        accumulator.emitFrame(
            structureState = state.copy(
                frontierIds = persistentSetOf(startId),
                visitedIds = persistentSetOf(),
                shortestPathIds = persistentListOf()
            ),
            activeLineIndex = 1,
            variables = mapOf(
                "algorithm" to (if (useDfs) "Depth-First Search (DFS)" else "Breadth-First Search (BFS)"),
                "start" to (vertexMap[startId]?.label ?: startId),
                "target" to (vertexMap[targetId]?.label ?: targetId),
                "mode" to state.mode.name
            ),
            hapticCue = HapticCue.TICK
        )

        val visited = mutableSetOf<String>()
        val parentMap = mutableMapOf<String, String>()
        var targetFound = false

        if (!useDfs) {
            // ==========================================
            // BFS: Queue-based Wavefront Expansion
            // ==========================================
            val queue = ArrayDeque<String>()
            val frontier = mutableSetOf<String>()

            queue.add(startId)
            frontier.add(startId)

            while (queue.isNotEmpty() && accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 4) {
                accumulator.incComparisons()
                val currId = queue.removeFirst()
                frontier.remove(currId)
                visited.add(currId)

                val currVertex = vertexMap[currId]

                // Line 2: curr = queue.poll()
                accumulator.emitFrame(
                    structureState = state.copy(
                        frontierIds = frontier.toImmutableSet(),
                        visitedIds = visited.toImmutableSet(),
                        shortestPathIds = persistentListOf(currId)
                    ),
                    activeLineIndex = 2,
                    variables = mapOf(
                        "action" to "expandNode",
                        "current" to (currVertex?.label ?: currId),
                        "queue.size" to queue.size.toString(),
                        "visited.size" to visited.size.toString()
                    ),
                    hapticCue = HapticCue.TICK,
                    expectedUserAction = ExpectedAction.ExpandFrontierNode(currId)
                )

                if (currId == targetId) {
                    targetFound = true
                    break
                }

                val neighbors = adjacency[currId] ?: emptyList()
                for (neighborId in neighbors) {
                    val neighborVertex = vertexMap[neighborId] ?: continue
                    if (neighborVertex.isObstacle) continue // Strictly skip obstacles

                    if (!visited.contains(neighborId) && !frontier.contains(neighborId)) {
                        accumulator.incMutations()
                        parentMap[neighborId] = currId
                        frontier.add(neighborId)
                        queue.add(neighborId)

                        // Line 3: queue.add(neighbor)
                        if (accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 6) {
                            accumulator.emitFrame(
                                structureState = state.copy(
                                    frontierIds = frontier.toImmutableSet(),
                                    visitedIds = visited.toImmutableSet()
                                ),
                                activeLineIndex = 3,
                                variables = mapOf(
                                    "action" to "enqueueNeighbor",
                                    "neighbor" to neighborVertex.label,
                                    "from" to (currVertex?.label ?: currId)
                                ),
                                hapticCue = HapticCue.TICK
                            )
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // DFS: Stack-based Exploration
            // ==========================================
            val stack = ArrayDeque<String>()
            val frontier = mutableSetOf<String>()

            stack.add(startId)
            frontier.add(startId)

            while (stack.isNotEmpty() && accumulator.frameCount < AlgorithmGenerator.MAX_FRAMES - 4) {
                accumulator.incComparisons()
                val currId = stack.removeLast()
                frontier.remove(currId)

                if (visited.contains(currId)) continue
                visited.add(currId)
                val currVertex = vertexMap[currId]

                accumulator.updateMaxDepth(stack.size)

                // Line 2: curr = stack.pop()
                accumulator.emitFrame(
                    structureState = state.copy(
                        frontierIds = frontier.toImmutableSet(),
                        visitedIds = visited.toImmutableSet(),
                        shortestPathIds = persistentListOf(currId)
                    ),
                    activeLineIndex = 2,
                    variables = mapOf(
                        "action" to "exploreNode",
                        "current" to (currVertex?.label ?: currId),
                        "stack.depth" to stack.size.toString(),
                        "visited.size" to visited.size.toString()
                    ),
                    hapticCue = HapticCue.TICK,
                    expectedUserAction = ExpectedAction.ExpandFrontierNode(currId)
                )

                if (currId == targetId) {
                    targetFound = true
                    break
                }

                val neighbors = adjacency[currId] ?: emptyList()
                for (neighborId in neighbors) {
                    val neighborVertex = vertexMap[neighborId] ?: continue
                    if (neighborVertex.isObstacle) continue

                    if (!visited.contains(neighborId)) {
                        accumulator.incMutations()
                        parentMap[neighborId] = currId
                        frontier.add(neighborId)
                        stack.add(neighborId)
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

        // Final completion frame
        accumulator.emitFrame(
            structureState = state.copy(
                frontierIds = persistentSetOf(),
                visitedIds = visited.toImmutableSet(),
                shortestPathIds = path.toImmutableList()
            ),
            activeLineIndex = 4,
            variables = mapOf(
                "result" to (if (targetFound) "Path discovered (${path.size} nodes)" else "No path exists (blocked)"),
                "totalVisited" to visited.size.toString(),
                "pathLength" to path.size.toString()
            ),
            hapticCue = if (targetFound) HapticCue.SUCCESS_LOCK else HapticCue.ERROR_BUZZ
        )

        return accumulator.build()
    }
}
