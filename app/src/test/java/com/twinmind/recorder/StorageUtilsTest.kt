package com.twinmind.recorder

import com.twinmind.recorder.util.StorageUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class StorageUtilsTest {

    @Test
    fun `MIN_FREE_BYTES is exactly 50MB`() {
        assertEquals(50L * 1024 * 1024, StorageUtils.MIN_FREE_BYTES)
    }

    @Test
    fun `hasEnoughStorage returns false for non-existent directory`() {
        val fakeDir = File("/non/existent/path")
        assertFalse(StorageUtils.hasEnoughStorage(fakeDir))
    }

    @Test
    fun `estimatedChunkSizeBytes is reasonable for 30s audio`() {
        val size = StorageUtils.estimatedChunkSizeBytes()
        // 16000 * 1 * 2 * 32 + 44 = 1,024,044 bytes (~1MB)
        assertTrue("Chunk size should be > 500KB", size > 500_000L)
        assertTrue("Chunk size should be < 2MB", size < 2_000_000L)
    }
}