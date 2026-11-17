package com.conreo.couchytv

import com.conreo.couchytv.ui.LocalizedText as Text
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.conreo.couchytv.game.CrossingArt
import com.conreo.couchytv.ui.*

object GameHub {
    fun last(c:Context)=c.getSharedPreferences("games",0).getString("last","crossing") ?: "crossing"
    fun launch(c:Context,id:String=last(c)) {
        val chosen=if(id=="scooter")"scooter" else "crossing"
        Experience.homePanel="";Experience.gameActive=true
        runCatching{c.startActivity(Intent(c,if(chosen=="scooter")ScooterActivity::class.java else CrossingActivity::class.java))}
            .onSuccess{c.getSharedPreferences("games",0).edit().putString("last",chosen).apply()}
            .onFailure{Experience.gameActive=false;Actions.toast(c,"Hru sa nepodarilo otvoriť.")}
    }
}

@Composable
fun GamePicker() {
    val c=LocalContext.current
    val last=remember{GameHub.last(c)}
    PanelShell("Tvoja herňa",waitForRelease=true,width=760.dp) {
        Text("Na hernej ikone stlač OK pre poslednú hru. Podrž OK pre výber inej hry.",color=Color(0xFFB8C9D8),fontSize=13.sp)
        Row(Modifier.padding(vertical=8.dp,horizontal=6.dp),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
            GameChoice(false,last=="crossing",Modifier.weight(1f)){GameHub.launch(c,"crossing")}
            GameChoice(true,last=="scooter",Modifier.weight(1f)){GameHub.launch(c,"scooter")}
        }
        Text("Každá hra má vlastné mince, rekordy a odomknuté postavičky.",color=Color(0xFFB8C9D8),fontSize=12.sp)
    }
}

@Composable
private fun GameChoice(scooter:Boolean,initial:Boolean,modifier:Modifier,onClick:()->Unit) {
    val focus=remember{FocusRequester()}
    val paint=remember{android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)}
    val rect=remember{android.graphics.RectF()}
    Surface(onClick=onClick,modifier=modifier.focusRequester(focus),
        shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        scale=ClickableSurfaceDefaults.scale(focusedScale=1.025f),border=tvFocusBorder(RoundedCornerShape(18.dp)),
        colors=tvFocusColors(Color(0xFF263C4B))) {
        Column {
            Canvas(Modifier.fillMaxWidth().height(142.dp).background(Brush.verticalGradient(if(scooter)listOf(Color(0xFF526D92),Color(0xFFABBDBF)) else listOf(Color(0xFF548E7B),Color(0xFF305B55))))) {
                val w=size.width;val h=size.height
                if(scooter) {
                    for(i in 0..5)drawRect(Color(0x55415E77),Offset(i*w/5,h*.24f+(i%3)*h*.09f),Size(w*.14f,h*.7f))
                    drawRect(Color(0xFF334B59),Offset(0f,h*.8f),Size(w,h*.2f))
                    drawCircle(Color(0xFF172838),h*.055f,Offset(w*.47f,h*.79f));drawCircle(Color(0xFF172838),h*.055f,Offset(w*.65f,h*.79f))
                    drawLine(Color(0xFFFFD58A),Offset(w*.44f,h*.76f),Offset(w*.65f,h*.76f),h*.025f)
                    drawLine(Color(0xFFCCE0E4),Offset(w*.65f,h*.76f),Offset(w*.62f,h*.48f),h*.025f)
                } else {
                    drawRect(Color(0xFF344C5A),Offset(0f,h*.42f),Size(w,h*.3f))
                    for(i in 0..4)drawLine(Color(0xFFB0B8A1),Offset(i*w/4,h*.57f),Offset(i*w/4+w*.11f,h*.57f),h*.018f)
                    drawRoundRect(Color(0xFFF1C47E),Offset(w*.12f,h*.43f),Size(w*.24f,h*.21f),androidx.compose.ui.geometry.CornerRadius(h*.04f))
                }
                drawIntoCanvas { canvas ->
                    val n=canvas.nativeCanvas;n.save()
                    n.translate(w*.53f,h*(if(scooter).58f else .36f));n.scale(h/145,h/145)
                    CrossingArt.draw(n,0f,0f,if(scooter)0 else 1,paint,rect);n.restore()
                }
            }
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text(if(scooter)"Kolobežka" else "Prejdi cestu",fontSize=23.sp,color=Color.White)
                Text(if(scooter)"Skoky, triky a rýchla jazda" else "Cesty, rieky a denná výzva",fontSize=13.sp,color=Color(0xFFBCD0DC))
                Text("20 postavičiek · výzvy · vlastný rekord",fontSize=11.sp,color=Color(0xFF8FD8C2))
            }
        }
    }
    LaunchedEffect(Unit){if(initial){withFrameNanos{};runCatching{focus.requestFocus()}}}
}
