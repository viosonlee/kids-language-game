package com.kidslanguage.companion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.kidslanguage.companion.core.audio.*
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.feature.home.HomeViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private class Store : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
    }

    private class Music : BackgroundMusicPlayer {
        var playing = false
        override fun play() { playing = true }
        override fun stop() { playing = false }
    }

    @Test fun musicFollowsSettingGreetingAndForegroundWithoutLateRestart() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = SettingsRepository(Store())
        val music = Music()
        val player = object : PromptPlayer {
            override suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String?) {}
            override suspend fun playVoiceAsset(audioAssetPath: String) { delay(1000) }
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
