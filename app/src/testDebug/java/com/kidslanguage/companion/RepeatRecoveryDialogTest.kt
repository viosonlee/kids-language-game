package com.kidslanguage.companion

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.kidslanguage.companion.ui.components.PracticeIllustration
import com.kidslanguage.companion.feature.repeat.RepeatProblem
import com.kidslanguage.companion.feature.repeat.RepeatRecoveryDialog
import com.kidslanguage.companion.ui.theme.KidsLanguageTheme
import com.kidslanguage.companion.ui.theme.LocalAnimationsEnabled
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class, qualifiers = "w1000dp-h700dp-land-mdpi")
class RepeatRecoveryDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun permissionProblemOffersPicturePracticeAndParentHelpWithoutSystemSettingsLink() {
        var choices = 0
        var parent = 0
        compose.setContent {
            KidsLanguageTheme {
                CompositionLocalProvider(LocalAnimationsEnabled provides false) {
                    RepeatRecoveryDialog(RepeatProblem.PERMISSION, onDismiss = {}, onBrowse = {}, onParent = { parent++ },
                        onRetry = {}, onChoose = { choices++ }, onReplay = {})
                }
            }
        }
        compose.onNodeWithText("去系统设置开启").assertDoesNotExist()
        compose.onNodeWithText("玩选图").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("请家长帮忙").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, choices)
            assertEquals(1, parent)
        }
    }

    @Test fun modelProblemKeepsReplayAndRetryAvailable() {
        var replays = 0
        var retries = 0
        compose.setContent {
            KidsLanguageTheme {
                CompositionLocalProvider(LocalAnimationsEnabled provides false) {
                    RepeatRecoveryDialog(RepeatProblem.MODEL, onDismiss = {}, onBrowse = {}, onParent = {},
                        onRetry = { retries++ }, onChoose = null, onReplay = { replays++ })
                }
            }
        }
        compose.onNodeWithText("玩选图").assertDoesNotExist()
        compose.onNodeWithContentDescription("Speaker").assertIsDisplayed().performClick()
        compose.onNodeWithText("再试一次").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, replays)
            assertEquals(1, retries)
        }
    }

    @Test fun emptyContentOffersBrowseWithoutDeadRetryOrChoiceActions() {
        var browsed = 0
        compose.setContent {
            KidsLanguageTheme {
                CompositionLocalProvider(LocalAnimationsEnabled provides false) {
                    RepeatRecoveryDialog(RepeatProblem.CONTENT, onDismiss = {}, onBrowse = { browsed++ }, onParent = {},
                        onRetry = null, onChoose = null, onReplay = null)
                }
            }
        }
        compose.onNodeWithText("玩选图").assertDoesNotExist()
        compose.onNodeWithText("再试一次").assertDoesNotExist()
        compose.onNodeWithContentDescription("Speaker").assertDoesNotExist()
        compose.onNodeWithContentDescription("换个场景").performClick()
        compose.runOnIdle { assertEquals(1, browsed) }
    }

    @Test fun localImageReferenceRendersAnImageInsteadOfTheFilePath() {
        val path = "mascot/png/bunny_idle.png"
        compose.setContent { PracticeIllustration(path, size = 96.dp) }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("练习图片").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("练习图片").assertIsDisplayed()
        compose.onNodeWithText(path).assertDoesNotExist()
    }
}
