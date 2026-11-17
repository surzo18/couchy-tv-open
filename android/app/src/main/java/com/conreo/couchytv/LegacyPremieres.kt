package com.conreo.couchytv

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object LegacyPremieres {
    suspend fun all(onPartial: (List<Premiere>) -> Unit = {}): List<Premiere> = coroutineScope {
        val pending = listOf("Kiná na Slovensku", "Seriály").map { category -> async {
            try { load(category).also{DiagnosticLog.report("Novinky · $category",if(it.isEmpty())"VAROVANIE" else "OK","Dostupné položky: ${it.size}.")} } catch (e: CancellationException) { throw e } catch (e: Exception) { DiagnosticLog.report("Novinky · $category","CHYBA","Načítanie prehľadu zlyhalo.",e);emptyList() }
        } }
        val rows = mutableListOf<Premiere>()
        for (task in pending) { rows += task.await(); if (rows.isNotEmpty()) onPartial(cinemaOrder(rows)) }
        check(rows.isNotEmpty())
        val today = LocalDate.now().toString()
        rows.filter { it.date <= today }.sortedWith(compareByDescending<Premiere> { it.date }.thenBy { it.title }) +
            rows.filter { it.date > today }.sortedWith(compareBy<Premiere> { it.date }.thenBy { it.title })
    }
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, List<Premiere>>>()
    fun clearCache() { cache.clear() }
    fun dateLabel(date: String): String = runCatching {
        val value = LocalDate.parse(date)
        value.format(DateTimeFormatter.ofPattern(if (value.year == LocalDate.now().year) "d. MMMM" else "d. MMMM yyyy", com.conreo.couchytv.AppLanguage.locale()))
    }.getOrDefault("Termín nepotvrdený")
    private fun cinemaOrder(rows: List<Premiere>): List<Premiere> {
        val today = LocalDate.now().toString()
        return rows.filter { it.date <= today }.sortedByDescending { it.date } + rows.filter { it.date > today }.sortedBy { it.date }
    }
    suspend fun load(category: String): List<Premiere> = withContext(Dispatchers.IO) {
        cache[category]?.takeIf { System.currentTimeMillis() - it.first in 0 until 6 * 3600_000 }?.let { return@withContext it.second }
        val data = when (category) {
            "Kiná na Slovensku" -> cinema()
            
            else -> seasonPremieres()
        }
        check(data.isNotEmpty()) { "Zdroj momentálne nevrátil žiadne položky." }
        cache[category] = System.currentTimeMillis() to data
        RuntimeHealth.premieresUpdated=System.currentTimeMillis()
        android.util.Log.i("CouchyPremieres", "$category: ${data.size} items")
        data
    }
    fun cinemaPage(html: String): List<Premiere> {
        val chunks = html.split("class=\"nectar-post-grid-item ")
        return chunks.drop(1).mapNotNull { chunk ->
            val url = Regex("class=\"nectar-post-grid-link\" href=\"([^\"]+)\"").find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
            val title = Regex("<h2[^>]*><span>(.*?)</span></h2>", RegexOption.DOT_MATCHES_ALL).find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
            val rawDate = Regex("data-key=\"premiera\">(.*?)</h4>").find(chunk)?.groupValues?.get(1).orEmpty()
            val numberDate = Regex("\\d{1,2}\\.\\d{1,2}\\.\\d{4}").find(rawDate)?.value
            val date = runCatching { LocalDate.parse(numberDate, DateTimeFormatter.ofPattern("d.M.yyyy")).toString() }.getOrNull() ?: return@mapNotNull null
            val day = LocalDate.parse(date)
            if (day < LocalDate.now().minusDays(21) || day > LocalDate.now().plusMonths(6)) return@mapNotNull null
            val image = Regex("<img[^>]* src=\"([^\"]+)\"").find(chunk)?.groupValues?.get(1).orEmpty()
            Premiere(PublicFeed.plain(title), date, "Slovenské kiná", image, "", url, "ITAFILM · slovenský distribútor", "Kiná na Slovensku")
        }.distinctBy { it.url }.sortedBy { it.date }
    }

    fun cinemaxPage(html: String): List<Premiere> = html.split("<div class=\"film-box\">").drop(1).mapNotNull { chunk ->
        val title = Regex("<h4><strong>(.*?)</strong></h4>", RegexOption.DOT_MATCHES_ALL).find(chunk)?.groupValues?.get(1)?.let(PublicFeed::plain) ?: return@mapNotNull null
        val genre = Regex("<p>(.*?)</p>").find(chunk)?.groupValues?.get(1).orEmpty().lowercase()
        if (title.contains("obnovená", true) || title.contains("výročie", true) || title.startsWith("ARTMAX", true) || genre.contains("koncert") || genre.contains("šport") || genre.contains("balet") || genre.contains("opera")) return@mapNotNull null
        val dateText = Regex("Premiéra: (\\d{1,2}\\.\\d{1,2}\\.\\d{4})").find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
        val date = runCatching { LocalDate.parse(dateText, DateTimeFormatter.ofPattern("d.M.yyyy")) }.getOrNull() ?: return@mapNotNull null
        if (date < LocalDate.now().minusDays(30) || date > LocalDate.now().plusMonths(6)) return@mapNotNull null
        val path = Regex("<a href='(/(?:filmy/film|akcie/akcia)/[^']+)'").find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
        val image = Regex("<img[^>]*src=\"([^\"]+)\"").find(chunk)?.groupValues?.get(1).orEmpty()
        Premiere(title.take(180), date.toString(), "Kino · CINEMAX SK", image, "", "https://www.cine-max.sk$path", "CINEMAX · termín podľa kina", "Kiná na Slovensku")
    }.distinctBy { it.url }.take(80)

    private suspend fun cinema(): List<Premiere> = coroutineScope {
        val ita = async { listOf("pripravujeme", "filmy-v-kinach").flatMap { part ->
            runCatching { cinemaPage(PublicFeed.get("https://www.itafilm.sk/$part/")) }.getOrDefault(emptyList())
        } }
        val max = async { runCatching { cinemaxPage(PublicFeed.get("https://www.cine-max.sk/filmy/pripravujeme")) }.getOrDefault(emptyList()) }
        // Prefer distributor dates if both sources list the same film.
        cinemaOrder((ita.await() + max.await()).distinctBy { it.title.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "") }).take(100)
    }

    data class SeasonStart(val showId: Int, val season: Int, val date: String)
    fun calendarPage(html: String): List<SeasonStart> {
        val markers = Regex("day date-(\\d{4}-\\d{2}-\\d{2})").findAll(html).toList()
        return markers.flatMapIndexed { index, marker ->
            val date = marker.groupValues[1]
            if (date < LocalDate.now().minusDays(60).toString() || date > LocalDate.now().plusDays(90).toString()) return@flatMapIndexed emptyList()
            val block = html.substring(marker.range.last + 1, markers.getOrNull(index + 1)?.range?.first ?: html.length)
            Regex("<li class=\"entry.*?</li>", RegexOption.DOT_MATCHES_ALL).findAll(block).mapNotNull { match ->
                val season = Regex(">(\\d+)x01</a>").find(match.value)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
                val id = Regex("href=\"/shows/(\\d+)/").find(match.value)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
                SeasonStart(id, season, date)
            }.toList()
        }.distinct()
    }
    fun seasonItem(start: SeasonStart, raw: String): Premiere? {
        val show = JSONObject(raw)
        if (show.optString("type") !in setOf("Scripted", "Animation")) return null
        val provider = (show.optJSONObject("webChannel") ?: show.optJSONObject("network"))?.optString("name").orEmpty()
        val image = show.optJSONObject("image")?.optString("medium").orEmpty()
        val title = PublicFeed.plain(show.optString("name")).take(150)
        val url = show.optString("url")
        if (title.isBlank() || !url.startsWith("https://www.tvmaze.com/shows/")) return null
        val average = show.optJSONObject("rating")?.optDouble("average", Double.NaN) ?: Double.NaN
        val rating = if (average.isFinite() && average > 0 && average <= 10) "★ ${String.format(com.conreo.couchytv.AppLanguage.locale(), "%.1f", average)} / 10 · TVmaze · celý seriál" else ""
        return Premiere("$title · séria ${start.season}", start.date, "$provider · medzinárodná premiéra", image,
            PublicFeed.plain(show.optString("summary").takeIf { it != "null" }.orEmpty()).take(4000), url,
            "TVmaze · CC BY-SA · dátum prvého vysielania; dostupnosť na Slovensku sa môže líšiť", "Seriály", rating = rating)
    }
    private suspend fun seasonPremieres(): List<Premiere> = coroutineScope {
        val today = LocalDate.now()
        val months = generateSequence(today.minusDays(60).withDayOfMonth(1)) { it.plusMonths(1) }
            .takeWhile { it <= today.plusDays(90) }.toList()
        val gate = Semaphore(3)
        val starts = months.map { month -> async {
            gate.withPermit { runCatching { calendarPage(PublicFeed.get("https://www.tvmaze.com/calendar?year=${month.year}&month=${month.monthValue}&filter=popular")) }.getOrDefault(emptyList()) }
        } }.awaitAll().flatten().distinct()
        // Cover both current seasons and future starts without an unbounded API crawl.
        val selected = (starts.filter { it.date <= today.toString() }.sortedByDescending { it.date }.take(64) +
            starts.filter { it.date > today.toString() }.sortedBy { it.date }.take(24))
        val result = mutableListOf<Premiere>()
        for (batch in selected.chunked(3)) {
            ensureActive()
            result += batch.map { start -> async {
                runCatching { seasonItem(start, PublicFeed.get("https://api.tvmaze.com/shows/${start.showId}")) }.getOrNull()
            } }.awaitAll().filterNotNull()
            delay(1600) // At most 3 calls per 1.6 seconds, below TVmaze's 20 calls / 10 seconds.
        }
        result
    }
    suspend fun detail(item: Premiere): Premiere = withContext(Dispatchers.IO) {
        if (item.category != "Kiná na Slovensku" || !(item.url.startsWith("https://www.itafilm.sk/film/") || item.url.startsWith("https://www.cine-max.sk/"))) return@withContext item
        runCatching {
            val html = PublicFeed.get(item.url)
            val description = Regex("<label>Popis:</label>\\s*<span[^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL).find(html)?.groupValues?.get(1)
                ?: Regex("<strong>O filme</strong>(.*?)</p>", RegexOption.DOT_MATCHES_ALL).find(html)?.groupValues?.get(1)
                ?: Regex("<meta[^>]*property=\"og:description\"[^>]*content=\"([^\"]*)\"").find(html)?.groupValues?.get(1)
                ?: Regex("<meta[^>]*name=\"description\"[^>]*content=\"([^\"]*)\"").find(html)?.groupValues?.get(1).orEmpty()
            val id = Regex("(?:youtube\\.com/(?:embed/|watch\\?v=)|youtu\\.be/)([A-Za-z0-9_-]{11})").find(html)?.groupValues?.get(1)
            item.copy(description = PublicFeed.plain(description).take(4000), trailer = id?.let { "https://www.youtube.com/watch?v=$it" }.orEmpty(), rating = movieRating(html))
        }.getOrDefault(item)
    }
    fun movieRating(html: String): String {
        fun visit(value: Any?, depth: Int): String? {
            if (depth > 6) return null
            if (value is JSONArray) return (0 until minOf(value.length(), 40)).firstNotNullOfOrNull { visit(value.opt(it), depth + 1) }
            if (value !is JSONObject) return null
            if (value.optString("@type") == "Movie") {
                val rating = value.optJSONObject("aggregateRating") ?: return null
                val score = rating.optDouble("ratingValue", Double.NaN)
                val best = rating.optDouble("bestRating", 5.0)
                if (score.isFinite() && best.isFinite() && score > 0 && best > 0 && score <= best) {
                    val count = rating.optInt("ratingCount", rating.optInt("reviewCount", 0))
                    return "★ $score / $best · hodnotenie zo stránky kina" + if (count > 0) " · $count hodnotení" else ""
                }
            }
            return value.opt("@graph")?.let { visit(it, depth + 1) }
        }
        return Regex("<script[^>]*type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
            .findAll(html).take(8).firstNotNullOfOrNull { match -> runCatching { visit(org.json.JSONTokener(match.groupValues[1]).nextValue(), 0) }.getOrNull() }.orEmpty()
    }
}
