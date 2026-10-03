package com.kidslanguage.companion.core.scoring

import com.kidslanguage.companion.core.model.EvaluationResult
import com.kidslanguage.companion.core.model.EvaluationStatus
import com.kidslanguage.companion.core.model.PracticeItem
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import com.atilika.kuromoji.ipadic.Tokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface SpeechEvaluationService {
    suspend fun evaluateAsync(spokenText: String, targetItem: PracticeItem, attemptCount: Int): EvaluationResult =
        evaluate(spokenText, targetItem, attemptCount)
    fun evaluate(
        spokenText: String,
        targetItem: PracticeItem,
        attemptCount: Int
    ): EvaluationResult
}

class ChildSpeechEvaluationService : SpeechEvaluationService {
    override suspend fun evaluateAsync(spokenText: String, targetItem: PracticeItem, attemptCount: Int) =
        withContext(Dispatchers.Default) { evaluate(spokenText, targetItem, attemptCount) }

    private companion object {
        val japaneseTokenizer by lazy { Tokenizer() }
    }

    internal fun japaneseReading(text: String): String = japaneseTokenizer.tokenize(
        Normalizer.normalize(text, Normalizer.Form.NFKC)
    ).joinToString("") { token ->
        // Unknown tokens retain their surface; never discard unrecognized words.
        token.reading.takeUnless { it.isNullOrEmpty() || it == "*" } ?: token.surface
    }.let { normalizeText(it, "ja-JP", preserveLongVowels = true) }

    override fun evaluate(
        spokenText: String,
        targetItem: PracticeItem,
        attemptCount: Int
    ): EvaluationResult {
        val normSpoken = normalizeText(spokenText, targetItem.language)
        if (normSpoken.isEmpty()) {
            return EvaluationResult(
                status = EvaluationStatus.NoSpeech,
                score = 0f,
                recognizedText = "",
                targetText = targetItem.text,
                message = "还没听清，再说一次吧"
            )
        }

        val normTarget = normalizeText(targetItem.text, targetItem.language)
        if (normTarget.isEmpty()) {
            return EvaluationResult(
                status = EvaluationStatus.Error,
                score = 0f,
                recognizedText = spokenText,
                targetText = targetItem.text,
                message = "这一题暂时不可用，请换一题吧"
            )
        }

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

        // Add only full-reading equality; keep the existing fuzzy-scoring thresholds.
        if (targetItem.language.startsWith("ja") &&
            japaneseReading(spokenText) == japaneseReading(targetItem.text)) {
            return EvaluationResult(EvaluationStatus.Pass, 1f, spokenText, targetItem.text, "发音很棒！")
        }

        // 2. Check aliases
        for (alias in targetItem.aliases) {
            val normAlias = normalizeText(alias, targetItem.language)
            if (normAlias.isNotEmpty() && matchesPhrase(normSpoken, normAlias, targetItem.language)) {
                return EvaluationResult(
                    status = EvaluationStatus.Pass,
                    score = 0.95f,
                    recognizedText = spokenText,
                    targetText = targetItem.text,
                    message = "太棒啦！"
                )
            }
        }

        // 3. Substring check: spoken contains full target or covers >= 70% of target
        if (matchesPhrase(normSpoken, normTarget, targetItem.language)) {
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
            similarity >= 0.55f -> {
                EvaluationResult(
                    status = EvaluationStatus.Pass,
                    score = similarity,
                    recognizedText = spokenText,
                    targetText = targetItem.text,
                    message = "发音很棒！"
                )
            }
            attemptCount == 2 && similarity >= 0.25f -> {
                // Preschooler friendly: after 2nd attempt, partial similarity passes gently
                EvaluationResult(
                    status = EvaluationStatus.SoftPass,
                    score = similarity,
                    recognizedText = spokenText,
                    targetText = targetItem.text,
                    message = "很有进步哦，太棒啦！"
                )
            }
            attemptCount >= 3 -> {
                // Child toy protection: after 3rd attempt, as long as child spoke, celebrate effort!
                EvaluationResult(
                    status = EvaluationStatus.SoftPass,
                    score = max(0.5f, similarity),
                    recognizedText = spokenText,
                    targetText = targetItem.text,
                    message = "泡泡努力开口练习，太棒啦！"
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

    private fun normalizeText(text: String, language: String, preserveLongVowels: Boolean = false): String {
        val english = language.startsWith("en")
        var result = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
            .replace(Regex("[’']"), "")
            .let { if (preserveLongVowels) it else it.replace("ー", "") }
            .replace(Regex("[^\\p{L}\\p{N}\\p{M}]+"), if (english) " " else "")
            .trim()
        if (result.none { it.isLetterOrDigit() }) return ""

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

    private fun matchesPhrase(spoken: String, target: String, language: String): Boolean {
        if (spoken == target) return true
        val containsTarget = if (language.startsWith("en")) {
            // A short alias such as "hi" must be a word, not part of "this".
            (" $spoken ").contains(" $target ")
        } else {
            spoken.contains(target)
        }
        val spokenLength = spoken.count { !it.isWhitespace() }
        val targetLength = target.count { !it.isWhitespace() }
        return containsTarget || (spokenLength >= 3 && target.contains(spoken) &&
            spokenLength >= targetLength * 0.7f)
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
