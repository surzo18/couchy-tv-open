package com.conreo.couchytv

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity

/** Local app language, without changing the TV's system language. */
open class LocalizedActivity:ComponentActivity() {
    private var language=""
    override fun attachBaseContext(base:Context) {
        language=base.getSharedPreferences("language",0).getString("code","sk").let{if(it=="en")"en" else "sk"}
        val configuration=Configuration(base.resources.configuration)
        configuration.setLocale(java.util.Locale.forLanguageTag(language))
        super.attachBaseContext(base.createConfigurationContext(configuration))
    }
    override fun onResume(){super.onResume();if(language!=AppLanguage.code)recreate()}
}
