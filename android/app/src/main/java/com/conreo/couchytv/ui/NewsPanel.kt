package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.conreo.couchytv.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import android.graphics.Bitmap
import android.graphics.BitmapFactory

private class NewsPictures {
    val gate=Semaphore(2)
    val cache=object:android.util.LruCache<String,Bitmap>(2*1024*1024){override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount}
    suspend fun load(url:String):Bitmap?=withContext(Dispatchers.IO) {
        if(!HomeNews.imageAllowed(url))return@withContext null
        cache.get(url) ?: gate.withPermit {
            cache.get(url) ?: runCatching {
                val c=java.net.URL(url).openConnection() as java.net.HttpURLConnection
                c.connectTimeout=5000;c.readTimeout=5000;c.instanceFollowRedirects=false
                try {
                    require(c.responseCode==200)
                    val out=java.io.ByteArrayOutputStream()
                    c.inputStream.use{stream->val buffer=ByteArray(8192);while(true){val n=stream.read(buffer);if(n<0)break;require(out.size()+n<=768000);out.write(buffer,0,n)}}
                    val bytes=out.toByteArray();val opts=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                    BitmapFactory.decodeByteArray(bytes,0,bytes.size,opts)
                    opts.inJustDecodeBounds=false;opts.inSampleSize=1
                    while(opts.outWidth/opts.inSampleSize>520 || opts.outHeight/opts.inSampleSize>320)opts.inSampleSize*=2
                    BitmapFactory.decodeByteArray(bytes,0,bytes.size,opts)
                }finally{c.disconnect()}
            }.getOrNull()?.also{cache.put(url,it)}
        }
    }
}
@Composable
fun NewsPanel() {
    val context=LocalContext.current
    val pictures=remember{NewsPictures()}
    var loaded by remember{mutableStateOf(false)}
    var source by remember{mutableStateOf("Všetky")}
    val scope=rememberCoroutineScope()
    LaunchedEffect(NewsSources.revision){loaded=false;source="Všetky";NewsUi.replace(HomeNews.load());loaded=true}
    DisposableEffect(Unit){onDispose{pictures.cache.evictAll()}}
    val sources=listOf("Všetky")+HomeNews.sources
    val listState=androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(source){listState.scrollToItem(0)}
    val articles=remember(NewsUi.items,source){NewsUi.items.filter{source=="Všetky" || it.source==source}.sortedByDescending{it.published}}
    val date=remember{java.text.SimpleDateFormat("d. MMMM · HH:mm",com.conreo.couchytv.AppLanguage.locale())}
    PanelShell("Správy",width=880.dp) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            PanelButton(if(NewsUi.enabled)"Skryť pás na ploche" else "Zobraziť pás na ploche",initial=true){NewsUi.enabled=!NewsUi.enabled;HomeNews.setEnabled(context,NewsUi.enabled)}
            PanelButton(if(loaded)"Obnoviť" else "Načítavam…"){if(loaded)scope.launch{loaded=false;try{NewsUi.replace(HomeNews.load(force=true))}finally{loaded=true}}}
            PanelButton("Zdroje správ"){(context as? android.app.Activity)?.let{NewsSourceDialogs(it){}.show()}}
            Text("${HomeNews.sources.size} zdrojov · najnovšie hore",fontSize=12.sp,color=Color(0xFFBACBD9))
        }
        LazyRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(4.dp)) {
            items(sources){name->PanelButton((if(source==name)"✓ " else "")+name){source=name}}
        }
        if(articles.isEmpty())Text(if(NewsSources.all.isEmpty())"Pridaj vlastný RSS alebo Atom zdroj cez Zdroje správ. Rešpektuj podmienky vydavateľa." else if(!loaded)"Načítavam správy…" else if(source=="Všetky")"Správy sa nepodarilo načítať. Skús Obnoviť. Ďalší pokus prebehne automaticky o minútu." else "$source teraz nemá dostupné správy z posledných 48 hodín.",color=Color.White,fontSize=14.sp)
        else LazyColumn(Modifier.panelListNavigation().weight(1f,fill=false).heightIn(max=310.dp),state=listState,contentPadding=PaddingValues(10.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items(articles,key={it.url}){item->
                val bitmap by produceState<Bitmap?>(null,item.image){value=pictures.load(item.image)}
                Surface(onClick={openWeb(context,item.url)},scale=ClickableSurfaceDefaults.scale(focusedScale=1f),
                    shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),border=tvFocusBorder(RoundedCornerShape(14.dp)),
                    colors=tvFocusColors(Color(0xFF243747))) {
                    Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        if(item.image.isNotBlank())Box(Modifier.size(130.dp,82.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF172737)),contentAlignment=Alignment.Center) {
                            if(bitmap!=null)Image(bitmap!!.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                            else Icon(AppIcons.News,null,Modifier.size(26.dp),tint=Color(0xFF8EA6BF))
                        }
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                val color=Color(HomeNews.sourceColor(item.source))
                                Text(item.source,Modifier.background(color.copy(alpha=.12f),RoundedCornerShape(5.dp)).padding(horizontal=7.dp,vertical=3.dp),fontSize=12.sp,color=color)
                                Text(item.section+(if(item.published>0)" · "+date.format(java.util.Date(item.published)) else ""),fontSize=11.sp,color=Color(0xFFBBCBD7))
                            }
                            Text(item.title,fontSize=18.sp,color=Color.White,maxLines=2,overflow=TextOverflow.Ellipsis)
                            if(item.summary.isNotBlank() && item.summary!=item.title)Text(item.summary,fontSize=13.sp,color=Color(0xFFD0DCE5),maxLines=2,overflow=TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
