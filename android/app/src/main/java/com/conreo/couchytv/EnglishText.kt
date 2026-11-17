package com.conreo.couchytv

/** Presentation-only translation with a bounded cache, including Canvas game labels. */
internal class EnglishText(private val dictionary: Map<String, String>) {
    private data class Template(val pattern: Regex, val replacement: String, val indices: List<String>)
    private val templates = dictionary.filterKeys { it.contains(Regex("\\{\\d+\\}")) }.map { (key, value) ->
        val slots = Regex("\\{(\\d+)\\}").findAll(key).toList()
        var start = 0
        val pattern = buildString {
            append('^')
            slots.forEach { slot -> append(Regex.escape(key.substring(start, slot.range.first))); append("([0-9]+(?:[.,][0-9]+)?)"); start = slot.range.last + 1 }
            append(Regex.escape(key.substring(start))); append('$')
        }
        Template(Regex(pattern), value, slots.map { it.groupValues[1] })
    }
    private val cache = object : LinkedHashMap<String, String>(128, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 512
    }
    @Synchronized fun text(value: String): String = cache[value] ?: render(value).also { cache[value] = it }
    private fun render(value: String): String {
        if (value.isBlank()) return value
        dictionary[value]?.let { return it }
        if (value.trim() != value) return value.takeWhile { it.isWhitespace() } + text(value.trim()) + value.takeLastWhile { it.isWhitespace() }
        templates.forEach { template -> template.pattern.matchEntire(value)?.let { match ->
            var result = template.replacement
            template.indices.forEachIndexed { i, index -> result = result.replace("{$index}", match.groupValues[i + 1]) }
            return result
        } }
        for (prefix in listOf("✓ ", "● ", "○ ", "▶ ", "♫ ", "› ")) if (value.startsWith(prefix)) return prefix + text(value.removePrefix(prefix))
        if (value.endsWith(" ›")) return text(value.removeSuffix(" ›")) + " ›"
        if (value.contains(" · ")) return value.split(" · ").joinToString(" · ") { text(it) }
        val colon = value.indexOf(':')
        if (colon > 0) dictionary[value.substring(0, colon)]?.let { return it + ":" + text(value.substring(colon + 1)) }
        return value
    }
}
