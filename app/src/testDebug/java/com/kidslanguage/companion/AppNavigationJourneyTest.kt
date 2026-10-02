package com.kidslanguage.companion

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.kidslanguage.companion.app.AppDependencies
import com.kidslanguage.companion.core.asr.*
import com.kidslanguage.companion.core.audio.*
import com.kidslanguage.companion.core.content.ContentRepository
import com.kidslanguage.companion.core.model.*
import com.kidslanguage.companion.core.persistence.*
import com.kidslanguage.companion.core.scoring.ChildSpeechEvaluationService
import com.kidslanguage.companion.ui.navigation.AppNavigation
import com.kidslanguage.companion.ui.theme.KidsLanguageTheme
import com.kidslanguage.companion.ui.theme.LocalAnimationsEnabled
import java.io.IOException
import java.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class, qualifiers = "w1280dp-h800dp-land-mdpi")
class AppNavigationJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private class Recognizer : SpeechRecognizerEngine {
        var failLoad = false
        var listening = false
        var loadGate: CompletableDeferred<Unit>? = null
        val loads = mutableListOf<LanguageCode>()
        var callback: ((RecognitionResult) -> Unit)? = null
        override suspend fun load(language: LanguageCode): Result<Unit> {
            loads += language
            loadGate?.await()
            return if (failLoad) Result.failure(IOException("model unavailable")) else Result.success(Unit)
        }
        override suspend fun startListening(language: LanguageCode, onResult: (RecognitionResult) -> Unit) {
            listening = true
            callback = onResult
        }
        override fun stopListening() { listening = false }
        override fun cancelListening() { listening = false }
        override suspend fun unload() { listening = false }
        override suspend fun recognizeFromAudio(pcm: ByteArray): RecognitionResult = error("unexpected PCM call")
        fun result(text: String) { checkNotNull(callback)(RecognitionResult(text)) }
    }

    private class Audio : PromptPlayer {
        val prompts = mutableListOf<Pair<LanguageCode, String>>()
        val voices = mutableListOf<String>()
        override suspend fun playPrompt(text: String, language: LanguageCode, audioAssetPath: String?) {
            prompts += language to text
        }
        override suspend fun playVoiceAsset(audioAssetPath: String, volume: Float) { voices += audioAssetPath }
        override fun stop() {}
        override fun release() {}
    }

    private class Records : ProgressRepository {
        val attempts = mutableListOf<PracticeAttemptEntity>()
        override val totals = MutableStateFlow(ProgressTotals())
        override val today = totals
        override val languages = MutableStateFlow(emptyList<LanguageProgress>())
        override val error = MutableStateFlow<String?>(null)
        override fun record(attempt: PracticeAttemptEntity) { attempts += attempt }
        override suspend fun clear() { error("Journey must not clear records") }
    }

    private class Dependencies : AppDependencies {
        override val speechRecognizerEngine = Recognizer()
        override val promptPlayer = Audio()
        override val progressRepository = Records()
        override val speechEvaluationService = ChildSpeechEvaluationService()
        override val soundEffectManager = object : SoundEffectManager {
            override fun playPop() {}
            override fun playSuccess() {}
            override fun playCelebrate() {}
            override fun playRetry() {}
            override fun release() {}
        }
        override val backgroundMusic = object : BackgroundMusicPlayer {
            override fun play() {}
            override fun stop() {}
        }
        override val settingsRepository = SettingsRepository(object : DataStore<Preferences> {
            override val data = MutableStateFlow(preferencesOf(
                booleanPreferencesKey("auto_next") to false,
                booleanPreferencesKey("animation_enabled") to false,
                intPreferencesKey("session_length") to 1,
            ))
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
                transform(data.value).also { data.value = it }
        })
        override val contentRepository = object : ContentRepository {
            override suspend fun getCategories(language: LanguageCode) =
                listOf(Category("greetings", language.code, "打招呼", icon = "👋"))
            override suspend fun getItems(language: LanguageCode, categoryId: String?) = listOf(
                PracticeItem("${language.code}-hello", language.code, "greetings",
                    text = when (language) {
                        LanguageCode.EN_US -> "hello"
                        LanguageCode.ZH_CN -> "你好"
                        LanguageCode.JA_JP -> "こんにちは"
                    }, image = "👋", distractors = listOf("🐱", "🐶"), translation = "打招呼",
                    audio = "audio/${language.code}/hello.mp3")
            )
            override suspend fun getItemById(id: String): PracticeItem? = null
        }
    }

    private val dependencies = Dependencies()

    // Compose effects and ViewModel coroutines use different virtual clocks in Robolectric.
    private fun advance(milliseconds: Long) {
        compose.mainClock.advanceTimeBy(milliseconds)
        compose.runOnUiThread { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(milliseconds)) }
        compose.waitForIdle()
    }

    private fun launchApp(microphoneGranted: Boolean = true) {
        val application = shadowOf(compose.activity.application)
        if (microphoneGranted) application.grantPermissions(Manifest.permission.RECORD_AUDIO)
        else application.denyPermissions(Manifest.permission.RECORD_AUDIO)
        compose.setContent {
            KidsLanguageTheme {
                CompositionLocalProvider(LocalAnimationsEnabled provides false) { AppNavigation(dependencies) }
            }
        }
        advance(1_300)
        compose.onNodeWithText("开始练习").assertIsDisplayed()
    }

    private fun openCategory(language: LanguageCode) {
        val flag = when (language) {
            LanguageCode.EN_US -> "🇺🇸"
            LanguageCode.ZH_CN -> "🇨🇳"
            LanguageCode.JA_JP -> "🇯🇵"
        }
        compose.onNodeWithText(flag).performClick()
        compose.onNodeWithText("开始练习").performClick()
        compose.onNodeWithText("${language.displayName} · 选择场景").assertIsDisplayed()
    }

    private fun enterPractice(mode: PracticeMode, language: LanguageCode = LanguageCode.EN_US) {
        openCategory(language)
        compose.onNodeWithText("打招呼").performClick()
        compose.onNodeWithText(mode.title).performClick()
        compose.waitForIdle()
    }

    private fun readyMicrophone() {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onNode(hasContentDescription("Tap to Speak") and isEnabled()).isDisplayed()
        }
    }

    private fun waitForCompletionPage() {
        // Completion navigates from a LaunchedEffect, after the button callback returns.
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("回主页").fetchSemanticsNodes().size == 1
        }
    }

    private fun startRecording() {
        readyMicrophone()
        compose.onNodeWithContentDescription("Tap to Speak").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onNodeWithContentDescription("Recording").isDisplayed()
        }
        compose.onNodeWithContentDescription("Recording").assertIsDisplayed()
        compose.runOnIdle { assertTrue(dependencies.speechRecognizerEngine.listening) }
    }

    @Test fun allLanguageChoicesReachTheMatchingCategoryAndReturnHome() {
        launchApp()
        LanguageCode.entries.forEach { language ->
            openCategory(language)
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("开始练习").assertIsDisplayed()
            advance(600)
        }
    }

    @Test fun chooseAssistedCompletionCanReplayAndFinishANewRoundWithoutDuplicateRecords() {
        launchApp()
        enterPractice(PracticeMode.CHOOSE)
        repeat(3) {
            compose.onNodeWithText("🐱").performClick()
            advance(600)
        }
        compose.runOnIdle {
            assertEquals(3, dependencies.progressRepository.attempts.size)
            assertEquals(1, dependencies.progressRepository.attempts.sumOf { it.completed })
            assertTrue(dependencies.promptPlayer.voices.contains(PaoPaoVoices.PRAISE_EFFORT))
        }
        compose.onNodeWithContentDescription("下一题").performClick()
        waitForCompletionPage()
        compose.onNodeWithText("再玩一次").performScrollTo().performClick()
        compose.onNodeWithText("🐶").assertIsDisplayed()
        compose.onNodeWithText("👋").performClick()
        advance(600)
        compose.onNodeWithContentDescription("下一题").performClick()
        waitForCompletionPage()
        compose.onNodeWithText("回主页").performScrollTo().performClick()
        advance(2_000)
        compose.onAllNodesWithText("开始练习").assertCountEquals(1)
        compose.runOnIdle {
            val attempts = dependencies.progressRepository.attempts
            assertEquals(4, attempts.size)
            assertEquals(2, attempts.sumOf { it.completed })
            assertEquals(2, attempts.map { it.itemRunId }.distinct().size)
        }
    }

    @Test fun repeatRetriesThenAssistsAndReplayStartsWithANewRecordingState() {
        launchApp()
        enterPractice(PracticeMode.REPEAT)
        repeat(3) { attempt ->
            startRecording()
            compose.runOnIdle { dependencies.speechRecognizerEngine.result("banana") }
            if (attempt < 2) compose.onNodeWithText("再试试看哦！", substring = true).assertIsDisplayed()
            else compose.onNodeWithText("太棒啦！", substring = true).assertIsDisplayed()
            advance(600)
        }
        compose.onNodeWithText("下一题").performClick()
        waitForCompletionPage()
        compose.onNodeWithText("再玩一次").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Tap to Speak").assertIsDisplayed()
        compose.onNodeWithText("太棒啦！", substring = true).assertDoesNotExist()
        compose.runOnIdle {
            val attempts = dependencies.progressRepository.attempts
            assertEquals(3, attempts.size)
            assertEquals(1, attempts.sumOf { it.assistedPasses })
            assertEquals(1, attempts.sumOf { it.completed })
            assertEquals(listOf(LanguageCode.EN_US, LanguageCode.EN_US), dependencies.speechRecognizerEngine.loads)
        }
    }

    @Test fun modelRecoveryCanSwitchModesAndLateRecordingCallbacksCannotReviveTheExitedPage() {
        dependencies.speechRecognizerEngine.failLoad = true
        launchApp()
        enterPractice(PracticeMode.REPEAT, LanguageCode.ZH_CN)
        compose.onNodeWithText("玩选图").performClick()
        compose.onNodeWithText("🐶").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("选择练习玩法").assertIsDisplayed()
        compose.runOnIdle { dependencies.speechRecognizerEngine.failLoad = false }
        compose.onNodeWithText(PracticeMode.REPEAT.title).performClick()
        startRecording()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("选择练习玩法").assertIsDisplayed()
        compose.runOnIdle {
            assertFalse(dependencies.speechRecognizerEngine.listening)
            dependencies.speechRecognizerEngine.result("你好")
        }
        advance(5_000)
        compose.onNodeWithText("选择练习玩法").assertIsDisplayed()
        compose.onNodeWithText("太棒啦！", substring = true).assertDoesNotExist()
        compose.runOnIdle {
            assertTrue(dependencies.progressRepository.attempts.isEmpty())
            assertTrue(dependencies.promptPlayer.voices.isEmpty())
        }
    }

    @Test fun parentGateIsRequiredBeforeSettingsAndBackReturnsHome() {
        launchApp()
        compose.onNodeWithContentDescription("Parent Settings").performClick()
        compose.onNodeWithText("请输入答案").performTextInput("0")
        compose.onNodeWithText("确定").performClick()
        compose.onNodeWithText("答案不正确，请重试").assertIsDisplayed()
        compose.onNodeWithText("家长管理与设置").assertDoesNotExist()
        val question = compose.onNodeWithText("×", substring = true).fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString { it.text }
        val match = checkNotNull(Regex("(\\d+)\\s*×\\s*(\\d+)").find(question))
        val answer = match.groupValues[1].toInt() * match.groupValues[2].toInt()
        compose.onNodeWithText("请输入答案").performTextInput(answer.toString())
        compose.onNodeWithText("确定").performClick()
        compose.onNodeWithText("家长管理与设置").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("开始练习").assertIsDisplayed()
    }

    @Test fun deniedMicrophonePermissionOffersAPlayableChooseJourney() {
        launchApp(microphoneGranted = false)
        enterPractice(PracticeMode.REPEAT)
        readyMicrophone()
        compose.onNodeWithContentDescription("Tap to Speak").performClick()
        compose.runOnIdle {
            val request = checkNotNull(shadowOf(compose.activity).lastRequestedPermission)
            assertArrayEquals(arrayOf(Manifest.permission.RECORD_AUDIO), request.requestedPermissions)
            assertFalse(dependencies.speechRecognizerEngine.listening)
            compose.activity.onRequestPermissionsResult(request.requestCode, request.requestedPermissions,
                intArrayOf(PackageManager.PERMISSION_DENIED))
        }
        compose.onNodeWithText("麦克风还没打开", substring = true).assertIsDisplayed()
        compose.onNodeWithText("玩选图").performClick()
        compose.onNodeWithText("👋").performClick()
        advance(600)
        compose.onNodeWithContentDescription("下一题").performClick()
        waitForCompletionPage()
        compose.onNodeWithText("回主页").performScrollTo().performClick()
        compose.onNodeWithText("开始练习").assertIsDisplayed()
        compose.runOnIdle {
            assertFalse(dependencies.speechRecognizerEngine.listening)
            assertNull(dependencies.speechRecognizerEngine.callback)
            assertEquals(1, dependencies.progressRepository.attempts.size)
            assertEquals(1, dependencies.progressRepository.attempts.sumOf { it.completed })
        }
    }

    @Test fun chooseAutoNextCompletesOnceAndDoesNotReturnAfterGoingHome() {
        autoNextJourney(PracticeMode.CHOOSE)
    }

    @Test fun repeatAutoNextCompletesOnceAndDoesNotReturnAfterGoingHome() {
        autoNextJourney(PracticeMode.REPEAT)
    }

    @Test fun microphoneIsDisabledUntilModelReadyAndDuringPassedFeedback() {
        val modelReady = CompletableDeferred<Unit>()
        dependencies.speechRecognizerEngine.loadGate = modelReady
        launchApp()
        enterPractice(PracticeMode.REPEAT)
        compose.onNodeWithText("正在准备语音", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Tap to Speak").assertIsNotEnabled()
            .performTouchInput { click() }
        compose.runOnIdle {
            assertFalse(dependencies.speechRecognizerEngine.listening)
            modelReady.complete(Unit)
        }
        startRecording()
        compose.runOnIdle { dependencies.speechRecognizerEngine.result("hello") }
        compose.onNodeWithContentDescription("Tap to Speak").assertIsNotEnabled()
        advance(600)
        compose.onNodeWithContentDescription("Tap to Speak").assertIsNotEnabled()
        compose.onNodeWithText("下一题").assertIsDisplayed()
    }

    private fun autoNextJourney(mode: PracticeMode) {
        runBlocking { dependencies.settingsRepository.setAutoNext(true) }
        launchApp()
        enterPractice(mode)
        if (mode == PracticeMode.REPEAT) {
            startRecording()
            compose.runOnIdle { dependencies.speechRecognizerEngine.result("hello") }
        } else {
            compose.onNodeWithText("👋").performClick()
        }
        advance(1_200)
        waitForCompletionPage()
        compose.onAllNodesWithText("再玩一次").assertCountEquals(1)
        compose.onNodeWithText("回主页").performScrollTo().performClick()
        advance(5_000)
        compose.onAllNodesWithText("开始练习").assertCountEquals(1)
        compose.onNodeWithText("再玩一次").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, dependencies.progressRepository.attempts.size)
            assertEquals(1, dependencies.progressRepository.attempts.sumOf { it.completed })
            val voices = dependencies.promptPlayer.voices
            assertEquals(2, voices.size)
            assertTrue(voices.first() in listOf(PaoPaoVoices.PRAISE_1, PaoPaoVoices.PRAISE_2))
            assertEquals(PaoPaoVoices.CELEBRATE, voices.last())
            assertFalse(dependencies.speechRecognizerEngine.listening)
        }
    }
}
