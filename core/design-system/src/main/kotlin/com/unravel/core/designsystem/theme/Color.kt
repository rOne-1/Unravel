package com.unravel.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Canonical Color Tokens (Dark Mode First)
// Reference: unravel_master_architecture_blueprint.md Section 1.3
val CanvasVoid = Color(0xFF0B0E14)
val SurfaceGlass = Color(0x99161B26)
val SurfaceGlassBorder = Color(0x1FFFFFFF)
val StructuralSlate = Color(0xFF262F40)
val StructuralSlateMuted = Color(0xFF1A202C)
val NeonMint = Color(0xFF00F5A0)
val ElectricVioletDeep = Color(0xFF7B2CBF)
val ElectricVioletBright = Color(0xFF9D4EDD)
val CoralFlame = Color(0xFFFF4365)
val TextPrimary = Color(0xFFF0F4F8)
val TextTelemetry = Color(0xFF8A99AD)

@Immutable
data class UnravelColors(
    val canvasVoid: Color = CanvasVoid,
    val surfaceGlass: Color = SurfaceGlass,
    val surfaceGlassBorder: Color = SurfaceGlassBorder,
    val structuralSlate: Color = StructuralSlate,
    val structuralSlateMuted: Color = StructuralSlateMuted,
    val neonMint: Color = NeonMint,
    val electricVioletDeep: Color = ElectricVioletDeep,
    val electricVioletBright: Color = ElectricVioletBright,
    val coralFlame: Color = CoralFlame,
    val textPrimary: Color = TextPrimary,
    val textTelemetry: Color = TextTelemetry
)

val LocalUnravelColors = staticCompositionLocalOf { UnravelColors() }
