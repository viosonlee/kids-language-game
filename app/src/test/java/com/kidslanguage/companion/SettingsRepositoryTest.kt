package com.kidslanguage.companion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.persistence.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SettingsRepositoryTest {
    private class Store(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        override val data = MutableStateFlow(initial)
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
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

    @Test fun clearingLegacyProgressPreservesParentPreferences() = runTest {
        val repo = SettingsRepository(Store(preferencesOf(intPreferencesKey("completed_count") to 17)))
        repo.setLanguage(LanguageCode.ZH_CN)
        repo.setSoundVolume(0f)
        repo.setAutoNext(false)
        repo.resetProgress()
        val settings = repo.settingsFlow.first()
        assertEquals(0, settings.completedCount)
        assertEquals(LanguageCode.ZH_CN, settings.selectedLanguage)
        assertEquals(0f, settings.soundVolume)
        assertFalse(settings.autoNext)
    }
}
