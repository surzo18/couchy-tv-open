package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import com.conreo.couchytv.Experience
import kotlinx.coroutines.delay

private object FocusHints {
    var text by mutableStateOf("")
    var right by mutableStateOf(false)
}
fun Modifier.focusHint(text:String,right:Boolean=false)=onFocusChanged {
    if(it.hasFocus){FocusHints.text=text;FocusHints.right=right}
    else if(FocusHints.text==text)FocusHints.text=""
}
@Composable
fun BoxScope.FocusHintOverlay() {
    val state by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    var visible by remember{mutableStateOf(false)}
    LaunchedEffect(FocusHints.text,Experience.artOnly,Experience.homePanel,state) {
        visible=false
        if(FocusHints.text.isNotBlank() && !Experience.artOnly && Experience.homePanel.isEmpty() && state==Lifecycle.State.RESUMED){delay(700);visible=true}
    }
    val belowExtraRow=if(FocusHints.right)(com.conreo.couchytv.Weather.enabled || (NewsUi.enabled && NewsUi.items.isNotEmpty())) else com.conreo.couchytv.ExternalMedia.current!=null
    if(visible && !(FocusHints.right && MemoryPortalState.visible) && !(!FocusHints.right && NowPlayingNoticeState.visible))Text(FocusHints.text,color=Color.White,fontSize=12.sp,maxLines=2,
        modifier=Modifier.align(if(FocusHints.right)Alignment.TopEnd else Alignment.TopStart).padding(horizontal=40.dp).offset(y=if(belowExtraRow)104.dp else 66.dp)
            .widthIn(max=390.dp).background(Color(0xF21C2A39),RoundedCornerShape(10.dp)).padding(horizontal=14.dp,vertical=9.dp))
}
