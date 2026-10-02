package com.kidslanguage.companion.feature.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.core.persistence.SettingsSnapshot
import com.kidslanguage.companion.core.persistence.ProgressRepository
import com.kidslanguage.companion.core.persistence.ProgressTotals
import com.kidslanguage.companion.core.persistence.LanguageProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class MathGate(
    val num1: Int,
    val num2: Int,
    val answer: Int
)

sealed interface ProgressResetState {
    data object Idle : ProgressResetState
    data object Running : ProgressResetState
    data object Complete : ProgressResetState
    data class Failed(val message: String) : ProgressResetState
}

sealed interface ProgressReadState {
    data object Loading : ProgressReadState
    data class Ready(val totals: ProgressTotals, val today: ProgressTotals, val languages: List<LanguageProgress>) : ProgressReadState
    data class Failed(val message: String) : ProgressReadState
}

@OptIn(ExperimentalCoroutinesApi::class)
class ParentViewModel(
    private val settingsRepository: SettingsRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {
    private val progressReads = MutableStateFlow(0L)
    val progress = progressReads.flatMapLatest {
        combine<ProgressTotals, ProgressTotals, List<LanguageProgress>, ProgressReadState>(
            progressRepository.totals, progressRepository.today, progressRepository.languages
        ) { totals, today, languages -> ProgressReadState.Ready(totals, today, languages) }
            .onStart { emit(ProgressReadState.Loading) }
            .catch { cause ->
                if (cause is CancellationException || cause !is Exception) throw cause
                emit(ProgressReadState.Failed("练习记录暂时无法读取，请重试。"))
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressReadState.Loading)

    fun retryProgressRead() { progressReads.update { it + 1 } }
    fun retrySettingsRead() = settingsRepository.retryRead()
    val progressError = progressRepository.error
    private val _resetState = MutableStateFlow<ProgressResetState>(ProgressResetState.Idle)
    val resetState = _resetState.asStateFlow()
    private val _settingsError = MutableStateFlow<String?>(null)
    val settingsError = _settingsError.asStateFlow()

    val settings: StateFlow<SettingsSnapshot?> = settingsRepository.settingsState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _gate = MutableStateFlow(generateMathGate())
    val gate: StateFlow<MathGate> = _gate.asStateFlow()

    private fun generateMathGate(): MathGate {
        val a = Random.nextInt(3, 9)
        val b = Random.nextInt(2, 6)
        return MathGate(a, b, a * b)
    }

    fun regenerateGate() {
        _gate.value = generateMathGate()
    }

    fun verifyGate(input: String): Boolean {
        val parsed = input.trim().toIntOrNull()
        return parsed == _gate.value.answer
    }

    fun setLanguage(language: LanguageCode) {
        saveSettings { settingsRepository.setLanguage(language) }
    }

    fun setShowText(show: Boolean) {
        saveSettings { settingsRepository.setShowText(show) }
    }

    fun setTextDisplayMode(mode: com.kidslanguage.companion.core.persistence.TextDisplayMode) {
        saveSettings { settingsRepository.setTextDisplayMode(mode) }
    }

    fun setChoiceCount(count: Int) { saveSettings { settingsRepository.setChoiceCount(count) } }

    fun setShuffleItems(enabled: Boolean) { saveSettings { settingsRepository.setShuffleItems(enabled) } }

    fun setAutoNext(autoNext: Boolean) {
        saveSettings { settingsRepository.setAutoNext(autoNext) }
    }

    fun setAutoPlayPrompt(enabled: Boolean) {
        saveSettings { settingsRepository.setAutoPlayPrompt(enabled) }
    }

    fun setAutoStartRecording(enabled: Boolean) {
        saveSettings { settingsRepository.setAutoStartRecording(enabled) }
    }

    fun setSessionLength(length: Int) {
        saveSettings { settingsRepository.setSessionLength(length) }
    }

    fun setSoundVolume(volume: Float) {
        saveSettings { settingsRepository.setSoundVolume(volume) }
    }

    fun setMusicEnabled(enabled: Boolean) {
        saveSettings { settingsRepository.setMusicEnabled(enabled) }
    }

    fun setAnimationEnabled(enabled: Boolean) {
        saveSettings { settingsRepository.setAnimationEnabled(enabled) }
    }

    private fun saveSettings(save: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                save()
                _settingsError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _settingsError.value = "设置未能保存，请检查设备存储空间后重试。"
            }
        }
    }

    fun resetProgress() {
        if (_resetState.value == ProgressResetState.Running) return
        _resetState.value = ProgressResetState.Running
        viewModelScope.launch {
            try {
                progressRepository.clear()
                _resetState.value = ProgressResetState.Complete
                retryProgressRead()
            } catch (e: CancellationException) {
                _resetState.value = ProgressResetState.Idle
                throw e
            } catch (_: Exception) {
                _resetState.value = ProgressResetState.Failed("清空记录未完成，请重试。")
            }
        }
    }
}
