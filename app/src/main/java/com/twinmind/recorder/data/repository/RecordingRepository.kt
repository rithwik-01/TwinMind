package com.twinmind.recorder.data.repository

import android.content.Context
import android.content.Intent
import android.os.Build
import com.twinmind.recorder.data.local.dao.SessionDao
import com.twinmind.recorder.data.local.entity.SessionEntity
import com.twinmind.recorder.service.RecordingService
import com.twinmind.recorder.service.RecordingStatus
import com.twinmind.recorder.service.ServiceStateHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val stateHolder: ServiceStateHolder,
    @ApplicationContext private val context: Context
) {
    /** All sessions for Dashboard, newest first */
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    /** Live recording state — observed by RecordingViewModel and DashboardViewModel */
    val recordingStatus: StateFlow<RecordingStatus> = stateHolder.status

    fun startRecording() {
        val intent = Intent(context, RecordingService::class.java)
            .setAction(RecordingService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopRecording() {
        val intent = Intent(context, RecordingService::class.java)
            .setAction(RecordingService.ACTION_STOP)
        context.startService(intent)
    }

    fun getSession(id: String): Flow<SessionEntity?> = sessionDao.getSession(id)
}
