package com.kidslanguage.companion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import com.kidslanguage.companion.core.persistence.*
import com.kidslanguage.companion.feature.parent.*
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParentReadStateTest {
    private class Progress : ProgressRepository {
        val totalSource = MutableStateFlow(Result.success(ProgressTotals(completed = 12)))
        val todaySource = MutableStateFlow(Result.success(ProgressTotals(completed = 3)))
        val languageSource = MutableStateFlow(Result.success(emptyList<LanguageProgress>()))
        override val totals = totalSource.map { it.getOrThrow() }
        override val today = todaySource.map { it.getOrThrow() }
        override val languages = languageSource.map { it.getOrThrow() }
        override val error = MutableStateFlow<String?>("older write failed")
        var clears = 0
        override fun record(attempt: PracticeAttemptEntity) {}
        override suspend fun clear() {
            clears++
            totalSource.value = Result.success(ProgressTotals())
            todaySource.value = Result.success(ProgressTotals())
            languageSource.value = Result.success(emptyList())
        }
    }

    private fun settings() = SettingsRepository(object : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
            transform(data.value).also { data.value = it }
    })

    @Test fun eachStatisticsReadCanFailAndRetryWithoutClearingRecordsOrWriteWarnings() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            for (which in 0..2) {
                val repo = Progress()
                val vm = ParentViewModel(settings(), repo)
                val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.progress.collect() }
                try {
                    assertEquals(ProgressReadState.Loading, vm.progress.value)
                    runCurrent()
                    assertEquals(12, (vm.progress.value as ProgressReadState.Ready).totals.completed)
                    when (which) {
                        0 -> repo.totalSource.value = Result.failure(IOException("totals"))
                        1 -> repo.todaySource.value = Result.failure(IOException("today"))
                        2 -> repo.languageSource.value = Result.failure(IOException("languages"))
                    }
                    runCurrent()
                    assertTrue(vm.progress.value is ProgressReadState.Failed)
                    repo.totalSource.value = Result.success(ProgressTotals(completed = 12))
                    repo.todaySource.value = Result.success(ProgressTotals(completed = 3))
                    repo.languageSource.value = Result.success(emptyList())
                    runCurrent()
                    assertTrue(vm.progress.value is ProgressReadState.Failed)
                    vm.retryProgressRead()
                    runCurrent()
                    val recovered = vm.progress.value as ProgressReadState.Ready
                    assertEquals(12, recovered.totals.completed)
                    assertEquals(3, recovered.today.completed)
                    assertEquals(0, repo.clears)
                    assertEquals("older write failed", vm.progressError.value)
                } finally {
                    observer.cancelAndJoin()
                    vm.viewModelScope.cancel()
                    runCurrent()
                }
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun explicitResetAlsoRestartsFailedStatisticsReads() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = Progress()
        repo.totalSource.value = Result.failure(IOException("read failed"))
        val vm = ParentViewModel(settings(), repo)
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.progress.collect() }
            runCurrent()
            assertTrue(vm.progress.value is ProgressReadState.Failed)
            vm.resetProgress()
            runCurrent()
            assertEquals(ProgressResetState.Complete, vm.resetState.value)
            assertEquals(0, (vm.progress.value as ProgressReadState.Ready).totals.completed)
            assertEquals(1, repo.clears)
        } finally { vm.viewModelScope.cancel(); runCurrent(); Dispatchers.resetMain() }
    }
}
