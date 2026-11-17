package com.conreo.couchytv

import android.content.Context
import android.content.Intent

/** Only the last launchable app, in memory. No usage history or private window data. */
object LastApp {
    private var last: String? = null
    // Only the most recent link opened by our UI, never browser history.
    private var webReturn: Intent? = null
    private var webPackage: String? = null
    fun openWeb(context: Context, url: String) {
        val uri = android.net.Uri.parse(url)
        if (uri.scheme != "https" || uri.host.isNullOrBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            val target = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            context.startActivity(intent)
            webPackage = target?.activityInfo?.packageName
            webReturn = Intent(intent)
            webPackage?.let { observe(context, it) }
        }.onFailure { Actions.toast(context, "Odkaz sa nepodarilo otvoriť.") }
    }
    fun observe(context: Context, pkg: String) {
        if (pkg == context.packageName || pkg == last || pkg in setOf(
            "com.google.android.apps.tv.launcherx", "com.android.systemui", "com.android.tv.settings",
            "com.neilturner.aerialviews", "com.google.android.inputmethod.latin", "com.tcl.tv")) return
        val pm = context.packageManager
        if (pm.getLeanbackLaunchIntentForPackage(pkg) != null || pm.getLaunchIntentForPackage(pkg) != null) last = pkg
    }
    fun resume(context: Context) {
        val pkg = last ?: return Actions.toast(context, "Zatiaľ sa nemáš do ktorej aplikácie vrátiť.")
        val intent = (if (pkg == webPackage) webReturn?.let { Intent(it) } else null)
            ?: context.packageManager.getLeanbackLaunchIntentForPackage(pkg)
            ?: context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return Actions.toast(context, "Posledná aplikácia už nie je dostupná.")
        // Bring the existing task forward; never CLEAR_TASK or CLEAR_TOP.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        runCatching { context.startActivity(intent) }
            .onSuccess { android.util.Log.i("CouchyReturn", "Resumed $pkg") }
            .onFailure { Actions.toast(context, "Aplikáciu sa nepodarilo otvoriť.") }
    }
}
