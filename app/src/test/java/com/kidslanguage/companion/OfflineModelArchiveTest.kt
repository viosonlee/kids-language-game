package com.kidslanguage.companion

import com.kidslanguage.companion.core.asr.OfflineModelArchive
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfflineModelArchiveTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun archive(vararg paths: String): ByteArrayInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            paths.forEach {
                zip.putNextEntry(ZipEntry(it))
                zip.write(byteArrayOf(1))
                zip.closeEntry()
            }
        }
        return ByteArrayInputStream(bytes.toByteArray())
    }

    @Test fun incompleteDirectoryIsNotAUsableModel() {
        val dir = temporary.newFolder()
        File(dir, "README").writeText("partial extraction")
        assertFalse(OfflineModelArchive.isValid(dir))
    }

    @Test fun completeVersionedArchiveIsAccepted() {
        val input = archive(*listOf("am/final.mdl", "conf/mfcc.conf", "conf/model.conf",
            "graph/HCLr.fst", "graph/Gr.fst", "graph/phones/word_boundary.int")
            .map { "vosk-model-small-test-0.1/$it" }.toTypedArray())
        assertTrue(OfflineModelArchive.isValid(OfflineModelArchive.extract(input, temporary.newFolder())))
    }

    @Test fun traversalIsRejectedWithoutWritingOutsideStaging() {
        val parent = temporary.newFolder()
        assertThrows(IllegalArgumentException::class.java) {
            OfflineModelArchive.extract(archive("../escaped"), File(parent, "stage"))
        }
        assertFalse(File(parent, "escaped").exists())
    }

    @Test fun truncatedArchiveIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            OfflineModelArchive.extract(archive("model/am/final.mdl"), temporary.newFolder())
        }
    }
}
