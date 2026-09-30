package com.kidslanguage.companion

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.kidslanguage.companion.ui.theme.LocalAnimationsEnabled
import com.kidslanguage.companion.ui.theme.loopingFloat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [29])
@OptIn(ExperimentalCoroutinesApi::class)
class MotionTest {
    private class NoUiApplier : AbstractApplier<Unit>(Unit) {
        override fun insertBottomUp(index: Int, instance: Unit) {}
        override fun insertTopDown(index: Int, instance: Unit) {}
        override fun move(from: Int, to: Int, count: Int) {}
        override fun remove(index: Int, count: Int) {}
        override fun onClear() {}
    }

    @Test fun preferenceStopsRunningMotionAndRestoresItWithoutRemountingContent() = runTest {
        val clock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + clock)
        val composition = Composition(NoUiApplier(), recomposer)
        val runner = launch(clock) { recomposer.runRecomposeAndApplyChanges() }
        val enabled = mutableStateOf(false)
        var offset = Float.NaN
        var scale = Float.NaN
        suspend fun frame(milliseconds: Long) {
            runCurrent()
            clock.sendFrame(milliseconds * 1_000_000)
            runCurrent()
        }
        try {
            composition.setContent {
                CompositionLocalProvider(LocalAnimationsEnabled provides enabled.value) {
                    offset = loopingFloat(-8f, 8f, restingValue = 0f, durationMillis = 800, label = "float")
                    scale = loopingFloat(1f, 1.05f, durationMillis = 800, label = "scale")
                }
            }
            frame(0)
            assertEquals(0f, offset, 0f)
            assertEquals(1f, scale, 0f)
            assertFalse(clock.hasAwaiters)

            Snapshot.withMutableSnapshot { enabled.value = true }
            frame(16)
            frame(32)
            frame(432)
            frame(448)
            assertTrue(scale > 1f)
            assertTrue(clock.hasAwaiters)

            Snapshot.withMutableSnapshot { enabled.value = false }
            frame(464)
            frame(480)
            assertEquals(0f, offset, 0f)
            assertEquals(1f, scale, 0f)
            assertFalse(clock.hasAwaiters)
            frame(2_000)
            assertEquals(0f, offset, 0f)
            assertEquals(1f, scale, 0f)
        } finally {
            composition.dispose()
            recomposer.cancel()
            runner.join()
        }
    }
}
