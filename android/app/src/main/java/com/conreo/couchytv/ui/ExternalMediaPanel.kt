package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import android.media.session.PlaybackState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.conreo.couchytv.*
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
private fun TrackTime(clock:MediaClock,visible:Boolean,progressColor:Color?=null) {
    val state by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    var now by remember(clock){mutableLongStateOf(android.os.SystemClock.elapsedRealtime())}
    LaunchedEffect(clock,visible,state){
        now=android.os.SystemClock.elapsedRealtime()
        if(visible && state==Lifecycle.State.RESUMED && clock.advancing && clock.position>=0)
            while(true){delay(1000);now=android.os.SystemClock.elapsedRealtime()}
    }
    val label=clock.label(now)
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(progressColor!=null && clock.duration>0 && clock.position>=0)Canvas(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))){
            drawRect(Color(0xFF485264))
            drawRect(progressColor,size=androidx.compose.ui.geometry.Size(size.width*(clock.positionAt(now).toFloat()/clock.duration).coerceIn(0f,1f),size.height))
        }
        if(label.isNotEmpty())Text(label,fontSize=12.sp,color=Color(0xFFCAD7E2),maxLines=1,
            style=MaterialTheme.typography.labelMedium.copy(fontFeatureSettings="tnum"))
    }
}

@Composable
fun ExternalMediaBadge(modifier:Modifier=Modifier) {
    val track=ExternalMedia.current ?: return
    Surface(onClick={Experience.homePanel="externalMusic"},modifier=modifier.height(26.dp),
        shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),colors=tvFocusColors(Color(0xD9121418)),
        scale=ClickableSurfaceDefaults.scale(focusedScale=1f),border=tvFocusBorder(RoundedCornerShape(10.dp))) {
        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(buildAnnotatedString {
                append(if(track.playing)"♫ " else "Ⅱ ")
                withStyle(SpanStyle(color=mediaAppColor(track.packageName))){append(track.app)}
                append(" · "+track.title+(if(track.artist.isNotBlank())" — "+track.artist else ""))
            },
                modifier=Modifier.weight(1f),fontSize=12.sp,color=Color.White,maxLines=1,overflow=TextOverflow.Ellipsis)
            TrackTime(track.clock,!Experience.artOnly && Experience.homePanel.isEmpty())
        }
    }
}
private fun mediaAppColor(pkg:String)=when {
    pkg.startsWith("com.spotify.")->Color(0xFF1ED760)
    pkg.startsWith("com.google.android.youtube")->Color(0xFFFF7272)
    pkg.startsWith("org.jellyfin.")->Color(0xFFC3A0FF)
    pkg.startsWith("com.netflix.")->Color(0xFFFF7272)
    pkg.startsWith("org.videolan.")->Color(0xFFFFB75E)
    else->Color(0xFFE3ECF4)
}
@Composable
fun ExternalMediaPanel() {
    val context=LocalContext.current
    val t=ExternalMedia.current
    PanelShell("Práve hrá",waitForRelease=true,width=720.dp) {
        if(t==null)Text("Aplikácia už neposkytuje ovládanie prehrávania.",color=Color.White)
        else {
            val accent=mediaAppColor(t.packageName)
            val art by produceState<android.graphics.Bitmap?>(null,t.packageName,t.title,t.artist){
                value=null
                val original=ExternalMedia.artwork(t.packageName)
                value=withContext(Dispatchers.Default){original?.takeIf{!it.isRecycled}?.let{
                    val scale=minOf(1f,480f/maxOf(it.width,it.height))
                    runCatching{android.graphics.Bitmap.createScaledBitmap(it,(it.width*scale).toInt().coerceAtLeast(1),(it.height*scale).toInt().coerceAtLeast(1),true)}.getOrNull()
                }}
            }
            Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(24.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(160.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(accent.copy(alpha=.28f),Color(0xFF17232F)))),contentAlignment=Alignment.Center){
                    art?.let{Image(it.asImageBitmap(),"Obal albumu",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)} ?: Text("♫",fontSize=68.sp,color=accent)
                }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(t.app+" · "+if(t.playing)"prehráva" else "pozastavené",color=accent,fontSize=14.sp)
                    Text(t.title,color=Color.White,fontSize=26.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                    if(t.artist.isNotEmpty())Text(t.artist,color=Color(0xFFD2DDE8),fontSize=16.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    TrackTime(t.clock,true,accent)
                }
            }
            Row(Modifier.padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                if(ExternalMedia.supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS))PanelButton("◀ Predošlá"){ExternalMedia.previous()}
                if(ExternalMedia.supports(PlaybackState.ACTION_PLAY_PAUSE) || ExternalMedia.supports(if(t.playing)PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY))
                    PanelButton(if(t.playing)"Ⅱ Pozastaviť" else "▶ Prehrať",initial=true){ExternalMedia.toggle()}
                if(ExternalMedia.supports(PlaybackState.ACTION_SKIP_TO_NEXT))PanelButton("Ďalšia ▶"){ExternalMedia.next()}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                PanelButton("Otvoriť "+t.app){ExternalMedia.open(context,t.packageName)}
                PanelButton("Hudba na ploche"){Experience.homePanel="music"}
            }
            Text("Dostupné ovládanie závisí od prehrávajúcej aplikácie.",fontSize=12.sp,color=Color(0xFFBACBD9))
        }
    }
}
