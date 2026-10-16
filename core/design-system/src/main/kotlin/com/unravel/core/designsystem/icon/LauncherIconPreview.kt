package com.unravel.core.designsystem.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unravel.core.designsystem.R
import com.unravel.core.designsystem.theme.UnravelTheme

/**
 * Preview Composable validating visual rendering of the Split Ribbon adaptive launcher icon.
 * Reference: Sprint 0 WP-08 & unravel_master_architecture_blueprint.md Section 7.3
 */
@Composable
fun LauncherIcon(
    modifier: Modifier = Modifier,
    isRound: Boolean = false
) {
    val shape = if (isRound) CircleShape else RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .size(108.dp)
            .clip(shape)
            .border(1.dp, UnravelTheme.colors.surfaceGlassBorder, shape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background),
            contentDescription = "Launcher Background",
            modifier = Modifier.matchParentSize()
        )
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Launcher Foreground (Split Ribbon)",
            modifier = Modifier.matchParentSize()
        )
    }
}

@Preview(name = "Adaptive Launcher Icon - Squircle", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
fun LauncherIconSquirclePreview() {
    UnravelTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LauncherIcon(isRound = false)
        }
    }
}

@Preview(name = "Adaptive Launcher Icon - Round", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
fun LauncherIconRoundPreview() {
    UnravelTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LauncherIcon(isRound = true)
        }
    }
}
