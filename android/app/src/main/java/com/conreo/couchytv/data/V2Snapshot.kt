package com.conreo.couchytv.data

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object V2Snapshot {
    private val lock = Mutex()
    fun discarded(context: Context) = context.getSharedPreferences("maintenance", 0).getBoolean("discardV2", false)
    private fun directory(context: Context) = File(context.filesDir, "rollback-v2").apply { mkdirs() }
    suspend fun ensure(context: Context, config: String) = withContext(Dispatchers.IO) {
      lock.withLock {
        if (discarded(context)) return@withLock
        val dir = directory(context)
        val target = File(dir, "snapshot.json")
        if (!target.exists()) {
            val p = context.getSharedPreferences("wallpaper_cycle", 0)
            val data = JSONObject().put("schema", 1).put("config", JSONObject(config))
                .put("last", p.getString("last", ""))
                .put("remaining", JSONArray(p.getStringSet("remaining", emptySet()).orEmpty().toList()))
            val temp = File(dir, "snapshot.tmp")
            temp.writeText(data.toString(2))
            check(temp.renameTo(target)) { "Zálohu nastavení sa nepodarilo uložiť." }
        }
        val export = File(context.getExternalFilesDir(null), "rollback-v2").apply { mkdirs() }
        if (!File(export, "snapshot.json").exists()) target.copyTo(File(export, "snapshot.json"))
      }
    }
    fun read(context: Context): JSONObject {
        val local = File(context.filesDir, "rollback-v2/snapshot.json")
        val restoredFromPc = File(context.getExternalFilesDir(null), "rollback-v2/snapshot.json")
        return JSONObject((if (local.isFile) local else restoredFromPc).readText())
    }
    suspend fun discard(context: Context): Long = withContext(Dispatchers.IO) {
        lock.withLock {
            check(context.getSharedPreferences("maintenance", 0).edit().putBoolean("discardV2", true).commit())
            var bytes = 0L
            val dirs = listOf(File(context.filesDir, "rollback-v2"), File(context.getExternalFilesDir(null), "rollback-v2"))
            // Exact known backup files only. Never recursively delete app data or media.
            for (dir in dirs) {
                for (name in listOf("snapshot.json", "snapshot.tmp", "restored.ok")) {
                    val file = File(dir, name)
                    if (file.isFile) { val size = file.length(); check(file.delete()); bytes += size }
                }
                if (dir.isDirectory && dir.list()?.isEmpty() == true) check(dir.delete())
            }
            val oldProfile = File(context.filesDir, "aerial-profiles/original-v2.avsettings")
            if (oldProfile.isFile) { val size = oldProfile.length(); check(oldProfile.delete()); bytes += size }
            bytes
        }
    }
    fun restoreWallpaper(context: Context, snapshot: JSONObject) {
        val arr = snapshot.optJSONArray("remaining") ?: JSONArray()
        context.getSharedPreferences("wallpaper_cycle", 0).edit().clear()
            .putString("last", snapshot.optString("last"))
            .putStringSet("remaining", (0 until arr.length()).map { arr.getString(it) }.toSet()).commit()
    }
}
