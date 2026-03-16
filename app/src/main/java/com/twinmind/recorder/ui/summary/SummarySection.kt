package com.twinmind.recorder.ui.summary

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.twinmind.recorder.ui.theme.*
import kotlinx.collections.immutable.ImmutableList

/**
 * Base card used for all 4 summary sections.
 */
@Stable
@Composable
fun SummaryCard(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Stable
@Composable
fun TitleSection(title: String?) {
    SummaryCard(label = "Title") {
        Text(
            text = title?.ifBlank { "Generating..." } ?: "Generating...",
            style = MaterialTheme.typography.headlineMedium,
            color = if (title.isNullOrBlank()) Muted else OnBackground
        )
    }
}

@Stable
@Composable
fun SummarySection(summary: String?) {
    SummaryCard(label = "Summary") {
        Text(
            text = summary?.ifBlank { "Generating..." } ?: "Generating...",
            style = MaterialTheme.typography.bodyLarge,
            color = if (summary.isNullOrBlank()) Muted else OnSurface
        )
    }
}

@Stable
@Composable
fun ActionItemsSection(items: ImmutableList<String>) {
    SummaryCard(label = "Action Items") {
        if (items.isEmpty()) {
            Text("Generating...", style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
        } else {
            items.forEach { item ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = null,
                        tint = Purple,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Stable
@Composable
fun KeyPointsSection(points: ImmutableList<String>) {
    SummaryCard(label = "Key Points") {
        if (points.isEmpty()) {
            Text("Generating...", style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
        } else {
            points.forEach { point ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // Purple bullet dot
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Purple)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * Shimmer skeleton placeholder card shown while summary is loading.
 */
@Composable
fun SkeletonCard(lines: Int = 3, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "shimmer_x"
    )

    val shimmerBrush = Brush.horizontalGradient(
        colors = listOf(SurfaceVariant, Surface.copy(alpha = 0.5f), SurfaceVariant),
        startX = shimmerX * 400f,
        endX = (shimmerX + 1) * 400f
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Label skeleton
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(shimmerBrush)
        )
        Spacer(Modifier.height(4.dp))
        // Content line skeletons
        repeat(lines) { idx ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (idx == lines - 1) 0.6f else 1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(shimmerBrush)
            )
        }
    }
}
