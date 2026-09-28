package com.kidslanguage.companion.core.scoring

import com.kidslanguage.companion.core.model.EvaluationResult
import com.kidslanguage.companion.core.model.EvaluationStatus
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.PracticeItem
import kotlin.math.max
import kotlin.math.min

interface SpeechEvaluationService {
    fun evaluate(
        spokenText: String,
        targetItem: PracticeItem,
        attemptCount: Int
    ): EvaluationResult
}

class ChildSpeechEvaluationService : SpeechEvaluationService {

    override fun evaluate(
        spokenText: String,
        targetItem: PracticeItem,
        attemptCount: Int
    ): EvaluationResult {
        val trimmed = spokenText.trim()
        if (trimmed.isEmpty()) {
            return EvaluationResult(
                status = EvaluationStatus.NoSpeech,
                score = 0f,
                recognizedText = "",
                targetText = targetItem.text,
                message = "没有听到声音哦，再试一次吧"
            )
        }

        val normSpoken = normalizeText(trimmed, targetItem.language)
        val normTarget = normalizeText(targetItem.text, targetItem.language)

        // 1. Exact match with target
        if (normSpoken == normTarget) {
            return EvaluationResult(
                status = EvaluationStatus.Pass,
                score = 1.0f,
                recognizedText = spokenText,
                targetText = targetItem.text,
                message = "发音很棒！"
            )
        }

        // 2. Check aliases
        for (alias in targetItem.aliases) {
            val normAlias = normalizeText(alias, targetItem.language)
            if (normSpoken == normAlias || normSpoken.contains(normAlias) || normAlias.contains(normSpoken)) {
                return EvaluationResult(
                    status = EvaluationStatus.Pass,
                    score = 0.95f,
                    recognizedText = spokenText,
                    targetText = targetItem.text,
                    message = "太棒啦！"
                )
            }
        }

        // 3. Substring check
        if (normSpoken.contains(normTarget) || (normTarget.length >= 3 && normTarget.contains(normSpoken))) {
            return EvaluationResult(
                status = EvaluationStatus.Pass,
                score = 0.9f,
                recognizedText = spokenText,
                targetText = targetItem.text,
                message = "真聪明！"
            )
        }

        // 4. Similarity calculation (Levenshtein distance)
        val similarity = calculateSimilarity(normSpoken, normTarget)

        return when {
            similarity >= 0.65f -> {
                EvaluationResult(
                    status = EvaluationStatus.Pass,
                    score = similarity,
                    recognizedText = spokenText,
                    targetText = targetItem.text,
                    message = "真不错！"
                )
            }
            attemptCount >= 2 && similarity >= 0.35f -> {
                // Generous for preschoolers: after 2 attempts, do not block child
                EvaluationResult(
                    status = EvaluationStatus.SoftPass,
                    score = similarity,
                    recognizedText = spokenText,
                    targetText = targetItem.text,
                    message = "有很大进步，我们继续吧！"
                )
            }
            else -> {
                EvaluationResult(
                    status = EvaluationStatus.Retry,
                    score = similarity,
                    recognizedText = spokenText,
                    targetText = targetItem.text,
                    message = "再听一次，跟着兔子一起说吧！"
                )
            }
        }
    }

    private fun normalizeText(text: String, language: String): String {
        var result = text.lowercase()
            .replace(Regex("[\\p{Punct}\\s、。！？・「」…ー\\-]"), "")

        // If Japanese, convert basic Katakana to Hiragana for loose matching
        if (language.startsWith("ja")) {
            val sb = StringBuilder()
            for (ch in result) {
                if (ch in '\u30A1'..'\u30F6') {
                    sb.append((ch.code - 0x60).toChar())
                } else {
                    sb.append(ch)
                }
            }
            result = sb.toString()
        }
        return result
    }

    private fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        val distance = levenshteinDistance(s1, s2)
        val maxLength = max(s1.length, s2.length)
        return max(0f, 1.0f - (distance.toFloat() / maxLength.toFloat()))
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }

        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}
