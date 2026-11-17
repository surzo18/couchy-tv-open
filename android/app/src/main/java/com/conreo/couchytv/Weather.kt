package com.conreo.couchytv

import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.URLEncoder

data class WeatherPlace(val name:String,val latitude:Double,val longitude:Double,val detail:String="",val automatic:Boolean=false)
data class WeatherDay(val date:String,val code:Int,val high:Double,val low:Double,val rain:Int)
data class WeatherHour(val time:String,val temperature:Double?,val feels:Double?,val rain:Int?,val amount:Double?,val wind:Double?,val code:Int,val daytime:Boolean)
data class WeatherData(val place:WeatherPlace,val temperature:Double,val feels:Double,val wind:Double,val code:Int,val daytime:Boolean,val days:List<WeatherDay>,val fetched:Long,val hours:List<WeatherHour> = emptyList(),val timezone:String="Europe/Bratislava")
object Weather {
    var imperial by mutableStateOf(false); private set
    fun setImperial(c:Context,value:Boolean){imperial=value;prefs(c).edit().putBoolean("imperial",value).apply()}
    fun temperature(celsius:Double,unit:Boolean=true):String {
        val n=kotlin.math.round(WeatherUnits.temperature(celsius,imperial)).toInt()
        return "$n°"+if(unit){if(imperial)"F" else "C"}else ""
    }
    fun wind(kmh:Double)=kotlin.math.round(WeatherUnits.wind(kmh,imperial)).toInt().toString()+if(imperial)" mph" else " km/h"

    var enabled by mutableStateOf(false);private set
    var place by mutableStateOf<WeatherPlace?>(null);private set
    var data by mutableStateOf<WeatherData?>(null);private set
    var busy by mutableStateOf(false);private set
    var error by mutableStateOf("");private set
    private var loaded=false
    private var attempted=0L
    private val gate=Mutex()
    private fun prefs(c:Context)=c.getSharedPreferences("weather",0)
    fun load(c:Context) {
        if(loaded)return
        loaded=true;val p=prefs(c);enabled=p.getBoolean("enabled",false);imperial=p.getBoolean("imperial",false)
        place=runCatching{val j=JSONObject(p.getString("place","").orEmpty());WeatherPlace(j.getString("name"),j.getDouble("lat"),j.getDouble("lon"),j.optString("detail"),j.optBoolean("auto"))}.getOrNull()?.takeIf{valid(it)}
    }
    private fun valid(p:WeatherPlace)=p.name.isNotBlank() && p.latitude.isFinite() && p.longitude.isFinite() && p.latitude in -90.0..90.0 && p.longitude in -180.0..180.0
    fun setEnabled(c:Context,value:Boolean){enabled=value;prefs(c).edit().putBoolean("enabled",value).apply()}
    fun select(c:Context,p:WeatherPlace) {
        require(valid(p));place=p;data=null;attempted=0;error=""
        prefs(c).edit().putString("place",JSONObject().put("name",p.name.take(100)).put("lat",p.latitude).put("lon",p.longitude).put("detail",p.detail.take(150)).put("auto",p.automatic).toString()).apply()
    }
    suspend fun auto(c:Context) { throw IllegalStateException("Vyber mesto ručne. Odhad podľa IP je v tejto verzii vypnutý.") }
    suspend fun refresh(c:Context,force:Boolean=false)=gate.withLock {
        load(c)
        val now=System.currentTimeMillis()
        if(!force && now-attempted in 0 until 30*60_000L && attempted>0)return@withLock
        if(force && now-attempted in 0 until 30_000L && attempted>0)return@withLock
        attempted=now;busy=true;error=""
        var cancelled=false
        try {
            val target=place ?: throw IllegalStateException("Vyber mesto alebo použi odhad polohy.")
            val result=withContext(Dispatchers.IO){
                val url="https://api.open-meteo.com/v1/forecast?latitude=${target.latitude}&longitude=${target.longitude}&current=temperature_2m,apparent_temperature,is_day,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&hourly=temperature_2m,apparent_temperature,precipitation_probability,precipitation,wind_speed_10m,weather_code,is_day&forecast_days=5&timezone=auto"
                val j=JSONObject(PublicFeed.get(url));parse(j,target,now)
            }
            if(place==target){data=result;DiagnosticLog.report("Počasie","OK","Predpoveď sa obnovila.")}
        } catch(e:CancellationException){cancelled=true;throw e}
        catch(e:Exception){DiagnosticLog.report("Počasie","CHYBA","Predpoveď sa nepodarilo obnoviť.",e);if(data?.let{now-it.fetched>3*3600_000L}==true)data=null;error=if(place==null)"Polohu sa nepodarilo určiť. Vyber mesto ručne." else "Počasie sa nepodarilo obnoviť. Skús to neskôr."}
        finally{busy=false;attempted=if(cancelled)0L else System.currentTimeMillis()}
    }
    fun parse(j:JSONObject,p:WeatherPlace,now:Long):WeatherData {
        val current=j.getJSONObject("current");val daily=j.getJSONObject("daily");val dates=daily.getJSONArray("time")
        val temp=current.getDouble("temperature_2m");require(temp.isFinite())
        val days=(0 until minOf(5,dates.length())).map { i -> WeatherDay(dates.getString(i),daily.getJSONArray("weather_code").getInt(i),daily.getJSONArray("temperature_2m_max").getDouble(i),daily.getJSONArray("temperature_2m_min").getDouble(i),daily.optJSONArray("precipitation_probability_max")?.optInt(i,0) ?: 0) }
        val hourly=j.optJSONObject("hourly");val times=hourly?.optJSONArray("time")
        val hours=(0 until minOf(125,times?.length() ?: 0)).mapNotNull { i ->
            val time=times!!.optString(i)
            if(runCatching{java.time.LocalDateTime.parse(time)}.isFailure)return@mapNotNull null
            fun number(key:String)=hourly?.optJSONArray(key)?.optDouble(i)?.takeIf{it.isFinite()}
            WeatherHour(time,number("temperature_2m"),number("apparent_temperature"),number("precipitation_probability")?.toInt()?.coerceIn(0,100),number("precipitation"),number("wind_speed_10m"),number("weather_code")?.toInt() ?: -1,number("is_day")!=0.0)
        }
        return WeatherData(p,temp,current.optDouble("apparent_temperature",temp),current.optDouble("wind_speed_10m",0.0),current.optInt("weather_code",-1),current.optInt("is_day",1)==1,days,now,hours,j.optString("timezone","Europe/Bratislava"))
    }
    suspend fun search(query:String):List<WeatherPlace> = withContext(Dispatchers.IO) {
        require(query.trim().length in 2..80)
        val j=JSONObject(PublicFeed.get("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(query.trim(),"UTF-8")}&count=5&language=${AppLanguage.code}&format=json"))
        val list=j.optJSONArray("results") ?: return@withContext emptyList()
        (0 until minOf(5,list.length())).mapNotNull { i -> runCatching { val p=list.getJSONObject(i);WeatherPlace(p.getString("name"),p.getDouble("latitude"),p.getDouble("longitude"),listOf(p.optString("admin1"),p.optString("country")).filter{it.isNotBlank()}.distinct().joinToString(", ")) }.getOrNull()?.takeIf{valid(it)} }
    }
    fun label(code:Int,day:Boolean=true)=when(code){
        0->if(day)"Jasno" else "Jasná noc";1->"Takmer jasno";2->"Polooblačno";3->"Zamračené";45,48->"Hmla"
        51,53,55->"Mrholenie";56,57->"Mrznúce mrholenie";61,63,65->"Dážď";66,67->"Mrznúci dážď"
        71,73,75,77->"Sneženie";80,81,82->"Prehánky";85,86->"Snehové prehánky";95->"Búrka";96,99->"Búrka s krúpami";else->"Údaj nie je dostupný"
    }
}
