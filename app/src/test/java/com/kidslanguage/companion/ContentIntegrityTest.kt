package com.kidslanguage.companion

import com.kidslanguage.companion.core.content.validateContentBundle
import com.kidslanguage.companion.core.model.*
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ContentIntegrityTest {
    private val categories = listOf(Category("greetings", "all", "问候"))
    private val item = PracticeItem("hello", "en-US", "greetings", text = "hello", audio = "audio/hello.mp3",
        image = "images/hello.png", distractors = listOf("👋"))

    @Test fun duplicateIdsAcrossLanguagesFailTheBuildContract() {
        val errors = validateContentBundle(categories, mapOf(LanguageCode.EN_US to listOf(item),
            LanguageCode.ZH_CN to listOf(item.copy(language = "zh-CN", text = "你好"))), { true })
        assertEquals(listOf("Duplicate global item ID hello"), errors)
    }

    @Test fun invalidReferencesAndUnsupportedModesCannotPassAsCompleteContent() {
        val bad = item.copy(category = "unknown", audio = "../audio.mp3", image = "images/missing.png",
            modes = listOf("choose", "unknown"), aliases = listOf("hi", "hi"), distractors = listOf("images/missing.png"))
        val errors = validateContentBundle(categories, mapOf(LanguageCode.EN_US to listOf(bad)), { !it.contains("missing") })
        for (reason in listOf("unknown category", "missing or invalid audio", "missing or invalid image", "invalid modes",
            "invalid aliases", "invalid distractors", "missing distractor image")) {
            assertTrue("Expected $reason in $errors", errors.any { reason in it })
        }
    }

    @Test fun bundledItemsHaveUniqueIdsValidCategoriesModesAndAssets() {
        val root = File("src/main/assets")
        val json = Json { ignoreUnknownKeys = true }
        val categories = json.decodeFromString<List<Category>>(File(root, "content/categories.json").readText())
        val items = mapOf(LanguageCode.JA_JP to "ja", LanguageCode.ZH_CN to "zh", LanguageCode.EN_US to "en")
            .mapValues { (_, suffix) -> json.decodeFromString<List<PracticeItem>>(File(root, "content/items_$suffix.json").readText()) }
        val errors = validateContentBundle(categories, items) { File(root, it).let { f -> f.isFile && f.length() > 0 } }
        assertEquals(emptyList<String>(), errors)
    }

    @Test fun anExistingNonBitmapAssetCannotBeUsedAsAPracticeImage() {
        for (reference in listOf("audio/hello.mp3", "images/hello.svg", "images/../hello.png", "images\\hello.png")) {
            val bad = item.copy(image = reference, distractors = listOf(reference, "👋"))
            val errors = validateContentBundle(categories, mapOf(LanguageCode.EN_US to listOf(bad)), { true })
            assertTrue(reference, errors.any { "missing or invalid image" in it })
            assertTrue(reference, errors.any { "missing distractor image" in it })
        }
    }
}
