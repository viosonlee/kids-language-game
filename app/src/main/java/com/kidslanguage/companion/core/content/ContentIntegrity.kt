package com.kidslanguage.companion.core.content

import com.kidslanguage.companion.core.model.Category
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.PracticeItem
import com.kidslanguage.companion.core.model.PracticeMode

fun isAssetImage(image: String): Boolean = '/' in image || '\\' in image ||
    image.endsWith(".png", true) || image.endsWith(".webp", true) ||
    image.endsWith(".jpg", true) || image.endsWith(".jpeg", true)

private fun portableAsset(path: String): Boolean = path.isNotBlank() &&
    path.split('/').all { it != "." && it != ".." && it.matches(Regex("[A-Za-z0-9_.-]+")) }

private fun bitmapAsset(path: String): Boolean = portableAsset(path) &&
    path.substringAfterLast('.', "").lowercase() in setOf("png", "webp", "jpg", "jpeg")

internal fun validContentImage(image: String?, assetExists: (String) -> Boolean): Boolean =
    !image.isNullOrBlank() && (!isAssetImage(image) || (bitmapAsset(image) && assetExists(image)))

internal fun contentItemIssues(
    item: PracticeItem,
    language: LanguageCode,
    categories: List<Category>,
    assetExists: (String) -> Boolean,
): List<String> = buildList {
    if (item.id.isBlank()) add("missing ID")
    if (item.text.none { it.isLetterOrDigit() }) add("missing target text")
    if (!item.language.equals(language.code, true)) add("wrong language")
    if (categories.none { it.id == item.category && (it.language == "all" || it.language.equals(language.code, true)) }) {
        add("unknown category")
    }
    val audio = item.audio
    if (audio == null || !portableAsset(audio) || !assetExists(audio)) add("missing or invalid audio")
    if (!validContentImage(item.image, assetExists)) {
        add("missing or invalid image")
    }
}

/** Strict build-time checks; runtime parsing may still isolate/normalize a malformed sibling. */
fun validateContentBundle(
    categories: List<Category>,
    itemsByLanguage: Map<LanguageCode, List<PracticeItem>>,
    assetExists: (String) -> Boolean,
): List<String> = buildList {
    categories.groupBy { it.language.lowercase() to it.id }.filterValues { it.size > 1 }.forEach { (key, _) ->
        add("Duplicate category $key")
    }
    categories.forEach {
        if (it.id.isBlank() || it.title.isBlank() || it.icon.isBlank() ||
            (it.language != "all" && LanguageCode.entries.none { language -> language.code.equals(it.language, true) })) {
            add("Invalid category ${it.id}")
        }
    }
    itemsByLanguage.values.flatten().groupBy { it.id }.filterValues { it.size > 1 }.forEach { (id, _) ->
        add("Duplicate global item ID $id")
    }
    itemsByLanguage.forEach { (language, items) ->
        val validSiblings = items.filter { sibling ->
            contentItemIssues(sibling, language, categories, assetExists).isEmpty() &&
                sibling.modes.any { mode -> PracticeMode.entries.any { it.id == mode } }
        }.map { it.copy(language = language.code) }
        items.forEach { item ->
            contentItemIssues(item, language, categories, assetExists).forEach { add("${item.id}: $it") }
            if (item.modes.isEmpty() || item.modes.distinct().size != item.modes.size ||
                item.modes.any { mode -> PracticeMode.entries.none { it.id == mode } }) add("${item.id}: invalid modes")
            if (item.aliases.any { it.isBlank() } || item.aliases.distinct().size != item.aliases.size) add("${item.id}: invalid aliases")
            if ("choose" in item.modes && (item.distractors.any { it.isBlank() || it == item.image } ||
                item.distractors.distinct().size != item.distractors.size)) add("${item.id}: invalid distractors")
            if ("choose" in item.modes) {
                if (choiceDistractors(item.copy(language = language.code), validSiblings, 4).none { validContentImage(it, assetExists) }) {
                    add("${item.id}: no playable distractors")
                }
            }
            item.distractors.filter(::isAssetImage).forEach { image ->
                if (!validContentImage(image, assetExists)) add("${item.id}: missing distractor image")
            }
        }
    }
}
