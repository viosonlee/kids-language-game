package com.kidslanguage.companion.app

import com.kidslanguage.companion.core.asr.SpeechRecognizerEngine
import com.kidslanguage.companion.core.audio.BackgroundMusicPlayer
import com.kidslanguage.companion.core.audio.PromptPlayer
import com.kidslanguage.companion.core.audio.SoundEffectManager
import com.kidslanguage.companion.core.content.ContentRepository
import com.kidslanguage.companion.core.persistence.ProgressRepository
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.core.scoring.SpeechEvaluationService

/** Dependencies used by the real navigation graph, supplied by Android or controlled tests. */
interface AppDependencies {
    val backgroundMusic: BackgroundMusicPlayer
    val progressRepository: ProgressRepository
    val contentRepository: ContentRepository
    val speechEvaluationService: SpeechEvaluationService
    val speechRecognizerEngine: SpeechRecognizerEngine
    val promptPlayer: PromptPlayer
    val settingsRepository: SettingsRepository
    val soundEffectManager: SoundEffectManager
}
