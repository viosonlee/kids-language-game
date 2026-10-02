package com.kidslanguage.companion.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAnimationsEnabled = staticCompositionLocalOf { true }

/** A brief celebration has a finite lifetime; disabling motion immediately returns the resting pose. */
@Composable
internal fun celebrationProgress(key: Any, enabled: Boolean): Float {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key, enabled) {
        if (enabled) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(700, easing = LinearEasing))
        } else progress.snapTo(1f)
    }
    return if (enabled) progress.value else 1f
}

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
