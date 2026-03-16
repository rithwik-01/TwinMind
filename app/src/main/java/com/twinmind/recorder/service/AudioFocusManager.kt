package com.twinmind.recorder.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/**
 * Wraps audio focus request/abandon lifecycle.
 *
 * AUDIOFOCUS_GAIN         → request exclusive focus (we're recording)
 * AUDIOFOCUS_LOSS         → another app took focus permanently → pause
 * AUDIOFOCUS_LOSS_TRANSIENT → brief loss (phone call, nav prompt) → pause, auto-resume
 * AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK → keep recording (navigation voice, etc.)
 */
class AudioFocusManager(
    private val context: Context,
    private val onFocusLoss: () -> Unit,
    private val onFocusLossTransient: () -> Unit,
    private val onFocusGain: () -> Unit
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var focusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK              -> onFocusGain()
            AudioManager.AUDIOFOCUS_LOSS              -> onFocusLoss()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT    -> onFocusLossTransient()
            // DUCK = keep recording, other app lowers its volume
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { /* intentionally no-op */ }
        }
    }

    fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }
}
