package com.conreo.couchytv

import android.content.Context
import androidx.compose.runtime.*
import org.json.JSONObject
import java.util.Locale

/** Translates presentation only: persisted category IDs and media metadata never change. */
object AppLanguage {
    var code by mutableStateOf("sk"); private set
    private var dictionary=emptyMap<String,String>()
    fun load(c:Context) {
        code=c.getSharedPreferences("language",0).getString("code","sk").let{if(it=="en")"en" else "sk"}
        dictionary=runCatching{val j=JSONObject(c.assets.open("i18n/en.json").bufferedReader().use{it.readText()});j.keys().asSequence().associateWith{j.getString(it)}}.getOrDefault(emptyMap())
    }
    fun set(c:Context,value:String) {require(value in listOf("sk","en"));c.getSharedPreferences("language",0).edit().putString("code",value).apply();code=value}
    fun locale():Locale=Locale.forLanguageTag(code)
    fun text(value:String):String {
        if(code!="en")return value
        dictionary[value]?.let{return it}
        // Selection indicators and status suffixes keep their data unchanged.
        for(prefix in listOf("✓ ","● ","○ "))if(value.startsWith(prefix))return prefix+text(value.removePrefix(prefix))
        if(value.contains(" · "))return value.split(" · ").joinToString(" · "){text(it)}
        val colon=value.indexOf(':')
        if(colon>0)dictionary[value.substring(0,colon)]?.let{return it+value.substring(colon)}
        return value
    }
}
