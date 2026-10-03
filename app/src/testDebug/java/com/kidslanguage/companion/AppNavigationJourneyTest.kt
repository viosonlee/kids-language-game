package com.kidslanguage.companion

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextLayoutResult
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
        var textOverride: String? = null
        var readingOverride: String? = null
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
                    text = textOverride ?: when (language) {
                        LanguageCode.EN_US -> "hello"
                        LanguageCode.ZH_CN -> "你好"
                        LanguageCode.JA_JP -> "こんにちは"
                    }, reading = readingOverride, image = "👋", distractors = listOf("🐱", "🐶"), translation = "打招呼",
                    audio = "audio/${language.code}/hello.mp3")
            )
            override suspend fun getItemById(id: String): PracticeItem? = null
        }
    }

    private val dependencies = Dependencies()
    private val fontScale = mutableStateOf(1f)

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
                CompositionLocalProvider(
                    LocalAnimationsEnabled provides false,
                    LocalDensity provides Density(LocalDensity.current.density, fontScale.value),
                ) { AppNavigation(dependencies) }
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
        repeat(3) { attempt ->
            compose.onNodeWithText("🐱").performClick()
            advance(if (attempt < 2) 600 else 900)
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
        advance(900)
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
            advance(if (attempt < 2) 600 else 900)
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
        advance(900)
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
        // Recognition callbacks update the StateFlow asynchronously; await recomposition first.
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Tap to Speak").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithContentDescription("Tap to Speak").assertIsNotEnabled()
        advance(600)
        compose.onNodeWithContentDescription("Tap to Speak").assertIsNotEnabled()
        advance(300)
        compose.onNodeWithText("下一题").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w960dp-h600dp-land-mdpi")
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun repeatLongCjkAtLargeFontCanScrollWithoutCoveringMicrophone() {
        val text = "今天我们一起去公园看看小动物，然后和好朋友打招呼。"
        val reading = "きょうはともだちといっしょにこうえんへいきましょう。"
        dependencies.textOverride = text
        dependencies.readingOverride = reading
        launchApp()
        enterPractice(PracticeMode.REPEAT)
        readyMicrophone()
        compose.runOnIdle { fontScale.value = 1.5f }
        compose.onNodeWithText(reading).performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(reading).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult
        ) { it(layouts) }
        assertFalse("Reading must be laid out in full: ${layouts.single().size}, lines=${layouts.single().lineCount}, width=${layouts.single().didOverflowWidth}, height=${layouts.single().didOverflowHeight}, text=${layouts.single().layoutInput.text}", layouts.single().hasVisualOverflow)
        val readingBounds = compose.onNodeWithText(reading).fetchSemanticsNode().boundsInRoot
        val micBounds = compose.onNodeWithContentDescription("Tap to Speak").fetchSemanticsNode().boundsInRoot
        val speakerBounds = compose.onNodeWithContentDescription("Speaker").fetchSemanticsNode().boundsInRoot
        assertTrue("Reading overlaps microphone", readingBounds.bottom <= micBounds.top)
        assertTrue("Reading overlaps header", readingBounds.top >= speakerBounds.bottom)
        saveNativePreview("repeat-large-font")
        compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
        startRecording()
        compose.runOnIdle { assertTrue(dependencies.speechRecognizerEngine.listening) }
    }

    @Test
    @Config(qualifiers = "w960dp-h600dp-land-mdpi")
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun homeLargeFontKeepsLanguageLabelsWholeAndStartReachable() {
        launchApp()
        compose.runOnIdle { fontScale.value = 1.5f }
        saveNativePreview("home-large-font-initial")
        LanguageCode.entries.forEach { language ->
            val labels = compose.onAllNodesWithText(language.englishName, useUnmergedTree = true)
            labels.assertCountEquals(if (language.displayName == language.englishName) 2 else 1)
            for (index in labels.fetchSemanticsNodes().indices) {
                val label = labels[index]
                label.performScrollTo().assertIsDisplayed()
                compose.onNodeWithText("开始练习").assertIsDisplayed()
                val layouts = mutableListOf<TextLayoutResult>()
                label.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
                val layout = layouts.single()
                // A shrink-wrapped Text may retain a wider paragraph box; inspect the glyph line itself.
                assertEquals("Broken language name: ${language.englishName}", 1, layout.lineCount)
                assertFalse(layout.isLineEllipsized(0))
                assertEquals(language.englishName.length, layout.getLineEnd(0))
                assertTrue("Clipped language right edge", layout.getLineRight(0) <= layout.size.width + 1f)
                assertTrue("Clipped language left edge", layout.getLineLeft(0) >= -1f)
                assertTrue("Clipped language height", layout.getLineBottom(0) <= layout.size.height + 1f)
            }
        }
        saveNativePreview("home-large-font")
        compose.onNodeWithText("开始练习").assertIsDisplayed().performClick()
        advance(200)
        compose.onNodeWithText("打招呼").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w960dp-h600dp-land-mdpi")
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun categoryAndModesAtDoubleFontRemainReadableAndEnterPractice() {
        launchApp()
        openCategory(LanguageCode.EN_US)
        compose.runOnIdle { fontScale.value = 2f }
        saveNativePreview("category-double-font")
        assertWholeText("English · 选择场景")
        compose.onNodeWithText("打招呼").assertIsDisplayed().performClick()
        saveNativePreview("modes-double-font")
        assertWholeText("选择练习玩法")
        PracticeMode.entries.forEach { assertWholeText(it.title) }
        val back = compose.onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("选择练习玩法").fetchSemanticsNode().boundsInRoot
        assertFalse("Mode heading covers back control", back.overlaps(title))
        compose.onNodeWithText(PracticeMode.CHOOSE.title).assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Speaker").assertIsDisplayed()
        compose.onNodeWithText("👋").performScrollTo().assertIsDisplayed().performClick()
        advance(900)
        compose.onNode(hasScrollAction()).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.ScrollBy
        ) { it(0f, -10_000f) }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription("Mascot Great").fetchSemanticsNodes().size == 1
        }
        val rabbit = compose.onNodeWithContentDescription("Mascot Great").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val next = compose.onNodeWithContentDescription("下一题").fetchSemanticsNode().boundsInRoot
        assertTrue("Rabbit feedback is cut off by footer", rabbit.bottom <= next.top)
        val speaker = compose.onNodeWithContentDescription("Speaker").fetchSemanticsNode().boundsInRoot
        assertTrue("Rabbit feedback is cut off by header", rabbit.top >= speaker.bottom)
        saveNativePreview("choose-manual-next-double-font")
        compose.onNodeWithContentDescription("下一题").assertIsDisplayed().performClick()
        waitForCompletionPage()
        compose.runOnIdle { assertEquals(1, dependencies.progressRepository.attempts.size) }
    }

    @Test
    @Config(qualifiers = "w794dp-h361dp-land-mdpi")
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun landscapePhoneKeepsHomeAndPracticeControlsInsideViewport() {
        launchApp()
        fun fullyVisible(node: SemanticsNodeInteraction) {
            node.assertIsDisplayed()
            val bounds = node.fetchSemanticsNode().boundsInRoot
            val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
            assertTrue("Clipped node: $bounds / $root", bounds.top >= root.top &&
                bounds.left >= root.left && bounds.bottom <= root.bottom && bounds.right <= root.right)
        }
        listOf("🇯🇵", "🇨🇳", "🇺🇸", "开始练习").forEach {
            fullyVisible(compose.onNodeWithText(it))
        }
        fullyVisible(compose.onNodeWithContentDescription("Mascot Idle"))
        saveNativePreview("phone-home")
        openCategory(LanguageCode.EN_US)
        saveNativePreview("phone-category")
        compose.onNodeWithText("打招呼").performClick()
        PracticeMode.entries.forEach { fullyVisible(compose.onNodeWithText(it.title)) }
        saveNativePreview("phone-modes")
        compose.onNodeWithText(PracticeMode.REPEAT.title).performClick()
        readyMicrophone()
        fullyVisible(compose.onNodeWithContentDescription("Tap to Speak"))
        fullyVisible(compose.onNodeWithContentDescription("Speaker"))
        assertWholeText("hello")
        val text = compose.onNodeWithText("hello").fetchSemanticsNode().boundsInRoot
        val mic = compose.onNodeWithContentDescription("Tap to Speak").fetchSemanticsNode().boundsInRoot
        assertTrue("Prompt overlaps microphone", text.bottom <= mic.top)
        saveNativePreview("phone-repeat")
        startRecording()
        compose.runOnIdle { dependencies.speechRecognizerEngine.result("hello") }
        advance(900)
        fullyVisible(compose.onNodeWithText("下一题"))
        saveNativePreview("phone-feedback")
        compose.onNodeWithText("下一题").performClick()
        waitForCompletionPage()
        fullyVisible(compose.onNodeWithText("回主页"))
        fullyVisible(compose.onNodeWithText("再玩一次"))
        saveNativePreview("phone-complete")
        compose.onNodeWithText("回主页").performClick()
        advance(600)
        enterPractice(PracticeMode.CHOOSE)
        listOf("👋", "🐱", "🐶").forEach { fullyVisible(compose.onNodeWithText(it)) }
        saveNativePreview("phone-choose")
        compose.onNodeWithText("👋").performClick()
        advance(900)
        fullyVisible(compose.onNodeWithContentDescription("下一题"))
        saveNativePreview("phone-choose-next")
        compose.onNodeWithContentDescription("下一题").performClick()
        waitForCompletionPage()
    }

    @Test
    @Config(qualifiers = "w794dp-h361dp-land-mdpi")
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun landscapePhoneBundledCategoriesFit() {
        compose.setContent {
            KidsLanguageTheme {
                CompositionLocalProvider(LocalAnimationsEnabled provides false) {
                    com.kidslanguage.companion.feature.category.CategoryScreen(
                        language = LanguageCode.ZH_CN,
                        contentRepository = com.kidslanguage.companion.core.content.AssetContentRepository(compose.activity),
                        onCategorySelected = {}, onBack = {},
                    )
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("问候与礼貌").fetchSemanticsNodes().isNotEmpty() }
        listOf("问候与礼貌", "一起玩耍", "日常习惯", "求助与表达").forEach { assertWholeText(it) }
        saveNativePreview("phone-bundled-categories")
    }

    @Test
    @Config(qualifiers = "w794dp-h361dp-land-mdpi")
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun landscapePhoneDiagnosticRecordingAndResultsRemainReachable() {
        compose.setContent {
            KidsLanguageTheme {
                com.kidslanguage.companion.feature.parent.DevAudioDiagnosticScreen(dependencies, {})
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("こんにちは").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("こんにちは")[0].performClick()
        compose.onNodeWithText("🎙️ 开始录音测试").performScrollTo().assertIsDisplayed()
        assertWholeText("🎙️ 开始录音测试")
        saveNativePreview("phone-diagnostic-record")
        compose.onNodeWithText("📊 实时诊断数据 (Live Diagnostic Output):").performScrollTo().assertIsDisplayed()
        saveNativePreview("phone-diagnostic-results")
    }

    @Test
    @Config(qualifiers = "w794dp-h361dp-land-mdpi")
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun landscapePhoneSplashKeepsTitleAndTaglineVisible() {
        checkSplashLayout(1f, "phone-splash")
    }

    @Test
    @Config(qualifiers = "w794dp-h361dp-land-mdpi")
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun landscapePhoneSplashKeepsDoubleFontTextVisible() {
        checkSplashLayout(2f, "phone-splash-double-font")
    }

    private fun checkSplashLayout(scale: Float, preview: String) {
        compose.setContent {
            KidsLanguageTheme {
                CompositionLocalProvider(
                    LocalAnimationsEnabled provides false,
                    LocalDensity provides Density(LocalDensity.current.density, scale),
                ) {
                    com.kidslanguage.companion.feature.splash.SplashScreen({})
                }
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Mascot Idle").fetchSemanticsNodes().isNotEmpty()
        }
        saveNativePreview(preview)
        assertWholeText("Kids Language Companion")
        assertWholeText("听懂 · 开口 · 快乐学语言")
        val mascot = compose.onNodeWithContentDescription("Mascot Idle").fetchSemanticsNode().boundsInRoot
        assertTrue("Mascot lost its square layout: $mascot", kotlin.math.abs(mascot.width - mascot.height) < 2f)
    }

    private fun assertWholeText(text: String) {
        val node = compose.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertEquals("Missing characters: $text", text.length, layout.getLineEnd(layout.lineCount - 1))
        for (line in 0 until layout.lineCount) {
            assertFalse("Ellipsized: $text", layout.isLineEllipsized(line))
            assertTrue("Right edge clipped: $text", layout.getLineRight(line) <= layout.size.width + 1f)
            assertTrue("Left edge clipped: $text", layout.getLineLeft(line) >= -1f)
            assertTrue("Bottom clipped: $text", layout.getLineBottom(line) <= layout.size.height + 1f)
        }
    }

    private fun saveNativePreview(name: String) {
        val screenshot = java.io.File("build/reports/ui/$name.png")
        checkNotNull(screenshot.parentFile).mkdirs()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            screenshot.outputStream().use {
                assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
            }
            bitmap.recycle()
        }
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
