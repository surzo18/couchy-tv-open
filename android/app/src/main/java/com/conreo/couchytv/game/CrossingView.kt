package com.conreo.couchytv.game

import android.content.Context
import android.graphics.*
import android.view.Choreographer
import android.view.KeyEvent
import android.view.View
import kotlin.math.*

/** Native vector scene: no network, textures, video decoder or per-frame Compose state. */
class CrossingView(context: Context, val game: CrossingGame, val pause: () -> Unit, val progress: () -> Unit) : View(context), Choreographer.FrameCallback {
    var best = 0
    var wallet = 0
    var skin = 0
    var nextCost = 12
    var nextName = "Žabka"
    var unlockedCount = 1
    var sounds:GameSounds?=null
    private var active = false
    private var scheduled = false
    private var lastFrame = 0L
    private var camera = -1.5f
    private var drawnX = 4f
    private var drawnRow = 0f
    private val deathMenu = Runnable { pause() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val normal = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val bold = Typeface.create("sans-serif", Typeface.BOLD)
    private val carColors = intArrayOf(0xFFFF916C.toInt(), 0xFF9F8CF4.toInt(), 0xFF5DD7DC.toInt(), 0xFFF4D273.toInt())
    private val grassColors = intArrayOf(0xFF527B55.toInt(),0xFF91AFB0.toInt(),0xFFB69A68.toInt(),0xFF536577.toInt())
    private val grassLight = intArrayOf(0xFF638B60.toInt(),0xFFADC6C4.toInt(),0xFFC4A879.toInt(),0xFF607186.toInt())
    init { isFocusableInTouchMode = true; isFocusable = true; contentDescription = "Prejdi cestu. Šípky pohyb, OK alebo Späť pauza." }
    fun running(value: Boolean) {
        removeCallbacks(deathMenu)
        active = value; game.paused = !value || game.dead; lastFrame = 0
        if (!value) { Choreographer.getInstance().removeFrameCallback(this); scheduled = false;sounds?.pause() }
        else { requestFocus(); schedule() }
        invalidate()
    }
    fun resetCamera() { camera = -1.5f; drawnX = game.x; drawnRow = game.row.toFloat(); invalidate() }
    private fun schedule() { if (active && !scheduled) { scheduled = true; Choreographer.getInstance().postFrameCallback(this) } }
    override fun doFrame(now: Long) {
        scheduled = false
        if (!active) return
        val dt = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1_000_000_000f).coerceAtMost(.05f)
        lastFrame = now
        game.update(dt)
        sounds?.crossingEnvironment(game)
        camera += (maxOf(-1.5f, game.score - 3.5f) - camera) * minOf(1f, dt * 7f)
        drawnX += (game.x - drawnX) * minOf(1f, dt * 20f)
        drawnRow += (game.row - drawnRow) * minOf(1f, dt * 20f)
        if (game.dead) endRun()
        invalidate(); schedule()
    }
    override fun onDetachedFromWindow() { running(false); super.onDetachedFromWindow() }
    override fun onKeyDown(code: Int, event: KeyEvent): Boolean {
        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_BACK) {
            if (event.repeatCount == 0) { running(false); progress(); pause() }; return true
        }
        val direction = when(code) {
            KeyEvent.KEYCODE_DPAD_UP -> 0 to 1; KeyEvent.KEYCODE_DPAD_DOWN -> 0 to -1
            KeyEvent.KEYCODE_DPAD_LEFT -> -1 to 0; KeyEvent.KEYCODE_DPAD_RIGHT -> 1 to 0
            else -> return super.onKeyDown(code,event)
        }
        val previousCoins=game.coins
        if (game.move(direction.first,direction.second)) {
            if(!game.dead)sounds?.play(if(game.lane(game.row).kind==2)"wood" else "step",(game.x-4)/4)
            if(game.coins>previousCoins)sounds?.play(if(game.coins-previousCoins>1)"gold" else "coin",(game.x-4)/4)
            progress();if(game.dead)endRun();invalidate()
        }
        return true
    }
    private fun showCollision() { drawnX=game.x; drawnRow=game.row.toFloat() }
    private fun endRun() {
        showCollision(); running(false); progress()
        sounds?.play(when(game.lane(game.row).kind){2->"splash";3->"rail_hit";else->"hit"})
        // Let the player see the actual impact before covering the board with a menu.
        postDelayed(deathMenu,450)
    }
    private fun box(c: Canvas,x: Float,y: Float,w: Float,h: Float,color: Int,r: Float=0f) {
        paint.color=color; rect.set(x,y,x+w,y+h)
        if(r>0) c.drawRoundRect(rect,r,r,paint) else c.drawRect(rect,paint)
    }
    private fun text(c: Canvas,s: String,x: Float,y: Float,size: Float,color: Int=0xFFF4F7F2.toInt(),heavy: Boolean=false) {
        paint.color=color; paint.textSize=size;paint.typeface=if(heavy)bold else normal;c.drawText(s,x,y,paint)
    }
    override fun onDraw(real: Canvas) {
        super.onDraw(real)
        val c=real; c.save();c.scale(width/1920f,height/1080f)
        c.drawColor(0xFF101E28.toInt())
        val tile=82f;val left=(1920-9*tile)/2; val bottom=835f
        fun y(row: Float)=bottom-(row-camera)*tile
        box(c,left-16,0f,9*tile+32,1080f,0xFF0A1821.toInt())
        for (r in floor(camera-4).toInt()..ceil(camera+12).toInt()) {
            val yy=y(r.toFloat());if(yy < -tile || yy > 1080)continue
            val lane=game.lane(r)
            val biome=if(game.variant==4)3 else Math.floorMod(r.coerceAtLeast(0)/40,4)
            when(lane.kind) {
                0 -> {
                    box(c,left,yy,9*tile,tile,if(r%2==0)grassColors[biome] else grassLight[biome])
                    box(c,left,yy,9*tile,5f,grassLight[biome])
                    for(i in 0..8) {
                        val gx=left+i*tile+Math.floorMod(r*31+i*17,50)
                        box(c,gx,yy+24f,4f,8f,0x2260A27A,2f)
                        if((r+i)%4==0)box(c,gx+17,yy+58,5f,5f,if(biome==1)0xFFD1E2DD.toInt() else 0xFFCEBC8D.toInt(),2f)
                    }
                    for (t in lane.trees) {
                        val tx=left+(t+.5f)*tile
                        box(c,tx-23,yy+48,54f,20f,0x6630442F,7f)
                        box(c,tx-7,yy+30,14f,34f,0xFF71533C.toInt())
                        box(c,tx-29,yy+4,58f,44f,if(biome==1)0xFF678B86.toInt() else 0xFF2D5945.toInt(),8f)
                        box(c,tx-29,yy+4,58f,25f,if(biome==1)0xFFD8E7DF.toInt() else if(biome==2)0xFF829260.toInt() else 0xFF41775A.toInt(),8f)
                    }
                }
                1 -> {
                    box(c,left,yy,9*tile,tile,0xFF343D4C.toInt())
                    for(i in 0..8)box(c,left+i*tile+22,yy+tile-5,35f,3f,0xFF9A9D97.toInt())
                }
                2 -> {
                    box(c,left,yy,9*tile,tile,0xFF286B86.toInt())
                    for(i in 0..8)box(c,left+i*tile+((game.time*12)%50).toFloat(),yy+17,30f,3f,0xFF418EA2.toInt())
                }
                3 -> {
                    box(c,left,yy,9*tile,tile,0xFF6B625A.toInt())
                    for(i in 0..21)box(c,left+i*35,yy+10,12f,62f,0xFF4B4545.toInt(),2f)
                    box(c,left,yy+20,9*tile,6f,0xFFADB4B9.toInt());box(c,left,yy+58,9*tile,6f,0xFFADB4B9.toInt())
                    val warn=lane.warning(game.time) && (game.time*5).toInt()%2==0
                    for(edge in listOf(left-24,left+9*tile+8)) {
                        box(c,edge,yy+4,16f,66f,0xFF25323E.toInt(),4f)
                        box(c,edge+3,yy+12,10f,10f,if(warn)0xFFFF7869.toInt() else 0xFF71484A.toInt(),5f)
                        box(c,edge+3,yy+37,10f,10f,if(warn)0xFFFF7869.toInt() else 0xFF71484A.toInt(),5f)
                    }
                    lane.trainX(game.time)?.let { train ->
                        c.save();c.clipRect(left,yy,left+9*tile,yy+tile)
                        for(wagon in 0..3) {
                            val tx=left+(train+wagon*2.25f)*tile
                            box(c,tx,yy+12,2.18f*tile,61f,0xFF263744.toInt(),7f)
                            box(c,tx,yy+7,2.18f*tile,55f,0xFFD9AF70.toInt(),7f)
                            box(c,tx+12,yy+16,2.18f*tile-24,14f,0xFF37566C.toInt(),4f)
                            box(c,tx+12,yy+41,2.18f*tile-24,5f,0xFFEDCE94.toInt(),2f)
                        };c.restore()
                    }
                }
            }
            if(lane.kind==1 || lane.kind==2) {
                c.save();c.clipRect(left,yy,left+9*tile,yy+tile)
                var at=lane.start(game.time)
                while(at<9) {
                    val xx=left+at*tile;val ww=lane.length*tile
                    if(lane.kind==2) {
                      if(!lane.submerged(game.time)) {
                        box(c,xx,yy+25,ww,45f,0xFF64472F.toInt(),12f)
                        box(c,xx,yy+18,ww,42f,if(lane.sinking)0xFF629C8C.toInt() else 0xFFAF8255.toInt(),12f)
                        box(c,xx+14,yy+28,ww-28,4f,0xFFD1A574.toInt(),2f)
                        if(lane.sinkingWarning(game.time) && (game.time*5).toInt()%2==0)box(c,xx+12,yy+48,ww-24,5f,0xFFFFCD78.toInt(),2f)
                      }
                    } else {
                        box(c,xx+3,yy+24,ww,51f,0x770B1119,10f)
                        box(c,xx,yy+13,ww,51f,carColors[Math.floorMod(r,carColors.size)],10f)
                        box(c,xx+14,yy+8,20f,7f,0xFF172735.toInt(),3f);box(c,xx+ww-32,yy+8,20f,7f,0xFF172735.toInt(),3f)
                        box(c,xx+14,yy+61,20f,7f,0xFF172735.toInt(),3f);box(c,xx+ww-32,yy+61,20f,7f,0xFF172735.toInt(),3f)
                        box(c,xx+ww*.3f,yy+18,ww*.36f,39f,0xFF21374B.toInt(),6f)
                        box(c,xx+(if(lane.direction>0)ww-7 else 2f),yy+19,5f,10f,0xFFFFE7AE.toInt(),2f)
                        box(c,xx+(if(lane.direction>0)ww-7 else 2f),yy+47,5f,10f,0xFFFFE7AE.toInt(),2f)
                        if(lane.length>1.5f)box(c,xx+ww*.08f,yy+23,ww*.18f,31f,0x55FFFFFF,4f)
                    };at+=lane.gap
                };c.restore()
            }
            if(game.hasCoin(r)) {
                val xx=left+(lane.coin+.5f)*tile
                paint.color=0x775C4826;c.drawCircle(xx+2,yy+48,16f,paint)
                val cy=yy+42+sin(game.time*3+r).toFloat()*3f
                paint.color=if(lane.coinValue>1)0xFFFFBB66.toInt() else 0xFFFFD673.toInt();c.drawCircle(xx,cy,if(lane.coinValue>1)18f else 15f,paint)
                text(c,if(lane.coinValue>1)"3" else "•",xx-6,cy+7,if(lane.coinValue>1)19f else 24f,0xFF9D6B28.toInt(),true)
            }
        }
        val hx=left+(drawnX+.5f)*tile; val hy=y(drawnRow)+tile*.52f
        val bounce=(abs(drawnRow-game.row)+abs(drawnX-game.x)).coerceAtMost(1f)*19f
        hero(c,hx,hy-bounce,skin)
        if(game.dead) {
            paint.color=0xFFFF8276.toInt();paint.style=Paint.Style.STROKE;paint.strokeWidth=5f
            c.drawCircle(hx,hy,39f,paint);paint.style=Paint.Style.FILL
        }
        box(c,48f,50f,390f,418f,0xFF182B37.toInt(),28f)
        text(c,"PREJDI",82f,115f,42f,heavy=true);text(c,"CESTU",82f,162f,42f,heavy=true)
        text(c,"SKÓRE",82f,226f,20f,0xFFA4B8C1.toInt())
        text(c,game.score.toString().padStart(3,'0'),78f,318f,92f,heavy=true)
        text(c,"REKORD  ${maxOf(best,game.score)}",82f,377f,25f,0xFFF5D489.toInt(),true)
        text(c,"Tento pokus: ${coinCount(game.coins)}",82f,427f,22f,0xFFB6C9CE.toInt())
        box(c,48f,488f,390f,184f,0xFF182B37.toInt(),28f)
        text(c,"NÁROČNOSŤ ${game.difficulty}/10",82f,535f,22f,0xFFF5D489.toInt(),true)
        text(c,game.difficultyLabel,82f,579f,28f,heavy=true)
        text(c,game.biome,82f,626f,21f,0xFFB6C9CE.toInt())
        box(c,82f,644f,320f,5f,0xFF2D424B.toInt(),2f)
        box(c,82f,644f,320f*game.difficulty/10f,5f,0xFF8FD8C2.toInt(),2f)
        box(c,1482f,50f,390f,400f,0xFF182B37.toInt(),28f)
        text(c,"TVOJA ZBIERKA",1516f,105f,22f,0xFFA4B8C1.toInt())
        hero(c,1674f,177f,skin)
        text(c,coinCount(wallet),1516f,267f,32f,0xFFF5D489.toInt(),true)
        text(c,"Zbierka $unlockedCount / ${CrossingCollection.skins.size}",1516f,312f,21f,0xFFB6C9CE.toInt())
        text(c,if(nextCost>0)nextName else "Zbierka je kompletná",1516f,360f,25f,heavy=true)
        text(c,if(nextCost>0)"$nextCost mincí · ďalší vzhľad" else "Každá cesta má svoj štýl.",1516f,403f,20f,0xFFB6C9CE.toInt())
        box(c,1482f,786f,390f,244f,0xFF182B37.toInt(),28f)
        text(c,"ŠÍPKY  ·  pohyb",1516f,843f,25f,heavy=true)
        text(c,"OK / SPÄŤ  ·  pauza",1516f,889f,25f,heavy=true)
        text(c,if(game.score>=35)"Vlak hlásia červené svetlá." else "Minca s číslom 3 má trojnásobnú hodnotu.",1516f,948f,21f,0xFFB6C9CE.toInt())
        text(c,"Cez rieku po dreve.",1516f,986f,22f,0xFFB6C9CE.toInt())
        c.restore()
    }
    private fun hero(c: Canvas,x: Float,y: Float,style: Int) {
        CrossingArt.draw(c,x,y,style,paint,rect)
    }
}
