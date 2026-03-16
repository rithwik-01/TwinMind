package com.twinmind.recorder.ui.summary

import androidx.compose.runtime.Immutable
import com.twinmind.recorder.data.local.entity.SummaryStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class SummaryUiState(
    val title: String? = null,
    val summary: String? = null,
    val actionItems: ImmutableList<String> = persistentListOf(),
    val keyPoints: ImmutableList<String> = persistentListOf(),
    val status: SummaryStatus? = null,
    val errorMessage: String? = null
)
