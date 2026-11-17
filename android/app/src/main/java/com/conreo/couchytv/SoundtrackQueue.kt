package com.conreo.couchytv

/** Each wallpaper's tracks once in random order, then a shuffled library without immediate repeats. */
class SoundtrackQueue(private val choose: (List<String>) -> String = { it.random() }) {
    private var preferred = emptyList<String>()
    private var themedSession = false
    private val played = mutableSetOf<String>()
    private var last: String? = null
    fun markPlayed(id: String) { played.add(id); last = id }

    fun reset(ids: List<String>) {
        preferred = ids.distinct()
        themedSession = preferred.isNotEmpty()
        played.clear()
        last = null
    }

    fun next(available: List<String>, allowUnmatched: Boolean): String? {
        val all = available.distinct()
        val remainingThemes = preferred.filter { it in all && it !in played }
        val themed = if (remainingThemes.isEmpty()) null else choose(remainingThemes)
        val selected = if (themed != null) themed else {
            if (!themedSession && !allowUnmatched) return null
            // If a library has only the last track left, stop instead of looping it.
            val other = all.filter { it != last }
            if (other.isEmpty()) return null
            var candidates = other.filter { it !in played }
            if (candidates.isEmpty()) {
                // Never replay the theme queue: once exhausted, all further choices are random.
                preferred = emptyList()
                candidates = other
                played.clear()
            }
            choose(candidates)
        }
        played.add(selected)
        last = selected
        return selected
    }
}
