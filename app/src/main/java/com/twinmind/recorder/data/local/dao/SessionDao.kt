package com.twinmind.recorder.data.local.dao

import androidx.room.*
import com.twinmind.recorder.data.local.entity.SessionEntity
import com.twinmind.recorder.data.local.entity.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    // Dashboard list — newest first
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    // Observe a single session (for RecordingScreen / SummaryScreen)
    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getSession(id: String): Flow<SessionEntity?>

    // One-shot fetch (for Workers)
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionOnce(id: String): SessionEntity?

    @Query("UPDATE sessions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: SessionStatus)

    @Query("UPDATE sessions SET transcript = :transcript WHERE id = :id")
    suspend fun updateTranscript(id: String, transcript: String)

    @Query("UPDATE sessions SET endedAt = :endedAt, status = :status WHERE id = :id")
    suspend fun finalizeSession(id: String, endedAt: Long, status: SessionStatus)

    @Query("UPDATE sessions SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: String, title: String)

    @Query("UPDATE sessions SET status = :status, errorReason = :reason WHERE id = :id")
    suspend fun setError(id: String, status: SessionStatus, reason: String)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)
}
