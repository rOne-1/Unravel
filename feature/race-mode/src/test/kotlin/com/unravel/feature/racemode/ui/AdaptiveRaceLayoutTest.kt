package com.unravel.feature.racemode.ui

import com.unravel.core.engine.model.AlgorithmId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AdaptiveRaceLayoutTest {

    @Test
    @DisplayName("WP-36: formatDeltaMetric formats positive deltas with leading plus")
    fun testFormatPositiveDelta() {
        assertEquals("+10", formatDeltaMetric(10))
        assertEquals("+1", formatDeltaMetric(1))
    }

    @Test
    @DisplayName("WP-36: formatDeltaMetric formats negative and zero deltas without extra plus")
    fun testFormatNegativeAndZeroDelta() {
        assertEquals("-15", formatDeltaMetric(-15))
        assertEquals("0", formatDeltaMetric(0))
    }

    @Test
    @DisplayName("WP-36: resolveRaceStatusBanner returns winner title when race finished")
    fun testStatusBannerFinished() {
        val bannerWithWinner = resolveRaceStatusBanner(
            isRaceFinished = true,
            isDiverged = true,
            winnerAlgorithmId = AlgorithmId.QUICK_SORT
        )
        assertEquals("🏆 QuickSort FINISHED", bannerWithWinner)

        val bannerNoWinner = resolveRaceStatusBanner(
            isRaceFinished = true,
            isDiverged = false,
            winnerAlgorithmId = null
        )
        assertEquals("🏆 RACE FINISHED", bannerNoWinner)
    }

    @Test
    @DisplayName("WP-36: resolveRaceStatusBanner returns divergence banner when diverged and unfinished")
    fun testStatusBannerDiverged() {
        val banner = resolveRaceStatusBanner(
            isRaceFinished = false,
            isDiverged = true,
            winnerAlgorithmId = null
        )
        assertEquals("⚠️ DIVERGENCE DETECTED", banner)
    }

    @Test
    @DisplayName("WP-36: resolveRaceStatusBanner returns synchronized when not diverged and unfinished")
    fun testStatusBannerSynchronized() {
        val banner = resolveRaceStatusBanner(
            isRaceFinished = false,
            isDiverged = false,
            winnerAlgorithmId = null
        )
        assertEquals("SYNCHRONIZED", banner)
    }
}
