package com.kidslanguage.companion

import com.kidslanguage.companion.core.persistence.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import java.time.ZonedDateTime
import java.time.Instant
import java.time.ZoneId
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
        val ranges = mutableListOf<Pair<Long, Long>>()
        val cancelledRanges = mutableListOf<Pair<Long, Long>>()
        var observeRanges = false
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
        override suspend fun legacyProgress(): LegacyProgressEntity? = error("not used")
        override suspend fun importLegacyProgress(progress: LegacyProgressEntity) = error("not used")
        override suspend fun saveLegacyProgress(progress: LegacyProgressEntity) = error("not used")
        override fun totals(start: Long, end: Long) = if (!observeRanges) flowOf(ProgressTotals()) else flow {
            val range = start to end
            ranges += range
            try {
                emit(ProgressTotals(attempts = ranges.size))
                awaitCancellation()
            } finally { cancelledRanges += range }
        }
        override fun languages() = flowOf(emptyList<LanguageProgress>())
    }

    private fun attempt(id: String) = PracticeAttemptEntity(id, id, "en-US", "hello", 100)

    @Test fun todayRefreshesAcrossMidnightAndCancelsOldQuery() = runTest {
        val dao = Dao().apply { observeRanges = true }
        var now = ZonedDateTime.parse("2026-10-03T23:59:50+09:00[Asia/Tokyo]")
        val values = mutableListOf<ProgressTotals>()
        val observer = backgroundScope.launch { observeTodayProgress(dao) { now }.collect { values += it } }
        runCurrent()
        val firstStart = Instant.parse("2026-10-02T15:00:00Z").toEpochMilli()
        val secondStart = Instant.parse("2026-10-03T15:00:00Z").toEpochMilli()
        assertEquals(listOf(firstStart to secondStart), dao.ranges)
        now = now.plusSeconds(30)
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(secondStart, dao.ranges.last().first)
        assertEquals(listOf(dao.ranges.first()), dao.cancelledRanges)
        assertEquals(listOf(1, 2), values.map { it.attempts })
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(2, dao.ranges.size) // Same date must keep the active Room observer.
        observer.cancelAndJoin()
        assertEquals(dao.ranges, dao.cancelledRanges)
    }

    @Test fun todayRecomputesTimezoneAndNewSubscriptionWithoutWaitingForPoll() = runTest {
        val dao = Dao().apply { observeRanges = true }
        var now = Instant.parse("2026-10-03T01:00:00Z").atZone(ZoneId.of("Asia/Tokyo"))
        val stream = observeTodayProgress(dao) { now }
        var observer = backgroundScope.launch { stream.collect() }
        runCurrent()
        now = now.withZoneSameInstant(ZoneId.of("America/Los_Angeles"))
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(Instant.parse("2026-10-02T07:00:00Z").toEpochMilli(), dao.ranges.last().first)
        observer.cancelAndJoin()
        now = now.plusDays(2)
        observer = backgroundScope.launch { stream.collect() }
        runCurrent()
        assertEquals(Instant.parse("2026-10-04T07:00:00Z").toEpochMilli(), dao.ranges.last().first)
        assertEquals(3, dao.ranges.size)
        observer.cancelAndJoin()
    }

    @Test fun todayUsesCalendarBoundariesOnShortAndLongDaylightSavingDays() = runTest {
        for ((date, expectedHours) in listOf("2026-03-08T12:00:00-04:00[America/New_York]" to 23,
                "2026-11-01T12:00:00-05:00[America/New_York]" to 25)) {
            val dao = Dao().apply { observeRanges = true }
            val observer = backgroundScope.launch {
                observeTodayProgress(dao) { ZonedDateTime.parse(date) }.collect()
            }
            runCurrent()
            val (start, end) = dao.ranges.single()
            assertEquals(expectedHours * 3_600_000L, end - start)
            assertEquals(0, Instant.ofEpochMilli(start).atZone(ZoneId.of("America/New_York")).hour)
            assertEquals(0, Instant.ofEpochMilli(end).atZone(ZoneId.of("America/New_York")).hour)
            observer.cancelAndJoin()
        }
    }

    @Test fun clearWaitsForEarlierWritesAndKeepsSubsequentPractice() = runTest {
        val dao = Dao()
        val repo = RoomProgressRepository(dao, backgroundScope) { error("not used") }
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
        val repo = RoomProgressRepository(dao, backgroundScope) { error("not used") }
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
        val repo = RoomProgressRepository(dao, backgroundScope) { error("not used") }
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
