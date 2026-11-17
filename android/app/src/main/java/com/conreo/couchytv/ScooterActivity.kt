package com.conreo.couchytv

import com.conreo.couchytv.ui.LocalizedText as Text
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import com.conreo.couchytv.ui.setTvContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.conreo.couchytv.game.*
import com.conreo.couchytv.ui.LiteTvTheme

class ScooterActivity:LocalizedActivity() {
    private val game=ScooterGame()
    private lateinit var board:ScooterView
    private lateinit var sounds:GameSounds
    private val prefs by lazy{getSharedPreferences("scooter_game",0)}
    private var paused by mutableStateOf(true)
    private var panel by mutableStateOf("")
    private var resumed by mutableStateOf(false)
    private var best by mutableIntStateOf(0)
    private var wallet by mutableIntStateOf(0)
    private var earned by mutableIntStateOf(0)
    private var coinBest by mutableIntStateOf(0)
    private var tricks by mutableIntStateOf(0)
    private var landings by mutableIntStateOf(0)
    private var selected by mutableIntStateOf(0)
    private var unlocked by mutableStateOf(setOf(0))
    private var claimed by mutableStateOf(emptySet<String>())
    private var reward by mutableStateOf("")
    private var notice by mutableStateOf("")
    private var soundMode by mutableIntStateOf(1)
    private var credited=0;private var savedTricks=0;private var savedLandings=0
    override fun onCreate(state:Bundle?){super.onCreate(state);Experience.gameActive=true;Experience.homePanel=""
        best=prefs.getInt("best",0).coerceAtLeast(0);wallet=prefs.getInt("coins",0).coerceAtLeast(0);earned=prefs.getInt("earned",0).coerceAtLeast(0)
        coinBest=prefs.getInt("coinBest",0);tricks=prefs.getInt("tricks",0);landings=prefs.getInt("landings",0)
        unlocked=prefs.getStringSet("skins",setOf("0")).orEmpty().mapNotNull{it.toIntOrNull()?.takeIf{id->id in CrossingCollection.skins.indices}}.toSet()+0
        selected=prefs.getInt("selected",0).takeIf{it in unlocked} ?: 0;claimed=prefs.getStringSet("goals",emptySet()).orEmpty().toSet()
        sounds=GameSounds(this);soundMode=sounds.mode;board=ScooterView(this,game,{paused=true},::save).also{it.sounds=sounds}
        setTvContent {LiteTvTheme(Color(0xFF8FD8C2)){
            BackHandler{if(panel.isNotEmpty())panel="" else paused=true}
            LaunchedEffect(paused,panel,resumed){board.running(resumed && !paused && panel.isEmpty())}
            AndroidView(factory={board},modifier=Modifier.fillMaxSize(),update={it.best=best;it.wallet=wallet;it.skin=selected;it.invalidate()})
            when(panel){
                "shop"->CrossingWardrobe(wallet,selected,unlocked,notice,::choose,{panel=""})
                "goals"->ScooterGoalMenu(best,coinBest,tricks,landings,earned,claimed,{panel=""})
                "help"->CrossingDialog("Ako jazdiť","Automatická jazda · všetko ovládaš šípkami",{panel=""}){
                    Text("Hore: skok. Druhé stlačenie vo vzduchu pridá trik a ďalší skok.\nDole: krátke prikrčenie pod vysokou zábranou.\nVľavo podrž: brzda. Vpravo podrž: zrýchlenie.\nOK alebo Späť: pauza.",color=Color.White,fontSize=18.sp)
                    Text("Mince a výzvy odomykajú jazdcov. Všetci majú rovnaké schopnosti. S rastúcou vzdialenosťou pribúdajú väčšie medzery, vyššia rýchlosť a rozmanitejšie prekážky.",color=Color(0xFFB4C9D5),fontSize=14.sp)
                    CrossingAction("Späť",initial=true){panel=""}
                }
                else->if(paused)ScooterPause(game,best,wallet,unlocked.size,reward,
                    when(soundMode){0->"Zvuky: vypnuté";1->"Zvuky: tiché";else->"Zvuky: bežná hlasitosť"},
                    onPlay={if(game.dead){game.restart();credited=0;savedTricks=0;savedLandings=0;reward=""};paused=false},
                    onShop={notice="";panel="shop"},onGoals={panel="goals"},onSound={soundMode=sounds.cycle()},onHelp={panel="help"},onNext={HomeMusic.next()},onExit=::exit)
            }
        }}
    }
    private fun choose(id:Int){val skin=CrossingCollection.skin(id)
        if(id !in unlocked && wallet<skin.price){notice="${skin.name}: chýba ${skin.price-wallet} mincí.";return}
        if(id !in unlocked){wallet-=skin.price;unlocked=unlocked+id;sounds.play("unlock")}
        selected=id;notice=skin.description
        prefs.edit().putInt("coins",wallet).putInt("selected",selected).putStringSet("skins",unlocked.map{it.toString()}.toSet()).apply()
    }
    private fun save(){
        val delta=(game.coins-credited).coerceAtLeast(0);val newBest=maxOf(best,game.score);val newCoins=maxOf(coinBest,game.coins)
        var changed=delta>0 || newBest!=best || newCoins!=coinBest || game.tricks!=savedTricks || game.landings!=savedLandings
        wallet=(wallet+delta).coerceAtMost(1_000_000);earned=(earned+delta).coerceAtMost(10_000_000);credited=game.coins;best=newBest;coinBest=newCoins
        tricks+=(game.tricks-savedTricks).coerceAtLeast(0);landings+=(game.landings-savedLandings).coerceAtLeast(0);savedTricks=game.tricks;savedLandings=game.landings
        val awards=ScooterGoals.all.filter{it.id !in claimed && ScooterGoals.value(it,best,coinBest,tricks,landings,earned)>=it.target}
        if(awards.isNotEmpty()){val bonus=awards.sumOf{it.reward};wallet=(wallet+bonus).coerceAtMost(1_000_000);claimed=claimed+awards.map{it.id};reward="${awards.joinToString{it.title}} · +$bonus mincí";sounds.play("reward");changed=true}
        if(changed)prefs.edit().putInt("best",best).putInt("coins",wallet).putInt("earned",earned).putInt("coinBest",coinBest).putInt("tricks",tricks).putInt("landings",landings).putStringSet("goals",claimed).apply()
        board.best=best;board.wallet=wallet
    }
    private fun exit(){save();Experience.gameActive=false;finish()}
    override fun onResume(){super.onResume();resumed=true;Experience.gameActive=true;sounds.foreground(true);HomeMusic.enterHome(this,WallpaperCycle.current(this))}
    override fun onPause(){resumed=false;paused=true;board.running(false);sounds.foreground(false);save();HomeMusic.leaveHome();super.onPause()}
    override fun onDestroy(){board.running(false);save();sounds.release();super.onDestroy()}
}
