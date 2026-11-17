package com.conreo.couchytv

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object AerialProfiles {
    fun open(context: Context, profile: String) {
        require(profile in setOf("album", "online", "original-v2"))
        if (!Actions.isInstalled(context,Actions.AERIAL_PKG)) { Actions.openAppStore(context,Actions.AERIAL_PKG); return }
        runCatching {
        val dir = File(context.filesDir, "aerial-profiles").apply { mkdirs() }
        val file = File(dir, "$profile.avsettings")
        context.assets.open("aerial/$profile.avsettings").use { input -> file.outputStream().use { input.copyTo(it) } }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/avsettings")
            .setClassName("com.neilturner.aerialviews", "com.neilturner.aerialviews.ui.MainActivity")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { Actions.toast(context,"Nastavenia šetriča sa nepodarilo otvoriť.") }
    }
}
