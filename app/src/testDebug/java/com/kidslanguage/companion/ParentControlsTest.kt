package com.kidslanguage.companion

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kidslanguage.companion.core.persistence.*
import com.kidslanguage.companion.feature.parent.*
import com.kidslanguage.companion.ui.theme.KidsLanguageTheme
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class, qualifiers = "w1000dp-h700dp-land-mdpi")
class ParentControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun resettingRequiresConfirmationAndCancelKeepsRecords() {
        var resets = 0
        val state = mutableStateOf<ProgressResetState>(ProgressResetState.Idle)
        compose.setContent {
            KidsLanguageTheme {
                // Match the screen's fixed root bounds while the status adds/removes text.
                Box(Modifier.fillMaxSize()) {
                    ProgressResetControl(state.value) { resets++; state.value = ProgressResetState.Running }
                }
            }
        }
        compose.onNodeWithText("清空练习记录").performClick()
        compose.onNodeWithText("清空练习记录？").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, resets) }
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("清空练习记录？").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, resets) }
        compose.onNodeWithText("清空练习记录").performClick()
        compose.onNodeWithText("清空记录").performClick()
        compose.onNodeWithText("正在清空…").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("清空练习记录？").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, resets)
            // Commit the simulated asynchronous completion before waiting for recomposition.
            Snapshot.withMutableSnapshot { state.value = ProgressResetState.Complete }
        }
        compose.onNodeWithText("清空操作已完成").assertIsDisplayed()
        compose.onNodeWithText("清空练习记录").assertIsEnabled()
    }

    @Test fun failedResetShowsErrorAndRetryStillRequiresConfirmation() {
        var resets = 0
        val message = "练习明细已清空，但旧版本累计数未能清空，请重试。"
        compose.setContent { KidsLanguageTheme { ProgressResetControl(ProgressResetState.Failed(message)) { resets++ } } }
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("清空操作已完成").assertDoesNotExist()
        compose.onNodeWithText("清空练习记录").performClick()
        compose.runOnIdle { assertEquals(0, resets) }
        compose.onNodeWithText("清空记录").performClick()
        compose.runOnIdle { assertEquals(1, resets) }
    }

    @Test fun parentGateRejectsWrongAnswerThenAcceptsCurrentAnswer() {
        val store = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
                transform(data.value).also { data.value = it }
        }
        val progress = object : ProgressRepository {
            override val totals = MutableStateFlow(ProgressTotals())
            override val today = totals
            override val languages = MutableStateFlow(emptyList<LanguageProgress>())
            override val error = MutableStateFlow<String?>(null)
            override fun record(attempt: PracticeAttemptEntity) {}
            override suspend fun clear() {}
        }
        val vm = ParentViewModel(SettingsRepository(store), progress)
        var entered = 0
        var dismissed = 0
        try {
            compose.setContent { KidsLanguageTheme { ParentGateDialog(vm, { entered++ }, { dismissed++ }) } }
            compose.onNodeWithText("请输入答案").performTextInput("0")
            compose.onNodeWithText("确定").performClick()
            compose.onNodeWithText("答案不正确，请重试").assertIsDisplayed()
            compose.runOnIdle { assertEquals(0, entered) }
            compose.onNodeWithText("请输入答案").performTextInput(vm.gate.value.answer.toString())
            compose.onNodeWithText("确定").performClick()
            compose.runOnIdle { assertEquals(1, entered); assertEquals(0, dismissed) }
        } finally { vm.viewModelScope.cancel() }
    }
}
