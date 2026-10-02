package com.kidslanguage.companion

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.room.Room
import com.kidslanguage.companion.core.persistence.*
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [29])
class LegacyProgressMigrationTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private fun open(name: String) = Room.databaseBuilder(context, ProgressDatabase::class.java, name)
        .addMigrations(ProgressDatabase.MIGRATION_1_2).build()
    private fun attempt(id: String) = PracticeAttemptEntity(id, id, "en-US", "hello", 100,
        normalPasses = 1, completed = 1)

    // Build the actual shipped v1 schema, not a hand-written approximation of v2.
    private fun createVersionOne(name: String) {
        val schema = JSONObject(File("schemas/com.kidslanguage.companion.core.persistence.ProgressDatabase/1.json").readText())
            .getJSONObject("database")
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", table))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO item_progress VALUES ('en-US', 'hello', 1, 1, 0, 0, 0, 100)")
            db.execSQL("INSERT INTO practice_attempt VALUES ('old:1', 'old', 'en-US', 'hello', 100, 1, 0, 0, 0, 1)")
            db.version = 1
        }
    }

    @Test fun upgradesV1AndImportsOnce() = runBlocking {
        val name = "migration-${UUID.randomUUID()}.db"
        createVersionOne(name)
        var db = open(name)
        try {
            var reads = 0
            val repo = RoomProgressRepository(db.progressDao(), this) { reads++; 17 }
            val totals = repo.totals.first()
            assertEquals(1, totals.completed)
            assertEquals(17, totals.legacyCompleted)
            assertEquals(1, db.progressDao().getItem("en-US", "hello")!!.normalPasses)
            assertEquals(1, db.progressDao().languages().first().single().completed)
            assertEquals(1, reads)
            assertEquals(totals, repo.totals.first())
            assertEquals(1, reads)
            db.close()
            db = open(name)
            val restored = RoomProgressRepository(db.progressDao(), this) { error("must not re-import") }
            assertEquals(totals, restored.totals.first())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun failedLegacyReadCanRetry() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, ProgressDatabase::class.java).build()
        try {
            val dao = db.progressDao()
            dao.record(attempt("new"))
            var failRead = true
            val repo = RoomProgressRepository(dao, this) {
                if (failRead) throw IOException("legacy file unavailable") else 17
            }
            try { repo.totals.first(); fail("Must not report a fake zero") } catch (_: IOException) { }
            assertNull(dao.legacyProgress())
            assertEquals(1, dao.totals(0, 200).first().completed)
            failRead = false
            assertEquals(17, repo.totals.first().legacyCompleted)
            assertEquals(1, repo.totals.first().completed)
        } finally { db.close() }
    }

    @Test fun failedImportSurvivesReopen() = runBlocking {
        val name = "import-${UUID.randomUUID()}.db"
        var db = open(name)
        try {
            db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER reject_legacy_insert
                BEFORE INSERT ON legacy_progress BEGIN SELECT RAISE(ABORT, 'simulated import failure'); END""")
            val repo = RoomProgressRepository(db.progressDao(), this) { 17 }
            try { repo.totals.first(); fail("Expected import failure") } catch (_: SQLiteException) { }
            assertNull(db.progressDao().legacyProgress())
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_legacy_insert")
            db.close()
            db = open(name)
            assertEquals(17, RoomProgressRepository(db.progressDao(), this) { 17 }.totals.first().legacyCompleted)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun resetRollsBackAllTables() = runBlocking {
        val name = "rollback-${UUID.randomUUID()}.db"
        var db = open(name)
        try {
            val dao = db.progressDao()
            dao.record(attempt("old"))
            val repo = RoomProgressRepository(dao, this) { 17 }
            val before = repo.totals.first()
            // Fail the third write, after both DELETEs, in the actual Room transaction.
            db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER reject_legacy_update
                BEFORE UPDATE ON legacy_progress BEGIN SELECT RAISE(ABORT, 'simulated reset failure'); END""")
            try { repo.clear(); fail("Expected reset failure") } catch (_: SQLiteException) { }
            assertEquals(before, repo.totals.first())
            assertEquals(1, dao.getItem("en-US", "hello")!!.totalAttempts)
            db.close()
            db = open(name)
            val restored = RoomProgressRepository(db.progressDao(), this) { error("must not re-import") }
            assertEquals(before, restored.totals.first())
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_legacy_update")
            restored.clear()
            assertEquals(ProgressTotals(), restored.totals.first())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun resetBeforeImportSurvivesReopen() = runBlocking {
        val name = "reset-${UUID.randomUUID()}.db"
        createVersionOne(name)
        var db = open(name)
        try {
            val repo = RoomProgressRepository(db.progressDao(), this) { error("reset must not need settings") }
            repo.clear()
            db.progressDao().record(attempt("after-reset"))
            db.close()
            db = open(name)
            var legacyReads = 0
            val restored = RoomProgressRepository(db.progressDao(), this) { legacyReads++; 17 }
            val totals = restored.totals.first()
            assertEquals(1, totals.completed)
            assertEquals(0, totals.legacyCompleted)
            assertEquals(0, legacyReads)
            assertEquals(1, db.progressDao().getItem("en-US", "hello")!!.totalAttempts)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun staleImporterCannotUndoReset() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, ProgressDatabase::class.java).build()
        try {
            val started = CompletableDeferred<Unit>()
            val legacyValue = CompletableDeferred<Int>()
            val importer = RoomProgressRepository(db.progressDao(), this) { started.complete(Unit); legacyValue.await() }
            val pending = async { importer.totals.first() }
            started.await()
            RoomProgressRepository(db.progressDao(), this) { error("not used") }.clear()
            db.progressDao().record(attempt("after-reset"))
            legacyValue.complete(17)
            val totals = pending.await()
            assertEquals(0, totals.legacyCompleted)
            assertEquals(1, totals.completed)
        } finally { db.close() }
    }

    @Test fun cancelImportThenReset() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, ProgressDatabase::class.java).build()
        try {
            val started = CompletableDeferred<Unit>()
            val repo = RoomProgressRepository(db.progressDao(), this) { started.complete(Unit); awaitCancellation() }
            val reader = launch { repo.totals.first() }
            started.await()
            reader.cancelAndJoin()
            assertNull(db.progressDao().legacyProgress())
            repo.clear()
            assertEquals(ProgressTotals(), repo.totals.first())
        } finally { db.close() }
    }

    @Test fun rawLegacyReadRejectsFallback() = runBlocking {
        val state = MutableStateFlow<Result<Preferences>>(Result.failure(IOException("unreadable")))
        val store = object : DataStore<Preferences> {
            override val data = state.map { it.getOrThrow() }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = error("must not write")
        }
        val settings = SettingsRepository(store)
        assertNotNull(settings.settingsState.first().error)
        try { settings.readLegacyCompletedCount(); fail("Must not import fallback") } catch (_: IOException) { }
        state.value = Result.success(preferencesOf(intPreferencesKey("completed_count") to 17,
            stringPreferencesKey("sound_volume") to "invalid"))
        assertEquals(17, settings.readLegacyCompletedCount())
        state.value = Result.success(preferencesOf(stringPreferencesKey("completed_count") to "invalid"))
        try { settings.readLegacyCompletedCount(); fail("Wrong count type must not become zero") } catch (_: ClassCastException) { }
        assertNull(settings.settingsState.first().error) // An obsolete seed cannot break unrelated settings.
    }
}
