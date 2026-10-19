package com.unravel.feature.skillmap.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unravel.core.database.model.ActiveSessionPreferences
import com.unravel.core.designsystem.theme.UnravelTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * Formats a raw canonical algorithm identifier into a title-cased display label.
 */
fun formatAlgorithmTitle(id: String): String = when (id) {
    "two_pointers" -> "Two Pointers"
    "sliding_window" -> "Sliding Window"
    "binary_search" -> "Binary Search"
    "bubble_sort" -> "Bubble Sort"
    "insertion_sort" -> "Insertion Sort"
    "merge_sort" -> "Merge Sort"
    "quick_sort" -> "Quick Sort"
    "linked_list_reversal" -> "Linked List Reversal"
    "floyd_cycle" -> "Floyd's Cycle Detection"
    "stack_queue" -> "Stack & Queue"
    "bst_ops" -> "BST Operations"
    "avl_rotations" -> "AVL Rotations"
    "graph_bfs_dfs" -> "Graph BFS & DFS"
    "dijkstra_astar" -> "Dijkstra & A* Pathfinding"
    else -> id.split('_').joinToString(" ") { part ->
        part.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }
}

/**
 * Formats elapsed time since [timestampEpochMs] into a concise telemetry label.
 */
fun formatElapsedTime(timestampEpochMs: Long, currentTimeMs: Long = System.currentTimeMillis()): String {
    if (timestampEpochMs <= 0L) return "Paused"
    val diffMs = maxOf(0L, currentTimeMs - timestampEpochMs)
    val seconds = diffMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}

/**
 * Floating frosted glass banner docked at the bottom of the Skill Map screen,
 * displaying the active session state from Proto DataStore and providing 1-tap instant resume.
 */
@Composable
fun JumpBackInBanner(
    session: ActiveSessionPreferences?,
    onResumeClick: (algorithmId: String) -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    totalSteps: Int = 42
) {
    val isVisible = session != null && session.algorithmId.isNotBlank()
    val motion = UnravelTheme.motion
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography
    val glass = UnravelTheme.glass

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = motion.spatialSpring()
        ) + fadeIn(animationSpec = motion.spatialSpring()),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = motion.spatialSpring()
        ) + fadeOut(animationSpec = motion.spatialSpring()),
        modifier = modifier
    ) {
        if (session != null && session.algorithmId.isNotBlank()) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 850, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )

            val dockShape = RoundedCornerShape(20.dp)
            val hazeModifier = if (hazeState != null) {
                Modifier.hazeBlur(
                    input = HazeInput.Sources(hazeState),
                    style = glass.toHazeBlurStyle()
                )
            } else {
                Modifier
            }

            val currentStepText = (session.stepIndex + 1).toString().padStart(3, '0')
            val totalStepsText = maxOf(1, totalSteps).toString().padStart(3, '0')
            val progress = ((session.stepIndex + 1).toFloat() / maxOf(1, totalSteps).toFloat()).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(hazeModifier)
                    .clip(dockShape)
                    .background(colors.surfaceGlass)
                    .border(1.dp, colors.surfaceGlassBorder, dockShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onResumeClick(session.algorithmId)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Pulsing Active indicator + Elapsed time + Dismiss button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(
                                    color = colors.neonMint.copy(alpha = pulseAlpha),
                                    radius = size.minDimension / 2f
                                )
                            }
                            Text(
                                text = "JUMP BACK IN",
                                style = typography.hudMicroLabel.copy(
                                    color = colors.neonMint,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatElapsedTime(session.timestampEpochMs),
                                style = typography.hudMicroLabel.copy(color = colors.textTelemetry)
                            )
                            // Dismiss close button
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colors.structuralSlateMuted)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        onDismissClick()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(9.dp)) {
                                    val strokeW = 1.6.dp.toPx()
                                    drawLine(
                                        color = colors.textTelemetry,
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = strokeW,
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = colors.textTelemetry,
                                        start = Offset(size.width, 0f),
                                        end = Offset(0f, size.height),
                                        strokeWidth = strokeW,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }

                    // Row 2: Algorithm Title + Step Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatAlgorithmTitle(session.algorithmId),
                            style = typography.titleMedium.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        // Step counter badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.structuralSlateMuted)
                                .border(1.dp, colors.structuralSlate, RoundedCornerShape(6.dp))
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
                    }

                    // Row 3: Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.structuralSlateMuted)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.neonMint)
                        )
                    }

                    // Row 4: Speed telemetry + "TAP TO RESUME →"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SPEED ${session.playbackSpeed}x",
                            style = typography.hudMicroLabel.copy(color = colors.textTelemetry)
                        )
                        Text(
                            text = "TAP TO RESUME →",
                            style = typography.hudMicroLabel.copy(
                                color = colors.neonMint,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "JumpBackInBanner Preview", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun JumpBackInBannerPreview() {
    UnravelTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            JumpBackInBanner(
                session = ActiveSessionPreferences(
                    algorithmId = "merge_sort",
                    stepIndex = 13,
                    playbackSpeed = 1.5f,
                    timestampEpochMs = System.currentTimeMillis() - 120_000L
                ),
                onResumeClick = {},
                onDismissClick = {},
                totalSteps = 42
            )
        }
    }
}
