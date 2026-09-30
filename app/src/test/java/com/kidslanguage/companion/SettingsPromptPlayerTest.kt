package com.kidslanguage.companion

import com.kidslanguage.companion.core.audio.*
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.persistence.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SettingsPromptPlayerTest {
    private class Player : PromptPlayer {
        val voices = mutableListOf<Pair<String, Float>>()
        var prompts = 0
        override suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String?) { prompts++ }
        override suspend fun playVoiceAsset(audioAssetPath: String, volume: Float) { voices += audioAssetPath to volume }
        override fun stop() {}
        override fun release() {}
    }

    @Test fun muteSuppressesAllCharacterFeedbackButKeepsLessonPrompt() = runTest {
        val delegate = Player()
        val player = SettingsPromptPlayer(delegate, MutableStateFlow(UserSettings(soundVolume = 0f)))
        listOf(PaoPaoVoices.PRAISE_1, PaoPaoVoices.PRAISE_2, PaoPaoVoices.PRAISE_EFFORT,
            PaoPaoVoices.TRY_AGAIN, PaoPaoVoices.CELEBRATE, PaoPaoVoices.GREETING).forEach {
            player.playVoiceAsset(it)
        }
        player.playPrompt("hello", LanguageCode.EN_US, "audio/hello.mp3")
        assertTrue(delegate.voices.isEmpty())
        assertEquals(1, delegate.prompts)
    }

    @Test fun nextClipUsesLatestVolumeWithoutRestartingPlayer() = runTest {
        val delegate = Player()
        val settings = MutableStateFlow(UserSettings(soundVolume = 0.4f))
        val player = SettingsPromptPlayer(delegate, settings)
        player.playVoiceAsset(PaoPaoVoices.CELEBRATE, 0.5f)
        assertEquals(0.2f, delegate.voices.single().second, 0.0001f)
        settings.value = settings.value.copy(soundVolume = 0f)
        player.playVoiceAsset(PaoPaoVoices.TRY_AGAIN)
        assertEquals(1, delegate.voices.size)
        settings.value = settings.value.copy(soundVolume = 0.8f)
        player.playVoiceAsset(PaoPaoVoices.PRAISE_1)
        assertEquals(0.8f, delegate.voices.last().second, 0.0001f)
    }
}
