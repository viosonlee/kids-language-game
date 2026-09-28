package com.kidslanguage.companion.feature.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.persistence.SettingsRepository
import com.kidslanguage.companion.core.persistence.UserSettings
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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

    fun setSessionLength(length: Int) {
        viewModelScope.launch { settingsRepository.setSessionLength(length) }
    }

    fun resetProgress() {
        viewModelScope.launch { settingsRepository.resetProgress() }
    }
}
