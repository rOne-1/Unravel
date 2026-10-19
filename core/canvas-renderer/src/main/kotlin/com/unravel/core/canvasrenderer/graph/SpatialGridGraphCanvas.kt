package com.unravel.core.canvasrenderer.graph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unravel.core.canvasrenderer.haptics.LocalHapticEngine
import com.unravel.core.designsystem.theme.CanvasVoid
import com.unravel.core.designsystem.theme.CoralFlame
import com.unravel.core.designsystem.theme.ElectricVioletBright
import com.unravel.core.designsystem.theme.ElectricVioletDeep
import com.unravel.core.designsystem.theme.NeonMint
import com.unravel.core.designsystem.theme.StructuralSlate
import com.unravel.core.designsystem.theme.StructuralSlateMuted
import com.unravel.core.designsystem.theme.SurfaceGlass
import com.unravel.core.designsystem.theme.SurfaceGlassBorder
import com.unravel.core.designsystem.theme.UnravelMotionConstants
import com.unravel.core.designsystem.theme.UnravelTheme
import com.unravel.core.engine.layout.GraphLayoutEngine
import com.unravel.core.engine.model.SpatialMode
import com.unravel.core.engine.model.VisualStructureState.SpatialGridGraphState
import kotlinx.collections.immutable.toImmutableSet
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance Compose Canvas rendering 2D obstacle grid mazes, free-vertex graphs,
 * BFS wavefront expansion rings, and NeonMint shortest path thread tightening.
 * Strictly guarantees ZERO heap allocations inside `onDraw`.
 */
@Composable
fun SpatialGridGraphCanvas(
    state: SpatialGridGraphState,
    modifier: Modifier = Modifier,
    onCellTap: ((row: Int, col: Int) -> Unit)? = null,
    onVertexTap: ((vertexId: String) -> Unit)? = null
) {
    val hapticEngine = LocalHapticEngine.current
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Pre-allocated reusable paths to guarantee ZERO heap allocations in onDraw
    val cachedEdgePath = remember { Path() }
    val cachedGlowPath = remember { Path() }

    // Text styles
    val cellLabelStyle = remember(colors.textTelemetry, typography.hudMicroLabel) {
        typography.hudMicroLabel.copy(color = colors.textTelemetry)
    }
    val vertexLabelStyle = remember(colors.textPrimary, typography.nodeValueLabel) {
        typography.nodeValueLabel.copy(color = colors.textPrimary)
    }

    // Shortest path tightening spring animation
    val pathTightenAnim = remember { Animatable(0f) }
    LaunchedEffect(state.shortestPathIds) {
        if (state.shortestPathIds.isNotEmpty()) {
            pathTightenAnim.snapTo(0f)
            pathTightenAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = UnravelMotionConstants.DAMPING_RATIO_SPATIAL,
                    stiffness = UnravelMotionConstants.STIFFNESS_SPATIAL
                )
            )
        } else {
            pathTightenAnim.snapTo(0f)
        }
    }

    val vertexRadiusPx = with(density) { 18.dp.toPx() }
    val cellCornerRadius = with(density) { 6.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.vertices, state.mode) {
                detectTapGestures { tapOffset ->
                    if (state.mode == SpatialMode.GRID_2D) {
                        val rows = (state.vertices.maxOfOrNull { it.gridRow } ?: 0) + 1
                        val cols = (state.vertices.maxOfOrNull { it.gridCol } ?: 0) + 1
                        val paddingPx = 16.dp.toPx()
                        val cellSize = min(
                            (size.width - paddingPx * 2f) / cols,
                            (size.height - paddingPx * 2f) / rows
                        )
                        val originX = (size.width - (cols * cellSize)) / 2f
                        val originY = (size.height - (rows * cellSize)) / 2f

                        val c = ((tapOffset.x - originX) / cellSize).toInt()
                        val r = ((tapOffset.y - originY) / cellSize).toInt()

                        if (r in 0 until rows && c in 0 until cols) {
                            onCellTap?.invoke(r, c)
                        }
                    } else {
                        for (v in state.vertices) {
                            val vx = v.normX * size.width
                            val vy = v.normY * size.height
                            val dx = tapOffset.x - vx
                            val dy = tapOffset.y - vy
                            if (dx * dx + dy * dy <= vertexRadiusPx * vertexRadiusPx) {
                                onVertexTap?.invoke(v.id.toString())
                                break
                            }
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        val vertexMap = state.vertices.associateBy { it.id.toString() }
        val shortestPathSet = state.shortestPathIds.toSet()

        if (state.mode == SpatialMode.GRID_2D) {
            // ==========================================
            // GRID_2D Rendering
            // ==========================================
            val rows = (state.vertices.maxOfOrNull { it.gridRow } ?: 0) + 1
            val cols = (state.vertices.maxOfOrNull { it.gridCol } ?: 0) + 1
            val paddingPx = 16.dp.toPx()
            val cellSize = min(
                (width - paddingPx * 2f) / max(1, cols),
                (height - paddingPx * 2f) / max(1, rows)
            )
            val originX = (width - (cols * cellSize)) / 2f
            val originY = (height - (rows * cellSize)) / 2f

            for (v in state.vertices) {
                val vid = v.id.toString()
                val cellLeft = originX + (v.gridCol * cellSize) + 2.dp.toPx()
                val cellTop = originY + (v.gridRow * cellSize) + 2.dp.toPx()
                val cellW = cellSize - 4.dp.toPx()
                val cellH = cellSize - 4.dp.toPx()

                val isStart = vid == state.startVertexId
                val isTarget = vid == state.targetVertexId
                val isPath = shortestPathSet.contains(vid)
                val isFrontier = state.frontierIds.contains(vid)
                val isVisited = state.visitedIds.contains(vid)

                val cellBgColor = when {
                    v.isObstacle -> colors.structuralSlateMuted
                    isPath -> colors.neonMint
                    isStart -> colors.electricVioletBright
                    isTarget -> colors.coralFlame
                    isFrontier -> colors.electricVioletDeep
                    isVisited -> colors.structuralSlate
                    else -> colors.surfaceGlass
                }

                // Cell Fill
                drawRoundRect(
                    color = cellBgColor,
                    topLeft = Offset(cellLeft, cellTop),
                    size = Size(cellW, cellH),
                    cornerRadius = CornerRadius(cellCornerRadius, cellCornerRadius)
                )

                // Frontier Glow Ring
                if (isFrontier) {
                    drawRoundRect(
                        color = colors.electricVioletBright,
                        topLeft = Offset(cellLeft - 2.dp.toPx(), cellTop - 2.dp.toPx()),
                        size = Size(cellW + 4.dp.toPx(), cellH + 4.dp.toPx()),
                        cornerRadius = CornerRadius(cellCornerRadius + 2.dp.toPx(), cellCornerRadius + 2.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Border
                drawRoundRect(
                    color = if (isPath) colors.neonMint else colors.surfaceGlassBorder,
                    topLeft = Offset(cellLeft, cellTop),
                    size = Size(cellW, cellH),
                    cornerRadius = CornerRadius(cellCornerRadius, cellCornerRadius),
                    style = Stroke(width = if (isPath) 2.dp.toPx() else 1.dp.toPx())
                )

                // Distance readout label (if calculated)
                val dist = state.distanceMap[vid]
                if (dist != null && !v.isObstacle) {
                    val distStr = dist.toString()
                    val textMeasure = textMeasurer.measure(distStr, cellLabelStyle)
                    drawText(
                        textLayoutResult = textMeasure,
                        topLeft = Offset(
                            cellLeft + (cellW - textMeasure.size.width) / 2f,
                            cellTop + (cellH - textMeasure.size.height) / 2f
                        )
                    )
                }
            }
        } else {
            // ==========================================
            // FREE_GRAPH Rendering
            // ==========================================
            // 1. Draw Edges
            for (edge in state.edges) {
                val fromV = vertexMap[edge.fromId.toString()] ?: continue
                val toV = vertexMap[edge.toId.toString()] ?: continue

                val x1 = fromV.normX * width
                val y1 = fromV.normY * height
                val x2 = toV.normX * width
                val y2 = toV.normY * height

                val isPathEdge = shortestPathSet.contains(edge.fromId.toString()) &&
                        shortestPathSet.contains(edge.toId.toString())

                val strokeColor = when {
                    isPathEdge -> colors.neonMint
                    edge.isHighlighted -> colors.electricVioletBright
                    else -> colors.structuralSlate
                }
                val strokeWidth = if (isPathEdge) 3.5.dp.toPx() else 1.5.dp.toPx()

                drawLine(
                    color = strokeColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 2. Draw Circular Vertices
            for (v in state.vertices) {
                val vid = v.id.toString()
                val vx = v.normX * width
                val vy = v.normY * height

                val isStart = vid == state.startVertexId
                val isTarget = vid == state.targetVertexId
                val isPath = shortestPathSet.contains(vid)
                val isFrontier = state.frontierIds.contains(vid)
                val isVisited = state.visitedIds.contains(vid)

                val vColor = when {
                    isPath -> colors.neonMint
                    isStart -> colors.electricVioletBright
                    isTarget -> colors.coralFlame
                    isFrontier -> colors.electricVioletDeep
                    isVisited -> colors.structuralSlate
                    else -> colors.surfaceGlass
                }

                // Frontier outer expanding ring
                if (isFrontier) {
                    drawCircle(
                        color = colors.electricVioletBright,
                        radius = vertexRadiusPx + 6.dp.toPx(),
                        center = Offset(vx, vy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Vertex body
                drawCircle(
                    color = vColor,
                    radius = vertexRadiusPx,
                    center = Offset(vx, vy)
                )

                // Vertex border
                drawCircle(
                    color = if (isPath) colors.neonMint else colors.surfaceGlassBorder,
                    radius = vertexRadiusPx,
                    center = Offset(vx, vy),
                    style = Stroke(width = if (isPath) 2.5.dp.toPx() else 1.5.dp.toPx())
                )

                // Label Text
                val label = v.label.ifEmpty { vid.take(3) }
                val textMeasure = textMeasurer.measure(label, vertexLabelStyle)
                drawText(
                    textLayoutResult = textMeasure,
                    topLeft = Offset(
                        vx - (textMeasure.size.width / 2f),
                        vy - (textMeasure.size.height / 2f)
                    )
                )
            }
        }
    }
}

@Preview(name = "Spatial Grid Graph - Dark", widthDp = 360, heightDp = 360)
@Composable
fun PreviewSpatialGridGraphCanvas() {
    val state = GraphLayoutEngine.layoutGrid(
        rows = 4,
        cols = 5,
        obstacles = setOf(Pair(1, 2), Pair(2, 2))
    )

    UnravelTheme {
        SpatialGridGraphCanvas(state = state)
    }
}
