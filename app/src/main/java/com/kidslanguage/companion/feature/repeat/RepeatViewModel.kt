package com.kidslanguage.companion.feature.repeat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidslanguage.companion.core.asr.RecognitionResult
import com.kidslanguage.companion.core.asr.SpeechRecognizerEngine
import com.kidslanguage.companion.core.audio.AudioPlaybackInterruptedException
import com.kidslanguage.companion.core.audio.PromptPlayer
import com.kidslanguage.companion.core.audio.SoundEffectManager
import com.kidslanguage.companion.core.content.ContentRepository
import com.kidslanguage.companion.core.content.planLesson
import com.kidslanguage.companion.core.model.PracticeMode
import com.kidslanguage.companion.core.model.EvaluationResult
import com.kidslanguage.companion.core.model.EvaluationStatus
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.core.model.PracticeItem
import com.kidslanguage.companion.core.model.SessionStepState
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.core.persistence.ProgressRepository
import com.kidslanguage.companion.core.persistence.repeatAttempt
import java.util.UUID
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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import com.kidslanguage.companion.core.audio.PaoPaoVoices
import com.kidslanguage.companion.core.audio.playSuccessFeedback
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
    val smallText: Boolean = false,
    val isSessionComplete: Boolean = false,
    val recordingProblem: RepeatProblem? = null,
    val modelReady: Boolean = false,
    val canChoose: Boolean = false,
    val autoNext: Boolean = true,
    val feedbackPlaying: Boolean = false
) {
    val recordingError: String? get() = recordingProblem?.message
    val currentItem: PracticeItem?
        get() = items.getOrNull(currentIndex)
}

class RepeatViewModel(
    private val contentRepository: ContentRepository,
    private val promptPlayer: PromptPlayer,
    private val speechRecognizer: SpeechRecognizerEngine,
    private val evaluationService: SpeechEvaluationService,
    private val settingsRepository: SettingsRepository,
    private val soundEffectManager: SoundEffectManager? = null,
    private val progressRepository: ProgressRepository? = null,
    private val canRecord: () -> Boolean = { false }
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepeatUiState())
    val uiState: StateFlow<RepeatUiState> = _uiState.asStateFlow()

    private var autoAdvanceJob: Job? = null
    private var promptJob: Job? = null
    private var recordingGeneration = 0
    private var sessionActive = false
    private var sessionKey: Pair<LanguageCode, String?>? = null
    private var sessionId = UUID.randomUUID().toString()

    fun loadSession(language: LanguageCode, categoryId: String?) {
        val previous = _uiState.value
        val key = language to categoryId
        val resume = sessionKey == key && previous.items.isNotEmpty() &&
            previous.sessionState != SessionStepState.Completed
        stopSession()
        sessionActive = true
        sessionKey = key
        if (!resume) sessionId = UUID.randomUUID().toString()
        _uiState.value = if (resume) previous.copy(
            sessionState = SessionStepState.Loading, isRecording = false,
            showFeedbackOverlay = false, feedbackPlaying = false, recordingProblem = null, modelReady = false
        ) else RepeatUiState()
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            if (!resume) {
                val items = contentRepository.getItems(language, categoryId)
                val lesson = contentRepository.getLesson(language, categoryId)
                val sessionItems = planLesson(items, lesson, PracticeMode.REPEAT, settings).items
                _uiState.value = RepeatUiState(items = sessionItems,
                    canChoose = planLesson(items, lesson, PracticeMode.CHOOSE, settings).items.isNotEmpty())
                if (sessionItems.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        sessionState = SessionStepState.Completed,
                        recordingProblem = RepeatProblem.CONTENT
                    )
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(
                showText = settings.showText,
                smallText = settings.smallText,
                autoNext = settings.autoNext,
                sessionState = SessionStepState.Loading,
                mascotState = MascotState.Listen
            )

            val loaded = try { speechRecognizer.load(language) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            if (loaded.isFailure) {
                _uiState.value = _uiState.value.copy(
                    sessionState = SessionStepState.ReadyToSpeak,
                    recordingProblem = RepeatProblem.MODEL
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(modelReady = true)
            if (resume && previous.sessionState == SessionStepState.Passed) {
                _uiState.value = _uiState.value.copy(
                    sessionState = SessionStepState.Passed, showFeedbackOverlay = true,
                    mascotState = MascotState.Great,
                    autoNext = previous.autoNext && settings.autoNext
                )
                if (_uiState.value.autoNext) nextItem()
                return@launch
            }
            _uiState.value = _uiState.value.copy(sessionState = SessionStepState.PromptPlaying)
            playPrompt(automatic = true)
        }
    }

    fun playCurrentPrompt() = playPrompt(automatic = false)

    private fun playPrompt(automatic: Boolean) {
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
            val settings = settingsRepository.settingsFlow.first()
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                feedbackPlaying = false,
                recordingProblem = null,
                sessionState = SessionStepState.PromptPlaying,
                mascotState = MascotState.Listen,
                showFeedbackOverlay = false,
                showText = settings.showText,
                smallText = settings.smallText,
                autoNext = settings.autoNext
            )
            if (!automatic || settings.autoPlayPrompt) {
                try {
                    promptPlayer.playPrompt(
                        text = item.text,
                        language = LanguageCode.fromCode(item.language),
                        audioAssetPath = item.audio
                    )
                } catch (_: AudioPlaybackInterruptedException) {
                    currentCoroutineContext().ensureActive()
                    _uiState.value = _uiState.value.copy(
                        sessionState = SessionStepState.ReadyToSpeak,
                        mascotState = MascotState.Speak
                    )
                    // The child has not heard a full prompt: wait for a deliberate action.
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(
                sessionState = SessionStepState.ReadyToSpeak,
                mascotState = MascotState.Speak
            )
            if (settings.autoStartRecording && _uiState.value.modelReady && canRecord()) {
                delay(400)
                if (sessionActive && canRecord() && _uiState.value.sessionState == SessionStepState.ReadyToSpeak) {
                    promptJob = null
                    startRecording()
                }
            }
        }
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun startRecordingIfReady() = startRecording()

    private fun startRecording() {
        if (!sessionActive || _uiState.value.feedbackPlaying || _uiState.value.sessionState !in listOf(
                SessionStepState.ReadyToSpeak, SessionStepState.Retry)) return
        val item = _uiState.value.currentItem ?: return
        if (!_uiState.value.modelReady) {
            _uiState.value = _uiState.value.copy(recordingProblem = RepeatProblem.MODEL)
            return
        }
        val language = LanguageCode.fromCode(item.language)
        autoAdvanceJob?.cancel()
        promptJob?.cancel()
        promptPlayer.stop()
        val request = ++recordingGeneration
        soundEffectManager?.stop()

        _uiState.value = _uiState.value.copy(
            isRecording = true,
            recordingProblem = null,
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
                recordingProblem = if (result.isSuccess) RepeatProblem.NO_SPEECH else RepeatProblem.RECORDING
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

        if (eval.status == EvaluationStatus.NoSpeech || eval.status == EvaluationStatus.Error) {
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                sessionState = SessionStepState.ReadyToSpeak,
                mascotState = MascotState.Speak,
                showFeedbackOverlay = false,
                recordingProblem = if (eval.status == EvaluationStatus.NoSpeech) RepeatProblem.NO_SPEECH else RepeatProblem.CONTENT
            )
            return
        }

        val isPassed = eval.status == EvaluationStatus.Pass || eval.status == EvaluationStatus.SoftPass
        repeatAttempt(item, "$sessionId:${_uiState.value.currentIndex}", currentAttempts, eval.status)
            ?.let { progressRepository?.record(it) }
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
            autoAdvanceJob?.cancel()
            autoAdvanceJob = viewModelScope.launch {
                val voice = if (eval.status == EvaluationStatus.SoftPass && currentAttempts >= 3)
                    PaoPaoVoices.PRAISE_EFFORT
                else if (Random.nextBoolean()) PaoPaoVoices.PRAISE_1 else PaoPaoVoices.PRAISE_2
                try {
                    promptPlayer.playSuccessFeedback(voice, soundEffectManager)
                } catch (_: AudioPlaybackInterruptedException) {
                    currentCoroutineContext().ensureActive()
                    // Keep the earned result; expose manual next instead of advancing silently.
                    _uiState.value = _uiState.value.copy(feedbackPlaying = false, autoNext = false)
                    return@launch
                }
                _uiState.value = _uiState.value.copy(feedbackPlaying = false)
                if (_uiState.value.autoNext) nextItem()
            }
        } else {
            soundEffectManager?.playRetry()
            autoAdvanceJob?.cancel()
            autoAdvanceJob = viewModelScope.launch {
                delay(300)
                try {
                    promptPlayer.playVoiceAsset(PaoPaoVoices.TRY_AGAIN)
                } catch (_: AudioPlaybackInterruptedException) {
                    currentCoroutineContext().ensureActive()
                    _uiState.value = _uiState.value.copy(feedbackPlaying = false)
                    return@launch
                }
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
            playPrompt(automatic = true)
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
        _uiState.value = _uiState.value.copy(recordingProblem = null)
    }

    fun retryModel() {
        val (language, categoryId) = sessionKey ?: return
        loadSession(language, categoryId)
    }

    fun stopSession() {
        if (!sessionActive) return
        sessionActive = false
        soundEffectManager?.stop()
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
