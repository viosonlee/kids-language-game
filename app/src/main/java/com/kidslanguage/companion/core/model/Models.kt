package com.kidslanguage.companion.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class LanguageCode(val code: String, val displayName: String, val englishName: String) {
    JA_JP("ja-JP", "日本語", "Japanese"),
    ZH_CN("zh-CN", "中文", "Chinese"),
    EN_US("en-US", "English", "English");

    companion object {
        fun fromCode(code: String): LanguageCode {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) }
                ?: JA_JP
        }
    }
}

@Serializable
enum class PracticeMode(val id: String, val title: String, val subtitle: String) {
    REPEAT("repeat", "听一听，说一说", "Listen & Repeat"),
    CHOOSE("choose", "听一听，选图片", "Listen & Choose")
}

@Serializable
enum class MascotState {
    Idle,
    Listen,
    Speak,
    Great,
    TryAgain,
    Praise
}

@Serializable
data class Category(
    val id: String,
    val language: String,
    val title: String,
    val description: String = "",
    val icon: String = "category_default",
    val order: Int = 0
)

@Serializable
data class PracticeItem(
    val id: String,
    val language: String,
    val category: String,
    val modes: List<String> = listOf("repeat", "choose"),
    val text: String,
    val reading: String? = null,
    val pinyin: String? = null,
    val translation: String? = null,
    val audio: String? = null,
    val image: String? = null,
    val aliases: List<String> = emptyList(),
    val distractors: List<String> = emptyList()
)

enum class SessionStepState {
    Loading,
    PromptPlaying,
    ReadyToSpeak,
    Recording,
    Recognizing,
    Evaluating,
    Passed,
    Retry,
    SoftPassed,
    Completed
}

enum class EvaluationStatus {
    Pass,
    Retry,
    SoftPass,
    NoSpeech,
    Error
}

data class EvaluationResult(
    val status: EvaluationStatus,
    val score: Float,
    val recognizedText: String,
    val targetText: String,
    val message: String = ""
)
