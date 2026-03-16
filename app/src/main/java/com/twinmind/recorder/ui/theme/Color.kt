package com.twinmind.recorder.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Dark backgrounds (layered depth system) ───────────────────────────────
val Background      = Color(0xFF0D0F14)   // Deepest layer — screen background
val Surface         = Color(0xFF161A23)   // Cards and sheets
val SurfaceVariant  = Color(0xFF1E2330)   // Elevated cards, dialogs
val SurfaceContainer= Color(0xFF252B3B)   // Input fields, chips
val Outline         = Color(0xFF2A3042)   // Dividers, borders

// ─── Brand / accent ────────────────────────────────────────────────────────
val Purple          = Color(0xFF7C3AED)   // Primary brand color
val PurpleLight     = Color(0xFF9F67FF)   // Lighter variant for gradients
val PurpleDim       = Color(0xFF4A1D96)   // Dark purple for pressed states
val PurpleGlow      = Color(0x337C3AED)   // Translucent — pulsing ring effect
val PurpleGlow2     = Color(0x1A7C3AED)   // More translucent — outer ring
val Blue            = Color(0xFF4F8EF7)   // Secondary accent (transcribing state)

// ─── Semantic status colors ────────────────────────────────────────────────
val RecordingRed    = Color(0xFFEF4444)   // Active recording indicator
val RecordingRedDim = Color(0xFF7F1D1D)   // Dark red for button gradient
val PausedAmber     = Color(0xFFF59E0B)   // Paused state
val SuccessGreen    = Color(0xFF22C55E)   // Completed / done
val ErrorRed        = Color(0xFFDC2626)   // Error states

// ─── Text hierarchy ────────────────────────────────────────────────────────
val OnBackground    = Color(0xFFF9FAFB)   // Primary text — highest contrast
val OnSurface       = Color(0xFFE5E7EB)   // Secondary text
val Muted           = Color(0xFF9CA3AF)   // Placeholder, labels, timestamps
val Subtle          = Color(0xFF6B7280)   // Very quiet text, disabled
