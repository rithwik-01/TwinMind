package com.twinmind.recorder.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.twinmind.recorder.data.local.dao.ChunkDao
import com.twinmind.recorder.data.local.dao.SessionDao
import com.twinmind.recorder.data.local.entity.UploadStatus
import com.twinmind.recorder.data.remote.WhisperApi
import com.twinmind.recorder.data.repository.TranscriptRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Worker that uploads audio chunks to OpenAI Whisper for transcription.
 * Handles network retries, deduplication, and automatic transcript stitching.
 */
@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val chunkDao: ChunkDao,
    private val sessionDao: SessionDao,
    private val whisperApi: WhisperApi,
    private val transcriptRepository: TranscriptRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val chunkId   = inputData.getString("chunkId")
        val sessionId = inputData.getString("sessionId")

        if (chunkId == null || sessionId == null) {
            return Result.failure()
        }

        val chunk = chunkDao.getChunk(chunkId)
        if (chunk == null) {
            return Result.failure()
        }

        if (chunk.uploadStatus == UploadStatus.DONE) {
            return Result.success()
        }

        val wavFile = File(chunk.filePath)
        if (!wavFile.exists() || wavFile.length() < 500) {
            chunkDao.updateStatus(chunkId, UploadStatus.FAILED)
            return Result.failure()
        }

        val connectivityManager = applicationContext
            .getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        if (!hasInternet) {
            return Result.retry()
        }

        chunkDao.updateStatus(chunkId, UploadStatus.UPLOADING)

        return try {
            val filePart = MultipartBody.Part.createFormData(
                "file",
                wavFile.name,
                wavFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            )

            val responseBody = whisperApi.transcribe(
                file     = filePart,
                model    = "whisper-1".toRequestBody("text/plain".toMediaTypeOrNull()),
                format   = "text".toRequestBody("text/plain".toMediaTypeOrNull()),
                language = "en".toRequestBody("text/plain".toMediaTypeOrNull())
            )

            val transcript = responseBody.string().trim()
            chunkDao.saveTranscript(chunkId, transcript)

            val completedChunks = chunkDao.getCompletedChunks(sessionId)
            val stitched = stitchWithDedup(completedChunks.map { it.transcript ?: "" })
            sessionDao.updateTranscript(sessionId, stitched)

            Result.success()

        } catch (e: HttpException) {
            chunkDao.incrementRetry(chunkId)
            chunkDao.updateStatus(chunkId, UploadStatus.FAILED)
            if (runAttemptCount < 3) Result.retry() else Result.failure()

        } catch (e: UnknownHostException) {
            chunkDao.incrementRetry(chunkId)
            chunkDao.updateStatus(chunkId, UploadStatus.FAILED)
            Result.retry()

        } catch (e: SocketTimeoutException) {
            chunkDao.incrementRetry(chunkId)
            chunkDao.updateStatus(chunkId, UploadStatus.FAILED)
            Result.retry()

        } catch (e: Exception) {
            chunkDao.incrementRetry(chunkId)
            chunkDao.updateStatus(chunkId, UploadStatus.FAILED)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    /**
     * Stitch transcript chunks while removing duplicate words at overlap boundaries.
     * Each chunk includes 2 seconds of overlap from the previous chunk for continuity.
     */
    private fun stitchWithDedup(transcripts: List<String>): String {
        if (transcripts.isEmpty()) return ""

        val builder = StringBuilder()

        transcripts.forEachIndexed { index, text ->
            val cleaned = text.trim()
            if (cleaned.isEmpty()) return@forEachIndexed

            if (index == 0) {
                builder.append(cleaned)
                return@forEachIndexed
            }

            val accumulated = builder.toString().split(" ").filter { it.isNotBlank() }
            val incoming    = cleaned.split(" ").filter { it.isNotBlank() }
            val windowSize  = minOf(15, accumulated.size, incoming.size)

            var overlapEnd = 0
            for (startIdx in incoming.indices) {
                val incomingWindow   = incoming.drop(startIdx).take(windowSize)
                val accumulatedTail  = accumulated.takeLast(incomingWindow.size)
                if (incomingWindow.map { it.lowercase() } == accumulatedTail.map { it.lowercase() }) {
                    overlapEnd = startIdx + incomingWindow.size
                    break
                }
            }

            val deduped = incoming.drop(overlapEnd).joinToString(" ")
            if (deduped.isNotBlank()) {
                builder.append(" ").append(deduped)
            }
        }

        return builder.toString().trim()
    }
}