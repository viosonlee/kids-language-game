package com.kidslanguage.companion

import com.kidslanguage.companion.core.persistence.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressRepositoryTest {
    private class Dao : ProgressDao() {
        val records = mutableListOf<String>()
        val events = mutableListOf<String>()
        var recordGate: CompletableDeferred<Unit>? = null
        var recordFails = false
        var clearFails = false
        override suspend fun record(attempt: PracticeAttemptEntity) {
            events += "start:${attempt.attemptId}"
            recordGate?.await()
            if (recordFails) throw IOException("write failed")
            records += attempt.attemptId
            events += "saved:${attempt.attemptId}"
        }
        override suspend fun clear() {
            events += "clear"
            if (clearFails) throw IOException("clear failed")
            records.clear()
        }
        override suspend fun insertAttempt(attempt: PracticeAttemptEntity): Long = error("not used")
        override suspend fun getItem(language: String, itemId: String): ItemProgressEntity? = error("not used")
        override suspend fun saveItem(item: ItemProgressEntity) = error("not used")
        override suspend fun deleteAttempts() = error("not used")
        override suspend fun deleteItems() = error("not used")
        override fun totals(start: Long, end: Long) = flowOf(ProgressTotals())
        override fun languages() = flowOf(emptyList<LanguageProgress>())
    }

    private fun attempt(id: String) = PracticeAttemptEntity(id, id, "en-US", "hello", 100)

    @Test fun clearWaitsForEarlierWritesAndKeepsSubsequentPractice() = runTest {
        val dao = Dao()
        val repo = RoomProgressRepository(dao, backgroundScope)
        dao.recordGate = CompletableDeferred()
        repo.record(attempt("old"))
        val clear = launch(start = CoroutineStart.UNDISPATCHED) { repo.clear() }
        repo.record(attempt("new"))
        assertEquals(listOf("start:old"), dao.events)
        dao.recordGate!!.complete(Unit)
        runCurrent()
        clear.join()
        assertEquals(listOf("start:old", "saved:old", "clear", "start:new", "saved:new"), dao.events)
        assertEquals(listOf("new"), dao.records)
    }

    @Test fun cancelledClearDoesNotDeleteRecordsLaterOrCancelApplicationWrites() = runTest {
        val dao = Dao()
        val repo = RoomProgressRepository(dao, backgroundScope)
        dao.recordGate = CompletableDeferred()
        val screen = launch(start = CoroutineStart.UNDISPATCHED) {
            repo.record(attempt("old"))
            awaitCancellation()
        }
        val clear = launch(start = CoroutineStart.UNDISPATCHED) { repo.clear() }
        clear.cancelAndJoin()
        screen.cancelAndJoin()
        repo.record(attempt("new"))
        dao.recordGate!!.complete(Unit)
        runCurrent()
        assertEquals(listOf("old", "new"), dao.records)
        assertFalse(dao.events.contains("clear"))
    }

    @Test fun failedWriteWarningSurvivesFailedClearAndClearsOnlyAfterSuccessfulReset() = runTest {
        val dao = Dao()
        val repo = RoomProgressRepository(dao, backgroundScope)
        dao.recordFails = true
        repo.record(attempt("failed"))
        runCurrent()
        assertNotNull(repo.error.value)
        dao.recordFails = false
        repo.record(attempt("saved"))
        runCurrent()
        assertNotNull(repo.error.value) // The older failed write was not recovered.
        dao.clearFails = true
        try { repo.clear(); fail("Clear failure must propagate") } catch (_: IOException) { }
        assertEquals(listOf("saved"), dao.records)
        assertNotNull(repo.error.value)
        dao.clearFails = false
        repo.clear()
        assertTrue(dao.records.isEmpty())
        assertNull(repo.error.value)
    }
}
