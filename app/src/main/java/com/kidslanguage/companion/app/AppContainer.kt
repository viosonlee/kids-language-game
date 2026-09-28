package com.kidslanguage.companion.app

import android.content.Context
import com.kidslanguage.companion.core.asr.SpeechRecognizerEngine
import com.kidslanguage.companion.core.asr.SystemSpeechRecognizerEngine
import com.kidslanguage.companion.core.audio.AndroidPromptPlayer
import com.kidslanguage.companion.core.audio.PromptPlayer
import com.kidslanguage.companion.core.content.AssetContentRepository
import com.kidslanguage.companion.core.content.ContentRepository
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.core.scoring.ChildSpeechEvaluationService
import com.kidslanguage.companion.core.scoring.SpeechEvaluationService

class AppContainer(private val context: Context) {
    val contentRepository: ContentRepository by lazy {
        AssetContentRepository(context)
    }

    val speechEvaluationService: SpeechEvaluationService by lazy {
        ChildSpeechEvaluationService()
    }

    val speechRecognizerEngine: SpeechRecognizerEngine by lazy {
        SystemSpeechRecognizerEngine(context)
    }

    val promptPlayer: PromptPlayer by lazy {
        AndroidPromptPlayer(context)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }
}
