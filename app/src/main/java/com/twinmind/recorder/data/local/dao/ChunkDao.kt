package com.twinmind.recorder.data.local.dao

import androidx.room.*
import com.twinmind.recorder.data.local.entity.ChunkEntity
import com.twinmind.recorder.data.local.entity.UploadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: ChunkEntity)

    @Update
    suspend fun update(chunk: ChunkEntity)

    // ORDER BY index ASC is MANDATORY — transcript stitching depends on chunk order
    @Query("SELECT * FROM chunks WHERE sessionId = :sessionId ORDER BY `index` ASC")
    suspend fun getChunksForSession(sessionId: String): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE id = :chunkId")
    suspend fun getChunk(chunkId: String): ChunkEntity?

    // Recovery query: everything not yet successfully transcribed (used on restart/reboot)
    @Query("""
        SELECT * FROM chunks
        WHERE uploadStatus IN ('PENDING', 'UPLOADING', 'FAILED')
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingChunks(): List<ChunkEntity>

    @Query("UPDATE chunks SET uploadStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: UploadStatus)

    @Query("UPDATE chunks SET transcript = :transcript, uploadStatus = 'DONE' WHERE id = :id")
    suspend fun saveTranscript(id: String, transcript: String)

    @Query("UPDATE chunks SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: String)

    // Only chunks with transcripts, in correct order — used for stitching
    @Query("""
        SELECT * FROM chunks
        WHERE sessionId = :sessionId AND uploadStatus = 'DONE'
        ORDER BY `index` ASC
    """)
    suspend fun getCompletedChunks(sessionId: String): List<ChunkEntity>

    // Observe count of un-transcribed chunks — used by UI to show "Transcribing X chunks..."
    @Query("SELECT COUNT(*) FROM chunks WHERE sessionId = :sessionId AND uploadStatus != 'DONE'")
    fun observePendingCount(sessionId: String): Flow<Int>

    @Query("DELETE FROM chunks WHERE sessionId = :sessionId")
    suspend fun deleteAllForSession(sessionId: String)
}
