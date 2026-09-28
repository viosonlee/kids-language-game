package com.kidslanguage.companion.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import java.io.InputStream

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
            // First attempt to load webp, fallback to png
            val stream: InputStream = try {
                context.assets.open("mascot/webp/$assetName.webp")
            } catch (_: Exception) {
                context.assets.open("mascot/png/$assetName.png")
            }
            val decoded = BitmapFactory.decodeStream(stream)
            bitmap = decoded?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Gentle animations based on mascot state
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_anim")
    
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = if (animate) -8f else 0f,
        targetValue = if (animate) 8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    MascotState.Great -> 500
                    MascotState.Speak -> 700
                    else -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_float"
    )

    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (animate && (state == MascotState.Great || state == MascotState.Praise)) 1.05f else 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_scale"
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
    }
}
