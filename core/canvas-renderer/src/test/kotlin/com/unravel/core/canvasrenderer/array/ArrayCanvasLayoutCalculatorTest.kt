package com.unravel.core.canvasrenderer.array

import androidx.compose.ui.geometry.Size
import com.unravel.core.designsystem.theme.UnravelColors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArrayCanvasLayoutCalculatorTest {

    private val colors = UnravelColors()
    private val canvasSize = Size(800f, 600f)
    private val horizontalPadding = 16f
    private val topMargin = 50f
    private val bottomMargin = 50f

    @Test
    fun `calculateBarGeometry produces non-overlapping contiguous slots with valid bounds`() {
        val itemCount = 5
        val maxValue = 100
        val values = listOf(20, 40, 60, 80, 100)

        val geometries = values.mapIndexed { index, value ->
            ArrayCanvasLayoutCalculator.calculateBarGeometry(
                index = index,
                value = value,
                maxValue = maxValue,
                itemCount = itemCount,
                canvasSize = canvasSize,
                horizontalPadding = horizontalPadding,
                topMargin = topMargin,
                bottomMargin = bottomMargin
            )
        }

        assertEquals(itemCount, geometries.size)

        for (i in 0 until itemCount) {
            val geom = geometries[i]
            assertEquals(i, geom.index)
            assertEquals(values[i], geom.value)

            // Bar is contained inside horizontal padding
            assertTrue(geom.rect.left >= horizontalPadding)
            assertTrue(geom.rect.right <= canvasSize.width - horizontalPadding)

            // Baseline is exactly canvas height - bottomMargin
            assertEquals(canvasSize.height - bottomMargin, geom.rect.bottom)

            // Top is above bottom
            assertTrue(geom.rect.top < geom.rect.bottom)

            // Center is midpoint of rect
            assertEquals((geom.rect.left + geom.rect.right) / 2f, geom.center.x, 0.001f)
            assertEquals((geom.rect.top + geom.rect.bottom) / 2f, geom.center.y, 0.001f)

            // Check non-overlapping with next bar
            if (i < itemCount - 1) {
                val nextGeom = geometries[i + 1]
                assertTrue(geom.rect.right < nextGeom.rect.left, "Bar $i overlaps with bar ${i + 1}")
            }
        }

        // Higher values produce taller bars (smaller top coordinate)
        for (i in 0 until itemCount - 1) {
            assertTrue(
                geometries[i].rect.top > geometries[i + 1].rect.top,
                "Value ${values[i]} should have top > value ${values[i + 1]}"
            )
        }
    }

    @Test
    fun `calculateIndexFromX maps coordinate to correct bar index`() {
        val itemCount = 10
        val availableWidth = canvasSize.width - 2f * horizontalPadding
        val slotWidth = availableWidth / itemCount

        // Exact center of slot 0
        val touch0 = horizontalPadding + 0.5f * slotWidth
        assertEquals(0, ArrayCanvasLayoutCalculator.calculateIndexFromX(touch0, itemCount, canvasSize.width, horizontalPadding))

        // Center of slot 4
        val touch4 = horizontalPadding + 4.5f * slotWidth
        assertEquals(4, ArrayCanvasLayoutCalculator.calculateIndexFromX(touch4, itemCount, canvasSize.width, horizontalPadding))

        // Center of last slot
        val touchLast = horizontalPadding + 9.5f * slotWidth
        assertEquals(9, ArrayCanvasLayoutCalculator.calculateIndexFromX(touchLast, itemCount, canvasSize.width, horizontalPadding))

        // Left boundary clamp
        assertEquals(0, ArrayCanvasLayoutCalculator.calculateIndexFromX(-50f, itemCount, canvasSize.width, horizontalPadding))

        // Right boundary clamp
        assertEquals(9, ArrayCanvasLayoutCalculator.calculateIndexFromX(1000f, itemCount, canvasSize.width, horizontalPadding))
    }

    @Test
    fun `calculateWindowBounds frames the designated range accurately`() {
        val window = ArrayCanvasLayoutCalculator.calculateWindowBounds(
            windowRange = 1..3,
            itemCount = 6,
            canvasSize = canvasSize,
            horizontalPadding = horizontalPadding,
            topMargin = topMargin,
            bottomMargin = bottomMargin
        )

        assertNotNull(window)
        assertTrue(window!!.left > 0f)
        assertTrue(window.right < canvasSize.width)
        assertTrue(window.top < window.bottom)
    }

    @Test
    fun `calculateWindowBounds returns null for invalid ranges or zero items`() {
        assertNull(
            ArrayCanvasLayoutCalculator.calculateWindowBounds(
                windowRange = 5..2, // Inverted
                itemCount = 6,
                canvasSize = canvasSize,
                horizontalPadding = horizontalPadding,
                topMargin = topMargin,
                bottomMargin = bottomMargin
            )
        )

        assertNull(
            ArrayCanvasLayoutCalculator.calculateWindowBounds(
                windowRange = 0..3,
                itemCount = 0, // Zero items
                canvasSize = canvasSize,
                horizontalPadding = horizontalPadding,
                topMargin = topMargin,
                bottomMargin = bottomMargin
            )
        )
    }

    @Test
    fun `resolveBarColor enforces semantic precedence`() {
        // Mutating takes precedence over all others
        val mutatingColor = ArrayCanvasLayoutCalculator.resolveBarColor(
            index = 2,
            activeIndices = setOf(2),
            mutatingIndices = setOf(2),
            sortedIndices = setOf(2),
            discardedRange = 0..4,
            colors = colors
        )
        assertEquals(colors.coralFlame, mutatingColor)

        // Active takes precedence over sorted and discarded
        val activeColor = ArrayCanvasLayoutCalculator.resolveBarColor(
            index = 3,
            activeIndices = setOf(3),
            mutatingIndices = setOf(),
            sortedIndices = setOf(3),
            discardedRange = 0..4,
            colors = colors
        )
        assertEquals(colors.electricVioletBright, activeColor)

        // Sorted takes precedence over discarded
        val sortedColor = ArrayCanvasLayoutCalculator.resolveBarColor(
            index = 1,
            activeIndices = setOf(),
            mutatingIndices = setOf(),
            sortedIndices = setOf(1),
            discardedRange = 0..4,
            colors = colors
        )
        assertEquals(colors.neonMint, sortedColor)

        // Discarded range
        val discardedColor = ArrayCanvasLayoutCalculator.resolveBarColor(
            index = 4,
            activeIndices = setOf(),
            mutatingIndices = setOf(),
            sortedIndices = setOf(),
            discardedRange = 4..5,
            colors = colors
        )
        assertEquals(colors.structuralSlateMuted, discardedColor)

        // Neutral
        val neutralColor = ArrayCanvasLayoutCalculator.resolveBarColor(
            index = 0,
            activeIndices = setOf(),
            mutatingIndices = setOf(),
            sortedIndices = setOf(),
            discardedRange = null,
            colors = colors
        )
        assertEquals(colors.structuralSlate, neutralColor)
    }
}
