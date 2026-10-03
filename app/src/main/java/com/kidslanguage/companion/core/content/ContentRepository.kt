package com.kidslanguage.companion.core.content

import android.content.Context
import android.graphics.BitmapFactory
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
    suspend fun getLesson(language: LanguageCode, categoryId: String?): LessonConfig? = null
}

class AssetContentRepository(
    private val readAsset: (String) -> String,
    private val assetExists: (String) -> Boolean,
    private val report: (String) -> Unit = {}
) : ContentRepository {
    constructor(context: Context) : this(
        readAsset = { path -> context.assets.open(path).bufferedReader().use { it.readText() } },
        assetExists = { path ->
            try { context.assets.open(path).use {
                if (path.substringAfterLast('.', "").lowercase() in setOf("png", "webp", "jpg", "jpeg")) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(it, null, bounds)
                    bounds.outWidth > 0 && bounds.outHeight > 0
                } else it.read() != -1
            } }
            catch (_: java.io.IOException) { false }
        },
        report = { android.util.Log.w("ContentRepository", it) }
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedCategories: List<Category>? = null
    private var cachedLessons: List<LessonConfig>? = null
    private val cachedItems = mutableMapOf<LanguageCode, List<PracticeItem>>()
    private val cacheLock = Mutex()

    // Caller holds cacheLock. A read failure is not cached, so a later request can recover.
    private fun categoryDefinitions(): List<Category>? = cachedCategories ?: readEntries<Category>("content/categories.json")
        ?.filter { it.id.isNotBlank() && it.title.isNotBlank() && it.icon.isNotBlank() }
        ?.distinctBy { it.language.lowercase() to it.id }?.sortedBy { it.order }
        ?.also { cachedCategories = it }

    override suspend fun getLesson(language: LanguageCode, categoryId: String?): LessonConfig? = withContext(Dispatchers.IO) {
        if (categoryId == null || categoryId == "all") return@withContext null
        cacheLock.withLock {
            val lessons = cachedLessons ?: readEntries<LessonConfig>("content/lessons.json")
                ?.filter { config ->
                    config.isValid().also { if (!it) report("Skipping invalid lesson ${config.language}/${config.categoryId}") }
                }?.distinctBy { it.language to it.categoryId }?.also { cachedLessons = it }.orEmpty()
            lessons.firstOrNull { it.language == language.code && it.categoryId == categoryId }
        }
    }

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
            val categories = categoryDefinitions().orEmpty()
            categories.filter {
                (it.language == "all" || it.language.equals(language.code, ignoreCase = true)) &&
                    it.id in activeCategories
            }.sortedBy { if (it.language == "all") 1 else 0 }.distinctBy { it.id }.sortedBy { it.order }
        }
    }

    override suspend fun getItems(language: LanguageCode, categoryId: String?): List<PracticeItem> = withContext(Dispatchers.IO) {
        val list = cacheLock.withLock {
            cachedItems[language]?.let { return@withLock it }
            val categories = categoryDefinitions() ?: return@withLock emptyList()
            val fileName = when (language) {
                LanguageCode.JA_JP -> "content/items_ja.json"
                LanguageCode.ZH_CN -> "content/items_zh.json"
                LanguageCode.EN_US -> "content/items_en.json"
            }
            readEntries<PracticeItem>(fileName)?.mapNotNull { validate(it, language, categories) }
                ?.distinctBy { it.id }?.let { candidates ->
                    // Judge choice availability only after valid same-category siblings are known.
                    candidates.mapNotNull { item ->
                        if ("choose" !in item.modes || choiceDistractors(item, candidates, 2).isNotEmpty()) item
                        else {
                            report("Removed unplayable choose mode from ${item.id}")
                            item.copy(modes = item.modes - "choose").takeIf { it.modes.isNotEmpty() }
                        }
                    }
                }?.also { cachedItems[language] = it }.orEmpty()
        }

        if (categoryId != null && categoryId != "all") {
            list.filter { it.category == categoryId }
        } else {
            list
        }
    }

    private fun validate(item: PracticeItem, language: LanguageCode, categories: List<Category>): PracticeItem? {
        val issues = contentItemIssues(item, language, categories, assetExists)
        if (issues.isNotEmpty()) {
            report("Skipping item ${item.id}: ${issues.joinToString()}")
            return null
        }
        val distractors = item.distractors.filter {
            it != item.image && validContentImage(it, assetExists)
        }.distinct()
        val modes = item.modes.distinct().filter {
            it == "repeat" || it == "choose"
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
