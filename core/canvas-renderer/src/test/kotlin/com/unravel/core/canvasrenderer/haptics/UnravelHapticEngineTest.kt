package com.unravel.core.canvasrenderer.haptics

import android.view.HapticFeedbackConstants
import com.unravel.core.engine.model.HapticCue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UnravelHapticEngineTest {

    @Test
    fun `mapCueToConstant maps all cues to correct system constants for modern API`() {
        val engine = UnravelHapticEngine(targetSdkInt = 34)

        assertEquals(HapticFeedbackConstants.CLOCK_TICK, engine.mapCueToConstant(HapticCue.TICK, 34))
        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, engine.mapCueToConstant(HapticCue.SWAP_THUD, 34))
        assertEquals(HapticFeedbackConstants.CONFIRM, engine.mapCueToConstant(HapticCue.SUCCESS_LOCK, 34))
        assertEquals(HapticFeedbackConstants.REJECT, engine.mapCueToConstant(HapticCue.ERROR_BUZZ, 34))
        assertNull(engine.mapCueToConstant(HapticCue.NONE, 34))
    }

    @Test
    fun `mapCueToConstant falls back gracefully on legacy Android API`() {
        val engine = UnravelHapticEngine(targetSdkInt = 29)

        assertEquals(HapticFeedbackConstants.CLOCK_TICK, engine.mapCueToConstant(HapticCue.TICK, 29))
        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, engine.mapCueToConstant(HapticCue.SWAP_THUD, 29))
        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, engine.mapCueToConstant(HapticCue.SUCCESS_LOCK, 29))
        assertEquals(HapticFeedbackConstants.LONG_PRESS, engine.mapCueToConstant(HapticCue.ERROR_BUZZ, 29))
        assertNull(engine.mapCueToConstant(HapticCue.NONE, 29))
    }

    @Test
    fun `isHighPriority correctly classifies cues`() {
        val engine = UnravelHapticEngine()

        assertTrue(engine.isHighPriority(HapticCue.SWAP_THUD))
        assertTrue(engine.isHighPriority(HapticCue.SUCCESS_LOCK))
        assertTrue(engine.isHighPriority(HapticCue.ERROR_BUZZ))

        assertFalse(engine.isHighPriority(HapticCue.TICK))
        assertFalse(engine.isHighPriority(HapticCue.NONE))
    }

    @Test
    fun `rate limiting drops low priority ticks arriving within 16ms interval`() {
        var currentTime = 1000L
        val playedConstants = mutableListOf<Int>()

        val engine = UnravelHapticEngine(
            hapticFeedbackPerformer = { constant ->
                playedConstants.add(constant)
                true
            },
            clock = { currentTime },
            minIntervalMs = 16L
        )

        // First tick at t=1000 should pass
        val result1 = engine.performHaptic(HapticCue.TICK)
        assertTrue(result1)
        assertEquals(1, playedConstants.size)

        // Second tick at t=1005 (<16ms) should be dropped
        currentTime = 1005L
        val result2 = engine.performHaptic(HapticCue.TICK)
        assertFalse(result2)
        assertEquals(1, playedConstants.size)

        // Third tick at t=1015 (<16ms) should still be dropped
        currentTime = 1015L
        val result3 = engine.performHaptic(HapticCue.TICK)
        assertFalse(result3)
        assertEquals(1, playedConstants.size)

        // Fourth tick at t=1016 (==16ms) should pass
        currentTime = 1016L
        val result4 = engine.performHaptic(HapticCue.TICK)
        assertTrue(result4)
        assertEquals(2, playedConstants.size)
    }

    @Test
    fun `high priority cues preempt and bypass the rate limit throttle`() {
        var currentTime = 2000L
        val playedConstants = mutableListOf<Int>()

        val engine = UnravelHapticEngine(
            hapticFeedbackPerformer = { constant ->
                playedConstants.add(constant)
                true
            },
            clock = { currentTime },
            minIntervalMs = 16L,
            targetSdkInt = 34
        )

        // Tick at t=2000
        assertTrue(engine.performHaptic(HapticCue.TICK))
        assertEquals(listOf(HapticFeedbackConstants.CLOCK_TICK), playedConstants)

        // High priority SWAP_THUD at t=2005 (5ms after tick) -> must bypass rate limit!
        currentTime = 2005L
        assertTrue(engine.performHaptic(HapticCue.SWAP_THUD))
        assertEquals(
            listOf(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.CONTEXT_CLICK),
            playedConstants
        )

        // Another high priority SUCCESS_LOCK at t=2010 (5ms after swap) -> must bypass rate limit!
        currentTime = 2010L
        assertTrue(engine.performHaptic(HapticCue.SUCCESS_LOCK))
        assertEquals(
            listOf(
                HapticFeedbackConstants.CLOCK_TICK,
                HapticFeedbackConstants.CONTEXT_CLICK,
                HapticFeedbackConstants.CONFIRM
            ),
            playedConstants
        )

        // Low priority TICK at t=2015 (5ms after SUCCESS_LOCK) -> must be throttled!
        currentTime = 2015L
        assertFalse(engine.performHaptic(HapticCue.TICK))
        assertEquals(3, playedConstants.size)

        // Low priority TICK at t=2026 (16ms after SUCCESS_LOCK) -> must pass!
        currentTime = 2026L
        assertTrue(engine.performHaptic(HapticCue.TICK))
        assertEquals(4, playedConstants.size)
    }

    @Test
    fun `HapticCue NONE is always a no-op`() {
        var performed = false
        val engine = UnravelHapticEngine(
            hapticFeedbackPerformer = {
                performed = true
                true
            }
        )

        val result = engine.performHaptic(HapticCue.NONE)
        assertFalse(result)
        assertFalse(performed)
    }
}
