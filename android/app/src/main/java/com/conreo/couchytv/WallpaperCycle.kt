package com.conreo.couchytv

import android.content.Context

/** A process session keeps its wallpaper on app returns; SCREEN_ON advances it. */
object WallpaperCycle {
    private var current: String? = null
    private var lastScreenOn = 0L
    fun randomAtWake(context: Context) = context.getSharedPreferences("wallpaper_cycle", 0).getBoolean("randomAtWake", true)
    fun toggleWakeMode(context: Context) {
        context.getSharedPreferences("wallpaper_cycle", 0).edit().putBoolean("randomAtWake", !randomAtWake(context)).apply()
        Experience.changed()
    }
    private fun saved(context: Context): String? {
        val uri = context.getSharedPreferences("wallpaper_cycle", 0).getString("last", null)
        return uri?.takeIf { name -> WallpaperLibrary.all(context).any { it.uri == name } }
    }
    fun screenOn(context: Context): String {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastScreenOn < 2_000) return current(context)
        lastScreenOn = now
        return if (randomAtWake(context)) next(context) else current(context)
    }
    fun current(context: Context): String = current ?: (if (!randomAtWake(context)) saved(context)?.also { current = it } else null) ?: next(context)
    fun next(context: Context): String {
        val choices = WallpaperLibrary.available(context).map { it.uri }
        val prefs = context.getSharedPreferences("wallpaper_cycle", Context.MODE_PRIVATE)
        val previous = current ?: prefs.getString("last", null)
        val remaining = prefs.getStringSet("remaining", emptySet()).orEmpty().intersect(choices.toSet())
        val pool = remaining.ifEmpty { choices.toSet() }
        val candidates = pool.filter { it != previous }.ifEmpty { pool.toList() }
        val selected = candidates.randomOrNull() ?: "asset:///wallpapers/Aurora.mp4"
        current = selected
        prefs.edit().putString("last", selected).putStringSet("remaining", pool - selected).apply()
        android.util.Log.i("CouchyWallpaper", "Selected $selected")
        return selected
    }
    fun select(context: Context, uri: String): String {
        require(WallpaperLibrary.all(context).any { it.uri == uri })
        current = uri
        context.getSharedPreferences("wallpaper_cycle", Context.MODE_PRIVATE).edit().putString("last", uri).apply()
        return uri
    }
    fun reset(context: Context) {
        val saved = context.getSharedPreferences("wallpaper_cycle", Context.MODE_PRIVATE).getString("last", null)
        current = saved?.takeIf { uri -> WallpaperLibrary.all(context).any { it.uri == uri } }
        if (current == null) current(context)
    }
}
