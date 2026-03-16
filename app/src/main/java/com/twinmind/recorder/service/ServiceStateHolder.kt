package com.twinmind.recorder.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that bridges RecordingService state to ViewModels.
 *
 * Why a singleton and not binding/AIDL?
 * - Simpler: no ServiceConnection boilerplate
 * - Safe: Hilt ensures the same instance in both service and ViewModel
 * - The service writes to _status; ViewModels observe status read-only
 */
@Singleton
class ServiceStateHolder @Inject constructor() {

    private val _status = MutableStateFlow(RecordingStatus())
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    fun update(transform: (RecordingStatus) -> RecordingStatus) {
        _status.value = transform(_status.value)
    }

    fun reset() {
        _status.value = RecordingStatus()
    }
}

// ─── State model ───────────────────────────────────────────────────────────

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED_PHONE_CALL,     // Edge case 1
    PAUSED_AUDIO_FOCUS,    // Edge case 2
    FINALIZING,            // Saving last chunk, stopping
    ERROR                  // Edge case 4 — low storage or mic failure
}

data class RecordingStatus(
    val state: RecordingState = RecordingState.IDLE,
    val sessionId: String? = null,
    val elapsedMs: Long = 0L,
    val amplitude: Float = 0f,    // 0.0–1.0, drives waveform animation
    val warning: String? = null,  // e.g. "No audio detected - Check microphone"
    val errorMessage: String? = null // e.g. "Recording stopped - Low storage"
)