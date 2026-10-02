package com.kidslanguage.companion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.viewModelScope
import com.kidslanguage.companion.core.audio.*
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.feature.home.HomeViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.io.IOException
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private class Store : DataStore<Preferences> {
        var failWrite = false
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            if (failWrite) throw IOException("storage unavailable")
            return transform(data.value).also { data.value = it }
        }
    }

    private class Music : BackgroundMusicPlayer {
        var playing = false
        override fun play() { playing = true }
        override fun stop() { playing = false }
    }

    @Test fun failedLanguageSelectionKeepsSavedLanguageAndRetrySavesTheRequestedLanguage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = Store()
        val repo = SettingsRepository(store)
        val vm = HomeViewModel(repo)
        try {
            store.failWrite = true
            vm.selectLanguage(LanguageCode.EN_US)
            runCurrent()
            assertNotNull(vm.languageSaveError.value)
            assertEquals(LanguageCode.JA_JP, repo.settingsFlow.first().selectedLanguage)
            store.failWrite = false
            vm.retrySettings()
            runCurrent()
            assertNull(vm.languageSaveError.value)
            assertEquals(LanguageCode.EN_US, repo.settingsFlow.first().selectedLanguage)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }

    @Test fun olderSaveFailureCannotReplaceTheLatestSuccessfulSelection() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val firstWrite = CompletableDeferred<Unit>()
        val store = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            var calls = 0
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                if (++calls == 1) { firstWrite.await(); throw IOException("old failure") }
                return transform(data.value).also { data.value = it }
            }
        }
        val repo = SettingsRepository(store)
        val vm = HomeViewModel(repo)
        try {
            vm.selectLanguage(LanguageCode.EN_US)
            runCurrent()
            vm.selectLanguage(LanguageCode.ZH_CN)
            runCurrent()
            firstWrite.complete(Unit)
            runCurrent()
            assertNull(vm.languageSaveError.value)
            assertEquals(LanguageCode.ZH_CN, repo.settingsFlow.first().selectedLanguage)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }

    @Test fun musicFollowsSettingGreetingAndForegroundWithoutLateRestart() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = SettingsRepository(Store())
        val music = Music()
        val player = object : PromptPlayer {
            override suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String?) {}
            override suspend fun playVoiceAsset(audioAssetPath: String, volume: Float) { delay(1000) }
            override fun stop() {}
            override fun release() {}
        }
        val vm = HomeViewModel(repo, player, music)
        try {
            vm.resumeHome()
            runCurrent()
            assertFalse(music.playing)
            repo.setMusicEnabled(true)
            runCurrent()
            assertTrue(music.playing)
            vm.playGreeting()
            runCurrent()
            assertFalse(music.playing)
            advanceTimeBy(500)
            vm.playGreeting()
            runCurrent()
            assertFalse(music.playing)
            advanceTimeBy(600)
            assertFalse(music.playing)
            advanceUntilIdle()
            assertTrue(music.playing)
            vm.playGreeting()
            runCurrent()
            vm.pauseHome()
            advanceUntilIdle()
            assertFalse(music.playing)
            vm.resumeHome()
            runCurrent()
            assertTrue(music.playing)
            repo.setMusicEnabled(false)
            runCurrent()
            assertFalse(music.playing)
        } finally {
            vm.pauseHome()
            Dispatchers.resetMain()
        }
    }
}
