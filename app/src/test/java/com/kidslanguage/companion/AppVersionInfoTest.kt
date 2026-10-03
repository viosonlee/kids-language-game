package com.kidslanguage.companion

import com.kidslanguage.companion.feature.parent.readVersionInfo
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException

class AppVersionInfoTest {
    private val modelManifest = """{"models":[{"language":"en-US","version":"0.15"}]}"""
    private fun read(content: String, models: String = modelManifest, version: String? = "1.0.2") =
        readVersionInfo({ version }) { if (it == "content/manifest.json") content else models }

    @Test fun bundledManifestsShowContentAndAllThreeModelVersions() {
        val lines = readVersionInfo({ "1.0.2" }) { File("src/main/assets", it).readText() }
        assertEquals(listOf("应用版本：1.0.2", "内容版本：1.0.4", "zh-CN：Vosk small 0.22",
            "en-US：Vosk small 0.15", "ja-JP：Vosk small 0.22"), lines)
    }

    @Test fun invalidContentValuesFallBackWithoutLosingOtherVersions() {
        for (content in listOf("{", "[]", "{}", """{"contentVersion":null}""",
            """{"contentVersion":12}""", """{"contentVersion":true}""", """{"contentVersion":"  "}""")) {
            assertEquals(listOf("应用版本：1.0.2", "内容版本：未知", "en-US：Vosk small 0.15"), read(content))
        }
    }

    @Test fun malformedOrEmptyModelsHaveExplicitFallback() {
        for (models in listOf("{", "{}", """{"models":[]}""", """{"models":[null]}""",
            """{"models":[{"language":"en-US","version":null}]}""",
            """{"models":[{"language":true,"version":"0.15"}]}""")) {
            assertEquals(listOf("应用版本：1.0.2", "内容版本：1.0.4", "语音模型版本：未知"),
                read("""{"contentVersion":"1.0.4"}""", models))
        }
    }

    @Test fun readFailuresAndMissingAppVersionRemainReadable() {
        assertEquals(listOf("应用版本：未知", "内容版本：未知", "语音模型版本：未知"),
            readVersionInfo({ throw IOException("package unavailable") }) { throw IOException("missing asset") })
        for (version in listOf(null, "", "  ")) {
            assertEquals("应用版本：未知", read("{}", version = version).first())
        }
    }

    @Test fun cancellationIsNotConvertedToUnknownVersions() {
        val expected = CancellationException("screen left")
        try {
            readVersionInfo({ "1.0.2" }) { throw expected }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(expected, actual)
        }
    }
}
