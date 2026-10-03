package com.kidslanguage.companion.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.speech.tts.UtteranceProgressListener
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.selects.select
import java.io.IOException
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
    suspend fun playVoiceAsset(audioAssetPath: String, volume: Float = 1f)
    fun stop()
    fun release()
}

/** System interruption, distinct from completing a prompt or leaving a screen. */
open class AudioPlaybackInterruptedException(message: String = "Audio focus unavailable") : CancellationException(message)

class OfflinePromptUnavailableException : AudioPlaybackInterruptedException("Offline prompt voice unavailable")

internal fun selectOfflineVoice(voices: Set<Voice>?, language: LanguageCode): Voice? {
    val target = Locale.forLanguageTag(language.code)
    return voices.orEmpty().filter {
        !it.isNetworkConnectionRequired && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty() &&
            it.locale.language == target.language && (it.locale.country == target.country || it.locale.country.isBlank())
    }.sortedWith(compareBy<Voice> { it.locale.country != target.country }.thenBy { it.name }).firstOrNull()
}

/** Each playback owns its completion and releases resources on completion or cancellation. */
class AndroidPromptPlayer(private val context: Context) : PromptPlayer {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val gate = PlaybackGate()
    private val ttsReady = CompletableDeferred<Boolean>()
    private var focusRequest: AudioFocusRequest? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    init {
        tts = TextToSpeech(context) { status ->
            mainHandler.post {
                if (status == TextToSpeech.SUCCESS) tts?.setSpeechRate(0.85f)
                ttsReady.complete(status == TextToSpeech.SUCCESS)
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                mainHandler.post { gate.complete(utteranceId) }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { gate.interrupt(utteranceId, OfflinePromptUnavailableException()) }
            }
        })
    }

    private fun requestAudioFocus(ticket: PlaybackGate.Ticket) {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener({ change ->
                if (change < 0) gate.interrupt(ticket)
            }, mainHandler)
            .build()
        focusRequest = request
        if (audioManager?.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            throw AudioPlaybackInterruptedException()
        }
    }

    private fun releaseResources() {
        val player = mediaPlayer
        mediaPlayer = null
        // release() is valid even when prepare()/start() failed.
        runCatching { player?.release() }
        runCatching { tts?.stop() }
        focusRequest?.let { request -> runCatching { audioManager?.abandonAudioFocusRequest(request) } }
        focusRequest = null
    }

    private suspend fun play(block: suspend (PlaybackGate.Ticket) -> Unit) = withContext(Dispatchers.Main) {
        stop()
        val ticket = gate.begin()
        try {
            requestAudioFocus(ticket)
            kotlinx.coroutines.withTimeoutOrNull(30_000) { block(ticket) }
            Unit
        } finally {
            // A cancelled older coroutine must never stop its replacement.
            if (gate.finish(ticket)) releaseResources()
        }
    }

    private suspend fun playAsset(path: String, ticket: PlaybackGate.Ticket, volume: Float = 1f) {
        context.assets.openFd(path).use { descriptor ->
            val player = MediaPlayer()
            mediaPlayer = player
            player.setAudioAttributes(audioAttributes)
            player.setVolume(volume, volume)
            player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            val prepared = CompletableDeferred<Unit>()
            player.setOnPreparedListener {
                if (mediaPlayer === player && gate.isActive(ticket)) prepared.complete(Unit)
            }
            player.setOnCompletionListener { gate.complete(ticket.id) }
            player.setOnErrorListener { _, what, extra ->
                if (mediaPlayer === player && gate.isActive(ticket)) {
                    if (!prepared.isCompleted) {
                        prepared.completeExceptionally(IOException("Audio preparation failed: $what/$extra"))
                    } else gate.complete(ticket.id)
                }
                true
            }
            player.prepareAsync()
            // A stop/replacement must interrupt preparation too, even if Android never calls back.
            select<Unit> {
                prepared.onAwait { }
                ticket.completion.onAwait { }
            }
            if (!gate.isActive(ticket) || mediaPlayer !== player) return
            player.start()
        }
        ticket.completion.await()
    }

    override suspend fun playVoiceAsset(audioAssetPath: String, volume: Float) {
        if (volume <= 0f) return
        play { ticket ->
            try {
                playAsset(audioAssetPath, ticket, volume.coerceIn(0f, 1f))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Optional encouragement clips must not break the practice session.
            }
        }
    }

    override suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String?) {
        play { ticket ->
            if (!audioAssetPath.isNullOrBlank()) {
                try {
                    playAsset(audioAssetPath, ticket)
                    return@play
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    runCatching { mediaPlayer?.release() }
                    mediaPlayer = null
                }
            }
            val ready = kotlinx.coroutines.withTimeoutOrNull(5_000) {
                select<Boolean> {
                    ttsReady.onAwait { it }
                    ticket.completion.onAwait { false }
                }
            } == true
            val engine = tts
            if (!ready || engine == null) throw OfflinePromptUnavailableException()
            if (gate.isActive(ticket)) {
                try {
                    // setLanguage can choose a network default. Explicitly select an installed local voice.
                    val voice = selectOfflineVoice(engine.voices, language) ?: throw OfflinePromptUnavailableException()
                    if (engine.setVoice(voice) != TextToSpeech.SUCCESS ||
                        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, ticket.id) != TextToSpeech.SUCCESS) {
                        throw OfflinePromptUnavailableException()
                    }
                    ticket.completion.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    throw OfflinePromptUnavailableException()
                }
            }
        }
    }

    override fun stop() {
        gate.cancel()
        releaseResources()
    }

    override fun release() {
        stop()
        tts?.shutdown()
        tts = null
        ttsReady.complete(false)
    }
}
