package com.kidslanguage.companion.feature.choose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidslanguage.companion.core.audio.PromptPlayer
import com.kidslanguage.companion.core.audio.SoundEffectManager
import com.kidslanguage.companion.core.content.ContentRepository
import com.kidslanguage.companion.core.model.EvaluationStatus
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.core.model.PracticeItem
import com.kidslanguage.companion.core.model.SessionStepState
import com.kidslanguage.companion.core.persistence.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ChooseOption(
    val id: String,
    val title: String,
    val emoji: String,
    val isCorrect: Boolean
)

data class ChooseUiState(
    val items: List<PracticeItem> = emptyList(),
    val currentIndex: Int = 0,
    val currentOptions: List<ChooseOption> = emptyList(),
    val selectedOptionId: String? = null,
    val isAnswerCorrect: Boolean? = null,
    val sessionState: SessionStepState = SessionStepState.Loading,
    val mascotState: MascotState = MascotState.Idle,
    val showText: Boolean = true,
    val isSessionComplete: Boolean = false
) {
    val currentItem: PracticeItem?
        get() = items.getOrNull(currentIndex)
}

class ChooseViewModel(
    private val contentRepository: ContentRepository,
    private val promptPlayer: PromptPlayer,
    private val settingsRepository: SettingsRepository,
    private val soundEffectManager: SoundEffectManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChooseUiState())
    val uiState: StateFlow<ChooseUiState> = _uiState.asStateFlow()

    private var autoAdvanceJob: Job? = null

    fun loadSession(language: LanguageCode, categoryId: String?) {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val allItems = contentRepository.getItems(language, categoryId)
            val sessionItems = allItems.take(settings.sessionLength)

            _uiState.value = ChooseUiState(
                items = sessionItems,
                currentIndex = 0,
                showText = settings.showText,
                sessionState = SessionStepState.PromptPlaying,
                mascotState = MascotState.Listen
            )
            setupCurrentQuestion()
        }
    }

    private fun setupCurrentQuestion() {
        val item = _uiState.value.currentItem ?: return

        // Build 3 options: 1 correct + 2 distractors
        val correctOption = ChooseOption(
            id = "correct",
            title = item.translation ?: item.text,
            emoji = item.image ?: "⭐",
            isCorrect = true
        )

        val distractors = if (item.distractors.isNotEmpty()) {
            item.distractors.take(2).mapIndexed { index, distractorEmoji ->
                ChooseOption(
                    id = "distractor_$index",
                    title = "选项",
                    emoji = distractorEmoji,
                    isCorrect = false
                )
            }
        } else {
            listOf(
                ChooseOption("d1", "选项", "🌟", false),
                ChooseOption("d2", "选项", "🎈", false)
            )
        }

        val options = (listOf(correctOption) + distractors).shuffled()

        _uiState.value = _uiState.value.copy(
            currentOptions = options,
            selectedOptionId = null,
            isAnswerCorrect = null,
            sessionState = SessionStepState.PromptPlaying,
            mascotState = MascotState.Listen
        )

        playPrompt()
    }

    fun playPrompt() {
        val item = _uiState.value.currentItem ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                sessionState = SessionStepState.PromptPlaying,
                mascotState = MascotState.Listen
            )
            promptPlayer.playPrompt(
                text = item.text,
                language = LanguageCode.fromCode(item.language),
                audioAssetPath = item.audio
            )
            _uiState.value = _uiState.value.copy(
                sessionState = SessionStepState.ReadyToSpeak,
                mascotState = MascotState.Idle
            )
        }
    }

    fun onOptionSelected(option: ChooseOption) {
        if (_uiState.value.isAnswerCorrect == true) return // already answered correctly

        _uiState.value = _uiState.value.copy(
            selectedOptionId = option.id,
            isAnswerCorrect = option.isCorrect,
            mascotState = if (option.isCorrect) MascotState.Great else MascotState.TryAgain,
            sessionState = if (option.isCorrect) SessionStepState.Passed else SessionStepState.Retry
        )

        if (option.isCorrect) {
            soundEffectManager?.playSuccess()
            viewModelScope.launch {
                settingsRepository.incrementCompletedCount()
            }
            autoAdvanceJob?.cancel()
            autoAdvanceJob = viewModelScope.launch {
                delay(1200)
                nextQuestion()
            }
        } else {
            soundEffectManager?.playRetry()
        }
    }

    fun nextQuestion() {
        autoAdvanceJob?.cancel()
        val nextIndex = _uiState.value.currentIndex + 1
        if (nextIndex < _uiState.value.items.size) {
            _uiState.value = _uiState.value.copy(currentIndex = nextIndex)
            setupCurrentQuestion()
        } else {
            soundEffectManager?.playCelebrate()
            _uiState.value = _uiState.value.copy(
                isSessionComplete = true,
                sessionState = SessionStepState.Completed,
                mascotState = MascotState.Praise
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoAdvanceJob?.cancel()
        promptPlayer.stop()
    }
}
