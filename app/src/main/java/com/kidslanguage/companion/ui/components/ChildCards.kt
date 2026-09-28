package com.kidslanguage.companion.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslanguage.companion.core.model.EvaluationStatus
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.ui.theme.AccentGreen
import com.kidslanguage.companion.ui.theme.AccentOrange
import com.kidslanguage.companion.ui.theme.AccentPurple
import com.kidslanguage.companion.ui.theme.AccentYellow
import com.kidslanguage.companion.ui.theme.AppRadius
import com.kidslanguage.companion.ui.theme.AppSpacing
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral0
import com.kidslanguage.companion.ui.theme.Neutral100
import com.kidslanguage.companion.ui.theme.Neutral300
import com.kidslanguage.companion.ui.theme.Neutral700
import com.kidslanguage.companion.ui.theme.Neutral900
import com.kidslanguage.companion.ui.theme.Primary100
import com.kidslanguage.companion.ui.theme.Primary500
import com.kidslanguage.companion.ui.theme.Primary600
import com.kidslanguage.companion.ui.theme.RetryBg
import com.kidslanguage.companion.ui.theme.SuccessBg

@Composable
fun LanguageCard(
    language: LanguageCode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Primary500 else Neutral300
    val borderWidth = if (isSelected) 4.dp else 2.dp
    val bgColor = if (isSelected) Primary100 else Neutral0

    Box(
        modifier = modifier
            .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(AppRadius.card))
            .clip(RoundedCornerShape(AppRadius.card))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(AppRadius.card))
            .debouncedClickable(onClick = onClick)
            .padding(vertical = AppSpacing.lg, horizontal = AppSpacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Flag or localized mark
            val flagEmoji = when (language) {
                LanguageCode.JA_JP -> "\uD83C\uDDEF\uD83C\uDDF5"
                LanguageCode.ZH_CN -> "\uD83C\uDDE8\uD83C\uDDF3"
                LanguageCode.EN_US -> "\uD83C\uDDFA\uD83C\uDDF8"
            }
            Text(text = flagEmoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(
                text = language.displayName,
                style = AppTypography.titleMedium,
                color = if (isSelected) Primary600 else Neutral900
            )
            Text(
                text = language.englishName,
                style = AppTypography.bodyMedium,
                color = Neutral700
            )
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    iconEmoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Neutral0
) {
    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(AppRadius.card))
            .clip(RoundedCornerShape(AppRadius.card))
            .background(backgroundColor)
            .border(2.dp, Neutral300, RoundedCornerShape(AppRadius.card))
            .debouncedClickable(onClick = onClick)
            .padding(AppSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = iconEmoji, fontSize = 56.sp)
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Text(
                text = title,
                style = AppTypography.titleMedium,
                color = Neutral900
            )
        }
    }
}

@Composable
fun ImageOptionCard(
    title: String,
    emoji: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when (isCorrect) {
        true -> AccentGreen
        false -> AccentPurple
        null -> if (isSelected) Primary500 else Neutral300
    }
    val borderWidth = if (isCorrect != null || isSelected) 5.dp else 2.dp

    Box(
        modifier = modifier
            .shadow(if (isSelected) 10.dp else 4.dp, RoundedCornerShape(AppRadius.optionCard))
            .clip(RoundedCornerShape(AppRadius.optionCard))
            .background(Neutral0)
            .border(borderWidth, borderColor, RoundedCornerShape(AppRadius.optionCard))
            .debouncedClickable(onClick = onClick)
            .padding(AppSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 72.sp)
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(
                text = title,
                style = AppTypography.bodyLarge,
                color = Neutral900
            )
        }
    }
}

@Composable
fun ProgressIndicatorBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSteps > 0) currentStep.toFloat() / totalSteps.toFloat() else 0f

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp)),
            color = AccentYellow,
            trackColor = Primary100,
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.width(AppSpacing.normal))
        Text(
            text = "$currentStep / $totalSteps",
            style = AppTypography.labelLarge,
            color = Primary600
        )
    }
}

@Composable
fun FeedbackOverlay(
    status: EvaluationStatus,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        val isSuccess = status == EvaluationStatus.Pass || status == EvaluationStatus.SoftPass
        val bgColor = if (isSuccess) SuccessBg else RetryBg
        val mascotState = if (isSuccess) MascotState.Great else MascotState.TryAgain

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(520.dp)
                    .clip(RoundedCornerShape(AppRadius.dialog))
                    .background(bgColor)
                    .border(4.dp, if (isSuccess) AccentYellow else AccentPurple, RoundedCornerShape(AppRadius.dialog))
                    .padding(AppSpacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    MascotView(state = mascotState, size = 180.dp)
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    Text(
                        text = if (isSuccess) "太棒啦！\uD83C\uDF1F" else "再试试看哦！\uD83D\uDC30",
                        style = AppTypography.displayLarge,
                        color = Neutral900
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = if (isSuccess) "泡泡说得非常好！" else "不要着急，泡泡，跟着兔子再听一次吧",
                        style = AppTypography.bodyMedium,
                        color = Neutral700
                    )
                }
            }
        }
    }
}
