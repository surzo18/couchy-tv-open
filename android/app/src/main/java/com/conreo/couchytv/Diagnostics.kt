package com.conreo.couchytv

import com.conreo.couchytv.ui.LocalizedText as Text
import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Debug
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*

object RuntimeHealth {
    @Volatile var newsUpdated=0L
    @Volatile var newsCount=0
    @Volatile var premieresUpdated=0L
    @Volatile var mediaError=""
    fun mediaFailure(message:String){mediaError=message.take(160);DiagnosticLog.report("Prehrávanie","CHYBA",mediaError)}
    fun time(value:Long)=if(value==0L)"Zatiaľ neobnovené" else java.text.SimpleDateFormat("d. M. HH:mm",java.util.Locale.getDefault()).format(java.util.Date(value))
}
data class DiagnosticsSnapshot(val free:Long,val total:Long,val available:Long,val ram:Long,val pss:Long,val network:String,val media:String,val timestamp:Long)
private fun collectDiagnostics(c:Context):DiagnosticsSnapshot {
    val storage=StatFs(c.filesDir.absolutePath)
    val memory=ActivityManager.MemoryInfo();(c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
    val own=Debug.MemoryInfo();Debug.getMemoryInfo(own)
    val cm=c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps=if(android.os.Build.VERSION.SDK_INT>=23)cm.getNetworkCapabilities(cm.activeNetwork) else null
    val net=when {caps==null->"Bez aktívneho pripojenia";caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)->"Ethernet";caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->"Wi-Fi";else->"Sieť pripojená"}+
        (if(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true)" · prístup na internet overený" else "")
    val media=listOf("wallpapers","music").sumOf{name->c.getExternalFilesDir(null)?.resolve(name)?.listFiles()?.count{it.isFile} ?: 0}
    return DiagnosticsSnapshot(storage.availableBytes,storage.totalBytes,memory.availMem,memory.totalMem,own.totalPss*1024L,net,"$media súborov pozadí, náhľadov a hudby",System.currentTimeMillis())
}

@Composable
fun DiagnosticsPanel() {
    val c=LocalContext.current
    var snapshot by remember{mutableStateOf<DiagnosticsSnapshot?>(null)}
    var refresh by remember{mutableIntStateOf(0)}
    var busy by remember{mutableStateOf(true)}
    var jelly by remember{mutableStateOf("Kontrolujem pripojenie…")}
    val scroll=rememberScrollState()
    val scope=rememberCoroutineScope()
    var showLogs by remember{mutableStateOf(false)}
    if(showLogs)com.conreo.couchytv.ui.DiagnosticLogPanel{showLogs=false}
    LaunchedEffect(refresh) {
        busy=true
        try {
            snapshot=withContext(Dispatchers.IO){collectDiagnostics(c)}
            jelly=if(!JellyfinLink.connected(c))"Účet pre prehľad Jellyfin nie je pripojený" else try{withContext(Dispatchers.IO){JellyfinLink.resume(c)};DiagnosticLog.report("Jellyfin","OK","Jellyfin je dostupný a prihlásenie je platné.");"Jellyfin je dostupný a prihlásenie je platné"}catch(e:CancellationException){throw e}catch(e:Exception){DiagnosticLog.report("Jellyfin","CHYBA","Server neodpovedá alebo vypršalo prihlásenie.",e);"Server neodpovedá alebo vypršalo prihlásenie"}
        }finally{busy=false}
    }
    fun gib(n:Long)=String.format(com.conreo.couchytv.AppLanguage.locale(),"%.1f GB",n/1073741824.0)
    Column(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
        val direction=when(event.key){Key.DirectionDown->1;Key.DirectionUp->-1;else->0}
        val can=if(direction>0)scroll.canScrollForward else scroll.canScrollBackward
        if(direction!=0 && can && event.type==KeyEventType.KeyDown){scope.launch{scroll.scrollTo((scroll.value+direction*160).coerceIn(0,scroll.maxValue))};true} else false
    }.verticalScroll(scroll).padding(10.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            StudioButton(if(busy)"Kontrolujem…" else "Obnoviť kontrolu"){if(!busy)refresh++}
            StudioButton("Počasie a oblasť"){Experience.homePanel="weather"}
            StudioButton("Záznamy a chyby"){showLogs=true}
        }
        Text("Údaje sa skontrolujú pri otvorení tejto obrazovky alebo po stlačení Obnoviť kontrolu.",color=Color(0xFFB8C9D8),fontSize=13.sp)
        snapshot?.let{s->
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                DiagnosticCard("Úložisko",gib(s.free)+" voľných","Z celkových ${gib(s.total)}",Modifier.weight(1f))
                DiagnosticCard("Pamäť TV",gib(s.available)+" dostupných","Z celkových ${gib(s.ram)}",Modifier.weight(1f))
                DiagnosticCard("Táto aplikácia","${s.pss/1048576} MB","Aktuálne využitie pamäte (PSS)",Modifier.weight(1f))
            }
            DiagnosticCard("Pripojenie",s.network,"Jellyfin: $jelly")
            DiagnosticCard("Obnovovanie dát","Správy: ${RuntimeHealth.time(RuntimeHealth.newsUpdated)}","Správy: ${RuntimeHealth.newsCount} článkov · obnova každých 15 minút\nNovinky: ${RuntimeHealth.time(RuntimeHealth.premieresUpdated)} · obnova každých 6 hodín\nPočasie: ${RuntimeHealth.time(Weather.data?.fetched ?: 0)} · ${if(Weather.enabled)"30 min na ploche" else "skryté, automatická obnova vypnutá"}")
            DiagnosticCard("Prehrávanie",HomeMusic.status,if(RuntimeHealth.mediaError.isBlank())"V tomto spustení nebola zaznamenaná chyba prehrávača." else "Posledná chyba: ${RuntimeHealth.mediaError}")
            DiagnosticCard("Dočasné dáta","Dočasné dáta sa priebežne čistia","Náhľady: Novinky do 8 MB, Jellyfin do 4 MB, správy do 2 MB. Ukladá sa najviac 180 správ.\nDiagnostické záznamy: najviac 30 dní, 400 udalostí alebo 256 KB.\nFotografie, videá a hudba sa automaticky nemažú.")
            Text("Posledná kontrola: ${RuntimeHealth.time(s.timestamp)}",fontSize=11.sp,color=Color(0xFFB8C9D8))
        }
    }
}
@Composable
private fun DiagnosticCard(title:String,value:String,detail:String,modifier:Modifier=Modifier.fillMaxWidth()) {
    Column(modifier.background(Color(0xFF223749),RoundedCornerShape(16.dp)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Text(title,color=Color(0xFF98B3C9),fontSize=12.sp)
        Text(value,color=Color.White,fontSize=19.sp)
        Text(detail,color=Color(0xFFBACBD9),fontSize=12.sp)
    }
}
