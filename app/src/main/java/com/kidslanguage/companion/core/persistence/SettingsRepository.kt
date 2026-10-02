package com.kidslanguage.companion.core.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

enum class TextDisplayMode(val title: String) { NORMAL("标准文字"), SMALL("辅助小字"), HIDDEN("隐藏文字") }

data class UserSettings(
    val selectedLanguage: LanguageCode = LanguageCode.JA_JP,
    val showText: Boolean = true,
    val smallText: Boolean = false,
    val autoNext: Boolean = true,
    val autoPlayPrompt: Boolean = true,
    val autoStartRecording: Boolean = false,
    val sessionLength: Int = 10,
    val choiceCount: Int = 3,
    val shuffleItems: Boolean = false,
    val soundVolume: Float = 1.0f,
    val musicEnabled: Boolean = false,
    val animationEnabled: Boolean = true
) {
    val textDisplayMode: TextDisplayMode get() = when {
        !showText -> TextDisplayMode.HIDDEN
        smallText -> TextDisplayMode.SMALL
        else -> TextDisplayMode.NORMAL
    }
}

data class SettingsSnapshot(val value: UserSettings, val error: String? = null)

private class CancelledSettingsRead(val cancellation: CancellationException) : RuntimeException(cancellation)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepository(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.dataStore)

    private val readAttempts = MutableStateFlow(0L)
    fun retryRead() { readAttempts.update { it + 1 } }

    // Each subscription retains only values it actually read. A failed read never writes defaults.
    val settingsState: Flow<SettingsSnapshot> = flow {
        var lastRead: UserSettings? = null
        try {
            emitAll(readAttempts.flatMapLatest {
                store.data.map { SettingsSnapshot(decode(it)) }.onEach {
                    lastRead = it.value
                }.retryWhen { cause, attempt ->
                    if (cause is CancellationException || cause !is Exception) return@retryWhen false
                    emit(SettingsSnapshot(
                        lastRead ?: UserSettings(soundVolume = 0f, musicEnabled = false, animationEnabled = false),
                        "设置暂时无法读取，显示的是临时或上次读取的值。"
                    ))
                    delay((1_000L shl attempt.coerceAtMost(5).toInt()).coerceAtMost(30_000L))
                    true
                }.catch { cause ->
                    // flatMapLatest normally treats a child's cancellation as completion. Preserve
                    // a source cancellation for first() callers, without cancelling a manual restart.
                    if (cause is CancellationException && currentCoroutineContext().isActive) {
                        throw CancelledSettingsRead(cause)
                    }
                    throw cause
                }
            })
        } catch (cancelled: CancelledSettingsRead) {
            throw cancelled.cancellation
        }
    }
    val settingsFlow: Flow<UserSettings> = settingsState.map { it.value }

    private suspend fun write(transform: suspend (MutablePreferences) -> Unit) {
        store.edit(transform)
        retryRead()
    }

    private val KEY_LANGUAGE = stringPreferencesKey("selected_language")
    private val KEY_SHOW_TEXT = booleanPreferencesKey("show_text")
    private val KEY_SMALL_TEXT = booleanPreferencesKey("small_text")
    private val KEY_CHOICE_COUNT = intPreferencesKey("choice_count")
    private val KEY_SHUFFLE = booleanPreferencesKey("shuffle_items")
    private val KEY_AUTO_NEXT = booleanPreferencesKey("auto_next")
    private val KEY_AUTO_PLAY = booleanPreferencesKey("auto_play_prompt")
    private val KEY_AUTO_RECORD = booleanPreferencesKey("auto_start_recording")
    private val KEY_SESSION_LENGTH = intPreferencesKey("session_length")
    private val KEY_SOUND_VOLUME = floatPreferencesKey("sound_volume")
    private val KEY_MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
    private val KEY_ANIMATION_ENABLED = booleanPreferencesKey("animation_enabled")
    private val KEY_COMPLETED_COUNT = intPreferencesKey("completed_count")

    private fun decode(preferences: Preferences): UserSettings {
        val langStr = preferences[KEY_LANGUAGE] ?: LanguageCode.JA_JP.code
        return UserSettings(
            selectedLanguage = LanguageCode.fromCode(langStr),
            showText = preferences[KEY_SHOW_TEXT] ?: true,
            smallText = preferences[KEY_SMALL_TEXT] ?: false,
            choiceCount = (preferences[KEY_CHOICE_COUNT] ?: 3).coerceIn(2, 4),
            shuffleItems = preferences[KEY_SHUFFLE] ?: false,
            autoNext = preferences[KEY_AUTO_NEXT] ?: true,
            autoPlayPrompt = preferences[KEY_AUTO_PLAY] ?: true,
            autoStartRecording = preferences[KEY_AUTO_RECORD] ?: false,
            sessionLength = (preferences[KEY_SESSION_LENGTH] ?: 10).coerceIn(1, 30),
            soundVolume = (preferences[KEY_SOUND_VOLUME] ?: 1.0f).let { if (it.isFinite()) it.coerceIn(0f, 1f) else 1f },
            animationEnabled = preferences[KEY_ANIMATION_ENABLED] ?: true,
            musicEnabled = preferences[KEY_MUSIC_ENABLED] ?: false
        )
    }

    suspend fun setLanguage(language: LanguageCode) {
        write { preferences ->
            preferences[KEY_LANGUAGE] = language.code
        }
    }

    suspend fun setShowText(show: Boolean) {
        write { preferences ->
            preferences[KEY_SHOW_TEXT] = show
        }
    }

    suspend fun setTextDisplayMode(mode: TextDisplayMode) {
        write {
            it[KEY_SHOW_TEXT] = mode != TextDisplayMode.HIDDEN
            it[KEY_SMALL_TEXT] = mode == TextDisplayMode.SMALL
        }
    }

    suspend fun setChoiceCount(count: Int) { write { it[KEY_CHOICE_COUNT] = count.coerceIn(2, 4) } }

    suspend fun setShuffleItems(enabled: Boolean) { write { it[KEY_SHUFFLE] = enabled } }

    suspend fun setAutoNext(autoNext: Boolean) {
        write { preferences ->
            preferences[KEY_AUTO_NEXT] = autoNext
        }
    }

    suspend fun setAutoPlayPrompt(enabled: Boolean) {
        write { it[KEY_AUTO_PLAY] = enabled }
    }

    suspend fun setAutoStartRecording(enabled: Boolean) {
        write { it[KEY_AUTO_RECORD] = enabled }
    }

    suspend fun setSessionLength(length: Int) {
        write { preferences ->
            preferences[KEY_SESSION_LENGTH] = length.coerceIn(1, 30)
        }
    }

    suspend fun setAnimationEnabled(enabled: Boolean) {
        write { preferences ->
            preferences[KEY_ANIMATION_ENABLED] = enabled
        }
    }

    suspend fun setSoundVolume(volume: Float) {
        require(volume.isFinite())
        write { it[KEY_SOUND_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        write { it[KEY_MUSIC_ENABLED] = enabled }
    }

    // Migration input only: errors must propagate rather than importing quiet fallback values.
    suspend fun readLegacyCompletedCount(): Int = (store.data.first()[KEY_COMPLETED_COUNT] ?: 0).coerceAtLeast(0)
}
