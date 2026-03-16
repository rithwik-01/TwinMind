package com.twinmind.recorder.data.remote.dto

/**
 * Represents one SSE data line from GPT-4o streaming response.
 *
 * Each line looks like:
 *   data: {"choices":[{"delta":{"content":"Hello"},"index":0}]}
 *
 * When stream ends:
 *   data: [DONE]
 */
data class StreamChunk(
    val choices: List<Choice>?
) {
    data class Choice(
        val delta: Delta?,
        val index: Int = 0
    )

    data class Delta(
        val content: String?,
        val role: String? = null
    )
}
