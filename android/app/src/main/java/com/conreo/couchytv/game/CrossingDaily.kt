package com.conreo.couchytv.game

import java.time.LocalDate
import java.time.ZoneId

data class DailyRoute(val date:String,val epoch:Long,val seed:Long,val variant:Int,val title:String,val description:String,val rows:Int,val coins:Int,val reward:Int=25)
object CrossingDaily {
    fun today()=forDate(LocalDate.now(ZoneId.of("Europe/Bratislava")))
    fun forDate(day:LocalDate):DailyRoute {
        val epoch=day.toEpochDay();val variant=Math.floorMod(epoch,4).toInt()+1
        val name=when(variant){1->"Mestská špička";2->"Riečna expedícia";3->"Zlatá trasa";else->"Nočná jazda"}
        val description=when(variant){1->"Viac dlhých vozidiel a menej času na bezpečný prechod.";2->"Viac riečnych úsekov a plávajúce plošiny.";3->"Viac trojitých mincí, náročnosť rastie rýchlejšie.";else->"Súmrak a hustejšia premávka."}
        return DailyRoute(day.toString(),epoch,epoch*6364136223846793005L+1442695040888963407L,variant,name,description,if(variant==2)35 else 45,if(variant==3)12 else 6)
    }
    fun eligible(route:DailyRoute,lastRewardEpoch:Long,rows:Int,coins:Int)=route.epoch>lastRewardEpoch && rows>=route.rows && coins>=route.coins
}
