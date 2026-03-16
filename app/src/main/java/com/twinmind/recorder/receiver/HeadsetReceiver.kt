package com.twinmind.recorder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.twinmind.recorder.service.RecordingService

/**
 * Handles wired headset plug/unplug (Edge Case 3).
 *
 * When a headset connects or disconnects, Android may switch the audio
 * input source. We must re-create AudioRecord to pick up the new source.
 * This receiver fires even if the app is in background.
 */
class HeadsetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_HEADSET_PLUG) return

        val state  = intent.getIntExtra("state", -1)
        val hasMic = intent.getIntExtra("microphone", 0) == 1

        val message = when {
            state == 1 && hasMic -> "Headset with mic connected"
            state == 1           -> "Headset connected"
            else                 -> "Headset disconnected - using built-in mic"
        }

        // Forward to RecordingService to re-create AudioRecord
        context.startService(
            Intent(context, RecordingService::class.java)
                .setAction(RecordingService.ACTION_REFRESH_AUDIO_SOURCE)
                .putExtra(RecordingService.EXTRA_SOURCE_MESSAGE, message)
        )
    }
}
