package com.kidslanguage.companion

import com.kidslanguage.companion.core.content.*
import com.kidslanguage.companion.core.persistence.UserSettings
import com.kidslanguage.companion.core.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ContentRepositoryTest {
    private val categoriesJson = Json.encodeToString(listOf(Category("greetings", "all", "问候")))
    private fun assets(body: String): (String) -> String = { path ->
        if (path == "content/categories.json") categoriesJson else body
    }
    private fun item(id: String = "hello") = PracticeItem(id, "en-US", "greetings", text = "hello",
        image = "👋", distractors = listOf("🍎", "🌙"), audio = "audio/hello.mp3")

    @Test fun implicitSameCategoryChoicesSurviveRepositoryAndReachLessonPlanner() = runTest {
        val source = listOf(
            item("hello").copy(distractors = emptyList(), modes = listOf("choose")),
            item("apple").copy(image = "🍎", distractors = emptyList(), modes = listOf("choose")),
        )
        val repo = AssetContentRepository(assets(Json.encodeToString(source)), { true })
        val loaded = repo.getItems(LanguageCode.EN_US)
        assertEquals(listOf("hello", "apple"), loaded.map { it.id })
        assertEquals(listOf(PracticeMode.CHOOSE), repo.availableModes(LanguageCode.EN_US, "greetings"))
        val lesson = planLesson(loaded, null, PracticeMode.CHOOSE, UserSettings(choiceCount = 4))
        assertEquals(2, lesson.items.size)
        assertEquals(listOf("🍎"), choiceDistractors(loaded.first(), loaded, 4))
        assertTrue(validateContentBundle(listOf(Category("greetings", "all", "问候")),
            mapOf(LanguageCode.EN_US to source), { true }).isEmpty())
    }

    @Test fun missingCuratedImagesCanUseOnlyValidSameCategorySibling() = runTest {
        val source = listOf(
            item().copy(distractors = listOf("images/missing.png")),
            item("apple").copy(image = "🍎", modes = listOf("repeat")),
            item("broken").copy(image = "🌙", audio = "audio/missing.mp3"),
        )
        val repo = AssetContentRepository(assets(Json.encodeToString(source)), { "missing" !in it })
        val loaded = repo.getItems(LanguageCode.EN_US)
        assertEquals(2, loaded.size)
        assertTrue("choose" in loaded.first().modes)
        assertTrue(loaded.first().distractors.isEmpty())
        assertEquals(listOf("🍎"), choiceDistractors(loaded.first(), loaded, 4))
    }

    @Test fun unavailableAlternativesCannotKeepAnUnplayableChoiceMode() = runTest {
        val categories = listOf(Category("greetings", "all", "问候"), Category("other", "all", "其他"))
        val source = listOf(
            item().copy(distractors = emptyList()),
            item("same-image").copy(distractors = emptyList(), modes = listOf("choose")),
            item("other-category").copy(image = "🍎", category = "other", modes = listOf("repeat")),
            item("foreign").copy(image = "🌙", language = "zh-CN"),
            item("broken").copy(image = "🐶", audio = "audio/missing.mp3"),
            item("unknown-mode").copy(image = "🐱", modes = listOf("unknown")),
        )
        val repo = AssetContentRepository({ path ->
            when (path) {
                "content/categories.json" -> Json.encodeToString(categories)
                "content/lessons.json" -> "[]"
                else -> Json.encodeToString(source)
            }
        }, { "missing" !in it })
        val loaded = repo.getItems(LanguageCode.EN_US, "greetings")
        assertEquals(listOf("hello"), loaded.map { it.id })
        assertEquals(listOf("repeat"), loaded.single().modes)
        assertEquals(listOf(PracticeMode.REPEAT), repo.availableModes(LanguageCode.EN_US, "greetings"))
        val issues = validateContentBundle(categories, mapOf(LanguageCode.EN_US to source), { "missing" !in it })
        assertTrue(issues.any { it == "hello: no playable distractors" })
    }

    @Test fun malformedSiblingDoesNotDiscardValidItems() = runTest {
        val reports = mutableListOf<String>()
        val body = "[${Json.encodeToString(item())},{\"id\":13}]"
        val repo = AssetContentRepository(assets(body), { true }, reports::add)
        assertEquals(listOf("hello"), repo.getItems(LanguageCode.EN_US).map { it.id })
        assertTrue(reports.any { "entry 1" in it })
    }

    @Test fun invalidResourcesLanguageAndDuplicateChoicesAreIsolated() = runTest {
        val source = listOf(item(), item("foreign").copy(language = "zh-CN"),
            item("missing").copy(audio = "missing.mp3"), item("blank").copy(text = "！"),
            item("ambiguous").copy(distractors = listOf("👋", "🍎", "🍎")))
        val repo = AssetContentRepository(assets(Json.encodeToString(source)), { it != "missing.mp3" })
        val loaded = repo.getItems(LanguageCode.EN_US)
        assertEquals(listOf("hello", "ambiguous"), loaded.map { it.id })
        assertEquals(listOf("repeat", "choose"), loaded.last().modes)
        assertEquals(listOf("🍎"), loaded.last().distractors)
    }

    @Test fun categoriesFollowLanguageAndPlayableItemsRegardlessOfCallOrder() = runTest {
        val categories = listOf(Category("greetings", "all", "通用"),
            Category("greetings", "en-US", "英语问候"), Category("missing", "all", "空场景"),
            Category("greetings", "zh-CN", "中文问候"))
        val repo = AssetContentRepository({ path ->
            when (path) {
                "content/categories.json" -> Json.encodeToString(categories)
                "content/items_en.json" -> Json.encodeToString(listOf(item()))
                else -> "[]"
            }
        }, { true })
        assertEquals(listOf("英语问候"), repo.getCategories(LanguageCode.EN_US).map { it.title })
        assertTrue(repo.getCategories(LanguageCode.ZH_CN).isEmpty())
        assertEquals(1, repo.getCategories(LanguageCode.EN_US).size)
    }

    @Test fun transientReadFailureCanRecover() = runTest {
        var reads = 0
        val repo = AssetContentRepository({ path ->
            if (path == "content/categories.json") categoriesJson else {
                if (reads++ == 0) throw java.io.IOException("temporarily unavailable")
                Json.encodeToString(listOf(item()))
            }
        }, { true })
        assertTrue(repo.getItems(LanguageCode.EN_US).isEmpty())
        assertEquals(1, repo.getItems(LanguageCode.EN_US).size)
    }

    @Test fun cancellationIsNotConvertedToEmptyContent() = runTest {
        val repo = AssetContentRepository({ throw CancellationException("leaving") }, { true })
        try {
            repo.getItems(LanguageCode.EN_US)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    @Test fun bundledContentRemainsPlayableInAllLanguages() = runTest {
        val root = File("src/main/assets")
        val repo = AssetContentRepository({ File(root, it).readText() }, { File(root, it).let { f -> f.isFile && f.length() > 0 } })
        for (language in LanguageCode.entries) {
            val items = repo.getItems(language)
            assertTrue(items.size >= if (language == LanguageCode.JA_JP) 60 else 30)
            assertTrue(items.all { "repeat" in it.modes && "choose" in it.modes })
            assertEquals(4, repo.getCategories(language).size)
        }
    }

    @Test fun missingRequiredAssetsAndUnknownCategoriesAreSkippedWithoutLosingValidSibling() = runTest {
        val source = listOf(item(), item("no-audio").copy(audio = null), item("no-image").copy(image = " "),
            item("wrong-category").copy(category = "unknown"), item("missing-image").copy(image = "images/missing.png"),
            item("unsafe").copy(audio = "../hello.mp3"), item("bitmap").copy(image = "images/hello.png"))
        val reports = mutableListOf<String>()
        val repo = AssetContentRepository(assets(Json.encodeToString(source)), { it != "images/missing.png" }, reports::add)
        assertEquals(listOf("hello", "bitmap"), repo.getItems(LanguageCode.EN_US).map { it.id })
        assertEquals(5, reports.size)
    }

    @Test fun missingCategoryDefinitionsCanRecoverWithoutCachingEmptyItems() = runTest {
        var unavailable = true
        val repo = AssetContentRepository({ path ->
            if (path == "content/categories.json") {
                if (unavailable) throw java.io.IOException("not ready")
                categoriesJson
            } else Json.encodeToString(listOf(item()))
        }, { true })
        assertTrue(repo.getItems(LanguageCode.EN_US).isEmpty())
        unavailable = false
        assertEquals(listOf("hello"), repo.getItems(LanguageCode.EN_US).map { it.id })
    }
}
