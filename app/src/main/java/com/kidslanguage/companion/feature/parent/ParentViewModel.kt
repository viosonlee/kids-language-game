package com.kidslanguage.companion.feature.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.core.persistence.UserSettings
import com.kidslanguage.companion.core.persistence.ProgressRepository
import com.kidslanguage.companion.core.persistence.ProgressTotals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class MathGate(
    val num1: Int,
    val num2: Int,
    val answer: Int
)

class ParentViewModel(
    private val settingsRepository: SettingsRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {
    val totals = progressRepository.totals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressTotals())
    val today = progressRepository.today.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressTotals())
    val languages = progressRepository.languages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val progressError = progressRepository.error
    private val _resetError = MutableStateFlow<String?>(null)
    val resetError = _resetError.asStateFlow()
    private val _resetting = MutableStateFlow(false)
    val resetting = _resetting.asStateFlow()

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
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
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun setShowText(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowText(show) }
    }

    fun setAutoNext(autoNext: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoNext(autoNext) }
    }

    fun setAutoPlayPrompt(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPlayPrompt(enabled) }
    }

    fun setAutoStartRecording(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoStartRecording(enabled) }
    }

    fun setSessionLength(length: Int) {
        viewModelScope.launch { settingsRepository.setSessionLength(length) }
    }

    fun setSoundVolume(volume: Float) {
        viewModelScope.launch { settingsRepository.setSoundVolume(volume) }
    }

    fun resetProgress() {
        if (_resetting.value) return
        _resetting.value = true
        viewModelScope.launch {
            try {
                progressRepository.clear()
                settingsRepository.resetProgress()
                _resetError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _resetError.value = "清空记录未完成，请重试。"
            } finally {
                _resetting.value = false
            }
        }
    }
}
