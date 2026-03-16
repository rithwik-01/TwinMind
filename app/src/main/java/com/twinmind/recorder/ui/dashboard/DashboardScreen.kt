package com.twinmind.recorder.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.twinmind.recorder.data.local.entity.SessionEntity
import com.twinmind.recorder.service.RecordingState
import com.twinmind.recorder.ui.navigation.Screen
import com.twinmind.recorder.ui.theme.*
import java.util.Locale

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val sessions      by viewModel.sessions.collectAsStateWithLifecycle()
    val recStatus     by viewModel.recordingStatus.collectAsStateWithLifecycle()
    val isRecording    = recStatus.state != RecordingState.IDLE

    // Delete confirmation dialog state
    var sessionToDelete by remember { mutableStateOf<SessionEntity?>(null) }
    
    // Track previous sessionId to detect when a new recording starts
    val previousSessionId = remember { mutableStateOf<String?>(null) }

    // Auto-navigate to recording screen when a new recording starts
    LaunchedEffect(recStatus.sessionId) {
        val newId = recStatus.sessionId
        if (newId != null && newId != previousSessionId.value) {
            previousSessionId.value = newId
            navController.navigate(Screen.Recording.createRoute(newId))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Top bar ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PurpleGlow, Background)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text  = "TwinMind",
                    style = MaterialTheme.typography.headlineLarge.copy(color = OnBackground)
                )
                // If currently recording, show live indicator in top-right
                if (isRecording) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiveDot()
                        Text(
                            text  = recStatus.elapsedMs.toTimerString(),
                            style = MaterialTheme.typography.labelMedium.copy(color = RecordingRed)
                        )
                    }
                }
            }

            // ─── Session list ─────────────────────────────────────────────────
            if (sessions.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        MeetingCard(
                            session    = session,
                            onClick    = { navController.navigate(Screen.Summary.createRoute(session.id)) },
                            onLongClick = { sessionToDelete = session }
                        )
                    }
                    item { Spacer(Modifier.height(100.dp)) } // Space for FAB
                }
            }
        }

        // ─── New Recording FAB ─────────────────────────────────────────────
        NewRecordingButton(
            isRecording = isRecording,
            sessionId   = recStatus.sessionId,
            modifier    = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            onStartRecording = {
                viewModel.startNewRecording()
            },
            onGoToRecording = { id ->
                navController.navigate(Screen.Recording.createRoute(id))
            }
        )
    }

    // ─── Delete confirmation dialog ────────────────────────────────────────
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            containerColor   = SurfaceVariant,
            title = { Text("Delete Recording", color = OnBackground) },
            text  = { Text("Delete \"${session.title}\"? This cannot be undone.", color = OnSurface) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(session)
                    sessionToDelete = null
                }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel", color = Muted)
                }
            }
        )
    }
}

@Composable
private fun NewRecordingButton(
    isRecording: Boolean,
    sessionId: String?,
    modifier: Modifier = Modifier,
    onStartRecording: () -> Unit,
    onGoToRecording: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.12f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Pulsing outer ring (only when NOT recording)
        if (!isRecording) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(glowScale)
                    .clip(CircleShape)
                    .background(PurpleGlow.copy(alpha = ringAlpha))
            )
        }

        // Main button
        Button(
            onClick = {
                if (isRecording && sessionId != null) {
                    onGoToRecording(sessionId)
                } else {
                    onStartRecording()
                }
            },
            modifier = Modifier.size(76.dp),
            shape    = CircleShape,
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) RecordingRed else Purple
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Mic,
                contentDescription = if (isRecording) "Go to recording" else "Start recording",
                modifier           = Modifier.size(32.dp),
                tint               = OnBackground
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector        = Icons.Default.Mic,
            contentDescription = null,
            modifier           = Modifier.size(56.dp),
            tint               = Subtle
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text      = "No recordings yet",
            style     = MaterialTheme.typography.titleMedium.copy(color = Muted),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Tap the button below to start\nyour first recording",
            style     = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LiveDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "live_dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 0.2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(RecordingRed.copy(alpha = alpha))
    )
}

private fun Long.toTimerString(): String {
    val s = this / 1000
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}
