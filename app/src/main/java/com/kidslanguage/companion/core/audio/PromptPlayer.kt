package com.kidslanguage.companion.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object PaoPaoVoices {
    const val GREETING = "audio/paopao/paopao_greeting.mp3"
    const val PRAISE_1 = "audio/paopao/paopao_praise_1.mp3"
    const val PRAISE_2 = "audio/paopao/paopao_praise_2.mp3"
    const val PRAISE_EFFORT = "audio/paopao/paopao_praise_effort.mp3"
    const val TRY_AGAIN = "audio/paopao/paopao_try_again.mp3"
    const val GUIDE_REPEAT = "audio/paopao/paopao_guide_repeat.mp3"
    const val CELEBRATE = "audio/paopao/paopao_celebrate.mp3"
}

interface PromptPlayer {
    suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String? = null)
    suspend fun playVoiceAsset(audioAssetPath: String)
    fun stop()
    fun release()
}

/**
 * AndroidPromptPlayer with transient audio focus management.
 * During prompt playback (TTS or cartoon audio), it automatically requests
 * transient audio focus with ducking so children hear words clearly without background interruption.
 */
class AndroidPromptPlayer(private val context: Context) : PromptPlayer {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var mediaPlayer: MediaPlayer? = null
    private var activeCompletion: CompletableDeferred<Unit>? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

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
                releaseAudioFocus()
                activeCompletion?.complete(Unit)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                releaseAudioFocus()
                activeCompletion?.complete(Unit)
            }
        })
    }

    private fun requestAudioFocus() {
        try {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .build()
            focusRequest = req
            audioManager?.requestAudioFocus(req)
        } catch (_: Exception) {}
    }

    private fun releaseAudioFocus() {
        try {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            focusRequest = null
        } catch (_: Exception) {}
    }

    override suspend fun playVoiceAsset(audioAssetPath: String): Unit = withContext(Dispatchers.Main) {
        stop()
        requestAudioFocus()

        try {
            val deferred = CompletableDeferred<Unit>()
            context.assets.openFd(audioAssetPath).use { afd ->
                val player = MediaPlayer()
                player.setAudioAttributes(audioAttributes)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                player.prepare()
                player.setOnCompletionListener {
                    releaseAudioFocus()
                    deferred.complete(Unit)
                }
                player.setOnErrorListener { _, _, _ ->
                    releaseAudioFocus()
                    deferred.complete(Unit)
                    true
                }
                player.start()
                mediaPlayer = player
                activeCompletion = deferred
            }
            activeCompletion?.await()
        } catch (_: Exception) {
            releaseAudioFocus()
        }
        Unit
    }

    override suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String?) = withContext(Dispatchers.Main) {
        stop()
        requestAudioFocus()

        // 1. If audio file exists in assets, play with MediaPlayer
        if (!audioAssetPath.isNullOrBlank()) {
            try {
                val deferred = CompletableDeferred<Unit>()
                context.assets.openFd(audioAssetPath).use { afd ->
                    val player = MediaPlayer()
                    player.setAudioAttributes(audioAttributes)
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    player.prepare()
                    player.setOnCompletionListener {
                        releaseAudioFocus()
                        deferred.complete(Unit)
                    }
                    player.setOnErrorListener { _, _, _ ->
                        releaseAudioFocus()
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
        } else {
            releaseAudioFocus()
        }
    }

    override fun stop() {
        releaseAudioFocus()
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
