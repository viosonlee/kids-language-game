package com.kidslanguage.companion.app

import android.content.Context
import androidx.room.Room
import com.kidslanguage.companion.core.persistence.ProgressDatabase
import com.kidslanguage.companion.core.persistence.RoomProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.kidslanguage.companion.core.asr.SpeechRecognizerEngine
import com.kidslanguage.companion.core.asr.VoskSpeechRecognizerEngine
import com.kidslanguage.companion.core.audio.AndroidPromptPlayer
import com.kidslanguage.companion.core.audio.AndroidSoundEffectManager
import com.kidslanguage.companion.core.audio.PromptPlayer
import com.kidslanguage.companion.core.audio.SoundEffectManager
import com.kidslanguage.companion.core.content.AssetContentRepository
import com.kidslanguage.companion.core.content.ContentRepository
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.core.scoring.ChildSpeechEvaluationService
import com.kidslanguage.companion.core.scoring.SpeechEvaluationService

class AppContainer(private val context: Context) : AppDependencies {
    override val backgroundMusic by lazy { com.kidslanguage.companion.core.audio.AndroidBackgroundMusicPlayer(context) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override val progressRepository by lazy {
        RoomProgressRepository(
            Room.databaseBuilder(context.applicationContext, ProgressDatabase::class.java, "practice_progress.db")
                .build().progressDao(), applicationScope
        )
    }
    override val contentRepository: ContentRepository by lazy {
        AssetContentRepository(context)
    }

    override val speechEvaluationService: SpeechEvaluationService by lazy {
        ChildSpeechEvaluationService()
    }

    override val speechRecognizerEngine: SpeechRecognizerEngine by lazy {
        VoskSpeechRecognizerEngine(context)
    }

    override val promptPlayer: PromptPlayer by lazy {
        com.kidslanguage.companion.core.audio.SettingsPromptPlayer(
            AndroidPromptPlayer(context), settingsRepository.settingsFlow)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    override val soundEffectManager: SoundEffectManager by lazy {
        AndroidSoundEffectManager(context, settingsRepository)
    }
}
