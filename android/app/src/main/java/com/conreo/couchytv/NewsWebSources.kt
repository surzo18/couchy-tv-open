package com.conreo.couchytv

import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Bounded homepage metadata only; no article bodies or restricted video content. */
object NewsWebSources {
    fun threeSixty(html:String):List<NewsItem> {
        val raw=Regex("""<script\b[^>]*id="__NEXT_DATA__"[^>]*>(.*?)</script>""",RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1) ?: return emptyList()
        val rows=JSONObject(raw).optJSONObject("props")?.optJSONObject("pageProps")
            ?.optJSONObject("serverProps")?.optJSONObject("screenRows")?.optJSONArray("items") ?: return emptyList()
        val result=mutableListOf<NewsItem>()
        for(i in 0 until minOf(rows.length(),40)) {
            val tiles=rows.optJSONObject(i)?.optJSONObject("tiles")?.optJSONArray("items") ?: continue
            for(k in 0 until minOf(tiles.length(),100)) {
                val tile=tiles.optJSONObject(k) ?: continue
                if(!tile.optString("itemType").equals("video",ignoreCase=true))continue
                val path=tile.optString("path")
                if(!path.matches(Regex("/videos/[A-Za-z0-9_-]{1,100}")))continue
                val title=PublicFeed.plain(tile.optJSONObject("name")?.optString("sk").orEmpty()).take(300)
                val date=tile.optLong("created",0)
                val image=tile.optJSONObject("itemSpecificData")?.optJSONObject("assets")?.optJSONObject("cover")
                    ?.optJSONObject("@1")?.optString("background").orEmpty().takeIf{HomeNews.imageAllowed(it)}.orEmpty()
                if(title.isNotBlank() && date>0)result.add(NewsItem(title,"https://360tka.sk$path","Rozhovory",date,image=image,source="360tka.sk"))
                if(result.size>=100)return result.distinctBy{it.url}.sortedByDescending{it.published}
            }
        }
        return result.distinctBy{it.url}.sortedByDescending{it.published}
    }

    fun tvNoviny(html:String):List<NewsItem> {
        val times=Regex("""<time\b[^>]*dateTime="([^"]+)"[^>]*>""",RegexOption.IGNORE_CASE).findAll(html).take(100).toList()
        val heading=Regex("""<h3\b[^>]*>.*?<a\b[^>]*href="([^"#]+)"[^>]*>(.*?)</a>""",setOf(RegexOption.IGNORE_CASE,RegexOption.DOT_MATCHES_ALL))
        val imagePattern=Regex("""<img\b[^>]*\ssrc="([^"]+)""",RegexOption.IGNORE_CASE)
        val dateFormat=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return times.mapIndexedNotNull{index,time->
            val end=minOf(html.length,time.range.last+6000,times.getOrNull(index+1)?.range?.first ?: html.length)
            val match=heading.find(html.substring(time.range.last+1,end)) ?: return@mapIndexedNotNull null
            val path=match.groupValues[1]
            if(!path.startsWith("/") || path.startsWith("//") || !path.contains("/clanok/"))return@mapIndexedNotNull null
            val title=PublicFeed.plain(match.groupValues[2]).take(300)
            val date=runCatching{LocalDateTime.parse(time.groupValues[1],dateFormat).atZone(ZoneId.of("Europe/Bratislava")).toInstant().toEpochMilli()}.getOrNull() ?: return@mapIndexedNotNull null
            val previousEnd=times.getOrNull(index-1)?.range?.last ?: 0
            val before=html.substring(maxOf(previousEnd,time.range.first-12000),time.range.first)
            val image=imagePattern.findAll(before).lastOrNull()?.groupValues?.get(1)?.let{PublicFeed.plain(it)}
                ?.takeIf{HomeNews.imageAllowed(it)}.orEmpty()
            if(title.isBlank())null else NewsItem(title,"https://tvnoviny.sk$path","Aktuálne",date,image=image,source="TV Noviny")
        }.distinctBy{it.url}.sortedByDescending{it.published}
    }
}
