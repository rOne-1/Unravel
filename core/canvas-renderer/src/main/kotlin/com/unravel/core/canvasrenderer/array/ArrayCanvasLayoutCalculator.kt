package com.unravel.core.canvasrenderer.array

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.unravel.core.designsystem.theme.UnravelColors

/**
 * Geometric parameters and coordinate bounds for an individual array bar.
 */
data class BarGeometry(
    val index: Int,
    val value: Int,
    val rect: Rect,
    val center: Offset
)

/**
 * Pure mathematical layout calculator for [ArrayPointerCanvas].
 * Decouples geometry and coordinate mapping from rendering for exhaustive testability.
 */
object ArrayCanvasLayoutCalculator {

    /**
     * Calculates the [BarGeometry] for a bar at [index].
     */
    fun calculateBarGeometry(
        index: Int,
        value: Int,
        maxValue: Int,
        itemCount: Int,
        canvasSize: Size,
        horizontalPadding: Float,
        topMargin: Float,
        bottomMargin: Float,
        minBarHeight: Float = 14f
    ): BarGeometry {
        if (itemCount <= 0 || canvasSize.width <= 0f || canvasSize.height <= 0f) {
            return BarGeometry(index, value, Rect.Zero, Offset.Zero)
        }

        val availableWidth = (canvasSize.width - 2f * horizontalPadding).coerceAtLeast(0f)
        val slotWidth = availableWidth / itemCount
        val spacing = (slotWidth * 0.18f).coerceIn(2f, 16f)
        val barWidth = (slotWidth - spacing).coerceAtLeast(1f)

        val left = horizontalPadding + index * slotWidth + (spacing / 2f)
        val right = left + barWidth

        val availableHeight = (canvasSize.height - topMargin - bottomMargin).coerceAtLeast(minBarHeight)
        val baseY = canvasSize.height - bottomMargin

        val normalizedValue = if (maxValue > 0) value.toFloat() / maxValue.toFloat() else 0f
        val barHeight = (normalizedValue * (availableHeight * 0.82f) + minBarHeight).coerceIn(minBarHeight, availableHeight)
        val top = baseY - barHeight

        val rect = Rect(left = left, top = top, right = right, bottom = baseY)
        val center = Offset((left + right) / 2f, (top + baseY) / 2f)

        return BarGeometry(index = index, value = value, rect = rect, center = center)
    }

    /**
     * Maps an X coordinate on the canvas to an array bar index in [0, itemCount - 1].
     */
    fun calculateIndexFromX(
        touchX: Float,
        itemCount: Int,
        canvasWidth: Float,
        horizontalPadding: Float
    ): Int {
        if (itemCount <= 0) return 0
        val availableWidth = (canvasWidth - 2f * horizontalPadding).coerceAtLeast(1f)
        val slotWidth = availableWidth / itemCount
        val relativeX = touchX - horizontalPadding
        val rawIndex = (relativeX / slotWidth).toInt()
        return rawIndex.coerceIn(0, itemCount - 1)
    }

    /**
     * Computes the bounding rectangle for a sliding window overlay covering [windowRange].
     */
    fun calculateWindowBounds(
        windowRange: IntRange,
        itemCount: Int,
        canvasSize: Size,
        horizontalPadding: Float,
        topMargin: Float,
        bottomMargin: Float
    ): Rect? {
        if (itemCount <= 0 || canvasSize.width <= 0f) return null
        val start = windowRange.first.coerceIn(0, itemCount - 1)
        val end = windowRange.last.coerceIn(0, itemCount - 1)
        if (start > end) return null

        val availableWidth = (canvasSize.width - 2f * horizontalPadding).coerceAtLeast(0f)
        val slotWidth = availableWidth / itemCount

        val left = (horizontalPadding + start * slotWidth - 4f).coerceAtLeast(0f)
        val right = (horizontalPadding + (end + 1) * slotWidth + 4f).coerceAtMost(canvasSize.width)
        val top = (topMargin - 6f).coerceAtLeast(0f)
        val bottom = (canvasSize.height - bottomMargin + 6f).coerceAtMost(canvasSize.height)

        return Rect(left = left, top = top, right = right, bottom = bottom)
    }

    /**
     * Resolves the canonical design system token color for a bar based on its execution state.
     */
    fun resolveBarColor(
        index: Int,
        activeIndices: Set<Int>,
        mutatingIndices: Set<Int>,
        sortedIndices: Set<Int>,
        discardedRange: IntRange?,
        colors: UnravelColors
    ): Color = when {
        index in mutatingIndices -> colors.coralFlame
        index in activeIndices -> colors.electricVioletBright
        index in sortedIndices -> colors.neonMint
        discardedRange != null && index in discardedRange -> colors.structuralSlateMuted
        else -> colors.structuralSlate
    }
}
