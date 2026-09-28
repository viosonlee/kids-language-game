package com.kidslanguage.companion.core.asr

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

internal object OfflineModelArchive {
    private val requiredFiles = listOf("am/final.mdl", "conf/mfcc.conf", "conf/model.conf",
        "graph/HCLr.fst", "graph/Gr.fst", "graph/phones/word_boundary.int")

    fun isValid(directory: File): Boolean = requiredFiles.all {
        File(directory, it).let { file -> file.isFile && file.length() > 0 }
    }

    fun extract(input: InputStream, staging: File): File {
        val root = staging.canonicalFile
        root.mkdirs()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val target = File(root, entry.name.replace('\\', '/')).canonicalFile
                require(target.toPath().startsWith(root.toPath()) && target != root) {
                    "Model archive contains an unsafe path"
                }
                if (entry.isDirectory) target.mkdirs() else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return requireNotNull(root.listFiles()?.singleOrNull { it.isDirectory && isValid(it) }) {
            "Model archive is incomplete or invalid"
        }
    }
}
