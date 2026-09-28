package com.kidslanguage.companion.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.kidslanguage.companion.core.persistence.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

interface SoundEffectManager {
    fun playPop()
    fun playSuccess()
    fun playCelebrate()
    fun playRetry()
    fun stop() {}
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
    private var volume: Float = 1f
    private var settingsJob: Job? = null
    private var released = false
    private val streams = linkedMapOf<Int, Float>()

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
            settingsJob = scope.launch {
                repo.settingsFlow.collectLatest { s ->
                    volume = s.soundVolume.coerceIn(0f, 1f)
                    if (volume == 0f) stop()
                    else if (!released) streams.forEach { (id, gain) ->
                        soundPool.setVolume(id, gain * volume, gain * volume)
                    }
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
        play(popId, 0.7f, 1)
    }

    override fun playSuccess() {
        play(successId, 0.9f, 2)
    }

    override fun playCelebrate() {
        play(celebrateId, 1f, 3)
    }

    override fun playRetry() {
        play(retryId, 0.6f, 1)
    }

    private fun play(sound: Int, gain: Float, priority: Int) {
        if (released || volume <= 0f || sound == 0) return
        if (streams.size >= 4) {
            val oldest = streams.keys.first()
            soundPool.stop(oldest)
            streams.remove(oldest)
        }
        val stream = soundPool.play(sound, gain * volume, gain * volume, priority, 0, 1f)
        if (stream != 0) streams[stream] = gain
    }

    override fun stop() {
        if (released) return
        streams.keys.forEach(soundPool::stop)
        streams.clear()
    }

    override fun release() {
        if (released) return
        settingsJob?.cancel()
        stop()
        released = true
        soundPool.release()
    }
}
