package com.kidslanguage.companion.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.kidslanguage.companion.core.persistence.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

interface SoundEffectManager {
    fun playPop()
    fun playSuccess()
    fun playCelebrate()
    fun playRetry()
    fun release()
}

class AndroidSoundEffectManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository? = null,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : SoundEffectManager {

    private val soundPool: SoundPool
    private var popId: Int = 0
    private var successId: Int = 0
    private var celebrateId: Int = 0
    private var retryId: Int = 0

    @Volatile
    private var isSoundEnabled: Boolean = true

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        loadSounds()

        settingsRepository?.let { repo ->
            scope.launch {
                repo.settingsFlow.collectLatest { s ->
                    isSoundEnabled = s.soundVolume > 0f
                }
            }
        }
    }

    private fun loadSounds() {
        try {
            context.assets.openFd("audio/sfx/pop.wav").use { afd ->
                popId = soundPool.load(afd, 1)
            }
        } catch (_: Exception) {}

        try {
            context.assets.openFd("audio/sfx/success.wav").use { afd ->
                successId = soundPool.load(afd, 1)
            }
        } catch (_: Exception) {}

        try {
            context.assets.openFd("audio/sfx/celebrate.wav").use { afd ->
                celebrateId = soundPool.load(afd, 1)
            }
        } catch (_: Exception) {}

        try {
            context.assets.openFd("audio/sfx/retry.wav").use { afd ->
                retryId = soundPool.load(afd, 1)
            }
        } catch (_: Exception) {}
    }

    override fun playPop() {
        if (!isSoundEnabled || popId == 0) return
        soundPool.play(popId, 0.7f, 0.7f, 1, 0, 1.0f)
    }

    override fun playSuccess() {
        if (!isSoundEnabled || successId == 0) return
        soundPool.play(successId, 0.9f, 0.9f, 2, 0, 1.0f)
    }

    override fun playCelebrate() {
        if (!isSoundEnabled || celebrateId == 0) return
        soundPool.play(celebrateId, 1.0f, 1.0f, 3, 0, 1.0f)
    }

    override fun playRetry() {
        if (!isSoundEnabled || retryId == 0) return
        soundPool.play(retryId, 0.6f, 0.6f, 1, 0, 1.0f)
    }

    override fun release() {
        soundPool.release()
    }
}
