package com.kidslanguage.companion

import com.kidslanguage.companion.core.content.AssetContentRepository
import com.kidslanguage.companion.core.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ContentRepositoryTest {
    private fun item(id: String = "hello") = PracticeItem(id, "en-US", "greetings", text = "hello",
        image = "👋", distractors = listOf("🍎", "🌙"), audio = "audio/hello.mp3")

    @Test fun malformedSiblingDoesNotDiscardValidItems() = runTest {
        val reports = mutableListOf<String>()
        val body = "[${Json.encodeToString(item())},{\"id\":13}]"
        val repo = AssetContentRepository({ body }, { true }, reports::add)
        assertEquals(listOf("hello"), repo.getItems(LanguageCode.EN_US).map { it.id })
        assertTrue(reports.any { "entry 1" in it })
    }

    @Test fun invalidResourcesLanguageAndDuplicateChoicesAreIsolated() = runTest {
        val source = listOf(item(), item("foreign").copy(language = "zh-CN"),
            item("missing").copy(audio = "missing.mp3"), item("blank").copy(text = "！"),
            item("ambiguous").copy(distractors = listOf("👋", "🍎", "🍎")))
        val repo = AssetContentRepository({ Json.encodeToString(source) }, { it != "missing.mp3" })
        val loaded = repo.getItems(LanguageCode.EN_US)
        assertEquals(listOf("hello", "ambiguous"), loaded.map { it.id })
        assertEquals(listOf("repeat"), loaded.last().modes)
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
        val repo = AssetContentRepository({
            if (reads++ == 0) throw java.io.IOException("temporarily unavailable")
            Json.encodeToString(listOf(item()))
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
}
