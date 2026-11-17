package com.conreo.couchytv

import android.content.Context
import androidx.compose.runtime.*
import org.json.JSONObject
import java.util.Locale

/** Translates presentation only: persisted category IDs and media metadata never change. */
object AppLanguage {
    var code by mutableStateOf("sk"); private set
    private var dictionary=emptyMap<String,String>()
    private var english=EnglishText(emptyMap())
    fun load(c:Context) {
        code=c.getSharedPreferences("language",0).getString("code","sk").let{if(it=="en")"en" else "sk"}
        dictionary=runCatching{val j=JSONObject(c.assets.open("i18n/en.json").bufferedReader().use{it.readText()});j.keys().asSequence().associateWith{j.getString(it)}}.getOrDefault(emptyMap())
        english=EnglishText(dictionary)
    }
    fun set(c:Context,value:String) {require(value in listOf("sk","en"));c.getSharedPreferences("language",0).edit().putString("code",value).apply();code=value}
    fun locale():Locale=Locale.forLanguageTag(code)
    fun text(value:String):String {
        if(code!="en")return value
        return english.text(value)
    }
}
