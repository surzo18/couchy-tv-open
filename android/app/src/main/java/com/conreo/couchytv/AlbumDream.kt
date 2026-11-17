package com.conreo.couchytv

import android.content.Context
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.conreo.couchytv.ui.drawWeatherIcon
import kotlin.math.roundToInt

/** Align to the text's baseline metrics rather than the bottom of a multiline line box. */
private class CenteredAlbumIcon(private val icon:android.graphics.drawable.Drawable):android.text.style.ReplacementSpan(){
    override fun getSize(paint:android.graphics.Paint,text:CharSequence,start:Int,end:Int,fm:android.graphics.Paint.FontMetricsInt?):Int=icon.bounds.width()
    override fun draw(canvas:android.graphics.Canvas,text:CharSequence,start:Int,end:Int,x:Float,top:Int,y:Int,bottom:Int,paint:android.graphics.Paint){
        val metrics=paint.fontMetrics
        val iconTop=y+(metrics.ascent+metrics.descent)/2f-icon.bounds.height()/2f
        val saved=canvas.save();canvas.translate(x,iconTop);icon.draw(canvas);canvas.restoreToCount(saved)
    }
}

/** Local-only slideshow: one decoder job, two image surfaces, no disk cache or polling. */
class AlbumStage(c:Context,private val startUri:String?=null,private val exit:()->Unit):FrameLayout(c){
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val photo=ImageView(c).apply{scaleType=ImageView.ScaleType.FIT_CENTER}
    private val video=VideoView(c)
    private val label=TextView(c).apply{textSize=18f;setTextColor(Color.WHITE);setPadding(26,20,26,20);maxWidth=(resources.displayMetrics.widthPixels*.82f).toInt();setLineSpacing(5f,1f);background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0xE6182434.toInt(),0xED0D151F.toInt())).apply{cornerRadius=20f}}
    private val clock=TextClock(c).apply{format24Hour="HH:mm";format12Hour="HH:mm";textSize=23f;setTextColor(Color.WHITE);setPadding(22,12,22,12);setShadowLayer(4f,0f,2f,Color.BLACK);background=GradientDrawable().apply{setColor(0xBB0C131D.toInt());cornerRadius=16f};compoundDrawablePadding=12}
    private var list=emptyList<AlbumPhoto>();private var index=-1;private var job:Job?=null;private var bitmap:Bitmap?=null
    private var closed=false
    private val decoderGate=Semaphore(1)
    init{setBackgroundColor(Color.BLACK);addView(photo,LayoutParams(-1,-1));addView(video,LayoutParams(-1,-1));video.visibility=View.GONE;addView(label,LayoutParams(-2,-2,Gravity.BOTTOM or Gravity.START).apply{setMargins(30,0,30,26)});addView(clock,LayoutParams(-2,-2,Gravity.TOP or Gravity.END).apply{setMargins(30,26,30,0)});isFocusableInTouchMode=true;requestFocus()
        scope.launch{Weather.load(c);while(true){if(Weather.enabled)Weather.refresh(c);updateClock();delay(30*60_000L)}}
        scope.launch{list=withContext(Dispatchers.IO){runCatching{
            val all=albumPhotos(c)
            if(startUri==null)AlbumStore.eligible(c,all).shuffled() else {
                val first=all.firstOrNull{it.uri.toString()==startUri}
                if(first==null)emptyList() else {val n=AlbumStore.notes(c,first);listOf(first)+all.filter{it.uri!=first.uri && AlbumStore.notes(c,it).let{next->next.album==n.album && (n.date.isEmpty() || next.date==n.date)}}}
            }
        }.onFailure{DiagnosticLog.report("Fotoalbum","CHYBA","Zoznam fotografií sa nepodarilo načítať.",it)}.getOrDefault(emptyList())};if(list.isEmpty())label.text="Vyber fotografie v ponuke Albumy a šetrič." else show(1)}
    }
    private fun updateClock(){
        val weather=Weather.data?.takeIf{Weather.enabled && System.currentTimeMillis()-it.fetched in 0..10_800_000L}
        val pattern=if(weather==null)"HH:mm" else "'${Weather.temperature(weather.temperature,false)}'   HH:mm"
        clock.format24Hour=pattern;clock.format12Hour=pattern
        val icon=weather?.let{
            val size=(clock.textSize*1.15f).roundToInt().coerceAtLeast(24)
            val bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888)
            androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(androidx.compose.ui.unit.Density(resources.displayMetrics.density),androidx.compose.ui.unit.LayoutDirection.Ltr,androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bitmap)),androidx.compose.ui.geometry.Size(size.toFloat(),size.toFloat())){drawWeatherIcon(it.code,it.daytime)}
            android.graphics.drawable.BitmapDrawable(resources,bitmap).apply{setBounds(0,0,size,size)}
        }
        clock.setCompoundDrawables(icon,null,null,null)
    }
    fun show(step:Int){
        if(closed || list.isEmpty())return
        job?.cancel();video.stopPlayback();video.visibility=View.GONE
        index=(index+step).mod(list.size);val p=list[index];val notes=AlbumStore.notes(context,p)
        val header="\uFFFC "+AlbumStore.name(context,notes.album)
        val date=runCatching{java.time.LocalDate.parse(notes.date).format(java.time.format.DateTimeFormatter.ofPattern("d. MMMM yyyy",com.conreo.couchytv.AppLanguage.locale()))}.getOrDefault(notes.date)
        val footer=listOf(date,notes.place).filter{it.isNotBlank()}.joinToString(" · ")
        label.text=if(AlbumStore.showInfo(context))android.text.SpannableString(listOf(header,notes.caption,footer).filter{it.isNotBlank()}.joinToString("\n")).apply{
            setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD),0,header.length,0)
            setSpan(android.text.style.ForegroundColorSpan(0xFFB7DAFF.toInt()),0,header.length,0)
            context.getDrawable(android.R.drawable.ic_menu_gallery)?.mutate()?.let{icon->val size=label.textSize.toInt();icon.setBounds(0,0,size,size);icon.setTint(0xFFB7DAFF.toInt());setSpan(CenteredAlbumIcon(icon),0,1,0)}
            if(footer.isNotEmpty()){
                val start=length-footer.length
                setSpan(android.text.style.RelativeSizeSpan(.82f),start,length,0)
                setSpan(android.text.style.ForegroundColorSpan(0xFFCAD5E0.toInt()),start,length,0)
                if(date.isNotEmpty())setSpan(android.text.style.StyleSpan(android.graphics.Typeface.ITALIC),start,start+date.length,0)
            }
        } else ""
        label.visibility=if(label.text.isEmpty())View.GONE else View.VISIBLE
        if(p.video){
            photo.visibility=View.GONE;video.visibility=View.VISIBLE;video.setVideoURI(p.uri)
            video.setOnPreparedListener{job?.cancel();it.setVolume(0f,0f);it.isLooping=false;video.start()}
            video.setOnCompletionListener{show(1)};video.setOnErrorListener{_,_,_->job=scope.launch{delay(1500);show(1)};true}
            job=scope.launch{delay(15_000);show(1)}
        }else{
            photo.visibility=View.VISIBLE
            job=scope.launch{
                val loaded=withContext(Dispatchers.IO){decoderGate.withPermit{runCatching{ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver,p.uri)){decoder,info,_->val maxW=resources.displayMetrics.widthPixels.coerceIn(1280,3840);val ratio=minOf(1f,maxW.toFloat()/info.size.width,2160f/info.size.height);decoder.setTargetSize((info.size.width*ratio).toInt().coerceAtLeast(1),(info.size.height*ratio).toInt().coerceAtLeast(1));decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE}}.getOrNull()}}
                if(loaded!=null){photo.setImageBitmap(loaded);bitmap=loaded}else {label.text="Fotografiu sa nepodarilo načítať.";DiagnosticLog.report("Fotoalbum","CHYBA","Dekódovanie fotografie zlyhalo.")}
                if(startUri==null){delay(if(loaded!=null)20_000 else 2000);show(1)}
            }
        }
    }
    override fun dispatchKeyEvent(e:KeyEvent):Boolean{
        if(e.keyCode in listOf(KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_BACK)){
            if(e.action==KeyEvent.ACTION_UP)when(e.keyCode){KeyEvent.KEYCODE_DPAD_LEFT->show(-1);KeyEvent.KEYCODE_DPAD_RIGHT->show(1);else->exit()};return true
        };return super.dispatchKeyEvent(e)
    }
    fun close(){closed=true;job?.cancel();scope.cancel();video.stopPlayback();photo.setImageDrawable(null);bitmap=null}
}
class AlbumDream:DreamService(){
    private var stage:AlbumStage?=null
    override fun onAttachedToWindow(){super.onAttachedToWindow();isInteractive=true;isFullscreen=true;isScreenBright=false;stage=AlbumStage(this){finish()};setContentView(stage)}
    override fun onDetachedFromWindow(){stage?.close();stage=null;super.onDetachedFromWindow()}
}
class AlbumPreviewActivity:android.app.Activity(){
    private var stage:AlbumStage?=null
    override fun onCreate(b:Bundle?){super.onCreate(b);window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY}
    override fun onStart(){super.onStart();stage=AlbumStage(this,intent.getStringExtra("photo")){finish()};setContentView(stage)}
    override fun onStop(){stage?.close();stage=null;super.onStop()}
}
