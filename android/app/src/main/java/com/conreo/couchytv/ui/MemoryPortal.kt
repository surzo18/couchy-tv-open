package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.animation.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import com.conreo.couchytv.*
internal object MemoryPortalState {var visible by mutableStateOf(false)}

@Composable private fun MemoryImage(p:AlbumPhoto,modifier:Modifier,small:Boolean=false){
    val c=LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null,p.uri,small){value=null;value=withContext(Dispatchers.IO){runCatching{c.contentResolver.loadThumbnail(p.uri,android.util.Size(if(small)320 else 960,if(small)180 else 540),null)}.getOrNull()}}
    Box(modifier.background(Color(0xFF162434)),contentAlignment=Alignment.Center){bitmap?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=if(small)ContentScale.Crop else ContentScale.Fit)}}
}
@Composable fun MemoryPortal(enabled:Boolean=true){
    val allowed by rememberUpdatedState(enabled)
    val c=LocalContext.current;val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var photos by remember{mutableStateOf(emptyList<AlbumPhoto>())};var open by remember{mutableStateOf(false)}
    var visible by remember{mutableStateOf(false)};var focused by remember{mutableStateOf(false)}
    var chosen by remember{mutableStateOf<AlbumPhoto?>(null)}
    var memoryIndex by remember{mutableIntStateOf(0)}
    val viewer=androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()){open=true;Experience.homePanel="memory";Experience.touch()}
    LaunchedEffect(lifecycle){lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){
        photos=withContext(Dispatchers.IO){if(AlbumStore.portal(c))runCatching{AlbumStore.eligible(c,albumPhotos(c))}.getOrDefault(emptyList()) else emptyList()}
        delay(20_000)
        try{while(true){
            if(allowed && photos.isNotEmpty() && !Experience.artOnly && Experience.homePanel.isEmpty()){
                chosen=withContext(Dispatchers.IO){AlbumStore.nextMemory(c,photos)}
                visible=true;delay(AlbumStore.portalDuration(c)*1000L)
                while(focused || open){Experience.touch();delay(5000)}
                visible=false
            }
            delay(AlbumStore.portalInterval(c)*60_000L)
        }}finally{visible=false;open=false;if(Experience.homePanel=="memory")Experience.homePanel=""}
    }}
    SideEffect{MemoryPortalState.visible=allowed && visible && !Experience.artOnly && Experience.homePanel.isEmpty()}
    DisposableEffect(Unit){onDispose{MemoryPortalState.visible=false}}
    if(photos.isEmpty())return
    val lead=chosen?:return
    val notes=AlbumStore.notes(c,lead)
    AnimatedVisibility(visible=allowed && visible && !Experience.artOnly && Experience.homePanel.isEmpty(),enter=slideInHorizontally{it}+fadeIn(),exit=slideOutHorizontally{it}+fadeOut()){
    val accent=MaterialTheme.colorScheme.primary
    Surface(onClick={memoryIndex=0;open=true;Experience.homePanel="memory"},modifier=Modifier.width(326.dp).onFocusChanged{focused=it.hasFocus},colors=tvFocusColors(Color(0xD9121418)),shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),border=tvFocusBorder(RoundedCornerShape(16.dp)),scale=ClickableSurfaceDefaults.scale(focusedScale=1.02f)){
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
            MemoryImage(lead,Modifier.size(92.dp,92.dp).clip(RoundedCornerShape(10.dp)),small=true)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("SPOMIENKA"+(if(notes.date.isNotEmpty())" · "+notes.date.takeLast(5).split('-').reversed().joinToString(".") else ""),fontSize=10.sp,lineHeight=12.sp,color=accent,fontWeight=FontWeight.Medium)
                Text(AlbumStore.name(c,notes.album),fontSize=15.sp,lineHeight=18.sp,fontWeight=FontWeight.SemiBold,color=Color.White,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(notes.caption,fontSize=11.sp,lineHeight=14.sp,color=Color(0xFFD0DDE9),maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("OK · Otvoriť spomienku",fontSize=10.sp,lineHeight=12.sp,color=accent)
            }
        }
    }
    }
    val batch=remember(photos,lead){listOf(lead)+photos.filter{it.uri!=lead.uri && AlbumStore.notes(c,it).let{n->n.album==notes.album && (notes.date.isEmpty() || n.date==notes.date)}}.shuffled().take(11)}
    if(open && Experience.homePanel=="memory"){
        val index=memoryIndex.mod(batch.size);val p=batch[index];val n=AlbumStore.notes(c,p)
        fun close(){open=false;visible=false;Experience.homePanel="";Experience.touch()}
        PanelShell("Spomienka · "+AlbumStore.name(c,n.album),width=760.dp,onBack={close()},onClose={close()}){
            MemoryImage(p,Modifier.fillMaxWidth().height(165.dp).clip(RoundedCornerShape(12.dp)))
            Text(n.caption.ifEmpty{p.name},color=Color.White,fontSize=18.sp,maxLines=2)
            Text(listOf(n.date,n.place,"${index+1} / ${batch.size}").filter{it.isNotBlank()}.joinToString(" · "),color=Color(0xFFB4C9DC),fontSize=12.sp)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                PanelButton("◀"){memoryIndex=(index-1).mod(batch.size)}
                PanelButton("▶"){memoryIndex=(index+1).mod(batch.size)}
                PanelButton("Celá obrazovka",initial=true,modifier=Modifier.weight(1f)){viewer.launch(android.content.Intent(c,AlbumPreviewActivity::class.java).putExtra("photo",p.uri.toString()))}
                PanelButton("Fotografie z tohto dňa",modifier=Modifier.weight(1f)){close();c.startActivity(android.content.Intent(c,AlbumActivity::class.java).putExtra("album",n.album).putExtra("day",n.date))}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                PanelButton("Nastavenie spomienok"){close();c.startActivity(android.content.Intent(c,StudioActivity::class.java).putExtra("tab","Fotoalbum").putExtra("memory_settings",true))}
                if(n.place.isNotEmpty())PanelButton("Zobraziť na mape"){close();AlbumStore.map(c,n.place)}
                if(p.video)PanelButton("Video"){close();runCatching{c.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(p.uri,"video/mp4").addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION))}.onFailure{Actions.toast(c,"Prehrávač videa nie je dostupný.")}}
            }
        }
    }
}
