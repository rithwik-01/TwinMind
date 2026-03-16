package com.twinmind.recorder.util

/**
 * Constants shared between NotificationHelper and RecordingService.
 * Extracted to avoid circular dependency (RecordingService is implemented in Prompt 06).
 */
object NotificationConstants {
    const val NOTIF_CHANNEL_ID = "recording_channel"
    const val NOTIF_ID         = 1001
}
