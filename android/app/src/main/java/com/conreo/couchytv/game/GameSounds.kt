package com.conreo.couchytv.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.SystemClock
import com.conreo.couchytv.HomeMusic
import com.conreo.couchytv.RuntimeHealth

/** One bounded sound bank per foreground game. No service, generator or decoding in the frame loop. */
class GameSounds(context:Context) {
    companion object {
        private var foregroundBank:GameSounds?=null
        fun menu(key:Int){if(key in 19..23)foregroundBank?.play(if(key==23)"confirm" else "ui",gain=.45f)}
    }
    private val prefs=context.getSharedPreferences("game_audio",0)
    var mode=prefs.getInt("mode",1).coerceIn(0,2);private set
    val label get()=when(mode){0->"Zvuky: vypnuté";1->"Zvuky: tiché";else->"Zvuky: bežná hlasitosť"}
    private val audio=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val pool=SoundPool.Builder().setMaxStreams(6).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val sounds=mutableMapOf<String,Int>()
    private val loaded=java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private val streams=LinkedHashMap<Int,Long>()
    private val last=mutableMapOf<String,Long>()
    private var visible=false
    private var focused=false
    private var closed=false
    private var retryAfter=0L
    private var water=0
    private var warning=0
    private var rolling=0
    private var lastEnvironment=0L
    private val focus=AudioManager.OnAudioFocusChangeListener { change ->
        if(change<0){focused=false;retryAfter=SystemClock.uptimeMillis()+1500;stopAll()}
        else if(change==AudioManager.AUDIOFOCUS_GAIN)focused=true
    }
    init {
        pool.setOnLoadCompleteListener{_,id,status->if(status==0)loaded.add(id) else RuntimeHealth.mediaFailure("Herný zvuk sa nepodarilo načítať")}
        listOf("step","wood","jump","land","coin","gold","hit","splash","rail_hit","unlock","reward","ui","confirm","brake","whoosh","warning","water","roll").forEach { name ->
            runCatching{context.assets.openFd("game-audio/$name.wav").use{sounds[name]=pool.load(it,1)}}
                .onFailure{RuntimeHealth.mediaFailure("Herný zvuk: $name")}
        }
    }
    fun cycle():Int {mode=(mode+1)%3;prefs.edit().putInt("mode",mode).apply();silence();return mode}
    fun foreground(value:Boolean){visible=value;if(value)foregroundBank=this else if(foregroundBank===this)foregroundBank=null;if(!value)silence() else retryAfter=maxOf(retryAfter,SystemClock.uptimeMillis()+900)}
    fun pause(){stopLoops()}
    @Suppress("DEPRECATION")
    private fun allowed():Boolean {
        if(closed || !visible || mode==0 || HomeMusic.holdsAudio()){silence();return false}
        if(!focused){
            if(SystemClock.uptimeMillis()<retryAfter || audio.isMusicActive)return false
            focused=audio.requestAudioFocus(focus,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        return focused
    }
    fun play(name:String,pan:Float=0f,gain:Float=1f,loop:Boolean=false):Int {
        if(!allowed())return 0
        val now=SystemClock.uptimeMillis();val id=sounds[name] ?: return 0
        if(id !in loaded)return 0
        if(!loop && now-(last[name] ?: 0)<if(name=="ui")90 else 65)return 0
        last[name]=now;streams.entries.removeAll{it.value<now}
        while(streams.size>=6){val oldest=streams.keys.first();pool.stop(oldest);streams.remove(oldest);if(water==oldest)water=0;if(warning==oldest)warning=0;if(rolling==oldest)rolling=0}
        val level=(if(mode==1).3f else .5f)*gain.coerceIn(0f,1f)
        val p=pan.coerceIn(-1f,1f)
        val rate=if(name in listOf("step","wood","ui"))(.95f+(now%11)/100f) else 1f
        val stream=pool.play(id,level*(1f-p.coerceAtLeast(0f)*.55f),level*(1f+ p.coerceAtMost(0f)*.55f),if(loop)0 else 1,if(loop)-1 else 0,rate)
        if(stream!=0)streams[stream]=if(loop)Long.MAX_VALUE else now+1600
        return stream
    }
    fun crossingEnvironment(game:CrossingGame) {
        val now=SystemClock.uptimeMillis();if(now-lastEnvironment<150)return;lastEnvironment=now
        if(game.paused || game.dead){stopLoops();return}
        if(!allowed())return
        var river=false;var rail=false
        for(row in game.row..game.row+2){val lane=game.lane(row);river=river || lane.kind==2;rail=rail || (lane.kind==3 && lane.warning(game.time))}
        if(river && water==0)water=play("water",gain=.3f,loop=true)
        if(!river && water!=0){pool.stop(water);streams.remove(water);water=0}
        if(rail && warning==0)warning=play("warning",gain=.65f,loop=true)
        if(!rail && warning!=0){pool.stop(warning);streams.remove(warning);warning=0}
        val front=game.lane(game.row+1)
        if(front.kind==1 && front.contains(game.x+.5f,game.time,.7f) && now-(last["whoosh"] ?: 0)>1800)play("whoosh",front.direction*.65f,.25f)
    }
    fun scooterEnvironment(game:ScooterGame) {
        val now=SystemClock.uptimeMillis();if(now-lastEnvironment<150)return;lastEnvironment=now
        if(game.paused || game.dead){stopLoops();return}
        if(!allowed())return
        if(game.grounded && rolling==0)rolling=play("roll",gain=.25f,loop=true)
        if(!game.grounded && rolling!=0){pool.stop(rolling);streams.remove(rolling);rolling=0}
        if(rolling!=0)pool.setRate(rolling,(game.speed/300f).coerceIn(.6f,1.7f))
        if(game.brake && game.grounded && now-(last["brake"] ?: 0)>1400)play("brake",gain=.22f)
    }
    private fun stopLoops(){if(water!=0){pool.stop(water);streams.remove(water);water=0};if(warning!=0){pool.stop(warning);streams.remove(warning);warning=0};if(rolling!=0){pool.stop(rolling);streams.remove(rolling);rolling=0}}
    private fun stopAll(){streams.keys.toList().forEach{pool.stop(it)};streams.clear();water=0;warning=0;rolling=0}
    @Suppress("DEPRECATION")
    private fun silence(){if(closed)return;stopAll();if(focused){audio.abandonAudioFocus(focus);focused=false}}
    fun release(){if(closed)return;if(foregroundBank===this)foregroundBank=null;silence();closed=true;pool.release();loaded.clear();sounds.clear()}
}
