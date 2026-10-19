package com.unravel.core.database.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import app.cash.turbine.test
import com.unravel.core.database.datastore.ActiveSessionSerializer
import com.unravel.core.database.model.ActiveSessionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.system.measureNanoTime

class SessionRepositoryTest {

    private lateinit var testFile: File
    private lateinit var testScope: CoroutineScope
    private lateinit var dataStore: DataStore<ActiveSessionPreferences>
    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        val uniqueName = "active_session_test_${System.currentTimeMillis()}_${System.nanoTime()}.pb"
        testFile = File(System.getProperty("java.io.tmpdir"), uniqueName)
        if (testFile.exists()) {
            testFile.delete()
        }

        testScope = CoroutineScope(Dispatchers.IO)
        dataStore = DataStoreFactory.create(
            serializer = ActiveSessionSerializer,
            scope = testScope,
            produceFile = { testFile }
        )
        repository = SessionRepositoryImpl(dataStore, testScope)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        if (testFile.exists()) {
            testFile.delete()
        }
        val tmpFile = File("${testFile.absolutePath}.tmp")
        if (tmpFile.exists()) {
            tmpFile.delete()
        }
    }

    @Test
    fun getActiveSessionReturnsWithinSubMillisecondLatency() = runTest {
        val session = ActiveSessionPreferences(
            algorithmId = "two_pointers",
            stepIndex = 15,
            playbackSpeed = 1.5f,
            datasetJson = "{\"array\":[1,2,3]}",
            timestampEpochMs = 123456789L
        )

        repository.saveActiveSession(session)

        // Read immediately from in-memory StateFlow
        val elapsedNanos = measureNanoTime {
            val cached = repository.getActiveSession()
            assertEquals("two_pointers", cached.algorithmId)
            assertEquals(15, cached.stepIndex)
        }

        // Sub-millisecond latency check (< 1ms = 1,000,000 ns)
        assertTrue("Latency was $elapsedNanos ns, expected < 1ms", elapsedNanos < 1_000_000L)
    }

    @Test
    fun activeSessionFlowEmitsUpdatesReactively() = runTest {
        repository.activeSessionFlow.test {
            // Initial default
            val item0 = awaitItem()
            assertEquals("", item0.algorithmId)

            // Save new session
            val session1 = ActiveSessionPreferences(
                algorithmId = "bubble_sort",
                stepIndex = 8,
                playbackSpeed = 2.0f,
                datasetJson = "{\"items\":[10,20]}",
                timestampEpochMs = 9999L
            )
            repository.saveActiveSession(session1)
            val item1 = awaitItem()
            assertEquals("bubble_sort", item1.algorithmId)
            assertEquals(8, item1.stepIndex)

            // Clear session
            repository.clearSession()
            val item2 = awaitItem()
            assertEquals("", item2.algorithmId)
            assertEquals(0, item2.stepIndex)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
