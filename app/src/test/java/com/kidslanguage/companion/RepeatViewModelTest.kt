package com.kidslanguage.companion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.kidslanguage.companion.core.asr.*
import com.kidslanguage.companion.core.audio.PromptPlayer
import com.kidslanguage.companion.core.content.ContentRepository
import com.kidslanguage.companion.core.model.*
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.core.scoring.ChildSpeechEvaluationService
import com.kidslanguage.companion.feature.repeat.RepeatViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepeatViewModelTest {
    private class Store : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            return transform(data.value).also { data.value = it }
        }
    }

    private class Recorder : SpeechRecognizerEngine {
        val callbacks = mutableListOf<(RecognitionResult) -> Unit>()
        var recording = false
        var finalOnStop = false
        var loadDelay = 0L
        var loadFails = false
        override suspend fun load(language: LanguageCode): Result<Unit> {
            delay(loadDelay)
            return if (loadFails) Result.failure(IllegalStateException("missing model")) else Result.success(Unit)
        }
        override suspend fun recognizeFromAudio(pcm: ByteArray) = RecognitionResult("hello")
        override suspend fun startListening(language: LanguageCode, onResult: (RecognitionResult) -> Unit) {
            recording = true
            callbacks += onResult
        }
        override fun stopListening() {
            if (finalOnStop) callbacks.last()(RecognitionResult("hello"))
        }
        override fun cancelListening() { recording = false }
        override suspend fun unload() {}
    }

    private class Player : PromptPlayer {
        var voices = 0
        var playing = false
        override suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String?) {}
        override suspend fun playVoiceAsset(audioAssetPath: String) {
            voices++
            playing = true
            delay(1000)
            playing = false
        }
        override fun stop() { playing = false }
        override fun release() = stop()
    }

    private fun scenario(autoNext: Boolean = true, block: suspend TestScope.(RepeatViewModel, Recorder, Player) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val recorder = Recorder()
        val player = Player()
        val content = object : ContentRepository {
            override suspend fun getCategories(language: LanguageCode) = emptyList<Category>()
            override suspend fun getItems(language: LanguageCode, categoryId: String?) = listOf(
                PracticeItem("1", "en-US", "greetings", text = "hello"),
                PracticeItem("2", "en-US", "greetings", text = "goodbye")
            )
            override suspend fun getItemById(id: String): PracticeItem? = null
        }
        val settings = SettingsRepository(Store())
        settings.setAutoNext(autoNext)
        val vm = RepeatViewModel(content, player, recorder, ChildSpeechEvaluationService(), settings)
        try {
            vm.loadSession(LanguageCode.EN_US, null)
            runCurrent()
            block(vm, recorder, player)
        } finally {
            vm.stopSession()
            Dispatchers.resetMain()
        }
    }

    @Test fun duplicateAndOldCallbacksCannotScoreOrAdvanceAgain() = scenario { vm, recorder, player ->
        vm.toggleRecording()
        runCurrent()
        val callback = recorder.callbacks.last()
        callback(RecognitionResult("hello"))
        assertFalse(recorder.recording)
        repeat(5) { callback(RecognitionResult("hello")) }
        assertEquals(1, vm.uiState.value.attemptCount)
        advanceUntilIdle()
        assertEquals(1, player.voices)
        assertEquals(1, vm.uiState.value.currentIndex)
        vm.toggleRecording()
        runCurrent()
        callback(RecognitionResult("hello"))
        assertTrue(vm.uiState.value.isRecording)
        assertEquals(0, vm.uiState.value.attemptCount)
    }

    @Test fun failureAndSilenceNeverCountAsAttempts() = scenario { vm, recorder, player ->
        for (result in listOf(RecognitionResult("", isSuccess = false), RecognitionResult(""))) {
            vm.toggleRecording()
            runCurrent()
            recorder.callbacks.last()(result)
            assertEquals(0, vm.uiState.value.attemptCount)
            assertFalse(vm.uiState.value.showFeedbackOverlay)
            assertNotNull(vm.uiState.value.recordingError)
            assertFalse(recorder.recording)
        }
        advanceUntilIdle()
        assertEquals(0, player.voices)
    }

    @Test fun leavingCancelsDelayedPraiseAndIgnoresLateResults() = scenario { vm, recorder, player ->
        vm.toggleRecording()
        runCurrent()
        val callback = recorder.callbacks.last()
        callback(RecognitionResult("hello"))
        vm.stopSession()
        repeat(5) { callback(RecognitionResult("hello")) }
        advanceUntilIdle()
        assertEquals(0, player.voices)
        assertEquals(0, vm.uiState.value.currentIndex)
        assertFalse(recorder.recording)
    }

    @Test fun leavingDuringPraiseStopsPlaybackAndAdvance() = scenario { vm, recorder, player ->
        vm.toggleRecording()
        runCurrent()
        recorder.callbacks.last()(RecognitionResult("hello"))
        advanceTimeBy(400)
        assertTrue(player.playing)
        vm.stopSession()
        advanceUntilIdle()
        assertFalse(player.playing)
        assertEquals(0, vm.uiState.value.currentIndex)
    }

    @Test fun synchronousFinalOnStopDoesNotOverwritePassedState() = scenario { vm, recorder, _ ->
        recorder.finalOnStop = true
        vm.toggleRecording()
        runCurrent()
        vm.toggleRecording()
        assertEquals(SessionStepState.Passed, vm.uiState.value.sessionState)
    }

    @Test fun speakerCannotEnableRecordingBeforeModelIsLoaded() = scenario { vm, recorder, _ ->
        recorder.loadDelay = 1000
        vm.loadSession(LanguageCode.ZH_CN, null)
        runCurrent()
        vm.playCurrentPrompt()
        vm.toggleRecording()
        runCurrent()
        assertEquals(SessionStepState.Loading, vm.uiState.value.sessionState)
        assertFalse(recorder.recording)
        advanceUntilIdle()
        assertEquals(SessionStepState.ReadyToSpeak, vm.uiState.value.sessionState)
    }

    @Test fun modelLoadFailureRemainsVisible() = scenario { vm, recorder, _ ->
        recorder.loadFails = true
        vm.loadSession(LanguageCode.EN_US, null)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.recordingError)
        assertFalse(vm.uiState.value.isRecording)
    }

    @Test fun manualNextWaitsForUserAndCannotSkipDuringPraise() = scenario(autoNext = false) { vm, recorder, _ ->
        vm.toggleRecording()
        runCurrent()
        recorder.callbacks.last()(RecognitionResult("hello"))
        vm.nextItem()
        assertEquals(0, vm.uiState.value.currentIndex)
        advanceUntilIdle()
        vm.dismissFeedback()
        assertTrue(vm.uiState.value.showFeedbackOverlay)
        assertFalse(vm.uiState.value.feedbackPlaying)
        assertEquals(0, vm.uiState.value.currentIndex)
        vm.nextItem()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.currentIndex)
    }

    @Test fun resumeKeepsPositionAndRetryAttempts() = scenario { vm, recorder, _ ->
        vm.toggleRecording()
        runCurrent()
        recorder.callbacks.last()(RecognitionResult("hello"))
        advanceUntilIdle()
        vm.toggleRecording()
        runCurrent()
        recorder.callbacks.last()(RecognitionResult("unrelated"))
        advanceUntilIdle()
        vm.stopSession()
        vm.loadSession(LanguageCode.EN_US, null)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.currentIndex)
        assertEquals(1, vm.uiState.value.attemptCount)
        assertEquals(SessionStepState.ReadyToSpeak, vm.uiState.value.sessionState)
    }

    @Test fun completionIsConsumedOnceAndReplayStartsFresh() = scenario { vm, recorder, _ ->
        for (answer in listOf("hello", "goodbye")) {
            vm.toggleRecording()
            runCurrent()
            recorder.callbacks.last()(RecognitionResult(answer))
            advanceUntilIdle()
        }
        assertTrue(vm.consumeSessionCompletion())
        assertFalse(vm.consumeSessionCompletion())
        vm.stopSession()
        vm.loadSession(LanguageCode.EN_US, null)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.currentIndex)
        assertEquals(0, vm.uiState.value.attemptCount)
        assertFalse(vm.uiState.value.isSessionComplete)
    }
}
