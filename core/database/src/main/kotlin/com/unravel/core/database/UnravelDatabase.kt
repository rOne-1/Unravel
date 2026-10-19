package com.unravel.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.unravel.core.database.converter.DatabaseConverters
import com.unravel.core.database.dao.CustomPresetDao
import com.unravel.core.database.dao.SkillNodeDao
import com.unravel.core.database.entity.CustomPresetEntity
import com.unravel.core.database.entity.SkillNodeEntity
import com.unravel.core.database.seed.CurriculumSeed

@Database(
    entities = [
        SkillNodeEntity::class,
        CustomPresetEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class UnravelDatabase : RoomDatabase() {

    abstract fun skillNodeDao(): SkillNodeDao
    abstract fun customPresetDao(): CustomPresetDao

    companion object {
        const val DATABASE_NAME = "unravel.db"

        /**
         * Room callback that synchronously seeds the 14 curriculum skill nodes on database creation.
         */
        val SeedCallback: Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedCurriculumNodes(db)
            }
        }

        fun buildDatabase(context: Context): UnravelDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                UnravelDatabase::class.java,
                DATABASE_NAME
            ).addCallback(SeedCallback)
                .build()
        }

        fun buildInMemoryDatabase(context: Context): UnravelDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                UnravelDatabase::class.java
            ).addCallback(SeedCallback)
                .allowMainThreadQueries()
                .build()
        }

        fun seedCurriculumNodes(db: SupportSQLiteDatabase) {
            val converters = DatabaseConverters()
            db.beginTransaction()
            try {
                for (node in CurriculumSeed.INITIAL_SKILL_NODES) {
                    val prereqJson = converters.fromStringList(node.prerequisiteIds)
                    val isUnlockedInt = if (node.isUnlocked) 1 else 0
                    db.execSQL(
                        "INSERT OR REPLACE INTO skill_nodes (id, title, category, clusterIndex, prerequisiteIds, masteryState, isUnlocked) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>(
                            node.id,
                            node.title,
                            node.category,
                            node.clusterIndex,
                            prereqJson,
                            node.masteryState,
                            isUnlockedInt
                        )
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }
}
