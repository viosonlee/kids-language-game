package com.kidslanguage.companion

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.kidslanguage.companion.core.persistence.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ProgressDatabaseTest {
    @Test fun transactionsDeduplicateAggregateAndClear() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            ProgressDatabase::class.java).build()
        try {
            val dao = db.progressDao()
            val retry = PracticeAttemptEntity("r:1", "r", "en-US", "hello", 100)
            dao.record(retry)
            dao.record(retry) // Duplicate callback must not change either table.
            dao.record(retry.copy(attemptId = "r:2", practicedAt = 200, assistedPasses = 1, completed = 1))
            dao.record(retry.copy(attemptId = "s:1", itemRunId = "s", language = "zh-CN", practicedAt = 300,
                chooseWrong = 1, completed = 1))
            val item = dao.getItem("en-US", "hello")!!
            assertEquals(2, item.totalAttempts)
            assertEquals(1, item.assistedPasses)
            assertEquals(200L, item.lastPracticedAt)
            val totals = dao.totals(0, 400).first()
            assertEquals(3, totals.attempts)
            assertEquals(2, totals.practiced)
            assertEquals(2, totals.completed)
            assertEquals(1, totals.chooseWrong)
            assertEquals(1, dao.totals(200, 300).first().attempts)
            assertEquals(2, dao.languages().first().size)
            dao.clear()
            assertNull(dao.getItem("en-US", "hello"))
            assertEquals(ProgressTotals(), dao.totals(0, 400).first())
        } finally { db.close() }
    }
}
