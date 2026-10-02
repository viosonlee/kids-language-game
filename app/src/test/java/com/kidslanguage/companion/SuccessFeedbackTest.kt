package com.kidslanguage.companion

import com.kidslanguage.companion.core.audio.*
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SuccessFeedbackTest {
    private class Effects : SoundEffectManager {
        var playing = false
        override fun playSuccess() { playing = true }
        override fun stop() { playing = false }
        override fun playPop() {}
        override fun playCelebrate() {}
        override fun playRetry() {}
        override fun release() {}
    }

    private class Player(val effects: Effects, val duration: Long) : PromptPlayer {
        var started = false
        var finished = false
        var cancelled = false
        override suspend fun playVoiceAsset(audioAssetPath: String, volume: Float) {
            assertFalse("Chime must finish before speech", effects.playing)
            started = true
            try { delay(duration); finished = true } finally { cancelled = !finished }
        }
        override suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String?) {}
        override fun stop() {}
        override fun release() {}
    }

    @Test fun fullChimeAndVoiceFinishWithinBudget() = runTest {
        val effects = Effects()
        val player = Player(effects, 950)
        val feedback = launch { player.playSuccessFeedback(PaoPaoVoices.PRAISE_1, effects) }
        runCurrent()
        assertTrue(effects.playing)
        advanceTimeBy(549); runCurrent()
        assertFalse(player.started)
        advanceTimeBy(1); runCurrent()
        assertTrue(player.started)
        advanceTimeBy(949); runCurrent()
        assertTrue(feedback.isActive)
        advanceTimeBy(1); runCurrent()
        assertTrue(player.finished)
        assertTrue(feedback.isCompleted)
        assertEquals(1500L, testScheduler.currentTime)
    }

    @Test fun mutedFeedbackStillHasMinimumVisualDuration() = runTest {
        val effects = Effects()
        val player = Player(effects, 0)
        val feedback = launch { player.playSuccessFeedback(PaoPaoVoices.PRAISE_1, effects) }
        advanceTimeBy(799); runCurrent()
        assertTrue(feedback.isActive)
        advanceTimeBy(1); runCurrent()
        assertTrue(feedback.isCompleted)
    }

    @Test fun slowPlaybackIsNeverCutOffToMeetTheNominalBudget() = runTest {
        val effects = Effects()
        val player = Player(effects, 3000)
        val feedback = launch { player.playSuccessFeedback(PaoPaoVoices.PRAISE_1, effects) }
        advanceTimeBy(1500); runCurrent()
        assertTrue(feedback.isActive)
        assertFalse(player.finished)
        advanceUntilIdle()
        assertTrue(player.finished)
        assertEquals(3550L, testScheduler.currentTime)
    }

    @Test fun leavingDuringSpeechCancelsRemainingFeedback() = runTest {
        val effects = Effects()
        val player = Player(effects, 950)
        var advanced = false
        val feedback = launch { player.playSuccessFeedback(PaoPaoVoices.PRAISE_1, effects); advanced = true }
        advanceTimeBy(600); runCurrent()
        feedback.cancelAndJoin()
        advanceUntilIdle()
        assertTrue(player.cancelled)
        assertFalse(advanced)
    }

    @Test fun leavingDuringChimeStopsItWithoutStartingSpeech() = runTest {
        val effects = Effects()
        val player = Player(effects, 950)
        val feedback = launch { player.playSuccessFeedback(PaoPaoVoices.PRAISE_1, effects) }
        advanceTimeBy(200); runCurrent()
        assertTrue(effects.playing)
        feedback.cancelAndJoin()
        advanceUntilIdle()
        assertFalse(effects.playing)
        assertFalse(player.started)
    }
}
