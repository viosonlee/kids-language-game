package com.kidslanguage.companion.core.asr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RecognitionResult(
    val text: String,
    val confidence: Float = 1.0f,
    val isSuccess: Boolean = true,
    val error: String? = null
)

interface SpeechRecognizerEngine {
    suspend fun load(language: LanguageCode): Result<Unit>
    suspend fun recognizeFromAudio(pcm: ByteArray): RecognitionResult
    suspend fun startListening(language: LanguageCode, onResult: (RecognitionResult) -> Unit)
    fun stopListening()
    fun cancelListening() { stopListening() }
    suspend fun unload()
}

/**
 * Android system SpeechRecognizer implementation with graceful fallback.
 */
class SystemSpeechRecognizerEngine(private val context: Context) : SpeechRecognizerEngine {

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentLanguage: LanguageCode = LanguageCode.JA_JP
    private var activeCallback: ((RecognitionResult) -> Unit)? = null
    private var generation = 0

    override suspend fun load(language: LanguageCode): Result<Unit> = withContext(Dispatchers.Main) {
        currentLanguage = language
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Speech recognition unavailable"))
        }
    }

    override suspend fun recognizeFromAudio(pcm: ByteArray): RecognitionResult {
        // Direct PCM offline recognition (plug Vosk here when model unpacked)
        return RecognitionResult(text = "", isSuccess = false, error = "Audio buffer not supported directly by system recognizer")
    }

    override suspend fun startListening(
        language: LanguageCode,
        onResult: (RecognitionResult) -> Unit
    ) = withContext(Dispatchers.Main) {
        cancelListening()
        val request = generation
        activeCallback = onResult
        fun finish(result: RecognitionResult) {
            if (request != generation) return
            val callback = activeCallback ?: return
            cancelListening()
            callback(result)
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            finish(RecognitionResult(text = "", isSuccess = false, error = "Speech recognition unavailable"))
            return@withContext
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.code)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                finish(
                    RecognitionResult(
                        text = "",
                        isSuccess = false,
                        error = "Recognition error: $error"
                    )
                )
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                finish(
                    RecognitionResult(
                        text = text,
                        confidence = 0.9f,
                        isSuccess = true
                    )
                )
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    override fun stopListening() {
        speechRecognizer?.stopListening()
    }

    override fun cancelListening() {
        generation++
        activeCallback = null
        speechRecognizer?.cancel()
    }

    override suspend fun unload() = withContext(Dispatchers.Main) {
        cancelListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        activeCallback = null
    }
}

/**
 * Dev / Simulation Recognizer for automated tests and sandbox verification.
 */
class MockSpeechRecognizerEngine : SpeechRecognizerEngine {
    private var simulatedResponse: String = ""

    fun setSimulatedSpeech(text: String) {
        simulatedResponse = text
    }

    override suspend fun load(language: LanguageCode): Result<Unit> = Result.success(Unit)

    override suspend fun recognizeFromAudio(pcm: ByteArray): RecognitionResult {
        return RecognitionResult(text = simulatedResponse, isSuccess = true)
    }

    override suspend fun startListening(
        language: LanguageCode,
        onResult: (RecognitionResult) -> Unit
    ) {
        onResult(RecognitionResult(text = simulatedResponse, isSuccess = true))
    }

    override fun stopListening() {}
    override suspend fun unload() {}
}
