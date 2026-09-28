package com.kidslanguage.companion.core.asr

import android.content.Context
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.IOException

/**
 * Offline Speech Recognizer using Vosk Native library.
 * Complies with V1 architecture:
 * - Single active model in memory
 * - Unloads previous language model when switching
 * - Graceful fallback to system recognizer if offline model is absent
 */
class VoskSpeechRecognizerEngine(
    private val context: Context,
    private val fallbackEngine: SpeechRecognizerEngine = SystemSpeechRecognizerEngine(context)
) : SpeechRecognizerEngine {

    private var currentModel: Model? = null
    private var currentRecognizer: Recognizer? = null
    private var speechService: SpeechService? = null
    private var currentLanguage: LanguageCode? = null

    private var activeCallback: ((RecognitionResult) -> Unit)? = null
    private var isUsingFallback = false
    private var generation = 0
    private val modelLock = Mutex()

    override suspend fun load(language: LanguageCode): Result<Unit> = withContext(Dispatchers.Main) {
        modelLock.withLock {
            if (currentLanguage == language && currentModel != null) return@withLock Result.success(Unit)
            releaseCurrentModel()
            var pendingModel: Model? = null
            try {
                val modelPath = ModelManager.getOrExtractModelPath(context, language)
                if (modelPath != null) {
                    withContext(Dispatchers.IO) { pendingModel = Model(modelPath) }
                    currentModel = pendingModel
                    pendingModel = null
                    currentLanguage = language
                    isUsingFallback = false
                    return@withLock Result.success(Unit)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(NonCancellable + Dispatchers.IO) { pendingModel?.close() }
            }
            isUsingFallback = true
            val loaded = fallbackEngine.load(language)
            if (loaded.isSuccess) currentLanguage = language
            loaded
        }
    }

    override suspend fun recognizeFromAudio(pcm: ByteArray): RecognitionResult = withContext(Dispatchers.Main) {
        modelLock.withLock {
        if (isUsingFallback || currentModel == null) {
            return@withLock fallbackEngine.recognizeFromAudio(pcm)
        }

        val model = currentModel
        try {
            withContext(Dispatchers.IO) {
                val recognizer = Recognizer(model, 16000.0f)
                try {
                    recognizer.acceptWaveForm(pcm, pcm.size)
                    RecognitionResult(text = parseVoskJson(recognizer.finalResult), isSuccess = true)
                } finally {
                    recognizer.close()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RecognitionResult(text = "", isSuccess = false, error = e.message)
        }
        }
    }

    override suspend fun startListening(
        language: LanguageCode,
        onResult: (RecognitionResult) -> Unit
    ) = withContext(Dispatchers.Main) {
        cancelListening()
        if (currentLanguage != language) {
            onResult(RecognitionResult("", isSuccess = false, error = "Language model is not ready"))
            return@withContext
        }
        val request = generation
        activeCallback = onResult
        fun finish(result: RecognitionResult) {
            if (request != generation) return
            val callback = activeCallback ?: return
            cancelListening()
            callback(result)
        }

        if (isUsingFallback || currentModel == null) {
            fallbackEngine.startListening(language, ::finish)
            return@withContext
        }

        try {
            val recognizer = Recognizer(currentModel, 16000.0f)
            currentRecognizer = recognizer

            val service = SpeechService(recognizer, 16000.0f)
            speechService = service

            val started = service.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {}

                override fun onResult(hypothesis: String?) {
                    if (!hypothesis.isNullOrBlank()) {
                        val text = parseVoskJson(hypothesis)
                        if (text.isNotBlank()) {
                            finish(RecognitionResult(text = text, isSuccess = true))
                        }
                    }
                }

                override fun onFinalResult(hypothesis: String?) {
                    val text = if (!hypothesis.isNullOrBlank()) parseVoskJson(hypothesis) else ""
                    finish(RecognitionResult(text = text, isSuccess = true))
                }

                override fun onError(exception: java.lang.Exception?) {
                    finish(
                        RecognitionResult(text = "", isSuccess = false, error = exception?.message)
                    )
                }

                override fun onTimeout() {
                    finish(
                        RecognitionResult(text = "", isSuccess = true, error = "Timeout")
                    )
                }
            }, 10000)
            if (!started) finish(RecognitionResult("", isSuccess = false, error = "Microphone did not start"))
        } catch (e: Exception) {
            e.printStackTrace()
            finish(RecognitionResult(text = "", isSuccess = false, error = e.message))
        }
    }

    override fun stopListening() {
        if (isUsingFallback) fallbackEngine.stopListening() else speechService?.stop()
    }

    override fun cancelListening() {
        generation++
        activeCallback = null
        if (isUsingFallback) {
            fallbackEngine.cancelListening()
        } else {
            try {
                speechService?.cancel()
                speechService?.shutdown()
                speechService = null
                currentRecognizer?.close()
                currentRecognizer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun unload() = withContext(Dispatchers.Main) {
        modelLock.withLock { releaseCurrentModel() }
    }

    private suspend fun releaseCurrentModel() {
        cancelListening()
        val previous = currentModel
        currentModel = null
        currentLanguage = null
        withContext(NonCancellable + Dispatchers.IO) { previous?.close() }
        if (isUsingFallback) {
            fallbackEngine.unload()
        }
        isUsingFallback = false
    }

    private fun getModelPathForLanguage(language: LanguageCode): String? {
        val langDirName = when (language) {
            LanguageCode.JA_JP -> "vosk-model-small-ja"
            LanguageCode.ZH_CN -> "vosk-model-small-cn"
            LanguageCode.EN_US -> "vosk-model-small-en-us"
        }

        // Check external files dir
        val extDir = context.getExternalFilesDir(null)
        if (extDir != null) {
            val candidate = File(extDir, "models/$langDirName")
            if (candidate.exists() && candidate.isDirectory) {
                return candidate.absolutePath
            }
        }

        // Check internal files dir
        val intCandidate = File(context.filesDir, "models/$langDirName")
        if (intCandidate.exists() && intCandidate.isDirectory) {
            return intCandidate.absolutePath
        }

        return null
    }

    private fun parseVoskJson(jsonString: String): String {
        return try {
            val obj = JSONObject(jsonString)
            obj.optString("text", "")
        } catch (_: Exception) {
            ""
        }
    }
}
