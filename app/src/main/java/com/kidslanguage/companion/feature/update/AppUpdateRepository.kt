package com.kidslanguage.companion.feature.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import com.kidslanguage.companion.BuildConfig
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class AppUpdateRepository(context: Context) {
    private val app = context.applicationContext
    private val preferences = app.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val directory get() = File(app.cacheDir, "updates")
    val currentVersion get() = app.packageManager.getPackageInfo(app.packageName, 0).longVersionCode
    val source get() = preferences.getString("manifest_url", null) ?: BuildConfig.UPDATE_MANIFEST_URL

    fun saveSource(address: String) {
        requireHttpsUrl(address)
        preferences.edit().putString("manifest_url", address).apply()
    }

    suspend fun check(address: String): UpdateManifest = withContext(Dispatchers.IO) {
        val connection = openUpdateConnection(address)
        try {
            connection.inputStream.use { UpdateManifest.parse(readManifest(it), app.packageName) }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(manifest: UpdateManifest, onProgress: (Long) -> Unit): File = withContext(Dispatchers.IO) {
        require(manifest.minSdk <= Build.VERSION.SDK_INT) { "此更新需要更高版本的 Android。" }
        val folder = directory
        check(folder.isDirectory || folder.mkdirs()) { "无法创建下载目录，请检查存储空间。" }
        // A route owns one transfer at a time; remove leftovers from interrupted earlier runs.
        folder.listFiles()?.forEach { it.delete() }
        require(folder.usableSpace > manifest.bytes + 16L * 1024 * 1024) { "存储空间不足，请清理后重试。" }
        val partial = File(folder, "${UUID.randomUUID()}.part")
        val ready = File(folder, "${UUID.randomUUID()}.apk")
        try {
            val connection = openUpdateConnection(manifest.apkUrl)
            try {
                val length = connection.contentLengthLong
                require(length == -1L || length == manifest.bytes) { "下载大小与更新信息不一致。" }
                connection.inputStream.use { input ->
                    partial.outputStream().use { output -> copyVerifiedApk(input, output, manifest, onProgress) }
                }
            } finally {
                connection.disconnect()
            }
            currentCoroutineContext().ensureActive()
            verifyIdentity(partial, manifest)
            check(partial.renameTo(ready)) { "无法保存安装包，请重试。" }
            ready
        } finally {
            partial.delete()
        }
    }

    suspend fun installerIntent(file: File, manifest: UpdateManifest): Intent = withContext(Dispatchers.IO) {
        require(file.parentFile?.canonicalFile == directory.canonicalFile && file.isFile) { "下载文件已被清理，请重新下载。" }
        file.inputStream().use { input ->
            copyVerifiedApk(input, object : java.io.OutputStream() {
                override fun write(b: Int) = Unit
                override fun write(b: ByteArray, off: Int, len: Int) = Unit
            }, manifest) {}
        }
        verifyIdentity(file, manifest)
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.updates", file)
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            clipData = ClipData.newRawUri("应用更新", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyIdentity(file: File, manifest: UpdateManifest) {
        val manager = app.packageManager
        val archive = manager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: throw IOException("无法读取安装包，请重新下载。")
        val installed = manager.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        fun signers(info: android.content.pm.PackageInfo): Set<String> =
            info.signingInfo?.apkContentsSigners?.map {
                MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).hex()
            }?.toSet().orEmpty()
        validateApkIdentity(manifest, archive.packageName, archive.longVersionCode,
            archive.applicationInfo?.minSdkVersion ?: Int.MAX_VALUE, installed.longVersionCode,
            signers(installed), signers(archive), Build.VERSION.SDK_INT)
    }
}
