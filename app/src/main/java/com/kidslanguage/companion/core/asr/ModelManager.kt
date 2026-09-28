package com.kidslanguage.companion.core.asr

import android.content.Context
import com.kidslanguage.companion.core.model.LanguageCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

object ModelManager {
    private val extractionLock = Mutex()

    fun getModelDirName(language: LanguageCode): String = when (language) {
        LanguageCode.JA_JP -> "vosk-model-small-ja"
        LanguageCode.ZH_CN -> "vosk-model-small-cn"
        LanguageCode.EN_US -> "vosk-model-small-en-us"
    }

    suspend fun getOrExtractModelPath(context: Context, language: LanguageCode): String? =
        withContext(Dispatchers.IO) {
            extractionLock.withLock {
                val name = getModelDirName(language)
                context.getExternalFilesDir(null)?.let { external ->
                    val model = File(external, "models/$name")
                    if (OfflineModelArchive.isValid(model)) return@withLock model.absolutePath
                }
                val parent = File(context.filesDir, "models")
                val target = File(parent, name)
                if (OfflineModelArchive.isValid(target)) return@withLock target.absolutePath
                // Incomplete extraction never becomes the active model directory.
                val staging = File(parent, "$name.staging")
                try {
                    staging.deleteRecursively()
                    val extracted = context.assets.open("models/$name.zip").use {
                        OfflineModelArchive.extract(it, staging)
                    }
                    if (target.exists()) check(target.deleteRecursively()) { "Cannot replace incomplete model" }
                    check(extracted.renameTo(target)) { "Cannot install extracted model" }
                    target.absolutePath
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } finally {
                    staging.deleteRecursively()
                }
            }
        }
}
