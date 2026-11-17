package com.conreo.couchytv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*

/** Local maintenance entry point: protected by DUMP (ADB/system), not callable by normal apps. */
class RollbackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cleanup = intent.action == "io.github.surzo18.couchytv.DISCARD_V2"
        if (!cleanup && intent.action != "io.github.surzo18.couchytv.RESTORE_V2") return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (cleanup) {
                    val removed = com.conreo.couchytv.data.V2Snapshot.discard(context)
                    android.util.Log.i("CouchyCleanup", "Removed V2 backup: $removed bytes; active configuration preserved")
                } else {
                    com.conreo.couchytv.data.ConfigStore(context).restoreV2()
                    WallpaperCycle.reset(context)
                }
                result.setResultCode(-1)
            } catch (e: Exception) { android.util.Log.e("CouchyRollback", "Obnova zlyhala", e) }
            finally { result.finish() }
        }
    }
}
