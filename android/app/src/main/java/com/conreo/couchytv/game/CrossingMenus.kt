package com.conreo.couchytv.game

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conreo.couchytv.ui.TvDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.*
import com.conreo.couchytv.ui.tvFocusBorder
import com.conreo.couchytv.ui.tvFocusColors

private val Mint=Color(0xFF8FD8C2)
private val Muted=Color(0xFFA9BCC7)
private val Gold=Color(0xFFF5D489)

@Composable
fun CrossingDialog(title:String,subtitle:String,onBack:()->Unit,content:@Composable ColumnScope.()->Unit) {
    var released by remember { mutableStateOf(false) }
    Dialog(onDismissRequest=onBack,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(Modifier.width(760.dp).heightIn(max=510.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF1D3441),Color(0xFF121F2D))),RoundedCornerShape(24.dp))
                .border(1.dp,Color(0xFF405965),RoundedCornerShape(24.dp)).padding(24.dp)
                .onPreviewKeyEvent { e ->
                    if(e.type==KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount==0)GameSounds.menu(e.nativeKeyEvent.keyCode)
                    val select=e.key==Key.DirectionCenter || e.key==Key.Enter
                    if(!select || released)false else if(e.type==KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount==0){released=true;false}
                    else {if(e.type==KeyEventType.KeyUp)released=true;true}
                },verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp,38.dp).background(Mint,RoundedCornerShape(4.dp)))
                    Column(Modifier.padding(start=14.dp)) {
                        Text(title,color=Color.White,fontSize=27.sp)
                        Text(subtitle,color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=3.dp))
                    }
                }
                content()
            }
        }
    }
}

@Composable
fun CrossingAction(label:String,subtitle:String="",modifier:Modifier=Modifier,initial:Boolean=false,primary:Boolean=false,onClick:()->Unit) {
    val focus=remember { FocusRequester() }
    Surface(onClick=onClick,modifier=modifier.focusRequester(focus),
        shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        scale=ClickableSurfaceDefaults.scale(focusedScale=1.025f),border=tvFocusBorder(RoundedCornerShape(14.dp)),
        colors=tvFocusColors(if(primary)Color(0xFF315449) else Color(0xFF273C4B))) {
        Column(Modifier.padding(horizontal=18.dp,vertical=13.dp)) {
            Text(label,color=Color.White,fontSize=16.sp)
            if(subtitle.isNotEmpty())Text(subtitle,color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=5.dp))
        }
    }
    LaunchedEffect(initial){if(initial){withFrameNanos{};runCatching{focus.requestFocus()}}}
}

@Composable
fun CrossingStat(label:String,value:String,modifier:Modifier=Modifier,tint:Color=Color.White) {
    Column(modifier.background(Color(0xFF132633),RoundedCornerShape(14.dp)).padding(16.dp)) {
        Text(label,color=Muted,fontSize=11.sp)
        Text(value,color=tint,fontSize=25.sp,modifier=Modifier.padding(top=6.dp))
    }
}

@Composable
fun CrossingPause(game:CrossingGame,best:Int,wallet:Int,unlocked:Int,reward:String,soundLabel:String,onDaily:()->Unit,onSound:()->Unit,onPlay:()->Unit,onCollection:()->Unit,onChallenges:()->Unit,onNext:()->Unit,onExit:()->Unit) {
    CrossingDialog(if(game.dead)"Ešte jedna cesta?" else "Prejdi cestu",
        if(game.dead)game.reason else "Šípky: pohyb   ·   OK / Späť: pauza",if(game.dead)onExit else onPlay) {
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            CrossingStat("TENTO POKUS",game.score.toString(),Modifier.weight(1f))
            CrossingStat("OSOBNÝ REKORD",best.toString(),Modifier.weight(1f),Gold)
            CrossingStat("TVOJE MINCE",wallet.toString(),Modifier.weight(1f),Mint)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            CrossingAction(if(game.dead)"▶  Nový pokus" else if(game.score==0)"▶  Vyraziť" else "▶  Pokračovať",
                if(game.dead)"Mince a rekord ti zostávajú" else "${game.biome} · stupeň ${game.difficulty}/10",
                Modifier.weight(1.25f),initial=true,primary=true,onClick=onPlay)
            CrossingAction("Postavičky","$unlocked z 20 odomknutých",Modifier.weight(1f),onClick=onCollection)
            CrossingAction("Výzvy","Ciele a bonusové mince",Modifier.weight(1f),onClick=onChallenges)
        }
        if(reward.isNotEmpty())Text(reward,color=Gold,fontSize=13.sp,maxLines=2)
        else Text("S prejdenou vzdialenosťou a získanými mincami rastie náročnosť. Postavičky majú rovnaké schopnosti.",color=Muted,fontSize=12.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            CrossingAction(if(game.variant>0)"Denná výzva · hráš" else "Denná výzva","Rovnaká trasa na celý deň",Modifier.weight(1f),onClick=onDaily)
            CrossingAction(soundLabel,"Pri prehrávaní hudby sa vypnú",Modifier.weight(1f),onClick=onSound)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            CrossingAction("♫  Ďalšia skladba",modifier=Modifier.weight(1f),onClick=onNext)
            CrossingAction("Ukončiť hru",modifier=Modifier.weight(1f),onClick=onExit)
        }
    }
}

@Composable
fun CrossingDailyMenu(route:DailyRoute,best:Int,claimed:Boolean,onStart:()->Unit,onFree:()->Unit,onBack:()->Unit) {
    CrossingDialog("Denná výzva · ${route.title}","${route.date} · nová trasa každý deň podľa času v Bratislave",onBack) {
        Text(route.description,color=Color.White,fontSize=18.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            CrossingStat("DNEŠNÝ REKORD",best.toString(),Modifier.weight(1f),Gold)
            CrossingStat("CIEĽ V JEDNOM POKUSE","${route.rows} krokov a ${route.coins} mincí",Modifier.weight(2f))
        }
        Text(if(claimed)"✓ Dnešná odmena je získaná. Rekord môžeš zlepšovať ďalej." else "Splň oba ciele v jednom pokuse a získaj ${route.reward} bonusových mincí.",color=if(claimed)Mint else Gold,fontSize=15.sp)
        Text("Trasa a počiatočné rozmiestnenie prekážok sú počas dňa rovnaké. Odmenu získaš raz denne.",color=Muted,fontSize=13.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            CrossingAction("▶ Začať denný pokus",initial=true,primary=true,onClick=onStart)
            CrossingAction("Nová voľná jazda",onClick=onFree)
            CrossingAction("Späť",onClick=onBack)
        }
    }
}

@Composable
fun CrossingWardrobe(wallet:Int,selected:Int,unlocked:Set<Int>,notice:String,onChoose:(Int)->Unit,onBack:()->Unit) {
    var page by remember { mutableIntStateOf(CrossingCollection.ordered.indexOfFirst { it.id==selected }.coerceAtLeast(0)/8) }
    val pages=(CrossingCollection.ordered.size+7)/8
    CrossingDialog("Tvoja zbierka","${coinCount(wallet)} · odomknuté postavičky: ${unlocked.size}/20",onBack) {
        val items=CrossingCollection.ordered.drop(page*8).take(8)
        key(page) {
            items.chunked(4).forEachIndexed { row,skins ->
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    skins.forEachIndexed { col,skin ->
                        val owned=skin.id in unlocked;val canBuy=wallet>=skin.price
                        val portrait=remember(skin.id){CrossingArt.portrait(skin.id).asImageBitmap()}
                        val focus=remember{FocusRequester()}
                        Surface(onClick={onChoose(skin.id)},modifier=Modifier.weight(1f).height(116.dp).focusRequester(focus),
                            shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                            border=tvFocusBorder(RoundedCornerShape(14.dp)),scale=ClickableSurfaceDefaults.scale(focusedScale=1.025f),
                            colors=tvFocusColors(if(selected==skin.id)Color(0xFF315449) else Color(0xFF233847))) {
                            Column(Modifier.fillMaxSize().padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                                Image(portrait,skin.name,Modifier.size(57.dp))
                                Text(skin.name,color=Color.White,fontSize=13.sp)
                                Text(if(selected==skin.id)"✓ Vybrané" else if(owned)"Odomknuté" else "${skin.price} mincí",color=if(owned)Mint else if(canBuy)Gold else Muted,fontSize=11.sp,modifier=Modifier.padding(top=3.dp))
                            }
                        }
                        LaunchedEffect(Unit){if(row==0 && col==0){withFrameNanos{};runCatching{focus.requestFocus()}}}
                    }
                    repeat(4-skins.size){Spacer(Modifier.weight(1f))}
                }
            }
        }
        Text(notice.ifEmpty { "OK: vybrať alebo odomknúť postavičku. Mince získavaš hraním a plnením výziev." },color=Muted,fontSize=12.sp,maxLines=2)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
            if(page>0)CrossingAction("‹ Predošlé"){page--}
            if(page+1<pages)CrossingAction("Ďalšie ›"){page++}
            Spacer(Modifier.weight(1f));Text("${page+1} / $pages",color=Muted,fontSize=13.sp)
            CrossingAction("Späť",onClick=onBack)
        }
    }
}

@Composable
fun CrossingChallengeMenu(best:Int,runCoins:Int,runs:Int,earned:Int,claimed:Set<String>,onBack:()->Unit) {
    var page by remember{mutableIntStateOf(0)}
    val pages=(CrossingChallenges.all.size+5)/6
    CrossingDialog("Cesta za odmenami","${claimed.size}/${CrossingChallenges.all.size} splnených   ·   odmeny sa pripíšu automaticky",onBack) {
        CrossingChallenges.all.drop(page*6).take(6).chunked(2).forEach { row ->
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                row.forEach { challenge ->
                    val done=challenge.id in claimed
                    val value=CrossingChallenges.value(challenge,best,runCoins,runs,earned).coerceAtMost(challenge.target)
                    Column(Modifier.weight(1f).background(Color(0xFF233847),RoundedCornerShape(12.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Row { Text((if(done)"✓ " else "")+challenge.title,color=if(done)Mint else Color.White,fontSize=14.sp);Spacer(Modifier.weight(1f));Text("+${challenge.reward}",color=Gold,fontSize=14.sp) }
                        Text(CrossingChallenges.hint(challenge),color=Muted,fontSize=11.sp)
                        Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF152936))) {
                            Box(Modifier.fillMaxWidth(value.toFloat()/challenge.target).height(3.dp).background(if(done)Mint else Gold))
                        }
                        Text("$value / ${challenge.target}",color=Muted,fontSize=10.sp)
                    }
                }
                if(row.size==1)Spacer(Modifier.weight(1f))
            }
        }
        key(page) { Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
            if(page>0)CrossingAction("‹ Predošlé"){page--}
            if(page+1<pages)CrossingAction("Ďalšie ›",initial=true){page++}
            Spacer(Modifier.weight(1f));Text("${page+1} / $pages",color=Muted,fontSize=13.sp)
            CrossingAction("Späť",initial=page+1==pages,onClick=onBack)
        } }
    }
}
