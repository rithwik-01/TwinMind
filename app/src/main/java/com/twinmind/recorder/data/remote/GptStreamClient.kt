package com.twinmind.recorder.data.remote

import com.google.gson.Gson
import com.twinmind.recorder.data.remote.dto.GptRequest
import com.twinmind.recorder.data.remote.dto.GptMessage
import com.twinmind.recorder.data.remote.dto.StreamChunk
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Raw OkHttp client for GPT-4o Server-Sent Events (SSE) streaming.
 * Uses a custom OkHttp client with infinite read timeout for long-running streams.
 */
@Singleton
class GptStreamClient @Inject constructor(
    @Named("streaming") private val okHttp: OkHttpClient,
    private val gson: Gson
) {

    companion object {
        private const val GPT_URL = "https://api.openai.com/v1/chat/completions"

        private val SYSTEM_PROMPT = """
            You are a professional meeting summarizer. 
            Given a meeting transcript, analyze the content and return a structured JSON object.
            
            You MUST return ONLY a valid JSON object with exactly these four fields:
            {
              "title": "A concise, descriptive title for this meeting (max 10 words)",
              "summary": "A clear paragraph summarizing the key discussion points and outcomes",
              "actionItems": ["Action item 1", "Action item 2", "Action item 3"],
              "keyPoints": ["Key point 1", "Key point 2", "Key point 3"]
            }
            
            Rules:
            - Return ONLY the JSON object. No markdown. No backticks. No explanation.
            - actionItems and keyPoints must be JSON arrays of strings
            - If the transcript is unclear or very short, do your best with available context
        """.trimIndent()
    }

    /**
     * Streams GPT-4o summary generation as a Flow of text tokens.
     * Callers accumulate tokens and periodically parse the growing JSON.
     */
    fun streamSummary(transcript: String): Flow<String> = callbackFlow {

        val requestPayload = gson.toJson(
            GptRequest(
                messages = listOf(
                    GptMessage(role = "system", content = SYSTEM_PROMPT),
                    GptMessage(role = "user", content = "Meeting Transcript:\n\n$transcript")
                )
            )
        )

        val request = Request.Builder()
            .url(GPT_URL)
            .post(requestPayload.toRequestBody("application/json".toMediaType()))
            .build()

        val call = okHttp.newCall(request)

        call.enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    close(Exception("GPT-4o API error: HTTP ${response.code} — ${response.message}"))
                    return
                }

                val body = response.body
                if (body == null) {
                    close(Exception("Empty response body from GPT-4o"))
                    return
                }

                body.source()?.use { source ->
                    try {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break

                            if (!line.startsWith("data: ")) continue

                            val data = line.removePrefix("data: ").trim()

                            if (data == "[DONE]") {
                                close()
                                return
                            }

                            try {
                                val chunk = gson.fromJson(data, StreamChunk::class.java)
                                val token = chunk.choices
                                    ?.firstOrNull()
                                    ?.delta
                                    ?.content
                                    ?: continue

                                trySend(token)
                            } catch (_: Exception) {
                                // Malformed line — skip silently
                            }
                        }
                    } catch (e: Exception) {
                        close(e)
                        return
                    }
                    close()
                }
            }
        })

        awaitClose { call.cancel() }
    }
}