package com.kidslanguage.companion

import android.content.Context
import androidx.room.Room
import com.kidslanguage.companion.core.persistence.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [29])
class RoomProgressDatabaseTest {
    @Test fun realDatabaseDeduplicatesAndSurvivesReopen() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val name = "progress-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, ProgressDatabase::class.java, name).build()
        var db = open()
        try {
            val first = PracticeAttemptEntity("r:1", "r", "en-US", "hello", 100)
            db.progressDao().record(first)
            db.progressDao().record(first)
            db.progressDao().record(first.copy(attemptId = "r:2", practicedAt = 200, assistedPasses = 1, completed = 1))
            db.progressDao().record(first.copy(attemptId = "s:1", itemRunId = "s", language = "zh-CN", practicedAt = 300,
                chooseWrong = 1, completed = 1))
            db.close()
            db = open()
            val dao = db.progressDao()
            val item = dao.getItem("en-US", "hello")!!
            assertEquals(2, item.totalAttempts)
            assertEquals(1, item.assistedPasses)
            assertEquals(200L, item.lastPracticedAt)
            val totals = dao.totals(0, 400).first()
            assertEquals(3, totals.attempts)
            assertEquals(2, totals.practiced)
            assertEquals(2, totals.completed)
            assertEquals(1, totals.chooseWrong)
            assertEquals(300L, totals.lastPracticedAt)
            assertEquals(1, dao.totals(200, 300).first().attempts)
            assertEquals(2, dao.languages().first().size)
            dao.clear()
            assertNull(dao.getItem("en-US", "hello"))
            assertEquals(ProgressTotals(), dao.totals(0, 400).first())
            db.close()
            db = open()
            assertEquals(ProgressTotals(), db.progressDao().totals(0, 400).first())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
