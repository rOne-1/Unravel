package com.unravel.core.database.repository

import androidx.datastore.core.DataStore
import com.unravel.core.database.model.ActiveSessionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repository interface providing instant (< 1ms) in-memory session resume and asynchronous persistence.
 */
interface SessionRepository {
    /**
     * Hot reactive [StateFlow] emitting updates to the active session.
     */
    val activeSessionFlow: StateFlow<ActiveSessionPreferences>

    /**
     * Reads the current in-memory cached session instantly without blocking (< 1ms latency).
     */
    fun getActiveSession(): ActiveSessionPreferences

    /**
     * Persists the active session state into the underlying Proto DataStore.
     * Updates the in-memory [StateFlow] immediately and writes to DataStore asynchronously.
     */
    suspend fun saveActiveSession(session: ActiveSessionPreferences)

    /**
     * Clears the active session and resets it to the default empty state.
     */
    suspend fun clearSession()
}

/**
 * Concrete implementation of [SessionRepository] powered by Proto DataStore with hot in-memory caching.
 */
class SessionRepositoryImpl(
    private val dataStore: DataStore<ActiveSessionPreferences>,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : SessionRepository {

    private val _activeSessionFlow = MutableStateFlow(ActiveSessionPreferences())
    override val activeSessionFlow: StateFlow<ActiveSessionPreferences> = _activeSessionFlow.asStateFlow()

    init {
        // Eagerly populate and sync in-memory StateFlow from persisted DataStore state
        scope.launch {
            dataStore.data.collect { persisted ->
                _activeSessionFlow.value = persisted
            }
        }
    }

    override fun getActiveSession(): ActiveSessionPreferences {
        // Returns hot in-memory state in sub-millisecond latency (< 1ms)
        return _activeSessionFlow.value
    }

    override suspend fun saveActiveSession(session: ActiveSessionPreferences) {
        // Update in-memory cache immediately
        _activeSessionFlow.value = session
        // Persist to DataStore asynchronously
        scope.launch {
            runCatching {
                dataStore.updateData { session }
            }
        }
    }

    override suspend fun clearSession() {
        val blank = ActiveSessionPreferences()
        _activeSessionFlow.value = blank
        scope.launch {
            runCatching {
                dataStore.updateData { blank }
            }
        }
    }
}
