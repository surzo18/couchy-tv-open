package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.conreo.couchytv.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun JellyfinShelf(items:List<ResumeItem>,loaded:Boolean,nextUp:Boolean,error:String,firstFocus:FocusRequester,
    onMode:(Boolean)->Unit,onRefresh:()->Unit,onManage:()->Unit,onRandom:()->Unit) {
    val context=LocalContext.current
    var selectedId by remember(nextUp){mutableStateOf("")}
    val selected=items.firstOrNull{it.id==selectedId} ?: items.firstOrNull()
    val grid=rememberLazyGridState()
    LaunchedEffect(nextUp){grid.scrollToItem(0)}
    Column(Modifier.fillMaxWidth().fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
            StudioButton(if(!nextUp)"● Rozpozerané" else "Rozpozerané"){onMode(false)}
            StudioButton(if(nextUp)"● Ďalšie epizódy" else "Ďalšie epizódy"){onMode(true)}
            StudioButton("Náhodný výber",onClick=onRandom)
            Spacer(Modifier.weight(1f))
            StudioButton("Obnoviť",onClick=onRefresh)
            StudioButton("Účet a pripojenie",onClick=onManage)
        }
        if(selected==null) {
            Column(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF182637),RoundedCornerShape(22.dp)).padding(28.dp),verticalArrangement=Arrangement.Center) {
                Text(if(!loaded)"Načítavam Jellyfin…" else if(error.isNotEmpty())"Spojenie sa nepodarilo" else "Zatiaľ tu nie sú žiadne tituly",fontSize=25.sp,color=Color.White)
                Text(if(error.isNotEmpty())error else if(!loaded)"Načítavam knižnicu z Jellyfinu." else if(nextUp)"Tu sa zobrazia ďalšie dostupné epizódy podľa histórie sledovania." else "Tu sa zobrazia filmy a epizódy, ktoré si ešte nedopozeral.",color=Color(0xFFBAC9D7),fontSize=15.sp,modifier=Modifier.padding(top=14.dp))
            }
        } else Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
            Column(Modifier.width(274.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(Color(0xFF293E50),Color(0xFF142130))),RoundedCornerShape(18.dp)).padding(16.dp)) {
                Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){
                    JellyfinPoster(selected,Modifier.fillMaxHeight().aspectRatio(2f/3f,matchHeightConstraintsFirst=true),ContentScale.Crop)
                }
                Text(selected.title,fontSize=23.sp,color=Color.White,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=12.dp))
                Text(selected.subtitle,fontSize=13.sp,color=Color(0xFFBAC9D7),maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=6.dp))
                if(!nextUp) {
                    Box(Modifier.fillMaxWidth().padding(top=10.dp).height(4.dp).background(Color(0xFF405363))) {
                        Box(Modifier.fillMaxWidth(selected.progress/100f).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                    }
                    Text("${selected.progress} % pozreté"+(if(selected.remainingMinutes>0)" · zostáva ${selected.remainingMinutes} min" else ""),fontSize=11.sp,color=Color(0xFFBAC9D7),modifier=Modifier.padding(top=6.dp))
                }
                Text("▶  OK otvorí v Jellyfine",fontSize=12.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=12.dp))
            }
            LazyVerticalGrid(GridCells.Fixed(2),state=grid,modifier=Modifier.weight(1f).fillMaxHeight(),contentPadding=PaddingValues(12.dp),
                horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                items(items,key={it.id}) { item ->
                    Surface(onClick={JellyfinLink.open(context,item)},
                        modifier=Modifier.fillMaxWidth().then(if(item.id==items.first().id)Modifier.focusRequester(firstFocus) else Modifier).onFocusChanged{if(it.hasFocus)selectedId=item.id},
                        shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(15.dp)),colors=tvFocusColors(Color(0xFF213246)),
                        border=tvFocusBorder(RoundedCornerShape(15.dp)),scale=ClickableSurfaceDefaults.scale(focusedScale=1.025f)) {
                        Column(Modifier.padding(12.dp)) {
                            JellyfinPoster(item,Modifier.fillMaxWidth().height(126.dp),ContentScale.Crop,wide=true)
                            Text(item.title,fontSize=16.sp,color=Color.White,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=10.dp))
                            Text(item.subtitle,fontSize=11.sp,color=Color(0xFFB8C8D6),maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=4.dp))
                            if(!nextUp)Box(Modifier.fillMaxWidth().padding(top=9.dp).height(3.dp).background(Color(0xFF435466))) {
                                Box(Modifier.fillMaxWidth(item.progress/100f).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JellyfinPoster(item:ResumeItem,modifier:Modifier,scale:ContentScale,wide:Boolean=false) {
    val context=LocalContext.current
    var loaded by remember(item.id,item.imageId,wide){mutableStateOf(false)}
    val image by produceState<android.graphics.Bitmap?>(null,item.id,item.imageId,wide) {
        value=null
        value=withContext(Dispatchers.IO){JellyfinLink.artwork(context,item,wide)}
        loaded=true
    }
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Color(0xFF294357),Color(0xFF172636)))),contentAlignment=Alignment.Center) {
        if(image!=null)Image(image!!.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=if(wide && image!!.width<image!!.height*1.2f)ContentScale.Fit else scale)
        else Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text("▶",color=Color(0xFF9EBBD2),fontSize=24.sp)
            Text(if(loaded)"Bez náhľadu" else "Načítavam…",color=Color(0xFFBACDDE),fontSize=10.sp,maxLines=1)
        }
    }
}
