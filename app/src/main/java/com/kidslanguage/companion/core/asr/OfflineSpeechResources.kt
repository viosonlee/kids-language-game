package com.kidslanguage.companion.core.asr

import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

// Explicit ownership boundaries keep JNI resources separate from session callbacks.
internal interface OfflineSpeechModel {
    fun recognizer(sampleRate: Float): OfflineSpeechRecognizer
    fun close()
}

internal interface OfflineSpeechRecognizer {
    fun microphone(sampleRate: Float): OfflineSpeechMicrophone
    fun accept(pcm: ByteArray)
    fun finalResult(): String
    fun close()
}

internal interface OfflineSpeechMicrophone {
    fun start(listener: RecognitionListener, timeoutMillis: Int): Boolean
    fun stop()
    fun cancel()
    fun shutdown()
}

internal class NativeSpeechModel(path: String) : OfflineSpeechModel {
    private val model = Model(path)
    override fun recognizer(sampleRate: Float): OfflineSpeechRecognizer = NativeSpeechRecognizer(Recognizer(model, sampleRate))
    override fun close() = model.close()
}

private class NativeSpeechRecognizer(private val recognizer: Recognizer) : OfflineSpeechRecognizer {
    override fun microphone(sampleRate: Float): OfflineSpeechMicrophone =
        NativeSpeechMicrophone(SpeechService(recognizer, sampleRate))
    override fun accept(pcm: ByteArray) { recognizer.acceptWaveForm(pcm, pcm.size) }
    override fun finalResult(): String = recognizer.finalResult
    override fun close() = recognizer.close()
}

private class NativeSpeechMicrophone(private val service: SpeechService) : OfflineSpeechMicrophone {
    override fun start(listener: RecognitionListener, timeoutMillis: Int) = service.startListening(listener, timeoutMillis)
    override fun stop() { service.stop() }
    override fun cancel() { service.cancel() }
    override fun shutdown() = service.shutdown()
}
