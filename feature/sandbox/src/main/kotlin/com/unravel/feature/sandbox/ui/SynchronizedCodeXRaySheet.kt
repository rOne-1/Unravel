package com.unravel.feature.sandbox.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unravel.core.designsystem.theme.UnravelTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlin.math.max
import kotlin.math.min

/**
 * Collapsible frosted glass bottom sheet integrating Snipme Highlights syntax coloring,
 * active line indicators with left neon accent bars, and inline live variable watch badges.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SynchronizedCodeXRaySheet(
    codeSnippet: String,
    activeLineIndex: Int,
    variableBindings: ImmutableMap<String, String>,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    hazeState: HazeState? = null
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography
    val glass = UnravelTheme.glass
    val listState = rememberLazyListState()

    // 1. Token Caching: Highlights parsed once per algorithm code snippet
    val lineAnnotatedStrings = remember(codeSnippet) {
        if (codeSnippet.isBlank()) {
            emptyList()
        } else {
            val highlights = Highlights.Builder()
                .code(codeSnippet)
                .language(SyntaxLanguage.KOTLIN)
                .theme(SyntaxThemes.monokai(true))
                .build()
            val codeHighlights = highlights.getHighlights()

            val lines = codeSnippet.lines()
            var currentOffset = 0
            lines.map { lineText ->
                val lineStart = currentOffset
                val lineEnd = lineStart + lineText.length
                currentOffset = lineEnd + 1 // newline offset

                buildAnnotatedString {
                    append(lineText)
                    for (h in codeHighlights) {
                        if (h is ColorHighlight) {
                            val hStart = h.location.start
                            val hEnd = h.location.end
                            val overlapStart = max(lineStart, hStart)
                            val overlapEnd = min(lineEnd, hEnd)
                            if (overlapStart < overlapEnd) {
                                addStyle(
                                    SpanStyle(color = Color(h.rgb)),
                                    start = overlapStart - lineStart,
                                    end = overlapEnd - lineStart
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Synchronous Auto-Scroll: Shifts viewport when activeLineIndex updates
    LaunchedEffect(activeLineIndex) {
        if (isExpanded && activeLineIndex > 0 && activeLineIndex <= lineAnnotatedStrings.size) {
            val targetScroll = (activeLineIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    val sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val hazeModifier = if (hazeState != null) {
        Modifier.hazeBlur(input = HazeInput.Sources(hazeState), style = glass.toHazeBlurStyle())
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(hazeModifier)
            .clip(sheetShape)
            .background(colors.surfaceGlass)
            .border(1.dp, colors.surfaceGlassBorder, sheetShape)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Title, Telemetry, and Collapse Toggle Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.electricVioletBright)
                    )
                    Text(
                        text = "CODE X-RAY",
                        style = typography.hudMicroLabel.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    )
                    if (activeLineIndex > 0) {
                        Text(
                            text = "• LINE $activeLineIndex/${lineAnnotatedStrings.size}",
                            style = typography.hudMicroLabel.copy(
                                color = colors.electricVioletBright
                            )
                        )
                    }
                }

                // Expand/Collapse Chevron Indicator
                val chevronRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                    label = "chevronRotation"
                )
                Text(
                    text = "▼",
                    color = colors.textTelemetry,
                    fontSize = 11.sp,
                    modifier = Modifier.rotate(chevronRotation)
                )
            }

            // Expandable Syntax Code Body
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .padding(bottom = 8.dp)
                ) {
                    itemsIndexed(lineAnnotatedStrings) { index, annotatedLine ->
                        val lineNumber = index + 1
                        val isLineActive = lineNumber == activeLineIndex

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isLineActive) {
                                        colors.electricVioletBright.copy(alpha = 0.14f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Neon Accent Bar for Active Line
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(20.dp)
                                        .background(
                                            if (isLineActive) colors.electricVioletBright else Color.Transparent,
                                            RoundedCornerShape(2.dp)
                                        )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Line Number Monospace Gutter
                                Text(
                                    text = lineNumber.toString().padStart(2, ' '),
                                    style = typography.codeXRayBody.copy(
                                        color = if (isLineActive) colors.electricVioletBright else colors.textTelemetry,
                                        fontWeight = if (isLineActive) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    modifier = Modifier.width(24.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Syntax Highlighted Line Text
                                Text(
                                    text = annotatedLine,
                                    style = typography.codeXRayBody.copy(
                                        color = colors.textPrimary
                                    )
                                )
                            }

                            // Live Variable Watch Badges rendered beneath active line
                            if (isLineActive && variableBindings.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 44.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    variableBindings.forEach { (key, value) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.structuralSlate)
                                                .border(1.dp, colors.surfaceGlassBorder, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = key,
                                                    style = typography.hudMicroLabel.copy(
                                                        color = colors.electricVioletBright,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                )
                                                Text(
                                                    text = "=",
                                                    style = typography.hudMicroLabel.copy(
                                                        color = colors.textTelemetry
                                                    )
                                                )
                                                Text(
                                                    text = value,
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
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Synchronized Code X-Ray - Dark")
@Composable
private fun SynchronizedCodeXRaySheetPreview() {
    UnravelTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(UnravelTheme.colors.canvasVoid)
                .padding(16.dp)
        ) {
            val sampleCode = """
                var left = 0, right = items.size - 1
                while (left < right) {
                    val currentSum = arr[left] + arr[right]
                    if (currentSum == target) return Pair(left, right)
                    else if (currentSum < target) left++
                    else right--
                }
                return null
            """.trimIndent()

            SynchronizedCodeXRaySheet(
                codeSnippet = sampleCode,
                activeLineIndex = 3,
                variableBindings = persistentMapOf("left" to "2", "right" to "7", "currentSum" to "48", "target" to "50")
            )
        }
    }
}
