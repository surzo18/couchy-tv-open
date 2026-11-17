package com.conreo.couchytv

import android.app.Activity
import android.app.AlertDialog
import android.widget.ScrollView
import android.widget.TextView
import android.text.util.Linkify
import android.text.method.LinkMovementMethod

/** Offline notices are shipped in the APK. */
object LegalDialogs {
    fun show(a:Activity) {
        val entries=linkedMapOf("Licenses and attribution" to "THIRD-PARTY-NOTICES.md",
            "Privacy and permissions" to "PRIVACY.md", "Online services and content" to "SERVICES.md",
            "Original media and provenance" to "MEDIA-LICENSE.md", "GNU GPL v3" to "GPL-3.0.txt",
            "Twemoji graphics · CC BY 4.0" to "CC-BY-4.0.txt", "Cookie font · SIL OFL" to "OFL-Cookie.txt")
        AlertDialog.Builder(a,R.style.MediaLibraryDialog).setTitle("About · licenses & privacy")
            .setItems(entries.keys.toTypedArray()){_,i->
                val entry=entries.entries.elementAt(i)
                val content=a.assets.open("legal/${entry.value}").bufferedReader().use{it.readText()}
                val view=TextView(a).apply{text=content;setTextColor(android.graphics.Color.WHITE);textSize=16f
                    setPadding(32,24,32,24);autoLinkMask=Linkify.WEB_URLS;movementMethod=LinkMovementMethod.getInstance()}
                AlertDialog.Builder(a,R.style.MediaLibraryDialog).setTitle(entry.key)
                    .setView(ScrollView(a).apply{isFocusable=true;addView(view)})
                    .setPositiveButton(AppLanguage.text("Zavrieť"),null).show()
            }.setNegativeButton(AppLanguage.text("Zavrieť"),null).show()
    }
}
