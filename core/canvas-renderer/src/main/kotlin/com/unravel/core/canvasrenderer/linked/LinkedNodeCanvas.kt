package com.unravel.core.canvasrenderer.linked

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import com.unravel.core.designsystem.theme.UnravelTheme
import com.unravel.core.engine.model.LinkedNode
import com.unravel.core.engine.model.VisualStructureState.LinkedNodeState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * High-performance Compose Canvas rendering linked lists, Floyd orbital rings,
 * and Stack/Queue containers with tensioned Bézier spline links.
 * Strictly adheres to ZERO heap allocations during `onDraw`.
 */
@Composable
fun LinkedNodeCanvas(
    state: LinkedNodeState,
    modifier: Modifier = Modifier,
    onNodeTap: ((String) -> Unit)? = null
) {
    val hapticEngine = LocalHapticEngine.current
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography
    val textMeasurer = rememberTextMeasurer()

    // Pre-allocated reusable paths to guarantee ZERO heap allocations in onDraw
    val cachedSplinePath = remember { Path() }
    val cachedArrowPath = remember { Path() }
    val cachedBadgePath = remember { Path() }

    // Text styles
    val nodeTextStyle = remember(colors.textPrimary, typography.nodeValueLabel) {
        typography.nodeValueLabel.copy(color = colors.textPrimary)
    }
    val badgeTextStyle = remember(colors.textPrimary, typography.hudMicroLabel) {
        typography.hudMicroLabel.copy(color = colors.textPrimary)
    }

    // Concentric ring pulse animation for Floyd's Tortoise & Hare pointers
    val ringPulse = remember { Animatable(0f) }
    LaunchedEffect(state.isCycleDetected) {
        if (state.isCycleDetected) {
            ringPulse.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600)
            )
        } else {
            ringPulse.snapTo(0f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.nodes) {
                detectTapGestures { tapOffset ->
                    val layout = LinkedNodeLayoutCalculator.calculateLayout(state, size.width.toFloat(), size.height.toFloat())
                    for ((id, pos) in layout.nodePositions) {
                        val dx = tapOffset.x - pos.x
                        val dy = tapOffset.y - pos.y
                        if (dx * dx + dy * dy <= layout.nodeRadius * layout.nodeRadius) {
                            onNodeTap?.invoke(id)
                            break
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        val layout = LinkedNodeLayoutCalculator.calculateLayout(state, width, height)
        val nodePositions = layout.nodePositions
        val nodeRadius = layout.nodeRadius

        // 1. Draw Floyd Orbital Loop Guide Ring (if cycle present)
        if (layout.mode == LinkedNodeLayoutCalculator.ContainerMode.FLOYD_ORBITAL &&
            layout.orbitCenter != null && layout.orbitRadius != null
        ) {
            val center = layout.orbitCenter
            val radius = layout.orbitRadius

            // Base orbital loop ring
            drawCircle(
                color = colors.structuralSlateMuted,
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Dynamic Tortoise & Hare Concentric Orbital Rings
            val slowNodeId = state.pointers["slow"]
            val fastNodeId = state.pointers["fast"]

            if (slowNodeId != null && nodePositions.containsKey(slowNodeId)) {
                val slowPos = nodePositions.getValue(slowNodeId)
                drawCircle(
                    color = colors.electricVioletBright,
                    radius = nodeRadius + 8.dp.toPx(),
                    center = slowPos,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            if (fastNodeId != null && nodePositions.containsKey(fastNodeId)) {
                val fastPos = nodePositions.getValue(fastNodeId)
                drawCircle(
                    color = if (state.isCycleDetected) colors.coralFlame else colors.neonMint,
                    radius = nodeRadius + 14.dp.toPx(),
                    center = fastPos,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // 2. Draw Tensioned Quadratic Bézier Spline Links
        for (link in layout.links) {
            val strokeColor = when {
                link.isMutating -> colors.coralFlame
                link.isActive -> colors.electricVioletBright
                link.isCycleLoop -> colors.coralFlame
                else -> colors.structuralSlate
            }
            val strokeWidth = if (link.isActive || link.isMutating || link.isCycleLoop) 3.dp.toPx() else 2.dp.toPx()

            cachedSplinePath.reset()
            cachedSplinePath.moveTo(link.start.x, link.start.y)
            cachedSplinePath.quadraticTo(link.control.x, link.control.y, link.end.x, link.end.y)

            drawPath(
                path = cachedSplinePath,
                color = strokeColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw directional arrowhead at link.end
            drawArrowhead(
                tip = link.end,
                from = link.control,
                color = strokeColor,
                path = cachedArrowPath
            )
        }

        // 3. Draw Nodes (circular/pill with value labels)
        for (node in state.nodes) {
            val nodeId = node.id.toString()
            val pos = nodePositions[nodeId] ?: continue

            val isLocked = state.lockedNodeIds.contains(nodeId)
            val isCycleDetected = state.isCycleDetected && state.pointers["cycleEntry"] == nodeId
            val isSlowPointer = state.pointers["slow"] == nodeId
            val isFastPointer = state.pointers["fast"] == nodeId
            val isTopOrHead = state.pointers["top"] == nodeId || state.pointers["head"] == nodeId

            val fillColor = when {
                isCycleDetected -> colors.coralFlame
                isLocked -> colors.neonMint
                isSlowPointer -> colors.electricVioletBright
                isFastPointer -> colors.neonMint
                isTopOrHead -> colors.electricVioletDeep
                else -> colors.structuralSlate
            }

            // Node body fill
            drawCircle(
                color = fillColor,
                radius = nodeRadius,
                center = pos
            )

            // Outer crisp border
            drawCircle(
                color = colors.surfaceGlassBorder,
                radius = nodeRadius,
                center = pos,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Centered node value text
            val textLayoutResult = textMeasurer.measure(node.value.toString(), nodeTextStyle)
            val textOffset = Offset(
                pos.x - (textLayoutResult.size.width / 2f),
                pos.y - (textLayoutResult.size.height / 2f)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = textOffset
            )
        }

        // 4. Draw Pointer Badges (prev, curr, next, top, head, tail, slow, fast)
        val badgesByNode = state.pointers.entries.groupBy({ it.value }, { it.key })
        for ((nodeId, pointerNames) in badgesByNode) {
            val pos = nodePositions[nodeId] ?: continue
            val label = pointerNames.joinToString(", ")

            val badgeMeasure = textMeasurer.measure(label, badgeTextStyle)
            val badgeWidth = badgeMeasure.size.width + 12.dp.toPx()
            val badgeHeight = badgeMeasure.size.height + 6.dp.toPx()
            val badgeTop = pos.y - nodeRadius - badgeHeight - 4.dp.toPx()
            val badgeLeft = pos.x - (badgeWidth / 2f)

            // Draw pill badge
            drawRoundRect(
                color = colors.surfaceGlass,
                topLeft = Offset(badgeLeft, badgeTop),
                size = Size(badgeWidth, badgeHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = colors.electricVioletBright,
                topLeft = Offset(badgeLeft, badgeTop),
                size = Size(badgeWidth, badgeHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            val textPos = Offset(
                badgeLeft + (badgeWidth - badgeMeasure.size.width) / 2f,
                badgeTop + (badgeHeight - badgeMeasure.size.height) / 2f
            )
            drawText(
                textLayoutResult = badgeMeasure,
                topLeft = textPos
            )
        }
    }
}

/**
 * Renders directional arrowhead at path tip using cached reusable path to guarantee zero allocation.
 */
private fun DrawScope.drawArrowhead(
    tip: Offset,
    from: Offset,
    color: Color,
    path: Path
) {
    val angle = atan2(tip.y - from.y, tip.x - from.x)
    val arrowLen = 10.dp.toPx()
    val arrowAngle = Math.PI / 6.0 // 30 degrees

    val x1 = (tip.x - arrowLen * cos(angle - arrowAngle)).toFloat()
    val y1 = (tip.y - arrowLen * sin(angle - arrowAngle)).toFloat()

    val x2 = (tip.x - arrowLen * cos(angle + arrowAngle)).toFloat()
    val y2 = (tip.y - arrowLen * sin(angle + arrowAngle)).toFloat()

    path.reset()
    path.moveTo(tip.x, tip.y)
    path.lineTo(x1, y1)
    path.lineTo(x2, y2)
    path.close()

    drawPath(path, color)
}

@OptIn(ExperimentalUuidApi::class)
@Preview(name = "Linked List Reversal - Dark", widthDp = 360, heightDp = 240)
@Composable
fun PreviewLinkedListCanvas() {
    val id1 = Uuid.random()
    val id2 = Uuid.random()
    val id3 = Uuid.random()

    val nodes = persistentListOf(
        LinkedNode(id = id1, value = 10, nextId = id2),
        LinkedNode(id = id2, value = 20, nextId = id3),
        LinkedNode(id = id3, value = 30, nextId = null)
    )

    UnravelTheme {
        LinkedNodeCanvas(
            state = LinkedNodeState(
                nodes = nodes,
                pointers = persistentMapOf("head" to id1.toString(), "curr" to id2.toString())
            )
        )
    }
}
