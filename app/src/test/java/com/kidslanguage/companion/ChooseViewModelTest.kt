package com.kidslanguage.companion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.kidslanguage.companion.core.audio.PromptPlayer
import com.kidslanguage.companion.core.content.ContentRepository
import com.kidslanguage.companion.core.model.*
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.feature.choose.ChooseViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChooseViewModelTest {
    private class Store : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
    }

    private class Player : PromptPlayer {
        var voices = 0
        var prompts = 0
        var playing = false
        override suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String?) {
            prompts++
            playing = true
            delay(100)
            playing = false
        }
        override suspend fun playVoiceAsset(audioAssetPath: String) {
            voices++
            playing = true
            delay(3000)
            playing = false
        }
        override fun stop() { playing = false }
        override fun release() = stop()
    }

    private fun scenario(autoNext: Boolean = true, autoPlay: Boolean = true, block: suspend TestScope.(ChooseViewModel, Player) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val settings = SettingsRepository(Store())
        settings.setAutoNext(autoNext)
        settings.setAutoPlayPrompt(autoPlay)
        val player = Player()
        val content = object : ContentRepository {
            override suspend fun getCategories(language: LanguageCode) = emptyList<Category>()
            override suspend fun getItems(language: LanguageCode, categoryId: String?) = listOf(
                PracticeItem("1", "en-US", "greetings", text = "hello", image = "👋"),
                PracticeItem("2", "en-US", "greetings", text = "goodbye", image = "🏠")
            )
            override suspend fun getItemById(id: String): PracticeItem? = null
        }
        val vm = ChooseViewModel(content, player, settings)
        try {
            vm.loadSession(LanguageCode.EN_US, null)
            advanceUntilIdle()
            block(vm, player)
        } finally {
            vm.stopSession()
            Dispatchers.resetMain()
        }
    }

    @Test fun disabledAutoplayAppliesToEachQuestionButManualReplayWorks() = scenario(autoPlay = false) { vm, player ->
        assertEquals(0, player.prompts)
        vm.playPrompt()
        advanceUntilIdle()
        assertEquals(1, player.prompts)
        vm.onOptionSelected(vm.uiState.value.currentOptions.first { it.isCorrect })
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.currentIndex)
        assertEquals(1, player.prompts)
        assertEquals(SessionStepState.ReadyToSpeak, vm.uiState.value.sessionState)
    }

    @Test fun duplicateTapScoresOnceAndNextPromptWaitsForPraise() = scenario { vm, player ->
        val correct = vm.uiState.value.currentOptions.first { it.isCorrect }
        repeat(5) { vm.onOptionSelected(correct) }
        advanceTimeBy(2300)
        assertEquals(0, vm.uiState.value.currentIndex)
        assertTrue(player.playing)
        assertEquals(1, player.prompts)
        advanceUntilIdle()
        assertEquals(1, player.voices)
        assertEquals(1, vm.uiState.value.currentIndex)
        vm.onOptionSelected(correct)
        assertEquals(0, vm.uiState.value.attemptCount)
    }

    @Test fun leavingCancelsPendingAudioAndNavigation() = scenario { vm, player ->
        vm.onOptionSelected(vm.uiState.value.currentOptions.first { it.isCorrect })
        vm.stopSession()
        advanceUntilIdle()
        assertEquals(0, player.voices)
        assertFalse(player.playing)
        assertEquals(0, vm.uiState.value.currentIndex)
    }

    @Test fun leavingDuringPraiseStopsPlayback() = scenario { vm, player ->
        vm.onOptionSelected(vm.uiState.value.currentOptions.first { it.isCorrect })
        advanceTimeBy(400)
        assertTrue(player.playing)
        vm.stopSession()
        advanceUntilIdle()
        assertFalse(player.playing)
        assertEquals(0, vm.uiState.value.currentIndex)
    }

    @Test fun manualNextPreferenceIsHonored() = scenario(autoNext = false) { vm, _ ->
        vm.onOptionSelected(vm.uiState.value.currentOptions.first { it.isCorrect })
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.currentIndex)
        assertEquals(true, vm.uiState.value.isAnswerCorrect)
        assertFalse(vm.uiState.value.feedbackPlaying)
        vm.nextQuestion()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.currentIndex)
    }

    @Test fun repeatedMistakesReplayThenHintThenAssist() = scenario(autoNext = false) { vm, player ->
        val wrong = vm.uiState.value.currentOptions.first { !it.isCorrect }
        vm.onOptionSelected(wrong)
        repeat(4) { vm.onOptionSelected(wrong) }
        assertEquals(1, vm.uiState.value.attemptCount)
        advanceUntilIdle()
        assertEquals(2, player.prompts)
        vm.onOptionSelected(wrong)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showAnswerHint)
        vm.onOptionSelected(wrong)
        advanceUntilIdle()
        assertEquals(true, vm.uiState.value.isAnswerCorrect)
        assertEquals(vm.uiState.value.currentOptions.first { it.isCorrect }.id, vm.uiState.value.selectedOptionId)
    }

    @Test fun backgroundResumeKeepsCurrentQuestion() = scenario { vm, _ ->
        vm.onOptionSelected(vm.uiState.value.currentOptions.first { it.isCorrect })
        advanceUntilIdle()
        vm.stopSession()
        vm.loadSession(LanguageCode.EN_US, null)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.currentIndex)
        assertEquals(SessionStepState.ReadyToSpeak, vm.uiState.value.sessionState)
    }

    @Test fun completionIsConsumedOnceAndReplayStartsFresh() = scenario { vm, _ ->
        repeat(2) {
            vm.onOptionSelected(vm.uiState.value.currentOptions.first { it.isCorrect })
            advanceUntilIdle()
        }
        assertTrue(vm.consumeSessionCompletion())
        assertFalse(vm.consumeSessionCompletion())
        vm.stopSession()
        vm.loadSession(LanguageCode.EN_US, null)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.currentIndex)
        assertFalse(vm.uiState.value.isSessionComplete)
        assertEquals(SessionStepState.ReadyToSpeak, vm.uiState.value.sessionState)
    }
}
