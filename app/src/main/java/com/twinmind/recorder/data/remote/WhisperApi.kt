package com.twinmind.recorder.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit interface for OpenAI Whisper speech-to-text.
 *
 * Endpoint: POST https://api.openai.com/v1/audio/transcriptions
 * Auth: Bearer token injected by NetworkModule OkHttp interceptor
 *
 * The file part must be a WAV file (with proper 44-byte header).
 * response_format = "text" returns plain UTF-8 transcript, not JSON.
 */
interface WhisperApi {

    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribe(
        @Part file: MultipartBody.Part,               // WAV audio binary
        @Part("model") model: RequestBody,            // "whisper-1"
        @Part("response_format") format: RequestBody, // "text"
        @Part("language") language: RequestBody,      // "en"
    ): ResponseBody  // Returns plain UTF-8 transcript string
}
