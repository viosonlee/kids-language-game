package com.kidslanguage.companion.core.content

import android.content.Context
import com.kidslanguage.companion.core.model.Category
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.PracticeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

interface ContentRepository {
    suspend fun getCategories(language: LanguageCode): List<Category>
    suspend fun getItems(language: LanguageCode, categoryId: String? = null): List<PracticeItem>
    suspend fun getItemById(id: String): PracticeItem?
}

class AssetContentRepository(
    private val readAsset: (String) -> String,
    private val assetExists: (String) -> Boolean,
    private val report: (String) -> Unit = {}
) : ContentRepository {
    constructor(context: Context) : this(
        readAsset = { path -> context.assets.open(path).bufferedReader().use { it.readText() } },
        assetExists = { path ->
            try { context.assets.open(path).use { it.read() != -1 } }
            catch (_: java.io.IOException) { false }
        },
        report = { android.util.Log.w("ContentRepository", it) }
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedCategories: List<Category>? = null
    private val cachedItems = mutableMapOf<LanguageCode, List<PracticeItem>>()
    private val cacheLock = Mutex()

    private inline fun <reified T> readEntries(path: String): List<T>? {
        return try {
            val array = json.parseToJsonElement(readAsset(path)) as? JsonArray
                ?: error("Expected array")
            array.mapIndexedNotNull { index, element ->
                try { json.decodeFromJsonElement<T>(element) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { report("Skipping invalid entry $index in $path"); null }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            report("Unable to load $path")
            null // A transient read failure must not poison the cache.
        }
    }

    override suspend fun getCategories(language: LanguageCode): List<Category> = withContext(Dispatchers.IO) {
        val activeCategories = getItems(language).map { it.category }.toSet()
        cacheLock.withLock {
            val categories = cachedCategories ?: readEntries<Category>("content/categories.json")
                ?.filter { it.id.isNotBlank() && it.title.isNotBlank() }
                ?.distinctBy { it.language to it.id }?.sortedBy { it.order }
                ?.also { cachedCategories = it }.orEmpty()
            categories.filter {
                (it.language == "all" || it.language.equals(language.code, ignoreCase = true)) &&
                    it.id in activeCategories
            }.sortedBy { if (it.language == "all") 1 else 0 }.distinctBy { it.id }.sortedBy { it.order }
        }
    }

    override suspend fun getItems(language: LanguageCode, categoryId: String?): List<PracticeItem> = withContext(Dispatchers.IO) {
        val list = cacheLock.withLock {
            cachedItems[language]?.let { return@withLock it }
            val fileName = when (language) {
                LanguageCode.JA_JP -> "content/items_ja.json"
                LanguageCode.ZH_CN -> "content/items_zh.json"
                LanguageCode.EN_US -> "content/items_en.json"
            }
            readEntries<PracticeItem>(fileName)?.mapNotNull { validate(it, language) }
                ?.distinctBy { it.id }?.also { cachedItems[language] = it }.orEmpty()
        }

        if (categoryId != null && categoryId != "all") {
            list.filter { it.category == categoryId }
        } else {
            list
        }
    }

    private fun validate(item: PracticeItem, language: LanguageCode): PracticeItem? {
        if (item.id.isBlank() || item.category.isBlank() || item.text.none { it.isLetterOrDigit() } ||
            !item.language.equals(language.code, ignoreCase = true)) {
            report("Skipping invalid item ${item.id} for ${language.code}")
            return null
        }
        val audio = item.audio
        if (audio != null && (audio.isBlank() || audio.startsWith('/') || '\\' in audio ||
                audio.split('/').any { it == ".." } || !assetExists(audio))) {
            report("Skipping item ${item.id}: missing or invalid audio")
            return null
        }
        val distractors = item.distractors.filter { it.isNotBlank() && it != item.image }.distinct()
        val modes = item.modes.distinct().filter {
            it == "repeat" || (it == "choose" && !item.image.isNullOrBlank() && distractors.size >= 2)
        }
        if (modes != item.modes) report("Removed unsupported or invalid modes from ${item.id}")
        return if (modes.isEmpty()) null else item.copy(
            language = language.code, modes = modes, distractors = distractors,
            aliases = item.aliases.filter { it.isNotBlank() }.distinct()
        )
    }

    override suspend fun getItemById(id: String): PracticeItem? = withContext(Dispatchers.IO) {
        for (lang in LanguageCode.entries) {
            val item = getItems(lang).firstOrNull { it.id == id }
            if (item != null) return@withContext item
        }
        null
    }
}
