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
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.kidslanguage.companion.core.audio.PaoPaoVoices
import kotlin.random.Random

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
    val isSessionComplete: Boolean = false,
    val attemptCount: Int = 0,
    val showAnswerHint: Boolean = false,
    val feedbackPlaying: Boolean = false,
    val autoNext: Boolean = true,
    val contentError: String? = null
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
    private var promptJob: Job? = null
    private var sessionActive = false
    private var sessionKey: Pair<LanguageCode, String?>? = null

    fun loadSession(language: LanguageCode, categoryId: String?) {
        if (sessionActive) return
        sessionActive = true
        val key = language to categoryId
        if (sessionKey == key && _uiState.value.items.isNotEmpty() &&
            _uiState.value.sessionState != SessionStepState.Completed) {
            if (_uiState.value.isAnswerCorrect == true) {
                if (_uiState.value.autoNext) nextQuestion()
            } else playPrompt()
            return
        }
        sessionKey = key
        _uiState.value = ChooseUiState()
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val allItems = contentRepository.getItems(language, categoryId)
            val sessionItems = allItems.filter { "choose" in it.modes }.take(settings.sessionLength.coerceAtLeast(1))

            _uiState.value = ChooseUiState(
                items = sessionItems,
                currentIndex = 0,
                showText = settings.showText,
                autoNext = settings.autoNext,
                contentError = if (sessionItems.isEmpty()) "这个场景还没有可练习的内容，请返回选择其他场景。" else null,
                sessionState = if (sessionItems.isEmpty()) SessionStepState.Completed else SessionStepState.PromptPlaying,
                mascotState = MascotState.Listen
            )
            setupCurrentQuestion()
        }
    }

    private fun setupCurrentQuestion() {
        val item = _uiState.value.currentItem ?: return

        // Build 3 options: 1 correct + 2 distractors
        val correctOption = ChooseOption(
            id = "${item.id}:correct",
            title = item.translation ?: item.text,
            emoji = item.image ?: "⭐",
            isCorrect = true
        )

        val distractors = if (item.distractors.isNotEmpty()) {
            item.distractors.take(2).mapIndexed { index, distractorEmoji ->
                ChooseOption(
                    id = "${item.id}:distractor_$index",
                    title = "选项",
                    emoji = distractorEmoji,
                    isCorrect = false
                )
            }
        } else {
            listOf(
                ChooseOption("${item.id}:d1", "选项", "🌟", false),
                ChooseOption("${item.id}:d2", "选项", "🎈", false)
            )
        }

        val options = (listOf(correctOption) + distractors).shuffled()

        _uiState.value = _uiState.value.copy(
            currentOptions = options,
            selectedOptionId = null,
            isAnswerCorrect = null,
            attemptCount = 0,
            showAnswerHint = false,
            feedbackPlaying = false,
            sessionState = SessionStepState.PromptPlaying,
            mascotState = MascotState.Listen
        )

        playPrompt()
    }

    fun playPrompt() {
        if (!sessionActive || _uiState.value.isAnswerCorrect == true ||
            _uiState.value.isSessionComplete) return
        val item = _uiState.value.currentItem ?: return
        autoAdvanceJob?.cancel()
        promptJob?.cancel()
        promptPlayer.stop()
        promptJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                feedbackPlaying = false,
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
        val state = _uiState.value
        if (!sessionActive || state.isAnswerCorrect == true || state.feedbackPlaying ||
            state.sessionState !in listOf(SessionStepState.ReadyToSpeak, SessionStepState.Retry)) return
        val selected = state.currentOptions.firstOrNull { it.id == option.id } ?: return
        val attempts = state.attemptCount + 1
        val passed = selected.isCorrect || attempts >= 3
        promptJob?.cancel()
        autoAdvanceJob?.cancel()
        promptPlayer.stop()

        _uiState.value = _uiState.value.copy(
            selectedOptionId = if (passed) state.currentOptions.first { it.isCorrect }.id else selected.id,
            isAnswerCorrect = passed,
            attemptCount = attempts,
            showAnswerHint = attempts >= 2,
            feedbackPlaying = true,
            mascotState = if (passed) MascotState.Great else MascotState.TryAgain,
            sessionState = if (passed) SessionStepState.Passed else SessionStepState.Retry
        )

        if (passed) {
            soundEffectManager?.playSuccess()
            autoAdvanceJob = viewModelScope.launch {
                settingsRepository.incrementCompletedCount()
                delay(300)
                val voice = if (!selected.isCorrect) PaoPaoVoices.PRAISE_EFFORT
                    else if (Random.nextBoolean()) PaoPaoVoices.PRAISE_1 else PaoPaoVoices.PRAISE_2
                promptPlayer.playVoiceAsset(voice)
                _uiState.value = _uiState.value.copy(feedbackPlaying = false)
                delay(500)
                if (_uiState.value.autoNext) nextQuestion()
            }
        } else {
            soundEffectManager?.playRetry()
            autoAdvanceJob = viewModelScope.launch {
                delay(300)
                promptPlayer.playVoiceAsset(PaoPaoVoices.TRY_AGAIN)
                playPrompt()
            }
        }
    }

    fun nextQuestion() {
        if (!sessionActive || _uiState.value.isAnswerCorrect != true ||
            _uiState.value.feedbackPlaying || _uiState.value.isSessionComplete) return
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
        stopSession()
    }

    fun consumeSessionCompletion(): Boolean {
        if (!_uiState.value.isSessionComplete) return false
        _uiState.value = _uiState.value.copy(isSessionComplete = false)
        return true
    }

    fun stopSession() {
        if (!sessionActive) return
        sessionActive = false
        viewModelScope.coroutineContext.cancelChildren()
        promptPlayer.stop()
        _uiState.value = _uiState.value.copy(feedbackPlaying = false)
    }
}
