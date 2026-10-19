package com.unravel.core.database.datastore

import androidx.datastore.core.CorruptionException
import com.unravel.core.database.model.ActiveSessionPreferences
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ActiveSessionSerializerTest {

    @Test
    fun defaultValueReturnsBlankActiveSession() {
        val defaultVal = ActiveSessionSerializer.defaultValue
        assertEquals("", defaultVal.algorithmId)
        assertEquals(0, defaultVal.stepIndex)
        assertEquals(1.0f, defaultVal.playbackSpeed, 0.001f)
        assertEquals("", defaultVal.datasetJson)
        assertEquals(0L, defaultVal.timestampEpochMs)
    }

    @Test
    fun roundtripSerializationPreservesExactFields() = runTest {
        val original = ActiveSessionPreferences(
            algorithmId = "quick_sort",
            stepIndex = 42,
            playbackSpeed = 2.5f,
            datasetJson = "{\"items\":[5,2,9,1]}",
            timestampEpochMs = 1718000000000L
        )

        val output = ByteArrayOutputStream()
        ActiveSessionSerializer.writeTo(original, output)

        val input = ByteArrayInputStream(output.toByteArray())
        val decoded = ActiveSessionSerializer.readFrom(input)

        assertEquals(original, decoded)
    }

    @Test
    fun readingFromEmptyStreamReturnsDefaultValue() = runTest {
        val input = ByteArrayInputStream(ByteArray(0))
        val decoded = ActiveSessionSerializer.readFrom(input)
        assertEquals(ActiveSessionSerializer.defaultValue, decoded)
    }

    @Test
    fun readingCorruptedDataThrowsCorruptionException() {
        val corruptedInput = ByteArrayInputStream("not-valid-json-payload-{{{".toByteArray())
        var caughtException = false
        runBlocking {
            try {
                ActiveSessionSerializer.readFrom(corruptedInput)
            } catch (e: CorruptionException) {
                caughtException = true
            }
        }
        assertTrue("Expected CorruptionException was not thrown", caughtException)
    }
}
