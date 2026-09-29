package com.kidslanguage.companion.feature.update

import java.net.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val MAX_APK_BYTES = 512L * 1024 * 1024

@Serializable
data class UpdateManifest(
    val schemaVersion: Int,
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val minSdk: Int,
    val apkUrl: String,
    val bytes: Long,
    val sha256: String,
    val notes: String = "",
) {
    fun validate(expectedPackage: String): UpdateManifest {
        require(schemaVersion == 1) { "更新信息格式暂不支持，请联系发布者。" }
        require(packageName == expectedPackage) { "更新包与当前应用不匹配，请检查更新地址。" }
        require(versionCode > 0 && versionName.isNotBlank() && versionName.length <= 64) { "更新版本信息不正确。" }
        require(minSdk > 0 && bytes in 1..MAX_APK_BYTES) { "更新包大小或系统版本信息不正确。" }
        require(sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "更新包缺少有效的校验信息。" }
        require(notes.length <= 8000) { "更新说明过长。" }
        requireHttpsUrl(apkUrl)
        return this
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(source: String, expectedPackage: String): UpdateManifest =
            json.decodeFromString<UpdateManifest>(source).validate(expectedPackage)
    }
}

fun requireHttpsUrl(value: String): URI {
    val uri = try { URI(value) } catch (_: Exception) {
        throw IllegalArgumentException("请输入完整的 HTTPS 更新地址。")
    }
    require(value.length <= 4096 && uri.scheme.equals("https", true) &&
        !uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawFragment == null) {
        "请输入不含账号、密码或片段的 HTTPS 更新地址。"
    }
    return uri
}

internal fun validateApkIdentity(
    manifest: UpdateManifest,
    archivePackage: String,
    archiveVersion: Long,
    archiveMinSdk: Int,
    installedVersion: Long,
    installedSigners: Set<String>,
    archiveSigners: Set<String>,
    deviceSdk: Int,
) {
    require(archivePackage == manifest.packageName && archiveVersion == manifest.versionCode) {
        "安装包的应用或版本与更新信息不一致。"
    }
    require(archiveVersion > installedVersion) { "该版本已安装，或版本低于当前应用。" }
    require(archiveMinSdk == manifest.minSdk && archiveMinSdk <= deviceSdk) { "此更新不支持当前 Android 系统。" }
    require(installedSigners.isNotEmpty() && installedSigners == archiveSigners) {
        "安装包签名与当前应用不一致，无法覆盖升级。"
    }
}
