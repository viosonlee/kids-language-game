package com.kidslanguage.companion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.persistence.*
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsReadRecoveryTest {
    private class Store(initial: Preferences) : DataStore<Preferences> {
        var contents = initial
        var reads = 0
        var writes = 0
        val events = MutableStateFlow(Result.success(initial))
        override val data = flow {
            reads++
            events.collect { emit(it.getOrThrow()) }
        }
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            writes++
            contents = transform(contents)
            events.value = Result.success(contents)
            return contents
        }
        fun failRead() { events.value = Result.failure(IOException("storage unavailable")) }
    }

    private fun preferences() = preferencesOf(
        stringPreferencesKey("selected_language") to "zh-CN",
        floatPreferencesKey("sound_volume") to 0.3f,
        booleanPreferencesKey("animation_enabled") to false,
        booleanPreferencesKey("music_enabled") to true,
        intPreferencesKey("completed_count") to 17,
    )

    @Test fun firstReadFailureUsesQuietTemporaryValuesWithoutWritingOrDeletingPreferences() = runTest {
        val original = preferences()
        val store = Store(original)
        store.failRead()
        val repo = SettingsRepository(store)
        val snapshot = repo.settingsState.first()
        assertNotNull(snapshot.error)
        assertEquals(0f, snapshot.value.soundVolume)
        assertFalse(snapshot.value.musicEnabled)
        assertFalse(snapshot.value.animationEnabled)
        assertFalse(snapshot.value.autoStartRecording)
        assertEquals(original, store.contents)
        assertEquals(0, store.writes)
        store.events.value = Result.success(original)
        val restored = SettingsRepository(store).settingsState.first()
        assertNull(restored.error)
        assertEquals(LanguageCode.ZH_CN, restored.value.selectedLanguage)
        assertEquals(17, repo.readLegacyCompletedCount())
        assertEquals(0.3f, restored.value.soundVolume)
    }

    @Test fun activeReaderKeepsItsLastSettingsAndAutomaticallyRecovers() = runTest {
        val store = Store(preferences())
        val repo = SettingsRepository(store)
        val seen = mutableListOf<SettingsSnapshot>()
        val reader = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.settingsState.toList(seen) }
        runCurrent()
        val good = seen.last().value
        store.failRead()
        runCurrent()
        assertNotNull(seen.last().error)
        assertEquals(good, seen.last().value)
        store.events.value = Result.success(preferencesOf(stringPreferencesKey("selected_language") to "en-US"))
        advanceTimeBy(999)
        assertNotNull(seen.last().error)
        advanceTimeBy(1)
        runCurrent()
        assertNull(seen.last().error)
        assertEquals(LanguageCode.EN_US, seen.last().value.selectedLanguage)
        assertEquals(0, store.writes)
        reader.cancelAndJoin()
        val reads = store.reads
        advanceTimeBy(60_000)
        assertEquals(reads, store.reads)
    }

    @Test fun manualRetryInterruptsBackoffAndDoesNotPretendAnotherReaderRecovered() = runTest {
        val store = Store(preferences())
        val repo = SettingsRepository(store)
        val seen = mutableListOf<SettingsSnapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.settingsState.toList(seen) }
        runCurrent()
        store.failRead()
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, store.reads)
        store.events.value = Result.success(store.contents)
        assertNull(repo.settingsState.first().error)
        // A fresh reader succeeding must not hide the older reader's stale snapshot.
        assertNotNull(seen.last().error)
        repo.retryRead()
        runCurrent()
        assertNull(seen.last().error)
        assertEquals(0, store.writes)
    }

    @Test fun successfulWriteWakesReadersWaitingToRetry() = runTest {
        val store = Store(preferences())
        store.failRead()
        val repo = SettingsRepository(store)
        val seen = mutableListOf<SettingsSnapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.settingsState.toList(seen) }
        runCurrent()
        assertNotNull(seen.last().error)
        repo.setLanguage(LanguageCode.EN_US)
        runCurrent()
        assertEquals(0L, testScheduler.currentTime)
        assertNull(seen.last().error)
        assertEquals(LanguageCode.EN_US, seen.last().value.selectedLanguage)
        assertEquals(17, repo.readLegacyCompletedCount())
        assertEquals(1, store.writes)
    }

    @Test fun cancellationIsNotTurnedIntoDefaultsOrRetried() = runTest {
        val store = Store(preferences())
        store.events.value = Result.failure(CancellationException("screen left"))
        val seen = mutableListOf<SettingsSnapshot>()
        val reader = launch { SettingsRepository(store).settingsState.toList(seen) }
        reader.join()
        assertTrue(reader.isCancelled)
        assertTrue(seen.isEmpty())
        advanceTimeBy(60_000)
        assertEquals(1, store.reads)
    }

    @Test fun leavingDuringFirstReadCancelsTheSourceWithoutEmittingFallback() = runTest {
        var reads = 0
        var sourceCancelled = false
        val store = object : DataStore<Preferences> {
            override val data = flow<Preferences> {
                reads++
                try { awaitCancellation() } finally { sourceCancelled = true }
            }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = error("unexpected write")
        }
        val seen = mutableListOf<SettingsSnapshot>()
        val reader = launch { SettingsRepository(store).settingsState.toList(seen) }
        runCurrent()
        reader.cancelAndJoin()
        assertTrue(sourceCancelled)
        assertTrue(seen.isEmpty())
        advanceTimeBy(60_000)
        assertEquals(1, reads)
    }

    @Test fun wrongPreferenceTypeIsReportedAndOnlyAnExplicitWriteRepairsIt() = runTest {
        val original = preferencesOf(stringPreferencesKey("sound_volume") to "invalid",
            intPreferencesKey("completed_count") to 17)
        val store = Store(original)
        val repo = SettingsRepository(store)
        assertNotNull(repo.settingsState.first().error)
        assertEquals(original, store.contents)
        assertEquals(0, store.writes)
        repo.setSoundVolume(0.25f)
        val restored = repo.settingsState.first()
        assertNull(restored.error)
        assertEquals(0.25f, restored.value.soundVolume)
        assertEquals(17, repo.readLegacyCompletedCount())
    }
}
