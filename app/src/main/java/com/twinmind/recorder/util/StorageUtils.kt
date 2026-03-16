package com.twinmind.recorder.util

import android.content.Context
import android.os.StatFs
import java.io.File

/**
 * Utility functions for storage management and audio file handling.
 */
object StorageUtils {

    /** Minimum free storage before refusing to start or continue recording */
    const val MIN_FREE_BYTES = 50L * 1024 * 1024  // 50 MB

    /**
     * Returns true if there is enough free storage to continue recording.
     * Called before starting a session AND before each new 30s chunk.
     */
    fun hasEnoughStorage(directory: File): Boolean {
        return try {
            val stat = StatFs(directory.path)
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            freeBytes >= MIN_FREE_BYTES
        } catch (e: Exception) {
            false // Treat stat failure as "not enough storage"
        }
    }

    /**
     * Returns (or creates) the directory where audio chunk WAV files are stored.
     * Uses internal storage (no permissions needed, private to app).
     */
    fun getAudioDir(context: Context): File {
        val dir = File(context.filesDir, "audio_chunks")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /** Approximate size of a 30-second WAV at 16kHz mono 16-bit + 2s overlap */
    fun estimatedChunkSizeBytes(): Long {
        val sampleRate = 16000
        val channels   = 1
        val bitDepth   = 2  // bytes
        val durationS  = 32 // 30s chunk + 2s overlap
        return (sampleRate * channels * bitDepth * durationS).toLong() + 44L // +44 for WAV header
    }
}