package com.kidslanguage.companion.feature.repeat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
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
    var showPermissionHint by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecordingIfReady()
        } else {
            showPermissionHint = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Neutral50)
    ) {
        Box(Modifier.fillMaxSize().padding(AppSpacing.xl)) {
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
                                    style = if (state.smallText) AppTypography.bodyMedium else AppTypography.displayLarge,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = Neutral900
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
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                viewModel.toggleRecording()
                            }
                        },
                        size = 130.dp
                    )

                }
            }

        }

        if (state.sessionState == SessionStepState.Loading) {
            Column(
                modifier = Modifier.align(Alignment.Center).background(Neutral50).padding(AppSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(AppSpacing.md))
                Text("正在准备语音，首次使用请稍等…")
            }
        }

        // Feedback Overlay
        FeedbackOverlay(
            status = state.evaluationResult?.status ?: EvaluationStatus.Retry,
            visible = state.showFeedbackOverlay,
            onDismiss = { viewModel.dismissFeedback() },
            onNext = if (state.sessionState == SessionStepState.Passed && !state.autoNext &&
                !state.feedbackPlaying) viewModel::nextItem else null,
            modifier = Modifier.zIndex(100f)
        )

        if (state.recordingError != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissRecordingError,
                title = { Text("再试一次吧") },
                text = { Text(state.recordingError.orEmpty()) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissRecordingError) { Text("知道了") }
                }
            )
        }

        if (showPermissionHint) {
            AlertDialog(
                onDismissRequest = { showPermissionHint = false },
                title = {
                    Text("🐰 兔兔需要麦克风权限哦", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Momo Bunny 需要听到泡泡的声音才能判断发音呢！请允许麦克风权限，或者先去玩有趣的【选图模式】吧！")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionHint = false
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                onBack()
                            }
                        }
                    ) {
                        Text("去系统设置开启")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionHint = false }) {
                        Text("知道了")
                    }
                },
                shape = RoundedCornerShape(AppRadius.card)
            )
        }
    }
}
