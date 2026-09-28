package com.kidslanguage.companion.core.asr

import android.content.Context
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    override suspend fun load(language: LanguageCode): Result<Unit> = withContext(Dispatchers.IO) {
        if (currentLanguage == language && currentModel != null) {
            return@withContext Result.success(Unit)
        }

        // Unload old model to conserve memory
        unload()
        currentLanguage = language

        val modelPath = getModelPathForLanguage(language)
        if (modelPath != null && File(modelPath).exists()) {
            try {
                val model = Model(modelPath)
                currentModel = model
                isUsingFallback = false
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to system engine when offline model files are not yet unpacked
        isUsingFallback = true
        fallbackEngine.load(language)
    }

    override suspend fun recognizeFromAudio(pcm: ByteArray): RecognitionResult = withContext(Dispatchers.IO) {
        if (isUsingFallback || currentModel == null) {
            return@withContext fallbackEngine.recognizeFromAudio(pcm)
        }

        try {
            val recognizer = Recognizer(currentModel, 16000.0f)
            recognizer.acceptWaveForm(pcm, pcm.size)
            val jsonResult = recognizer.finalResult
            recognizer.close()

            val text = parseVoskJson(jsonResult)
            RecognitionResult(text = text, isSuccess = true)
        } catch (e: Exception) {
            RecognitionResult(text = "", isSuccess = false, error = e.message)
        }
    }

    override suspend fun startListening(
        language: LanguageCode,
        onResult: (RecognitionResult) -> Unit
    ) = withContext(Dispatchers.Main) {
        activeCallback = onResult

        if (isUsingFallback || currentModel == null) {
            fallbackEngine.startListening(language, onResult)
            return@withContext
        }

        try {
            val recognizer = Recognizer(currentModel, 16000.0f)
            currentRecognizer = recognizer

            val service = SpeechService(recognizer, 16000.0f)
            speechService = service

            service.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {}

                override fun onResult(hypothesis: String?) {
                    if (!hypothesis.isNullOrBlank()) {
                        val text = parseVoskJson(hypothesis)
                        if (text.isNotBlank()) {
                            activeCallback?.invoke(RecognitionResult(text = text, isSuccess = true))
                        }
                    }
                }

                override fun onFinalResult(hypothesis: String?) {
                    val text = if (!hypothesis.isNullOrBlank()) parseVoskJson(hypothesis) else ""
                    activeCallback?.invoke(RecognitionResult(text = text, isSuccess = true))
                }

                override fun onError(exception: java.lang.Exception?) {
                    activeCallback?.invoke(
                        RecognitionResult(text = "", isSuccess = false, error = exception?.message)
                    )
                }

                override fun onTimeout() {
                    activeCallback?.invoke(
                        RecognitionResult(text = "", isSuccess = true, error = "Timeout")
                    )
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback immediately if native recording throws
            isUsingFallback = true
            fallbackEngine.startListening(language, onResult)
        }
    }

    override fun stopListening() {
        if (isUsingFallback) {
            fallbackEngine.stopListening()
        } else {
            try {
                speechService?.stop()
                speechService?.shutdown()
                speechService = null
                currentRecognizer?.close()
                currentRecognizer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun unload() = withContext(Dispatchers.IO) {
        stopListening()
        currentModel?.close()
        currentModel = null
        currentLanguage = null
        if (isUsingFallback) {
            fallbackEngine.unload()
        }
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
