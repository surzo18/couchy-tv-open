package com.conreo.couchytv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import com.conreo.couchytv.ui.setTvContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.conreo.couchytv.game.*
import com.conreo.couchytv.ui.LiteTvTheme

class CrossingActivity : LocalizedActivity() {
    private val game=CrossingGame()
    private lateinit var board:CrossingView
    private lateinit var sounds:GameSounds
    private var soundMode by mutableIntStateOf(1)
    private var daily by mutableStateOf(CrossingDaily.today())
    private var dailyBest by mutableIntStateOf(0)
    private var dailyAwardEpoch by mutableLongStateOf(Long.MIN_VALUE)
    private var runDaily:DailyRoute?=null
    private val prefs by lazy { getSharedPreferences("crossing_game",0) }
    private var paused by mutableStateOf(true)
    private var panel by mutableStateOf("")
    private var best by mutableIntStateOf(0)
    private var wallet by mutableIntStateOf(0)
    private var earned by mutableIntStateOf(0)
    private var runs by mutableIntStateOf(0)
    private var bestRunCoins by mutableIntStateOf(0)
    private var selected by mutableIntStateOf(0)
    private var unlocked by mutableStateOf(setOf(0))
    private var claimed by mutableStateOf(emptySet<String>())
    private var reward by mutableStateOf("")
    private var shopNotice by mutableStateOf("")
    private var credited=0
    private var countedRun=false
    private var resumed by mutableStateOf(false)
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        Experience.gameActive=true;Experience.homePanel=""
        best=prefs.getInt("best",0).coerceAtLeast(0)
        wallet=prefs.getInt("coins",0).coerceIn(0,1_000_000)
        unlocked=prefs.getStringSet("skins",setOf("0")).orEmpty().mapNotNull { it.toIntOrNull()?.takeIf { id -> id in CrossingCollection.skins.indices } }.toSet()+0
        selected=prefs.getInt("selected",0).takeIf { it in unlocked } ?: 0
        earned=prefs.getInt("earned",wallet+unlocked.sumOf { CrossingCollection.skin(it).price }).coerceAtLeast(0)
        runs=prefs.getInt("runs",0).coerceAtLeast(0)
        bestRunCoins=prefs.getInt("bestRunCoins",0).coerceAtLeast(0)
        claimed=prefs.getStringSet("challenges",emptySet()).orEmpty().toSet()
        sounds=GameSounds(this);soundMode=sounds.mode
        dailyBest=if(prefs.getString("dailyDate","")==daily.date)prefs.getInt("dailyBest",0) else 0
        dailyAwardEpoch=prefs.getLong("dailyAwardEpoch",Long.MIN_VALUE)
        board=CrossingView(this,game,{paused=true},{saveProgress()}).also{it.sounds=sounds}
        saveProgress()
        setTvContent { LiteTvTheme(Color(0xFF8FD8C2)) {
            BackHandler { if(panel.isNotEmpty())panel="" else paused=true }
            LaunchedEffect(paused,panel,resumed){board.running(resumed && !paused && panel.isEmpty())}
            AndroidView(factory={board},modifier=Modifier.fillMaxSize(),update={
                it.best=best;it.wallet=wallet;it.skin=selected;it.unlockedCount=unlocked.size
                val next=CrossingCollection.ordered.firstOrNull { skin -> skin.id !in unlocked }
                it.nextCost=next?.price ?: 0;it.nextName=next?.name.orEmpty();it.invalidate()
            })
            when(panel) {
                "collection" -> CrossingWardrobe(wallet,selected,unlocked,shopNotice,::chooseSkin,{panel=""})
                "challenges" -> CrossingChallengeMenu(best,bestRunCoins,runs,earned,claimed,{panel=""})
                "daily" -> CrossingDailyMenu(daily,dailyBest,dailyAwardEpoch>=daily.epoch,{startRun(daily)},{startRun(null)},{panel=""})
                else -> if(paused)CrossingPause(game,best,wallet,unlocked.size,reward,
                    soundLabel=when(soundMode){0->"Zvuky: vypnuté";1->"Zvuky: tiché";else->"Zvuky: bežná hlasitosť"},
                    onDaily={daily=CrossingDaily.today();dailyBest=if(prefs.getString("dailyDate","")==daily.date)prefs.getInt("dailyBest",0) else 0;panel="daily"},
                    onSound={soundMode=sounds.cycle()},
                    onPlay={if(game.dead)startRun(runDaily) else paused=false},
                    onCollection={shopNotice="";panel="collection"},onChallenges={panel="challenges"},
                    onNext={HomeMusic.next()},onExit=::exitGame)
            }
        } }
    }
    private fun startRun(route:DailyRoute?) {
        saveProgress(true);runDaily=route;game.variant=route?.variant ?: 0
        game.restart(route?.seed ?: System.nanoTime());credited=0;countedRun=false;reward="";board.resetCamera();panel="";paused=false
    }
    private fun chooseSkin(id:Int) {
        val skin=CrossingCollection.skin(id)
        if(id !in unlocked && wallet<skin.price){shopNotice="${skin.name}: ešte ${skin.price-wallet} mincí. ${skin.description}";return}
        if(id !in unlocked){wallet-=skin.price;unlocked=unlocked+id;shopNotice="Odomknuté: ${skin.name}. ${skin.description}";sounds.play("unlock")}
        else shopNotice=skin.description
        selected=id
        prefs.edit().putInt("coins",wallet).putInt("selected",selected).putStringSet("skins",unlocked.map{it.toString()}.toSet()).apply()
    }
    private fun saveProgress(finishRun:Boolean=false) {
        val coins=(game.coins-credited).coerceAtLeast(0)
        val record=maxOf(best,game.score)
        val coinRecord=maxOf(bestRunCoins,game.coins)
        val count=(game.dead || finishRun) && game.score>0 && !countedRun
        var changed=coins>0 || record!=best || coinRecord!=bestRunCoins || count
        wallet=(wallet+coins).coerceAtMost(1_000_000);earned=(earned+coins).coerceAtMost(10_000_000)
        credited=game.coins;best=record;bestRunCoins=coinRecord
        if(count){runs++;countedRun=true}
        val rewards=CrossingChallenges.all.filter { it.id !in claimed && CrossingChallenges.value(it,best,bestRunCoins,runs,earned)>=it.target }
        if(rewards.isNotEmpty()) {
            val bonus=rewards.sumOf{it.reward};wallet=(wallet+bonus).coerceAtMost(1_000_000)
            claimed=claimed+rewards.map{it.id};reward="Výzva splnená: ${rewards.joinToString { it.title }} · +$bonus mincí";changed=true;sounds.play("reward")
        }
        runDaily?.let { route ->
            val previous=if(prefs.getString("dailyDate","")==route.date)prefs.getInt("dailyBest",0) else 0
            if(game.score>previous){prefs.edit().putString("dailyDate",route.date).putInt("dailyBest",game.score).apply();if(daily.date==route.date)dailyBest=game.score}
            if(CrossingDaily.eligible(route,dailyAwardEpoch,game.score,game.coins)) {
                dailyAwardEpoch=route.epoch;wallet=(wallet+route.reward).coerceAtMost(1_000_000);changed=true
                reward="Denná výzva splnená · +${route.reward} mincí";sounds.play("reward")
            }
        }
        if(changed)prefs.edit().putInt("best",best).putInt("coins",wallet).putInt("earned",earned)
            .putInt("bestRunCoins",bestRunCoins).putInt("runs",runs).putLong("dailyAwardEpoch",dailyAwardEpoch).putStringSet("challenges",claimed).apply()
        board.best=best;board.wallet=wallet
    }
    private fun exitGame(){saveProgress(true);Experience.homePanel="";Experience.gameActive=false;finish()}
    // Android Activity public callback; required for TV remote long presses.
    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event:android.view.KeyEvent):Boolean {
        if(paused && event.action==android.view.KeyEvent.ACTION_DOWN && event.repeatCount==0 && event.keyCode in 19..23)sounds.play(if(event.keyCode==23)"confirm" else "ui",gain=.45f)
        return super.dispatchKeyEvent(event)
    }
    override fun onResume(){super.onResume();resumed=true;Experience.gameActive=true;sounds.foreground(true);HomeMusic.enterHome(this,WallpaperCycle.current(this))}
    override fun onPause(){resumed=false;paused=true;board.running(false);sounds.foreground(false);saveProgress();HomeMusic.leaveHome();super.onPause()}
    override fun onDestroy(){board.running(false);saveProgress(true);sounds.release();super.onDestroy()}
}
