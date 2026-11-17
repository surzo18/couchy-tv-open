package com.conreo.couchytv

import android.content.Context
import org.json.JSONObject
import org.json.JSONArray

data class PhotoNotes(val album:String="default",val caption:String="",val date:String="",val place:String="")
object AlbumStore {
    private fun prefs(c:Context)=c.getSharedPreferences("photo_albums",0)
    @Volatile private var seeds:JSONObject?=null
    @Synchronized fun load(c:Context) {
        if(seeds==null)seeds=runCatching{JSONObject(c.assets.open("photo-notes.json").bufferedReader().use{it.readText()})}.getOrDefault(JSONObject())
        if(!prefs(c).contains("albums"))prefs(c).edit().putString("albums",JSONObject().put("default","Moje fotografie").toString()).apply()
    }
    fun albums(c:Context):Map<String,String> {load(c);val j=JSONObject(prefs(c).getString("albums","{}")!!);return j.keys().asSequence().associateWith{j.getString(it)}}
    fun name(c:Context,id:String)=albums(c)[id] ?: "Nezaradené"
    fun create(c:Context,name:String):String {val id=java.util.UUID.randomUUID().toString();rename(c,id,name);return id}
    fun rename(c:Context,id:String,name:String){require(name.trim().isNotEmpty());val all=JSONObject(albums(c));all.put(id,name.trim().take(60));prefs(c).edit().putString("albums",all.toString()).apply()}
    fun notes(c:Context,p:AlbumPhoto):PhotoNotes {
        load(c)
        val j=prefs(c).getString("photo:${p.uri}",null)?.let{runCatching{JSONObject(it)}.getOrNull()} ?: seeds?.optJSONObject(p.name) ?: JSONObject()
        return PhotoNotes(j.optString("album","default"),j.optString("caption"),j.optString("date",p.date),j.optString("place"))
    }
    fun save(c:Context,p:AlbumPhoto,n:PhotoNotes){prefs(c).edit().putString("photo:${p.uri}",JSONObject().put("album",n.album).put("caption",n.caption.take(240)).put("date",n.date.take(10)).put("place",n.place.take(120)).toString()).apply()}
    fun selection(c:Context):Set<String>?=prefs(c).getStringSet("selection",null)?.toSet()
    fun choose(c:Context,ids:Set<String>?){val e=prefs(c).edit();if(ids==null)e.remove("selection") else e.putStringSet("selection",ids);e.apply()}
    fun showInfo(c:Context)=prefs(c).getBoolean("info",true)
    fun setInfo(c:Context,v:Boolean){prefs(c).edit().putBoolean("info",v).apply()}
    fun portal(c:Context)=prefs(c).getBoolean("portal",true)
    @Synchronized fun nextMemory(c:Context,photos:List<AlbumPhoto>):AlbumPhoto? {
        if(photos.isEmpty())return null
        val byId=photos.associateBy{it.uri.toString()}
        val saved=prefs(c)
        val choice=MemoryRotation.next(byId.keys,saved.getStringSet("portal_seen",emptySet()).orEmpty(),saved.getString("portal_last",""))?:return null
        saved.edit().putStringSet("portal_seen",choice.seen).putString("portal_last",choice.selected).apply()
        return byId[choice.selected]
    }
    fun setPortal(c:Context,v:Boolean){prefs(c).edit().putBoolean("portal",v).apply()}
    fun portalInterval(c:Context)=prefs(c).getInt("portal_interval",5).let{if(it in listOf(2,5,10,15))it else 5}
    fun portalDuration(c:Context)=prefs(c).getInt("portal_duration",12).let{if(it in listOf(8,12,20))it else 12}
    fun setPortalTiming(c:Context,interval:Int=portalInterval(c),duration:Int=portalDuration(c)){
        require(interval in listOf(2,5,10,15) && duration in listOf(8,12,20))
        prefs(c).edit().putInt("portal_interval",interval).putInt("portal_duration",duration).apply()
    }
    fun settings(c:Context){
        val intents=listOf(android.content.Intent(android.provider.Settings.ACTION_DREAM_SETTINGS),android.content.Intent().setClassName("com.android.tv.settings","com.android.tv.settings.device.display.daydream.DaydreamActivity"))
        for(i in intents)if(runCatching{c.startActivity(i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))}.isSuccess)return
        Actions.toast(c,"Nastavenia šetriča nie sú dostupné.")
    }
    fun eligible(c:Context,photos:List<AlbumPhoto>):List<AlbumPhoto>{val ids=selection(c);return photos.filter{it.active && (ids==null || notes(c,it).album in ids)}}
    fun map(c:Context,place:String){if(place.isBlank())return;val uri=android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query="+android.net.Uri.encode(place));runCatching{c.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,uri).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))}.onFailure{Actions.toast(c,"Mapu sa nepodarilo otvoriť.")}}
}
