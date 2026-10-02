package com.kidslanguage.companion.core.asr

import com.kidslanguage.companion.core.model.LanguageCode

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
