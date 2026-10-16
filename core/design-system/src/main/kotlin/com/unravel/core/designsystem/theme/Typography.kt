package com.unravel.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Canonical Typography Scale
// Reference: unravel_master_architecture_blueprint.md Section 1.4

val TelemetryHero = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum"
)

val NodeValueLabel = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    fontFeatureSettings = "tnum"
)

val CodeXRayBody = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 19.sp,
    fontFeatureSettings = "tnum"
)

val HudMicroLabel = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 1.2.sp,
    fontFeatureSettings = "tnum"
)

val DisplayHeader = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    letterSpacing = (-0.5).sp
)

val TitleMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp
)

val BodyMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp
)

val LabelMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp
)

@Immutable
data class UnravelTypography(
    val telemetryHero: TextStyle = TelemetryHero,
    val nodeValueLabel: TextStyle = NodeValueLabel,
    val codeXRayBody: TextStyle = CodeXRayBody,
    val hudMicroLabel: TextStyle = HudMicroLabel,
    val displayHeader: TextStyle = DisplayHeader,
    val titleMedium: TextStyle = TitleMedium,
    val bodyMedium: TextStyle = BodyMedium,
    val labelMedium: TextStyle = LabelMedium
)

val LocalUnravelTypography = staticCompositionLocalOf { UnravelTypography() }
