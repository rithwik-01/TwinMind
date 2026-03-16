package com.twinmind.recorder.util

import android.util.Log

/**
 * Centralized logging utility for production error tracking.
 * All tags are prefixed with "TM_" for easy Logcat filtering.
 */
object DebugLogger {

    private const val PREFIX = "TM_"

    /**
     * Log an error with optional exception details.
     * Use this for production error tracking and debugging.
     */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("${PREFIX}ERROR_$tag", message, throwable)
        } else {
            Log.e("${PREFIX}ERROR_$tag", message)
        }
    }
}