package com.twinmind.recorder.util

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Utilities for writing WAV files from raw PCM data.
 *
 * WHY: AudioRecord records raw PCM (no header). The Whisper API requires
 * a proper WAV file with a 44-byte RIFF header. This util handles that.
 *
 * WAV header format (44 bytes):
 *   "RIFF" + file size + "WAVE" + "fmt " chunk + "data" chunk
 *
 * Default params match AudioRecord config in RecordingService:
 *   - 16kHz sample rate
 *   - Mono (1 channel)
 *   - 16-bit PCM
 */
object WavUtils {

    /**
     * Write a WAV header to the given OutputStream for the given PCM data length.
     * After calling this, write the raw PCM bytes to the same stream.
     */
    fun writeWavHeader(
        out:        OutputStream,
        pcmBytes:   Int,
        sampleRate: Int   = 16000,
        channels:   Short = 1,
        bitDepth:   Short = 16
    ) {
        val byteRate   = sampleRate * channels * (bitDepth / 8)
        val blockAlign = (channels * (bitDepth / 8)).toShort()
        val dataLen    = pcmBytes
        val totalLen   = dataLen + 36

        out.write("RIFF".toByteArray())
        out.writeIntLE(totalLen)
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        out.writeIntLE(16)                       // PCM sub-chunk size
        out.writeShortLE(1)                      // Audio format: PCM
        out.writeShortLE(channels.toInt())
        out.writeIntLE(sampleRate)
        out.writeIntLE(byteRate)
        out.writeShortLE(blockAlign.toInt())
        out.writeShortLE(bitDepth.toInt())
        out.write("data".toByteArray())
        out.writeIntLE(dataLen)
    }

    /**
     * Convert a raw PCM ByteArray to a .wav File with proper header.
     * Writes: [44-byte WAV header][raw PCM bytes]
     */
    fun pcmToWav(
        pcm:        ByteArray,
        outFile:    File,
        sampleRate: Int = 16000
    ) {
        FileOutputStream(outFile).use { fos ->
            writeWavHeader(fos, pcm.size, sampleRate)
            fos.write(pcm)
        }
    }

    /**
     * Combine overlap PCM + current chunk PCM into a single WAV file.
     * overlap goes first (chronologically earlier), then chunkPcm.
     */
    fun writeChunkWav(
        overlap:    ByteArray,
        chunkPcm:   ByteArray,
        outFile:    File,
        sampleRate: Int = 16000
    ) {
        val totalPcmBytes = overlap.size + chunkPcm.size
        FileOutputStream(outFile).use { fos ->
            writeWavHeader(fos, totalPcmBytes, sampleRate)
            if (overlap.isNotEmpty()) fos.write(overlap)
            fos.write(chunkPcm)
        }
    }

    // Little-endian write helpers
    private fun OutputStream.writeIntLE(v: Int) {
        write(v and 0xFF)
        write((v shr 8)  and 0xFF)
        write((v shr 16) and 0xFF)
        write((v shr 24) and 0xFF)
    }

    private fun OutputStream.writeShortLE(v: Int) {
        write(v and 0xFF)
        write((v shr 8) and 0xFF)
    }
}
