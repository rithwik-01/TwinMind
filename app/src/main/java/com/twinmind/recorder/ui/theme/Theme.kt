package com.twinmind.recorder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TwinMindDarkColorScheme = darkColorScheme(
    primary             = Purple,
    onPrimary           = Color.White,
    primaryContainer    = PurpleDim,
    onPrimaryContainer  = PurpleLight,
    secondary           = Blue,
    onSecondary         = Color.White,
    background          = Background,
    onBackground        = OnBackground,
    surface             = Surface,
    onSurface           = OnSurface,
    surfaceVariant      = SurfaceVariant,
    onSurfaceVariant    = Muted,
    outline             = Outline,
    error               = ErrorRed,
    onError             = Color.White,
)

@Composable
fun TwinMindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TwinMindDarkColorScheme,
        typography  = TwinMindTypography,
        content     = content
    )
}
