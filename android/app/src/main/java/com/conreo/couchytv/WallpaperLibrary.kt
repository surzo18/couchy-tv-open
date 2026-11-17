package com.conreo.couchytv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class WallpaperItem(val id: String, val title: String, val collection: String, val uri: String, val builtIn: Boolean)

object WallpaperLibrary {
    private val builtIns get()=EditionConfig.wallpapers
    fun folder(context: Context) = File(context.getExternalFilesDir(null), "wallpapers").apply { mkdirs() }
    val collections get()=EditionConfig.collections
    fun all(context: Context): List<WallpaperItem> {
        val dir = folder(context)
        // Optional metadata never controls paths; discover only actual files in our own folder.
        val catalog = runCatching { MediaLibraryStore.recover(dir, "catalog.json") }.getOrNull()
        return (builtIns.map {
            WallpaperItem(it.first, it.second, it.third, "asset:///wallpapers/${it.first}", true)
        } + dir.listFiles().orEmpty().filter { it.isFile && it.length() > 0 && it.extension.lowercase() == "mp4" }.sortedBy { it.name }.map {
            val meta = catalog?.optJSONObject(it.name)
            val title = meta?.optString("title")?.trim()?.take(80)?.takeIf { name -> name.isNotEmpty() }
                ?: it.nameWithoutExtension.substringAfter('_').replace('-', ' ')
            val group = meta?.optString("collection")?.takeIf { name -> name in collections && name != "Všetky" } ?: "Vlastné"
            WallpaperItem(it.name, title, group, Uri.fromFile(it).toString(), false)
        }).map { item ->
            val edit = MediaLibraryStore.wallpaperEdit(context, item.id)
            item.copy(title = edit.optString("title", item.title), collection = edit.optString("collection", item.collection)
                .takeIf { it in collections && it != "Všetky" } ?: item.collection)
        }
    }
    private fun prefs(context: Context) = context.getSharedPreferences("wallpaper_library", 0)
    fun disabled(context: Context) = prefs(context).getStringSet("disabled", emptySet()).orEmpty().toSet()
    fun collection(context: Context) = prefs(context).getString("collection", "Všetky") ?: "Všetky"
    fun setCollection(context: Context, value: String) {
        prefs(context).edit().putString("collection", value).apply(); Experience.changed()
    }
    fun toggle(context: Context, item: WallpaperItem): Boolean {
        val set = disabled(context).toMutableSet()
        if (!set.remove(item.id)) {
            if (all(context).count { it.id !in set } <= 1) return false
            set.add(item.id)
        }
        prefs(context).edit().putStringSet("disabled", set).apply(); Experience.changed(); return true
    }
    fun available(context: Context): List<WallpaperItem> {
        val all = all(context)
        if (!Experience.enabled) return all.filter { it.builtIn }
        val enabled = all.filter { it.id !in disabled(context) }
        val chosen = enabled.filter { collection(context) == "Všetky" || it.collection == collection(context) }
        return chosen.ifEmpty { enabled }.ifEmpty { all.filter { it.builtIn } }
    }
    fun thumbnail(context: Context, item: WallpaperItem): Bitmap? = runCatching {
        if (item.builtIn) context.assets.open("thumbnails/${item.id.removeSuffix(".mp4")}.jpg").use { BitmapFactory.decodeStream(it) }
        else {
            val file = File(folder(context), "${item.id}.jpg")
            if (!file.exists()) makeThumbnail(context, Uri.parse(item.uri), file)
            BitmapFactory.decodeFile(file.path)
        }
    }.getOrNull()
    private fun makeThumbnail(context: Context, uri: Uri, target: File) {
        val reader = MediaMetadataRetriever()
        try {
            reader.setDataSource(context, uri)
            val frame = reader.getScaledFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 480, 270)
            if (frame != null) { target.outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, 85, it) }; frame.recycle() }
        } finally { reader.release() }
    }
    /** Import once; playback never depends on a revocable document-provider URI. */
    fun importVideo(context: Context, source: Uri, name: String): WallpaperItem {
        val dir = folder(context)
        val safeName = name.substringBeforeLast('.').replace(Regex("[^\\p{L}\\p{N} _-]"), "").take(70).ifBlank { "Video" }
        val target = File(dir, "${UUID.randomUUID().toString().take(8)}_$safeName.mp4")
        val temp = File(dir, "${target.name}.part")
        try {
            context.contentResolver.openInputStream(source).use { input ->
                requireNotNull(input)
                temp.outputStream().use { output ->
                    val buffer = ByteArray(256 * 1024); var copied = 0L
                    while (true) {
                        val count = input.read(buffer); if (count < 0) break
                        copied += count
                        require(copied <= 1_500_000_000L && dir.usableSpace > 2_000_000_000L) { "Video je príliš veľké alebo v TV nie je dostatok miesta. Po importe musí zostať voľných aspoň 2 GB." }
                        output.write(buffer, 0, count)
                    }
                }
            }
            val reader = MediaMetadataRetriever()
            try { reader.setDataSource(temp.path); require((reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0) > 0) { "Súbor neobsahuje čitateľné video." } }
            finally { reader.release() }
            check(temp.renameTo(target))
            makeThumbnail(context, Uri.fromFile(target), File(dir, "${target.name}.jpg"))
            return all(context).first { it.id == target.name }
        } catch (e: Exception) { temp.delete(); target.delete(); throw e }
    }
}
