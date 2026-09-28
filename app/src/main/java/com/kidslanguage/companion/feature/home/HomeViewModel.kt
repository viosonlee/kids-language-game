package com.kidslanguage.companion.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidslanguage.companion.core.audio.PaoPaoVoices
import com.kidslanguage.companion.core.audio.PromptPlayer
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.persistence.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val promptPlayer: PromptPlayer? = null
) : ViewModel() {
    private var greetingJob: Job? = null

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.kidslanguage.companion.core.persistence.UserSettings()
    )

    fun selectLanguage(language: LanguageCode) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
    }

    fun playGreeting() {
        greetingJob?.cancel()
        greetingJob = viewModelScope.launch {
            promptPlayer?.playVoiceAsset(PaoPaoVoices.GREETING)
        }
    }

    fun stopGreeting() {
        if (greetingJob?.isActive == true) {
            greetingJob?.cancel()
        }
        greetingJob = null
    }

    override fun onCleared() {
        stopGreeting()
        super.onCleared()
    }
}
