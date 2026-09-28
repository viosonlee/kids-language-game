package com.kidslanguage.companion.core.content

import android.content.Context
import com.kidslanguage.companion.core.model.Category
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.PracticeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface ContentRepository {
    suspend fun getCategories(language: LanguageCode): List<Category>
    suspend fun getItems(language: LanguageCode, categoryId: String? = null): List<PracticeItem>
    suspend fun getItemById(id: String): PracticeItem?
}

class AssetContentRepository(private val context: Context) : ContentRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedCategories: List<Category>? = null
    private val cachedItems = mutableMapOf<LanguageCode, List<PracticeItem>>()

    override suspend fun getCategories(language: LanguageCode): List<Category> = withContext(Dispatchers.IO) {
        cachedCategories?.let { return@withContext it }

        val loaded = try {
            val content = context.assets.open("content/categories.json").bufferedReader().use { it.readText() }
            json.decodeFromString<List<Category>>(content).sortedBy { it.order }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
        cachedCategories = loaded
        loaded
    }

    override suspend fun getItems(language: LanguageCode, categoryId: String?): List<PracticeItem> = withContext(Dispatchers.IO) {
        val list = cachedItems.getOrPut(language) {
            val fileName = when (language) {
                LanguageCode.JA_JP -> "content/items_ja.json"
                LanguageCode.ZH_CN -> "content/items_zh.json"
                LanguageCode.EN_US -> "content/items_en.json"
            }
            try {
                val content = context.assets.open(fileName).bufferedReader().use { it.readText() }
                json.decodeFromString<List<PracticeItem>>(content)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

        if (categoryId != null && categoryId != "all") {
            list.filter { it.category == categoryId }
        } else {
            list
        }
    }

    override suspend fun getItemById(id: String): PracticeItem? = withContext(Dispatchers.IO) {
        for (lang in LanguageCode.entries) {
            val item = getItems(lang).firstOrNull { it.id == id }
            if (item != null) return@withContext item
        }
        null
    }
}
