package com.twinmind.recorder.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinmind.recorder.data.local.entity.SessionEntity
import com.twinmind.recorder.data.local.entity.SessionStatus
import com.twinmind.recorder.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MeetingCard(
    session: SessionEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when (session.status) {
        SessionStatus.COMPLETED -> SuccessGreen
        SessionStatus.PAUSED    -> PausedAmber
        SessionStatus.ERROR     -> ErrorRed
        SessionStatus.RECORDING -> RecordingRed
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(0.dp)
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(88.dp)
                .background(accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
        )

        // Card content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text     = session.title.ifBlank { "Untitled Recording" },
                    style    = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = session.startedAt.toDateString(),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(6.dp))

            // Duration + status chip row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val duration = session.endedAt?.let {
                    ((it - session.startedAt) / 1000).toInt().toFormattedDuration()
                } ?: run {
                    val elapsed = (System.currentTimeMillis() - session.startedAt) / 1000
                    elapsed.toInt().toFormattedDuration()
                }
                Text(
                    text  = duration,
                    style = MaterialTheme.typography.bodySmall
                )

                StatusChip(status = session.status, transcript = session.transcript)
            }
        }
    }
}

@Composable
fun StatusChip(status: SessionStatus, transcript: String?) {
    val (label, chipColor) = when {
        status == SessionStatus.RECORDING                        -> "Recording" to RecordingRed
        status == SessionStatus.COMPLETED && transcript == null  -> "Transcribing..." to Blue
        status == SessionStatus.COMPLETED && transcript != null  -> "Completed" to SuccessGreen
        status == SessionStatus.PAUSED                          -> "Paused" to PausedAmber
        status == SessionStatus.ERROR                           -> "Error" to ErrorRed
        else                                                    -> "Unknown" to Muted
    }

    // Pulsing dot for "Recording" status
    val alpha = if (status == SessionStatus.RECORDING) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue  = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_alpha"
        ).value
    } else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(chipColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        if (status == SessionStatus.RECORDING) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(chipColor.copy(alpha = alpha))
            )
        }
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(color = chipColor)
        )
    }
}

private fun Long.toDateString(): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(this))

private fun Int.toFormattedDuration(): String {
    val m = this / 60
    val s = this % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
