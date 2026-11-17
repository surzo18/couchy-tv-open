package com.conreo.couchytv

import android.content.Context
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

/** Distribution-specific media/presets; all behavior remains in the shared source. */
object EditionConfig {
    private var data=JSONObject()
    fun load(c:Context) { data=runCatching{JSONObject(c.assets.open("edition.json").bufferedReader().use{it.readText()})}.getOrDefault(JSONObject()) }
    val collections get()=(data.optJSONArray("collections") ?: JSONArray()).let{a->(0 until a.length()).map{a.getString(it)}}.ifEmpty{listOf("Všetky","Vlastné")}
    val wallpapers get()=(data.optJSONArray("wallpapers") ?: JSONArray()).let{a->(0 until minOf(a.length(),64)).mapNotNull{i->
        val j=a.optJSONObject(i) ?: return@mapNotNull null
        val file=j.optString("file");if(!file.matches(Regex("[A-Za-z0-9._ -]+\\.mp4")))return@mapNotNull null
        Triple(file,j.optString("title",file).take(80),j.optString("collection","Vlastné"))}}
    val cyclingVideoUri get()=data.optString("cyclingVideoUri","asset:///wallpapers/Aurora.mp4")
    fun isCyclingUri(uri:String)=uri==cyclingVideoUri
    private fun photoPath(key:String,fallback:String)=data.optString(key,fallback).takeIf{it.matches(Regex("Pictures/[A-Za-z0-9_-]+/"))} ?: fallback
    val albumActive get()=photoPath("albumActive","Pictures/Couchy-Album/")
    val albumHidden get()=photoPath("albumHidden","Pictures/Couchy-Hidden/")
    val newsSources get()=data.optJSONArray("newsSources") ?: JSONArray()
    val premiereSources get()=(data.optJSONArray("premiereSources") ?: JSONArray()).let{a->(0 until a.length()).map{a.getString(it)}.toSet()}
    val startupTitle get()=data.optString("startupTitle","Couchy").take(40)
    fun startupFile(c:Context):File? {
        val name=data.optString("startupSound","game-audio/welcome.wav")
        if(name.startsWith("external:")) {
            val root=c.getExternalFilesDir(null) ?: return null
            val file=File(root,name.removePrefix("external:")).canonicalFile
            return file.takeIf{it.path.startsWith(root.canonicalPath+File.separator) && it.isFile}
        }
        if(name!="game-audio/welcome.wav")return null
        return File(c.filesDir,"welcome.wav").also{target->if(!target.exists())c.assets.open(name).use{input->target.outputStream().use{input.copyTo(it)}}}
    }
}
