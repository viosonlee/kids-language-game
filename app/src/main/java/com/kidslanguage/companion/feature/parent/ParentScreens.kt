package com.kidslanguage.companion.feature.parent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.kidslanguage.companion.core.persistence.UserSettings
import com.kidslanguage.companion.ui.components.StorageNotice
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.feature.update.AppUpdateCard
import com.kidslanguage.companion.ui.theme.AppRadius
import com.kidslanguage.companion.ui.theme.AppSpacing
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral0
import com.kidslanguage.companion.ui.theme.Neutral50
import com.kidslanguage.companion.ui.theme.Neutral700
import com.kidslanguage.companion.ui.theme.Neutral900
import com.kidslanguage.companion.ui.theme.Primary100
import com.kidslanguage.companion.ui.theme.Primary800
import com.kidslanguage.companion.ui.theme.Primary600

@Composable
fun ParentGateDialog(
    viewModel: ParentViewModel,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val gate by viewModel.gate.collectAsState()
    var answerText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "家长验证", style = AppTypography.titleMedium)
        },
        text = {
            Column {
                Text(
                    text = "请回答算术题进入家长设置：",
                    style = AppTypography.bodyMedium,
                    color = Neutral700
                )
                Spacer(modifier = Modifier.height(AppSpacing.md))
                Text(
                    text = "${gate.num1} × ${gate.num2} = ?",
                    style = AppTypography.titleLarge,
                    color = Primary800
                )
                Spacer(modifier = Modifier.height(AppSpacing.md))
                OutlinedTextField(
                    value = answerText,
                    onValueChange = {
                        answerText = it
                        isError = false
                    },
                    label = { Text("请输入答案") },
                    isError = isError,
                    singleLine = true
                )
                if (isError) {
                    Text(
                        text = "答案不正确，请重试",
                        color = androidx.compose.ui.graphics.Color.Red,
                        style = AppTypography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (viewModel.verifyGate(answerText)) {
                        onSuccess()
                    } else {
                        isError = true
                        viewModel.regenerateGate()
                        answerText = ""
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentSettingsScreen(
    viewModel: ParentViewModel,
    onBack: () -> Unit,
    onOpenDevLab: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settingsSnapshot by viewModel.settings.collectAsState()
    val savedSettings = settingsSnapshot?.value
    val settings = savedSettings ?: UserSettings(soundVolume = 0f, animationEnabled = false)
    val settingsReadError = settingsSnapshot?.error
    val progress by viewModel.progress.collectAsState()
    val progressError by viewModel.progressError.collectAsState()
    val resetState by viewModel.resetState.collectAsState()
    val settingsError by viewModel.settingsError.collectAsState()
    val context = LocalContext.current
    var microphoneGranted by remember { mutableStateOf(false) }
    var permissionSettingsError by remember { mutableStateOf<String?>(null) }
    LifecycleResumeEffect(context) {
        microphoneGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        onPauseOrDispose { }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Neutral50)
            .padding(AppSpacing.xl)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Primary100)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Primary600,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(AppSpacing.lg))
                Text(
                    text = "家长管理与设置",
                    style = AppTypography.titleLarge,
                    color = Neutral900
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            // Setting Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.card),
                colors = CardDefaults.cardColors(containerColor = Neutral0)
            ) {
                Column(modifier = Modifier.padding(AppSpacing.xl)) {
                    Text(text = "学习设置", style = AppTypography.titleMedium, color = Primary800)
                    settingsReadError?.let { StorageNotice(it, viewModel::retrySettingsRead) }
                    if (savedSettings == null) Text("正在读取学习设置…", color = Neutral900)
                    settingsError?.let { Text(it, color = Neutral900) }
                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    Text(if (microphoneGranted) "麦克风权限：已允许" else "麦克风权限：未允许", style = AppTypography.bodyMedium)
                    Button(onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            })
                            permissionSettingsError = null
                        } catch (_: Exception) {
                            permissionSettingsError = "无法打开系统设置，请在设备的应用设置中检查麦克风权限。"
                        }
                    }) { Text("管理麦克风权限", maxLines = 1, softWrap = false) }
                    permissionSettingsError?.let { Text(it, color = Neutral700) }
                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    Text("默认语言", style = AppTypography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        LanguageCode.entries.forEach { language ->
                            val title = when (language) {
                                LanguageCode.JA_JP -> "日语"
                                LanguageCode.ZH_CN -> "中文"
                                LanguageCode.EN_US -> "英语"
                            }
                            Button(onClick = { viewModel.setLanguage(language) }) {
                                Text(if (settings.selectedLanguage == language) "✓ $title" else title, maxLines = 1)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("反馈语音与音效", style = AppTypography.bodyLarge)
                        Switch(checked = settings.soundVolume > 0f,
                            onCheckedChange = { viewModel.setSoundVolume(if (it) 1f else 0f) })
                    }
                    Text("音效音量：${(settings.soundVolume * 100).toInt()}%", color = Neutral700)
                    Slider(value = settings.soundVolume, onValueChange = viewModel::setSoundVolume)
                    Text("控制兔子招呼、表扬、重试和庆祝声音；示范发音仍由设备媒体音量控制。", color = Neutral700)
                    ParentToggle("首页背景音乐", "只在首页播放，兔子说话、进入练习或切到后台时停止。",
                        settings.musicEnabled, viewModel::setMusicEnabled)
                    ParentToggle("界面动效", "控制兔子浮动、按钮脉冲和页面淡入淡出；关闭后仍显示录音状态和反馈表情。",
                        settings.animationEnabled, viewModel::setAnimationEnabled)
                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    Text("练习文字", style = AppTypography.bodyLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        com.kidslanguage.companion.core.persistence.TextDisplayMode.entries.forEach { mode ->
                            Button(onClick = { viewModel.setTextDisplayMode(mode) }) {
                                Text(if (settings.textDisplayMode == mode) "✓ ${mode.title}" else mode.title,
                                    maxLines = 1, softWrap = false)
                            }
                        }
                    }
                    Text("辅助小字保留目标文字和读音；隐藏后仍可看图、听示范完成练习。", color = Neutral700)
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    ParentToggle("随机出题", "每轮重新排列题目；关闭后按课程顺序练习。",
                        settings.shuffleItems, viewModel::setShuffleItems)
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    Text("选图数量", style = AppTypography.bodyLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        (2..4).forEach { count ->
                            Button(onClick = { viewModel.setChoiceCount(count) }) {
                                Text(if (settings.choiceCount == count) "✓ $count 张" else "$count 张",
                                    maxLines = 1, softWrap = false)
                            }
                        }
                    }
                    Text("下一轮生效；候选图片不足时使用实际可用数量。", color = Neutral700)
                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    // Auto Next toggle
                    ParentToggle(
                        title = "自动播放示范",
                        description = "进入新题时自动播放；关闭后仍可点击喇叭听示范。",
                        checked = settings.autoPlayPrompt,
                        onCheckedChange = viewModel::setAutoPlayPrompt
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    ParentToggle(
                        title = "自动开始录音",
                        description = "跟读准备好后开始录音。首次请点击麦克风授权；无声或识别错误后需手动重试。",
                        checked = settings.autoStartRecording,
                        onCheckedChange = viewModel::setAutoStartRecording
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "跟读通过后自动跳下一题", style = AppTypography.bodyLarge)
                            Text(
                                text = "反馈语音结束后自动进入下一题，适用于跟读和选图",
                                style = AppTypography.bodyMedium,
                                color = Neutral700
                            )
                        }
                        Switch(
                            checked = settings.autoNext,
                            onCheckedChange = { viewModel.setAutoNext(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    // Session Length selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "每次练习题数", style = AppTypography.bodyLarge)
                            Text(
                                text = "当前：${settings.sessionLength} 题",
                                style = AppTypography.bodyMedium,
                                color = Neutral700
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                            listOf(5, 10, 15).forEach { len ->
                                Button(
                                    onClick = { viewModel.setSessionLength(len) },
                                    shape = RoundedCornerShape(AppRadius.button)
                                ) {
                                    Text("$len 题")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            // Progress & Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.card),
                colors = CardDefaults.cardColors(containerColor = Neutral0)
            ) {
                Column(modifier = Modifier.padding(AppSpacing.xl)) {
                    Text(text = "练习统计与数据", style = AppTypography.titleMedium, color = Primary800)
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    ProgressSummary(progress,
                        onRetry = viewModel::retryProgressRead)
                    progressError?.let { Text(it, color = Neutral900) }
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    ProgressResetControl(resetState, viewModel::resetProgress)
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            AppUpdateCard()
            Spacer(modifier = Modifier.height(AppSpacing.xl))

            // Developer / Diagnostic Lab Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.card),
                colors = CardDefaults.cardColors(containerColor = Neutral0)
            ) {
                Column(modifier = Modifier.padding(AppSpacing.xl)) {
                    AppVersionInfo()
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    Text(text = "🛠️ 语音与 ASR 诊断实验室 (Developer Lab)", style = AppTypography.titleMedium, color = Primary800)
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Text(
                        text = "针对 Vosk 离线语音识别、儿童发音多层算法评分与 AI 卡通音频进行实时调校与检测",
                        style = AppTypography.bodyMedium,
                        color = Neutral700
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    Button(
                        onClick = onOpenDevLab,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = com.kidslanguage.companion.ui.theme.Primary700
                        )
                    ) {
                        Text("进入语音与 ASR 诊断实验室 →")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentToggle(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = AppTypography.bodyLarge)
            Text(description, style = AppTypography.bodyMedium, color = Neutral700)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
