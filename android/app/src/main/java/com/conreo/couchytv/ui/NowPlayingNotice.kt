package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import com.conreo.couchytv.*
import kotlinx.coroutines.delay

internal object NowPlayingNoticeState{var visible by mutableStateOf(false)}

/** Metadata callbacks trigger one short notice; no polling, artwork download, or history. */
@Composable fun BoxScope.NowPlayingNotice(allowed:Boolean){
    val state by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val external=ExternalMedia.current
    val externalPlaying=external?.playing==true && external.clock.advancing
    val localPlaying=HomeMusic.enabled && HomeMusic.status.startsWith("Hrá:")
    val title=if(externalPlaying)external!!.title else if(localPlaying)HomeMusic.status.removePrefix("Hrá:").trim() else ""
    val detail=if(externalPlaying)listOf(external!!.app,external.artist).filter{it.isNotBlank()}.joinToString(" · ") else HomeMusic.detail.ifBlank { "Hudba k pozadiu" }
    val identity=if(title.isBlank())"" else (if(externalPlaying)external!!.packageName+":"+external.artist else "local")+":"+title
    var lastAnnounced by remember{mutableStateOf("")}
    var visible by remember{mutableStateOf(false)}
    LaunchedEffect(identity,state,allowed){
        visible=false
        if(identity.isNotBlank() && identity!=lastAnnounced){
            lastAnnounced=identity
            if(allowed && state==Lifecycle.State.RESUMED){visible=true;delay(5000);visible=false}
        }
    }
    val show=visible && allowed && identity.isNotBlank() && state==Lifecycle.State.RESUMED
    SideEffect{NowPlayingNoticeState.visible=show && !Experience.artOnly}
    DisposableEffect(Unit){onDispose{NowPlayingNoticeState.visible=false}}
    val art=Experience.artOnly
    val position=if(art)Alignment.TopCenter else Alignment.TopStart
    val top=if(art)26.dp else if(external!=null)104.dp else 66.dp
    Box(Modifier.align(position).padding(start=if(art)0.dp else 40.dp,top=top)){
        AnimatedVisibility(show,enter=if(art)fadeIn() else slideInHorizontally{ -it }+fadeIn(),exit=fadeOut()){
            Column(Modifier.widthIn(max=if(art)460.dp else 326.dp)
                .background(Color(0xEB111D2A),RoundedCornerShape(12.dp)).padding(horizontal=16.dp,vertical=11.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text("♫  PRÁVE HRÁ",fontSize=11.sp,color=Color(0xFFAECFF0))
                Text(title,fontSize=16.sp,color=Color.White,maxLines=2,overflow=TextOverflow.Ellipsis)
                if(!art)Text(detail,fontSize=11.sp,color=Color(0xFFC1CFDD),maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}
