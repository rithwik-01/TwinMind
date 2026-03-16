package com.twinmind.recorder.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("session_prefs")

/**
 * Persists the active session ID across process death.
 * On restart, TerminationWorker reads this to re-enqueue pending transcription jobs.
 */
@Singleton
class SessionPreferences @Inject constructor(
    private val context: Context
) {
    private val ACTIVE_SESSION_KEY = stringPreferencesKey("active_session_id")

    suspend fun setActiveSessionId(id: String) {
        context.dataStore.edit { it[ACTIVE_SESSION_KEY] = id }
    }

    suspend fun getActiveSessionId(): String? =
        context.dataStore.data.first()[ACTIVE_SESSION_KEY]

    suspend fun clearActiveSession() {
        context.dataStore.edit { it.remove(ACTIVE_SESSION_KEY) }
    }
}
