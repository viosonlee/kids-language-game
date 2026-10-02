package com.kidslanguage.companion

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kidslanguage.companion.core.persistence.ProgressTotals
import com.kidslanguage.companion.feature.parent.ProgressReadState
import com.kidslanguage.companion.feature.parent.ProgressSummary
import com.kidslanguage.companion.ui.theme.KidsLanguageTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class, qualifiers = "w1000dp-h700dp-land-mdpi")
class StorageRecoveryControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failedStatisticsOfferRetryWithoutShowingFalseZeroCounts() {
        var retries = 0
        val message = "练习记录暂时无法读取，请重试。"
        compose.setContent {
            KidsLanguageTheme { Box(Modifier.fillMaxSize()) {
                ProgressSummary(ProgressReadState.Failed(message)) { retries++ }
            } }
        }
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("累计完成题目数", substring = true).assertDoesNotExist()
        compose.onNodeWithText("暂无记录", substring = true).assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, retries) }
        compose.onNodeWithText("重试").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun migratedLegacyCountIsIncludedOnlyInAllTimeTotal() {
        compose.setContent {
            KidsLanguageTheme { Box(Modifier.fillMaxSize()) {
                ProgressSummary(ProgressReadState.Ready(ProgressTotals(completed = 12, legacyCompleted = 17), ProgressTotals(), emptyList())) {}
            } }
        }
        compose.onNodeWithText("累计完成题目数：29 题").assertIsDisplayed()
        compose.onNodeWithText("今日练习：0 题 · 完成：0 题").assertIsDisplayed()
        compose.onNodeWithText("含旧版本历史累计 17 题", substring = true).assertIsDisplayed()
        compose.onNodeWithText("重试").assertDoesNotExist()
    }

    @Test fun initialLoadingDoesNotLookLikeAnEmptyDatabase() {
        compose.setContent {
            KidsLanguageTheme { ProgressSummary(ProgressReadState.Loading) {} }
        }
        compose.onNodeWithText("正在读取练习记录…").assertIsDisplayed()
        compose.onNodeWithText("累计完成题目数", substring = true).assertDoesNotExist()
    }
}
