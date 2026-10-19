package com.unravel.core.database

import android.content.Context
import androidx.room.Room
import com.unravel.core.database.entity.CustomPresetEntity
import com.unravel.core.database.seed.CurriculumSeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UnravelDatabaseTest {

    private lateinit var db: UnravelDatabase

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, UnravelDatabase::class.java)
            .addCallback(UnravelDatabase.SeedCallback)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun databaseInitialSeedContainsExactly14CurriculumNodes() = runTest {
        val count = db.skillNodeDao().getSkillNodeCount()
        assertEquals(14, count)

        val allNodes = db.skillNodeDao().getAllSkillNodes().first()
        assertEquals(14, allNodes.size)

        // Verify entry point TWO_POINTERS is unlocked
        val twoPointers = db.skillNodeDao().getSkillNodeById("two_pointers")
        assertNotNull(twoPointers)
        assertTrue(twoPointers!!.isUnlocked)
        assertEquals("UNEXPLORED", twoPointers.masteryState)
        assertTrue(twoPointers.prerequisiteIds.isEmpty())

        // Verify all 14 node IDs match the expected curriculum IDs
        val expectedIds = CurriculumSeed.INITIAL_SKILL_NODES.map { it.id }.toSet()
        val actualIds = allNodes.map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)
    }

    @Test
    fun skillNodeDaoUpdateOperationsCorrectlyUpdateMasteryAndUnlockStatus() = runTest {
        val nodeId = "sliding_window"
        val initial = db.skillNodeDao().getSkillNodeById(nodeId)
        assertNotNull(initial)
        assertEquals(false, initial!!.isUnlocked)
        assertEquals("UNEXPLORED", initial.masteryState)

        // Unlock node
        db.skillNodeDao().updateUnlockedState(nodeId, true)
        val afterUnlock = db.skillNodeDao().getSkillNodeById(nodeId)
        assertTrue(afterUnlock!!.isUnlocked)

        // Update mastery
        db.skillNodeDao().updateMasteryState(nodeId, "MASTERED")
        val afterMastery = db.skillNodeDao().getSkillNodeById(nodeId)
        assertEquals("MASTERED", afterMastery!!.masteryState)
    }

    @Test
    fun customPresetDaoSupportsInsertionQueryAndDeletion() = runTest {
        val preset = CustomPresetEntity(
            id = "preset_1",
            algorithmId = "two_pointers",
            name = "Test Two Pointers Preset",
            datasetPayload = "{\"values\":[1,2,3,4,5]}",
            createdAtEpochMs = 1700000000000L
        )

        db.customPresetDao().insertCustomPreset(preset)

        val retrieved = db.customPresetDao().getCustomPresetById("preset_1")
        assertNotNull(retrieved)
        assertEquals("Test Two Pointers Preset", retrieved!!.name)
        assertEquals("{\"values\":[1,2,3,4,5]}", retrieved.datasetPayload)

        val presetsForAlgo = db.customPresetDao().getCustomPresetsByAlgorithm("two_pointers").first()
        assertEquals(1, presetsForAlgo.size)
        assertEquals("preset_1", presetsForAlgo[0].id)

        // Delete preset
        db.customPresetDao().deleteCustomPresetById("preset_1")
        val afterDelete = db.customPresetDao().getCustomPresetById("preset_1")
        assertNull(afterDelete)
        assertEquals(0, db.customPresetDao().getPresetCountByAlgorithm("two_pointers"))
    }
}
