package com.kidslanguage.companion.feature.repeat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidslanguage.companion.core.asr.RecognitionResult
import com.kidslanguage.companion.core.asr.SpeechRecognizerEngine
import com.kidslanguage.companion.core.audio.PromptPlayer
import com.kidslanguage.companion.core.audio.SoundEffectManager
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.kidslanguage.companion.core.audio.PaoPaoVoices
import kotlin.random.Random

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
    val isSessionComplete: Boolean = false,
    val recordingError: String? = null,
    val autoNext: Boolean = true,
    val feedbackPlaying: Boolean = false
) {
    val currentItem: PracticeItem?
        get() = items.getOrNull(currentIndex)
}

class RepeatViewModel(
    private val contentRepository: ContentRepository,
    private val promptPlayer: PromptPlayer,
    private val speechRecognizer: SpeechRecognizerEngine,
    private val evaluationService: SpeechEvaluationService,
    private val settingsRepository: SettingsRepository,
    private val soundEffectManager: SoundEffectManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepeatUiState())
    val uiState: StateFlow<RepeatUiState> = _uiState.asStateFlow()

    private var autoAdvanceJob: Job? = null
    private var promptJob: Job? = null
    private var recordingGeneration = 0
    private var sessionActive = false
    private var sessionKey: Pair<LanguageCode, String?>? = null

    fun loadSession(language: LanguageCode, categoryId: String?) {
        val previous = _uiState.value
        val key = language to categoryId
        val resume = sessionKey == key && previous.items.isNotEmpty() &&
            previous.sessionState != SessionStepState.Completed
        stopSession()
        sessionActive = true
        sessionKey = key
        _uiState.value = if (resume) previous.copy(
            sessionState = SessionStepState.Loading, isRecording = false,
            showFeedbackOverlay = false, feedbackPlaying = false, recordingError = null
        ) else RepeatUiState()
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            if (!resume) {
                val sessionItems = contentRepository.getItems(language, categoryId)
                    .filter { "repeat" in it.modes }.take(settings.sessionLength.coerceIn(1, 30))
                _uiState.value = RepeatUiState(items = sessionItems)
                if (sessionItems.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        sessionState = SessionStepState.Completed,
                        recordingError = "这个场景还没有可练习的内容，请返回选择其他场景。"
                    )
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(
                showText = settings.showText,
                autoNext = settings.autoNext,
                sessionState = SessionStepState.Loading,
                mascotState = MascotState.Listen
            )

            val loaded = speechRecognizer.load(language)
            if (loaded.isFailure) {
                _uiState.value = _uiState.value.copy(
                    sessionState = SessionStepState.ReadyToSpeak,
                    recordingError = "语音模型暂时不可用，请稍后重试，或先玩选图模式。"
                )
                return@launch
            }
            if (resume && previous.sessionState == SessionStepState.Passed) {
                _uiState.value = _uiState.value.copy(
                    sessionState = SessionStepState.Passed, showFeedbackOverlay = true,
                    mascotState = MascotState.Great
                )
                if (settings.autoNext) nextItem()
                return@launch
            }
            _uiState.value = _uiState.value.copy(sessionState = SessionStepState.PromptPlaying)
            playCurrentPrompt()
        }
    }

    fun playCurrentPrompt() {
        if (!sessionActive || _uiState.value.isSessionComplete ||
            _uiState.value.sessionState == SessionStepState.Loading ||
            _uiState.value.sessionState == SessionStepState.Passed) return
        val item = _uiState.value.currentItem ?: return
        recordingGeneration++
        speechRecognizer.cancelListening()
        autoAdvanceJob?.cancel()
        promptJob?.cancel()
        promptPlayer.stop()
        promptJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                feedbackPlaying = false,
                recordingError = null,
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
        if (!sessionActive || _uiState.value.feedbackPlaying || _uiState.value.sessionState !in listOf(
                SessionStepState.ReadyToSpeak, SessionStepState.Retry)) return
        val item = _uiState.value.currentItem ?: return
        val language = LanguageCode.fromCode(item.language)
        autoAdvanceJob?.cancel()
        promptJob?.cancel()
        promptPlayer.stop()
        val request = ++recordingGeneration

        _uiState.value = _uiState.value.copy(
            isRecording = true,
            recordingError = null,
            showFeedbackOverlay = false,
            sessionState = SessionStepState.Recording,
            mascotState = MascotState.Speak
        )

        viewModelScope.launch {
            try {
                speechRecognizer.startListening(language) { result ->
                    if (request == recordingGeneration) processRecognitionResult(result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (request == recordingGeneration) {
                    processRecognitionResult(RecognitionResult("", isSuccess = false, error = e.message))
                }
            }
        }
    }

    private fun stopRecording() {
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            sessionState = SessionStepState.Evaluating
        )
        speechRecognizer.stopListening()
    }

    private fun processRecognitionResult(result: RecognitionResult) {
        if (!sessionActive || _uiState.value.sessionState !in listOf(
                SessionStepState.Recording, SessionStepState.Evaluating)) return
        recordingGeneration++
        speechRecognizer.cancelListening()
        if (!result.isSuccess || result.text.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                sessionState = SessionStepState.ReadyToSpeak,
                mascotState = MascotState.Speak,
                showFeedbackOverlay = false,
                recordingError = if (result.isSuccess) "没有听到声音，请再试一次。"
                    else "录音或语音识别暂时不可用，请检查麦克风权限和语音服务，或先玩选图模式。"
            )
            return
        }
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
            feedbackPlaying = true,
            evaluationResult = eval,
            showFeedbackOverlay = true,
            mascotState = nextMascot,
            sessionState = if (isPassed) SessionStepState.Passed else SessionStepState.Retry
        )

        if (isPassed) {
            soundEffectManager?.playSuccess()
            autoAdvanceJob?.cancel()
            autoAdvanceJob = viewModelScope.launch {
                settingsRepository.incrementCompletedCount()
                delay(300)
                if (eval.status == EvaluationStatus.SoftPass && currentAttempts >= 3) {
                    promptPlayer.playVoiceAsset(PaoPaoVoices.PRAISE_EFFORT)
                } else {
                    val voice = if (Random.nextBoolean()) PaoPaoVoices.PRAISE_1 else PaoPaoVoices.PRAISE_2
                    promptPlayer.playVoiceAsset(voice)
                }
                _uiState.value = _uiState.value.copy(feedbackPlaying = false)
                delay(500)
                if (_uiState.value.autoNext) nextItem()
            }
        } else {
            soundEffectManager?.playRetry()
            autoAdvanceJob?.cancel()
            autoAdvanceJob = viewModelScope.launch {
                delay(300)
                promptPlayer.playVoiceAsset(PaoPaoVoices.TRY_AGAIN)
                _uiState.value = _uiState.value.copy(feedbackPlaying = false)
                playCurrentPrompt()
            }
        }
    }

    fun nextItem() {
        if (!sessionActive || _uiState.value.isSessionComplete ||
            _uiState.value.sessionState != SessionStepState.Passed || _uiState.value.feedbackPlaying) return
        autoAdvanceJob?.cancel()
        recordingGeneration++
        speechRecognizer.cancelListening()
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
            soundEffectManager?.playCelebrate()
            _uiState.value = _uiState.value.copy(
                isSessionComplete = true,
                sessionState = SessionStepState.Completed,
                mascotState = MascotState.Praise
            )
        }
    }

    fun dismissFeedback() {
        if (_uiState.value.sessionState == SessionStepState.Passed && !_uiState.value.autoNext) return
        _uiState.value = _uiState.value.copy(showFeedbackOverlay = false)
    }

    fun consumeSessionCompletion(): Boolean {
        if (!_uiState.value.isSessionComplete) return false
        _uiState.value = _uiState.value.copy(isSessionComplete = false)
        return true
    }

    fun dismissRecordingError() {
        _uiState.value = _uiState.value.copy(recordingError = null)
    }

    fun stopSession() {
        if (!sessionActive) return
        sessionActive = false
        recordingGeneration++
        viewModelScope.coroutineContext.cancelChildren()
        speechRecognizer.cancelListening()
        promptPlayer.stop()
        _uiState.value = _uiState.value.copy(isRecording = false, feedbackPlaying = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}
