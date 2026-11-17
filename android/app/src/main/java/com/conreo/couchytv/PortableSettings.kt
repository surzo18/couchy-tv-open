package com.conreo.couchytv

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.conreo.couchytv.ui.LocalizedText as Text
import com.conreo.couchytv.ui.PanelButton

@Composable
fun PortableSettings(a:Activity) {
    val scope=rememberCoroutineScope()
    var homeEnabled by remember { mutableStateOf(HomeControls.enabled(a)) }
    var longHome by remember { mutableStateOf(HomeControls.longPressOriginal(a)) }
    val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner){
        val observer=androidx.lifecycle.LifecycleEventObserver{_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_RESUME)homeEnabled=HomeControls.enabled(a)}
        owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer)}
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        PanelButton(if(AppLanguage.code=="en")"Run setup wizard" else "Spustiť úvodného sprievodcu") { scope.launch { a.getSharedPreferences("onboarding",0).edit().putInt("step",0).apply(); com.conreo.couchytv.data.ConfigStore(a).update{it.copy(setupDone=false)}; Actions.openCustomHome(a);a.finish() } }
        PanelButton("Licencie a súkromie / Licenses and privacy") { LegalDialogs.show(a) }
        Text("Jazyk")
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            PanelButton((if(AppLanguage.code=="sk")"✓ " else "")+"Slovenčina"){AppLanguage.set(a,"sk");a.recreate()}
            PanelButton((if(AppLanguage.code=="en")"✓ " else "")+"English"){AppLanguage.set(a,"en");a.recreate()}
        }
        Text("Nastavenia Home")
        Text(if(homeEnabled)"Skratka Home je zapnutá" else "Skratka Home je vypnutá")
        PanelButton("Povoliť skratku Home") { runCatching{a.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}.onFailure{Actions.openSystemSettings(a)} }
        PanelButton(if(longHome)"Podržať Home: pôvodná plocha" else "Podržať Home: naša plocha") {longHome=!longHome;HomeControls.setLongPressOriginal(a,longHome)}
        PanelButton("Výber domovskej aplikácie") {runCatching{a.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))}.onFailure{Actions.openSystemSettings(a)}}
        Text("Nie každá TV povoľuje zmenu Home. Dostupnosť závisí od výrobcu.")
        PanelButton("Zdroje správ"){NewsSourceDialogs(a){}.show()}
        PanelButton(AppLanguage.text("Obnovovanie správ")+": ${NewsSources.refreshMinutes} min") {
            val choices=listOf(5,15,30,60);NewsSources.interval(choices[(choices.indexOf(NewsSources.refreshMinutes)+1)%choices.size])
        }
        PanelButton(AppLanguage.text("Jednotky počasia")+": "+if(Weather.imperial)"°F · mph" else "°C · km/h") {Weather.setImperial(a,!Weather.imperial)}
        Text("Open-Meteo")
        PanelButton("Počasie"){Experience.homePanel="weather"}
    }
}
