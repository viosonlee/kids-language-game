package com.kidslanguage.companion.core.persistence

import com.kidslanguage.companion.core.model.EvaluationStatus
import com.kidslanguage.companion.core.model.PracticeItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZonedDateTime

interface ProgressRepository {
    val totals: Flow<ProgressTotals>
    val today: Flow<ProgressTotals>
    val languages: Flow<List<LanguageProgress>>
    val error: StateFlow<String?>
    fun record(attempt: PracticeAttemptEntity)
    suspend fun clear()
}

fun repeatAttempt(item: PracticeItem, runId: String, number: Int, status: EvaluationStatus,
                  now: Long = System.currentTimeMillis()): PracticeAttemptEntity? {
    if (status == EvaluationStatus.NoSpeech || status == EvaluationStatus.Error) return null
    return PracticeAttemptEntity(
        attemptId = "$runId:$number", itemRunId = runId, language = item.language,
        itemId = item.id, practicedAt = now,
        normalPasses = if (status == EvaluationStatus.Pass) 1 else 0,
        assistedPasses = if (status == EvaluationStatus.SoftPass) 1 else 0,
        completed = if (status == EvaluationStatus.Pass || status == EvaluationStatus.SoftPass) 1 else 0
    )
}

fun chooseAttempt(item: PracticeItem, runId: String, number: Int, correct: Boolean,
                  completed: Boolean, now: Long = System.currentTimeMillis()) = PracticeAttemptEntity(
    attemptId = "$runId:$number", itemRunId = runId, language = item.language,
    itemId = item.id, practicedAt = now,
    chooseCorrect = if (correct) 1 else 0, chooseWrong = if (correct) 0 else 1,
    completed = if (completed) 1 else 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class RoomProgressRepository(
    private val dao: ProgressDao,
    private val applicationScope: CoroutineScope,
    private val readLegacyCompletedCount: suspend () -> Int
) : ProgressRepository {
    private val writes = Mutex()
    private val _error = MutableStateFlow<String?>(null)
    override val error = _error.asStateFlow()
    override val totals = flow {
        writes.withLock {
            if (dao.legacyProgress() == null) {
                // Never import temporary settings defaults after a read failure. The singleton
                // INSERT also prevents an older importer from undoing a concurrent reset.
                dao.importLegacyProgress(LegacyProgressEntity(completedCount = readLegacyCompletedCount().coerceAtLeast(0)))
            }
        }
        emitAll(dao.totals(Long.MIN_VALUE, Long.MAX_VALUE))
    }
    override val languages = dao.languages()
    // Recompute on date/time-zone changes while the parent screen remains open.
    override val today = observeTodayProgress(dao)

    override fun record(attempt: PracticeAttemptEntity) {
        // Acquire/queue in call order; leaving a practice screen must not cancel a write.
        applicationScope.launch(start = CoroutineStart.UNDISPATCHED) {
            writes.withLock {
                try {
                    dao.record(attempt)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _error.value = "部分练习记录未能保存，请检查设备存储空间。"
                }
            }
        }
    }

    override suspend fun clear() = writes.withLock {
        dao.clear()
        _error.value = null
    }
}

/** Query local calendar days, which may be shorter or longer than 24 hours. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeTodayProgress(
    dao: ProgressDao,
    now: () -> ZonedDateTime = { ZonedDateTime.now() },
): Flow<ProgressTotals> = flow {
    while (currentCoroutineContext().isActive) {
        val current = now()
        val date = current.toLocalDate()
        val zone = current.zone
        emit(date.atStartOfDay(zone).toInstant().toEpochMilli() to
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
        delay(30_000)
    }
}.distinctUntilChanged().flatMapLatest { (start, end) -> dao.totals(start, end) }
