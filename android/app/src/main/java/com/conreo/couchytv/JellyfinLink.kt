package com.conreo.couchytv

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ResumeItem(val id: String, val title: String, val subtitle: String, val progress: Int, val imageId: String, val remainingMinutes:Int=0,val kind:String="",val parentImageId:String="")
data class LibraryItem(val id:String,val title:String,val subtitle:String,val overview:String,val type:String,val imageId:String,val seriesId:String="") {
    fun poster()=ResumeItem(id,title,subtitle,0,imageId,kind=type,parentImageId=seriesId)
}
data class LibraryPage(val items:List<LibraryItem>,val total:Int)
data class QuickCode(val code: String, val secret: String,val expiresAt:Long=android.os.SystemClock.elapsedRealtime()+300_000L)
class JellyfinAuthExpired:java.io.IOException("Prihlásenie do Jellyfinu vypršalo. Vytvor nový prihlasovací kód.")

object JellyfinLink {
    fun library(context:Context,type:String="Movie,Series",favorites:Boolean=false,series:String="",season:String="",offset:Int=0):LibraryPage {
        val userId=Uri.encode(prefs(context).getString("userId","").orEmpty())
        val path=when {
            season.isNotEmpty()->"/Shows/${Uri.encode(series)}/Episodes?userId=$userId&SeasonId=${Uri.encode(season)}&StartIndex=$offset&Limit=30&IsMissing=false"
            series.isNotEmpty()->"/Shows/${Uri.encode(series)}/Seasons?userId=$userId&IsMissing=false"
            else->"/Items?userId=$userId&IncludeItemTypes=${Uri.encode(type)}&Recursive=true&SortBy=Random&Limit=12"+(if(favorites)"&IsFavorite=true" else "")
        }
        val response=JSONObject(request(context,path+"&Fields=Overview,PrimaryImageAspectRatio&EnableImages=true&ImageTypeLimit=1"))
        val rows=response.optJSONArray("Items")
        val result=(0 until (rows?.length()?:0)).map { index ->
            val item=rows!!.getJSONObject(index)
            val kind=item.optString("Type")
            val subtitle=when(kind){
                "Episode"->"S${item.optInt("ParentIndexNumber")} · E${item.optInt("IndexNumber")}"
                "Series"->"Seriál"
                "Season"->"Séria ${item.optInt("IndexNumber")}"
                else->"Film"+item.optInt("ProductionYear").takeIf{it>0}?.let{" · $it"}.orEmpty()
            }
            LibraryItem(item.getString("Id"),item.optString("Name"),subtitle,
                item.optString("Overview").replace(Regex("<[^>]*>")," ").trim(),kind,
                if(kind=="Episode")item.optString("SeriesId").ifBlank{item.getString("Id")} else if(item.optJSONObject("ImageTags")?.has("Primary")==true)item.getString("Id") else item.optString("SeriesId").ifBlank{item.getString("Id")},item.optString("SeriesId"))
        }
        return LibraryPage(result,response.optInt("TotalRecordCount",result.size))
    }
    private val imageSlots=java.util.concurrent.Semaphore(2)
    fun clearArtworkCache(){artworkCache.evictAll();missingArtwork.evictAll()}
    private val missingArtwork=android.util.LruCache<String,Long>(200)
    private val artworkCache = object : android.util.LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).callTimeout(8, TimeUnit.SECONDS).connectTimeout(4, TimeUnit.SECONDS).build()
    private fun prefs(context: Context) = context.getSharedPreferences("jellyfin_link", 0)
    fun server(context: Context): String = prefs(context).getString("server", "").orEmpty()
    fun user(context: Context) = prefs(context).getString("userName", "")!!
    fun connected(context: Context) = !prefs(context).getString("token", "").isNullOrEmpty()
    fun saveServer(context: Context, value: String) {
        val uri = Uri.parse(value.trim())
        require(uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null) { "Zadaj adresu servera, napr. http://jellyfin.local:8096." }
        if (server(context) != value.trim().trimEnd('/')) prefs(context).edit().clear().putString("server", value.trim().trimEnd('/')).commit()
    }
    private fun request(context: Context, path: String, body: JSONObject? = null, authorized: Boolean = true): String {
        val token = if (authorized) prefs(context).getString("token", "").orEmpty() else ""
        val deviceId = prefs(context).getString("deviceId", null) ?: java.util.UUID.randomUUID().toString().also {
            prefs(context).edit().putString("deviceId", it).apply()
        }
        val auth = "MediaBrowser Client=\"Couchy TV Open\", Device=\"Android TV\", DeviceId=\"$deviceId\", Version=\"1.0.0\"" +
            if (token.isNotEmpty()) ", Token=\"$token\"" else ""

        val builder = Request.Builder().url(server(context) + path).header("Authorization", auth)
        if (body != null) builder.post(body.toString().toRequestBody("application/json".toMediaType()))
        client.newCall(builder.build()).execute().use {
            if(!it.isSuccessful){
                DiagnosticLog.report("Jellyfin","CHYBA","Server odmietol požiadavku: HTTP ${it.code}.")
                if(authorized && it.code==401)throw JellyfinAuthExpired()
                throw java.io.IOException("Server odpovedal chybou HTTP ${it.code}.")
            }
            return it.body?.string() ?: "{}"
        }
    }
    fun begin(context: Context): QuickCode {
        val response = JSONObject(request(context, "/QuickConnect/Initiate", JSONObject(), false))
        return QuickCode(response.getString("Code"), response.getString("Secret"))
    }
    fun finishIfReady(context: Context, code: QuickCode): Boolean {
        val state = JSONObject(request(context, "/QuickConnect/Connect?secret=${Uri.encode(code.secret)}", authorized = false))
        if (!state.optBoolean("Authenticated")) return false
        val response = JSONObject(request(context, "/Users/AuthenticateWithQuickConnect", JSONObject().put("Secret", code.secret), false))
        val user = response.getJSONObject("User")
        prefs(context).edit().putString("token", response.getString("AccessToken"))
            .putString("userId", user.getString("Id")).putString("userName", user.getString("Name")).commit()
        artworkCache.evictAll()
        DiagnosticLog.report("Jellyfin","OK","Profil bol pripojený cez Quick Connect.")
        return true
    }
    fun disconnect(context: Context) {
        prefs(context).edit().remove("token").remove("userId").remove("userName").commit()
        artworkCache.evictAll()
    }
    fun resume(context: Context): List<ResumeItem> {
        val userId = prefs(context).getString("userId", "").orEmpty()
        val response = JSONObject(request(context, "/UserItems/Resume?userId=${Uri.encode(userId)}&limit=12&mediaTypes=Video&enableImages=true&fields=SeriesName"))
        return parseItems(response)
    }
    fun nextUp(context: Context): List<ResumeItem> {
        val userId = prefs(context).getString("userId", "").orEmpty()
        val response = JSONObject(request(context, "/Shows/NextUp?userId=${Uri.encode(userId)}&limit=20&enableImages=true&enableUserData=true&enableResumable=false&enableRewatching=false&fields=SeriesName"))
        return parseItems(response)
    }
    private fun parseItems(response: JSONObject): List<ResumeItem> {
        val items = response.optJSONArray("Items") ?: return emptyList()
        return (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            val series = item.optString("SeriesName")
            val position = item.optJSONObject("UserData")?.optLong("PlaybackPositionTicks") ?: 0L
            val duration = item.optLong("RunTimeTicks").coerceAtLeast(1)
            ResumeItem(item.getString("Id"), series.ifBlank { item.optString("Name") },
                if (series.isNotBlank()) "S${item.optInt("ParentIndexNumber")} E${item.optInt("IndexNumber")} · ${item.optString("Name")}" else "Rozpozeraný film",
                (100.0 * position / duration).toInt().coerceIn(0, 100),
                item.optString("SeriesId").ifBlank { item.getString("Id") },
                if(item.optLong("RunTimeTicks")>0)kotlin.math.ceil((duration-position).coerceAtLeast(0)/600_000_000.0).toInt().coerceIn(0,999) else 0,item.optString("Type"),item.optString("SeriesId"))
        }
    }
    fun artwork(context: Context, item: ResumeItem,wide:Boolean=false): Bitmap? = runCatching {
        val cacheKey = server(context) + "/" + user(context) + "/" + (if(wide)item.id+"/wide" else item.imageId)
        artworkCache.get(cacheKey)?.let { return@runCatching it }
        imageSlots.acquire()
        try {
        artworkCache.get(cacheKey)?.let { return@runCatching it }
        val token = prefs(context).getString("token", "").orEmpty()
        val candidates=if(!wide)listOf(item.imageId to "Primary") else buildList {
            if(item.kind=="Episode")add(item.id to "Primary")
            add(item.id to "Backdrop")
            if(item.parentImageId.isNotBlank())add(item.parentImageId to "Backdrop")
            add(item.imageId to "Primary")
            if(item.parentImageId.isNotBlank())add(item.parentImageId to "Primary")
        }.distinct()
        for((imageId,imageType) in candidates){
        val missingKey=server(context)+"/"+imageId+"/"+imageType
        if(android.os.SystemClock.elapsedRealtime()-(missingArtwork.get(missingKey)?:-1_000_000L)<900_000L)continue
        val req = Request.Builder().url(server(context) + "/Items/$imageId/Images/$imageType?maxWidth=${if(wide)640 else 360}&maxHeight=${if(wide)360 else 540}&quality=85")
            .header("Authorization", "MediaBrowser Token=\"$token\"").build()
        val bitmap=client.newCall(req).execute().use { response -> if(response.isSuccessful)response.body?.byteStream()?.use { stream ->
            val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
            while(true){val n=stream.read(buffer);if(n<0)break;check(out.size()+n<=1_500_000);out.write(buffer,0,n)}
            val bytes=out.toByteArray();val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
            val options=BitmapFactory.Options().apply{inSampleSize=1};while(bounds.outWidth/options.inSampleSize>540 || bounds.outHeight/options.inSampleSize>720)options.inSampleSize*=2
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
        } else {if(response.code==404)missingArtwork.put(missingKey,android.os.SystemClock.elapsedRealtime());null} }
        if(bitmap!=null){artworkCache.put(cacheKey,bitmap);return@runCatching bitmap}
        }
        null
        } finally {imageSlots.release()}
    }.getOrNull()
    fun open(context: Context, item: ResumeItem) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.id))
            .setClassName("org.jellyfin.androidtv", "org.jellyfin.androidtv.ui.startup.StartupActivity")
            .putExtra("ItemId", item.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure { Actions.launchApp(context, "org.jellyfin.androidtv") }
    }
}
