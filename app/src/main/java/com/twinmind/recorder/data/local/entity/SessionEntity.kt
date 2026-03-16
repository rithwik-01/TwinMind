package com.twinmind.recorder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val status: SessionStatus = SessionStatus.RECORDING,
    val errorReason: String? = null,
    val transcript: String? = null,   // stitched full transcript from all chunks
)

enum class SessionStatus {
    RECORDING, PAUSED, COMPLETED, ERROR
}
