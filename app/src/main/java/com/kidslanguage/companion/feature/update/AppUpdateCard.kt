package com.kidslanguage.companion.feature.update

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kidslanguage.companion.BuildConfig
import com.kidslanguage.companion.ui.theme.AppRadius
import com.kidslanguage.companion.ui.theme.AppSpacing
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral0
import com.kidslanguage.companion.ui.theme.Neutral700
import com.kidslanguage.companion.ui.theme.Primary800
import java.util.Locale

@Composable
fun AppUpdateCard() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // The parent navigation entry owns this model, so leaving parent settings cancels work.
    val model: AppUpdateViewModel = viewModel { AppUpdateViewModel(AppUpdateRepository(context)) }
    val state by model.state.collectAsStateWithLifecycle()
    var editingSource by remember { mutableStateOf(false) }
    var sourceText by remember { mutableStateOf("") }
    var sourceError by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        model.showMessage(if (context.packageManager.canRequestPackageInstalls())
            "已允许安装，请点击安装更新继续。" else "尚未允许安装。可再次点击安装更新进行设置。")
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = Neutral0, contentColor = Neutral700)) {
        Column(Modifier.padding(AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text("应用更新", style = AppTypography.titleMedium, color = Primary800)
            Text("当前版本：${BuildConfig.VERSION_NAME}")
            Text("由家长检查和安装更新，保留练习记录与设置。")
            Text(state.message)
            state.available?.let { update ->
                Text("${update.versionName} · ${String.format(Locale.ROOT, "%.1f", update.bytes / 1048576.0)} MB")
                if (update.notes.isNotBlank()) Text(update.notes)
                state.received?.let { received ->
                    val progress = (received.toFloat() / update.bytes).coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("已下载 ${(progress * 100).toInt()}%")
                }
                if (state.downloaded == null) {
                    Button(onClick = model::download, enabled = !state.busy) { Text("下载更新", maxLines = 1, softWrap = false) }
                } else {
                    Button(enabled = !state.busy, onClick = {
                        if (context.packageManager.canRequestPackageInstalls()) {
                            model.install(context) { lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
                        } else {
                            model.showMessage("请允许本应用安装更新；设置完成后返回这里，点击安装更新。")
                            try {
                                permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}")))
                            } catch (_: Exception) {
                                model.showMessage("无法打开安装权限页面，请在系统设置中允许本应用安装未知应用。")
                            }
                        }
                    }) { Text("安装更新", maxLines = 1, softWrap = false) }
                }
            }
            if (state.busy) {
                TextButton(onClick = model::cancel) { Text("取消", maxLines = 1, softWrap = false) }
            } else {
                Button(onClick = model::check) { Text("检查更新", maxLines = 1, softWrap = false) }
                TextButton(onClick = { sourceText = state.source; sourceError = null; editingSource = true }) {
                    Text("设置更新地址", maxLines = 1, softWrap = false)
                }
            }
        }
    }

    if (editingSource) {
        AlertDialog(onDismissRequest = { editingSource = false }, title = { Text("更新地址") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Text("使用发布者提供的 HTTPS 更新地址。仅在检查或下载更新时联网。")
                OutlinedTextField(value = sourceText, onValueChange = { sourceText = it; sourceError = null },
                    label = { Text("更新信息地址") }, singleLine = true, isError = sourceError != null)
                sourceError?.let { Text(it) }
                TextButton(onClick = { sourceText = BuildConfig.UPDATE_MANIFEST_URL; sourceError = null }) { Text("恢复默认地址") }
            }
        }, confirmButton = {
            TextButton(onClick = {
                sourceError = model.saveSource(sourceText)
                if (sourceError == null) editingSource = false
            }) { Text("保存") }
        }, dismissButton = { TextButton(onClick = { editingSource = false }) { Text("取消") } })
    }
}
