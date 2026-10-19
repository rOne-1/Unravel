package com.unravel.feature.racemode.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unravel.core.canvasrenderer.array.ArrayPointerCanvas
import com.unravel.core.canvasrenderer.graph.SpatialGridGraphCanvas
import com.unravel.core.canvasrenderer.linked.LinkedNodeCanvas
import com.unravel.core.canvasrenderer.tree.HierarchyTreeCanvas
import com.unravel.core.designsystem.theme.CoralFlame
import com.unravel.core.designsystem.theme.ElectricVioletBright
import com.unravel.core.designsystem.theme.NeonMint
import com.unravel.core.designsystem.theme.TextTelemetry
import com.unravel.core.designsystem.theme.UnravelTheme
import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.SandboxCategory
import com.unravel.core.engine.model.VisualStructureState
import com.unravel.feature.racemode.model.RaceModeUiState

/**
 * Formats an integer delta with a leading explicit sign (+/-).
 */
fun formatDeltaMetric(delta: Int): String = when {
    delta > 0 -> "+$delta"
    else -> "$delta"
}

/**
 * Resolves the real-time status banner description for the Race Mode telemetry bar.
 */
fun resolveRaceStatusBanner(
    isRaceFinished: Boolean,
    isDiverged: Boolean,
    winnerAlgorithmId: AlgorithmId?
): String = when {
    isRaceFinished -> "🏆 ${winnerAlgorithmId?.title ?: "RACE"} FINISHED"
    isDiverged -> "⚠️ DIVERGENCE DETECTED"
    else -> "SYNCHRONIZED"
}

/**
 * Adaptive split-screen layout dynamically switching between:
 * - Portrait (Compact): Vertical split (Top Canvas A / Bottom Canvas B) separated by Horizontal Glass Prism.
 * - Landscape / Foldable (Expanded): Horizontal split (Left Canvas A / Right Canvas B) separated by Vertical Glass Prism.
 */
@Composable
fun AdaptiveRaceLayout(
    uiState: RaceModeUiState,
    modifier: Modifier = Modifier
) {
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val orientation = if (isLandscape) DividerOrientation.VERTICAL else DividerOrientation.HORIZONTAL

        val statusBannerText = resolveRaceStatusBanner(
            isRaceFinished = uiState.isRaceFinished,
            isDiverged = uiState.isDiverged,
            winnerAlgorithmId = uiState.winnerAlgorithmId
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLandscape) {
                // Landscape / Expanded: Left Engine A | Vertical Divider | Right Engine B
                Row(modifier = Modifier.fillMaxSize()) {
                    EngineCanvasContainer(
                        algorithmId = uiState.algorithmA,
                        category = uiState.category,
                        frame = uiState.currentFrameA,
                        stepIndex = uiState.stepIndexA,
                        totalSteps = uiState.totalStepsA,
                        isWinner = uiState.winnerAlgorithmId == uiState.algorithmA,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    GlassPrismDivider(
                        orientation = DividerOrientation.VERTICAL,
                        isDiverged = uiState.isDiverged,
                        isRaceFinished = uiState.isRaceFinished
                    )

                    EngineCanvasContainer(
                        algorithmId = uiState.algorithmB,
                        category = uiState.category,
                        frame = uiState.currentFrameB,
                        stepIndex = uiState.stepIndexB,
                        totalSteps = uiState.totalStepsB,
                        isWinner = uiState.winnerAlgorithmId == uiState.algorithmB,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                // Portrait: Top Engine A / Horizontal Divider / Bottom Engine B
                Column(modifier = Modifier.fillMaxSize()) {
                    EngineCanvasContainer(
                        algorithmId = uiState.algorithmA,
                        category = uiState.category,
                        frame = uiState.currentFrameA,
                        stepIndex = uiState.stepIndexA,
                        totalSteps = uiState.totalStepsA,
                        isWinner = uiState.winnerAlgorithmId == uiState.algorithmA,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

                    GlassPrismDivider(
                        orientation = DividerOrientation.HORIZONTAL,
                        isDiverged = uiState.isDiverged,
                        isRaceFinished = uiState.isRaceFinished
                    )

                    EngineCanvasContainer(
                        algorithmId = uiState.algorithmB,
                        category = uiState.category,
                        frame = uiState.currentFrameB,
                        stepIndex = uiState.stepIndexB,
                        totalSteps = uiState.totalStepsB,
                        isWinner = uiState.winnerAlgorithmId == uiState.algorithmB,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }

            // Floating Central Delta HUD Badges
            RealTimeDeltaHud(
                comparisonDelta = uiState.comparisonDelta,
                mutationDelta = uiState.mutationDelta,
                statusText = statusBannerText,
                isDiverged = uiState.isDiverged,
                isRaceFinished = uiState.isRaceFinished,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp)
            )
        }
    }
}

/**
 * Individual algorithm canvas pane with header telemetry, metrics HUD, and winner border glow.
 */
@Composable
private fun EngineCanvasContainer(
    algorithmId: AlgorithmId,
    category: SandboxCategory,
    frame: AlgorithmFrame?,
    stepIndex: Int,
    totalSteps: Int,
    isWinner: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography

    val infiniteTransition = rememberInfiniteTransition(label = "winnerPulse")
    val winnerPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "winnerPulseAlpha"
    )

    val winnerBorderModifier = if (isWinner) {
        Modifier.border(2.5.dp, colors.neonMint.copy(alpha = winnerPulseAlpha))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .background(colors.canvasVoid)
            .then(winnerBorderModifier)
    ) {
        // 1. Render Underlying Domain Canvas
        AlgorithmCanvasRenderer(
            category = category,
            frame = frame,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Engine Title & Complexity Badge (Top-Left)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = algorithmId.title.uppercase(),
                    style = typography.titleMedium.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )

                if (isWinner) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.neonMint)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "WINNER 🏆",
                            style = typography.hudMicroLabel.copy(
                                color = colors.canvasVoid,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${algorithmId.timeComplexityBest} · ${algorithmId.spaceComplexity}",
                style = typography.hudMicroLabel.copy(
                    color = colors.textTelemetry,
                    fontSize = 10.sp
                )
            )
        }

        // 3. Step Counter & Operation Telemetry HUD (Top-Right)
        val stepText = (stepIndex + 1).toString().padStart(3, '0')
        val totalText = maxOf(1, totalSteps).toString().padStart(3, '0')

        val comparisons = frame?.metrics?.comparisons ?: 0
        val mutations = frame?.metrics?.mutations ?: 0

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.structuralSlateMuted)
                    .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "STEP $stepText/$totalText",
                    style = typography.hudMicroLabel.copy(
                        color = colors.neonMint,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CMP: $comparisons",
                    style = typography.hudMicroLabel.copy(
                        color = colors.textTelemetry,
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = "MUT: $mutations",
                    style = typography.hudMicroLabel.copy(
                        color = colors.textTelemetry,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

/**
 * Dispatches frame visual structures to the correct canvas renderer.
 */
@Composable
private fun AlgorithmCanvasRenderer(
    category: SandboxCategory,
    frame: AlgorithmFrame?,
    modifier: Modifier = Modifier
) {
    val structure = frame?.structureState
    when {
        category == SandboxCategory.ARRAY_POINTER && structure is VisualStructureState.ArrayState -> {
            ArrayPointerCanvas(arrayState = structure, modifier = modifier)
        }
        category == SandboxCategory.LINKED_STACK_QUEUE && structure is VisualStructureState.LinkedNodeState -> {
            LinkedNodeCanvas(state = structure, modifier = modifier)
        }
        category == SandboxCategory.HIERARCHICAL_TREE && structure is VisualStructureState.HierarchyTreeState -> {
            HierarchyTreeCanvas(state = structure, modifier = modifier)
        }
        category == SandboxCategory.SPATIAL_GRID_GRAPH && structure is VisualStructureState.SpatialGridGraphState -> {
            SpatialGridGraphCanvas(state = structure, modifier = modifier)
        }
        else -> {
            Box(modifier = modifier)
        }
    }
}

/**
 * Floating real-time delta badges comparing Engine A and Engine B.
 */
@Composable
private fun RealTimeDeltaHud(
    comparisonDelta: Int,
    mutationDelta: Int,
    statusText: String,
    isDiverged: Boolean,
    isRaceFinished: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography

    val accentColor = when {
        isRaceFinished -> colors.neonMint
        isDiverged -> colors.coralFlame
        else -> colors.electricVioletBright
    }

    val hudShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(hudShape)
            .background(colors.surfaceGlass.copy(alpha = 0.92f))
            .border(1.dp, accentColor, hudShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Delta Comparisons Badge
            val cmpColor = when {
                comparisonDelta < 0 -> colors.electricVioletBright
                comparisonDelta > 0 -> colors.coralFlame
                else -> colors.textTelemetry
            }
            Text(
                text = "Δ CMP: ${formatDeltaMetric(comparisonDelta)}",
                style = typography.hudMicroLabel.copy(
                    color = cmpColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )

            Text(
                text = "•",
                style = typography.hudMicroLabel.copy(color = colors.textTelemetry)
            )

            // Delta Mutations Badge
            val mutColor = when {
                mutationDelta < 0 -> colors.electricVioletBright
                mutationDelta > 0 -> colors.coralFlame
                else -> colors.textTelemetry
            }
            Text(
                text = "Δ MUT: ${formatDeltaMetric(mutationDelta)}",
                style = typography.hudMicroLabel.copy(
                    color = mutColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )

            Text(
                text = "•",
                style = typography.hudMicroLabel.copy(color = colors.textTelemetry)
            )

            // Status Description
            Text(
                text = statusText,
                style = typography.hudMicroLabel.copy(
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Preview(name = "AdaptiveRaceLayout - Portrait", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360, heightDp = 640)
@Composable
private fun AdaptiveRaceLayoutPortraitPreview() {
    UnravelTheme {
        AdaptiveRaceLayout(
            uiState = RaceModeUiState(
                algorithmA = AlgorithmId.BUBBLE_SORT,
                algorithmB = AlgorithmId.QUICK_SORT,
                stepIndexA = 5,
                stepIndexB = 8,
                comparisonDelta = 12,
                mutationDelta = -4,
                isDiverged = true
            )
        )
    }
}

@Preview(name = "AdaptiveRaceLayout - Landscape", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 720, heightDp = 360)
@Composable
private fun AdaptiveRaceLayoutLandscapePreview() {
    UnravelTheme {
        AdaptiveRaceLayout(
            uiState = RaceModeUiState(
                algorithmA = AlgorithmId.BUBBLE_SORT,
                algorithmB = AlgorithmId.QUICK_SORT,
                stepIndexA = 10,
                stepIndexB = 22,
                comparisonDelta = -15,
                mutationDelta = 8,
                isDiverged = true,
                isRaceFinished = true,
                winnerAlgorithmId = AlgorithmId.QUICK_SORT
            )
        )
    }
}
