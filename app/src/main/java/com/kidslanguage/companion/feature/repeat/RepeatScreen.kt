package com.kidslanguage.companion.feature.repeat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslanguage.companion.core.asr.RecognitionResult
import com.kidslanguage.companion.core.model.EvaluationStatus
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.SessionStepState
import com.kidslanguage.companion.ui.components.BigMicButton
import com.kidslanguage.companion.ui.components.FeedbackOverlay
import com.kidslanguage.companion.ui.components.MascotView
import com.kidslanguage.companion.ui.components.ProgressIndicatorBar
import com.kidslanguage.companion.ui.components.SpeakerButton
import com.kidslanguage.companion.ui.theme.AppRadius
import com.kidslanguage.companion.ui.theme.AppSpacing
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral0
import com.kidslanguage.companion.ui.theme.Neutral300
import com.kidslanguage.companion.ui.theme.Neutral50
import com.kidslanguage.companion.ui.theme.Neutral700
import com.kidslanguage.companion.ui.theme.Neutral900
import com.kidslanguage.companion.ui.theme.Primary100
import com.kidslanguage.companion.ui.theme.Primary600

@Composable
fun RepeatScreen(
    viewModel: RepeatViewModel,
    language: LanguageCode,
    categoryId: String?,
    onBack: () -> Unit,
    onSessionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(language, categoryId) {
        viewModel.loadSession(language, categoryId)
    }

    LaunchedEffect(state.isSessionComplete) {
        if (state.isSessionComplete) {
            onSessionComplete()
        }
    }

    val currentItem = state.currentItem

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
                onClick = { viewModel.playCurrentPrompt() },
                isPlaying = state.sessionState == SessionStepState.PromptPlaying,
                size = 76.dp
            )
        }

        // Center Content Area: Scene Card (Left) + Mascot (Right)
        if (currentItem != null) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, bottom = 120.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scene Card
                Box(
                    modifier = Modifier
                        .weight(0.68f)
                        .shadow(6.dp, RoundedCornerShape(AppRadius.card))
                        .clip(RoundedCornerShape(AppRadius.card))
                        .background(Neutral0)
                        .border(2.dp, Neutral300, RoundedCornerShape(AppRadius.card))
                        .padding(AppSpacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentItem.image ?: "⭐",
                            fontSize = 110.sp
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.md))

                        if (state.showText) {
                            Text(
                                text = currentItem.text,
                                style = AppTypography.displayLarge,
                                color = Neutral900
                            )
                            if (!currentItem.reading.isNullOrBlank() && currentItem.reading != currentItem.text) {
                                Spacer(modifier = Modifier.height(AppSpacing.xs))
                                Text(
                                    text = currentItem.reading,
                                    style = AppTypography.bodyMedium,
                                    color = Neutral700
                                )
                            }
                        }
                    }
                }

                // Mascot on the Right
                Column(
                    modifier = Modifier
                        .weight(0.32f)
                        .padding(start = AppSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    MascotView(
                        state = state.mascotState,
                        size = 280.dp
                    )
                }
            }
        }

        // Bottom Action Area: Big Mic Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AppSpacing.xs),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BigMicButton(
                    isRecording = state.isRecording,
                    onClick = { viewModel.toggleRecording() },
                    size = 130.dp
                )

                // Quick tap simulation helper for devices/testing without real mic
                if (currentItem != null) {
                    Spacer(modifier = Modifier.width(AppSpacing.lg))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Primary100)
                            .clickable {
                                viewModel.processRecognitionResult(
                                    RecognitionResult(text = currentItem.text, isSuccess = true)
                                )
                            }
                            .padding(horizontal = AppSpacing.normal, vertical = AppSpacing.sm)
                    ) {
                        Text(
                            text = "✨ 示范说出",
                            style = AppTypography.labelMedium,
                            color = Primary600
                        )
                    }
                }
            }
        }

        // Feedback Overlay
        FeedbackOverlay(
            status = state.evaluationResult?.status ?: EvaluationStatus.Retry,
            visible = state.showFeedbackOverlay,
            onDismiss = { viewModel.dismissFeedback() }
        )
    }
}
