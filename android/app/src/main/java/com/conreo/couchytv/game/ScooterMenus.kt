package com.conreo.couchytv.game

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ScooterGoals {
    val all=listOf(
        CrossingChallenge("m100","Prvé kolesá","best",100,5),CrossingChallenge("c5","Vreckové","coins",5,5),
        CrossingChallenge("m300","Mestský jazdec","best",300,10),CrossingChallenge("t10","Vo vzduchu","tricks",10,10),
        CrossingChallenge("l20","Mäkké pristátie","landings",20,12),CrossingChallenge("c15","Zberateľ","coins",15,12),
        CrossingChallenge("m750","Dlhá výprava","best",750,20),CrossingChallenge("t50","Akrobat","tricks",50,25),
        CrossingChallenge("c30","Plné vrecká","coins",30,25),CrossingChallenge("m1500","Za mestom","best",1500,35),
        CrossingChallenge("l100","Majster dopadov","landings",100,35),CrossingChallenge("e300","Garážový fond","earned",300,40),
        CrossingChallenge("m3000","Legenda kolies","best",3000,60),CrossingChallenge("e1000","Veľká zbierka","earned",1000,80))
    fun value(g:CrossingChallenge,best:Int,coins:Int,tricks:Int,landings:Int,earned:Int)=when(g.metric){"best"->best;"coins"->coins;"tricks"->tricks;"landings"->landings;else->earned}
    fun hint(g:CrossingChallenge)=when(g.metric){"best"->"Prejdi ${g.target} metrov v jednom pokuse";"coins"->"Nazbieraj ${g.target} mincí v jednom pokuse";"tricks"->"Urob spolu ${g.target} trikov vo vzduchu";"landings"->"Zvládni spolu ${g.target} pristátí";else->"Nazbieraj spolu ${g.target} mincí"}
}
@Composable
fun ScooterPause(game:ScooterGame,best:Int,wallet:Int,count:Int,reward:String,soundLabel:String,onPlay:()->Unit,onShop:()->Unit,onGoals:()->Unit,onSound:()->Unit,onHelp:()->Unit,onNext:()->Unit,onExit:()->Unit) {
    CrossingDialog(if(game.dead)"Ďalšia jazda čaká" else "Kolobežka",if(game.dead)game.reason else "Hore: skok · druhý skok: trik · OK / Späť: pauza",if(game.dead)onExit else onPlay) {
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            CrossingStat("TENTO POKUS","${game.score} m",Modifier.weight(1f))
            CrossingStat("OSOBNÝ REKORD","$best m",Modifier.weight(1f),Color(0xFFF5D489))
            CrossingStat("TVOJE MINCE",wallet.toString(),Modifier.weight(1f),Color(0xFF8FD8C2))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            CrossingAction(if(game.dead)"▶ Nová jazda" else "▶ Jazdiť","Náročnosť ${game.difficulty}/10",Modifier.weight(1f),initial=true,primary=true,onClick=onPlay)
            CrossingAction("Garáž","Odomknutí jazdci: $count/20",Modifier.weight(1f),onClick=onShop)
            CrossingAction("Výzvy","Ciele a bonusové mince",Modifier.weight(1f),onClick=onGoals)
        }
        Text(reward.ifEmpty{"Vľavo brzda · vpravo zrýchlenie · dole prikrčenie"},color=if(reward.isEmpty())Color(0xFFA9BCC7) else Color(0xFFF5D489),fontSize=13.sp,maxLines=2)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            CrossingAction(soundLabel,"Pri prehrávaní hudby sa vypnú",Modifier.weight(1f),onClick=onSound)
            CrossingAction("Ako jazdiť","Skoky, triky a prekážky",Modifier.weight(1f),onClick=onHelp)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            CrossingAction("♫ Ďalšia skladba",modifier=Modifier.weight(1f),onClick=onNext)
            CrossingAction("Ukončiť hru",modifier=Modifier.weight(1f),onClick=onExit)
        }
    }
}
@Composable
fun ScooterGoalMenu(best:Int,coins:Int,tricks:Int,landings:Int,earned:Int,claimed:Set<String>,onBack:()->Unit) {
    var page by remember{mutableIntStateOf(0)}
    CrossingDialog("Výzvy na kolesách","${claimed.size}/14 splnených · odmeny sa pripíšu automaticky",onBack) {
        ScooterGoals.all.drop(page*6).take(6).chunked(2).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            row.forEach { g -> val v=ScooterGoals.value(g,best,coins,tricks,landings,earned).coerceAtMost(g.target)
                Column(Modifier.weight(1f).background(Color(0xFF233847),RoundedCornerShape(12.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text((if(g.id in claimed)"✓ " else "")+g.title+"  +${g.reward}",color=if(g.id in claimed)Color(0xFF8FD8C2) else Color.White,fontSize=14.sp)
                    Text(ScooterGoals.hint(g),color=Color(0xFFA9BCC7),fontSize=11.sp)
                    Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF152936))){Box(Modifier.fillMaxWidth(v.toFloat()/g.target).height(3.dp).background(Color(0xFFF5D489)))}
                    Text("$v / ${g.target}",color=Color(0xFFA9BCC7),fontSize=10.sp)
                }
            };if(row.size==1)Spacer(Modifier.weight(1f))
        }}
        key(page){Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            if(page>0)CrossingAction("‹ Predošlé"){page--}
            if(page<2)CrossingAction("Ďalšie ›",initial=true){page++}
            Spacer(Modifier.weight(1f));CrossingAction("Späť",initial=page==2,onClick=onBack)
        }}
    }
}
