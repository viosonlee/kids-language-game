package com.kidslanguage.companion.feature.parent

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import com.kidslanguage.companion.ui.theme.Neutral700
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

@Composable
fun AppVersionInfo() {
    val context = LocalContext.current
    var lines by remember { mutableStateOf(listOf("正在读取版本信息…")) }
    LaunchedEffect(context) {
        lines = withContext(Dispatchers.IO) {
            val appVersion = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: "未知"
            val contentVersion = runCatching {
                val source = context.assets.open("content/manifest.json").bufferedReader().use { it.readText() }
                Json.parseToJsonElement(source).jsonObject["contentVersion"]?.jsonPrimitive?.content
            }.getOrNull() ?: "未知"
            val models = runCatching {
                val source = context.assets.open("models/manifest.json").bufferedReader().use { it.readText() }
                Json.parseToJsonElement(source).jsonObject.getValue("models").jsonArray.map {
                    val model = it.jsonObject
                    "${model.getValue("language").jsonPrimitive.content}：Vosk small ${model.getValue("version").jsonPrimitive.content}"
                }
            }.getOrElse { listOf("语音模型版本：未知") }
            listOf("应用版本：$appVersion", "内容版本：$contentVersion") + models
        }
    }
    lines.forEach { Text(it, color = Neutral700) }
}
