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
}
