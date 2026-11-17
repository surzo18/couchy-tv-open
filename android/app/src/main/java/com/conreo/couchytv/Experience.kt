package com.conreo.couchytv

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.*

/** Optional v3 features live separately so the v2 configuration stays intact. */
object Experience {
    var canReturnToApp = false
    var systemPanel = false
    var gameActive = false
    var homePanel by mutableStateOf("")
    var enabled by mutableStateOf(true); private set
    var unifiedIcons by mutableStateOf(false); private set
    var idleSeconds by mutableIntStateOf(30); private set
    var artOnly by mutableStateOf(false)
    var interaction by mutableLongStateOf(SystemClock.elapsedRealtime()); private set
    var revision by mutableIntStateOf(0); private set
    fun load(context: Context) {
        val p = context.getSharedPreferences("experience_v3", 0)
        if (!p.getBoolean("classicIconsPreferredV3", false)) {
            p.edit().putBoolean("classicIconsPreferredV3", true).putBoolean("unifiedIcons", false).commit()
        }
        enabled = p.getBoolean("enabled", true)
        unifiedIcons = p.getBoolean("unifiedIcons", false)
        idleSeconds = p.getInt("idleSeconds", 30)
    }
    fun touch() { artOnly = false; interaction = SystemClock.elapsedRealtime() }
    fun configure(context: Context, active: Boolean = enabled, icons: Boolean = unifiedIcons, idle: Int = idleSeconds) {
        context.getSharedPreferences("experience_v3", 0).edit()
            .putBoolean("enabled", active).putBoolean("unifiedIcons", icons).putInt("idleSeconds", idle).apply()
        load(context); touch(); revision++
    }
    fun changed() { revision++; touch() }
}
