package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.conreo.couchytv.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@Composable fun JellyfinRandom(onClose:()->Unit,onReconnect:()->Unit) {
    val context=LocalContext.current
    var kind by remember{mutableIntStateOf(0)}
    var favorites by remember{mutableStateOf(false)}
    var revision by remember{mutableIntStateOf(0)}
    var series by remember{mutableStateOf<LibraryItem?>(null)}
    var season by remember{mutableStateOf<LibraryItem?>(null)}
    var offset by remember{mutableIntStateOf(0)}
    var page by remember{mutableStateOf(LibraryPage(emptyList(),0))}
    var selection by remember{mutableStateOf<LibraryItem?>(null)}
    var loading by remember{mutableStateOf(true)}
    var error by remember{mutableStateOf("")}
    var expired by remember{mutableStateOf(false)}
    val pages=remember{linkedMapOf<String,LibraryPage>()}
    val list=rememberLazyListState()
    val backFocus=remember{FocusRequester()}
    LaunchedEffect(series?.id,season?.id){withFrameNanos{};backFocus.requestFocus()}
    LaunchedEffect(loading){if(!loading&&page.items.isNotEmpty())list.scrollToItem(0)}
    fun back(){when{season!=null->{season=null;offset=0};series!=null->series=null;else->onClose()}}
    BackHandler{back()}
    LaunchedEffect(kind,favorites,revision,series?.id,season?.id,offset){
        loading=true;error="";expired=false;selection=null;page=LibraryPage(emptyList(),0)
        try {
            val cacheKey="$kind/$favorites/$revision/${series?.id}/${season?.id}/$offset"
            page=pages[cacheKey]?:withContext(Dispatchers.IO){JellyfinLink.library(context,listOf("Movie,Series","Movie","Series")[kind],favorites,series?.id.orEmpty(),season?.id.orEmpty(),offset)}.also{
                if(pages.size>=8)pages.remove(pages.keys.first())
                pages[cacheKey]=it
            }
            selection=page.items.firstOrNull()
        }catch(e:CancellationException){throw e}
        catch(e:Exception){error=e.message?:"Server nie je dostupný.";expired=e is JellyfinAuthExpired;DiagnosticLog.report("Jellyfin","CHYBA","Náhodný výber sa nepodarilo načítať.",e)}
        finally{loading=false}
    }
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            StudioButton("‹ Späť",Modifier.focusRequester(backFocus)){back()}
            Text(if(season!=null)"${series?.title} · ${season?.title}" else series?.title?:"Náhodný výber",color=Color.White,fontSize=23.sp,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f).padding(top=8.dp))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            if(series==null){
                StudioButton(listOf("Filmy aj seriály","Filmy","Seriály")[kind]){kind=(kind+1)%3}
                StudioButton(if(favorites)"♥ Obľúbené" else "Celá knižnica"){favorites=!favorites}
                StudioButton(if(loading)"Načítavam…" else "Premiešať"){if(!loading)revision++}
            }else if(season!=null){
                if(offset>0)StudioButton("‹ Predchádzajúce"){if(!loading)offset=(offset-30).coerceAtLeast(0)}
                Text("${if(page.items.isEmpty())0 else offset+1}–${offset+page.items.size} / ${page.total}",color=Color.White,modifier=Modifier.padding(10.dp))
                if(offset+page.items.size<page.total)StudioButton("Ďalšie ›"){if(!loading)offset+=30}
            }
        }
        if(loading||page.items.isEmpty()){
            Text(if(loading)"Načítavam z tvojej knižnice…" else error.ifBlank{if(favorites)"V tomto výbere nie sú obľúbené tituly. Označ ich srdiečkom v Jellyfine alebo vyber celú knižnicu." else "V tomto výbere nie sú dostupné tituly."},color=Color(0xFFD3DFEB),fontSize=18.sp,modifier=Modifier.padding(24.dp))
            if(error.isNotEmpty())StudioButton(if(expired)"Znovu pripojiť účet" else "Skúsiť znova"){if(expired)onReconnect() else revision++}
        }else Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(18.dp)){
            val item=selection?:page.items.first()
            Column(Modifier.weight(1f).fillMaxHeight().background(Brush.verticalGradient(listOf(Color(0xFF293E50),Color(0xFF142130))),RoundedCornerShape(18.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){
                    JellyfinPoster(item.poster(),Modifier.width(68.dp).height(102.dp),ContentScale.Crop)
                    Column(Modifier.weight(1f)){
                        Text(item.title,color=Color.White,fontSize=21.sp,maxLines=3,overflow=TextOverflow.Ellipsis)
                        Text(item.subtitle,color=MaterialTheme.colorScheme.primary,fontSize=14.sp,modifier=Modifier.padding(top=8.dp))
                    }
                }
                key(item.id){
                    var descriptionFocused by remember{mutableStateOf(false)}
                    val descriptionScroll=rememberScrollState()
                    val scope=rememberCoroutineScope()
                    val step=with(LocalDensity.current){60.dp.toPx().toInt()}
                    Text(item.overview.ifBlank{"Popis nie je dostupný."},color=Color(0xFFCEDBE8),fontSize=15.sp,modifier=Modifier.weight(1f).fillMaxWidth().border(1.dp,if(descriptionFocused)MaterialTheme.colorScheme.primary else Color.Transparent,RoundedCornerShape(6.dp)).padding(5.dp).onPreviewKeyEvent{event->
                        if(event.type!=KeyEventType.KeyDown)false else {
                            val delta=when{event.key==Key.DirectionDown&&descriptionScroll.canScrollForward->step;event.key==Key.DirectionUp&&descriptionScroll.canScrollBackward->-step;else->0}
                            if(delta==0)false else {scope.launch{descriptionScroll.scrollTo((descriptionScroll.value+delta).coerceIn(0,descriptionScroll.maxValue))};true}
                        }
                    }.verticalScroll(descriptionScroll).onFocusChanged{descriptionFocused=it.hasFocus}.focusable())
                }
                StudioButton(if(item.type=="Series")"Vybrať sériu" else if(item.type=="Season")"Vybrať epizódu" else "Otvoriť v Jellyfine"){
                    when(item.type){"Series"->{series=item;offset=0};"Season"->{season=item;offset=0};else->JellyfinLink.open(context,item.poster())}
                }
            }
            LazyColumn(state=list,modifier=Modifier.panelListNavigation().weight(1f),contentPadding=PaddingValues(10.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                items(page.items,key={it.id}){entry->
                    Surface(onClick={selection=entry;when(entry.type){"Series"->{series=entry;offset=0};"Season"->{season=entry;offset=0};else->JellyfinLink.open(context,entry.poster())}},
                        modifier=Modifier.fillMaxWidth().onFocusChanged{if(it.hasFocus)selection=entry},
                        shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),colors=tvFocusColors(Color(0xFF213246)),border=tvFocusBorder(RoundedCornerShape(14.dp)),scale=ClickableSurfaceDefaults.scale(focusedScale=1.02f)){
                        Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                            JellyfinPoster(entry.poster(),Modifier.width(144.dp).height(81.dp),ContentScale.Crop,wide=true)
                            Column(Modifier.weight(1f)){
                                Text(entry.title,color=Color.White,fontSize=17.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                                Text(entry.subtitle,color=Color(0xFFBACDDE),fontSize=13.sp,modifier=Modifier.padding(top=8.dp))
                                if(entry.overview.isNotBlank())Text(entry.overview,color=Color(0xFFBACDDE),fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
