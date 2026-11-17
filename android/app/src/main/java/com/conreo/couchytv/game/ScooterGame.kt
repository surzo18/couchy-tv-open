package com.conreo.couchytv.game

import java.util.Random
import java.util.ArrayDeque
import kotlin.math.*

/** Fixed substeps, swept floor landing and bounded procedural track; independent of Android. */
class ScooterGame @JvmOverloads constructor(var seed:Long=System.nanoTime()) {
    data class Platform(val id:Int,val start:Float,val end:Float,val y:Float,val obstacle:Int,val obstacleX:Float,val coins:FloatArray,var taken:Int=0)
    var x=160f;private set
    var y=760f;private set
    var vy=0f;private set
    var speed=260f;private set
    var time=0f;private set
    var coins=0;private set
    var tricks=0;private set
    var landings=0;private set
    var dead=false;private set
    var reason="";private set
    var paused=true
    var grounded=true;private set
    var brake=false
    var boost=false
    var lastTrick=-100f;private set
    val score get()=((x-160)/10).toInt().coerceAtLeast(0)
    val difficulty get()=(1+score/180+coins/12).coerceAtMost(10)
    val ducking get()=time<duckUntil && grounded
    val biome get()=(score/500)%4
    private var duckUntil=0f
    private var lastGround=0f
    private var lastJump=-1f
    private var jumpUntil=-1f
    private var jumps=0
    private var nextId=0
    private var end=0f
    private var levelY=760f
    private var airSince=0f
    val platforms=ArrayList<Platform>()
    private val events=ArrayDeque<String>()
    init{generate()}
    fun event():String?=events.pollFirst()
    private fun emit(value:String){if(events.size<32)events.addLast(value)}
    fun restart(newSeed:Long=System.nanoTime()) {
        seed=newSeed;x=160f;y=760f;vy=0f;speed=260f;time=0f;coins=0;tricks=0;landings=0;dead=false;reason="";paused=false;grounded=true;brake=false;boost=false
        lastTrick=-100f;duckUntil=0f;lastGround=0f;lastJump=-1f;jumpUntil=-1f;jumps=0;nextId=0;end=0f;levelY=760f;airSince=0f;platforms.clear();events.clear();generate()
    }
    fun duck(){if(!paused && !dead)duckUntil=time+.85f}
    fun jump():Boolean {
        if(paused || dead || time-lastJump<.16f)return false
        if(grounded || time-lastGround<.11f){takeOff();return true}
        if(jumps==1){vy=minOf(vy-210f,-470f);jumps=2;lastJump=time;lastTrick=time;tricks++;emit("jump");return true}
        jumpUntil=time+.14f;return false
    }
    private fun takeOff(){vy=-660f;grounded=false;jumps=1;lastJump=time;airSince=time;jumpUntil=-1f;duckUntil=0f;emit("jump")}
    fun update(delta:Float) {
        if(paused || dead)return
        val frame=delta.coerceIn(0f,.05f);val count=maxOf(1,ceil(frame*120).toInt());val dt=frame/count
        repeat(count){if(!dead)step(dt)}
    }
    private fun step(dt:Float) {
        time+=dt
        val target=(260f+(difficulty-1)*17f)*(if(brake).72f else if(boost)1.25f else 1f)
        speed+=(target-speed)*minOf(1f,dt*3.5f)
        val oldY=y;val wasGrounded=grounded
        x+=speed*dt;vy+=1320f*dt;y+=vy*dt;grounded=false
        generate()
        for(p in platforms) {
            if(x+12>=p.start && x-12<=p.end && vy>=0 && oldY<=p.y+8 && y>=p.y) {
                y=p.y;vy=0f;grounded=true;lastGround=time;jumps=0
                if(!wasGrounded && time-airSince>.12f){landings++;emit("land")}
                if(jumpUntil>=time)takeOff()
                break
            }
        }
        if(wasGrounded && !grounded && vy>=0){airSince=time;jumps=1}
        val bodyHeight=if(ducking)47f else 101f
        for(p in platforms) {
            if(p.obstacle>0 && abs(x-p.obstacleX)<if(p.obstacle==3)42f else 28f) {
                val top=when(p.obstacle){1->p.y-43;2->p.y-88;else->p.y-135}
                val bottom=if(p.obstacle==3)p.y-65 else p.y
                if(y>top+8 && y-bodyHeight<bottom-5){die(if(p.obstacle==3)"Pod zábranou sa prikrč šípkou dole." else "Prekážku preskoč šípkou hore.");return}
            }
            p.coins.forEachIndexed { i,cx ->
                val cy=p.y-(if(i==1)130 else 85)
                if(p.taken and (1 shl i)==0 && abs(x-cx)<30 && cy>y-bodyHeight-22 && cy<y+10){p.taken=p.taken or (1 shl i);coins++;emit("coin")}
            }
        }
        if(y>1170){die("Medzera bola príliš veľká. Pomôže druhý skok alebo správna rýchlosť.");return}
        platforms.removeAll{it.end<x-600}
    }
    private fun die(message:String){dead=true;paused=true;reason=message;emit(if(y>1100 && biome==1)"splash" else "hit")}
    private fun generate() {
        while(end<x+2200) {
            val rng=Random(seed xor (nextId.toLong()*104729))
            val pressure=(nextId/35f).coerceAtMost(1f)
            val gap=if(nextId==0)0f else 65f+rng.nextFloat()*(55f+pressure*65)
            val start=end+gap
            if(nextId>0)levelY=(levelY+(rng.nextInt(3)-1)*30).coerceIn(650f,810f)
            val width=if(nextId==0)1300f else 440f+rng.nextFloat()*330
            val obstacle=if(nextId<1)0 else if(rng.nextFloat()<.5f+pressure*.3f) 1+rng.nextInt(if(nextId>3)3 else 2) else 0
            val obstacleX=start+width*.57f
            val coinX=floatArrayOf(start+width*.2f,start+width*.32f,start+width*.43f)
            platforms.add(Platform(nextId,start,start+width,levelY,obstacle,obstacleX,coinX));end=start+width;nextId++
        }
    }
}
