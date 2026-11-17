package com.conreo.couchytv

import kotlinx.coroutines.*
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Premiere(val title:String,val date:String,val provider:String,val image:String,
    val description:String,val url:String,val source:String,val category:String,val trailer:String="",val rating:String="")

/** Documented TVmaze API only. Data: CC BY-SA; no publisher HTML scraping. */
object ApiPremieres {
    private val gate=kotlinx.coroutines.sync.Mutex()
    private var cached=emptyList<Premiere>()
    private var fetched=0L
    fun clearCache(){fetched=0;cached=emptyList()}
    fun dateLabel(date:String)=runCatching {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d. MMMM yyyy",AppLanguage.locale()))
    }.getOrDefault("Termín nepotvrdený")
    fun parseSchedule(raw:String):List<Premiere> {
        val entries=JSONArray(raw)
        return (0 until minOf(entries.length(),500)).mapNotNull { i ->
            val episode=entries.optJSONObject(i) ?: return@mapNotNull null
            if(episode.optInt("number")!=1)return@mapNotNull null
            val show=episode.optJSONObject("show") ?: episode.optJSONObject("_embedded")?.optJSONObject("show") ?: return@mapNotNull null
            if(show.optString("type") !in setOf("Scripted","Animation"))return@mapNotNull null
            val title=PublicFeed.plain(show.optString("name")).take(150)
            val url=show.optString("url")
            if(title.isBlank() || !url.startsWith("https://www.tvmaze.com/shows/"))return@mapNotNull null
            val date=runCatching{LocalDate.parse(episode.getString("airdate")).toString()}.getOrNull() ?: return@mapNotNull null
            val channel=(show.optJSONObject("webChannel") ?: show.optJSONObject("network"))?.optString("name").orEmpty()
            val score=show.optJSONObject("rating")?.optDouble("average",Double.NaN) ?: Double.NaN
            Premiere("$title · séria ${episode.optInt("season",1)}",date,channel.ifBlank{"TVmaze"},
                show.optJSONObject("image")?.optString("medium").orEmpty(),
                PublicFeed.plain(show.optString("summary").takeIf{it!="null"}.orEmpty()).take(4000),url,
                "TVmaze · CC BY-SA 4.0 · termín podľa amerického programu; miestna dostupnosť sa môže líšiť","Seriály",
                rating=if(score.isFinite() && score>0 && score<=10)"★ ${String.format(AppLanguage.locale(),"%.1f",score)} / 10 · TVmaze · celý seriál" else "")
        }
    }
    suspend fun all(onPartial:(List<Premiere>)->Unit={}):List<Premiere> {
        gate.lock()
        try {
            val now=System.currentTimeMillis()
            if(fetched>0 && now-fetched in 0 until 6*3600_000L){onPartial(cached);return cached}
            val today=LocalDate.now()
            val urls=(-3L..3L).flatMap { offset ->
                val date=today.plusDays(offset)
                listOf("https://api.tvmaze.com/schedule?country=US&date=$date","https://api.tvmaze.com/schedule/web?country=US&date=$date")
            }
            val rows=mutableListOf<Premiere>();var successes=0
            for(batch in urls.chunked(2)) {
                currentCoroutineContext().ensureActive()
                val parts=coroutineScope {batch.map { url -> async(Dispatchers.IO) {
                    try{parseSchedule(PublicFeed.get(url))}catch(e:CancellationException){throw e}catch(e:Exception){DiagnosticLog.report("Novinky","VAROVANIE","Časť programu TVmaze sa nepodarilo načítať.",e);null}
                }}.awaitAll()}
                successes+=parts.count{it!=null};rows+=parts.filterNotNull().flatten()
                onPartial(order(rows,today));delay(1200)
            }
            check(successes>0){"TVmaze nie je dostupné."}
            cached=order(rows,today);fetched=System.currentTimeMillis();RuntimeHealth.premieresUpdated=fetched
            return cached
        } finally {gate.unlock()}
    }
    private fun order(rows:List<Premiere>,today:LocalDate):List<Premiere> {
        val distinct=rows.distinctBy{it.url+it.title}.take(120)
        return distinct.filter{it.date<=today.toString()}.sortedByDescending{it.date}+
            distinct.filter{it.date>today.toString()}.sortedBy{it.date}
    }
    suspend fun detail(item:Premiere)=item
}
