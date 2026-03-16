package com.twinmind.recorder

import com.twinmind.recorder.util.SilenceDetector
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SilenceDetectorTest {

    private var silenceDetected = false
    private var audioDetected = false
    private lateinit var detector: SilenceDetector

    @Before
    fun setup() {
        silenceDetected = false
        audioDetected = false
        detector = SilenceDetector(
            silenceThresholdRms = 150,
            silenceDurationMs = 500L, // short for testing
            onSilenceDetected = { silenceDetected = true },
            onAudioDetected = { audioDetected = true }
        )
    }

    @Test
    fun `silent buffer triggers silence detected after threshold duration`() {
        val silentBuffer = ShortArray(1024) { 0 }
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 600L) {
            detector.feed(silentBuffer, silentBuffer.size)
        }
        assertTrue("Silence should have been detected", silenceDetected)
    }

    @Test
    fun `loud buffer does not trigger silence detected`() {
        val loudBuffer = ShortArray(1024) { 10000 }
        repeat(100) { detector.feed(loudBuffer, loudBuffer.size) }
        assertFalse("Silence should NOT be detected for loud audio", silenceDetected)
    }

    @Test
    fun `audio detected fires after silence then loud audio`() {
        val silentBuffer = ShortArray(1024) { 0 }
        val loudBuffer = ShortArray(1024) { 10000 }

        // First go silent
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 600L) {
            detector.feed(silentBuffer, silentBuffer.size)
        }
        assertTrue(silenceDetected)

        // Then go loud
        detector.feed(loudBuffer, loudBuffer.size)
        assertTrue("Audio detected should fire after silence breaks", audioDetected)
    }

    @Test
    fun `reset clears silence state`() {
        val silentBuffer = ShortArray(1024) { 0 }
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 600L) {
            detector.feed(silentBuffer, silentBuffer.size)
        }
        assertTrue(silenceDetected)

        detector.reset()
        silenceDetected = false

        // Feed silence again — should need full duration again
        detector.feed(silentBuffer, silentBuffer.size)
        assertFalse("Should not immediately re-trigger after reset", silenceDetected)
    }

    @Test
    fun `empty buffer does not crash`() {
        detector.feed(ShortArray(0), 0)
        assertFalse(silenceDetected)
    }
}