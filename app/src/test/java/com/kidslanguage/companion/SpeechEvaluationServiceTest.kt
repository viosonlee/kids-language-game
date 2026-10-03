package com.kidslanguage.companion

import com.kidslanguage.companion.core.model.EvaluationStatus
import com.kidslanguage.companion.core.model.PracticeItem
import com.kidslanguage.companion.core.scoring.ChildSpeechEvaluationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpeechEvaluationServiceTest {

    private lateinit var service: ChildSpeechEvaluationService

    @Test fun `kanji and kana with identical complete readings pass`() {
        listOf("順番" to "じゅんばん", "眠い" to "ねむい", "明日" to "あした",
            "一緒に遊ぼう" to "いっしょにあそぼう", "図書館" to "としょかん").forEach { (spoken, target) ->
            val item = PracticeItem("reading", "ja-JP", "daily", text = target)
            val result = service.evaluate(spoken, item, 1)
            assertEquals("$spoken / $target", EvaluationStatus.Pass, result.status)
            assertEquals(1f, result.score, 0f)
            assertEquals(spoken, result.recognizedText)
        }
    }

    @Test fun `reading equality preserves word order unknown words and long vowels`() {
        listOf("猫と犬" to "犬と猫", "眠い" to "寒い", "順番xyz" to "順番",
            "コート" to "コト").forEach { (left, right) ->
            org.junit.Assert.assertNotEquals("$left / $right", service.japaneseReading(left), service.japaneseReading(right))
        }
        val item = PracticeItem("reading", "ja-JP", "daily", text = "じゅんばん")
        assertEquals(EvaluationStatus.Retry, service.evaluate("飛行機", item, 1).status)
        assertEquals(EvaluationStatus.NoSpeech, service.evaluate("...", item, 1).status)
    }

    @Before
    fun setUp() {
        service = ChildSpeechEvaluationService()
    }

    @Test
    fun `exact match passes with high score`() {
        val item = PracticeItem(
            id = "test_1",
            language = "ja-JP",
            category = "greetings",
            text = "おはよう",
            reading = "おはよう"
        )
        val result = service.evaluate("おはよう", item, attemptCount = 1)
        assertEquals(EvaluationStatus.Pass, result.status)
        assertTrue(result.score >= 0.95f)
    }

    @Test
    fun `alias match passes`() {
        val item = PracticeItem(
            id = "test_2",
            language = "ja-JP",
            category = "play",
            text = "いっしょにあそぼう",
            aliases = listOf("いっしょにあそぼ", "あそぼう")
        )
        val result = service.evaluate("いっしょにあそぼ", item, attemptCount = 1)
        assertEquals(EvaluationStatus.Pass, result.status)
    }

    @Test
    fun `katakana to hiragana normalizes and passes`() {
        val item = PracticeItem(
            id = "test_3",
            language = "ja-JP",
            category = "help",
            text = "トイレにいきたい",
            aliases = listOf("といれ")
        )
        // Spoken with hiragana
        val result = service.evaluate("といれにいきたい", item, attemptCount = 1)
        assertEquals(EvaluationStatus.Pass, result.status)
    }

    @Test
    fun `empty input returns NoSpeech`() {
        val item = PracticeItem(
            id = "test_4",
            language = "ja-JP",
            category = "greetings",
            text = "ありがとう"
        )
        val result = service.evaluate("   ", item, attemptCount = 1)
        assertEquals(EvaluationStatus.NoSpeech, result.status)
    }

    @Test
    fun `repeated attempt with partial similarity soft passes for preschooler`() {
        val item = PracticeItem(
            id = "test_5",
            language = "ja-JP",
            category = "daily",
            text = "おみずください"
        )
        // Partial child articulation on 2nd attempt
        val result = service.evaluate("おみずくだ", item, attemptCount = 2)
        assertTrue(
            result.status == EvaluationStatus.Pass || result.status == EvaluationStatus.SoftPass
        )
    }

    @Test
    fun `english phrases ignore casing and punctuation`() {
        val item = PracticeItem(
            id = "test_en",
            language = "en-US",
            category = "greetings",
            text = "Good morning"
        )
        val result = service.evaluate("good morning!", item, attemptCount = 1)
        assertEquals(EvaluationStatus.Pass, result.status)
    }

    @Test
    fun `attempt count 3 or more passes gently as long as child spoke`() {
        val item = PracticeItem(
            id = "test_6",
            language = "ja-JP",
            category = "daily",
            text = "おみずください"
        )
        // Child made an effort with very soft/fuzzy speech
        val result = service.evaluate("お", item, attemptCount = 3)
        assertEquals(EvaluationStatus.SoftPass, result.status)
        assertTrue(result.message.contains("努力") || result.message.contains("太棒啦"))
    }

    private fun englishItem() = PracticeItem(
        id = "hello", language = "en-US", category = "greetings",
        text = "Hello", aliases = listOf("hi", "hey", "", "！")
    )

    @Test
    fun `punctuation and emoji do not count as speech on any attempt`() {
        for (attempt in 1..4) {
            for (input in listOf("！ …　", "🫧🎉", "---")) {
                assertEquals(EvaluationStatus.NoSpeech, service.evaluate(input, englishItem(), attempt).status)
            }
        }
    }

    @Test
    fun `empty aliases and tiny alias fragments cannot grant a pass`() {
        for (input in listOf("h", "xyz")) {
            assertEquals(EvaluationStatus.Retry, service.evaluate(input, englishItem(), 1).status)
        }
    }

    @Test
    fun `english alias must be a whole word`() {
        assertEquals(EvaluationStatus.Retry, service.evaluate("this", englishItem(), 1).status)
        assertEquals(EvaluationStatus.Pass, service.evaluate("hi there", englishItem(), 1).status)
    }

    @Test
    fun `unicode width and kana voicing normalize consistently`() {
        assertEquals(EvaluationStatus.Pass, service.evaluate("ＨＥＬＬＯ！", englishItem(), 1).status)
        val item = PracticeItem(id = "bag", language = "ja-JP", category = "daily", text = "ばっぐ")
        assertEquals(EvaluationStatus.Pass, service.evaluate("ﾊﾞｯｸﾞ", item, 1).status)
    }

    @Test
    fun `invalid target reports error instead of granting a pass`() {
        val item = englishItem().copy(text = "...", aliases = listOf("hi"))
        assertEquals(EvaluationStatus.Error, service.evaluate("hi", item, 3).status)
    }

    @Test
    fun `latest spec retains second attempt assistance and third attempt protection`() {
        val item = englishItem().copy(aliases = emptyList())
        assertEquals(EvaluationStatus.Retry, service.evaluate("he", item, 1).status)
        assertEquals(EvaluationStatus.SoftPass, service.evaluate("he", item, 2).status)
        assertEquals(EvaluationStatus.SoftPass, service.evaluate("xyz", item, 3).status)
    }
}
