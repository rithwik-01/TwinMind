package com.twinmind.recorder.data.repository

import com.twinmind.recorder.data.local.dao.ChunkDao
import com.twinmind.recorder.data.local.dao.SessionDao
import com.twinmind.recorder.data.local.entity.ChunkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing transcript data and chunk status.
 */
@Singleton
class TranscriptRepository @Inject constructor(
    private val chunkDao: ChunkDao,
    private val sessionDao: SessionDao
) {
    /** Observe count of chunks still pending transcription (drives progress UI) */
    fun observePendingCount(sessionId: String): Flow<Int> =
        chunkDao.observePendingCount(sessionId)

    suspend fun getChunksForSession(sessionId: String): List<ChunkEntity> =
        chunkDao.getChunksForSession(sessionId)
}