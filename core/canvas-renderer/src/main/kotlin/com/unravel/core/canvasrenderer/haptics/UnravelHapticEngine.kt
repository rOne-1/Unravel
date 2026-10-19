package com.unravel.core.canvasrenderer.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import com.unravel.core.engine.model.HapticCue

/**
 * Interface representing the haptic feedback executor for algorithm visualizations.
 */
interface HapticEngine {
    /**
     * Executes the appropriate hardware haptic effect corresponding to [cue].
     *
     * @return True if haptic feedback was triggered, false if throttled, ignored, or no-op.
     */
    fun performHaptic(cue: HapticCue): Boolean
}

/**
 * High-performance, rate-limited hardware haptic feedback engine wrapping Android view haptics.
 *
 * Implements:
 * - Rate-limiting throttle of [minIntervalMs] (default 16ms, ~60Hz cap) for repetitive ticks.
 * - Priority bypass allowing high-priority cues (SWAP_THUD, SUCCESS_LOCK, ERROR_BUZZ) to override
 *   pending throttled ticks.
 * - Graceful fallback to earlier API constants on legacy Android versions (API < 30).
 */
open class UnravelHapticEngine(
    private val hapticFeedbackPerformer: (Int) -> Boolean = { false },
    private val clock: () -> Long = { System.currentTimeMillis() },
    val minIntervalMs: Long = 16L,
    private val targetSdkInt: Int = resolveSdkInt()
) : HapticEngine {

    constructor(
        view: View?,
        minIntervalMs: Long = 16L
    ) : this(
        hapticFeedbackPerformer = { constant ->
            view?.performHapticFeedback(constant) ?: false
        },
        clock = { System.currentTimeMillis() },
        minIntervalMs = minIntervalMs,
        targetSdkInt = resolveSdkInt()
    )

    private var lastHapticTimestamp: Long = 0L

    override fun performHaptic(cue: HapticCue): Boolean {
        if (cue == HapticCue.NONE) return false

        val constant = mapCueToConstant(cue, targetSdkInt) ?: return false
        val now = clock()
        val isHighPriority = isHighPriority(cue)

        if (!isHighPriority && (now - lastHapticTimestamp < minIntervalMs)) {
            // Drop low-priority tick due to hardware rate limit
            return false
        }

        lastHapticTimestamp = now
        return hapticFeedbackPerformer(constant)
    }

    fun isHighPriority(cue: HapticCue): Boolean = when (cue) {
        HapticCue.SWAP_THUD,
        HapticCue.SUCCESS_LOCK,
        HapticCue.ERROR_BUZZ -> true
        HapticCue.TICK,
        HapticCue.NONE -> false
    }

    fun mapCueToConstant(cue: HapticCue, sdkInt: Int = targetSdkInt): Int? = when (cue) {
        HapticCue.TICK -> HapticFeedbackConstants.CLOCK_TICK
        HapticCue.SWAP_THUD -> HapticFeedbackConstants.CONTEXT_CLICK
        HapticCue.SUCCESS_LOCK -> if (sdkInt >= 30) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        }
        HapticCue.ERROR_BUZZ -> if (sdkInt >= 30) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        HapticCue.NONE -> null
    }

    companion object {
        fun resolveSdkInt(): Int = runCatching { Build.VERSION.SDK_INT }.getOrDefault(34)
    }
}

/**
 * Remembers an [UnravelHapticEngine] bound to the active [LocalView].
 */
@Composable
fun rememberHapticEngine(minIntervalMs: Long = 16L): UnravelHapticEngine {
    val view = LocalView.current
    return remember(view, minIntervalMs) {
        UnravelHapticEngine(view = view, minIntervalMs = minIntervalMs)
    }
}

/**
 * CompositionLocal providing access to the [HapticEngine] across the Compose hierarchy.
 */
val LocalHapticEngine = staticCompositionLocalOf<HapticEngine> {
    object : HapticEngine {
        override fun performHaptic(cue: HapticCue): Boolean = false
    }
}
