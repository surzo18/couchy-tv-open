package com.conreo.couchytv

import android.content.Context
import androidx.compose.runtime.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class NewsSource(val id:String,val url:String,val section:String,val source:String,val color:Int=0xFFD4DEE8.toInt(),val enabled:Boolean=true,val parser:String="rss")
object NewsSources {
    private var context:Context?=null
    var revision by mutableIntStateOf(0); private set
    var all by mutableStateOf(emptyList<NewsSource>()); private set
    var refreshMinutes by mutableIntStateOf(15); private set
    private fun defaults() = emptyList<NewsSource>()
    fun load(c:Context) {
        if(context!=null)return
        context=c.applicationContext
        val p=c.getSharedPreferences("news_sources",0)
        refreshMinutes=p.getInt("minutes",15).takeIf{it in listOf(5,15,30,60)} ?: 15
        all=runCatching {
            val raw=p.getString("sources",null) ?: return@runCatching defaults()
            val a=JSONArray(raw);require(a.length()<=20)
            (0 until a.length()).map { i->val j=a.getJSONObject(i)
                NewsSource(j.getString("id"),j.getString("url"),j.getString("section"),j.getString("name"),j.optInt("color",0xFFD4DEE8.toInt()),j.optBoolean("enabled",true),j.optString("parser","rss")).also{validate(it)} }
        }.getOrElse{DiagnosticLog.report("Správy","VAROVANIE","Nastavenia zdrojov sa nepodarilo načítať.");defaults()}
    }
    fun validUrl(s:String)=runCatching {val u=java.net.URI(s);u.scheme=="https" && !u.host.isNullOrBlank() && u.userInfo==null && u.fragment==null && s.length<=2000}.getOrDefault(false)
    private fun validate(s:NewsSource){require(s.parser == "rss");require(validUrl(s.url));require(s.source.trim().length in 1..60);require(s.section.length<=60)}
    private fun persist() {
        val a=JSONArray();all.forEach{s->a.put(JSONObject().put("id",s.id).put("url",s.url).put("section",s.section).put("name",s.source).put("color",s.color).put("enabled",s.enabled).put("parser",s.parser))}
        checkNotNull(context).getSharedPreferences("news_sources",0).edit().putString("sources",a.toString()).putInt("minutes",refreshMinutes).apply()
        revision++
    }
    fun save(s:NewsSource){validate(s);require(all.any{it.id==s.id} || all.size<20);all=all.filterNot{it.id==s.id}+s;persist()}
    fun remove(id:String){all=all.filterNot{it.id==id};persist()}
    fun reset(){all=defaults();persist()}
    fun interval(minutes:Int){require(minutes in listOf(5,15,30,60));refreshMinutes=minutes;persist()}
    fun newId()=UUID.randomUUID().toString()
}
