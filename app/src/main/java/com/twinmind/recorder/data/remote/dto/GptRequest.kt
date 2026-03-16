package com.twinmind.recorder.data.remote.dto

data class GptRequest(
    val model: String = "gpt-4o",
    val messages: List<GptMessage>,
    val stream: Boolean = true,
    val max_tokens: Int = 2000,
    val temperature: Double = 0.3   // Lower = more deterministic structured output
)

data class GptMessage(
    val role: String,     // "system" or "user"
    val content: String
)
