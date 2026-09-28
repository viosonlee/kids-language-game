package com.kidslanguage.companion.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kidslanguage.companion.ui.theme.AccentOrange
import com.kidslanguage.companion.ui.theme.AccentYellow
import com.kidslanguage.companion.ui.theme.AppRadius
import com.kidslanguage.companion.ui.theme.AppSpacing
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral0
import com.kidslanguage.companion.ui.theme.Neutral300
import com.kidslanguage.companion.ui.theme.Neutral700
import com.kidslanguage.companion.ui.theme.Neutral900
import com.kidslanguage.companion.ui.theme.Primary100
import com.kidslanguage.companion.ui.theme.Primary500
import com.kidslanguage.companion.ui.theme.Primary600

@Composable
fun PrimaryLargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 80.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(height)
            .shadow(8.dp, RoundedCornerShape(AppRadius.button))
            .clip(RoundedCornerShape(AppRadius.button))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (enabled) listOf(Primary500, Primary600) else listOf(Neutral300, Neutral300)
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Neutral0),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.normal),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Neutral0,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.md))
            }
            Text(
                text = text,
                style = AppTypography.labelLarge,
                color = Neutral0
            )
        }
    }
}

@Composable
fun SecondaryLargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 72.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(height)
            .shadow(4.dp, RoundedCornerShape(AppRadius.button))
            .clip(RoundedCornerShape(AppRadius.button))
            .background(Neutral0)
            .border(3.dp, Primary500, RoundedCornerShape(AppRadius.button))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Primary100),
                onClick = onClick
            )
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary600,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
            }
            Text(
                text = text,
                style = AppTypography.titleMedium,
                color = Primary600
            )
        }
    }
}

@Composable
fun SpeakerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    size: Dp = 96.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speaker_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaker_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPlaying) listOf(AccentYellow, AccentOrange) else listOf(Primary500, Primary600)
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "Speaker",
            tint = if (isPlaying) Neutral900 else Neutral0,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
fun BigMicButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 500 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isRecording) 0.05f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 600 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_alpha"
    )

    Box(
        modifier = modifier.size(size + 32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size((size + 24.dp) * pulseScale)
                .clip(CircleShape)
                .background(if (isRecording) AccentOrange.copy(alpha = waveAlpha) else Primary100.copy(alpha = waveAlpha))
        )

        // Core Mic Button
        Box(
            modifier = Modifier
                .size(size)
                .scale(if (isRecording) pulseScale else 1f)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isRecording) listOf(AccentOrange, Color(0xFFFF5722)) else listOf(Primary500, Primary600)
                    )
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isRecording) "Recording" else "Tap to Speak",
                tint = Neutral0,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}
