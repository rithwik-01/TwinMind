package com.twinmind.recorder.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.twinmind.recorder.MainActivity
import com.twinmind.recorder.R

/**
 * Builds all notification variants used by RecordingService.
 *
 * Three variants:
 *   1. Recording   — persistent, "Stop" action only
 *   2. Paused      — persistent, "Resume" + "Stop" actions (audio focus loss)
 *   3. PhoneCall   — persistent, "Stop" action, different title
 */
object NotificationHelper {

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationConstants.NOTIF_CHANNEL_ID,
                "Recording",
                NotificationManager.IMPORTANCE_LOW  // No sound, no heads-up
            ).apply {
                description = "Shows recording status and controls"
                setShowBadge(false)
            }
        )
    }

    fun buildRecordingNotification(
        context: Context,
        elapsed: String,
        statusText: String = "Recording..."
    ): Notification {
        val stopIntent = buildServiceIntent(context, "ACTION_STOP_RECORDING", requestCode = 0)
        val openIntent = buildOpenAppIntent(context)

        return NotificationCompat.Builder(context, NotificationConstants.NOTIF_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(statusText)
            .setContentText("Recording time: $elapsed")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Paused notification for audio focus loss (Edge Case 2).
     * Shows BOTH Resume and Stop actions.
     */
    fun buildPausedAudioFocusNotification(context: Context): Notification {
        val resumeIntent = buildServiceIntent(context, "ACTION_RESUME_RECORDING", requestCode = 1)
        val stopIntent   = buildServiceIntent(context, "ACTION_STOP_RECORDING",   requestCode = 2)
        val openIntent   = buildOpenAppIntent(context)

        return NotificationCompat.Builder(context, NotificationConstants.NOTIF_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Paused – Audio focus lost")
            .setContentText("Another app is using audio")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_mic, "Resume", resumeIntent)
            .addAction(R.drawable.ic_stop, "Stop",  stopIntent)
            .build()
    }

    /**
     * Paused notification for phone calls (Edge Case 1).
     * Only Stop action — recording resumes automatically when call ends.
     */
    fun buildPhoneCallNotification(context: Context): Notification {
        val stopIntent = buildServiceIntent(context, "ACTION_STOP_RECORDING", requestCode = 3)
        val openIntent = buildOpenAppIntent(context)

        return NotificationCompat.Builder(context, NotificationConstants.NOTIF_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Paused – Phone call")
            .setContentText("Recording will resume when call ends")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .build()
    }

    fun buildErrorNotification(context: Context, message: String): Notification {
        return NotificationCompat.Builder(context, NotificationConstants.NOTIF_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Recording stopped")
            .setContentText(message)
            .setAutoCancel(true)
            .build()
    }

    private fun buildServiceIntent(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            context,
            requestCode,
            Intent().setClassName(context, "com.twinmind.recorder.service.RecordingService").setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun buildOpenAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            99,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )
}
