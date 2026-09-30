package com.kidslanguage.companion.core.content

import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.PracticeItem
import com.kidslanguage.companion.core.model.PracticeMode
import com.kidslanguage.companion.core.persistence.UserSettings
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class LessonConfig(
    val language: String,
    val categoryId: String,
    val itemIds: List<String> = emptyList(),
    val randomOrder: Boolean? = null,
    val itemCount: Int? = null,
    val modes: List<String> = listOf("repeat", "choose"),
    val optionCount: Int? = null,
) {
    fun isValid(): Boolean = LanguageCode.entries.any { it.code == language } &&
        categoryId.isNotBlank() && itemIds.all { it.isNotBlank() } && itemIds.distinct().size == itemIds.size &&
        modes.isNotEmpty() && modes.all { id -> PracticeMode.entries.any { it.id == id } } &&
        (itemCount == null || itemCount in 1..30) && (optionCount == null || optionCount in 2..4)
}

data class LessonPlan(val items: List<PracticeItem>, val choiceCount: Int)

fun planLesson(
    items: List<PracticeItem>,
    config: LessonConfig?,
    mode: PracticeMode,
    settings: UserSettings,
    random: Random = Random.Default,
): LessonPlan {
    val count = config?.optionCount ?: settings.choiceCount.coerceIn(2, 4)
    if (config != null && mode.id !in config.modes) return LessonPlan(emptyList(), count)
    val ordered = if (config?.itemIds.isNullOrEmpty()) items else {
        val byId = items.associateBy { it.id }
        config!!.itemIds.mapNotNull(byId::get)
    }
    val eligible = ordered.filter {
        mode.id in it.modes && (mode != PracticeMode.CHOOSE ||
            (!it.image.isNullOrBlank() && choiceDistractors(it, items, count).isNotEmpty()))
    }
    val sequence = if (config?.randomOrder ?: settings.shuffleItems) eligible.shuffled(random) else eligible
    // A lesson can be shorter than the parent's session limit, never longer.
    val limit = minOf(settings.sessionLength.coerceIn(1, 30), config?.itemCount ?: 30)
    return LessonPlan(sequence.take(limit), count)
}

suspend fun ContentRepository.availableModes(language: LanguageCode, categoryId: String): List<PracticeMode> {
    val items = getItems(language, categoryId)
    val config = getLesson(language, categoryId)
    return PracticeMode.entries.filter { planLesson(items, config, it, UserSettings()).items.isNotEmpty() }
}

fun choiceDistractors(item: PracticeItem, categoryItems: List<PracticeItem>, choiceCount: Int): List<String> =
    (item.distractors + categoryItems.filter {
        it.language == item.language && it.category == item.category
    }.mapNotNull { it.image })
        .filter { it.isNotBlank() && it != item.image }.distinct().take(choiceCount.coerceIn(2, 4) - 1)
