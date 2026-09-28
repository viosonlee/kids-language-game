package com.kidslanguage.companion.core.asr

import android.content.Context
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * ModelManager manages offline Vosk speech recognition models:
 * 1. Checks internal and external application storage for extracted models.
 * 2. If missing, automatically checks assets for a bundled model zip (e.g. models/vosk-model-small-ja.zip)
 *    and unpacks it into internal storage on first run without requiring any PC/ADB connection.
 */
object ModelManager {

    fun getModelDirName(language: LanguageCode): String = when (language) {
        LanguageCode.JA_JP -> "vosk-model-small-ja"
        LanguageCode.ZH_CN -> "vosk-model-small-cn"
        LanguageCode.EN_US -> "vosk-model-small-en-us"
    }

    suspend fun getOrExtractModelPath(context: Context, language: LanguageCode): String? = withContext(Dispatchers.IO) {
        val dirName = getModelDirName(language)

        // 1. Check external files dir
        val extDir = context.getExternalFilesDir(null)
        if (extDir != null) {
            val extCandidate = File(extDir, "models/$dirName")
            if (isModelValid(extCandidate)) {
                return@withContext extCandidate.absolutePath
            }
        }

        // 2. Check internal files dir
        val intCandidate = File(context.filesDir, "models/$dirName")
        if (isModelValid(intCandidate)) {
            return@withContext intCandidate.absolutePath
        }

        // 3. Check assets for bundled zip (e.g., models/vosk-model-small-ja.zip)
        val zipAssetName = "models/$dirName.zip"
        try {
            val assetList = context.assets.list("models") ?: emptyArray()
            if (assetList.contains("$dirName.zip")) {
                val targetDir = File(context.filesDir, "models")
                targetDir.mkdirs()

                context.assets.open(zipAssetName).use { inputStream ->
                    unzipStream(inputStream, targetDir, dirName)
                }

                if (isModelValid(intCandidate)) {
                    return@withContext intCandidate.absolutePath
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
    }

    private fun isModelValid(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        val files = dir.list() ?: return false
        return files.isNotEmpty()
    }

    private fun unzipStream(inputStream: java.io.InputStream, destDir: File, expectedTargetName: String) {
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            var rootDirName: String? = null

            while (entry != null) {
                val entryName = entry.name
                val parts = entryName.split("/", "\\")
                if (rootDirName == null && parts.isNotEmpty() && parts[0].isNotBlank()) {
                    rootDirName = parts[0]
                }

                val outFile = File(destDir, entryName)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }

            // If extracted with version suffix (e.g. vosk-model-small-ja-0.22), rename to expectedTargetName
            if (rootDirName != null && rootDirName != expectedTargetName) {
                val extractedDir = File(destDir, rootDirName)
                val targetDir = File(destDir, expectedTargetName)
                if (extractedDir.exists()) {
                    if (targetDir.exists()) {
                        targetDir.deleteRecursively()
                    }
                    extractedDir.renameTo(targetDir)
                }
            }
        }
    }
}
