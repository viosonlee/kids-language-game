package com.kidslanguage.companion

import java.io.File
import java.util.zip.ZipFile
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import kotlinx.serialization.json.*

class BundledModelsTest {
    @Test fun modelManifestMatchesPackagedBytesAndLanguages() {
        val root = File("src/main/assets/models")
        val models = Json.parseToJsonElement(File(root, "manifest.json").readText())
            .jsonObject.getValue("models").jsonArray
        assertEquals(setOf("ja-JP", "zh-CN", "en-US"),
            models.map { it.jsonObject.getValue("language").jsonPrimitive.content }.toSet())
        assertEquals(3, models.size)
        models.forEach { element ->
            val model = element.jsonObject
            val file = File(root, model.getValue("file").jsonPrimitive.content)
            assertEquals(model.getValue("bytes").jsonPrimitive.long, file.length())
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(65536)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            assertEquals(file.name, model.getValue("sha256").jsonPrimitive.content, actual)
        }
    }
    @Test fun everySupportedLanguageShipsACompleteOfflineModel() {
        val names = listOf("vosk-model-small-ja", "vosk-model-small-cn", "vosk-model-small-en-us")
        val required = listOf("am/final.mdl", "conf/mfcc.conf", "conf/model.conf",
            "graph/HCLr.fst", "graph/Gr.fst", "graph/phones/word_boundary.int")
        for (name in names) {
            val archive = File("src/main/assets/models/$name.zip")
            assertTrue("Missing bundled model: $name", archive.isFile)
            ZipFile(archive).use { zip ->
                val entries = zip.entries().asSequence().toList()
                for (path in required) {
                    assertTrue("Incomplete $name: $path", entries.any { it.name.endsWith("/$path") && it.size > 0 })
                }
            }
        }
    }
}
