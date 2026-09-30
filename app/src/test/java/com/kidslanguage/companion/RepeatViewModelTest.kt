package com.kidslanguage.companion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.kidslanguage.companion.core.asr.*
import com.kidslanguage.companion.core.audio.PromptPlayer
import com.kidslanguage.companion.core.audio.SettingsPromptPlayer
import com.kidslanguage.companion.core.content.ContentRepository
import com.kidslanguage.companion.core.model.*
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.core.persistence.TextDisplayMode
import com.kidslanguage.companion.core.scoring.ChildSpeechEvaluationService
import com.kidslanguage.companion.feature.repeat.RepeatViewModel
import com.kidslanguage.companion.feature.repeat.RepeatProblem
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
        var loadThrows = false
        override suspend fun load(language: LanguageCode): Result<Unit> {
            delay(loadDelay)
            if (loadThrows) throw IllegalStateException("engine unavailable")
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
        var prompts = 0
        var promptDelay = 0L
        var voices = 0
        var playing = false
        override suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String?) {
            prompts++
            delay(promptDelay)
        }
        override suspend fun playVoiceAsset(audioAssetPath: String, volume: Float) {
            voices++
            playing = true
            delay(1000)
            playing = false
        }
        override fun stop() { playing = false }
        override fun release() = stop()
    }

    private fun scenario(autoNext: Boolean = true, autoPlay: Boolean = true, autoRecord: Boolean = false,
                         permission: Boolean = false, promptDelay: Long = 0,
                         soundVolume: Float = 1f, textMode: TextDisplayMode = TextDisplayMode.NORMAL,
                         choiceContent: Boolean = true,
                         block: suspend TestScope.(RepeatViewModel, Recorder, Player) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val recorder = Recorder()
        val player = Player()
        player.promptDelay = promptDelay
        val content = object : ContentRepository {
            override suspend fun getCategories(language: LanguageCode) = emptyList<Category>()
            override suspend fun getItems(language: LanguageCode, categoryId: String?) = listOf(
                PracticeItem("1", "en-US", "greetings", text = "hello", image = if (choiceContent) "👋" else null),
                PracticeItem("2", "en-US", "greetings", text = "goodbye", image = if (choiceContent) "🏠" else null)
            )
            override suspend fun getItemById(id: String): PracticeItem? = null
        }
        val settings = SettingsRepository(Store())
        settings.setAutoNext(autoNext)
        settings.setAutoPlayPrompt(autoPlay)
        settings.setAutoStartRecording(autoRecord)
        settings.setSoundVolume(soundVolume)
        settings.setTextDisplayMode(textMode)
        val vm = RepeatViewModel(content, SettingsPromptPlayer(player, settings.settingsFlow), recorder, ChildSpeechEvaluationService(), settings,
            canRecord = { permission })
        try {
            vm.loadSession(LanguageCode.EN_US, null)
            runCurrent()
            block(vm, recorder, player)
        } finally {
            vm.stopSession()
            Dispatchers.resetMain()
        }
    }

    @Test fun mutedSuccessAdvancesAndKeepsSmallTextAfterResume() =
        scenario(soundVolume = 0f, textMode = TextDisplayMode.SMALL) { vm, recorder, player ->
            assertTrue(vm.uiState.value.smallText)
            vm.toggleRecording()
            runCurrent()
            recorder.callbacks.last()(RecognitionResult("hello"))
            advanceUntilIdle()
            assertEquals(0, player.voices)
            assertEquals(1, vm.uiState.value.currentIndex)
            assertEquals(2, player.prompts)
            vm.stopSession()
            vm.loadSession(LanguageCode.EN_US, null)
            advanceUntilIdle()
            assertEquals(1, vm.uiState.value.currentIndex)
            assertTrue(vm.uiState.value.smallText)
            assertTrue(vm.uiState.value.showText)
        }

    @Test fun disabledAutomaticPromptKeepsManualSpeakerAvailable() = scenario(autoPlay = false) { vm, _, player ->
        assertEquals(0, player.prompts)
        assertEquals(SessionStepState.ReadyToSpeak, vm.uiState.value.sessionState)
        vm.playCurrentPrompt()
        runCurrent()
        assertEquals(1, player.prompts)
    }

    @Test fun automaticRecordingWaitsForPromptAndStopsRetryingAfterSilence() =
        scenario(autoRecord = true, permission = true, promptDelay = 1000) { vm, recorder, _ ->
            advanceTimeBy(1100)
            assertFalse(recorder.recording)
            advanceTimeBy(400)
            runCurrent()
            assertTrue(recorder.recording)
            recorder.callbacks.last()(RecognitionResult(""))
            advanceUntilIdle()
            assertFalse(recorder.recording)
            assertEquals(1, recorder.callbacks.size)
            assertEquals(0, vm.uiState.value.attemptCount)
        }

    @Test fun automaticRecordingCannotStartWithoutPermission() = scenario(autoRecord = true) { vm, recorder, _ ->
        advanceUntilIdle()
        assertTrue(recorder.callbacks.isEmpty())
        assertEquals(SessionStepState.ReadyToSpeak, vm.uiState.value.sessionState)
    }

    @Test fun leavingDuringAutomaticRecordingDelayCancelsStart() =
        scenario(autoRecord = true, permission = true) { vm, recorder, _ ->
            vm.stopSession()
            advanceUntilIdle()
            assertTrue(recorder.callbacks.isEmpty())
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
        for (result in listOf(RecognitionResult("", isSuccess = false), RecognitionResult(""),
            RecognitionResult("！…"), RecognitionResult("🫧"))) {
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

    @Test fun failedModelCannotRecordAfterReplayAndExplicitRetryRecovers() = scenario { vm, recorder, player ->
        recorder.loadFails = true
        vm.loadSession(LanguageCode.EN_US, null)
        advanceUntilIdle()
        assertEquals(RepeatProblem.MODEL, vm.uiState.value.recordingProblem)
        assertTrue(vm.uiState.value.canChoose)
        vm.playCurrentPrompt()
        advanceUntilIdle()
        assertEquals(2, player.prompts)
        vm.startRecordingIfReady()
        runCurrent()
        assertTrue(recorder.callbacks.isEmpty())
        recorder.loadFails = false
        vm.retryModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.modelReady)
        vm.startRecordingIfReady()
        runCurrent()
        assertTrue(recorder.recording)
    }

    @Test fun thrownModelFailureOffersRecoveryInsteadOfEscapingViewModel() = scenario { vm, recorder, _ ->
        recorder.loadThrows = true
        vm.retryModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.modelReady)
        assertEquals(RepeatProblem.MODEL, vm.uiState.value.recordingProblem)
        assertEquals(0, vm.uiState.value.attemptCount)
    }

    @Test fun recoveryDoesNotOfferAnEmptyChoiceLesson() = scenario(choiceContent = false) { vm, _, _ ->
        assertFalse(vm.uiState.value.canChoose)
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
