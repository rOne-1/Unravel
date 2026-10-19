package com.unravel.core.database.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.unravel.core.database.model.ActiveSessionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * DataStore serializer for [ActiveSessionPreferences] supporting instant resume.
 */
object ActiveSessionSerializer : Serializer<ActiveSessionPreferences> {

    override val defaultValue: ActiveSessionPreferences = ActiveSessionPreferences()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun readFrom(input: InputStream): ActiveSessionPreferences {
        return try {
            val bytes = withContext(Dispatchers.IO) { input.readBytes() }
            if (bytes.isEmpty()) {
                defaultValue
            } else {
                json.decodeFromString(ActiveSessionPreferences.serializer(), bytes.decodeToString())
            }
        } catch (exception: SerializationException) {
            throw CorruptionException("Cannot decode ActiveSessionPreferences", exception)
        }
    }

    override suspend fun writeTo(t: ActiveSessionPreferences, output: OutputStream) {
        withContext(Dispatchers.IO) {
            val bytes = json.encodeToString(ActiveSessionPreferences.serializer(), t).encodeToByteArray()
            output.write(bytes)
        }
    }
}
