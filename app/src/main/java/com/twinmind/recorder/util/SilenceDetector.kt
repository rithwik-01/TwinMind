package com.twinmind.recorder.util

import kotlin.math.sqrt

/**
 * Detects sustained silence in an audio stream (Edge Case 6).
 *
 * Algorithm:
 *   1. Compute RMS (Root Mean Square) of each PCM short buffer
 *   2. RMS < silenceThreshold → potential silence, start/continue timer
 *   3. If silence persists for silenceDurationMs → fire onSilenceDetected()
 *   4. Any buffer above threshold → reset timer, fire onAudioDetected()
 *
 * RMS range: 0 (pure silence) to 32,768 (max 16-bit PCM amplitude).
 * Threshold of 100 represents very quiet but not dead-silent audio.
 *
 * Usage: Call feed() on every AudioRecord.read() buffer.
 */
class SilenceDetector(
    private val silenceThresholdRms: Int = 500,
    private val silenceDurationMs: Long = 10_000L,
    private val onSilenceDetected: () -> Unit,
    private val onAudioDetected: () -> Unit
) {

    private var silenceSinceMs: Long? = null
    private var silenceNotified       = false

    fun feed(buffer: ShortArray, samplesRead: Int) {
        if (samplesRead <= 0) return

        val rms = computeRms(buffer, samplesRead)
        val now = System.currentTimeMillis()

        if (rms < silenceThresholdRms) {
            if (silenceSinceMs == null) silenceSinceMs = now

            val silenceDuration = now - (silenceSinceMs ?: now)
            if (silenceDuration >= silenceDurationMs && !silenceNotified) {
                silenceNotified = true
                onSilenceDetected()
            }
        } else {
            val wasInSilence = silenceSinceMs != null
            silenceSinceMs   = null
            silenceNotified  = false
            if (wasInSilence) onAudioDetected()
        }
    }

    private fun computeRms(buffer: ShortArray, count: Int): Double {
        var sum = 0.0
        for (i in 0 until count) {
            val sample = buffer[i].toDouble()
            sum += sample * sample
        }
        return sqrt(sum / count)
    }

    fun reset() {
        silenceSinceMs  = null
        silenceNotified = false
    }
}
