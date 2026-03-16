package com.twinmind.recorder.ui.recording

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.twinmind.recorder.service.RecordingState
import com.twinmind.recorder.service.RecordingStatus
import com.twinmind.recorder.ui.navigation.Screen
import com.twinmind.recorder.ui.theme.*
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.compose.runtime.snapshotFlow

/**
 * Recording screen with live waveform, timer, and real-time transcript preview.
 * Handles recording state changes, errors, and navigation to summary.
 */
@Composable
fun RecordingScreen(
    navController: NavController,
    sessionId: String,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val recStatus     by viewModel.recordingStatus.collectAsStateWithLifecycle()
    val session       by viewModel.getSession(sessionId).collectAsStateWithLifecycle(null)
    val pendingChunks by viewModel.getPendingChunkCount(sessionId).collectAsStateWithLifecycle(0)

    var showStopDialog by remember { mutableStateOf(false) }
    var hasNavigated by remember { mutableStateOf(false) }
    var displayAmplitude by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(recStatus.state, "fade") {
        when (recStatus.state) {
            RecordingState.RECORDING -> {
                // Amplitude updated separately via snapshotFlow
            }
            RecordingState.FINALIZING -> {
                val startAmplitude = displayAmplitude
                val fadeDuration = 1000L
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < fadeDuration) {
                    val progress = (System.currentTimeMillis() - startTime).toFloat() / fadeDuration
                    displayAmplitude = startAmplitude * (1f - progress)
                    delay(16)
                }
                displayAmplitude = 0f
            }
            else -> { displayAmplitude = 0f }
        }
    }

    LaunchedEffect(recStatus.state, "amplitude") {
        if (recStatus.state == RecordingState.RECORDING) {
            snapshotFlow { recStatus.amplitude }
                .collect { displayAmplitude = it }
        }
    }

    LaunchedEffect(hasNavigated) {
        if (hasNavigated) {
            delay(300)
            navController.navigate(Screen.Summary.createRoute(sessionId)) {
                launchSingleTop = true
                popUpTo(Screen.Recording.createRoute(sessionId)) {
                    inclusive = true
                }
            }
        }
    }

    LaunchedEffect(recStatus.state) {
        if (recStatus.state == RecordingState.ERROR) {
            delay(2000)
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Dashboard.route) { inclusive = false }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            RecordingTopBar(
                title         = session?.title ?: "Recording...",
                onBack        = { navController.popBackStack() },
                onStopDiscard = { showStopDialog = true }
            )

            Spacer(Modifier.weight(0.5f))

            Text(
                text  = recStatus.elapsedMs.toTimerString(),
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(Modifier.height(24.dp))

            RecordingButton(
                status  = recStatus,
                onClick = {
                    if (recStatus.state == RecordingState.RECORDING) {
                        showStopDialog = true
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            WaveformView(
                amplitude   = displayAmplitude,
                isAnimating = recStatus.state == RecordingState.RECORDING,
                modifier    = Modifier
                    .fillMaxWidth(0.9f)
                    .height(80.dp)
            )

            Spacer(Modifier.weight(0.5f))

            StatusRow(status = recStatus, pendingChunks = pendingChunks)

            Spacer(Modifier.height(12.dp))

            TranscriptPreviewCard(
                transcript = session?.transcript,
                modifier   = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.navigationBarsPadding().height(16.dp))
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            containerColor   = SurfaceVariant,
            title = { Text("Stop Recording?", color = OnBackground) },
            text  = {
                Text(
                    "Recording will stop and a summary will be generated.",
                    color = OnSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopDialog = false
                        hasNavigated = true
                        viewModel.stopRecording()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) { Text("Stop & Summarize") }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Keep Recording", color = Muted)
                }
            }
        )
    }
}

@Composable
private fun RecordingTopBar(
    title: String,
    onBack: () -> Unit,
    onStopDiscard: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
        }
        Text(
            text     = title,
            style    = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = OnSurface)
            }
            DropdownMenu(
                expanded         = showMenu,
                onDismissRequest = { showMenu = false },
                modifier         = Modifier.background(SurfaceVariant)
            ) {
                DropdownMenuItem(
                    text    = { Text("Stop & Discard", color = ErrorRed) },
                    onClick = { showMenu = false; onStopDiscard() }
                )
            }
        }
    }
}

@Composable
private fun RecordingButton(
    status: RecordingStatus,
    onClick: () -> Unit
) {
    val isRecording  = status.state == RecordingState.RECORDING
    val isFinalizing = status.state == RecordingState.FINALIZING

    val buttonColor by animateColorAsState(
        targetValue = when {
            isRecording -> RecordingRed
            else        -> PausedAmber
        },
        animationSpec = tween(300),
        label = "button_color"
    )

    Box(contentAlignment = Alignment.Center) {
        if (isRecording) {
            listOf(0, 400, 800).forEachIndexed { idx, delayMs ->
                PulsingRing(delayMs = delayMs, alpha = 0.5f - idx * 0.12f)
            }
        }

        Button(
            onClick        = onClick,
            modifier       = Modifier.size(88.dp),
            shape          = CircleShape,
            colors         = ButtonDefaults.buttonColors(containerColor = buttonColor),
            contentPadding = PaddingValues(0.dp),
            elevation      = ButtonDefaults.buttonElevation(defaultElevation = 12.dp),
            enabled        = isRecording
        ) {
            Icon(
                imageVector        = if (isRecording || isFinalizing) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Stop recording",
                modifier           = Modifier.size(36.dp),
                tint               = Color.White
            )
        }
    }
}

@Composable
private fun PulsingRing(delayMs: Int, alpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_$delayMs")

    val scale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.8f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = FastOutSlowInEasing, delayMillis = delayMs),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale_$delayMs"
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue  = alpha,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, delayMillis = delayMs),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha_$delayMs"
    )

    Box(
        modifier = Modifier
            .size(88.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(RecordingRed.copy(alpha = ringAlpha))
    )
}

@Composable
private fun StatusRow(status: RecordingStatus, pendingChunks: Int) {
    val (icon, statusText, color) = when {
        status.state == RecordingState.ERROR ->
            Triple(Icons.Default.ErrorOutline, status.errorMessage ?: "Recording stopped", ErrorRed)
        status.state == RecordingState.FINALIZING ->
            Triple(Icons.Default.HourglassTop, "Finalizing...", Muted)
        status.warning != null ->
            Triple(Icons.Default.Warning, status.warning, PausedAmber)
        status.state == RecordingState.PAUSED_PHONE_CALL ->
            Triple(Icons.Default.Phone, "Paused – Phone call", PausedAmber)
        status.state == RecordingState.PAUSED_AUDIO_FOCUS ->
            Triple(Icons.AutoMirrored.Filled.VolumeOff, "Paused – Audio focus lost", PausedAmber)
        status.state == RecordingState.RECORDING && pendingChunks > 0 ->
            Triple(Icons.Default.CloudUpload, "Transcribing $pendingChunks chunk${if (pendingChunks > 1) "s" else ""}...", Blue)
        status.state == RecordingState.RECORDING ->
            Triple(Icons.Default.FiberManualRecord, "Recording...", RecordingRed)
        else ->
            Triple(Icons.Default.Info, "Processing...", Muted)
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier              = Modifier.padding(horizontal = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = statusText, style = MaterialTheme.typography.bodySmall.copy(color = color))
    }
}

@Composable
private fun TranscriptPreviewCard(
    transcript: String?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(transcript) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(16.dp)
    ) {
        Text(
            text  = "Live Transcript",
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Text(
                text  = transcript?.takeIf { it.isNotBlank() }
                    ?: "Transcript will appear here as each 30-second chunk is processed...",
                style = if (transcript.isNullOrBlank())
                    MaterialTheme.typography.bodySmall.copy(color = Subtle)
                else
                    MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun Long.toTimerString(): String {
    val totalSeconds = this / 1000
    return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}