package com.kidslanguage.companion.feature.choose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.core.model.SessionStepState
import com.kidslanguage.companion.ui.components.ImageOptionCard
import com.kidslanguage.companion.ui.components.MascotView
import com.kidslanguage.companion.ui.components.ProgressIndicatorBar
import com.kidslanguage.companion.ui.components.SpeakerButton
import com.kidslanguage.companion.ui.theme.AppSpacing
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral50
import com.kidslanguage.companion.ui.theme.Neutral700
import com.kidslanguage.companion.ui.theme.Neutral900
import com.kidslanguage.companion.ui.theme.Primary100
import com.kidslanguage.companion.ui.theme.Primary600

@Composable
fun ChooseScreen(
    viewModel: ChooseViewModel,
    language: LanguageCode,
    categoryId: String?,
    onBack: () -> Unit,
    onSessionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LifecycleResumeEffect(viewModel, language, categoryId) {
        viewModel.loadSession(language, categoryId)
        onPauseOrDispose { viewModel.stopSession() }
    }

    LaunchedEffect(state.isSessionComplete) {
        if (state.isSessionComplete && viewModel.consumeSessionCompletion()) {
            onSessionComplete()
        }
    }

    val currentItem = state.currentItem

    if (state.contentError != null) {
        AlertDialog(
            onDismissRequest = onBack,
            text = { Text(state.contentError.orEmpty()) },
            confirmButton = { TextButton(onClick = onBack) { Text("返回选择场景") } }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Neutral50)
            .padding(AppSpacing.xl)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Primary100)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Primary600,
                    modifier = Modifier.size(36.dp)
                )
            }

            ProgressIndicatorBar(
                currentStep = state.currentIndex + 1,
                totalSteps = state.items.size,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppSpacing.xl)
            )

            SpeakerButton(
                onClick = { viewModel.playPrompt() },
                isPlaying = state.sessionState == SessionStepState.PromptPlaying,
                size = 76.dp
            )
        }

        // Center Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Prompt Title / Prompt Text
            if (currentItem != null && state.showText) {
                Text(
                    text = currentItem.text,
                    style = if (state.smallText) AppTypography.bodyMedium else AppTypography.titleLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Primary600
                )
                if (!currentItem.reading.isNullOrBlank() && currentItem.reading != currentItem.text) {
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = currentItem.reading,
                        style = if (state.smallText) AppTypography.labelMedium else AppTypography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Neutral700
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.xl))
            } else {
                Text(
                    text = "听声音，选出正确的图片吧！",
                    style = AppTypography.titleMedium,
                    color = Neutral900
                )
                Spacer(modifier = Modifier.height(AppSpacing.xl))
            }

            // Options Row + Mascot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Keep image cards readable when four choices or larger system fonts need more rows.
                BoxWithConstraints(modifier = Modifier.weight(0.72f)) {
                    val minCardWidth = 160.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                    val columns = ((maxWidth + AppSpacing.lg) / (minCardWidth + AppSpacing.lg))
                        .toInt().coerceIn(1, state.currentOptions.size.coerceAtLeast(1))
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
                        state.currentOptions.chunked(columns).forEach { rowOptions ->
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
                                rowOptions.forEach { option ->
                                    val isSelected = state.selectedOptionId == option.id
                                    val isCorrect = if (isSelected) state.isAnswerCorrect else null

                                    ImageOptionCard(
                                        title = if (state.showText) option.title else "",
                                        emoji = option.emoji,
                                        smallText = state.smallText,
                                        highlighted = state.showAnswerHint && option.isCorrect,
                                        isSelected = isSelected || (state.showAnswerHint && option.isCorrect),
                                        isCorrect = isCorrect,
                                        onClick = { viewModel.onOptionSelected(option) },
                                        modifier = Modifier.weight(1f).heightIn(min = 220.dp)
                                    )
                                }
                                repeat(columns - rowOptions.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                // Mascot Reaction
                Column(
                    modifier = Modifier
                        .weight(0.28f)
                        .padding(start = AppSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    MascotView(
                        state = state.mascotState,
                        size = 240.dp
                    )
                }
            }

            if (state.isAnswerCorrect == true && !state.autoNext && !state.feedbackPlaying) {
                Spacer(modifier = Modifier.height(AppSpacing.md))
                FilledIconButton(onClick = viewModel::nextQuestion, modifier = Modifier.size(88.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一题", modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}
