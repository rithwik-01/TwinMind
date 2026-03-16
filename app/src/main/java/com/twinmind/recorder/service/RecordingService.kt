package com.twinmind.recorder.service

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.lifecycle.lifecycleScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.twinmind.recorder.data.local.dao.ChunkDao
import com.twinmind.recorder.data.local.dao.SessionDao
import com.twinmind.recorder.data.local.entity.ChunkEntity
import com.twinmind.recorder.data.local.entity.SessionEntity
import com.twinmind.recorder.data.local.entity.SessionStatus
import com.twinmind.recorder.data.local.entity.UploadStatus
import com.twinmind.recorder.datastore.SessionPreferences
import com.twinmind.recorder.util.NotificationHelper
import com.twinmind.recorder.util.PcmRingBuffer
import com.twinmind.recorder.util.SilenceDetector
import com.twinmind.recorder.util.StorageUtils
import com.twinmind.recorder.util.WavUtils
import com.twinmind.recorder.util.audioRecordMinBufferSize
import com.twinmind.recorder.util.toPcmByteArray
import com.twinmind.recorder.util.toTimerString
import com.twinmind.recorder.worker.TerminationWorker
import com.twinmind.recorder.worker.TranscriptionWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import androidx.lifecycle.LifecycleService

/**
 * Foreground service that manages audio recording with chunking, silence detection,
 * and automatic transcription scheduling.
 * 
 * Handles edge cases like phone calls, audio focus changes, Bluetooth routing,
 * low storage, and process death recovery.
 */
@AndroidEntryPoint
class RecordingService : LifecycleService() {

    @Inject lateinit var sessionDao: SessionDao
    @Inject lateinit var chunkDao: ChunkDao
    @Inject lateinit var stateHolder: ServiceStateHolder
    @Inject lateinit var prefs: SessionPreferences

    companion object {
        const val SAMPLE_RATE         = 16000
        const val CHANNEL_CONFIG      = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT        = AudioFormat.ENCODING_PCM_16BIT
        const val CHUNK_DURATION_MS   = 30_000L
        const val OVERLAP_DURATION_MS = 2_000L
        val OVERLAP_BYTES = (SAMPLE_RATE * 2 * (OVERLAP_DURATION_MS / 1000)).toInt()

        const val ACTION_START                = "ACTION_START_RECORDING"
        const val ACTION_STOP                 = "ACTION_STOP_RECORDING"
        const val ACTION_RESUME               = "ACTION_RESUME_RECORDING"
        const val ACTION_REFRESH_AUDIO_SOURCE = "ACTION_REFRESH_AUDIO_SOURCE"

        const val EXTRA_SOURCE_MESSAGE = "extra_source_message"
        const val NOTIF_CHANNEL_ID = "recording_channel"
        const val NOTIF_ID         = 1001
        const val ERROR_LOW_STORAGE = "Recording stopped - Low storage"
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job?        = null
    private var timerJob: Job?            = null

    private val ringBuffer      = PcmRingBuffer(OVERLAP_BYTES)
    private val silenceDetector = SilenceDetector(
        silenceThresholdRms = 150,
        onSilenceDetected   = { onSilenceDetected() },
        onAudioDetected     = { onAudioDetected()   }
    )

    private var pausedForCall       = false
    private var pausedForAudioFocus = false

    private var currentSessionId: String?  = null
    private var currentChunkIndex: Int     = 0
    private var chunkStartMs: Long         = 0L
    private var sessionStartMs: Long       = 0L
    private val currentChunkPcm            = ByteArrayOutputStream()

    private lateinit var audioFocusManager: AudioFocusManager
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START                -> startRecording()
            ACTION_STOP                 -> stopRecording()
            ACTION_RESUME               -> resumeAfterFocusGain()
            ACTION_REFRESH_AUDIO_SOURCE -> {
                val msg = intent.getStringExtra(EXTRA_SOURCE_MESSAGE) ?: "Audio source changed"
                recreateAudioRecord(msg)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupRecording()
        if (::audioFocusManager.isInitialized) {
            audioFocusManager.abandonFocus()
        }
        unregisterReceivers()
    }

    private fun startRecording() {
        val permissionGranted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            showErrorAndStop("Microphone permission not granted")
            return
        }

        val audioDir = StorageUtils.getAudioDir(this)
        if (!StorageUtils.hasEnoughStorage(audioDir)) {
            showErrorAndStop(ERROR_LOW_STORAGE)
            return
        }

        val sessionId = UUID.randomUUID().toString()
        currentSessionId  = sessionId
        sessionStartMs    = System.currentTimeMillis()
        currentChunkIndex = 0

        lifecycleScope.launch {
            val title = "Meeting ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())}"
            sessionDao.insert(SessionEntity(id = sessionId, title = title, startedAt = sessionStartMs))
            prefs.setActiveSessionId(sessionId)
        }

        startForeground(NOTIF_ID, NotificationHelper.buildRecordingNotification(this, "00:00"))

        audioFocusManager = AudioFocusManager(
            context              = this,
            onFocusLoss          = { pauseForAudioFocus() },
            onFocusLossTransient = { pauseForAudioFocus() },
            onFocusGain          = { resumeAfterFocusGain() }
        )
        audioFocusManager.requestFocus()

        registerPhoneStateListener()
        registerBluetoothReceiver()

        stateHolder.update { it.copy(state = RecordingState.RECORDING, sessionId = sessionId) }

        startAudioRecord()
        startChunkLoop()
        startTimer()

        enqueueTerminationWorker(sessionId)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startAudioRecord() {
        val bufferSize = audioRecordMinBufferSize()

        val audioSource = if (isEmulator()) {
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        } else {
            MediaRecorder.AudioSource.MIC
        }

        audioRecord = AudioRecord(
            audioSource,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        val state = audioRecord?.state
        if (state != AudioRecord.STATE_INITIALIZED) {
            showErrorAndStop("Microphone initialization failed")
            return
        }

        audioRecord?.startRecording()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun recreateAudioRecord(sourceChangeMessage: String) {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Thread.sleep(200)
        startAudioRecord()
        updateNotification("↔ $sourceChangeMessage")
    }

    private fun startChunkLoop() {
        val bufferSize = audioRecordMinBufferSize()
        recordingJob = lifecycleScope.launch(Dispatchers.IO) {
            chunkStartMs = System.currentTimeMillis()
            var totalBytesThisChunk = 0
            var readCount = 0

            while (isActive) {
                if (pausedForCall || pausedForAudioFocus) {
                    delay(100)
                    continue
                }

                val shortBuf = ShortArray(bufferSize / 2)
                val read = audioRecord?.read(shortBuf, 0, shortBuf.size) ?: -1

                if (read <= 0) {
                    delay(10)
                    continue
                }

                val bytes = shortBuf.toPcmByteArray(read)
                totalBytesThisChunk += bytes.size
                readCount++

                silenceDetector.feed(shortBuf, read)
                ringBuffer.write(bytes)
                currentChunkPcm.write(bytes)

                val rms = computeRms(shortBuf, read).toFloat() / 32768f
                stateHolder.update { it.copy(amplitude = minOf(rms, 1f)) }

                val elapsed = System.currentTimeMillis() - chunkStartMs
                if (elapsed >= CHUNK_DURATION_MS) {
                    totalBytesThisChunk = 0
                    readCount = 0
                    finalizeCurrentChunk()

                    val audioDir = StorageUtils.getAudioDir(this@RecordingService)
                    if (!StorageUtils.hasEnoughStorage(audioDir)) {
                        withContext(Dispatchers.Main) {
                            stopRecording(ERROR_LOW_STORAGE)
                        }
                        return@launch
                    }
                }
            }
        }
    }

    private suspend fun finalizeCurrentChunk() {
        val sessionId = currentSessionId
        if (sessionId == null) {
            return
        }

        val pcmData = currentChunkPcm.toByteArray()
        val chunkId = UUID.randomUUID().toString()
        val idx     = currentChunkIndex++

        if (pcmData.isEmpty()) {
            currentChunkPcm.reset()
            chunkStartMs = System.currentTimeMillis()
            return
        }

        val overlapBytes = ringBuffer.drain()
        val audioDir = StorageUtils.getAudioDir(this@RecordingService)
        val wavFile  = File(audioDir, "chunk_${sessionId}_${idx}.wav")

        withContext(Dispatchers.IO) {
            try {
                WavUtils.writeChunkWav(
                    overlap  = if (idx > 0) overlapBytes else ByteArray(0),
                    chunkPcm = pcmData,
                    outFile  = wavFile
                )
            } catch (e: Exception) {
                return@withContext
            }
        }

        try {
            chunkDao.insert(ChunkEntity(
                id           = chunkId,
                sessionId    = sessionId,
                index        = idx,
                filePath     = wavFile.absolutePath,
                durationMs   = CHUNK_DURATION_MS,
                uploadStatus = UploadStatus.PENDING
            ))
        } catch (e: Exception) {
            // Chunk insertion failed - transcription won't be scheduled
        }

        try {
            WorkManager.getInstance(this@RecordingService)
                .enqueueUniqueWork(
                    "transcribe_$chunkId",
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<TranscriptionWorker>()
                        .addTag("transcription")
                        .setInputData(workDataOf(
                            "chunkId"   to chunkId,
                            "sessionId" to sessionId
                        ))
                        .setConstraints(Constraints.NONE)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                        .build()
                )
        } catch (e: Exception) {
            // WorkManager enqueue failed - retry will happen on next chunk
        }

        currentChunkPcm.reset()
        chunkStartMs = System.currentTimeMillis()
    }

    private fun stopRecording(errorMessage: String? = null) {
        recordingJob?.cancel()
        timerJob?.cancel()

        stateHolder.update { it.copy(state = RecordingState.FINALIZING) }

        lifecycleScope.launch {
            if (currentChunkPcm.size() > 0) {
                finalizeCurrentChunk()
            }

            val sessionId = currentSessionId
            if (sessionId != null) {
                if (errorMessage != null) {
                    sessionDao.setError(sessionId, SessionStatus.ERROR, errorMessage)
                } else {
                    sessionDao.finalizeSession(sessionId, System.currentTimeMillis(), SessionStatus.COMPLETED)
                }
                prefs.clearActiveSession()
            }

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            if (::audioFocusManager.isInitialized) {
                audioFocusManager.abandonFocus()
            }

            if (errorMessage != null) {
                stateHolder.update { it.copy(state = RecordingState.ERROR, errorMessage = errorMessage) }
                notificationManager.notify(
                    NOTIF_ID,
                    NotificationHelper.buildErrorNotification(this@RecordingService, errorMessage)
                )
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                stateHolder.reset()
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
            stopSelf()
        }
    }

    private fun showErrorAndStop(message: String) {
        if (currentSessionId != null) {
            stopRecording(message)
        } else {
            notificationManager.notify(
                NOTIF_ID,
                NotificationHelper.buildErrorNotification(this, message)
            )
            stateHolder.reset()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun registerPhoneStateListener() {
        val tm = getSystemService(TelephonyManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = handleCallState(state)
            }
            tm.registerTelephonyCallback(mainExecutor, cb)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallState(state)
                }
            }
            @Suppress("DEPRECATION")
            tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> pauseForPhoneCall()
            TelephonyManager.CALL_STATE_IDLE    -> resumeAfterPhoneCall()
        }
    }

    private fun pauseForPhoneCall() {
        if (pausedForCall) return
        pausedForCall = true
        audioRecord?.stop()
        stateHolder.update { it.copy(state = RecordingState.PAUSED_PHONE_CALL) }
        updateNotification(NotificationHelper.buildPhoneCallNotification(this))
    }

    private fun resumeAfterPhoneCall() {
        pausedForCall = false
        if (!pausedForAudioFocus) doResume()
    }

    private fun pauseForAudioFocus() {
        if (pausedForAudioFocus) return
        pausedForAudioFocus = true
        audioRecord?.stop()
        stateHolder.update { it.copy(state = RecordingState.PAUSED_AUDIO_FOCUS) }
        updateNotification(NotificationHelper.buildPausedAudioFocusNotification(this))
    }

    private fun resumeAfterFocusGain() {
        pausedForAudioFocus = false
        if (!pausedForCall) doResume()
    }

    private fun doResume() {
        silenceDetector.reset()
        audioRecord?.startRecording()
        stateHolder.update { it.copy(state = RecordingState.RECORDING, warning = null) }
        updateNotification("Recording...")
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                    recreateAudioRecord("Bluetooth audio source changed")
                }
            }
        }
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun onSilenceDetected() {
        val warning = "No audio detected - Check microphone"
        stateHolder.update { it.copy(warning = warning) }
        updateNotification(warning)
    }

    private fun onAudioDetected() {
        stateHolder.update { it.copy(warning = null) }
    }

    private fun enqueueTerminationWorker(sessionId: String) {
        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "termination_$sessionId",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<TerminationWorker>()
                    .setInputData(workDataOf("sessionId" to sessionId))
                    .build()
            )
    }

    private fun startTimer() {
        timerJob = lifecycleScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - sessionStartMs
                stateHolder.update { it.copy(elapsedMs = elapsed) }

                val elapsedStr = elapsed.toTimerString()
                val statusText = when (stateHolder.status.value.state) {
                    RecordingState.RECORDING          -> "Recording"
                    RecordingState.PAUSED_PHONE_CALL  -> "Paused - Phone call"
                    RecordingState.PAUSED_AUDIO_FOCUS -> "Paused - Audio focus lost"
                    else                              -> "Recording"
                }

                if (stateHolder.status.value.state == RecordingState.RECORDING
                    && stateHolder.status.value.warning == null) {
                    val notif = NotificationHelper.buildRecordingNotification(
                        this@RecordingService, elapsedStr, statusText
                    )
                    notificationManager.notify(NOTIF_ID, notif)
                }
                delay(1000L)
            }
        }
    }

    private fun updateNotification(statusText: String) {
        val elapsed = stateHolder.status.value.elapsedMs.toTimerString()
        val notif   = NotificationHelper.buildRecordingNotification(this, elapsed, statusText)
        notificationManager.notify(NOTIF_ID, notif)
    }

    private fun updateNotification(notification: android.app.Notification) {
        notificationManager.notify(NOTIF_ID, notification)
    }

    private fun computeRms(buffer: ShortArray, count: Int): Double {
        if (count == 0) return 0.0
        var sum = 0.0
        for (i in 0 until count) {
            val s = buffer[i].toDouble()
            sum += s * s
        }
        return kotlin.math.sqrt(sum / count)
    }

    private fun cleanupRecording() {
        recordingJob?.cancel()
        timerJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun unregisterReceivers() {
        try { unregisterReceiver(bluetoothReceiver) } catch (_: Exception) { }
    }

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.contains("generic") ||
                Build.FINGERPRINT.contains("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK")
}