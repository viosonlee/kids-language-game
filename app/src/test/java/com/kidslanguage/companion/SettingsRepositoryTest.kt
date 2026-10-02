package com.kidslanguage.companion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.core.persistence.TextDisplayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SettingsRepositoryTest {
    @Test fun animationPreferenceSurvivesRepositoryRecreation() = runTest {
        val store = Store()
        val repo = SettingsRepository(store)
        repo.setAnimationEnabled(false)
        assertFalse(SettingsRepository(store).settingsFlow.first().animationEnabled)
        repo.setAnimationEnabled(true)
        assertTrue(repo.settingsFlow.first().animationEnabled)
    }

    private class Store(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        override val data = MutableStateFlow(initial)
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
    }

    @Test fun textModesPreserveLegacyHiddenPreferenceAndPersistAcrossRepositories() = runTest {
        val store = Store(preferencesOf(booleanPreferencesKey("show_text") to false))
        val repo = SettingsRepository(store)
        assertEquals(TextDisplayMode.HIDDEN, repo.settingsFlow.first().textDisplayMode)
        for (mode in TextDisplayMode.entries) {
            repo.setTextDisplayMode(mode)
            assertEquals(mode, SettingsRepository(store).settingsFlow.first().textDisplayMode)
        }
        repo.setTextDisplayMode(TextDisplayMode.SMALL)
        assertEquals(TextDisplayMode.SMALL, repo.settingsFlow.first().textDisplayMode)
    }

    @Test fun lessonPreferencesAreBoundedAndPersisted() = runTest {
        val store = Store(preferencesOf(intPreferencesKey("choice_count") to 99))
        val repo = SettingsRepository(store)
        assertEquals(4, repo.settingsFlow.first().choiceCount)
        repo.setChoiceCount(1)
        repo.setShuffleItems(true)
        val restored = SettingsRepository(store).settingsFlow.first()
        assertEquals(2, restored.choiceCount)
        assertTrue(restored.shuffleItems)
    }

    @Test fun legacyInvalidValuesCannotReachAudioOrSession() = runTest {
        val repo = SettingsRepository(Store(preferencesOf(
            floatPreferencesKey("sound_volume") to Float.NaN,
            intPreferencesKey("session_length") to -20
        )))
        assertEquals(1f, repo.settingsFlow.first().soundVolume)
        assertEquals(1, repo.settingsFlow.first().sessionLength)
        repo.setSoundVolume(0.35f)
        assertEquals(0.35f, repo.settingsFlow.first().soundVolume)
        repo.setSessionLength(1000)
        assertEquals(30, repo.settingsFlow.first().sessionLength)
    }

    @Test fun readingLegacyMigrationInputPreservesParentPreferences() = runTest {
        val repo = SettingsRepository(Store(preferencesOf(intPreferencesKey("completed_count") to 17)))
        repo.setLanguage(LanguageCode.ZH_CN)
        repo.setSoundVolume(0f)
        repo.setAutoNext(false)
        val settings = repo.settingsFlow.first()
        assertEquals(17, repo.readLegacyCompletedCount())
        assertEquals(LanguageCode.ZH_CN, settings.selectedLanguage)
        assertEquals(0f, settings.soundVolume)
        assertFalse(settings.autoNext)
    }
}
