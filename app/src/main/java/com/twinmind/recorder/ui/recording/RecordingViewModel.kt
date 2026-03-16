package com.twinmind.recorder.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinmind.recorder.data.local.entity.SessionEntity
import com.twinmind.recorder.data.repository.RecordingRepository
import com.twinmind.recorder.data.repository.TranscriptRepository
import com.twinmind.recorder.service.RecordingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val transcriptRepository: TranscriptRepository
) : ViewModel() {

    val recordingStatus: StateFlow<RecordingStatus> = recordingRepository.recordingStatus

    fun getSession(sessionId: String): Flow<SessionEntity?> =
        recordingRepository.getSession(sessionId)

    /**
     * Observe how many chunks are still waiting to be transcribed.
     * Used to show "Transcribing X chunks..." progress in UI.
     */
    fun getPendingChunkCount(sessionId: String): Flow<Int> =
        transcriptRepository.observePendingCount(sessionId)

    fun stopRecording() = recordingRepository.stopRecording()
}
