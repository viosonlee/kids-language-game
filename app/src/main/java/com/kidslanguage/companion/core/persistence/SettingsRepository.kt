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
    val autoPlayPrompt: Boolean = true,
    val autoStartRecording: Boolean = false,
    val sessionLength: Int = 10,
    val soundVolume: Float = 1.0f,
    val musicEnabled: Boolean = false,
    val animationEnabled: Boolean = true,
    val completedCount: Int = 0
)

class SettingsRepository(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.dataStore)

    private val KEY_LANGUAGE = stringPreferencesKey("selected_language")
    private val KEY_SHOW_TEXT = booleanPreferencesKey("show_text")
    private val KEY_AUTO_NEXT = booleanPreferencesKey("auto_next")
    private val KEY_AUTO_PLAY = booleanPreferencesKey("auto_play_prompt")
    private val KEY_AUTO_RECORD = booleanPreferencesKey("auto_start_recording")
    private val KEY_SESSION_LENGTH = intPreferencesKey("session_length")
    private val KEY_SOUND_VOLUME = floatPreferencesKey("sound_volume")
    private val KEY_MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
    private val KEY_ANIMATION_ENABLED = booleanPreferencesKey("animation_enabled")
    private val KEY_COMPLETED_COUNT = intPreferencesKey("completed_count")

    val settingsFlow: Flow<UserSettings> = store.data.map { preferences ->
        val langStr = preferences[KEY_LANGUAGE] ?: LanguageCode.JA_JP.code
        UserSettings(
            selectedLanguage = LanguageCode.fromCode(langStr),
            showText = preferences[KEY_SHOW_TEXT] ?: true,
            autoNext = preferences[KEY_AUTO_NEXT] ?: true,
            autoPlayPrompt = preferences[KEY_AUTO_PLAY] ?: true,
            autoStartRecording = preferences[KEY_AUTO_RECORD] ?: false,
            sessionLength = (preferences[KEY_SESSION_LENGTH] ?: 10).coerceIn(1, 30),
            soundVolume = (preferences[KEY_SOUND_VOLUME] ?: 1.0f).let { if (it.isFinite()) it.coerceIn(0f, 1f) else 1f },
            animationEnabled = preferences[KEY_ANIMATION_ENABLED] ?: true,
            musicEnabled = preferences[KEY_MUSIC_ENABLED] ?: false,
            completedCount = preferences[KEY_COMPLETED_COUNT] ?: 0
        )
    }

    suspend fun setLanguage(language: LanguageCode) {
        store.edit { preferences ->
            preferences[KEY_LANGUAGE] = language.code
        }
    }

    suspend fun setShowText(show: Boolean) {
        store.edit { preferences ->
            preferences[KEY_SHOW_TEXT] = show
        }
    }

    suspend fun setAutoNext(autoNext: Boolean) {
        store.edit { preferences ->
            preferences[KEY_AUTO_NEXT] = autoNext
        }
    }

    suspend fun setAutoPlayPrompt(enabled: Boolean) {
        store.edit { it[KEY_AUTO_PLAY] = enabled }
    }

    suspend fun setAutoStartRecording(enabled: Boolean) {
        store.edit { it[KEY_AUTO_RECORD] = enabled }
    }

    suspend fun setSessionLength(length: Int) {
        store.edit { preferences ->
            preferences[KEY_SESSION_LENGTH] = length.coerceIn(1, 30)
        }
    }

    suspend fun setAnimationEnabled(enabled: Boolean) {
        store.edit { preferences ->
            preferences[KEY_ANIMATION_ENABLED] = enabled
        }
    }

    suspend fun setSoundVolume(volume: Float) {
        require(volume.isFinite())
        store.edit { it[KEY_SOUND_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        store.edit { it[KEY_MUSIC_ENABLED] = enabled }
    }

    suspend fun incrementCompletedCount() {
        store.edit { preferences ->
            val current = preferences[KEY_COMPLETED_COUNT] ?: 0
            preferences[KEY_COMPLETED_COUNT] = current + 1
        }
    }

    suspend fun resetProgress() {
        store.edit { preferences ->
            preferences[KEY_COMPLETED_COUNT] = 0
        }
    }
}
