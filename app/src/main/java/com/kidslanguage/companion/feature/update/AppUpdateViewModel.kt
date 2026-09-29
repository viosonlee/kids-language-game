package com.kidslanguage.companion.feature.update

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppUpdateState(
    val source: String,
    val busy: Boolean = false,
    val message: String = "点击检查更新，需要联网。",
    val available: UpdateManifest? = null,
    val downloaded: File? = null,
    val received: Long? = null,
)

class AppUpdateViewModel(private val repository: AppUpdateRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AppUpdateState(repository.source))
    val state = mutableState.asStateFlow()
    private var task: Job? = null

    fun saveSource(address: String): String? = try {
        check(!mutableState.value.busy)
        repository.saveSource(address.trim())
        mutableState.value = AppUpdateState(repository.source)
        null
    } catch (error: IllegalArgumentException) {
        error.message
    }

    fun check() = runTask {
        mutableState.update { it.copy(message = "正在检查更新…", available = null, downloaded = null, received = null) }
        val manifest = repository.check(mutableState.value.source)
        val message = when {
            manifest.versionCode <= repository.currentVersion -> "当前已是最新版本。"
            manifest.minSdk > Build.VERSION.SDK_INT -> "发现 ${manifest.versionName}，但它需要更高版本的 Android，当前设备暂不能安装。"
            else -> "发现新版本 ${manifest.versionName}"
        }
        val available = manifest.takeIf { it.versionCode > repository.currentVersion && it.minSdk <= Build.VERSION.SDK_INT }
        mutableState.update { it.copy(message = message, available = available) }
    }

    fun download() {
        val manifest = mutableState.value.available ?: return
        runTask {
            mutableState.update { it.copy(message = "正在下载…", downloaded = null, received = 0) }
            val file = repository.download(manifest) { count -> mutableState.update { it.copy(received = count) } }
            mutableState.update { it.copy(downloaded = file, received = null, message = "下载完成，已验证安装包。点击安装后请按系统提示操作。") }
        }
    }

    fun install(context: Context, canOpenInstaller: () -> Boolean) {
        val snapshot = mutableState.value
        val manifest = snapshot.available ?: return
        val file = snapshot.downloaded ?: return
        runTask {
            mutableState.update { it.copy(message = "正在核验安装包…") }
            val intent = repository.installerIntent(file, manifest)
            check(canOpenInstaller()) { "请返回家长设置后再次点击安装更新。" }
            context.startActivity(intent)
            mutableState.update { it.copy(message = "已打开系统安装界面。如取消安装，可再次点击安装更新。") }
        }
    }

    fun showMessage(message: String) { mutableState.update { it.copy(message = message) } }

    fun cancel() { task?.cancel() }

    private fun runTask(block: suspend () -> Unit) {
        if (task?.isActive == true) return
        task = viewModelScope.launch {
            mutableState.update { it.copy(busy = true) }
            try {
                block()
            } catch (cancelled: CancellationException) {
                mutableState.update { it.copy(message = "已取消，可再次重试。", received = null) }
                throw cancelled
            } catch (error: Exception) {
                val message = when (error) {
                    is java.net.SocketTimeoutException -> "连接超时，请检查网络后重试。"
                    is java.net.UnknownHostException -> "无法连接更新服务，请检查网络后重试。"
                    is kotlinx.serialization.SerializationException -> "更新信息格式不正确，请检查更新地址。"
                    is android.content.ActivityNotFoundException -> "当前设备没有可用的安装器。"
                    is IllegalArgumentException, is IllegalStateException, is java.io.IOException ->
                        error.message ?: "更新失败，请稍后重试。"
                    else -> "更新失败，请检查网络或安装权限后重试。"
                }
                mutableState.update { it.copy(message = message, received = null) }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}
