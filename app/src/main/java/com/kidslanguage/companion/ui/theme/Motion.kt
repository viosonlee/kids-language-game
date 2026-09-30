package com.kidslanguage.companion.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAnimationsEnabled = staticCompositionLocalOf { true }

/** Disabled decorative motion has no running transition and immediately uses its resting value. */
@Composable
internal fun loopingFloat(
    initialValue: Float,
    targetValue: Float,
    restingValue: Float = initialValue,
    durationMillis: Int,
    label: String,
    easing: Easing = FastOutSlowInEasing,
    enabled: Boolean = LocalAnimationsEnabled.current,
): Float {
    if (!enabled || initialValue == targetValue) return restingValue
    val transition = rememberInfiniteTransition(label = label)
    val value by transition.animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = easing), RepeatMode.Reverse),
        label = label,
    )
    return value
}
