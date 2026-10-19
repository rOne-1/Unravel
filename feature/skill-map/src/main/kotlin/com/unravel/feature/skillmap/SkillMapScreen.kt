package com.unravel.feature.skillmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.unravel.core.database.model.ActiveSessionPreferences
import com.unravel.core.designsystem.theme.UnravelTheme
import com.unravel.feature.skillmap.layout.SkillMapLayoutCoordinates
import com.unravel.feature.skillmap.model.ClusterBounds
import com.unravel.feature.skillmap.model.MasteryState
import com.unravel.feature.skillmap.model.SkillMapIntent
import com.unravel.feature.skillmap.model.SkillMapUiState
import com.unravel.feature.skillmap.model.SkillNodeUiModel
import com.unravel.feature.skillmap.ui.JumpBackInBanner
import com.unravel.feature.skillmap.ui.SkillMapCanvas
import com.unravel.feature.skillmap.ui.SkillMapCanvasState
import com.unravel.feature.skillmap.ui.rememberSkillMapCanvasState
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import kotlinx.collections.immutable.persistentListOf

/**
 * Stateful entry point route for the Skill Map screen.
 */
@Composable
fun SkillMapRoute(
    onNavigateToSandbox: (algorithmId: String, resumeSession: Boolean) -> Unit,
    onNavigateToRaceMode: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SkillMapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SkillMapScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateToSandbox = onNavigateToSandbox,
        onNavigateToRaceMode = onNavigateToRaceMode,
        modifier = modifier
    )
}

/**
 * Complete interactive DAG Skill Map screen displaying the 2D pan/zoom canvas,
 * top brand HUD telemetry, quick-jump cluster navigation pill strip, and
 * docked "Jump Back In" frosted glass banner.
 */
@Composable
fun SkillMapScreen(
    uiState: SkillMapUiState,
    onIntent: (SkillMapIntent) -> Unit,
    onNavigateToSandbox: (algorithmId: String, resumeSession: Boolean) -> Unit,
    onNavigateToRaceMode: () -> Unit,
    modifier: Modifier = Modifier,
    canvasState: SkillMapCanvasState = rememberSkillMapCanvasState()
) {
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography
    val glass = UnravelTheme.glass
    val hazeState = remember { HazeState() }

    var selectedClusterIndex by remember { mutableIntStateOf(1) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasVoid)
    ) {
        // 1. Interactive 2D DAG Canvas Background (Haze Source)
        SkillMapCanvas(
            state = uiState,
            canvasState = canvasState,
            onNodeClick = { node ->
                onIntent(SkillMapIntent.SelectNode(node.id))
                if (node.isUnlocked) {
                    onNavigateToSandbox(node.id, false)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        )

        // 2. Top HUD Overlay: Brand Title, Mastery Ratio, Race Mode Quick-Launch
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding.calculateTopPadding())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val hudShape = RoundedCornerShape(18.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = glass.toHazeBlurStyle()
                    )
                    .clip(hudShape)
                    .background(colors.surfaceGlass)
                    .border(1.dp, colors.surfaceGlassBorder, hudShape)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Brand & Curriculum Completion Telemetry
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "UNRAVEL",
                                style = typography.telemetryHero.copy(
                                    color = colors.neonMint,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "// DAG MAP",
                                style = typography.hudMicroLabel.copy(
                                    color = colors.textTelemetry,
                                    letterSpacing = 1.2.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${uiState.masteredCount}/${uiState.totalNodes} NODES MASTERED",
                            style = typography.hudMicroLabel.copy(
                                color = colors.neonMint,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // Race Mode Quick-Launch Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.structuralSlateMuted)
                            .border(1.dp, colors.electricVioletBright, RoundedCornerShape(12.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                onNavigateToRaceMode()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⚡",
                                fontSize = 12.sp
                            )
                            Text(
                                text = "RACE MODE",
                                style = typography.hudMicroLabel.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Quick-Jump Cluster Pill Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (cluster in uiState.clusterBounds) {
                    val isClusterSelected = cluster.clusterIndex == selectedClusterIndex
                    val pillShape = RoundedCornerShape(10.dp)
                    val pillBg = if (isClusterSelected) colors.surfaceGlass else colors.surfaceGlass.copy(alpha = 0.5f)
                    val pillBorder = if (isClusterSelected) colors.neonMint else colors.surfaceGlassBorder

                    Box(
                        modifier = Modifier
                            .clip(pillShape)
                            .background(pillBg)
                            .border(1.dp, pillBorder, pillShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                selectedClusterIndex = cluster.clusterIndex
                                canvasState.animateToCluster(cluster)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "0${cluster.clusterIndex} · ${cluster.title}",
                            style = typography.hudMicroLabel.copy(
                                color = if (isClusterSelected) colors.neonMint else colors.textTelemetry,
                                fontWeight = if (isClusterSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // 4. Docked "Jump Back In" Floating Frosted Glass Banner
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = navBarPadding.calculateBottomPadding() + 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            JumpBackInBanner(
                session = uiState.activeSession,
                onResumeClick = { algorithmId ->
                    onNavigateToSandbox(algorithmId, true)
                },
                onDismissClick = {
                    onIntent(SkillMapIntent.ClearActiveSession)
                },
                hazeState = hazeState
            )
        }
    }
}

@Preview(name = "SkillMapScreen Dark Mode", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun SkillMapScreenPreview() {
    UnravelTheme {
        val sampleNodes = persistentListOf(
            SkillNodeUiModel(
                id = "two_pointers",
                title = "Two Pointers",
                category = "ARRAY",
                clusterIndex = 1,
                x = 300f,
                y = 350f,
                prerequisiteIds = persistentListOf(),
                isUnlocked = true,
                masteryState = MasteryState.MASTERED
            ),
            SkillNodeUiModel(
                id = "sliding_window",
                title = "Sliding Window",
                category = "ARRAY",
                clusterIndex = 1,
                x = 650f,
                y = 250f,
                prerequisiteIds = persistentListOf("two_pointers"),
                isUnlocked = true,
                masteryState = MasteryState.TINKERED
            ),
            SkillNodeUiModel(
                id = "binary_search",
                title = "Binary Search",
                category = "ARRAY",
                clusterIndex = 1,
                x = 650f,
                y = 480f,
                prerequisiteIds = persistentListOf("two_pointers"),
                isUnlocked = false,
                masteryState = MasteryState.UNEXPLORED
            )
        )

        SkillMapScreen(
            uiState = SkillMapUiState(
                nodes = sampleNodes,
                clusterBounds = SkillMapLayoutCoordinates.CLUSTER_BOUNDS,
                totalNodes = 14,
                masteredCount = 1,
                unlockedCount = 2,
                activeSession = ActiveSessionPreferences(
                    algorithmId = "sliding_window",
                    stepIndex = 7,
                    playbackSpeed = 1.0f,
                    timestampEpochMs = System.currentTimeMillis() - 300_000L
                )
            ),
            onIntent = {},
            onNavigateToSandbox = { _, _ -> },
            onNavigateToRaceMode = {}
        )
    }
}
