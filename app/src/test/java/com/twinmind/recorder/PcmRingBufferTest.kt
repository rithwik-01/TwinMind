package com.twinmind.recorder

import com.twinmind.recorder.util.PcmRingBuffer
import org.junit.Assert.*
import org.junit.Test

class PcmRingBufferTest {

    @Test
    fun `drain returns last N bytes written when more than capacity written`() {
        val capacity = 10
        val buffer = PcmRingBuffer(capacity)

        // Write 20 bytes — only last 10 should be retained
        buffer.write(ByteArray(20) { it.toByte() })
        val drained = buffer.drain()

        assertEquals(capacity, drained.size)
        // Last 10 bytes of 0..19 are 10..19
        assertEquals(10.toByte(), drained[0])
        assertEquals(19.toByte(), drained[9])
    }

    @Test
    fun `drain returns all bytes when less than capacity written`() {
        val buffer = PcmRingBuffer(100)
        buffer.write(byteArrayOf(1, 2, 3, 4, 5))
        val drained = buffer.drain()
        assertEquals(5, drained.size)
    }

    @Test
    fun `drain after no writes returns empty`() {
        val buffer = PcmRingBuffer(100)
        val drained = buffer.drain()
        assertEquals(0, drained.size)
    }

    @Test
    fun `write empty array does not crash`() {
        val buffer = PcmRingBuffer(100)
        buffer.write(ByteArray(0))
        assertEquals(0, buffer.drain().size)
    }
}