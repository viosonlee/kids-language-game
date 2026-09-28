package com.kidslanguage.companion.feature.repeat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidslanguage.companion.core.asr.RecognitionResult
import com.kidslanguage.companion.core.asr.SpeechRecognizerEngine
import com.kidslanguage.companion.core.audio.PromptPlayer
import com.kidslanguage.companion.core.content.ContentRepository
import com.kidslanguage.companion.core.model.EvaluationResult
import com.kidslanguage.companion.core.model.EvaluationStatus
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.core.model.PracticeItem
import com.kidslanguage.companion.core.model.SessionStepState
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.core.scoring.SpeechEvaluationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RepeatUiState(
    val items: List<PracticeItem> = emptyList(),
    val currentIndex: Int = 0,
    val sessionState: SessionStepState = SessionStepState.Loading,
    val mascotState: MascotState = MascotState.Idle,
    val attemptCount: Int = 0,
    val evaluationResult: EvaluationResult? = null,
    val showFeedbackOverlay: Boolean = false,
    val isRecording: Boolean = false,
    val showText: Boolean = true,
    val isSessionComplete: Boolean = false
) {
    val currentItem: PracticeItem?
        get() = items.getOrNull(currentIndex)
}

class RepeatViewModel(
    private val contentRepository: ContentRepository,
    private val promptPlayer: PromptPlayer,
    private val speechRecognizer: SpeechRecognizerEngine,
    private val evaluationService: SpeechEvaluationService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepeatUiState())
    val uiState: StateFlow<RepeatUiState> = _uiState.asStateFlow()

    private var autoAdvanceJob: Job? = null

    fun loadSession(language: LanguageCode, categoryId: String?) {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val allItems = contentRepository.getItems(language, categoryId)
            val sessionItems = allItems.take(settings.sessionLength)

            _uiState.value = RepeatUiState(
                items = sessionItems,
                currentIndex = 0,
                showText = settings.showText,
                sessionState = SessionStepState.PromptPlaying,
                mascotState = MascotState.Listen
            )

            speechRecognizer.load(language)
            playCurrentPrompt()
        }
    }

    fun playCurrentPrompt() {
        val item = _uiState.value.currentItem ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                sessionState = SessionStepState.PromptPlaying,
                mascotState = MascotState.Listen,
                showFeedbackOverlay = false
            )
            promptPlayer.playPrompt(
                text = item.text,
                language = LanguageCode.fromCode(item.language),
                audioAssetPath = item.audio
            )
            _uiState.value = _uiState.value.copy(
                sessionState = SessionStepState.ReadyToSpeak,
                mascotState = MascotState.Speak
            )
        }
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val item = _uiState.value.currentItem ?: return
        val language = LanguageCode.fromCode(item.language)

        _uiState.value = _uiState.value.copy(
            isRecording = true,
            sessionState = SessionStepState.Recording,
            mascotState = MascotState.Speak
        )

        viewModelScope.launch {
            speechRecognizer.startListening(language) { result ->
                processRecognitionResult(result)
            }
        }
    }

    private fun stopRecording() {
        speechRecognizer.stopListening()
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            sessionState = SessionStepState.Evaluating
        )
    }

    fun processRecognitionResult(result: RecognitionResult) {
        val item = _uiState.value.currentItem ?: return
        val currentAttempts = _uiState.value.attemptCount + 1

        val eval = evaluationService.evaluate(
            spokenText = result.text,
            targetItem = item,
            attemptCount = currentAttempts
        )

        val isPassed = eval.status == EvaluationStatus.Pass || eval.status == EvaluationStatus.SoftPass
        val nextMascot = if (isPassed) MascotState.Great else MascotState.TryAgain

        _uiState.value = _uiState.value.copy(
            isRecording = false,
            attemptCount = currentAttempts,
            evaluationResult = eval,
            showFeedbackOverlay = true,
            mascotState = nextMascot,
            sessionState = if (isPassed) SessionStepState.Passed else SessionStepState.Retry
        )

        if (isPassed) {
            viewModelScope.launch {
                settingsRepository.incrementCompletedCount()
            }
            autoAdvanceJob?.cancel()
            autoAdvanceJob = viewModelScope.launch {
                delay(1400)
                nextItem()
            }
        }
    }

    fun nextItem() {
        autoAdvanceJob?.cancel()
        val nextIndex = _uiState.value.currentIndex + 1
        if (nextIndex < _uiState.value.items.size) {
            _uiState.value = _uiState.value.copy(
                currentIndex = nextIndex,
                attemptCount = 0,
                evaluationResult = null,
                showFeedbackOverlay = false,
                sessionState = SessionStepState.PromptPlaying,
                mascotState = MascotState.Listen
            )
            playCurrentPrompt()
        } else {
            _uiState.value = _uiState.value.copy(
                isSessionComplete = true,
                sessionState = SessionStepState.Completed,
                mascotState = MascotState.Praise
            )
        }
    }

    fun dismissFeedback() {
        _uiState.value = _uiState.value.copy(showFeedbackOverlay = false)
    }

    override fun onCleared() {
        super.onCleared()
        autoAdvanceJob?.cancel()
        promptPlayer.stop()
        speechRecognizer.stopListening()
    }
}
