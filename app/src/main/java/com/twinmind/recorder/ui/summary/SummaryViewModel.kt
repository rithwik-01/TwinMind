package com.twinmind.recorder.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.twinmind.recorder.data.local.entity.SessionEntity
import com.twinmind.recorder.data.local.entity.SummaryEntity
import com.twinmind.recorder.data.repository.RecordingRepository
import com.twinmind.recorder.data.repository.SummaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the Summary screen.
 * Manages summary generation state and real-time updates from streaming GPT responses.
 */
@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val summaryRepository: SummaryRepository,
    private val recordingRepository: RecordingRepository,
    private val gson: Gson
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val uiState: StateFlow<SummaryUiState> =
        summaryRepository.getSummary(sessionId)
            .debounce(100)
            .distinctUntilChangedBy { entity ->
                "${entity?.status}_${entity?.title?.length}_${entity?.summary?.length}" +
                        "_${entity?.actionItems?.length}_${entity?.keyPoints?.length}"
            }
            .map { entity -> entity.toUiState() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SummaryUiState()
            )

    val session: Flow<SessionEntity?> =
        recordingRepository.getSession(sessionId)

    /** Request summary generation (or retry if previously failed) */
    fun requestSummary() =
        summaryRepository.requestSummary(sessionId)

    /** Parse the stored JSON array string into a Kotlin List */
    private fun parseList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(json, Array<String>::class.java).toList()
        } catch (_: Exception) {
            json.lines().filter { it.isNotBlank() }
        }
    }

    private fun SummaryEntity?.toUiState(): SummaryUiState {
        if (this == null) return SummaryUiState()
        return SummaryUiState(
            title        = title,
            summary      = summary,
            actionItems  = parseList(actionItems).toImmutableList(),
            keyPoints    = parseList(keyPoints).toImmutableList(),
            status       = status,
            errorMessage = errorMessage
        )
    }
}