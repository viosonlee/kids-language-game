package com.kidslanguage.companion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.viewModelScope
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.persistence.*
import com.kidslanguage.companion.feature.parent.ParentViewModel
import com.kidslanguage.companion.feature.parent.ProgressResetState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ParentViewModelTest {
    private class Store : DataStore<Preferences> {
        override val data = MutableStateFlow(preferencesOf(intPreferencesKey("completed_count") to 17))
        var fail = false
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            if (fail) throw IOException("storage unavailable")
            return transform(data.value).also { data.value = it }
        }
    }

    private class Progress : ProgressRepository {
        override val totals = MutableStateFlow(ProgressTotals(completed = 3, legacyCompleted = 17))
        override val today = totals
        override val languages = MutableStateFlow(emptyList<LanguageProgress>())
        override val error = MutableStateFlow<String?>(null)
        var clearCalls = 0
        var block: CompletableDeferred<Unit>? = null
        var fail = false
        override fun record(attempt: PracticeAttemptEntity) {}
        override suspend fun clear() {
            clearCalls++
            block?.await()
            if (fail) throw IOException("database unavailable")
            totals.value = ProgressTotals()
        }
    }

    private fun scenario(block: suspend TestScope.(ParentViewModel, SettingsRepository, Store, Progress) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = Store()
        val settings = SettingsRepository(store)
        val progress = Progress()
        val vm = ParentViewModel(settings, progress)
        try { block(vm, settings, store, progress) } finally {
            vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test fun confirmedResetRunsOnceAndPreservesAllLearningPreferences() = scenario { vm, settings, _, progress ->
        settings.setLanguage(LanguageCode.ZH_CN)
        settings.setTextDisplayMode(TextDisplayMode.SMALL)
        settings.setAutoNext(false)
        settings.setAutoPlayPrompt(false)
        settings.setAutoStartRecording(true)
        settings.setSessionLength(15)
        settings.setChoiceCount(4)
        settings.setShuffleItems(true)
        settings.setSoundVolume(0.25f)
        settings.setMusicEnabled(true)
        settings.setAnimationEnabled(false)
        val before = settings.settingsFlow.first()
        progress.block = CompletableDeferred()
        repeat(4) { vm.resetProgress() }
        assertEquals(ProgressResetState.Running, vm.resetState.value)
        runCurrent()
        assertEquals(1, progress.clearCalls)
        assertEquals(17, settings.readLegacyCompletedCount())
        progress.block!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(ProgressResetState.Complete, vm.resetState.value)
        assertEquals(ProgressTotals(), progress.totals.value)
        assertEquals(before, settings.settingsFlow.first())
    }

    @Test fun failedDatabaseClearDoesNotClearLegacyCountAndCanRetry() = scenario { vm, settings, _, progress ->
        progress.fail = true
        vm.resetProgress()
        advanceUntilIdle()
        assertTrue(vm.resetState.value is ProgressResetState.Failed)
        assertEquals(17, settings.readLegacyCompletedCount())
        assertEquals(3, progress.totals.value.completed)
        progress.fail = false
        vm.resetProgress()
        assertEquals(ProgressResetState.Running, vm.resetState.value)
        advanceUntilIdle()
        assertEquals(ProgressResetState.Complete, vm.resetState.value)
        assertEquals(0, progress.totals.value.legacyCompleted)
        assertEquals(17, settings.readLegacyCompletedCount()) // The old key is only a migration seed.
        assertEquals(2, progress.clearCalls)
    }

    @Test fun resetNoLongerDependsOnWritingTheSettingsStore() = scenario { vm, settings, store, progress ->
        store.fail = true
        vm.resetProgress()
        advanceUntilIdle()
        assertEquals(ProgressResetState.Complete, vm.resetState.value)
        assertEquals(ProgressTotals(), progress.totals.value)
        assertEquals(17, settings.readLegacyCompletedCount())
        assertEquals(1, progress.clearCalls)
    }

    @Test fun cancellingResetDoesNotReportSuccessOrClearLegacyCount() = scenario { vm, settings, _, progress ->
        progress.block = CompletableDeferred()
        vm.resetProgress()
        runCurrent()
        vm.viewModelScope.cancel()
        runCurrent()
        assertEquals(ProgressResetState.Idle, vm.resetState.value)
        assertEquals(17, settings.readLegacyCompletedCount())
        assertEquals(3, progress.totals.value.completed)
    }

    @Test fun settingsWriteFailureIsVisibleAndLaterSuccessfulWriteClearsIt() = scenario { vm, settings, store, _ ->
        store.fail = true
        vm.setLanguage(LanguageCode.EN_US)
        advanceUntilIdle()
        assertNotNull(vm.settingsError.value)
        assertEquals(LanguageCode.JA_JP, settings.settingsFlow.first().selectedLanguage)
        store.fail = false
        vm.setLanguage(LanguageCode.EN_US)
        advanceUntilIdle()
        assertNull(vm.settingsError.value)
        assertEquals(LanguageCode.EN_US, settings.settingsFlow.first().selectedLanguage)
    }
}
