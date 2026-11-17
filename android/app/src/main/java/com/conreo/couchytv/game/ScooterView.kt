package com.conreo.couchytv.game

import android.content.Context
import android.graphics.*
import android.view.Choreographer
import android.view.KeyEvent
import android.view.View
import kotlin.math.*

class ScooterView(context:Context,val game:ScooterGame,val pause:()->Unit,val progress:()->Unit):View(context),Choreographer.FrameCallback {
    var best=0;var wallet=0;var skin=0
    var sounds:GameSounds?=null
    private var active=false;private var scheduled=false;private var lastFrame=0L;private var lastSave=0f
    private val p=Paint(Paint.ANTI_ALIAS_FLAG);private val rect=RectF();private val path=Path()
    private val font=Typeface.create("sans-serif-medium",Typeface.NORMAL);private val bold=Typeface.create("sans-serif",Typeface.BOLD)
    private val sky=listOf(0xFF31566E.toInt() to 0xFFADC5C4.toInt(),0xFF295E76.toInt() to 0xFFA7CFCB.toInt(),0xFF614B72.toInt() to 0xFFDDA391.toInt(),0xFF152A48.toInt() to 0xFF52658A.toInt()).map{LinearGradient(0f,0f,0f,1080f,it.first,it.second,Shader.TileMode.CLAMP)}
    private val menu=Runnable{pause()}
    private val wheels=floatArrayOf(-33f,39f)
    private val biomeNames=arrayOf("Mesto","Mólo","Západ slnka","Nočné mesto")
    init{isFocusableInTouchMode=true;isFocusable=true;contentDescription="Kolobežka. Hore skok, dvakrát hore trik, dole prikrčenie, vľavo brzda, vpravo zrýchlenie, OK pauza."}
    fun running(value:Boolean){removeCallbacks(menu);active=value;game.paused=!value || game.dead;lastFrame=0
        if(value){requestFocus();schedule()}else{Choreographer.getInstance().removeFrameCallback(this);scheduled=false;game.brake=false;game.boost=false;sounds?.pause()};invalidate()}
    private fun schedule(){if(active && !scheduled){scheduled=true;Choreographer.getInstance().postFrameCallback(this)}}
    override fun doFrame(now:Long){scheduled=false;if(!active)return
        val dt=if(lastFrame==0L)0f else ((now-lastFrame)/1_000_000_000f).coerceAtMost(.05f);lastFrame=now
        val oldCoins=game.coins;game.update(dt)
        while(true){val sound=game.event() ?: break;sounds?.play(sound,gain=if(sound=="land").6f else 1f)}
        sounds?.scooterEnvironment(game)
        if(game.coins!=oldCoins || game.time<lastSave || game.time-lastSave>=2f){lastSave=game.time;progress()}
        if(game.dead){running(false);progress();postDelayed(menu,450)}
        invalidate();schedule()
    }
    override fun onDetachedFromWindow(){running(false);super.onDetachedFromWindow()}
    override fun onKeyDown(code:Int,event:KeyEvent):Boolean {
        when(code){
            KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_BACK->{if(event.repeatCount==0){running(false);progress();pause()};return true}
            KeyEvent.KEYCODE_DPAD_UP->{if(event.repeatCount==0)game.jump();return true}
            KeyEvent.KEYCODE_DPAD_DOWN->{game.duck();return true}
            KeyEvent.KEYCODE_DPAD_LEFT->{if(!game.brake)sounds?.play("brake",gain=.4f);game.brake=true;return true}
            KeyEvent.KEYCODE_DPAD_RIGHT->{game.boost=true;return true}
        };return super.onKeyDown(code,event)
    }
    override fun onKeyUp(code:Int,event:KeyEvent):Boolean=when(code){KeyEvent.KEYCODE_DPAD_LEFT->{game.brake=false;true};KeyEvent.KEYCODE_DPAD_RIGHT->{game.boost=false;true};KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN->true;else->super.onKeyUp(code,event)}
    private fun box(c:Canvas,x:Float,y:Float,w:Float,h:Float,color:Int,r:Float=0f){p.color=color;p.shader=null;p.style=Paint.Style.FILL;rect.set(x,y,x+w,y+h);c.drawRoundRect(rect,r,r,p)}
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int=0xFFF1F5EF.toInt(),strong:Boolean=false){p.color=color;p.shader=null;p.typeface=if(strong)bold else font;p.textSize=size;c.drawText(s,x,y,p)}
    override fun onDraw(c:Canvas){super.onDraw(c);c.save();c.scale(width/1920f,height/1080f)
        p.shader=sky[game.biome];c.drawRect(0f,0f,1920f,1080f,p);p.shader=null
        val camera=game.x-450f
        p.color=if(game.biome==3)0xFFC8DBE8.toInt() else 0xFFFFDDA5.toInt();c.drawCircle(1540f,240f,62f,p)
        for(i in -1..11){val index=floor(camera*.17f/200).toInt()+i;val x=index*200-camera*.17f;val h=160f+Math.floorMod(index*137,190)
            box(c,x,620-h,152f,h,if(game.biome==3)0xFF223D5B.toInt() else 0x55506A7C)
            if(game.biome==0 || game.biome==3)for(window in 0..2)box(c,x+23+window*38,650-h,15f,24f,if(game.biome==3)0x99E7BD7A.toInt() else 0x446C94A3)
        }
        box(c,0f,620f,1920f,460f,if(game.biome==1)0xFF397E94.toInt() else 0xFF385A67.toInt())
        if(game.biome==1)for(i in 0..16)box(c,i*130f-(camera*.25f)%130,880f+(i%3)*24,70f,4f,0x5567B8C2,2f)
        for(platform in game.platforms){val x=platform.start-camera;val w=platform.end-platform.start;if(x>1920 || x+w<0)continue
            box(c,x,platform.y,w,1080-platform.y,if(game.biome==1)0xFF745F4E.toInt() else 0xFF364653.toInt(),5f)
            box(c,x,platform.y,w,13f,if(game.biome==1)0xFFC7A77C.toInt() else 0xFF9FC2AC.toInt(),4f)
            box(c,x,platform.y+25,w,5f,0x440F2232)
            if(game.biome==1)for(i in 0..(w/100).toInt())box(c,x+i*100,platform.y+14,6f,1080-platform.y,0x44312621)
            val ox=platform.obstacleX-camera
            when(platform.obstacle){
                1->{p.color=0xFFEB9766.toInt();path.reset();path.moveTo(ox,platform.y-43);path.lineTo(ox-25,platform.y);path.lineTo(ox+25,platform.y);path.close();c.drawPath(path,p);box(c,ox-16,platform.y-18,32f,7f,0xFFF2E6C9.toInt(),2f)}
                2->{box(c,ox-28,platform.y-88,56f,88f,0xFFC98B63.toInt(),5f);box(c,ox-28,platform.y-66,56f,13f,0xFFEFD7AA.toInt());box(c,ox-28,platform.y-28,56f,13f,0xFFEFD7AA.toInt())}
                3->{box(c,ox-52,platform.y-150,8f,150f,0xFF627C88.toInt(),3f);box(c,ox+44,platform.y-150,8f,150f,0xFF627C88.toInt(),3f);box(c,ox-48,platform.y-135,96f,70f,0xFFB8D1C8.toInt(),6f);text(c,"↓",ox-16,platform.y-86,44f,0xFF304E5B.toInt(),true)}
            }
            platform.coins.forEachIndexed{i,cx->if(platform.taken and (1 shl i)==0){val cy=platform.y-(if(i==1)130 else 85);p.color=0xFFFFD678.toInt();c.drawCircle(cx-camera,cy+sin(game.time*3+i)*3,13f,p);p.color=0xFFAD813E.toInt();c.drawCircle(cx-camera,cy+sin(game.time*3+i)*3,5f,p)}}
        }
        c.save();c.translate(450f,game.y-40)
        val trick=game.time-game.lastTrick
        val angle=if(trick in 0f.. .48f)trick/.48f*360 else if(!game.grounded)(game.vy*.015f).coerceIn(-12f,16f) else if(game.boost)-4f else 0f
        c.rotate(angle)
        val deck=CrossingCollection.skin(skin).color.toInt()
        for(wheel in wheels){p.color=0xFF172838.toInt();c.drawCircle(wheel,26f,16f,p);p.color=0xFFB0C4C8.toInt();c.drawCircle(wheel,26f,8f,p)
            p.color=0xFF4E6C7E.toInt();p.strokeWidth=2f;val spin=game.x/16;c.drawLine(wheel-cos(spin)*7,26-sin(spin)*7,wheel+cos(spin)*7,26+sin(spin)*7,p)}
        box(c,-43f,9f,83f,8f,deck,4f);p.color=0xFFADC5CC.toInt();p.strokeWidth=6f;p.strokeCap=Paint.Cap.ROUND
        c.drawLine(39f,18f,26f,-49f,p);c.drawLine(14f,-50f,39f,-50f,p)
        c.save();if(game.ducking){c.translate(0f,23f);c.scale(1.1f,.62f)}
        CrossingArt.draw(c,-12f,-34f,skin,p,rect)
        p.color=0xFFB6CBCF.toInt();p.strokeWidth=5f;c.drawLine(7f,-36f,26f,-50f,p);c.restore();c.restore()
        if(game.dead){p.color=0xFFFF8F79.toInt();p.style=Paint.Style.STROKE;p.strokeWidth=5f;c.drawCircle(450f,game.y-40,77f,p);p.style=Paint.Style.FILL}
        box(c,42f,36f,470f,142f,0xEA172C3A.toInt(),24f);text(c,"KOLOBEŽKA",70f,78f,21f,0xFFAEC8D5.toInt(),true)
        text(c,"${game.score} m",68f,145f,53f,strong=true);text(c,"Rekord ${maxOf(best,game.score)} m",290f,142f,22f,0xFFF5D489.toInt())
        box(c,1330f,36f,548f,142f,0xEA172C3A.toInt(),24f);text(c,"${coinCount(wallet)}",1360f,92f,32f,0xFFF5D489.toInt(),true)
        text(c,"Náročnosť ${game.difficulty}/10 · ${biomeNames[game.biome]}",1360f,143f,23f)
        box(c,42f,967f,1836f,76f,0xED172C3A.toInt(),20f)
        text(c,"↑ skok / druhý skok = trik     ↓ prikrčenie     ← brzda     → zrýchlenie     OK / Späť pauza",76f,1015f,26f)
        c.restore()
    }
}
