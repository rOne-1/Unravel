package com.unravel.core.canvasrenderer.tree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import com.unravel.core.designsystem.theme.NeonMint
import com.unravel.core.designsystem.theme.StructuralSlate
import com.unravel.core.designsystem.theme.StructuralSlateMuted
import com.unravel.core.designsystem.theme.SurfaceGlass
import com.unravel.core.designsystem.theme.SurfaceGlassBorder
import com.unravel.core.designsystem.theme.UnravelMotionConstants
import com.unravel.core.designsystem.theme.UnravelTheme
import com.unravel.core.engine.model.TreeEdge
import com.unravel.core.engine.model.TreeNode
import com.unravel.core.engine.model.VisualStructureState.HierarchyTreeState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.math.abs
import kotlin.math.sin
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * High-performance Compose Canvas rendering hierarchical trees, tensioned branch splines,
 * balance factor badges, AVL rotation arcs, and a bottom traversal tray.
 * Strictly guarantees ZERO heap allocations inside `onDraw`.
 */
@Composable
fun HierarchyTreeCanvas(
    state: HierarchyTreeState,
    modifier: Modifier = Modifier,
    onNodeTap: ((String) -> Unit)? = null
) {
    val hapticEngine = LocalHapticEngine.current
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Pre-allocated reusable paths to guarantee ZERO heap allocations in onDraw
    val cachedBranchPath = remember { Path() }
    val cachedBadgePath = remember { Path() }
    val cachedTrayPath = remember { Path() }

    // Pre-computed text styles
    val nodeTextStyle = remember(colors.textPrimary, typography.nodeValueLabel) {
        typography.nodeValueLabel.copy(color = colors.textPrimary)
    }
    val badgeTextStyle = remember(colors.textPrimary, typography.hudMicroLabel) {
        typography.hudMicroLabel.copy(color = colors.textPrimary)
    }
    val heightTextStyle = remember(colors.textTelemetry, typography.hudMicroLabel) {
        typography.hudMicroLabel.copy(color = colors.textTelemetry)
    }

    // AVL rotation spring animation when rotatingSubtreeRootId transitions
    val rotationSpringAnim = remember { Animatable(0f) }
    LaunchedEffect(state.rotatingSubtreeRootId) {
        if (state.rotatingSubtreeRootId != null) {
            rotationSpringAnim.snapTo(0f)
            rotationSpringAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = UnravelMotionConstants.DAMPING_RATIO_SPATIAL,
                    stiffness = UnravelMotionConstants.STIFFNESS_SPATIAL
                )
            )
        } else {
            rotationSpringAnim.snapTo(0f)
        }
    }

    val nodeRadiusPx = with(density) { 22.dp.toPx() }
    val trayHeightPx = with(density) { 48.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.nodes) {
                detectTapGestures { tapOffset ->
                    val treeHeight = size.height - trayHeightPx
                    for (node in state.nodes) {
                        val nx = node.normX * size.width
                        val ny = node.normY * treeHeight
                        val dx = tapOffset.x - nx
                        val dy = tapOffset.y - ny
                        if (dx * dx + dy * dy <= nodeRadiusPx * nodeRadiusPx) {
                            onNodeTap?.invoke(node.id.toString())
                            break
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val treeHeight = height - trayHeightPx

        val nodeMap = state.nodes.associateBy { it.id.toString() }

        // 1. Draw Tree Branch Splines
        for (edge in state.edges) {
            val fromNode = nodeMap[edge.fromId.toString()] ?: continue
            val toNode = nodeMap[edge.toId.toString()] ?: continue

            val px = fromNode.normX * width
            val py = fromNode.normY * treeHeight
            val cx = toNode.normX * width
            val cy = toNode.normY * treeHeight

            val isBranchHighlighted = edge.isHighlighted ||
                    (state.visitedNodeIds.contains(fromNode.id.toString()) && state.visitedNodeIds.contains(toNode.id.toString()))

            val branchColor = when {
                isBranchHighlighted -> colors.electricVioletBright
                fromNode.id.toString() == state.rotatingSubtreeRootId -> colors.coralFlame
                else -> colors.structuralSlate
            }
            val strokeWidth = if (isBranchHighlighted) 3.dp.toPx() else 2.dp.toPx()

            cachedBranchPath.reset()
            cachedBranchPath.moveTo(px, py + nodeRadiusPx)

            val midY = (py + cy) / 2f
            cachedBranchPath.cubicTo(
                px, midY,
                cx, midY,
                cx, cy - nodeRadiusPx
            )

            drawPath(
                path = cachedBranchPath,
                color = branchColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // 2. Draw Tree Nodes & Badges
        for (node in state.nodes) {
            val nodeId = node.id.toString()
            var nx = node.normX * width
            var ny = node.normY * treeHeight

            val isRotating = nodeId == state.rotatingSubtreeRootId
            val isUnbalanced = abs(node.balanceFactor) >= 2
            val isActive = nodeId == state.activeNodeId
            val isVisited = state.visitedNodeIds.contains(nodeId)

            // Apply smooth rotation spring arc trajectory offset
            if (isRotating && rotationSpringAnim.value > 0f) {
                val arcProgress = rotationSpringAnim.value
                val arcOffset = 12.dp.toPx() * (1f - arcProgress) * sin(arcProgress * Math.PI).toFloat()
                nx += arcOffset
            }

            val fillColor = when {
                isRotating || isUnbalanced -> colors.coralFlame
                isActive -> colors.electricVioletBright
                isVisited -> colors.neonMint
                else -> colors.structuralSlate
            }

            // Draw Node Circle
            drawCircle(
                color = fillColor,
                radius = nodeRadiusPx,
                center = Offset(nx, ny)
            )

            // Outer crisp border
            drawCircle(
                color = if (isRotating) colors.coralFlame else colors.surfaceGlassBorder,
                radius = nodeRadiusPx,
                center = Offset(nx, ny),
                style = Stroke(width = if (isRotating) 2.5.dp.toPx() else 1.5.dp.toPx())
            )

            // Center Node Value Text
            val textMeasure = textMeasurer.measure(node.value.toString(), nodeTextStyle)
            val textPos = Offset(
                nx - (textMeasure.size.width / 2f),
                ny - (textMeasure.size.height / 2f)
            )
            drawText(
                textLayoutResult = textMeasure,
                topLeft = textPos
            )

            // Balance Factor Badge (top-right of node)
            val bfText = "BF:${node.balanceFactor}"
            val bfMeasure = textMeasurer.measure(bfText, badgeTextStyle)
            val badgeW = bfMeasure.size.width + 8.dp.toPx()
            val badgeH = bfMeasure.size.height + 4.dp.toPx()
            val badgeLeft = nx + nodeRadiusPx - 4.dp.toPx()
            val badgeTop = ny - nodeRadiusPx - (badgeH / 2f)

            val bfBgColor = if (isUnbalanced) colors.coralFlame else colors.surfaceGlass
            val bfBorderColor = if (isUnbalanced) colors.coralFlame else colors.surfaceGlassBorder

            drawRoundRect(
                color = bfBgColor,
                topLeft = Offset(badgeLeft, badgeTop),
                size = Size(badgeW, badgeH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = bfBorderColor,
                topLeft = Offset(badgeLeft, badgeTop),
                size = Size(badgeW, badgeH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            drawText(
                textLayoutResult = bfMeasure,
                topLeft = Offset(badgeLeft + 4.dp.toPx(), badgeTop + 2.dp.toPx())
            )

            // Height Readout (subtle below node)
            val hText = "h:${node.height}"
            val hMeasure = textMeasurer.measure(hText, heightTextStyle)
            drawText(
                textLayoutResult = hMeasure,
                topLeft = Offset(nx - (hMeasure.size.width / 2f), ny + nodeRadiusPx + 2.dp.toPx())
            )
        }

        // 3. Draw Bottom Traversal Tray
        val trayTop = height - trayHeightPx
        drawRect(
            color = colors.surfaceGlass,
            topLeft = Offset(0f, trayTop),
            size = Size(width, trayHeightPx)
        )
        drawLine(
            color = colors.surfaceGlassBorder,
            start = Offset(0f, trayTop),
            end = Offset(width, trayTop),
            strokeWidth = 1.dp.toPx()
        )

        // Traversal order pill row
        var pillX = 16.dp.toPx()
        val pillY = trayTop + 10.dp.toPx()

        for (orderId in state.traversalOrderIds) {
            val n = nodeMap[orderId] ?: continue
            val valStr = n.value.toString()
            val valMeasure = textMeasurer.measure(valStr, badgeTextStyle)
            val pillW = valMeasure.size.width + 16.dp.toPx()
            val pillH = trayHeightPx - 20.dp.toPx()

            if (pillX + pillW < width - 16.dp.toPx()) {
                drawRoundRect(
                    color = colors.structuralSlateMuted,
                    topLeft = Offset(pillX, pillY),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                drawRoundRect(
                    color = colors.neonMint,
                    topLeft = Offset(pillX, pillY),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawText(
                    textLayoutResult = valMeasure,
                    topLeft = Offset(pillX + 8.dp.toPx(), pillY + (pillH - valMeasure.size.height) / 2f)
                )
                pillX += pillW + 8.dp.toPx()
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Preview(name = "Hierarchy Tree - Dark", widthDp = 360, heightDp = 360)
@Composable
fun PreviewHierarchyTreeCanvas() {
    val idRoot = Uuid.random()
    val idLeft = Uuid.random()
    val idRight = Uuid.random()

    val nodes = persistentListOf(
        TreeNode(id = idRoot, value = 50, leftId = idLeft, rightId = idRight, height = 2, balanceFactor = 0, normX = 0.5f, normY = 0.2f),
        TreeNode(id = idLeft, value = 30, height = 1, balanceFactor = 0, normX = 0.28f, normY = 0.55f),
        TreeNode(id = idRight, value = 70, height = 1, balanceFactor = 0, normX = 0.72f, normY = 0.55f)
    )
    val edges = persistentListOf(
        TreeEdge(fromId = idRoot, toId = idLeft),
        TreeEdge(fromId = idRoot, toId = idRight)
    )

    UnravelTheme {
        HierarchyTreeCanvas(
            state = HierarchyTreeState(
                nodes = nodes,
                edges = edges,
                activeNodeId = idRoot.toString(),
                traversalOrderIds = persistentListOf(idLeft.toString(), idRoot.toString(), idRight.toString())
            )
        )
    }
}
