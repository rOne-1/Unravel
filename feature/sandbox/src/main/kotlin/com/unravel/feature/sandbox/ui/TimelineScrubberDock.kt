package com.unravel.feature.sandbox.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unravel.core.canvasrenderer.haptics.LocalHapticEngine
import com.unravel.core.designsystem.theme.UnravelTheme
import com.unravel.core.engine.model.HapticCue
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur
import kotlin.math.roundToInt

/**
 * Floating frosted-glass bottom dock providing bidirectional scrubbing,
 * play/pause spring button, step nudgers, monospace telemetry, and speed cycling.
 */
@Composable
fun TimelineScrubberDock(
    currentStep: Int,
    totalSteps: Int,
    isPlaying: Boolean,
    playbackSpeed: Float,
    onPlayPauseToggle: () -> Unit,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit,
    onSeekToStep: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val hapticEngine = LocalHapticEngine.current
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography
    val glass = UnravelTheme.glass

    var lastReportedStep by remember { mutableIntStateOf(currentStep) }

    val dockShape = RoundedCornerShape(24.dp)
    val hazeModifier = if (hazeState != null) {
        Modifier.hazeBlur(input = HazeInput.Sources(hazeState), style = glass.toHazeBlurStyle())
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(hazeModifier)
            .clip(dockShape)
            .background(colors.surfaceGlass)
            .border(1.dp, colors.surfaceGlassBorder, dockShape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Timeline Scrubber Slider
            val maxRange = maxOf(1f, (totalSteps - 1).toFloat())
            val sliderValue = currentStep.toFloat().coerceIn(0f, maxRange)
            val sliderSteps = maxOf(0, totalSteps - 2)

            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    val rounded = newValue.roundToInt().coerceIn(0, totalSteps - 1)
                    if (rounded != lastReportedStep) {
                        hapticEngine.performHaptic(HapticCue.TICK)
                        lastReportedStep = rounded
                        onSeekToStep(rounded)
                    }
                },
                valueRange = 0f..maxRange,
                steps = sliderSteps,
                colors = SliderDefaults.colors(
                    thumbColor = colors.neonMint,
                    activeTrackColor = colors.neonMint,
                    inactiveTrackColor = colors.structuralSlate
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Control Row: Telemetry Badge | Playback Controls | Speed Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // A. Monospace Telemetry Badge
                val currentStepText = (currentStep + 1).toString().padStart(3, '0')
                val totalStepsText = maxOf(1, totalSteps).toString().padStart(3, '0')
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.structuralSlateMuted)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "STEP $currentStepText/$totalStepsText",
                        style = typography.hudMicroLabel.copy(
                            color = colors.neonMint,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // B. Center Transport Controls: Backward | Play/Pause | Forward
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val canStepBack = currentStep > 0
                    val canStepForward = currentStep < totalSteps - 1

                    // Step Backward (-1)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (canStepBack) colors.structuralSlate else colors.structuralSlateMuted)
                            .clickable(
                                enabled = canStepBack,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                hapticEngine.performHaptic(HapticCue.TICK)
                                onStepBackward()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val iconColor = if (canStepBack) colors.textPrimary else colors.textTelemetry
                            // Vertical bar + left-pointing triangle
                            drawRect(
                                color = iconColor,
                                topLeft = Offset(0f, 2.dp.toPx()),
                                size = Size(2.5.dp.toPx(), size.height - 4.dp.toPx())
                            )
                            val triangle = Path().apply {
                                moveTo(size.width, 2.dp.toPx())
                                lineTo(4.dp.toPx(), size.height / 2f)
                                lineTo(size.width, size.height - 2.dp.toPx())
                                close()
                            }
                            drawPath(triangle, color = iconColor, style = Fill)
                        }
                    }

                    // Play / Pause Button with Spring Animation
                    val playButtonScale by animateFloatAsState(
                        targetValue = if (isPlaying) 1.08f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
                        label = "playPauseScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .scale(playButtonScale)
                            .clip(CircleShape)
                            .background(colors.neonMint)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                hapticEngine.performHaptic(HapticCue.TICK)
                                onPlayPauseToggle()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            val iconColor = colors.canvasVoid
                            if (isPlaying) {
                                // Pause icon: two vertical bars
                                val barWidth = 4.dp.toPx()
                                val barHeight = size.height - 2.dp.toPx()
                                drawRect(
                                    color = iconColor,
                                    topLeft = Offset(3.dp.toPx(), 1.dp.toPx()),
                                    size = Size(barWidth, barHeight)
                                )
                                drawRect(
                                    color = iconColor,
                                    topLeft = Offset(size.width - 3.dp.toPx() - barWidth, 1.dp.toPx()),
                                    size = Size(barWidth, barHeight)
                                )
                            } else {
                                // Play icon: right-pointing triangle
                                val playPath = Path().apply {
                                    moveTo(3.dp.toPx(), 1.dp.toPx())
                                    lineTo(size.width - 1.dp.toPx(), size.height / 2f)
                                    lineTo(3.dp.toPx(), size.height - 1.dp.toPx())
                                    close()
                                }
                                drawPath(playPath, color = iconColor, style = Fill)
                            }
                        }
                    }

                    // Step Forward (+1)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (canStepForward) colors.structuralSlate else colors.structuralSlateMuted)
                            .clickable(
                                enabled = canStepForward,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                hapticEngine.performHaptic(HapticCue.TICK)
                                onStepForward()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val iconColor = if (canStepForward) colors.textPrimary else colors.textTelemetry
                            // Right-pointing triangle + vertical bar
                            val triangle = Path().apply {
                                moveTo(0f, 2.dp.toPx())
                                lineTo(size.width - 4.dp.toPx(), size.height / 2f)
                                lineTo(0f, size.height - 2.dp.toPx())
                                close()
                            }
                            drawPath(triangle, color = iconColor, style = Fill)
                            drawRect(
                                color = iconColor,
                                topLeft = Offset(size.width - 2.5.dp.toPx(), 2.dp.toPx()),
                                size = Size(2.5.dp.toPx(), size.height - 4.dp.toPx())
                            )
                        }
                    }
                }

                // C. Speed Selector Pill (cycles: 0.25x -> 0.5x -> 1.0x -> 2.0x -> 4.0x)
                val speeds = listOf(0.25f, 0.5f, 1.0f, 2.0f, 4.0f)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.structuralSlate)
                        .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(12.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            hapticEngine.performHaptic(HapticCue.TICK)
                            val currentIndex = speeds.indexOf(playbackSpeed).let { if (it >= 0) it else 2 }
                            val nextSpeed = speeds[(currentIndex + 1) % speeds.size]
                            onSpeedChange(nextSpeed)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        style = typography.hudMicroLabel.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Preview(name = "Timeline Scrubber Dock - Dark")
@Composable
private fun TimelineScrubberDockPreview() {
    UnravelTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(UnravelTheme.colors.canvasVoid)
                .padding(16.dp)
        ) {
            TimelineScrubberDock(
                currentStep = 13,
                totalSteps = 42,
                isPlaying = false,
                playbackSpeed = 1.0f,
                onPlayPauseToggle = {},
                onStepForward = {},
                onStepBackward = {},
                onSeekToStep = {},
                onSpeedChange = {}
            )
        }
    }
}
