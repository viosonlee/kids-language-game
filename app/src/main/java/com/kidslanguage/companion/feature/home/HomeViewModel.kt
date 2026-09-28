package com.kidslanguage.companion.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.persistence.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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
}
