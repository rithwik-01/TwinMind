package com.twinmind.recorder.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.twinmind.recorder.data.local.dao.ChunkDao
import com.twinmind.recorder.datastore.SessionPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Process death recovery worker (Edge Case 5).
 *
 * This worker is enqueued by RecordingService at the START of each recording session
 * (not at the end). Its job is to run if the process is killed mid-recording.
 *
 * When the process dies:
 *   1. RecordingService is killed (last chunk is partially written)
 *   2. WorkManager persists the TerminationWorker in its database
 *   3. On next app launch (or reboot), WorkManager runs this worker
 *   4. Worker queries Room for all PENDING/FAILED chunks for this session
 *   5. Re-enqueues TranscriptionWorker for each one
 *
 * Why WorkManager and not just START_STICKY?
 * START_STICKY only restarts the service with a null intent — you'd need to
 * re-derive which chunks need processing. WorkManager persists inputs across
 * process death and device reboots, making it the correct tool here.
 */
@HiltWorker
class TerminationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val chunkDao: ChunkDao,
    private val prefs: SessionPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString("sessionId")
            ?: prefs.getActiveSessionId()
            ?: return Result.success() // No active session, nothing to recover

        val pendingChunks = chunkDao.getPendingChunks()
            .filter { it.sessionId == sessionId }

        if (pendingChunks.isEmpty()) return Result.success()

        // Re-enqueue each pending chunk for transcription
        pendingChunks.forEach { chunk ->
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(
                    "transcribe_${chunk.id}",
                    ExistingWorkPolicy.KEEP,  // Don't duplicate if already queued
                    OneTimeWorkRequestBuilder<TranscriptionWorker>()
                        .setInputData(workDataOf(
                            "chunkId"   to chunk.id,
                            "sessionId" to chunk.sessionId
                        ))
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                )
        }

        // Clear the active session flag — recovery is complete
        prefs.clearActiveSession()

        return Result.success()
    }
}
