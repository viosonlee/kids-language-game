package com.kidslanguage.companion

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.ui.components.MascotView
import com.kidslanguage.companion.ui.theme.LocalAnimationsEnabled
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class, qualifiers = "w800dp-h600dp-land-mdpi")
class MascotMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun realMascotBouncesOnceAndDisabledMotionRestoresItsBounds() {
        val enabled = mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalAnimationsEnabled provides enabled.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MascotView(MascotState.Great, size = 200.dp)
                }
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.mainClock.advanceTimeByFrame()
            compose.onAllNodesWithContentDescription("Mascot Great").fetchSemanticsNodes().size == 1
        }
        val mascot = compose.onNodeWithContentDescription("Mascot Great")
        val resting = mascot.fetchSemanticsNode().boundsInRoot
        assertEquals(200f, resting.width, 0.1f)

        compose.runOnUiThread { Snapshot.withMutableSnapshot { enabled.value = true } }
        compose.mainClock.advanceTimeBy(380)
        val bouncing = mascot.fetchSemanticsNode().boundsInRoot
        assertTrue(bouncing.width > resting.width * 1.03f)
        assertTrue(bouncing.center.y < resting.center.y)
        compose.mainClock.advanceTimeBy(500)
        assertEquals(resting, mascot.fetchSemanticsNode().boundsInRoot)
        compose.mainClock.advanceTimeBy(2_000)
        assertEquals(resting, mascot.fetchSemanticsNode().boundsInRoot)

        compose.runOnUiThread { Snapshot.withMutableSnapshot { enabled.value = false } }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnUiThread { Snapshot.withMutableSnapshot { enabled.value = true } }
        compose.mainClock.advanceTimeBy(350)
        assertTrue(mascot.fetchSemanticsNode().boundsInRoot.width > resting.width)
        compose.runOnUiThread { Snapshot.withMutableSnapshot { enabled.value = false } }
        compose.mainClock.advanceTimeByFrame()
        assertEquals(resting, mascot.fetchSemanticsNode().boundsInRoot)
    }
}
