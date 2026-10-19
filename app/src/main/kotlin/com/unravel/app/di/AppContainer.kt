package com.unravel.app.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import com.unravel.core.database.UnravelDatabase
import com.unravel.core.database.dao.SkillNodeDao
import com.unravel.core.database.datastore.ActiveSessionSerializer
import com.unravel.core.database.repository.SessionRepository
import com.unravel.core.database.repository.SessionRepositoryImpl
import java.io.File

/**
 * Lightweight application-scoped dependency graph for Unravel.
 * Provides thread-safe singletons for Room Database, DAOs, and Proto DataStore SessionRepository.
 */
object AppContainer {

    @Volatile
    private var database: UnravelDatabase? = null

    @Volatile
    private var sessionRepository: SessionRepository? = null

    fun getDatabase(context: Context): UnravelDatabase {
        return database ?: synchronized(this) {
            database ?: UnravelDatabase.buildDatabase(context.applicationContext).also {
                database = it
            }
        }
    }

    fun getSkillNodeDao(context: Context): SkillNodeDao {
        return getDatabase(context).skillNodeDao()
    }

    fun getSessionRepository(context: Context): SessionRepository {
        return sessionRepository ?: synchronized(this) {
            sessionRepository ?: run {
                val dataStore = DataStoreFactory.create(
                    serializer = ActiveSessionSerializer,
                    produceFile = { File(context.applicationContext.filesDir, "active_session.pb") }
                )
                SessionRepositoryImpl(dataStore).also {
                    sessionRepository = it
                }
            }
        }
    }
}
