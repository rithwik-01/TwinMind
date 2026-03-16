package com.twinmind.recorder.util

/**
 * Thread-safe circular byte buffer for storing the last N bytes of PCM audio.
 *
 * Used for 2-second chunk overlap (Edge Case 7):
 *   - At 16kHz mono 16-bit PCM: 16000 samples/s × 2 bytes × 2s = 64,000 bytes
 *   - When a 30s chunk ends, drain() returns the last 2s of audio
 *   - That 2s is prepended to the next chunk's WAV file before recording continues
 *   - This ensures words spoken at chunk boundaries appear in both chunks,
 *     preventing speech from being cut off at the 30s boundary
 *
 * @param capacityBytes Total capacity. Set to SAMPLE_RATE * 2 * OVERLAP_SECONDS.
 */
class PcmRingBuffer(val capacityBytes: Int) {

    private val buffer       = ByteArray(capacityBytes)
    private var writePos     = 0
    private var totalWritten = 0L

    /**
     * Write bytes into the ring buffer.
     * When full, oldest bytes are overwritten (circular behavior).
     */
    @Synchronized
    fun write(data: ByteArray, offset: Int = 0, length: Int = data.size) {
        var remaining = length
        var srcPos    = offset

        while (remaining > 0) {
            val toCopy = minOf(remaining, capacityBytes - writePos)
            System.arraycopy(data, srcPos, buffer, writePos, toCopy)
            writePos      = (writePos + toCopy) % capacityBytes
            totalWritten += toCopy
            srcPos        += toCopy
            remaining     -= toCopy
        }
    }

    /**
     * Returns all buffered bytes in chronological order (oldest first).
     * Does NOT consume/clear the buffer — the ring continues rolling after drain().
     */
    @Synchronized
    fun drain(): ByteArray {
        val size   = minOf(totalWritten, capacityBytes.toLong()).toInt()
        val result = ByteArray(size)

        if (totalWritten < capacityBytes) {
            // Buffer not yet full — bytes start at index 0
            System.arraycopy(buffer, 0, result, 0, size)
        } else {
            // Buffer wrapped: writePos points to the oldest byte
            val tail = capacityBytes - writePos
            System.arraycopy(buffer, writePos, result, 0,    tail)
            System.arraycopy(buffer, 0,         result, tail, writePos)
        }
        return result
    }

    @Synchronized
    fun reset() {
        writePos     = 0
        totalWritten = 0L
    }

    val isEmpty: Boolean get() = totalWritten == 0L
}
