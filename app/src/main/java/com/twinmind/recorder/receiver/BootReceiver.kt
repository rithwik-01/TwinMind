package com.twinmind.recorder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.twinmind.recorder.data.local.db.AppDatabase
import com.twinmind.recorder.worker.TranscriptionWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-enqueues pending transcription jobs after device reboot (Edge Case 5).
 *
 * WorkManager persists job queues across reboots by default, BUT
 * we re-query the DB and re-enqueue here as a safety net in case
 * WorkManager's own persistence fails (e.g., data wipe, fresh install
 * of same package via MY_PACKAGE_REPLACED).
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var database: AppDatabase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        CoroutineScope(Dispatchers.IO).launch {
            val pendingChunks = database.chunkDao().getPendingChunks()
            pendingChunks.forEach { chunk ->
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "transcribe_${chunk.id}",
                        ExistingWorkPolicy.KEEP,
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
        }
    }
}
