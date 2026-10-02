package com.kidslanguage.companion.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.ui.theme.LocalAnimationsEnabled
import com.kidslanguage.companion.ui.theme.loopingFloat
import com.kidslanguage.companion.ui.theme.celebrationProgress
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MascotView(
    state: MascotState,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    animate: Boolean = true
) {
    val context = LocalContext.current
    var bitmap by remember(state) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(state) {
        val assetName = when (state) {
            MascotState.Idle -> "bunny_idle"
            MascotState.Listen -> "bunny_listen"
            MascotState.Speak -> "bunny_speak"
            MascotState.Great -> "bunny_great"
            MascotState.TryAgain -> "bunny_try_again"
            MascotState.Praise -> "bunny_praise"
        }

        try {
            val decoded = withContext(Dispatchers.IO) {
                context.assets.open("mascot/png/$assetName.png").use { stream -> BitmapFactory.decodeStream(stream) }
            }
            bitmap = decoded?.asImageBitmap()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Gentle animations based on mascot state
    val motionEnabled = animate && LocalAnimationsEnabled.current
    val celebrating = state == MascotState.Great || state == MascotState.Praise
    val progress = if (celebrating && bitmap != null) celebrationProgress(state, motionEnabled) else 1f
    val bounce = if (progress <= 0f || progress >= 1f) 0f else sin(PI * progress).toFloat()
    val floatOffset = if (celebrating) -8f * bounce else loopingFloat(
        initialValue = -8f, targetValue = 8f, restingValue = 0f,
        durationMillis = when (state) {
            MascotState.Speak -> 700
            else -> 1800
        },
        label = "mascot_float", enabled = motionEnabled,
    )

    val scaleFactor = if (celebrating) 1f + 0.05f * bounce else loopingFloat(
        initialValue = 1f,
        targetValue = 1.01f,
        durationMillis = 800, easing = LinearEasing,
        label = "mascot_scale", enabled = motionEnabled,
    )

    Box(
        modifier = modifier
            .size(size)
            .offset(y = floatOffset.dp)
            .scale(scaleFactor),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { img ->
            Image(
                bitmap = img,
                contentDescription = "Mascot $state",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size)
            )
        }
        if (celebrating && motionEnabled && bitmap != null) {
            SuccessStars(progress, Modifier.matchParentSize())
        }
    }
}
