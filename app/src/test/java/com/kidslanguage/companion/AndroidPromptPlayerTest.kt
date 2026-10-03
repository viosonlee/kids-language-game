package com.kidslanguage.companion

import android.app.Application
import android.media.MediaPlayer
import android.media.AudioManager
import android.content.Context
import org.robolectric.Shadows.shadowOf
import com.kidslanguage.companion.core.audio.AudioPlaybackInterruptedException
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.audio.AndroidPromptPlayer
import com.kidslanguage.companion.core.audio.PaoPaoVoices
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class AndroidPromptPlayerTest {
    private fun scenario(block: suspend TestScope.(AndroidPromptPlayer, MutableList<Pair<MediaPlayer, ShadowMediaPlayer>>) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val created = mutableListOf<Pair<MediaPlayer, ShadowMediaPlayer>>()
        ShadowMediaPlayer.setCreateListener { player, shadow -> created += player to shadow }
        // Negative delay leaves preparation pending until the test explicitly completes it.
        ShadowMediaPlayer.setMediaInfoProvider { ShadowMediaPlayer.MediaInfo(1000, -1) }
        val player = AndroidPromptPlayer(RuntimeEnvironment.getApplication())
        try {
            block(player, created)
        } finally {
            player.release()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    private fun audio() = shadowOf(RuntimeEnvironment.getApplication()
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager)

    @Test fun deniedFocusDoesNotCreatePlayerAndReportsInterruption() = scenario { player, created ->
        audio().setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        var interrupted = false
        launch {
            try { player.playVoiceAsset(PaoPaoVoices.GREETING) }
            catch (_: AudioPlaybackInterruptedException) { interrupted = true }
        }
        runCurrent()
        assertTrue(interrupted)
        assertTrue(created.isEmpty())
        assertNotNull(audio().lastAbandonedAudioFocusRequest)
    }

    @Test fun focusLossDuringPreparationReleasesAndNeverStartsLateAudio() = scenario { player, created ->
        var interrupted = false
        launch {
            try { player.playVoiceAsset(PaoPaoVoices.GREETING) }
            catch (_: AudioPlaybackInterruptedException) { interrupted = true }
        }
        runCurrent()
        val (native, media) = created.single()
        val ready = media.onPreparedListener
        audio().lastAudioFocusRequest.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        runCurrent()
        assertTrue(interrupted)
        assertEquals(ShadowMediaPlayer.State.END, media.state)
        ready.onPrepared(native)
        runCurrent()
        assertFalse(media.isReallyPlaying)
    }

    @Test fun oldFocusLossDoesNotInterruptReplacementButCurrentLossDoes() = scenario { player, created ->
        launch { player.playVoiceAsset(PaoPaoVoices.GREETING) }
        runCurrent()
        val oldFocus = audio().lastAudioFocusRequest.listener
        var interrupted = false
        val replacement = launch {
            try { player.playVoiceAsset(PaoPaoVoices.PRAISE_1) }
            catch (_: AudioPlaybackInterruptedException) { interrupted = true }
        }
        runCurrent()
        val media = created.last().second
        media.invokePreparedListener()
        runCurrent()
        oldFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        runCurrent()
        assertTrue(media.isReallyPlaying)
        assertTrue(replacement.isActive)
        audio().lastAudioFocusRequest.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        runCurrent()
        assertTrue(interrupted)
        assertEquals(ShadowMediaPlayer.State.END, media.state)
    }

    @Test fun focusLossWhileWaitingForTtsDoesNotWaitForInitializationTimeout() = scenario { player, created ->
        var interrupted = false
        launch {
            try { player.playPrompt("hello", LanguageCode.EN_US) }
            catch (_: AudioPlaybackInterruptedException) { interrupted = true }
        }
        runCurrent()
        audio().lastAudioFocusRequest.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        runCurrent()
        assertTrue(interrupted)
        assertTrue(created.isEmpty())
        assertNotNull(audio().lastAbandonedAudioFocusRequest)
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test fun preparationDoesNotBlockCallerAndAudioStartsOnlyAfterReady() = scenario { player, created ->
        val playback = launch { player.playVoiceAsset(PaoPaoVoices.GREETING) }
        runCurrent()
        val media = created.single().second
        assertEquals(ShadowMediaPlayer.State.PREPARING, media.state)
        assertFalse(media.isReallyPlaying)
        var mainThreadActionRan = false
        launch(Dispatchers.Main) { mainThreadActionRan = true }
        runCurrent()
        assertTrue(mainThreadActionRan)
        media.invokePreparedListener()
        runCurrent()
        assertTrue(media.isReallyPlaying)
        assertTrue(playback.isActive)
        media.invokeCompletionListener()
        runCurrent()
        assertTrue(playback.isCompleted)
        assertEquals(ShadowMediaPlayer.State.END, media.state)
    }

    @Test fun stopDuringPreparationCancelsWaiterAndRejectsLateReadyCallback() = scenario { player, created ->
        val playback = launch { player.playVoiceAsset(PaoPaoVoices.GREETING) }
        runCurrent()
        val (native, media) = created.single()
        val callback = media.onPreparedListener
        player.stop()
        runCurrent()
        assertTrue(playback.isCancelled)
        callback.onPrepared(native)
        runCurrent()
        assertFalse(media.isReallyPlaying)
        assertEquals(ShadowMediaPlayer.State.END, media.state)
    }

    @Test fun replacementIgnoresOldReadyAndErrorCallbacks() = scenario { player, created ->
        val first = launch { player.playVoiceAsset(PaoPaoVoices.GREETING) }
        runCurrent()
        val (oldNative, old) = created.single()
        val lateReady = old.onPreparedListener
        val second = launch { player.playVoiceAsset(PaoPaoVoices.PRAISE_1) }
        runCurrent()
        val current = created.last().second
        assertTrue(first.isCancelled)
        assertEquals(ShadowMediaPlayer.State.END, old.state)
        lateReady.onPrepared(oldNative)
        old.invokeErrorListener(1, 0)
        runCurrent()
        assertEquals(ShadowMediaPlayer.State.PREPARING, current.state)
        assertTrue(second.isActive)
        current.invokePreparedListener()
        runCurrent()
        assertTrue(current.isReallyPlaying)
        // invokeErrorListener itself changes the shadow state to ERROR, even after release.
        assertFalse(old.isReallyPlaying)
        current.invokeCompletionListener()
        runCurrent()
        assertTrue(second.isCompleted)
    }

    @Test fun cancellingOwningScreenDuringPreparationReleasesPlayer() = scenario { player, created ->
        val playback = launch { player.playVoiceAsset(PaoPaoVoices.GREETING) }
        runCurrent()
        val (native, media) = created.single()
        val lateReady = media.onPreparedListener
        playback.cancel()
        runCurrent()
        assertEquals(ShadowMediaPlayer.State.END, media.state)
        lateReady.onPrepared(native)
        runCurrent()
        assertFalse(media.isReallyPlaying)
    }

    @Test fun preparationErrorReleasesOptionalVoiceAndDoesNotHang() = scenario { player, created ->
        val playback = launch { player.playVoiceAsset(PaoPaoVoices.GREETING) }
        runCurrent()
        val media = created.single().second
        media.invokeErrorListener(1, 0)
        runCurrent()
        assertTrue(playback.isCompleted)
        assertFalse(playback.isCancelled)
        assertEquals(ShadowMediaPlayer.State.END, media.state)
    }

    @Test fun missingPreparationCallbackTimesOutAndReleasesPlayer() = scenario { player, created ->
        val playback = launch { player.playVoiceAsset(PaoPaoVoices.GREETING) }
        runCurrent()
        advanceTimeBy(30_001)
        runCurrent()
        assertTrue(playback.isCompleted)
        assertEquals(ShadowMediaPlayer.State.END, created.single().second.state)
    }
}
