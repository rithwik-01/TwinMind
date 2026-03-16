package com.twinmind.recorder.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.twinmind.recorder.data.local.dao.SummaryDao
import com.twinmind.recorder.data.local.entity.SummaryEntity
import com.twinmind.recorder.worker.SummaryWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing meeting summaries.
 * Handles summary generation scheduling and real-time updates.
 */
@Singleton
class SummaryRepository @Inject constructor(
    private val summaryDao: SummaryDao,
    @ApplicationContext private val context: Context
) {
    /** Observe summary — emits updates as SummaryWorker writes streaming tokens */
    fun getSummary(sessionId: String): Flow<SummaryEntity?> =
        summaryDao.getSummary(sessionId)

    /**
     * Enqueue summary generation via WorkManager.
     * Uses KEEP policy to prevent duplicate jobs for the same session.
     */
    fun requestSummary(sessionId: String) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "summary_$sessionId",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SummaryWorker>()
                    .setInputData(workDataOf("sessionId" to sessionId))
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
            )
    }
}