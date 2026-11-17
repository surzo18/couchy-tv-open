package com.conreo.couchytv

import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.content.pm.PackageManager

/** Resolve HOME activities rather than assuming Google TV or a manufacturer. */
object TvCompatibility {
    fun stockHome(c: Context): Intent? {
        val query=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val homes=c.packageManager.queryIntentActivities(query,PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.activityInfo.packageName != c.packageName && it.activityInfo.enabled && it.activityInfo.exported }
        val selected=homes.sortedByDescending { it.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0 }.firstOrNull() ?: return null
        return Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .setComponent(ComponentName(selected.activityInfo.packageName,selected.activityInfo.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }
}

object HomeControls {
    private fun prefs(c:Context)=c.getSharedPreferences("home_controls",0)
    fun longPressOriginal(c:Context)=prefs(c).getBoolean("long_original",true)
    fun setLongPressOriginal(c:Context,v:Boolean)=prefs(c).edit().putBoolean("long_original",v).apply()
    fun enabled(c:Context):Boolean {
        val expected=ComponentName(c,KeyRemapService::class.java)
        return android.provider.Settings.Secure.getString(c.contentResolver,"enabled_accessibility_services")
            .orEmpty().split(':').any { ComponentName.unflattenFromString(it)==expected }
    }
}
