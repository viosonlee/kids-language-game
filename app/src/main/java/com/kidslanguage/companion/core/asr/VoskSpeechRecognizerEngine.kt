package com.kidslanguage.companion.core.asr

import android.content.Context
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.vosk.android.RecognitionListener

/** Local recognition only. A missing/broken model is reported to the recovery UI. */
class VoskSpeechRecognizerEngine internal constructor(
    private val modelPath: suspend (LanguageCode) -> String?,
    private val openModel: (String) -> OfflineSpeechModel,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val report: (Throwable) -> Unit = { it.printStackTrace() },
) : SpeechRecognizerEngine {
    constructor(context: Context) : this(
        modelPath = { ModelManager.getOrExtractModelPath(context.applicationContext, it) },
        openModel = ::NativeSpeechModel,
    )

    private var currentModel: OfflineSpeechModel? = null
    private var currentRecognizer: OfflineSpeechRecognizer? = null
    private var microphone: OfflineSpeechMicrophone? = null
    private var currentLanguage: LanguageCode? = null
    private var activeCallback: ((RecognitionResult) -> Unit)? = null
    private var generation = 0
    private val modelLock = Mutex()

    override suspend fun load(language: LanguageCode): Result<Unit> = withContext(Dispatchers.Main) {
        modelLock.withLock {
            if (currentLanguage == language && currentModel != null) return@withLock Result.success(Unit)
            releaseCurrentModel()?.let { return@withLock Result.failure(it) }
            var pendingModel: OfflineSpeechModel? = null
            try {
                engineResult {
                    val path = modelPath(language) ?: error("Offline language model is unavailable")
                    // Assign inside IO: cancellation when returning to Main must not leak the model.
                    withContext(ioDispatcher) { pendingModel = openModel(path) }
                    currentModel = pendingModel
                    pendingModel = null
                    currentLanguage = language
                }
            } finally {
                withContext(NonCancellable + ioDispatcher) { cleanup { pendingModel?.close() } }
            }
        }
    }

    override suspend fun recognizeFromAudio(pcm: ByteArray): RecognitionResult = withContext(Dispatchers.Main) {
        modelLock.withLock {
            val model = currentModel ?: return@withLock failure("Offline language model is not ready")
            engineResult {
                withContext(ioDispatcher) {
                    val recognizer = model.recognizer(SAMPLE_RATE)
                    try {
                        recognizer.accept(pcm)
                        RecognitionResult(parseVoskJson(recognizer.finalResult()))
                    } finally {
                        cleanup { recognizer.close() }
                    }
                }
            }.getOrElse { failure(it.message) }
        }
    }

    override suspend fun startListening(language: LanguageCode, onResult: (RecognitionResult) -> Unit): Unit =
        withContext(Dispatchers.Main) {
            disposeRecording()?.let { onResult(failure(it.message)); return@withContext }
            val model = currentModel
            if (currentLanguage != language || model == null) {
                onResult(failure("Offline language model is not ready"))
                return@withContext
            }
            val request = generation
            activeCallback = onResult
            try {
                engineResult {
                    val recognizer = model.recognizer(SAMPLE_RATE)
                    currentRecognizer = recognizer
                    val service = recognizer.microphone(SAMPLE_RATE)
                    microphone = service
                    val started = service.start(object : RecognitionListener {
                        override fun onPartialResult(hypothesis: String?) {}
                        override fun onResult(hypothesis: String?) {
                            val text = parseVoskJson(hypothesis)
                            if (text.isNotBlank()) finish(request, RecognitionResult(text))
                        }
                        override fun onFinalResult(hypothesis: String?) =
                            finish(request, RecognitionResult(parseVoskJson(hypothesis)))
                        override fun onError(exception: Exception?) = finish(request, failure(exception?.message))
                        override fun onTimeout() = finish(request, RecognitionResult("", error = "Timeout"))
                    }, RECORDING_TIMEOUT_MILLIS)
                    if (!started) finish(request, failure("Microphone did not start"))
                }.exceptionOrNull()?.let { finish(request, failure(it.message)) }
            } catch (e: CancellationException) {
                disposeRecording()
                throw e
            }
        }

    override fun stopListening() {
        val request = generation
        engineResult { microphone?.stop() }.exceptionOrNull()?.let { finish(request, failure(it.message)) }
    }

    override fun cancelListening() { disposeRecording() }

    override suspend fun unload() = withContext(Dispatchers.Main) {
        modelLock.withLock { releaseCurrentModel(); Unit }
    }

    private fun finish(request: Int, result: RecognitionResult) {
        if (request != generation) return
        val callback = activeCallback ?: return
        val cleanupError = disposeRecording()
        callback(if (cleanupError == null) result else failure(cleanupError.message))
    }

    private fun disposeRecording(): Throwable? {
        // Invalidate before any native call, which may synchronously deliver a final callback.
        generation++
        activeCallback = null
        val service = microphone
        val cancelled = cleanup { service?.cancel() }
        val stopped = cancelled == null || cleanup { service?.stop() } == null
        val shutdownError = cleanup { service?.shutdown() }
        if (!stopped) {
            // Keep ownership and refuse model replacement until the worker can be stopped.
            // Closing a recognizer still in use by the worker would risk a native crash.
            return cancelled
        }
        microphone = null
        val recognizer = currentRecognizer
        currentRecognizer = null
        val closeError = cleanup { recognizer?.close() }
        return shutdownError ?: closeError
    }

    private suspend fun releaseCurrentModel(): Throwable? {
        val recordingError = disposeRecording()
        if (microphone != null) return recordingError
        val previous = currentModel
        currentModel = null
        currentLanguage = null
        val modelError = withContext(NonCancellable + ioDispatcher) { cleanup { previous?.close() } }
        return recordingError ?: modelError
    }

    private inline fun <T> engineResult(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        report(e)
        Result.failure(e)
    } catch (e: LinkageError) {
        report(e)
        Result.failure(e)
    }

    private inline fun cleanup(block: () -> Unit): Throwable? = try {
        block()
        null
    } catch (e: Exception) {
        report(e)
        e
    } catch (e: LinkageError) {
        report(e)
        e
    }

    private fun parseVoskJson(value: String?): String = try {
        val text = Json.parseToJsonElement(value.orEmpty()).jsonObject["text"] as? JsonPrimitive
        if (text?.isString == true) text.content.trim() else ""
    } catch (_: Exception) { "" }

    private fun failure(message: String?) = RecognitionResult("", isSuccess = false, error = message)

    private companion object {
        const val SAMPLE_RATE = 16000.0f
        const val RECORDING_TIMEOUT_MILLIS = 10000
    }
}
