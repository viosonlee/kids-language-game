package com.kidslanguage.companion.ui.components

import android.os.SystemClock
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Debounced clickable modifier tailored for preschooler tablet interactions.
 * Prevents rapid accidental double-taps or palm mashing (default 400ms interval).
 */
fun Modifier.debouncedClickable(
    debounceMillis: Long = 400L,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onClick: () -> Unit
): Modifier = composed {
    // A fresh control must accept a tap even before the device's first debounce interval.
    var lastClickTime by remember { mutableLongStateOf(Long.MIN_VALUE) }

    if (interactionSource != null) {
        clickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled
        ) {
            val currentTime = SystemClock.uptimeMillis()
            if (lastClickTime == Long.MIN_VALUE || currentTime - lastClickTime >= debounceMillis) {
                lastClickTime = currentTime
                onClick()
            }
        }
    } else {
        clickable(enabled = enabled) {
            val currentTime = SystemClock.uptimeMillis()
            if (lastClickTime == Long.MIN_VALUE || currentTime - lastClickTime >= debounceMillis) {
                lastClickTime = currentTime
                onClick()
            }
        }
    }
}
