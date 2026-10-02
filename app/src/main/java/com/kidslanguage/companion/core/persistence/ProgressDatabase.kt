package com.kidslanguage.companion.core.persistence

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "item_progress", primaryKeys = ["language", "itemId"])
data class ItemProgressEntity(
    val language: String,
    val itemId: String,
    val totalAttempts: Int = 0,
    val normalPasses: Int = 0,
    val assistedPasses: Int = 0,
    val chooseCorrect: Int = 0,
    val chooseWrong: Int = 0,
    val lastPracticedAt: Long = 0
)

// Only counters and identifiers are stored; no recording or recognized text.
@Entity(tableName = "practice_attempt", indices = [Index("practicedAt"), Index("language")])
data class PracticeAttemptEntity(
    @PrimaryKey val attemptId: String,
    val itemRunId: String,
    val language: String,
    val itemId: String,
    val practicedAt: Long,
    val normalPasses: Int = 0,
    val assistedPasses: Int = 0,
    val chooseCorrect: Int = 0,
    val chooseWrong: Int = 0,
    val completed: Int = 0
)

// Presence of the singleton row also marks legacy import as complete, including after reset.
@Entity(tableName = "legacy_progress")
data class LegacyProgressEntity(@PrimaryKey val id: Int = 0, val completedCount: Int)

data class ProgressTotals(
    val practiced: Int = 0,
    val attempts: Int = 0,
    val completed: Int = 0,
    val normalPasses: Int = 0,
    val assistedPasses: Int = 0,
    val chooseCorrect: Int = 0,
    val chooseWrong: Int = 0,
    val lastPracticedAt: Long = 0,
    val legacyCompleted: Int = 0
)

data class LanguageProgress(val language: String, val practiced: Int, val completed: Int)

@Dao
abstract class ProgressDao {
    @Query("SELECT * FROM legacy_progress WHERE id = 0")
    abstract suspend fun legacyProgress(): LegacyProgressEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun importLegacyProgress(progress: LegacyProgressEntity)

    @Upsert
    abstract suspend fun saveLegacyProgress(progress: LegacyProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAttempt(attempt: PracticeAttemptEntity): Long

    @Query("SELECT * FROM item_progress WHERE language = :language AND itemId = :itemId")
    abstract suspend fun getItem(language: String, itemId: String): ItemProgressEntity?

    @Upsert
    abstract suspend fun saveItem(item: ItemProgressEntity)

    @Transaction
    open suspend fun record(attempt: PracticeAttemptEntity) {
        if (insertAttempt(attempt) == -1L) return
        val old = getItem(attempt.language, attempt.itemId)
            ?: ItemProgressEntity(attempt.language, attempt.itemId)
        saveItem(old.copy(
            totalAttempts = old.totalAttempts + 1,
            normalPasses = old.normalPasses + attempt.normalPasses,
            assistedPasses = old.assistedPasses + attempt.assistedPasses,
            chooseCorrect = old.chooseCorrect + attempt.chooseCorrect,
            chooseWrong = old.chooseWrong + attempt.chooseWrong,
            lastPracticedAt = maxOf(old.lastPracticedAt, attempt.practicedAt)
        ))
    }

    @Query("""SELECT COUNT(DISTINCT itemRunId) AS practiced, COUNT(*) AS attempts,
        COALESCE(SUM(completed), 0) AS completed,
        COALESCE(SUM(normalPasses), 0) AS normalPasses,
        COALESCE(SUM(assistedPasses), 0) AS assistedPasses,
        COALESCE(SUM(chooseCorrect), 0) AS chooseCorrect,
        COALESCE(SUM(chooseWrong), 0) AS chooseWrong,
        COALESCE(MAX(practicedAt), 0) AS lastPracticedAt,
        COALESCE((SELECT completedCount FROM legacy_progress WHERE id = 0), 0) AS legacyCompleted
        FROM practice_attempt WHERE practicedAt >= :start AND practicedAt < :end""")
    abstract fun totals(start: Long, end: Long): Flow<ProgressTotals>

    @Query("""SELECT language, COUNT(DISTINCT itemRunId) AS practiced,
        COALESCE(SUM(completed), 0) AS completed FROM practice_attempt GROUP BY language""")
    abstract fun languages(): Flow<List<LanguageProgress>>

    @Query("DELETE FROM practice_attempt")
    abstract suspend fun deleteAttempts()

    @Query("DELETE FROM item_progress")
    abstract suspend fun deleteItems()

    @Transaction
    open suspend fun clear() {
        deleteAttempts()
        deleteItems()
        saveLegacyProgress(LegacyProgressEntity(completedCount = 0))
    }
}

@Database(entities = [ItemProgressEntity::class, PracticeAttemptEntity::class, LegacyProgressEntity::class], version = 2, exportSchema = true)
abstract class ProgressDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS legacy_progress (id INTEGER NOT NULL, completedCount INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }
    }
}
