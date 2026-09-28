package com.kidslanguage.companion

import com.kidslanguage.companion.core.model.*
import com.kidslanguage.companion.core.persistence.*
import org.junit.Assert.*
import org.junit.Test

class ProgressRecordTest {
    private val item = PracticeItem("hello", "en-US", "greetings", text = "hello")

    @Test fun invalidSpeechDoesNotBecomeAnAttempt() {
        assertNull(repeatAttempt(item, "run", 1, EvaluationStatus.NoSpeech, 100))
        assertNull(repeatAttempt(item, "run", 1, EvaluationStatus.Error, 100))
    }

    @Test fun repeatedItemKeepsIdentityAndDistinguishesAssistedPass() {
        val retry = repeatAttempt(item, "run", 1, EvaluationStatus.Retry, 100)!!
        val passed = repeatAttempt(item, "run", 2, EvaluationStatus.SoftPass, 200)!!
        assertEquals(retry.itemRunId, passed.itemRunId)
        assertNotEquals(retry.attemptId, passed.attemptId)
        assertEquals(0, retry.completed)
        assertEquals(1, passed.assistedPasses)
        assertEquals(0, passed.normalPasses)
        assertEquals(1, passed.completed)
        assertEquals("en-US", passed.language)
    }

    @Test fun assistedChooseCompletionDoesNotFalsifyCorrectAnswer() {
        val attempt = chooseAttempt(item, "run", 3, correct = false, completed = true, now = 100)
        assertEquals(1, attempt.chooseWrong)
        assertEquals(0, attempt.chooseCorrect)
        assertEquals(1, attempt.completed)
        assertEquals(0, attempt.assistedPasses)
    }
}
