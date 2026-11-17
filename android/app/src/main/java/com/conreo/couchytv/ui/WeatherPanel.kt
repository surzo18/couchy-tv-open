package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.conreo.couchytv.*
import kotlinx.coroutines.*
import kotlin.math.*

@Composable
fun WeatherIcon(code:Int,day:Boolean,modifier:Modifier=Modifier) {
    Canvas(modifier) { drawWeatherIcon(code,day) }
}
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWeatherIcon(code:Int,day:Boolean) {
        val u=size.minDimension/32f
        fun pt(x:Float,y:Float)=Offset(x*u,y*u)
        val cloud=Color(0xFFD0DFEA);val sun=Color(0xFFFFD778);val blue=Color(0xFF85C9EF)
        if(code in 0..2) {
            drawCircle(if(day)sun else Color(0xFFCEDAF4),if(code==0)8*u else 6*u,pt(if(code==0)16f else 10f,if(code==0)16f else 10f))
            if(!day)drawCircle(Color(0xFF243247),6*u,pt(if(code==0)20f else 13f,if(code==0)12f else 7f))
            else if(code==0)for(i in 0..7){val a=i*Math.PI/4;drawLine(sun,pt(16+cos(a).toFloat()*11,16+sin(a).toFloat()*11),pt(16+cos(a).toFloat()*14,16+sin(a).toFloat()*14),u*1.6f,StrokeCap.Round)}
        }
        if(code!=0) {
            drawCircle(cloud,6*u,pt(10f,17f));drawCircle(cloud,8*u,pt(18f,14f));drawCircle(cloud,5*u,pt(25f,18f))
            drawRoundRect(cloud,pt(7f,17f),Size(21*u,7*u),androidx.compose.ui.geometry.CornerRadius(3*u))
        }
        when(code){
            45,48 -> for(i in 0..2)drawLine(cloud,pt(5f,23f+i*3),pt(28f,23f+i*3),u*1.4f,StrokeCap.Round)
            in 51..67,in 80..82 -> for(i in 0..2)drawLine(if(code in listOf(56,57,66,67))Color.White else blue,pt(10f+i*7,26f),pt(8f+i*7,30f),u*1.8f,StrokeCap.Round)
            71,73,75,77,85,86 -> for(i in 0..2){val x=9f+i*8;drawLine(Color.White,pt(x-2,28f),pt(x+2,28f),u);drawLine(Color.White,pt(x,26f),pt(x,30f),u)}
            95,96,99 -> {val p=Path().apply{moveTo(18*u,21*u);lineTo(13*u,27*u);lineTo(18*u,27*u);lineTo(15*u,32*u);lineTo(24*u,24*u);lineTo(19*u,24*u);close()};drawPath(p,sun);if(code!=95)drawCircle(Color.White,1.5f*u,pt(8f,29f))}
        }
}

@Composable
fun WeatherBadge() {
    val d=Weather.data
    val fresh=d!=null && System.currentTimeMillis()-d.fetched<3*3600_000L
    Surface(onClick={Experience.homePanel="weather"},modifier=Modifier.widthIn(min=50.dp,max=90.dp).height(26.dp).focusHint("Počasie · predpoveď a nastavenia",right=true),
        colors=tvFocusColors(Color(0xD9121418)),shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        scale=ClickableSurfaceDefaults.scale(focusedScale=1f),border=tvFocusBorder(RoundedCornerShape(10.dp))) {
        Row(Modifier.height(26.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            WeatherIcon(d?.code ?: -1,d?.daytime ?: true,Modifier.size(22.dp))
            Text(if(fresh)"${Weather.temperature(d!!.temperature,false)}" else "—",color=Color.White,fontSize=14.sp)
        }
    }
}

@Composable
fun WeatherPanel() {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var search by remember{mutableStateOf(false)};var query by remember{mutableStateOf("")}
    var results by remember{mutableStateOf(emptyList<WeatherPlace>())};var searching by remember{mutableStateOf(false)}
    var note by remember{mutableStateOf("")}
    var selectedDay by remember{mutableStateOf<String?>(null)}
    val searchFocus=remember{FocusRequester()}
    val searchButtonFocus=remember{FocusRequester()}
    LaunchedEffect(search){if(search){withFrameNanos{};runCatching{searchFocus.requestFocus()}}}
    LaunchedEffect(Unit){Weather.load(context);Weather.refresh(context)}
    fun back(){if(selectedDay!=null)selectedDay=null else if(search)search=false else Experience.homePanel=""}
    PanelShell(if(selectedDay!=null)"Hodinová predpoveď" else if(search)"Vyber mesto" else "Počasie",onBack={back()},closeLabel=if(selectedDay!=null || search)"Späť" else "Zavrieť",onClose={back()}) {
        if(selectedDay!=null) {
            val d=Weather.data
            val date=selectedDay!!
            val hours=remember(d,date){d?.hours?.filter{it.time.startsWith(date)}.orEmpty()}
            val zone=remember(d?.timezone){runCatching{java.time.ZoneId.of(d?.timezone ?: "Europe/Bratislava")}.getOrDefault(java.time.ZoneId.of("Europe/Bratislava"))}
            val now=java.time.LocalDateTime.now(zone)
            val initial=remember(date,d){if(date==now.toLocalDate().toString())hours.indexOfFirst{java.time.LocalDateTime.parse(it.time).hour>=now.hour}.coerceAtLeast(0) else 0}
            Text((d?.place?.name ?: "")+" · "+runCatching{java.time.LocalDate.parse(date).format(java.time.format.DateTimeFormatter.ofPattern("EEEE d. MMMM",com.conreo.couchytv.AppLanguage.locale()))}.getOrDefault(date),color=Color.White,fontSize=18.sp)
            Text("Hodinová predpoveď · časy platia pre vybrané miesto",color=Color(0xFFBACBD9),fontSize=12.sp)
            if(hours.isEmpty())Text("Hodinové údaje zatiaľ nie sú dostupné. Skús obnoviť počasie.",color=Color.White)
            else {
                val list=androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex=initial)
                LazyColumn(Modifier.panelListNavigation().weight(1f,false).heightIn(max=285.dp),state=list,contentPadding=PaddingValues(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(hours,key={it.time}){h->
                        val focus=remember{FocusRequester()}
                        Surface(onClick={},modifier=Modifier.fillMaxWidth().focusRequester(focus),colors=tvFocusColors(Color(0xFF25394B)),shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),scale=ClickableSurfaceDefaults.scale(focusedScale=1f),border=tvFocusBorder(RoundedCornerShape(12.dp))) {
                            Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                                Text(h.time.substringAfter('T'),Modifier.width(48.dp),color=Color.White,fontSize=16.sp)
                                WeatherIcon(h.code,h.daytime,Modifier.size(32.dp))
                                Column(Modifier.weight(1f)){Text(Weather.label(h.code,h.daytime),fontSize=14.sp,color=Color.White);Text("Pocitovo ${h.feels?.let{Weather.temperature(it,false)} ?: "—"}",fontSize=11.sp,color=Color(0xFFBACBD9))}
                                Text(h.temperature?.let{Weather.temperature(it)} ?: "—",Modifier.width(64.dp),color=Color.White,fontSize=17.sp)
                                Column(Modifier.width(115.dp)){Text("${h.rain?.let{"$it %"} ?: "—"} · ${h.amount?.let{String.format(com.conreo.couchytv.AppLanguage.locale(),"%.1f mm",it)} ?: "—"}",fontSize=13.sp,color=Color.White);Text("Zrážky",fontSize=10.sp,color=Color(0xFFBACBD9))}
                                Text(h.wind?.let{Weather.wind(it)} ?: "—",fontSize=13.sp,color=Color.White)
                            }
                        }
                        LaunchedEffect(date){if(h==hours[initial]){withFrameNanos{};runCatching{focus.requestFocus()}}}
                    }
                }
            }
        } else if(search) {
            Text("Napíš názov mesta a vyber ho z výsledkov.",color=Color(0xFFB9CAD8),fontSize=13.sp)
            BasicTextField(query,{query=it.take(80)},textStyle=TextStyle(color=Color.White,fontSize=20.sp),singleLine=true,
                modifier=Modifier.fillMaxWidth().focusRequester(searchFocus).onPreviewKeyEvent{e->
                    if(e.key==Key.DirectionDown){if(e.type==KeyEventType.KeyDown)runCatching{searchButtonFocus.requestFocus()};true}else false
                }.background(Color(0xFF304255),RoundedCornerShape(10.dp)).padding(14.dp))
            PanelButton(if(searching)"Hľadám…" else "Vyhľadať",modifier=Modifier.focusRequester(searchButtonFocus)) {
                if(!searching && query.trim().length>=2)scope.launch {
                    searching=true;note="";results=emptyList()
                    try{results=Weather.search(query);if(results.isEmpty())note="Nenašli sa žiadne mestá."}
                    catch(e:CancellationException){throw e}catch(e:Exception){note="Vyhľadávanie nie je dostupné."}finally{searching=false}
                }
            }
            if(results.isNotEmpty())LazyColumn(Modifier.panelListNavigation().heightIn(max=190.dp),contentPadding=PaddingValues(8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(results){p->PanelButton("${p.name} · ${p.detail}"){Weather.select(context,p);search=false;scope.launch{Weather.refresh(context)}}}
            }
            if(note.isNotEmpty())Text(note,color=Color(0xFFFFCEB1),fontSize=13.sp)
        } else {
            val d=Weather.data
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                WeatherIcon(d?.code ?: -1,d?.daytime ?: true,Modifier.size(80.dp))
                Column {
                    Text(Weather.place?.name ?: "Vyber mesto",fontSize=26.sp,color=Color.White)
                    Text(if(d!=null)"${Weather.temperature(d.temperature)} · ${Weather.label(d.code,d.daytime)}" else if(Weather.busy)"Načítavam predpoveď…" else "Údaj nie je dostupný",fontSize=21.sp,color=Color.White)
                    if(d!=null)Text("Pocitovo ${Weather.temperature(d.feels)} · vietor ${Weather.wind(d.wind)}",fontSize=13.sp,color=Color(0xFFBACBD9))
                    if(Weather.place?.automatic==true)Text("Približná poloha podľa internetového pripojenia",fontSize=11.sp,color=Color(0xFFBACBD9))
                }
            }
            if(d!=null)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                d.days.forEachIndexed{i,day->Surface(onClick={selectedDay=day.date},modifier=Modifier.weight(1f),colors=tvFocusColors(Color(0xFF25394B)),shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),scale=ClickableSurfaceDefaults.scale(focusedScale=1.02f),border=tvFocusBorder(RoundedCornerShape(12.dp))) { Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(if(i==0)"Dnes" else runCatching{java.time.LocalDate.parse(day.date).format(java.time.format.DateTimeFormatter.ofPattern("EEE d. M.",com.conreo.couchytv.AppLanguage.locale()))}.getOrDefault(day.date),fontSize=12.sp,color=Color.White)
                    WeatherIcon(day.code,true,Modifier.padding(vertical=6.dp).size(36.dp))
                    Text("${Weather.temperature(day.high,false)} / ${Weather.temperature(day.low,false)}",fontSize=15.sp,color=Color.White)
                    Text("Pravdepodobnosť zrážok: ${day.rain} %",fontSize=10.sp,color=Color(0xFFBACBD9))
                }}}
            }
            if(Weather.error.isNotEmpty())Text(Weather.error,color=Color(0xFFFFCEB1),fontSize=12.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                PanelButton("Zmeniť mesto",initial=true){search=true}
                PanelButton(if(Weather.busy)"Obnovujem…" else "Obnoviť"){if(!Weather.busy)scope.launch{Weather.refresh(context,true)}}
            }
            PanelButton(if(Weather.enabled)"✓ Zobraziť počasie na ploche" else "Zobraziť počasie na ploche"){Weather.setEnabled(context,!Weather.enabled)}
            if(note.isNotEmpty())Text(note,color=Color(0xFFFFCEB1),fontSize=12.sp)
            PanelButton(if(Weather.imperial)"°F · mph" else "°C · km/h"){Weather.setImperial(context,!Weather.imperial)}
            PanelButton("Open-Meteo · licencia a podmienky") { openWeb(context,"https://open-meteo.com/en/terms") }
            Text("Open-Meteo · CC BY 4.0 · nekomerčné API · obnova každých 30 minút na ploche"+(d?.let{" · načítané "+java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault()).format(java.util.Date(it.fetched))} ?: ""),fontSize=10.sp,color=Color(0xFFA5B7C8))
        }
    }
}
