package com.unravel.core.canvasrenderer.array

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unravel.core.canvasrenderer.haptics.LocalHapticEngine
import com.unravel.core.canvasrenderer.interpolator.ParabolicArcInterpolator
import com.unravel.core.designsystem.theme.CanvasVoid
import com.unravel.core.designsystem.theme.UnravelTheme
import com.unravel.core.engine.model.ArrayNode
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.abs

/**
 * Primary high-frame-rate Canvas rendering animated array bars, pointer badges,
 * sliding window overlays, and direct-edit touch interactions.
 *
 * Adheres strictly to the ZERO-ALLOCATION rule during `onDraw` passes by caching
 * paths, measurements, and geometry calculations.
 */
@Composable
fun ArrayPointerCanvas(
    arrayState: VisualStructureState.ArrayState,
    modifier: Modifier = Modifier,
    swapProgress: Float? = null,
    onValueChange: ((index: Int, newValue: Int) -> Unit)? = null,
    onSwap: ((indexA: Int, indexB: Int) -> Unit)? = null,
    onBarTap: ((index: Int) -> Unit)? = null
) {
    val hapticEngine = LocalHapticEngine.current
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography
    val density = LocalDensity.current

    val horizontalPaddingPx = with(density) { 16.dp.toPx() }
    val topMarginPx = with(density) { 56.dp.toPx() }
    val bottomMarginPx = with(density) { 46.dp.toPx() }
    val cornerRadiusPx = with(density) { 4.dp.toPx() }
    val arcHeightPx = with(density) { 48.dp.toPx() }
    val badgeCornerRadius = with(density) { 4.dp.toPx() }
    val badgeCornerRadiusObj = remember(badgeCornerRadius) { CornerRadius(badgeCornerRadius, badgeCornerRadius) }

    // Pre-allocated reusable path to guarantee zero heap allocations in onDraw
    val cachedBarPath = remember { Path() }
    val cachedBadgePath = remember { Path() }
    val textMeasurer = rememberTextMeasurer()

    // Pre-compute text styles with theme tokens
    val valueTextStyle = remember(colors.textPrimary, typography.nodeValueLabel) {
        typography.nodeValueLabel.copy(color = colors.textPrimary)
    }
    val indexTextStyle = remember(colors.textTelemetry, typography.hudMicroLabel) {
        typography.hudMicroLabel.copy(color = colors.textTelemetry)
    }
    val badgeTextStyle = remember(colors.textPrimary, typography.hudMicroLabel) {
        typography.hudMicroLabel.copy(color = colors.textPrimary)
    }

    // Group pointers by index for efficient badge lookup
    val pointersByIndex = remember(arrayState.pointers) {
        arrayState.pointers.entries.groupBy({ it.value }, { it.key })
    }

    // Dynamic internal animation for mutating/swapping indices when swapProgress is not caller-driven
    val internalSwapAnim = remember { Animatable(0f) }
    LaunchedEffect(arrayState.mutatingIndices) {
        if (arrayState.mutatingIndices.size == 2) {
            internalSwapAnim.snapTo(0f)
            internalSwapAnim.animateTo(
                targetValue = 1f,
                animationSpec = ParabolicArcInterpolator.DefaultSpringSpec
            )
        } else {
            internalSwapAnim.snapTo(0f)
        }
    }
    val effectiveSwapProgress = swapProgress ?: internalSwapAnim.value

    val items = arrayState.items
    val itemCount = items.size
    val maxValue = remember(items) {
        items.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(itemCount) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val draggedIndex = ArrayCanvasLayoutCalculator.calculateIndexFromX(
                        touchX = down.position.x,
                        itemCount = itemCount,
                        canvasWidth = size.width.toFloat(),
                        horizontalPadding = horizontalPaddingPx
                    )
                    val startValue = items.getOrNull(draggedIndex)?.value ?: 0
                    var lastReportedValue = startValue
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var isDrag = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) {
                            if (!isDrag) {
                                if (draggedIndex in 0 until itemCount) {
                                    onBarTap?.invoke(draggedIndex)
                                    hapticEngine.performHaptic(HapticCue.TICK)
                                }
                            } else {
                                if (abs(totalDragX) > abs(totalDragY) && abs(totalDragX) > 24f && draggedIndex in 0 until itemCount) {
                                    val targetIndex = ArrayCanvasLayoutCalculator.calculateIndexFromX(
                                        touchX = horizontalPaddingPx + (draggedIndex + 0.5f) * ((size.width - 2f * horizontalPaddingPx) / itemCount) + totalDragX,
                                        itemCount = itemCount,
                                        canvasWidth = size.width.toFloat(),
                                        horizontalPadding = horizontalPaddingPx
                                    )
                                    if (targetIndex != draggedIndex && targetIndex in 0 until itemCount) {
                                        onSwap?.invoke(draggedIndex, targetIndex)
                                        hapticEngine.performHaptic(HapticCue.SWAP_THUD)
                                    }
                                }
                            }
                            break
                        }

                        val dragAmount = change.position - change.previousPosition
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        if (!isDrag && (abs(totalDragX) > 8f || abs(totalDragY) > 8f)) {
                            isDrag = true
                        }

                        if (isDrag) {
                            change.consume()
                            if (abs(totalDragY) > abs(totalDragX) && abs(totalDragY) > 8f) {
                                val stepPx = 14f
                                val deltaVal = (-totalDragY / stepPx).toInt()
                                val newVal = (startValue + deltaVal).coerceIn(1, 99)
                                if (newVal != lastReportedValue && draggedIndex in 0 until itemCount) {
                                    lastReportedValue = newVal
                                    onValueChange?.invoke(draggedIndex, newVal)
                                    hapticEngine.performHaptic(HapticCue.TICK)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        if (itemCount == 0) return@Canvas

        // 1. Draw Sliding Window Overlay (if active)
        val windowRange = arrayState.windowRange
        if (windowRange != null) {
            val windowBounds = ArrayCanvasLayoutCalculator.calculateWindowBounds(
                windowRange = windowRange,
                itemCount = itemCount,
                canvasSize = size,
                horizontalPadding = horizontalPaddingPx,
                topMargin = topMarginPx,
                bottomMargin = bottomMarginPx
            )
            if (windowBounds != null) {
                // Translucent fill
                drawRoundRect(
                    color = colors.electricVioletDeep.copy(alpha = 0.18f),
                    topLeft = Offset(windowBounds.left, windowBounds.top),
                    size = Size(windowBounds.width, windowBounds.height),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                // Crisp border
                drawRoundRect(
                    color = colors.electricVioletBright,
                    topLeft = Offset(windowBounds.left, windowBounds.top),
                    size = Size(windowBounds.width, windowBounds.height),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Determine swapping indices for parabolic trajectory offset
        val mutatingList = arrayState.mutatingIndices.sorted()
        val isSwappingPair = mutatingList.size == 2

        // 2. Draw Array Bars
        for (i in 0 until itemCount) {
            val item = items[i]
            val baseGeom = ArrayCanvasLayoutCalculator.calculateBarGeometry(
                index = i,
                value = item.value,
                maxValue = maxValue,
                itemCount = itemCount,
                canvasSize = size,
                horizontalPadding = horizontalPaddingPx,
                topMargin = topMarginPx,
                bottomMargin = bottomMarginPx
            )

            // Calculate translation offset if participating in parabolic swap
            var offsetX = 0f
            var offsetY = 0f

            if (isSwappingPair && (i == mutatingList[0] || i == mutatingList[1]) && effectiveSwapProgress > 0f) {
                val targetIndex = if (i == mutatingList[0]) mutatingList[1] else mutatingList[0]
                val targetGeom = ArrayCanvasLayoutCalculator.calculateBarGeometry(
                    index = targetIndex,
                    value = item.value,
                    maxValue = maxValue,
                    itemCount = itemCount,
                    canvasSize = size,
                    horizontalPadding = horizontalPaddingPx,
                    topMargin = topMarginPx,
                    bottomMargin = bottomMarginPx
                )

                val interpolatedPos = ParabolicArcInterpolator.calculatePosition(
                    sourceCenter = baseGeom.center,
                    targetCenter = targetGeom.center,
                    progress = effectiveSwapProgress,
                    arcHeight = arcHeightPx
                )
                offsetX = interpolatedPos.x - baseGeom.center.x
                offsetY = interpolatedPos.y - baseGeom.center.y
            }

            val barColor = ArrayCanvasLayoutCalculator.resolveBarColor(
                index = i,
                activeIndices = arrayState.activeIndices,
                mutatingIndices = arrayState.mutatingIndices,
                sortedIndices = arrayState.sortedIndices,
                discardedRange = arrayState.discardedRange,
                colors = colors
            )

            // Draw bar with rounded top corners via cachedPath (Zero allocations in onDraw)
            val barLeft = baseGeom.rect.left + offsetX
            val barRight = baseGeom.rect.right + offsetX
            val barTop = baseGeom.rect.top + offsetY
            val barBottom = baseGeom.rect.bottom + offsetY

            cachedBarPath.reset()
            cachedBarPath.moveTo(barLeft, barBottom)
            cachedBarPath.lineTo(barLeft, barTop + cornerRadiusPx)
            cachedBarPath.quadraticTo(barLeft, barTop, barLeft + cornerRadiusPx, barTop)
            cachedBarPath.lineTo(barRight - cornerRadiusPx, barTop)
            cachedBarPath.quadraticTo(barRight, barTop, barRight, barTop + cornerRadiusPx)
            cachedBarPath.lineTo(barRight, barBottom)
            cachedBarPath.close()

            drawPath(path = cachedBarPath, color = barColor)

            // 3. Draw Monospace Value Label below baseline
            val valueStr = item.value.toString()
            val valueLayout = textMeasurer.measure(valueStr, style = valueTextStyle)
            val valueX = ((barLeft + barRight) / 2f) - (valueLayout.size.width / 2f)
            val valueY = baseGeom.rect.bottom + 4.dp.toPx()

            drawText(
                textLayoutResult = valueLayout,
                topLeft = Offset(valueX, valueY)
            )

            // 4. Draw Monospace Index Label below value
            val indexStr = i.toString()
            val indexLayout = textMeasurer.measure(indexStr, style = indexTextStyle)
            val indexX = ((barLeft + barRight) / 2f) - (indexLayout.size.width / 2f)
            val indexY = valueY + valueLayout.size.height + 1.dp.toPx()

            drawText(
                textLayoutResult = indexLayout,
                topLeft = Offset(indexX, indexY)
            )

            // 5. Draw Floating Pointer Badges above bar
            val pointers = pointersByIndex[i]
            if (pointers != null && pointers.isNotEmpty()) {
                val badgeText = pointers.joinToString(separator = ",")
                val badgeLayout = textMeasurer.measure(badgeText, style = badgeTextStyle)

                val badgeWidth = badgeLayout.size.width + 12.dp.toPx()
                val badgeHeight = badgeLayout.size.height + 6.dp.toPx()
                val badgeCenterX = (barLeft + barRight) / 2f
                val badgeLeft = badgeCenterX - (badgeWidth / 2f)
                val badgeTop = (barTop - badgeHeight - 8.dp.toPx()).coerceAtLeast(4.dp.toPx())

                // Draw badge pill background
                drawRoundRect(
                    color = colors.surfaceGlass,
                    topLeft = Offset(badgeLeft, badgeTop),
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = badgeCornerRadiusObj
                )
                // Draw badge border
                drawRoundRect(
                    color = colors.electricVioletBright,
                    topLeft = Offset(badgeLeft, badgeTop),
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = badgeCornerRadiusObj,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Draw pointer downward caret
                val caretBottom = badgeTop + badgeHeight + 4.dp.toPx()
                cachedBadgePath.reset()
                cachedBadgePath.moveTo(badgeCenterX - 4.dp.toPx(), badgeTop + badgeHeight)
                cachedBadgePath.lineTo(badgeCenterX + 4.dp.toPx(), badgeTop + badgeHeight)
                cachedBadgePath.lineTo(badgeCenterX, caretBottom)
                cachedBadgePath.close()
                drawPath(path = cachedBadgePath, color = colors.electricVioletBright)

                // Draw badge label text
                drawText(
                    textLayoutResult = badgeLayout,
                    topLeft = Offset(badgeLeft + 6.dp.toPx(), badgeTop + 3.dp.toPx())
                )
            }
        }
    }
}

// ======================================================================================
// Previews for Jetpack Compose & Roborazzi
// ======================================================================================

@Preview(name = "ArrayPointerCanvas - Neutral State", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun ArrayPointerCanvasPreview_Neutral() {
    UnravelTheme {
        val sampleItems = persistentListOf(
            ArrayNode(value = 42),
            ArrayNode(value = 17),
            ArrayNode(value = 89),
            ArrayNode(value = 63),
            ArrayNode(value = 31),
            ArrayNode(value = 55)
        )
        ArrayPointerCanvas(
            arrayState = VisualStructureState.ArrayState(items = sampleItems)
        )
    }
}

@Preview(name = "ArrayPointerCanvas - Active Comparing", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun ArrayPointerCanvasPreview_ActiveComparing() {
    UnravelTheme {
        val sampleItems = persistentListOf(
            ArrayNode(value = 12),
            ArrayNode(value = 45),
            ArrayNode(value = 28),
            ArrayNode(value = 76),
            ArrayNode(value = 54)
        )
        ArrayPointerCanvas(
            arrayState = VisualStructureState.ArrayState(
                items = sampleItems,
                activeIndices = persistentSetOf(1, 2),
                pointers = persistentMapOf("i" to 1, "j" to 2)
            )
        )
    }
}

@Preview(name = "ArrayPointerCanvas - Swapping Trajectory", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun ArrayPointerCanvasPreview_Swapping() {
    UnravelTheme {
        val sampleItems = persistentListOf(
            ArrayNode(value = 30),
            ArrayNode(value = 90),
            ArrayNode(value = 10),
            ArrayNode(value = 70)
        )
        ArrayPointerCanvas(
            arrayState = VisualStructureState.ArrayState(
                items = sampleItems,
                mutatingIndices = persistentSetOf(1, 2),
                sortedIndices = persistentSetOf(3),
                pointers = persistentMapOf("pivot" to 3)
            ),
            swapProgress = 0.5f
        )
    }
}

@Preview(name = "ArrayPointerCanvas - Sliding Window", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun ArrayPointerCanvasPreview_SlidingWindow() {
    UnravelTheme {
        val sampleItems = (1..8).map { ArrayNode(value = it * 11) }.toPersistentList()
        ArrayPointerCanvas(
            arrayState = VisualStructureState.ArrayState(
                items = sampleItems,
                windowRange = 2..5,
                pointers = persistentMapOf("left" to 2, "right" to 5)
            )
        )
    }
}
