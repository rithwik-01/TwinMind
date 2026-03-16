package com.twinmind.recorder.util

import android.media.AudioFormat
import android.media.AudioRecord
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/**
 * Convert a ShortArray (raw PCM samples from AudioRecord) to a ByteArray.
 * Uses little-endian byte order, matching the WAV PCM_16BIT format.
 *
 * Renamed to toPcmByteArray() to avoid collision with Kotlin stdlib's ShortArray.toByteArray()
 */
fun ShortArray.toPcmByteArray(count: Int = this.size): ByteArray {
    val byteBuffer = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (i in 0 until count) byteBuffer.putShort(this[i])
    return byteBuffer.array()
}

/**
 * Format milliseconds as "MM:SS" string for the recording timer display.
 */
fun Long.toTimerString(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

/**
 * Returns the minimum AudioRecord buffer size, validated.
 * Throws IllegalStateException if AudioRecord returns an error code.
 */
fun audioRecordMinBufferSize(
    sampleRate: Int = 16000,
    channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
): Int {
    val size = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    check(size != AudioRecord.ERROR && size != AudioRecord.ERROR_BAD_VALUE) {
        "AudioRecord.getMinBufferSize returned error: $size"
    }
    return size * 4  // 4x min for stable recording without buffer overruns
}
