package com.twinmind.recorder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable

@Entity(tableName = "summaries")
@Immutable
data class SummaryEntity(
    @PrimaryKey val sessionId: String,
    val title: String? = null,
    val summary: String? = null,
    val actionItems: String? = null,  // JSON array string e.g. ["item1","item2"]
    val keyPoints: String? = null,    // JSON array string e.g. ["point1","point2"]
    val status: SummaryStatus = SummaryStatus.PENDING,
    val errorMessage: String? = null,
)

enum class SummaryStatus {
    PENDING,    // Not yet started
    STREAMING,  // GPT-4o is streaming tokens, partial data may be present
    DONE,       // Complete
    ERROR       // Failed, errorMessage is set
}
