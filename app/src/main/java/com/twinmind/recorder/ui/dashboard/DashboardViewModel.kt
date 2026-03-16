package com.twinmind.recorder.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinmind.recorder.data.local.dao.SessionDao
import com.twinmind.recorder.data.local.entity.SessionEntity
import com.twinmind.recorder.data.repository.RecordingRepository
import com.twinmind.recorder.service.RecordingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val sessionDao: SessionDao
) : ViewModel() {

    val sessions: StateFlow<List<SessionEntity>> = recordingRepository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val recordingStatus: StateFlow<RecordingStatus> = recordingRepository.recordingStatus

    fun startNewRecording() = recordingRepository.startRecording()

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            // Delete audio files from disk
            try {
                File(session.id).parentFile?.listFiles { f ->
                    f.name.contains(session.id)
                }?.forEach { it.delete() }
            } catch (_: Exception) {}
            // Delete from DB (CASCADE deletes chunks + summary)
            sessionDao.delete(session.id)
        }
    }
}
