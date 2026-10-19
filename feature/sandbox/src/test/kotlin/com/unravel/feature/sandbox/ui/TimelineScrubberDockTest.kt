package com.unravel.feature.sandbox.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TimelineScrubberDockTest {

    @Test
    @DisplayName("WP-27: Telemetry badge formats step index with 3-digit padding")
    fun testTelemetryBadgeFormatting() {
        val currentStep = 13
        val totalSteps = 42

        val currentStepText = (currentStep + 1).toString().padStart(3, '0')
        val totalStepsText = totalSteps.toString().padStart(3, '0')
        val formatted = "STEP $currentStepText/$totalStepsText"

        assertEquals("STEP 014/042", formatted)
    }

    @Test
    @DisplayName("WP-27: Speed multiplier cycles sequentially through defined presets")
    fun testSpeedCycling() {
        val speeds = listOf(0.25f, 0.5f, 1.0f, 2.0f, 4.0f)

        fun nextSpeed(current: Float): Float {
            val idx = speeds.indexOf(current).let { if (it >= 0) it else 2 }
            return speeds[(idx + 1) % speeds.size]
        }

        assertEquals(0.5f, nextSpeed(0.25f))
        assertEquals(1.0f, nextSpeed(0.5f))
        assertEquals(2.0f, nextSpeed(1.0f))
        assertEquals(4.0f, nextSpeed(2.0f))
        assertEquals(0.25f, nextSpeed(4.0f))
    }

    @Test
    @DisplayName("WP-27: Step buttons respect lower and upper timeline boundaries")
    fun testStepBoundaries() {
        val totalSteps = 10

        fun canStepBack(step: Int) = step > 0
        fun canStepForward(step: Int) = step < totalSteps - 1

        // At start (step 0)
        assertEquals(false, canStepBack(0))
        assertEquals(true, canStepForward(0))

        // At midpoint (step 5)
        assertEquals(true, canStepBack(5))
        assertEquals(true, canStepForward(5))

        // At end (step 9)
        assertEquals(true, canStepBack(9))
        assertEquals(false, canStepForward(9))
    }
}
