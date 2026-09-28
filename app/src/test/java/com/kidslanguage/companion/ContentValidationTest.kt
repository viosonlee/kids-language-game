package com.kidslanguage.companion

import com.kidslanguage.companion.core.model.Category
import com.kidslanguage.companion.core.model.PracticeItem
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContentValidationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `validate categories count and schema`() {
        val file = File("src/main/assets/content/categories.json")
        assertTrue("categories.json should exist", file.exists())
        val categories = json.decodeFromString<List<Category>>(file.readText())
        assertTrue("Should have at least 4 categories", categories.size >= 4)
    }

    @Test
    fun `validate Japanese items count reaches PRD threshold of at least 60`() {
        val file = File("src/main/assets/content/items_ja.json")
        assertTrue("items_ja.json should exist", file.exists())
        val items = json.decodeFromString<List<PracticeItem>>(file.readText())
        assertTrue("Japanese items must be >= 60, found ${items.size}", items.size >= 60)
    }

    @Test
    fun `validate Chinese items count reaches PRD threshold of at least 30`() {
        val file = File("src/main/assets/content/items_zh.json")
        assertTrue("items_zh.json should exist", file.exists())
        val items = json.decodeFromString<List<PracticeItem>>(file.readText())
        assertTrue("Chinese items must be >= 30, found ${items.size}", items.size >= 30)
    }

    @Test
    fun `validate English items count reaches PRD threshold of at least 30`() {
        val file = File("src/main/assets/content/items_en.json")
        assertTrue("items_en.json should exist", file.exists())
        val items = json.decodeFromString<List<PracticeItem>>(file.readText())
        assertTrue("English items must be >= 30, found ${items.size}", items.size >= 30)
    }

    @Test
    fun `validate total items across three languages exceeds 120`() {
        val ja = json.decodeFromString<List<PracticeItem>>(File("src/main/assets/content/items_ja.json").readText())
        val zh = json.decodeFromString<List<PracticeItem>>(File("src/main/assets/content/items_zh.json").readText())
        val en = json.decodeFromString<List<PracticeItem>>(File("src/main/assets/content/items_en.json").readText())
        val total = ja.size + zh.size + en.size
        assertTrue("Total items must be >= 120, found $total", total >= 120)
    }
}
