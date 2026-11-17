package com.conreo.couchytv

import android.content.Context
import android.text.Html
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.xmlpull.v1.XmlPullParser
import java.net.URL
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.Locale

data class NewsItem(val title: String, val url: String, val section: String, val published: Long,val summary:String="",val image:String="",val source:String="RSS")

object PublicFeed {
    fun get(url: String): String {
        require(url.startsWith("https://"))
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 7000; connection.readTimeout = 7000
        connection.setRequestProperty("User-Agent", "CouchyTV/1.0 (personal RSS reader)")
        try {
            val status=connection.responseCode
            if(status!=200)throw java.io.IOException("HTTP $status")
            return connection.inputStream.use {
                val bytes = it.readBytesLimited(2_000_000)
                val headerCharset=Regex("charset\\s*=\\s*[\"']?([A-Za-z0-9._-]+)",RegexOption.IGNORE_CASE).find(connection.contentType.orEmpty())?.groupValues?.get(1)
                val declaration=String(bytes,0,minOf(bytes.size,256),Charsets.ISO_8859_1)
                val xmlCharset=Regex("<\\?xml[^>]*encoding\\s*=\\s*[\"']([A-Za-z0-9._-]+)",RegexOption.IGNORE_CASE).find(declaration)?.groupValues?.get(1)
                val charset=runCatching{java.nio.charset.Charset.forName(headerCharset ?: xmlCharset ?: "UTF-8")}.getOrDefault(Charsets.UTF_8)
                String(bytes, charset)
            }
        } catch(e:Exception){DiagnosticLog.report("Sieť · ${URL(url).host}","CHYBA","Zdroj sa nepodarilo načítať.",e);throw e}
        finally { connection.disconnect() }
    }
    private fun java.io.InputStream.readBytesLimited(max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) { val n = read(buffer); if (n < 0) break; require(out.size() + n <= max); out.write(buffer, 0, n) }
        return out.toByteArray()
    }
    fun plain(text: String) = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().replace(Regex("\\s+"), " ").trim()
}

object HomeNews {
    val sources get()=NewsSources.all.filter{it.enabled}.map{it.source}.distinct()
    private val gate=Mutex()
    fun sourceColor(source:String)=NewsSources.all.firstOrNull{it.source==source}?.color ?: 0xFFD4DEE8.toInt()
    fun imageAllowed(url:String)=NewsSources.validUrl(url)
    fun fetch(feed:NewsSource):List<NewsItem> {
        val body=PublicFeed.get(feed.url)
        return when(feed.parser){"360"->NewsWebSources.threeSixty(body);"tvnoviny"->NewsWebSources.tvNoviny(body);else->parse(body,feed.section,feed.source)}.map{it.copy(source=feed.source,section=feed.section)}
    }

    private var sourceRevision=-1
    private val seen = java.util.Collections.synchronizedSet(LinkedHashSet<String>())
    fun markSeen(url: String) = synchronized(seen) {
        seen.add(url)
        while (seen.size > 240) seen.remove(seen.first())
    }
    private var lastAttempt = 0L
    private var cache = emptyList<NewsItem>()
    fun enabled(context: Context) = context.getSharedPreferences("home_news", 0).getBoolean("enabled", true)
    fun setEnabled(context: Context, value: Boolean) { context.getSharedPreferences("home_news", 0).edit().putBoolean("enabled", value).apply() }
    suspend fun load(force:Boolean=false): List<NewsItem> = withContext(Dispatchers.IO) { gate.withLock {
        val version=NewsSources.revision
        if(sourceRevision!=version){cache=emptyList();lastAttempt=0L;sourceRevision=version}
        val now = System.currentTimeMillis()
        val retry=if(cache.isEmpty())60_000L else NewsSources.refreshMinutes*60_000L
        if (now-lastAttempt>retry || (force && now-lastAttempt>=30_000L) || lastAttempt == 0L || now < lastAttempt) {
            lastAttempt = now
            val network=Semaphore(2)
            val downloaded=java.util.concurrent.atomic.AtomicBoolean(false)
            val lists = try{NewsSources.all.filter{it.enabled}.map { feed -> async {
                network.withPermit {
                    try {
                        val items=fetch(feed)
                        if(items.isEmpty()){
                            DiagnosticLog.report("Správy · ${feed.source}","VAROVANIE","Zdroj neposkytol použiteľné články (${feed.section}). Zobrazujú sa dostupné uložené správy.")
                            cache.filter{it.source==feed.source && it.section==feed.section}
                        }else items.also{downloaded.set(true);DiagnosticLog.report("Správy · ${feed.source}","OK","Načítané položky: ${it.size} (${feed.section}).")}
                    }
                    catch(e:CancellationException){lastAttempt=0L;throw e}
                    catch(e:Exception){DiagnosticLog.report("Správy · ${feed.source}","CHYBA","Načítanie zdroja zlyhalo (${feed.section}). Zobrazujú sa dostupné uložené správy.",e);cache.filter{it.source==feed.source && it.section==feed.section}}
                }.filter{now-it.published in 0..48*3600_000L}.take(18)
            }}.awaitAll()}catch(e:CancellationException){lastAttempt=0L;throw e}
            if(version!=NewsSources.revision){lastAttempt=0L;cache=emptyList();return@withLock cache}
            cache = lists.flatten().distinctBy { it.url.substringBefore('?') }.shuffled().sortedBy { it.url in seen }.take(180)
            if(downloaded.get())RuntimeHealth.newsUpdated=now
            RuntimeHealth.newsCount=cache.size
            android.util.Log.i("CouchyNews", "Loaded ${cache.size} fresh headlines: ${cache.groupingBy{it.source}.eachCount()}")
        }
        cache.filter { now - it.published in 0..48 * 3600_000L }
    }}
    fun parse(xml: String, section: String,source:String="RSS"): List<NewsItem> {
        require(!xml.contains("<!DOCTYPE",ignoreCase=true))
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        parser.setInput(xml.reader())
        val result = mutableListOf<NewsItem>()
        var inside = false; var depth=0;var title = ""; var link = ""; var date = "";var summary="";var image=""
        while (parser.eventType != XmlPullParser.END_DOCUMENT && result.size < 100) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when {
                    parser.name in setOf("item","entry") -> { inside = true;depth=parser.depth; title = ""; link = ""; date = "";summary="";image="" }
                    inside && parser.depth==depth+1 && parser.name == "title" -> title = PublicFeed.plain(parser.nextText()).take(300)
                    inside && parser.depth==depth+1 && parser.name == "link" -> { val href=parser.getAttributeValue(null,"href"); val rel=parser.getAttributeValue(null,"rel"); if(href!=null){if(rel==null || rel=="alternate")link=href.trim()}else link=parser.nextText().trim() }
                    inside && parser.depth==depth+1 && parser.name in setOf("pubDate","published","updated") -> date = parser.nextText().trim()
                    inside && parser.depth==depth+1 && parser.name in setOf("description","summary") -> summary=PublicFeed.plain(parser.nextText()).take(600)
                    inside && (parser.name.substringAfter(':') in setOf("thumbnail","enclosure")) -> parser.getAttributeValue(null,"url")?.trim()?.takeIf{imageAllowed(it)}?.take(2000)?.let{image=it}
                    inside && parser.name.substringAfter(':')=="url" -> parser.nextText().trim().takeIf{imageAllowed(it)}?.take(2000)?.let{image=it}
                }
            } else if (parser.eventType == XmlPullParser.END_TAG && parser.name in setOf("item","entry")) {
                val time = runCatching { SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).parse(date)?.time }.getOrNull() ?: runCatching{java.time.OffsetDateTime.parse(date).toInstant().toEpochMilli()}.getOrNull() ?: 0
                val allowed=NewsSources.validUrl(link)
                if (title.isNotBlank() && allowed) result.add(NewsItem(title, link, section, time,summary,image,source))
                inside = false
            }
            parser.next()
        }
        return result.sortedByDescending { it.published }
    }
}
