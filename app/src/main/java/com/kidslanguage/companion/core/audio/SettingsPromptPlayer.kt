package com.kidslanguage.companion.core.audio

import com.kidslanguage.companion.core.persistence.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Character speech follows feedback volume; lesson prompts keep their media volume. */
class SettingsPromptPlayer(
    private val player: PromptPlayer,
    private val settings: Flow<UserSettings>,
) : PromptPlayer by player {
    override suspend fun playVoiceAsset(audioAssetPath: String, volume: Float) {
        val feedbackVolume = settings.first().soundVolume
        if (!volume.isFinite() || !feedbackVolume.isFinite()) return
        val gain = volume.coerceIn(0f, 1f) * feedbackVolume.coerceIn(0f, 1f)
        if (gain > 0f) player.playVoiceAsset(audioAssetPath, gain)
    }
}
