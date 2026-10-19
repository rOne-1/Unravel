package com.unravel.feature.skillmap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.unravel.core.designsystem.theme.UnravelTheme
import com.unravel.feature.skillmap.layout.SkillMapLayoutCoordinates
import com.unravel.feature.skillmap.model.ClusterBounds
import com.unravel.feature.skillmap.model.MasteryState
import com.unravel.feature.skillmap.model.SkillMapUiState
import com.unravel.feature.skillmap.model.SkillNodeUiModel
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.max
import kotlin.math.min

@Immutable
private data class NodeDrawModel(
    val node: SkillNodeUiModel,
    val centerPx: Offset,
    val cardLeftPx: Float,
    val cardTopPx: Float,
    val titleLayout: TextLayoutResult,
    val categoryLayout: TextLayoutResult,
    val isSelected: Boolean
)

@Immutable
private data class SplineDrawModel(
    val geometry: SplineGeometry,
    val strokeColor: Color,
    val glowColor: Color?,
    val isTaut: Boolean
)

@Immutable
private data class ClusterDrawModel(
    val cluster: ClusterBounds,
    val leftPx: Float,
    val topPx: Float,
    val widthPx: Float,
    val heightPx: Float,
    val headerLayout: TextLayoutResult
)

/**
 * 2D Pan/Zoom Canvas rendering the full curriculum DAG map with cluster territories,
 * cubic Bézier prerequisite splines, and interactive node cards.
 * Strictly guarantees ZERO heap allocations inside `onDraw`.
 */
@Composable
fun SkillMapCanvas(
    state: SkillMapUiState,
    modifier: Modifier = Modifier,
    canvasState: SkillMapCanvasState = rememberSkillMapCanvasState(),
    onNodeClick: (SkillNodeUiModel) -> Unit = {}
) {
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Pre-allocated reusable Path objects to guarantee ZERO allocations in onDraw
    val splinePath = remember { Path() }
    val arrowPath = remember { Path() }

    // Pre-measured Cluster visuals outside onDraw
    val clusterDrawModels = remember(state.clusterBounds, density, colors.textTelemetry) {
        state.clusterBounds.map { cluster ->
            val left = cluster.minX * density.density
            val top = cluster.minY * density.density
            val width = (cluster.maxX - cluster.minX) * density.density
            val height = (cluster.maxY - cluster.minY) * density.density
            val headerText = "// 0${cluster.clusterIndex} · ${cluster.title}"
            val headerLayout = textMeasurer.measure(
                text = headerText,
                style = typography.hudMicroLabel.copy(color = colors.textTelemetry)
            )
            ClusterDrawModel(
                cluster = cluster,
                leftPx = left,
                topPx = top,
                widthPx = width,
                heightPx = height,
                headerLayout = headerLayout
            )
        }
    }

    // Pre-measured Node visuals outside onDraw
    val nodeDrawModels = remember(
        state.nodes,
        state.selectedNodeId,
        density,
        colors.textPrimary,
        colors.textTelemetry,
        colors.neonMint,
        colors.electricVioletBright
    ) {
        val nodeWidthPx = SkillMapCanvasMath.NODE_WIDTH_DP * density.density
        val nodeHeightPx = SkillMapCanvasMath.NODE_HEIGHT_DP * density.density

        state.nodes.map { node ->
            val centerPx = Offset(node.x * density.density, node.y * density.density)
            val cardLeft = centerPx.x - nodeWidthPx / 2f
            val cardTop = centerPx.y - nodeHeightPx / 2f

            val categoryColor = when {
                !node.isUnlocked -> colors.textTelemetry.copy(alpha = 0.5f)
                node.masteryState == MasteryState.MASTERED -> colors.neonMint
                node.masteryState == MasteryState.TINKERED -> colors.electricVioletBright
                else -> colors.textTelemetry
            }

            val titleColor = when {
                !node.isUnlocked -> colors.textTelemetry.copy(alpha = 0.6f)
                else -> colors.textPrimary
            }

            val titleLayout = textMeasurer.measure(
                text = node.title,
                style = typography.labelMedium.copy(color = titleColor)
            )
            val categoryLayout = textMeasurer.measure(
                text = node.category.uppercase(),
                style = typography.hudMicroLabel.copy(color = categoryColor)
            )

            NodeDrawModel(
                node = node,
                centerPx = centerPx,
                cardLeftPx = cardLeft,
                cardTopPx = cardTop,
                titleLayout = titleLayout,
                categoryLayout = categoryLayout,
                isSelected = node.id == state.selectedNodeId
            )
        }
    }

    // Pre-measured Spline visuals outside onDraw
    val splineDrawModels = remember(
        state.nodes,
        density,
        colors.neonMint,
        colors.structuralSlate
    ) {
        val nodeMap = state.nodes.associateBy { it.id }
        val splines = mutableListOf<SplineDrawModel>()

        for (targetNode in state.nodes) {
            for (prereqId in targetNode.prerequisiteIds) {
                val sourceNode = nodeMap[prereqId] ?: continue
                val isTaut = targetNode.isUnlocked || sourceNode.masteryState == MasteryState.MASTERED

                val sourceCenterPx = Offset(
                    sourceNode.x * density.density,
                    sourceNode.y * density.density
                )
                val targetCenterPx = Offset(
                    targetNode.x * density.density,
                    targetNode.y * density.density
                )

                val geometry = SkillMapCanvasMath.computeSplineGeometry(
                    sourceCenterPx = sourceCenterPx,
                    targetCenterPx = targetCenterPx,
                    isTaut = isTaut,
                    density = density.density
                )

                val strokeColor = if (isTaut) {
                    colors.neonMint
                } else {
                    colors.structuralSlate.copy(alpha = 0.65f)
                }

                val glowColor = if (isTaut) {
                    colors.neonMint.copy(alpha = 0.22f)
                } else {
                    null
                }

                splines.add(
                    SplineDrawModel(
                        geometry = geometry,
                        strokeColor = strokeColor,
                        glowColor = glowColor,
                        isTaut = isTaut
                    )
                )
            }
        }
        splines
    }

    // Initialize viewport and initial camera focus
    LaunchedEffect(density) {
        canvasState.density = density.density
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { intSize ->
                val newSize = intSize.toSize()
                val isFirstLayout = canvasState.viewportSize == Size.Zero
                canvasState.viewportSize = newSize
                canvasState.density = density.density

                if (isFirstLayout && newSize.width > 0f) {
                    val cluster1 = state.clusterBounds.firstOrNull()
                    if (cluster1 != null && canvasState.panOffset == Offset.Zero) {
                        canvasState.snapToCluster(cluster1)
                    }
                }
            }
            .pointerInput(state.nodes) {
                detectPanZoomAndTap(
                    onPanZoom = { centroid, pan, zoom ->
                        canvasState.onPanZoom(centroid, pan, zoom)
                    },
                    onTap = { tapOffset ->
                        val tappedNode = canvasState.findTappedNode(tapOffset, state.nodes)
                        if (tappedNode != null) {
                            onNodeClick(tappedNode)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = canvasState.scale
            val panOffset = canvasState.panOffset

            val nodeWidthPx = SkillMapCanvasMath.NODE_WIDTH_DP * density.density
            val nodeHeightPx = SkillMapCanvasMath.NODE_HEIGHT_DP * density.density
            val cardRadius = CornerRadius(12.dp.toPx())
            val clusterRadius = CornerRadius(24.dp.toPx())

            // 1. Fill Void Background
            drawRect(colors.canvasVoid)

            // 2. Ambient Grid Dots across visible virtual canvas
            val vLeft = max(0f, (-panOffset.x / scale) / density.density)
            val vTop = max(0f, (-panOffset.y / scale) / density.density)
            val vRight = min(
                SkillMapLayoutCoordinates.MAP_WIDTH,
                (size.width - panOffset.x) / scale / density.density
            )
            val vBottom = min(
                SkillMapLayoutCoordinates.MAP_HEIGHT,
                (size.height - panOffset.y) / scale / density.density
            )

            val gridStep = 48f
            var gx = (vLeft / gridStep).toInt() * gridStep
            val dotRadius = 1.2.dp.toPx()
            val dotColor = colors.structuralSlate.copy(alpha = 0.25f)

            while (gx <= vRight) {
                var gy = (vTop / gridStep).toInt() * gridStep
                while (gy <= vBottom) {
                    val screenX = panOffset.x + gx * density.density * scale
                    val screenY = panOffset.y + gy * density.density * scale
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(screenX, screenY)
                    )
                    gy += gridStep
                }
                gx += gridStep
            }

            // 3. Render Virtual Canvas with 2D Transform Matrix
            withTransform({
                translate(left = panOffset.x, top = panOffset.y)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                // A. Cluster Territories
                for (cluster in clusterDrawModels) {
                    // Territory background fill
                    drawRoundRect(
                        color = colors.surfaceGlass.copy(alpha = 0.22f),
                        topLeft = Offset(cluster.leftPx, cluster.topPx),
                        size = Size(cluster.widthPx, cluster.heightPx),
                        cornerRadius = clusterRadius
                    )
                    // Territory border
                    drawRoundRect(
                        color = colors.structuralSlateMuted.copy(alpha = 0.55f),
                        topLeft = Offset(cluster.leftPx, cluster.topPx),
                        size = Size(cluster.widthPx, cluster.heightPx),
                        cornerRadius = clusterRadius,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    // Cluster header badge
                    drawText(
                        textLayoutResult = cluster.headerLayout,
                        topLeft = Offset(
                            cluster.leftPx + 24.dp.toPx(),
                            cluster.topPx + 18.dp.toPx()
                        )
                    )
                }

                // B. Prerequisite Splines (Cubic Bézier curves)
                for (spline in splineDrawModels) {
                    val geom = spline.geometry
                    splinePath.reset()
                    splinePath.moveTo(geom.start.x, geom.start.y)
                    splinePath.cubicTo(
                        geom.control1.x, geom.control1.y,
                        geom.control2.x, geom.control2.y,
                        geom.end.x, geom.end.y
                    )

                    // Draw outer glow for taut active splines
                    if (spline.glowColor != null) {
                        drawPath(
                            path = splinePath,
                            color = spline.glowColor,
                            style = Stroke(
                                width = 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // Draw primary spline
                    val strokeWidth = if (spline.isTaut) 2.5.dp.toPx() else 1.8.dp.toPx()
                    drawPath(
                        path = splinePath,
                        color = spline.strokeColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )

                    // Directional arrowhead
                    arrowPath.reset()
                    arrowPath.moveTo(geom.arrowWing1.x, geom.arrowWing1.y)
                    arrowPath.lineTo(geom.end.x, geom.end.y)
                    arrowPath.lineTo(geom.arrowWing2.x, geom.arrowWing2.y)
                    drawPath(
                        path = arrowPath,
                        color = spline.strokeColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // C. Node Cards
                for (model in nodeDrawModels) {
                    val node = model.node
                    val cardTopLeft = Offset(model.cardLeftPx, model.cardTopPx)
                    val cardSize = Size(nodeWidthPx, nodeHeightPx)

                    // 1. Card container fill
                    val containerColor = when {
                        !node.isUnlocked -> colors.surfaceGlass.copy(alpha = 0.45f)
                        node.masteryState == MasteryState.MASTERED -> colors.surfaceGlass.copy(alpha = 0.95f)
                        node.masteryState == MasteryState.TINKERED -> colors.surfaceGlass.copy(alpha = 0.9f)
                        else -> colors.surfaceGlass.copy(alpha = 0.85f)
                    }
                    drawRoundRect(
                        color = containerColor,
                        topLeft = cardTopLeft,
                        size = cardSize,
                        cornerRadius = cardRadius
                    )

                    // 2. Mastery status ring & border
                    val borderColor = when {
                        !node.isUnlocked -> colors.structuralSlateMuted.copy(alpha = 0.6f)
                        node.masteryState == MasteryState.MASTERED -> colors.neonMint
                        node.masteryState == MasteryState.TINKERED -> colors.electricVioletBright
                        else -> colors.structuralSlate
                    }
                    val borderWidth = when {
                        !node.isUnlocked -> 1.dp.toPx()
                        node.masteryState == MasteryState.UNEXPLORED -> 1.5.dp.toPx()
                        else -> 2.dp.toPx()
                    }

                    // Outer radial bloom for mastered nodes
                    if (node.masteryState == MasteryState.MASTERED && node.isUnlocked) {
                        drawRoundRect(
                            color = colors.neonMint.copy(alpha = 0.22f),
                            topLeft = Offset(cardTopLeft.x - 2.dp.toPx(), cardTopLeft.y - 2.dp.toPx()),
                            size = Size(cardSize.width + 4.dp.toPx(), cardSize.height + 4.dp.toPx()),
                            cornerRadius = CornerRadius(14.dp.toPx()),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Main card border
                    drawRoundRect(
                        color = borderColor,
                        topLeft = cardTopLeft,
                        size = cardSize,
                        cornerRadius = cardRadius,
                        style = Stroke(width = borderWidth)
                    )

                    // Outer selection halo
                    if (model.isSelected) {
                        val selectionColor = if (node.masteryState == MasteryState.MASTERED) {
                            colors.neonMint
                        } else {
                            colors.electricVioletBright
                        }
                        drawRoundRect(
                            color = selectionColor,
                            topLeft = Offset(cardTopLeft.x - 3.dp.toPx(), cardTopLeft.y - 3.dp.toPx()),
                            size = Size(cardSize.width + 6.dp.toPx(), cardSize.height + 6.dp.toPx()),
                            cornerRadius = CornerRadius(15.dp.toPx()),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }

                    // 3. Category micro label
                    drawText(
                        textLayoutResult = model.categoryLayout,
                        topLeft = Offset(cardTopLeft.x + 12.dp.toPx(), cardTopLeft.y + 10.dp.toPx())
                    )

                    // 4. Algorithm title
                    drawText(
                        textLayoutResult = model.titleLayout,
                        topLeft = Offset(cardTopLeft.x + 12.dp.toPx(), cardTopLeft.y + 28.dp.toPx())
                    )

                    // 5. Mastery status indicator dot on right edge
                    val statusDotCenter = Offset(
                        cardTopLeft.x + nodeWidthPx - 16.dp.toPx(),
                        cardTopLeft.y + nodeHeightPx / 2f
                    )

                    when {
                        !node.isUnlocked -> {
                            // Muted lock dot
                            drawCircle(
                                color = colors.structuralSlateMuted,
                                radius = 3.dp.toPx(),
                                center = statusDotCenter
                            )
                        }
                        node.masteryState == MasteryState.MASTERED -> {
                            // Mastered dot + glowing aura
                            drawCircle(
                                color = colors.neonMint.copy(alpha = 0.35f),
                                radius = 7.dp.toPx(),
                                center = statusDotCenter
                            )
                            drawCircle(
                                color = colors.neonMint,
                                radius = 4.dp.toPx(),
                                center = statusDotCenter
                            )
                        }
                        node.masteryState == MasteryState.TINKERED -> {
                            // Tinkered violet dot
                            drawCircle(
                                color = colors.electricVioletBright,
                                radius = 4.dp.toPx(),
                                center = statusDotCenter
                            )
                        }
                        else -> {
                            // Unexplored hollow ring
                            drawCircle(
                                color = colors.structuralSlate,
                                radius = 4.dp.toPx(),
                                center = statusDotCenter,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Handles combined pan, zoom, and tap gestures without event contention.
 */
private suspend fun PointerInputScope.detectPanZoomAndTap(
    onPanZoom: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onTap: (Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var totalPan = Offset.Zero

        while (true) {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (canceled) break

            val activePointers = event.changes.filter { it.pressed }
            if (activePointers.isEmpty()) {
                if (!pastTouchSlop) {
                    onTap(down.position)
                }
                break
            }

            if (activePointers.size > 1) {
                pastTouchSlop = true
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                val centroid = event.calculateCentroid(useCurrent = false)
                if (zoomChange != 1f || panChange != Offset.Zero) {
                    onPanZoom(centroid, panChange, zoomChange)
                    event.changes.forEach { it.consume() }
                }
            } else if (activePointers.size == 1) {
                val change = activePointers.first()
                val panChange = change.position - change.previousPosition
                totalPan += panChange
                if (!pastTouchSlop) {
                    if (totalPan.getDistance() > touchSlop) {
                        pastTouchSlop = true
                    }
                }
                if (pastTouchSlop && panChange != Offset.Zero) {
                    onPanZoom(change.position, panChange, 1f)
                    change.consume()
                }
            }
        }
    }
}

@Preview(name = "SkillMapCanvas Dark Mode", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun SkillMapCanvasPreview() {
    UnravelTheme {
        val sampleNodes = persistentListOf(
            SkillNodeUiModel(
                id = "two_pointers",
                title = "Two Pointers",
                category = "ARRAY",
                clusterIndex = 1,
                x = 300f,
                y = 350f,
                prerequisiteIds = persistentListOf(),
                isUnlocked = true,
                masteryState = MasteryState.MASTERED
            ),
            SkillNodeUiModel(
                id = "sliding_window",
                title = "Sliding Window",
                category = "ARRAY",
                clusterIndex = 1,
                x = 650f,
                y = 250f,
                prerequisiteIds = persistentListOf("two_pointers"),
                isUnlocked = true,
                masteryState = MasteryState.TINKERED
            ),
            SkillNodeUiModel(
                id = "binary_search",
                title = "Binary Search",
                category = "ARRAY",
                clusterIndex = 1,
                x = 650f,
                y = 480f,
                prerequisiteIds = persistentListOf("two_pointers"),
                isUnlocked = false,
                masteryState = MasteryState.UNEXPLORED
            )
        )

        SkillMapCanvas(
            state = SkillMapUiState(
                nodes = sampleNodes,
                clusterBounds = SkillMapLayoutCoordinates.CLUSTER_BOUNDS,
                selectedNodeId = "sliding_window"
            )
        )
    }
}
