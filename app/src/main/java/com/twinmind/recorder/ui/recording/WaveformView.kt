package com.twinmind.recorder.ui.recording

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.twinmind.recorder.ui.theme.Purple
import com.twinmind.recorder.ui.theme.PurpleLight
import kotlin.math.sin

/**
 * Animated waveform visualizer driven by real-time audio amplitude.
 * Displays 40 bars with varying heights based on audio levels.
 */
@Composable
fun WaveformView(
    amplitude: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 40,
    isAnimating: Boolean = true
) {
    val barMultipliers = remember {
        List(barCount) { idx ->
            val distanceFromCenter = kotlin.math.abs(idx - barCount / 2f) / (barCount / 2f)
            (1f - distanceFromCenter * 0.6f) * (0.7f + (idx * 37 % 30) / 100f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        barMultipliers.forEachIndexed { index, multiplier ->
            val phaseOffset = index * 9f
            
            val waveValue = if (isAnimating) {
                val amplifiedAmplitude = amplitude * 8f
                val sineWave = sin(Math.toRadians((phase + phaseOffset).toDouble())).toFloat()
                
                val baseHeight = (amplifiedAmplitude * multiplier * 60f).coerceIn(8f, 60f)
                val modulatedHeight = baseHeight * (0.2f + sineWave * 0.8f)
                
                modulatedHeight.coerceIn(8f, 60f)
            } else {
                8f
            }

            val animatedHeight by animateFloatAsState(
                targetValue = waveValue,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "bar_$index"
            )

            val barColor = if (index % 2 == 0) Purple else PurpleLight

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animatedHeight.dp)
                    .background(
                        barColor.copy(alpha = 0.5f + amplitude * 0.5f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}