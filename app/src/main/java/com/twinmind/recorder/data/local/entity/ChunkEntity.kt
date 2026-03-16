package com.twinmind.recorder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "chunks",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class ChunkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val index: Int,               // 0-based. ORDER BY index ASC for correct transcript stitching.
    val filePath: String,         // Absolute path to the .wav file on disk
    val durationMs: Long = 0L,
    val uploadStatus: UploadStatus = UploadStatus.PENDING,
    val transcript: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class UploadStatus {
    PENDING,    // Saved to disk, not yet uploaded
    UPLOADING,  // In-flight Whisper API call
    DONE,       // Transcript received and saved
    FAILED      // Max retries exhausted
}
