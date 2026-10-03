package com.kidslanguage.companion.feature.parent

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import com.kidslanguage.companion.ui.theme.Neutral700
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

private inline fun <T> readVersionOrNull(read: () -> T): T? = try {
    read()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

private fun JsonElement?.versionLabel(): String? = (this as? JsonPrimitive)
    ?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }

internal fun readVersionInfo(readAppVersion: () -> String?, readAsset: (String) -> String): List<String> {
    val appVersion = readVersionOrNull { readAppVersion()?.trim()?.takeIf { it.isNotEmpty() } } ?: "未知"
    val contentVersion = readVersionOrNull {
        Json.parseToJsonElement(readAsset("content/manifest.json")).jsonObject["contentVersion"].versionLabel()
    } ?: "未知"
    val models = readVersionOrNull {
        val entries = Json.parseToJsonElement(readAsset("models/manifest.json")).jsonObject.getValue("models").jsonArray
        require(entries.isNotEmpty())
        entries.map {
            val model = it.jsonObject
            val language = requireNotNull(model["language"].versionLabel())
            val version = requireNotNull(model["version"].versionLabel())
            "$language：Vosk small $version"
        }
    } ?: listOf("语音模型版本：未知")
    return listOf("应用版本：$appVersion", "内容版本：$contentVersion") + models
}

@Composable
fun AppVersionInfo() {
    val context = LocalContext.current
    var lines by remember { mutableStateOf(listOf("正在读取版本信息…")) }
    LaunchedEffect(context) {
        lines = withContext(Dispatchers.IO) {
            readVersionInfo(
                readAppVersion = { context.packageManager.getPackageInfo(context.packageName, 0).versionName },
                readAsset = { path -> context.assets.open(path).bufferedReader().use { it.readText() } }
            )
        }
    }
    lines.forEach { Text(it, color = Neutral700) }
}
