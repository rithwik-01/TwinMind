package com.twinmind.recorder.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.twinmind.recorder.data.local.dao.SessionDao
import com.twinmind.recorder.data.local.dao.SummaryDao
import com.twinmind.recorder.data.local.entity.SummaryEntity
import com.twinmind.recorder.data.local.entity.SummaryStatus
import com.twinmind.recorder.data.remote.GptStreamClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

/**
 * Worker that generates structured summaries from transcripts using GPT-4o streaming.
 * Updates the database in real-time as tokens arrive for progressive UI updates.
 */
@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val summaryDao: SummaryDao,
    private val sessionDao: SessionDao,
    private val gptStreamClient: GptStreamClient,
    private val gson: Gson
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString("sessionId") ?: return Result.failure()

        val existing = summaryDao.getSummaryOnce(sessionId)
        if (existing?.status == SummaryStatus.DONE) {
            return Result.success()
        }

        val session    = sessionDao.getSessionOnce(sessionId)
        val transcript = session?.transcript

        if (transcript.isNullOrBlank()) {
            return if (runAttemptCount < 5) Result.retry() else Result.failure()
        }

        summaryDao.insert(
            SummaryEntity(sessionId = sessionId, status = SummaryStatus.STREAMING)
        )

        val tokenBuffer = StringBuilder()
        var lastSaveLength = 0
        var firstTokenReceived = false

        return try {
            gptStreamClient.streamSummary(transcript)
                .onEach { token ->
                    if (!firstTokenReceived) {
                        firstTokenReceived = true
                    }
                    
                    tokenBuffer.append(token)
                    if (tokenBuffer.length - lastSaveLength > 50) {
                        tryParseAndSave(sessionId, tokenBuffer.toString(), SummaryStatus.STREAMING)
                        lastSaveLength = tokenBuffer.length
                    }
                }
                .catch { e ->
                    summaryDao.updateStatus(
                        sessionId,
                        SummaryStatus.ERROR,
                        e.message ?: "Unknown streaming error"
                    )
                    throw e
                }
                .collect()

            val parsed = parseJson(tokenBuffer.toString())
            summaryDao.update(
                SummaryEntity(
                    sessionId   = sessionId,
                    title       = parsed["title"],
                    summary     = parsed["summary"],
                    actionItems = parsed["actionItems"],
                    keyPoints   = parsed["keyPoints"],
                    status      = SummaryStatus.DONE
                )
            )

            Result.success()

        } catch (e: Exception) {
            summaryDao.updateStatus(
                sessionId,
                SummaryStatus.ERROR,
                buildErrorMessage(e)
            )
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private suspend fun tryParseAndSave(sessionId: String, raw: String, status: SummaryStatus) {
        try {
            val parsed = parseJson(raw)
            summaryDao.update(
                SummaryEntity(
                    sessionId   = sessionId,
                    title       = parsed["title"],
                    summary     = parsed["summary"],
                    actionItems = parsed["actionItems"],
                    keyPoints   = parsed["keyPoints"],
                    status      = status
                )
            )
        } catch (_: Exception) {
            // Partial JSON — not yet parseable
        }
    }

    /**
     * Parse the GPT-4o JSON response.
     * Strips markdown fences in case the model ignores system prompt instructions.
     */
    private fun parseJson(raw: String): Map<String, String?> {
        val clean = raw
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val startIdx = clean.indexOf('{')
        if (startIdx == -1) return emptyMap()
        val jsonStr = clean.substring(startIdx)

        val obj = gson.fromJson(jsonStr, JsonObject::class.java)
        return mapOf(
            "title"       to obj.get("title")?.asString,
            "summary"     to obj.get("summary")?.asString,
            "actionItems" to obj.get("actionItems")?.toString(),
            "keyPoints"   to obj.get("keyPoints")?.toString()
        )
    }

    private fun buildErrorMessage(e: Exception): String = when {
        e.message?.contains("401") == true -> "API key invalid or expired"
        e.message?.contains("429") == true -> "Rate limit reached - please try again"
        e.message?.contains("500") == true -> "OpenAI server error - please try again"
        e.message?.contains("network") == true -> "Network error - check your connection"
        else -> e.message ?: "Failed to generate summary"
    }
}