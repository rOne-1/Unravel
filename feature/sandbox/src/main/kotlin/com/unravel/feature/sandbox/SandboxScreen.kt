package com.unravel.feature.sandbox

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unravel.core.canvasrenderer.array.ArrayPointerCanvas
import com.unravel.core.canvasrenderer.graph.SpatialGridGraphCanvas
import com.unravel.core.canvasrenderer.linked.LinkedNodeCanvas
import com.unravel.core.canvasrenderer.stack.CallStackOverlay
import com.unravel.core.canvasrenderer.tree.HierarchyTreeCanvas
import com.unravel.core.designsystem.theme.UnravelTheme
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.VisualStructureState
import com.unravel.feature.sandbox.model.SandboxIntent
import com.unravel.feature.sandbox.model.SandboxMode
import com.unravel.feature.sandbox.model.SandboxUiState
import com.unravel.feature.sandbox.ui.SynchronizedCodeXRaySheet
import com.unravel.feature.sandbox.ui.TimelineScrubberDock
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.collections.immutable.persistentMapOf

/**
 * Top-level Sandbox Screen hosting the dynamic canvas renderer, call stack HUD,
 * top navigation bar with mode switching, synchronized code X-ray bottom sheet,
 * and floating timeline scrubber glass dock.
 */
@Composable
fun SandboxScreen(
    viewModel: SandboxViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lifecycle observer persisting checkpoint to SessionRepository on ON_PAUSE
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.onLifecyclePause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SandboxContent(
        state = state,
        onIntent = viewModel::processIntent,
        modifier = modifier,
        onNavigateBack = onNavigateBack
    )
}

/**
 * Stateless content composable rendering the complete Sandbox experience.
 */
@Composable
fun SandboxContent(
    state: SandboxUiState,
    onIntent: (SandboxIntent) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography
    val hazeState = remember { HazeState() }

    // Rejection pulse visual feedback on invalid challenge gesture
    val borderPulseColor by animateColorAsState(
        targetValue = if (state.rejectionPulse) colors.coralFlame else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "rejectionBorderPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasVoid)
            .border(2.dp, borderPulseColor)
    ) {
        // 1. Dynamic Canvas Layer with Haze Source backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            when (val structure = state.currentFrame?.structureState) {
                is VisualStructureState.ArrayState -> {
                    ArrayPointerCanvas(
                        arrayState = structure,
                        modifier = Modifier.fillMaxSize(),
                        onSwap = { a, b ->
                            if (state.sandboxMode == SandboxMode.CHALLENGE_BE_THE_ALGO) {
                                onIntent(SandboxIntent.SubmitChallengeAction(ExpectedAction.SwapIndices(a, b)))
                            }
                        }
                    )
                }
                is VisualStructureState.LinkedNodeState -> {
                    LinkedNodeCanvas(
                        state = structure,
                        modifier = Modifier.fillMaxSize(),
                        onNodeTap = { nodeId ->
                            if (state.sandboxMode == SandboxMode.CHALLENGE_BE_THE_ALGO) {
                                onIntent(SandboxIntent.SubmitChallengeAction(ExpectedAction.SelectBranch(nodeId)))
                            }
                        }
                    )
                }
                is VisualStructureState.HierarchyTreeState -> {
                    HierarchyTreeCanvas(
                        state = structure,
                        modifier = Modifier.fillMaxSize(),
                        onNodeTap = { nodeId ->
                            if (state.sandboxMode == SandboxMode.CHALLENGE_BE_THE_ALGO) {
                                onIntent(SandboxIntent.SubmitChallengeAction(ExpectedAction.SelectBranch(nodeId)))
                            }
                        }
                    )
                }
                is VisualStructureState.SpatialGridGraphState -> {
                    SpatialGridGraphCanvas(
                        state = structure,
                        modifier = Modifier.fillMaxSize(),
                        onVertexTap = { vertexId ->
                            if (state.sandboxMode == SandboxMode.CHALLENGE_BE_THE_ALGO) {
                                onIntent(SandboxIntent.SubmitChallengeAction(ExpectedAction.ExpandFrontierNode(vertexId)))
                            }
                        }
                    )
                }
                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Initializing sequence frames...",
                            style = typography.bodyMedium,
                            color = colors.textTelemetry
                        )
                    }
                }
            }

            // 2. Recursive Call Stack Floating HUD (when call stack is non-empty)
            val callStack = state.currentFrame?.callStack
            if (!callStack.isNullOrEmpty()) {
                CallStackOverlay(
                    callStack = callStack,
                    maxRecursionDepth = maxOf(1, state.currentFrame.metrics.maxRecursionDepth),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 74.dp)
                )
            }
        }

        // 3. Top HUD: Back Button | Title & Badges | Mode Selector Segmented Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Algorithm Title and Category Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onNavigateBack != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceGlass)
                                .border(1.dp, colors.surfaceGlassBorder, CircleShape)
                                .clickable { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "←", color = colors.textPrimary, fontSize = 16.sp)
                        }
                    }

                    Column {
                        Text(
                            text = state.algorithmId.title,
                            style = typography.titleMedium.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val categoryLabel = state.category.name.replace('_', ' ')
                            Text(
                                text = categoryLabel,
                                style = typography.hudMicroLabel.copy(
                                    color = colors.neonMint,
                                    letterSpacing = 1.sp
                                )
                            )
                            if (state.sandboxMode == SandboxMode.CHALLENGE_BE_THE_ALGO) {
                                Text(
                                    text = "• 🔥 STREAK ${state.challengeStreak}",
                                    style = typography.hudMicroLabel.copy(
                                        color = if (state.challengeStreak > 0) colors.neonMint else colors.coralFlame,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                // Mode Selector Segmented Pill (PLAYBACK, EDIT, CHALLENGE)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceGlass)
                        .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModePill(
                        label = "PLAY",
                        isSelected = state.sandboxMode == SandboxMode.PLAYBACK,
                        onClick = { onIntent(SandboxIntent.SetMode(SandboxMode.PLAYBACK)) }
                    )
                    ModePill(
                        label = "EDIT",
                        isSelected = state.sandboxMode == SandboxMode.DIRECT_EDIT,
                        onClick = { onIntent(SandboxIntent.SetMode(SandboxMode.DIRECT_EDIT)) }
                    )
                    ModePill(
                        label = "CHALLENGE",
                        isSelected = state.sandboxMode == SandboxMode.CHALLENGE_BE_THE_ALGO,
                        onClick = { onIntent(SandboxIntent.SetMode(SandboxMode.CHALLENGE_BE_THE_ALGO)) }
                    )
                }
            }
        }

        // 4. Bottom Controls: Synchronized Code X-Ray Bottom Sheet & Scrubber Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Synchronized Code X-Ray Bottom Sheet
            SynchronizedCodeXRaySheet(
                codeSnippet = state.codeSnippet,
                activeLineIndex = state.currentFrame?.codeSync?.activeLineIndex ?: 0,
                variableBindings = state.currentFrame?.codeSync?.variableBindings ?: persistentMapOf(),
                hazeState = hazeState,
                initiallyExpanded = false
            )

            // Timeline Scrubber Glass Dock
            TimelineScrubberDock(
                currentStep = state.currentFrameIndex,
                totalSteps = state.totalFrames,
                isPlaying = state.isPlaying,
                playbackSpeed = state.playbackSpeed,
                onPlayPauseToggle = { onIntent(SandboxIntent.TogglePlayPause) },
                onStepForward = { onIntent(SandboxIntent.StepForward) },
                onStepBackward = { onIntent(SandboxIntent.StepBackward) },
                onSeekToStep = { step -> onIntent(SandboxIntent.SeekToStep(step)) },
                onSpeedChange = { speed -> onIntent(SandboxIntent.SetSpeed(speed)) },
                hazeState = hazeState
            )
        }
    }
}

@Composable
private fun ModePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.electricVioletBright else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = typography.hudMicroLabel.copy(
                color = if (isSelected) colors.textPrimary else colors.textTelemetry,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}

@Preview(name = "Sandbox Screen - Dark")
@Composable
private fun SandboxScreenPreview() {
    UnravelTheme {
        SandboxContent(
            state = SandboxUiState(
                algorithmId = AlgorithmId.TWO_POINTERS,
                totalFrames = 25,
                currentFrameIndex = 4,
                playbackSpeed = 1.0f,
                sandboxMode = SandboxMode.PLAYBACK,
                codeSnippet = "var left = 0, right = n - 1"
            ),
            onIntent = {}
        )
    }
}
