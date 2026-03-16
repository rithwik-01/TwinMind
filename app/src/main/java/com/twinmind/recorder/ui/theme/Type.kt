package com.twinmind.recorder.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val TwinMindTypography = Typography(
    // Large timer display "00:00"
    displayLarge = TextStyle(
        fontSize     = 48.sp,
        fontWeight   = FontWeight.Bold,
        letterSpacing = (-1).sp,
        color        = OnBackground
    ),
    // Section headers
    headlineLarge = TextStyle(
        fontSize   = 24.sp,
        fontWeight = FontWeight.SemiBold,
        color      = OnBackground
    ),
    headlineMedium = TextStyle(
        fontSize   = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color      = OnBackground
    ),
    // Meeting title in card
    titleLarge = TextStyle(
        fontSize   = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color      = OnBackground
    ),
    titleMedium = TextStyle(
        fontSize   = 15.sp,
        fontWeight = FontWeight.Medium,
        color      = OnBackground
    ),
    // Body text in transcript / summary
    bodyLarge = TextStyle(
        fontSize      = 16.sp,
        fontWeight    = FontWeight.Normal,
        lineHeight    = 24.sp,
        color         = OnSurface
    ),
    bodyMedium = TextStyle(
        fontSize   = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp,
        color      = OnSurface
    ),
    bodySmall = TextStyle(
        fontSize   = 12.sp,
        fontWeight = FontWeight.Normal,
        color      = Muted
    ),
    // "SUMMARY", "ACTION ITEMS" section labels
    labelMedium = TextStyle(
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color         = Purple
    ),
    labelSmall = TextStyle(
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        color         = Muted
    )
)
