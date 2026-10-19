package com.unravel.feature.racemode

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unravel.core.designsystem.theme.UnravelTheme
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.SandboxCategory
import com.unravel.feature.racemode.engine.RaceAlgorithmCatalog
import com.unravel.feature.racemode.engine.RaceMatchup
import com.unravel.feature.racemode.model.RaceModeIntent
import com.unravel.feature.racemode.model.RaceModeUiState
import com.unravel.feature.racemode.ui.AdaptiveRaceLayout
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource

/**
 * Stateful entry point route for the Split-Screen Race Mode screen.
 */
@Composable
fun RaceModeRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RaceModeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RaceModeScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * Complete Split-Screen Race Mode screen hosting adaptive dual canvases,
 * top benchmark header with category chips and matchup selector,
 * and bottom floating frosted glass playback controls dock.
 */
@Composable
fun RaceModeScreen(
    uiState: RaceModeUiState,
    onIntent: (RaceModeIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography
    val glass = UnravelTheme.glass
    val hazeState = remember { HazeState() }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasVoid)
    ) {
        val isLandscape = maxWidth > maxHeight

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Benchmark Telemetry & Selector Bar
            RaceModeTopBar(
                category = uiState.category,
                selectedMatchupId = uiState.selectedMatchupId,
                isDiverged = uiState.isDiverged,
                isRaceFinished = uiState.isRaceFinished,
                onCategorySelected = { onIntent(RaceModeIntent.SelectCategory(it)) },
                onMatchupSelected = { onIntent(RaceModeIntent.SelectMatchup(it)) },
                onNavigateBack = onNavigateBack,
                isLandscape = isLandscape,
                topPadding = statusBarPadding.calculateTopPadding(),
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Central Split Canvases with Refraction Divider & Real-Time Delta HUD
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .hazeSource(state = hazeState)
            ) {
                AdaptiveRaceLayout(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. Floating Frosted Glass Playback Controls Dock
            RaceModeFloatingDock(
                isPlaying = uiState.isPlaying,
                playbackSpeed = uiState.playbackSpeed,
                isRaceFinished = uiState.isRaceFinished,
                onTogglePlayPause = { onIntent(RaceModeIntent.TogglePlayPause) },
                onStepForward = { onIntent(RaceModeIntent.StepForward) },
                onStepBackward = { onIntent(RaceModeIntent.StepBackward) },
                onResetRace = { onIntent(RaceModeIntent.ResetRace) },
                onCycleSpeed = {
                    val nextSpeed = when (uiState.playbackSpeed) {
                        0.5f -> 1.0f
                        1.0f -> 2.0f
                        2.0f -> 3.0f
                        else -> 0.5f
                    }
                    onIntent(RaceModeIntent.SetSpeed(nextSpeed))
                },
                hazeState = hazeState,
                bottomPadding = navBarPadding.calculateBottomPadding(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Top bar housing navigation, title telemetry, category selector chips, and matchup options.
 */
@Composable
private fun RaceModeTopBar(
    category: SandboxCategory,
    selectedMatchupId: String,
    isDiverged: Boolean,
    isRaceFinished: Boolean,
    onCategorySelected: (SandboxCategory) -> Unit,
    onMatchupSelected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    isLandscape: Boolean,
    topPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography

    val availableMatchups = remember(category) {
        RaceAlgorithmCatalog.getMatchupsByCategory(category)
    }

    Column(
        modifier = modifier
            .background(colors.canvasVoid)
            .padding(top = topPadding + 6.dp, start = 12.dp, end = 12.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Back Navigation, Brand Title, Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.structuralSlateMuted)
                        .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(8.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onNavigateBack() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "◀ BACK",
                        style = typography.hudMicroLabel.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Title Telemetry
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "UNRAVEL",
                            style = typography.telemetryHero.copy(
                                color = colors.neonMint,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "// RACE MODE",
                            style = typography.hudMicroLabel.copy(
                                color = colors.textTelemetry,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }
                    Text(
                        text = "DUAL-ENGINE BENCHMARK",
                        style = typography.hudMicroLabel.copy(
                            color = colors.textTelemetry,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            // Status Indicator Pill
            when {
                isRaceFinished -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.neonMint)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "FINISHED 🏆",
                            style = typography.hudMicroLabel.copy(
                                color = colors.canvasVoid,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                isDiverged -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.coralFlame)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DIVERGED ⚠️",
                            style = typography.hudMicroLabel.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.structuralSlateMuted)
                            .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SYNCED",
                            style = typography.hudMicroLabel.copy(
                                color = colors.textTelemetry,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // Row 2: Category Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val categories = listOf(
                Pair(SandboxCategory.ARRAY_POINTER, "Sorting & Arrays"),
                Pair(SandboxCategory.LINKED_STACK_QUEUE, "Pointers & Lists"),
                Pair(SandboxCategory.HIERARCHICAL_TREE, "Trees"),
                Pair(SandboxCategory.SPATIAL_GRID_GRAPH, "Graphs")
            )

            for ((cat, label) in categories) {
                val isSelected = cat == category
                val shape = RoundedCornerShape(8.dp)
                val bg = if (isSelected) colors.surfaceGlass else colors.surfaceGlass.copy(alpha = 0.45f)
                val border = if (isSelected) colors.neonMint else colors.surfaceGlassBorder
                val textColor = if (isSelected) colors.neonMint else colors.textTelemetry

                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(bg)
                        .border(1.dp, border, shape)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onCategorySelected(cat) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = typography.hudMicroLabel.copy(
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Row 3: Matchup Options in Selected Category
        if (availableMatchups.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (matchup in availableMatchups) {
                    val isMatchupSelected = matchup.id == selectedMatchupId
                    val shape = RoundedCornerShape(6.dp)
                    val bg = if (isMatchupSelected) colors.structuralSlateMuted else colors.canvasVoid
                    val border = if (isMatchupSelected) colors.electricVioletBright else colors.surfaceGlassBorder
                    val textColor = if (isMatchupSelected) colors.electricVioletBright else colors.textTelemetry

                    Box(
                        modifier = Modifier
                            .clip(shape)
                            .background(bg)
                            .border(1.dp, border, shape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onMatchupSelected(matchup.id) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = matchup.title,
                            style = typography.hudMicroLabel.copy(
                                color = textColor,
                                fontWeight = if (isMatchupSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating frosted glass playback controls dock at screen bottom.
 */
@Composable
private fun RaceModeFloatingDock(
    isPlaying: Boolean,
    playbackSpeed: Float,
    isRaceFinished: Boolean,
    onTogglePlayPause: () -> Unit,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit,
    onResetRace: () -> Unit,
    onCycleSpeed: () -> Unit,
    hazeState: HazeState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography
    val glass = UnravelTheme.glass

    val dockShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = bottomPadding)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .hazeBlur(
                    input = HazeInput.Sources(hazeState),
                    style = glass.toHazeBlurStyle()
                )
                .clip(dockShape)
                .background(colors.surfaceGlass)
                .border(1.dp, colors.surfaceGlassBorder, dockShape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Reset Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.structuralSlateMuted)
                        .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(8.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onResetRace() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↺ RESET",
                        style = typography.hudMicroLabel.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Step Backward Nudger
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.structuralSlateMuted)
                        .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(8.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onStepBackward() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏮ STEP",
                        style = typography.hudMicroLabel.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Play / Pause Toggle Button (Hero Action)
                val playButtonBg by animateColorAsState(
                    targetValue = if (isPlaying) colors.coralFlame else colors.electricVioletBright,
                    animationSpec = tween(durationMillis = 200),
                    label = "playButtonBg"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(playButtonBg)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onTogglePlayPause() }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPlaying) "⏸ PAUSE" else "▶ RACE",
                        style = typography.hudMicroLabel.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }

                // Step Forward Nudger
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.structuralSlateMuted)
                        .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(8.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onStepForward() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "STEP ⏭",
                        style = typography.hudMicroLabel.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Speed Multiplier Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.structuralSlateMuted)
                        .border(1.dp, colors.neonMint.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onCycleSpeed() }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        style = typography.hudMicroLabel.copy(
                            color = colors.neonMint,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Preview(name = "RaceModeScreen - Portrait", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360, heightDp = 640)
@Composable
private fun RaceModeScreenPortraitPreview() {
    UnravelTheme {
        RaceModeScreen(
            uiState = RaceModeUiState(
                algorithmA = AlgorithmId.BUBBLE_SORT,
                algorithmB = AlgorithmId.QUICK_SORT,
                stepIndexA = 4,
                stepIndexB = 12,
                comparisonDelta = 8,
                mutationDelta = -2,
                isDiverged = false
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "RaceModeScreen - Landscape", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 720, heightDp = 360)
@Composable
private fun RaceModeScreenLandscapePreview() {
    UnravelTheme {
        RaceModeScreen(
            uiState = RaceModeUiState(
                algorithmA = AlgorithmId.BUBBLE_SORT,
                algorithmB = AlgorithmId.QUICK_SORT,
                stepIndexA = 10,
                stepIndexB = 25,
                comparisonDelta = 15,
                mutationDelta = 6,
                isDiverged = true,
                isRaceFinished = true,
                winnerAlgorithmId = AlgorithmId.QUICK_SORT
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}
