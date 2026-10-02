package com.kidslanguage.companion.core.audio

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/** Play the full 550 ms chime and short speech; muted feedback still lasts at least 800 ms. */
suspend fun PromptPlayer.playSuccessFeedback(voice: String, effects: SoundEffectManager?) = coroutineScope {
    val minimumDisplay = async { delay(800) }
    effects?.playSuccess()
    try { delay(550) } finally { effects?.stop() }
    playVoiceAsset(voice)
    minimumDisplay.await()
}
