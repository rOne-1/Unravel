package com.unravel.core.canvasrenderer.stack

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unravel.core.designsystem.theme.CanvasVoid
import com.unravel.core.designsystem.theme.CoralFlame
import com.unravel.core.designsystem.theme.ElectricVioletBright
import com.unravel.core.designsystem.theme.NeonMint
import com.unravel.core.designsystem.theme.StructuralSlate
import com.unravel.core.designsystem.theme.StructuralSlateMuted
import com.unravel.core.designsystem.theme.SurfaceGlass
import com.unravel.core.designsystem.theme.SurfaceGlassBorder
import com.unravel.core.designsystem.theme.UnravelMotionConstants
import com.unravel.core.designsystem.theme.UnravelTheme
import com.unravel.core.engine.model.CallStackFrame
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Floating Compose HUD card surface displaying recursive call stack frames
 * with mechanical spring-animated push/pop transitions and telemetry counters.
 */
@Composable
fun CallStackOverlay(
    callStack: ImmutableList<CallStackFrame>,
    maxRecursionDepth: Int,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceGlass)
            .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Title, Telemetry Readout, and Expand/Collapse Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (callStack.isNotEmpty()) colors.electricVioletBright else colors.textTelemetry)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CALL STACK",
                        style = typography.hudMicroLabel.copy(color = colors.textPrimary)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Telemetry Readout: DEPTH: [curr] / MAX: [max]
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.structuralSlateMuted)
                            .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DEPTH: ${callStack.size} / MAX: $maxRecursionDepth",
                            style = typography.hudMicroLabel.copy(
                                color = if (callStack.size >= 8) colors.coralFlame else colors.textTelemetry
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExpanded) "▼" else "▲",
                        style = typography.hudMicroLabel.copy(color = colors.textTelemetry)
                    )
                }
            }

            // Animated Expandable Stack Frames
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(
                    animationSpec = spring(
                        dampingRatio = UnravelMotionConstants.DAMPING_RATIO_CALL_STACK,
                        stiffness = UnravelMotionConstants.STIFFNESS_CALL_STACK
                    )
                ),
                exit = fadeOut() + shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = UnravelMotionConstants.DAMPING_RATIO_CALL_STACK,
                        stiffness = UnravelMotionConstants.STIFFNESS_CALL_STACK
                    )
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    if (callStack.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Stack Empty (Idle)",
                                style = typography.hudMicroLabel.copy(color = colors.textTelemetry)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Render top of stack first
                            itemsIndexed(
                                items = callStack.reversed(),
                                key = { _, frame -> frame.id.toString() }
                            ) { index, frame ->
                                val isActiveFrame = index == 0
                                CallStackFrameItem(
                                    frame = frame,
                                    isActive = isActiveFrame
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CallStackFrameItem(
    frame: CallStackFrame,
    isActive: Boolean
) {
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography

    val borderColor = if (isActive) colors.electricVioletBright else colors.surfaceGlassBorder
    val bgColor = if (isActive) colors.structuralSlate else colors.structuralSlateMuted

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Function Signature: func(params)
                val paramsStr = frame.parameters.entries.joinToString(separator = ", ") { "${it.key}=${it.value}" }
                val signature = "${frame.functionName}($paramsStr)"

                Text(
                    text = signature,
                    style = typography.codeXRayBody.copy(
                        color = if (isActive) colors.textPrimary else colors.textTelemetry
                    )
                )

                if (isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.neonMint)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = typography.hudMicroLabel.copy(color = colors.canvasVoid)
                        )
                    }
                }
            }

            // Variable bindings (if any)
            if (frame.variables.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for ((varName, varVal) in frame.variables) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.surfaceGlass)
                                .border(0.5.dp, colors.surfaceGlassBorder, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$varName = $varVal",
                                style = typography.hudMicroLabel.copy(color = colors.textTelemetry)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Preview(name = "Call Stack Overlay - Dark", widthDp = 360)
@Composable
fun PreviewCallStackOverlay() {
    val frames = persistentListOf(
        CallStackFrame(
            id = Uuid.random(),
            functionName = "quickSort",
            parameters = persistentMapOf("low" to "0", "high" to "7"),
            variables = persistentMapOf("pivot" to "4", "i" to "2"),
            depth = 1
        ),
        CallStackFrame(
            id = Uuid.random(),
            functionName = "quickSort",
            parameters = persistentMapOf("low" to "0", "high" to "3"),
            variables = persistentMapOf("pivot" to "2"),
            depth = 2
        )
    )

    UnravelTheme {
        Box(
            modifier = Modifier
                .background(CanvasVoid)
                .padding(16.dp)
        ) {
            CallStackOverlay(
                callStack = frames,
                maxRecursionDepth = 4,
                initiallyExpanded = true
            )
        }
    }
}
