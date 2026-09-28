package com.kidslanguage.companion

import java.io.File
import java.util.zip.ZipFile
import org.junit.Assert.*
import org.junit.Test

class BundledModelsTest {
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
