package com.unravel.feature.racemode.ui

import com.unravel.core.designsystem.theme.CoralFlame
import com.unravel.core.designsystem.theme.ElectricVioletBright
import com.unravel.core.designsystem.theme.NeonMint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GlassPrismDividerTest {

    @Test
    @DisplayName("WP-35: Prism accent color resolves to NeonMint when race is finished")
    fun testFinishedAccentColor() {
        val color = resolvePrismAccentColor(
            isRaceFinished = true,
            isDiverged = true,
            defaultColor = ElectricVioletBright
        )
        assertEquals(NeonMint, color)
    }

    @Test
    @DisplayName("WP-35: Prism accent color resolves to CoralFlame when divergence is detected")
    fun testDivergenceAccentColor() {
        val color = resolvePrismAccentColor(
            isRaceFinished = false,
            isDiverged = true,
            defaultColor = ElectricVioletBright
        )
        assertEquals(CoralFlame, color)
    }

    @Test
    @DisplayName("WP-35: Prism accent color resolves to default color when synchronized and unfinished")
    fun testDefaultAccentColor() {
        val color = resolvePrismAccentColor(
            isRaceFinished = false,
            isDiverged = false,
            defaultColor = ElectricVioletBright
        )
        assertEquals(ElectricVioletBright, color)
    }
}
