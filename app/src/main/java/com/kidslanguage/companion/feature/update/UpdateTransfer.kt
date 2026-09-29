package com.kidslanguage.companion.feature.update

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }

/** Redirects are followed explicitly so an HTTPS origin cannot downgrade to HTTP. */
internal suspend fun openUpdateConnection(address: String): HttpsURLConnection {
    var uri = requireHttpsUrl(address)
    repeat(6) {
        currentCoroutineContext().ensureActive()
        val connection = uri.toURL().openConnection() as HttpsURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", "KidsLanguageCompanion-Updater")
            connection.setRequestProperty("Accept-Encoding", "identity")
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> return connection
                301, 302, 303, 307, 308 -> {
                    val next = connection.getHeaderField("Location")
                        ?: throw IOException("更新服务返回了无效的下载地址。")
                    uri = requireHttpsUrl(uri.resolve(next).toString())
                }
                404 -> throw IOException("还没有发布可用更新，或更新地址不正确。")
                else -> throw IOException("更新服务暂不可用（${connection.responseCode}），请稍后重试。")
            }
        } catch (error: Exception) {
            connection.disconnect()
            throw error
        }
        connection.disconnect()
    }
    throw IOException("更新地址跳转次数过多。")
}

internal suspend fun readManifest(input: InputStream): String {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = input.read(buffer)
        if (count < 0) break
        require(output.size() + count <= 64 * 1024) { "更新信息文件过大。" }
        output.write(buffer, 0, count)
    }
    return output.toString(Charsets.UTF_8.name())
}

internal suspend fun copyVerifiedApk(
    input: InputStream,
    output: OutputStream,
    manifest: UpdateManifest,
    onProgress: (Long) -> Unit,
) {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(64 * 1024)
    var received = 0L
    var lastPercent = -1L
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = input.read(buffer)
        if (count < 0) break
        received += count
        require(received <= manifest.bytes && received <= MAX_APK_BYTES) { "下载大小与更新信息不一致，请重新检查更新。" }
        digest.update(buffer, 0, count)
        output.write(buffer, 0, count)
        val percent = received * 100 / manifest.bytes
        if (percent != lastPercent) {
            lastPercent = percent
            onProgress(received)
        }
    }
    require(received == manifest.bytes) { "下载未完成，请重试。" }
    require(digest.digest().hex().equals(manifest.sha256, ignoreCase = true)) { "安装包校验失败，请重新下载。" }
}
