package com.kidslanguage.companion

import com.kidslanguage.companion.core.content.*
import com.kidslanguage.companion.core.model.*
import com.kidslanguage.companion.core.persistence.UserSettings
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class LessonPlanTest {
    private val items = (1..8).map {
        PracticeItem("item-$it", "en-US", "greetings", text = "word $it", image = "image-$it")
    }

    @Test fun explicitOrderModeAndCountAreRespectedWithinParentLimit() {
        val config = LessonConfig("en-US", "greetings", listOf("item-4", "item-2", "item-7"),
            randomOrder = false, itemCount = 2, modes = listOf("repeat"), optionCount = 4)
        val settings = UserSettings(sessionLength = 10, shuffleItems = true)
        val plan = planLesson(items, config, PracticeMode.REPEAT, settings)
        assertEquals(listOf("item-4", "item-2"), plan.items.map { it.id })
        assertEquals(4, plan.choiceCount)
        assertEquals(1, planLesson(items, config, PracticeMode.REPEAT, settings.copy(sessionLength = 1)).items.size)
        assertTrue(planLesson(items, config, PracticeMode.CHOOSE, settings).items.isEmpty())
    }

    @Test fun randomOrderIsReproducibleAndKeepsExactlyTheEligibleItems() {
        val settings = UserSettings(sessionLength = 30, shuffleItems = true)
        val first = planLesson(items, null, PracticeMode.REPEAT, settings, Random(123)).items
        assertEquals(items.toSet(), first.toSet())
        assertNotEquals(items, first)
        assertEquals(first, planLesson(items, null, PracticeMode.REPEAT, settings, Random(123)).items)
        assertEquals(items, planLesson(items, null, PracticeMode.REPEAT, settings.copy(shuffleItems = false)).items)
    }

    @Test fun choicesUseTwoToFourDistinctImagesWithoutInventingFallbackPictures() {
        val item = items.first().copy(distractors = listOf("image-1", "image-2", "image-2", ""))
        assertEquals(listOf("image-2"), choiceDistractors(item, items, 2))
        assertEquals(listOf("image-2", "image-3", "image-4"), choiceDistractors(item, items, 4))
        assertEquals(listOf("image-2"), choiceDistractors(item, listOf(item), 4))
        assertTrue(choiceDistractors(items.first(), listOf(items.first()), 3).isEmpty())
    }

    @Test fun removedOrUnavailableItemsCannotLeakIntoConfiguredMode() {
        val config = LessonConfig("en-US", "greetings", listOf("missing", "item-1", "item-2"))
        val content = items.map { if (it.id == "item-1") it.copy(modes = listOf("repeat")) else it }
        assertEquals(listOf("item-2"), planLesson(content, config, PracticeMode.CHOOSE, UserSettings()).items.map { it.id })
    }

    @Test fun invalidChoiceQuestionsAreSkippedBeforeApplyingSessionLimit() {
        val content = listOf(items[0].copy(image = null), items[1], items[2])
        val plan = planLesson(content, null, PracticeMode.CHOOSE, UserSettings(sessionLength = 1))
        assertEquals(listOf("item-2"), plan.items.map { it.id })
        assertTrue(planLesson(listOf(items[0]), null, PracticeMode.CHOOSE, UserSettings()).items.isEmpty())
        assertTrue(choiceDistractors(items[0], listOf(items[1].copy(category = "animals"),
            items[2].copy(language = "zh-CN")), 4).isEmpty())
    }

    @Test fun repositoryIsolatesInvalidLessonConfigAndFiltersUnavailableMode() = runTest {
        val configs = listOf(LessonConfig("en-US", "greetings", modes = listOf("repeat")),
            LessonConfig("en-US", "invalid", optionCount = 7))
        val reports = mutableListOf<String>()
        val repo = AssetContentRepository({ path -> when (path) {
            "content/lessons.json" -> Json.encodeToString(configs)
            "content/items_en.json" -> Json.encodeToString(items)
            else -> "[]"
        } }, { true }, reports::add)
        assertNull(repo.getLesson(LanguageCode.EN_US, "invalid"))
        assertEquals(listOf(PracticeMode.REPEAT), repo.availableModes(LanguageCode.EN_US, "greetings"))
        assertTrue(reports.any { "invalid lesson" in it })
    }

    @Test fun everyBundledLessonReferencesRealItemsInItsLanguageAndCategory() = runTest {
        val root = File("src/main/assets")
        val configs = Json.decodeFromString<List<LessonConfig>>(File(root, "content/lessons.json").readText())
        val repo = AssetContentRepository({ File(root, it).readText() }, { File(root, it).isFile })
        assertEquals(12, configs.size)
        for (config in configs) {
            assertTrue(config.isValid())
            val language = LanguageCode.fromCode(config.language)
            val content = repo.getItems(language, config.categoryId)
            assertEquals(content.map { it.id }.toSet(), config.itemIds.toSet())
            assertEquals(config, repo.getLesson(language, config.categoryId))
            assertEquals(PracticeMode.entries, repo.availableModes(language, config.categoryId))
        }
    }
}
