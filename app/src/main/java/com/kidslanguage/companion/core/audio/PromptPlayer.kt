package com.kidslanguage.companion.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

interface PromptPlayer {
    suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String? = null)
    fun stop()
    fun release()
}

class AndroidPromptPlayer(private val context: Context) : PromptPlayer {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var mediaPlayer: MediaPlayer? = null
    private var activeCompletion: CompletableDeferred<Unit>? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                tts?.setSpeechRate(0.85f) // slightly slower for young kids
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                activeCompletion?.complete(Unit)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                activeCompletion?.complete(Unit)
            }
        })
    }

    override suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String?) = withContext(Dispatchers.Main) {
        stop()

        // 1. If audio file exists in assets, play with MediaPlayer
        if (!audioAssetPath.isNullOrBlank()) {
            try {
                val deferred = CompletableDeferred<Unit>()
                context.assets.openFd(audioAssetPath).use { afd ->
                    val player = MediaPlayer()
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    player.prepare()
                    player.setOnCompletionListener {
                        deferred.complete(Unit)
                    }
                    player.setOnErrorListener { _, _, _ ->
                        deferred.complete(Unit)
                        true
                    }
                    player.start()
                    mediaPlayer = player
                    activeCompletion = deferred
                }
                activeCompletion?.await()
                return@withContext
            } catch (_: Exception) {
                // Asset not present or playback error, fallback to TTS
            }
        }

        // 2. Fallback to Android TTS
        if (isTtsInitialized && tts != null) {
            val locale = when (language) {
                LanguageCode.JA_JP -> Locale.JAPANESE
                LanguageCode.ZH_CN -> Locale.CHINESE
                LanguageCode.EN_US -> Locale.ENGLISH
            }
            tts?.language = locale
            val deferred = CompletableDeferred<Unit>()
            activeCompletion = deferred
            val utteranceId = "prompt_${System.currentTimeMillis()}"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            deferred.await()
        }
    }

    override fun stop() {
        activeCompletion?.complete(Unit)
        activeCompletion = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    override fun release() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
