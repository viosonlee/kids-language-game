package com.kidslanguage.companion

import android.app.Application
import android.os.SystemClock
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.kidslanguage.companion.ui.components.debouncedClickable
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class)
class DebounceClickTest {
    @get:Rule val compose = createComposeRule()

    @Test fun defaultClickableAcceptsFirstTapThenDebounces() = checkTapSequence(false)

    @Test fun customInteractionClickableAcceptsFirstTapThenDebounces() = checkTapSequence(true)

    private fun checkTapSequence(customInteraction: Boolean) {
        var clicks = 0
        // Longer than this test's uptime, so the first tap cannot accidentally pass
        // merely because the clock has already exceeded the debounce interval.
        val interval = SystemClock.uptimeMillis() + 60_000L
        compose.setContent {
            val source = if (customInteraction) remember { MutableInteractionSource() } else null
            Box(Modifier.size(88.dp).testTag("button").debouncedClickable(
                debounceMillis = interval,
                interactionSource = source,
                onClick = { clicks++ },
            ))
        }
        compose.runOnIdle { assertTrue(SystemClock.uptimeMillis() < interval) }
        compose.onNodeWithTag("button").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
        compose.onNodeWithTag("button").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
        ShadowSystemClock.advanceBy(Duration.ofMillis(interval))
        compose.onNodeWithTag("button").performClick()
        compose.runOnIdle { assertEquals(2, clicks) }
    }
}
