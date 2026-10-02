package com.kidslanguage.companion.feature.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslanguage.companion.app.AppDependencies
import com.kidslanguage.companion.core.model.EvaluationResult
import com.kidslanguage.companion.core.model.EvaluationStatus
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.PracticeItem
import com.kidslanguage.companion.ui.theme.Neutral900
import com.kidslanguage.companion.ui.theme.Primary100
import com.kidslanguage.companion.ui.theme.Primary500
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

@Composable
fun DevAudioDiagnosticScreen(
    container: AppDependencies,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedLanguage by remember { mutableStateOf(LanguageCode.JA_JP) }
    var items by remember { mutableStateOf<List<PracticeItem>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<PracticeItem?>(null) }

    var isRecording by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var evaluationResult by remember { mutableStateOf<EvaluationResult?>(null) }
    var attemptCount by remember { mutableStateOf(1) }
    var statusMessage by remember { mutableStateOf("就绪 - 点击麦克风测试识别") }
    var modelReady by remember { mutableStateOf(false) }
    var generation by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Load items when language changes
    LifecycleResumeEffect(selectedLanguage) {
        modelReady = false
        generation++
        val loading = coroutineScope.launch {
            statusMessage = "正在准备离线语音模型…"
            items = container.contentRepository.getItems(selectedLanguage, null)
            selectedItem = items.firstOrNull()
            recognizedText = ""
            evaluationResult = null
            attemptCount = 1
            val loaded = container.speechRecognizerEngine.load(selectedLanguage)
            modelReady = loaded.isSuccess
            statusMessage = if (modelReady) "模型就绪 - 点击麦克风测试识别" else "语音模型不可用"
        }
        onPauseOrDispose {
            generation++
            modelReady = false
            isRecording = false
            loading.cancel()
            coroutineScope.coroutineContext.cancelChildren()
            container.speechRecognizerEngine.cancelListening()
            container.promptPlayer.stop()
            container.soundEffectManager.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp)
    ) {
        // Top Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                ) {
                    Text("← 返回设置", color = Neutral900, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "语音识别与发音诊断",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Neutral900
                )
            }

            // Quick SFX Audition Buttons
            Row(
                modifier = Modifier.align(Alignment.End).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { container.soundEffectManager.playPop() }) {
                    Text("Pop 🫧", maxLines = 1, softWrap = false)
                }
                OutlinedButton(onClick = { container.soundEffectManager.playSuccess() }) {
                    Text("Success 🔔", maxLines = 1, softWrap = false)
                }
                OutlinedButton(onClick = { container.soundEffectManager.playCelebrate() }) {
                    Text("Celebrate 🎉", maxLines = 1, softWrap = false)
                }
                OutlinedButton(onClick = { container.soundEffectManager.playRetry() }) {
                    Text("Retry 🥁", maxLines = 1, softWrap = false)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Selector Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LanguageCode.entries.forEach { lang ->
                val isSelected = lang == selectedLanguage
                Button(
                    onClick = { selectedLanguage = lang },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) com.kidslanguage.companion.ui.theme.Primary700 else Color(0xFFE2E8F0),
                        contentColor = if (isSelected) Color.White else Neutral900
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when (lang) {
                            LanguageCode.JA_JP -> "🇯🇵 日语 (62题)"
                            LanguageCode.ZH_CN -> "🇨🇳 中文 (32题)"
                            LanguageCode.EN_US -> "🇺🇸 英语 (32题)"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Two-column layout: Left Item List, Right Diagnostic Testing Console
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left: Items List (Card selection)
            Card(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("词库列表 (${items.size} 项)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items) { item ->
                            val isSel = item.id == selectedItem?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) Primary100 else Color(0xFFF1F5F9))
                                    .clickable {
                                        generation++
                                        container.speechRecognizerEngine.cancelListening()
                                        isRecording = false
                                        selectedItem = item
                                        recognizedText = ""
                                        evaluationResult = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.image ?: "📝", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(item.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Neutral900)
                                        Text(item.reading ?: item.translation ?: "", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                Text(item.id.takeLast(7), fontSize = 11.sp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }

            // Right: Testing & Evaluation Console
            Card(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val current = selectedItem
                if (current == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("请选择左侧词汇条目")
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Current Item Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(current.image ?: "🎯", fontSize = 44.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(current.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Neutral900)
                                    Text(
                                        text = "读音: ${current.reading ?: current.pinyin ?: "无"} | 含义: ${current.translation ?: "无"}",
                                        fontSize = 14.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }

                            // Play Audio Button
                            Button(
                                onClick = {
                                    generation++
                                    container.speechRecognizerEngine.cancelListening()
                                    isRecording = false
                                    coroutineScope.launch {
                                        statusMessage = "正在播放卡通发音..."
                                        container.promptPlayer.playPrompt(
                                            text = current.reading ?: current.text,
                                            language = selectedLanguage,
                                            audioAssetPath = current.audio
                                        )
                                        statusMessage = "播放完毕"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = com.kidslanguage.companion.ui.theme.Primary700),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("🔊 播放发音", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "音频资源路径: ${current.audio ?: "未配置 (将回退TTS)"}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Mic Recording Test Section
                        Text("🎙️ 现场录音与 ASR 算法评测", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                enabled = modelReady,
                                onClick = {
                                    if (isRecording) {
                                        container.speechRecognizerEngine.stopListening()
                                        isRecording = false
                                        statusMessage = "识别已停止"
                                    } else {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                            statusMessage = "请先在系统设置中允许麦克风权限"
                                            return@Button
                                        }
                                        container.promptPlayer.stop()
                                        container.soundEffectManager.stop()
                                        val request = ++generation
                                        isRecording = true
                                        statusMessage = "正在录音与识别中... 请对着麦克风发音"
                                        coroutineScope.launch {
                                            try {
                                            container.speechRecognizerEngine.startListening(selectedLanguage) { res ->
                                                if (request != generation) return@startListening
                                                recognizedText = res.text
                                                isRecording = false
                                                statusMessage = if (res.isSuccess) "识别完成" else "识别出错: ${res.error}"
                                                
                                                // Run evaluation algorithm
                                                val eval = if (res.isSuccess && res.text.isNotBlank()) container.speechEvaluationService.evaluate(
                                                    spokenText = res.text,
                                                    targetItem = current,
                                                    attemptCount = attemptCount
                                                ) else null
                                                evaluationResult = eval
                                            }
                                            } catch (e: CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                if (request == generation) {
                                                    isRecording = false
                                                    statusMessage = "无法开始录音：${e.message}"
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecording) Color(0xFFEF4444) else Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    text = if (isRecording) "⏹ 停止录音" else "🎙️ 开始录音测试",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("尝试次数(SoftPass测试): ", fontSize = 14.sp)
                                OutlinedButton(onClick = { if (attemptCount > 1) attemptCount-- }) { Text("-") }
                                Text(" $attemptCount ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                OutlinedButton(onClick = { attemptCount++ }) { Text("+") }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(statusMessage, fontSize = 13.sp, color = if (isRecording) Color.Red else Color.DarkGray)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Results Console Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📊 实时诊断数据 (Live Diagnostic Output):", fontWeight = FontWeight.Bold, color = Neutral900)
                                
                                Row {
                                    Text("原始 ASR 识别文本: ", fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                                    Text(
                                        text = if (recognizedText.isNotBlank()) "「$recognizedText」" else "(暂无识别结果)",
                                        fontWeight = FontWeight.Bold,
                                        color = if (recognizedText.isNotBlank()) Color(0xFF0284C7) else Color.Gray
                                    )
                                }

                                val eval = evaluationResult
                                if (eval != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("评测判定状态: ", fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                                        val (statusColor, statusLabel) = when (eval.status) {
                                            EvaluationStatus.Pass -> Color(0xFF10B981) to "Pass (通过 100%)"
                                            EvaluationStatus.SoftPass -> Color(0xFFF59E0B) to "SoftPass (幼儿温和辅助通过)"
                                            EvaluationStatus.Retry -> Color(0xFFEF4444) to "Retry (建议重试)"
                                            EvaluationStatus.NoSpeech -> Color(0xFF6B7280) to "NoSpeech (未检出有效语音)"
                                            EvaluationStatus.Error -> Color(0xFFDC2626) to "Error (发生异常)"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(statusColor)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(statusLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }

                                    Row {
                                        Text("算法相似度评分: ", fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                                        Text("${(eval.score * 100).toInt()}%", fontWeight = FontWeight.Bold, color = Primary500)
                                    }

                                    Row {
                                        Text("儿童激励反馈语: ", fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                                        Text("“${eval.message}”", fontWeight = FontWeight.Medium, color = Neutral900)
                                    }

                                    if (current.aliases.isNotEmpty()) {
                                        Text(
                                            text = "配置别名列表: ${current.aliases.joinToString(", ")}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
