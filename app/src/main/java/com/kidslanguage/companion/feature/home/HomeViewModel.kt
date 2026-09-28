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
import com.kidslanguage.companion.core.audio.BackgroundMusicPlayer

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val promptPlayer: PromptPlayer? = null,
    private val music: BackgroundMusicPlayer? = null
) : ViewModel() {
    private var greetingJob: Job? = null
    private var musicJob: Job? = null
    private var active = false
    private var musicEnabled = false
    private var greeting = false
    private var greetingGeneration = 0

    fun resumeHome() {
        if (active) return
        active = true
        musicJob = viewModelScope.launch {
            settingsRepository.settingsFlow.collect {
                musicEnabled = it.musicEnabled
                updateMusic()
            }
        }
    }

    private fun updateMusic() {
        if (active && musicEnabled && !greeting) music?.play() else music?.stop()
    }

    fun pauseHome() {
        active = false
        musicJob?.cancel()
        stopGreeting()
        updateMusic()
    }

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
        if (!active) return
        val request = ++greetingGeneration
        greetingJob?.cancel()
        greeting = true
        updateMusic()
        greetingJob = viewModelScope.launch {
            try {
                promptPlayer?.playVoiceAsset(PaoPaoVoices.GREETING)
            } finally {
                if (request == greetingGeneration) {
                    greeting = false
                    updateMusic()
                }
            }
        }
    }

    fun stopGreeting() {
        greetingGeneration++
        greeting = false
        if (greetingJob?.isActive == true) {
            greetingJob?.cancel()
        }
        greetingJob = null
    }

    override fun onCleared() {
        pauseHome()
        super.onCleared()
    }
}
