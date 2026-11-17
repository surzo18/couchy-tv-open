package com.conreo.couchytv

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LibrarySong(val id: String, val title: String, val artist: String, val origin: String,
    val duration: Long, val enabled: Boolean, val bytes: Long)

/** Small persistent overrides survive catalog updates; no polling, player or artwork cache. */
object MediaLibraryStore {
    /** Import an owned local copy; validate audio and commit the catalog only after a complete copy. */
    fun importSong(c:Context,uri:android.net.Uri):String {
        val dir=music(c).apply{mkdirs()}
        val display=c.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())it.getString(0) else null} ?: "Audio"
        val suffix=display.substringAfterLast('.',"m4a").lowercase().takeIf{it in setOf("m4a","mp3","ogg","opus","flac","wav","aac")} ?: "m4a"
        val id=java.util.UUID.randomUUID().toString()+"."+suffix
        val temp=File(dir,"$id.part");val target=file(dir,id)
        try {
            require(dir.usableSpace>110L*1024*1024){"Nedostatok voľného miesta."}
            c.contentResolver.openInputStream(uri)!!.use{input->temp.outputStream().use{out->
                val buffer=ByteArray(65536);var total=0L
                while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=100L*1024*1024){"Skladba je príliš veľká (maximum 100 MB)."};out.write(buffer,0,n)}
            }}
            val metadata=android.media.MediaMetadataRetriever()
            val row=try {
                metadata.setDataSource(temp.path)
                val duration=metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                require(duration>0 && metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)=="yes"){"Súbor neobsahuje použiteľnú zvukovú stopu."}
                JSONObject().put("id",id).put("file",id).put("duration",duration/1000.0)
                    .put("title",metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf{it.isNotBlank()} ?: display.substringBeforeLast('.'))
                    .put("artist",metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty())
                    .put("origin",metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty())
            } finally {metadata.release()}
            synchronized(this) {
                val data=recover(dir,"playlist.json")
                val rows=data.optJSONArray("tracks") ?: JSONArray()
                require(rows.length()<1000){"Knižnica dosiahla limit 1000 skladieb."}
                check(temp.renameTo(target))
                rows.put(row);data.put("tracks",rows)
                write(File(dir,"playlist.json"),data)
            }
            return id
        } catch(e:Exception){target.delete();throw e}finally{temp.delete()}
    }
    private fun prefs(c: Context) = c.getSharedPreferences("media_library_edits", 0)
    private fun music(c: Context) = File(c.getExternalFilesDir(null), "music")
    private fun file(dir: File, id: String): File {
        require(id.isNotBlank() && id.none { it == '/' || it == '\\' || it == ':' || it.isISOControl() } && !id.startsWith('.')) { "Neplatný súbor." }
        return File(dir, id).canonicalFile.also { require(it.parentFile == dir.canonicalFile) }
    }
    private fun read(f: File) = if (f.exists() || File(f.path + ".bak").exists())
        JSONObject(AtomicFile(f).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }) else JSONObject()
    @Synchronized fun recover(dir: File, catalogName: String): JSONObject {
        val data = read(File(dir, catalogName))
        val live = if(catalogName == "playlist.json") {
            val rows = data.optJSONArray("tracks") ?: JSONArray()
            (0 until rows.length()).map { rows.optJSONObject(it)?.optString("file").orEmpty() }.toSet()
        } else data.keys().asSequence().toSet()
        dir.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(".deleting") }.forEach { tomb ->
            val id = tomb.name.removeSuffix(".deleting")
            if(id in live) { val original = file(dir,id); if(!original.exists()) tomb.renameTo(original) }
            else tomb.delete()
        }
        return data
    }
    private fun write(f: File, json: JSONObject) {
        val atomic = AtomicFile(f); val out = atomic.startWrite()
        try { out.write(json.toString(2).toByteArray(Charsets.UTF_8)); atomic.finishWrite(out) }
        catch (e: Exception) { atomic.failWrite(out); throw e }
    }
    fun catalog(c: Context) = runCatching { recover(music(c), "playlist.json") }.getOrElse {
        DiagnosticLog.report("Knižnica", "CHYBA", "Zoznam skladieb sa nepodarilo načítať.", it)
        JSONObject()
    }
    fun songs(c: Context, data: JSONObject = catalog(c)): List<LibrarySong> {
        val disabled = prefs(c).getStringSet("disabled_songs", emptySet()).orEmpty()
        val rows = data.optJSONArray("tracks") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { i ->
            val row = rows.optJSONObject(i) ?: return@mapNotNull null
            val id = row.optString("file")
            val f = runCatching { file(music(c), id) }.getOrNull() ?: return@mapNotNull null
            if (!f.isFile || f.length() == 0L) return@mapNotNull null
            val edit = runCatching { JSONObject(prefs(c).getString("song:$id", "{}")!!) }.getOrDefault(JSONObject())
            LibrarySong(id, edit.optString("title", row.optString("title", id)),
                edit.optString("artist", row.optString("artist")),
                edit.optString("origin", row.optString("origin", row.optString("album"))),
                (row.optDouble("duration", 0.0) * 1000).toLong(), id !in disabled, f.length())
        }.distinctBy { it.id }
    }
    fun binding(c: Context, wallpaper: String, data: JSONObject = catalog(c)): List<String> {
        val custom = prefs(c).getString("binding:$wallpaper", null)
        val ids = if (custom != null) runCatching { JSONArray(custom) }.getOrNull()
            else data.optJSONObject("bindings")?.optJSONArray(wallpaper)
        return if (ids == null) emptyList() else (0 until ids.length()).map { ids.optString(it) }.filter { it.isNotBlank() }.distinct()
    }
    fun saveSong(c: Context, song: LibrarySong, title: String, artist: String, origin: String) {
        require(title.trim().isNotEmpty() && artist.trim().isNotEmpty()) { "Vyplň názov aj interpreta." }
        require(songs(c).any { it.id == song.id }) { "Skladba už nie je v knižnici." }
        prefs(c).edit().putString("song:${song.id}", JSONObject().put("title", title.trim().take(120))
            .put("artist", artist.trim().take(120)).put("origin", origin.trim().take(160)).toString()).apply()
        HomeMusic.libraryChanged()
    }
    fun toggleSong(c: Context, id: String) {
        val disabled = prefs(c).getStringSet("disabled_songs", emptySet()).orEmpty().toMutableSet()
        if (!disabled.remove(id)) disabled.add(id)
        prefs(c).edit().putStringSet("disabled_songs", disabled).apply(); HomeMusic.libraryChanged()
    }
    fun saveBinding(c: Context, wallpaper: String, ids: List<String>) {
        require(WallpaperLibrary.all(c).any { it.id == wallpaper }) { "Pozadie už nie je dostupné." }
        val available = songs(c).map { it.id }.toSet()
        prefs(c).edit().putString("binding:$wallpaper", JSONArray(ids.distinct().filter { it in available }).toString()).apply()
        HomeMusic.libraryChanged()
    }
    fun wallpaperEdit(c: Context, id: String): JSONObject = runCatching {
        JSONObject(prefs(c).getString("wallpaper:$id", "{}")!!)
    }.getOrDefault(JSONObject())
    fun saveWallpaper(c: Context, item: WallpaperItem, title: String, collection: String) {
        require(title.trim().isNotEmpty()) { "Vyplň názov pozadia." }
        require(collection in WallpaperLibrary.collections && collection != "Všetky")
        prefs(c).edit().putString("wallpaper:${item.id}", JSONObject().put("title", title.trim().take(80)).put("collection", collection).toString()).apply()
        Experience.changed()
    }
    @Synchronized fun deleteSong(c: Context, id: String) {
        val dir = music(c); val f = file(dir, id); require(f.isFile) { "Skladba už nie je v knižnici." }
        val target = File(dir, "playlist.json"); val before = recover(dir, "playlist.json"); val next = JSONObject(before.toString())
        val rows = next.optJSONArray("tracks") ?: JSONArray()
        next.put("tracks", JSONArray((0 until rows.length()).mapNotNull { rows.optJSONObject(it) }.filter { it.optString("file") != id }))
        val bindings = next.optJSONObject("bindings")
        bindings?.keys()?.asSequence()?.toList()?.forEach { key ->
            val a = bindings.optJSONArray(key) ?: JSONArray()
            bindings.put(key, JSONArray((0 until a.length()).map { a.optString(it) }.filter { it != id }))
        }
        // Rename in the same directory, then atomically update metadata. Restore on failure.
        val tomb = file(dir, id + ".deleting"); require(!tomb.exists() && f.renameTo(tomb)) { "Súbor sa nepodarilo odstrániť." }
        try { write(target, next); check(tomb.delete()) { "Súbor sa nepodarilo odstrániť." } }
        catch (e: Exception) { tomb.renameTo(f); write(target, before); throw e }
        val editor = prefs(c).edit().remove("song:$id")
        editor.putStringSet("disabled_songs", prefs(c).getStringSet("disabled_songs", emptySet()).orEmpty() - id)
        prefs(c).all.keys.filter { it.startsWith("binding:") }.forEach { key ->
            val a = JSONArray(prefs(c).getString(key, "[]")); editor.putString(key, JSONArray((0 until a.length()).map { a.optString(it) }.filter { it != id }).toString())
        }
        editor.apply(); HomeMusic.libraryChanged()
    }
    @Synchronized fun deleteWallpaper(c: Context, item: WallpaperItem) {
        require(!item.builtIn) { "Toto pozadie je súčasťou aplikácie. Môžeš ho vyradiť z náhodného výberu." }
        require(WallpaperLibrary.all(c).count { it.id != item.id && it.id !in WallpaperLibrary.disabled(c) } > 0) { "Ponechaj povolené aspoň jedno pozadie." }
        val dir = WallpaperLibrary.folder(c); val f = file(dir, item.id); require(f.isFile)
        val catalogFile = File(dir, "catalog.json"); val before = read(catalogFile); val next = JSONObject(before.toString()).apply { remove(item.id) }
        val tomb = file(dir, item.id + ".deleting"); require(!tomb.exists() && f.renameTo(tomb)) { "Pozadie sa nepodarilo odstrániť." }
        try { write(catalogFile, next); check(tomb.delete()) { "Pozadie sa nepodarilo odstrániť." } }
        catch (e: Exception) { tomb.renameTo(f); write(catalogFile, before); throw e }
        file(dir, item.id + ".jpg").delete()
        runCatching {
            val playlist = catalog(c)
            playlist.optJSONObject("bindings")?.remove(item.id)
            write(File(music(c), "playlist.json"), playlist)
        }.onFailure { DiagnosticLog.report("Knižnica", "CHYBA", "Video odstránené, čistenie priradení hudby zlyhalo.", it) }
        prefs(c).edit().remove("wallpaper:${item.id}").remove("binding:${item.id}").apply()
        val wp = c.getSharedPreferences("wallpaper_library", 0)
        wp.edit().putStringSet("disabled", wp.getStringSet("disabled", emptySet()).orEmpty() - item.id).apply()
        WallpaperCycle.reset(c); HomeMusic.libraryChanged(); Experience.changed()
    }
}
