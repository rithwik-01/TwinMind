package com.twinmind.recorder.data.local.dao

import androidx.room.*
import com.twinmind.recorder.data.local.entity.SummaryEntity
import com.twinmind.recorder.data.local.entity.SummaryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: SummaryEntity)

    @Update
    suspend fun update(summary: SummaryEntity)

    // UI observes this — updates in real time as SummaryWorker writes streaming tokens
    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId")
    fun getSummary(sessionId: String): Flow<SummaryEntity?>

    // One-shot fetch (for Workers)
    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId")
    suspend fun getSummaryOnce(sessionId: String): SummaryEntity?

    @Query("UPDATE summaries SET status = :status, errorMessage = :error WHERE sessionId = :id")
    suspend fun updateStatus(id: String, status: SummaryStatus, error: String? = null)

    @Query("DELETE FROM summaries WHERE sessionId = :sessionId")
    suspend fun delete(sessionId: String)
}
