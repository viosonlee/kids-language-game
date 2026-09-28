package com.kidslanguage.companion.core.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val selectedLanguage: LanguageCode = LanguageCode.JA_JP,
    val showText: Boolean = true,
    val autoNext: Boolean = true,
    val sessionLength: Int = 10,
    val soundVolume: Float = 1.0f,
    val animationEnabled: Boolean = true,
    val completedCount: Int = 0
)

class SettingsRepository(private val context: Context) {

    private val KEY_LANGUAGE = stringPreferencesKey("selected_language")
    private val KEY_SHOW_TEXT = booleanPreferencesKey("show_text")
    private val KEY_AUTO_NEXT = booleanPreferencesKey("auto_next")
    private val KEY_SESSION_LENGTH = intPreferencesKey("session_length")
    private val KEY_SOUND_VOLUME = floatPreferencesKey("sound_volume")
    private val KEY_ANIMATION_ENABLED = booleanPreferencesKey("animation_enabled")
    private val KEY_COMPLETED_COUNT = intPreferencesKey("completed_count")

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val langStr = preferences[KEY_LANGUAGE] ?: LanguageCode.JA_JP.code
        UserSettings(
            selectedLanguage = LanguageCode.fromCode(langStr),
            showText = preferences[KEY_SHOW_TEXT] ?: true,
            autoNext = preferences[KEY_AUTO_NEXT] ?: true,
            sessionLength = preferences[KEY_SESSION_LENGTH] ?: 10,
            soundVolume = preferences[KEY_SOUND_VOLUME] ?: 1.0f,
            animationEnabled = preferences[KEY_ANIMATION_ENABLED] ?: true,
            completedCount = preferences[KEY_COMPLETED_COUNT] ?: 0
        )
    }

    suspend fun setLanguage(language: LanguageCode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language.code
        }
    }

    suspend fun setShowText(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_TEXT] = show
        }
    }

    suspend fun setAutoNext(autoNext: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_NEXT] = autoNext
        }
    }

    suspend fun setSessionLength(length: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SESSION_LENGTH] = length
        }
    }

    suspend fun setAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ANIMATION_ENABLED] = enabled
        }
    }

    suspend fun incrementCompletedCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_COMPLETED_COUNT] ?: 0
            preferences[KEY_COMPLETED_COUNT] = current + 1
        }
    }

    suspend fun resetProgress() {
        context.dataStore.edit { preferences ->
            preferences[KEY_COMPLETED_COUNT] = 0
        }
    }
}
