package com.kidslanguage.companion.feature.parent

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.kidslanguage.companion.ui.theme.AppRadius
import com.kidslanguage.companion.ui.theme.AppSpacing
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral0
import com.kidslanguage.companion.ui.theme.Neutral50
import com.kidslanguage.companion.ui.theme.Neutral700
import com.kidslanguage.companion.ui.theme.Neutral900
import com.kidslanguage.companion.ui.theme.Primary100
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
                    color = Primary600
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

@Composable
fun ParentSettingsScreen(
    viewModel: ParentViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()

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
                    Text(text = "学习设置", style = AppTypography.titleMedium, color = Primary600)
                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    // Text display toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "显示文字字幕", style = AppTypography.bodyLarge)
                            Text(
                                text = "关闭后练习界面将不显示文本，强化纯听音辨别能力",
                                style = AppTypography.bodyMedium,
                                color = Neutral700
                            )
                        }
                        Switch(
                            checked = settings.showText,
                            onCheckedChange = { viewModel.setShowText(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    // Auto Next toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "跟读通过后自动跳下一题", style = AppTypography.bodyLarge)
                            Text(
                                text = "通过后给予 1.2 秒庆祝动画后自动进入下一题",
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
                    Text(text = "练习统计与数据", style = AppTypography.titleMedium, color = Primary600)
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    Text(
                        text = "累计完成题目数：${settings.completedCount} 题",
                        style = AppTypography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    Button(
                        onClick = { viewModel.resetProgress() },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFE53935)
                        )
                    ) {
                        Text("清空练习记录")
                    }
                }
            }
        }
    }
}
