package com.kidslanguage.companion.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer

interface BackgroundMusicPlayer {
    fun play()
    fun stop()
}

class AndroidBackgroundMusicPlayer(private val context: Context) : BackgroundMusicPlayer {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null

    override fun play() {
        if (player != null) return
        val current = MediaPlayer()
        player = current
        var prepared = false
        var hasFocus = false
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                if (player !== current) return@setOnAudioFocusChangeListener
                when (change) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        hasFocus = true
                        current.setVolume(0.18f, 0.18f)
                        if (prepared) current.start()
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> stop()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        hasFocus = false
                        if (prepared) current.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> current.setVolume(0.04f, 0.04f)
                }
            }.build()
        focus = request
        try {
            hasFocus = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!hasFocus) { stop(); return }
            current.setAudioAttributes(attributes)
            context.assets.openFd("audio/music/home.wav").use {
                current.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            current.isLooping = true
            current.setVolume(0.18f, 0.18f)
            current.setOnPreparedListener {
                if (player === current) {
                    prepared = true
                    if (hasFocus) current.start()
                }
            }
            current.setOnErrorListener { _, _, _ ->
                if (player === current) stop()
                true
            }
            current.prepareAsync()
        } catch (_: Exception) { stop() }
    }

    override fun stop() {
        val old = player
        player = null
        old?.release()
        focus?.let(audioManager::abandonAudioFocusRequest)
        focus = null
    }
}
