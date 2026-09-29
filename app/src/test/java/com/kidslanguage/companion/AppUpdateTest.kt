package com.kidslanguage.companion

import com.kidslanguage.companion.feature.update.*
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AppUpdateTest {
    private val bytes = "verified-apk-content".toByteArray()
    private val manifest = UpdateManifest(1, "com.kidslanguage.companion.debug", 2, "1.0.1", 29,
        "https://example.com/app.apk", bytes.size.toLong(), MessageDigest.getInstance("SHA-256").digest(bytes).hex())

    @Test fun manifestRejectsWrongPackageAndInvalidFields() {
        val candidates = listOf(manifest.copy(schemaVersion = 2), manifest.copy(packageName = "another.app"),
            manifest.copy(bytes = 0), manifest.copy(bytes = MAX_APK_BYTES + 1), manifest.copy(versionCode = 0),
            manifest.copy(sha256 = "bad"), manifest.copy(minSdk = 0), manifest.copy(notes = "x".repeat(8001)))
        candidates.forEach { value -> assertThrows(IllegalArgumentException::class.java) { value.validate(manifest.packageName) } }
        assertEquals(manifest, manifest.validate(manifest.packageName))
    }

    @Test fun manifestDecodesFutureOptionalFields() {
        val source = """{"schemaVersion":1,"packageName":"${manifest.packageName}","versionCode":2,"versionName":"1.0.1",
            "minSdk":29,"apkUrl":"${manifest.apkUrl}","bytes":${manifest.bytes},"sha256":"${manifest.sha256}","future":true}"""
        assertEquals(manifest, UpdateManifest.parse(source, manifest.packageName))
    }

    @Test fun urlsRejectDowngradeCredentialsAndInvalidOrigins() {
        listOf("http://example.com/file", "file:///file", "https://user:password@example.com/file",
            "https:///file", "https://example.com/file#fragment").forEach {
            assertThrows(IllegalArgumentException::class.java) { requireHttpsUrl(it) }
        }
        assertEquals("release-assets.githubusercontent.com", requireHttpsUrl("https://release-assets.githubusercontent.com/file?sig=123").host)
    }

    @Test fun apkMustMatchVersionPackageSdkAndCurrentSigner() {
        fun verify(pkg: String = manifest.packageName, version: Long = 2, minSdk: Int = 29,
            installed: Long = 1, signers: Set<String> = setOf("trusted"), current: Set<String> = setOf("trusted")) =
            validateApkIdentity(manifest, pkg, version, minSdk, installed, current, signers, 29)
        verify()
        assertThrows(IllegalArgumentException::class.java) { verify(pkg = "other.app") }
        assertThrows(IllegalArgumentException::class.java) { verify(version = 3) }
        assertThrows(IllegalArgumentException::class.java) { verify(installed = 2) }
        assertThrows(IllegalArgumentException::class.java) { verify(installed = 3) }
        assertThrows(IllegalArgumentException::class.java) { verify(minSdk = 30) }
        assertThrows(IllegalArgumentException::class.java) { verify(signers = setOf("untrusted")) }
        assertThrows(IllegalArgumentException::class.java) { verify(current = emptySet(), signers = emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { verify(signers = setOf("trusted", "extra")) }
    }

    @Test fun downloadVerifiesBytesAndReportsProgress() = runBlocking {
        val output = ByteArrayOutputStream()
        val progress = mutableListOf<Long>()
        copyVerifiedApk(bytes.inputStream(), output, manifest) { progress += it }
        assertArrayEquals(bytes, output.toByteArray())
        assertEquals(manifest.bytes, progress.last())
    }

    @Test fun downloadRejectsTruncationOversizeAndTampering() = runBlocking {
        for (content in listOf(bytes.copyOf(bytes.size - 1), bytes + 1.toByte(), bytes.reversedArray())) {
            try {
                copyVerifiedApk(content.inputStream(), ByteArrayOutputStream(), manifest) {}
                fail("Expected corrupted download rejection")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun cancelledDownloadStopsBeforeReadingMoreBytes() = runBlocking {
        var reads = 0
        val content = ByteArray(150_000) { 1 }
        val input = object : java.io.ByteArrayInputStream(content) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                reads++
                return super.read(b, off, len)
            }
        }
        val task = async {
            val job = currentCoroutineContext()
            copyVerifiedApk(input, ByteArrayOutputStream(), manifest.copy(bytes = content.size.toLong())) {
                job.cancel()
            }
        }
        try { task.await(); fail("Expected cancellation") } catch (_: CancellationException) { }
        assertEquals(1, reads)
    }

    @Test fun manifestReadIsBoundedAndAcceptsUtf8() = runBlocking {
        assertEquals("升级说明", readManifest("升级说明".byteInputStream()))
        try {
            readManifest(ByteArray(65537).inputStream())
            fail("Expected oversized manifest rejection")
        } catch (_: IllegalArgumentException) { }
    }
}
