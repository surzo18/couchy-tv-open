package com.conreo.couchytv

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.*

/** Permission bridge only. Notification contents are never read, logged or stored. */
class MediaAccessService:NotificationListenerService() {
    override fun onListenerConnected(){ExternalMedia.permissionChanged()}
    override fun onListenerDisconnected(){ExternalMedia.permissionChanged()}
}
data class ExternalTrack(val app:String,val title:String,val artist:String,val playing:Boolean,val actions:Long,val packageName:String,val clock:MediaClock)
object ExternalMedia {
    var current by mutableStateOf<ExternalTrack?>(null);private set
    var permitted by mutableStateOf(false);private set
    private var context:Context?=null
    private var manager:MediaSessionManager?=null
    private var watching=false
    private var selected:MediaController?=null
    private val handler=Handler(Looper.getMainLooper())
    private val controllers=LinkedHashMap<MediaController,MediaController.Callback>()
    private val listener=MediaSessionManager.OnActiveSessionsChangedListener{bind(it.orEmpty())}
    fun start(c:Context){if(watching)return;context=c.applicationContext;watching=true;connect()}
    private fun connect() {
        val c=context ?: return
        val m=c.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        runCatching {
            manager?.removeOnActiveSessionsChangedListener(listener)
            m.addOnActiveSessionsChangedListener(listener,ComponentName(c,MediaAccessService::class.java),handler)
            manager=m;permitted=true;bind(m.getActiveSessions(ComponentName(c,MediaAccessService::class.java)))
        }.onFailure{permitted=false;bind(emptyList())}
    }
    fun permissionChanged(){handler.post{if(watching)connect()}}
    private fun bind(list:List<MediaController>) {
        controllers.forEach{(c,cb)->runCatching{c.unregisterCallback(cb)}};controllers.clear()
        val own=context?.packageName
        list.filter{it.packageName!=own}.take(6).forEach{c->
            val cb=object:MediaController.Callback(){
                override fun onPlaybackStateChanged(state:PlaybackState?){publish()}
                override fun onMetadataChanged(metadata:MediaMetadata?){publish()}
                override fun onSessionDestroyed(){controllers.remove(c)?.let{runCatching{c.unregisterCallback(it)}};publish()}
            }
            runCatching{c.registerCallback(cb,handler);controllers[c]=cb}
        };publish()
    }
    private fun playing(c:MediaController)=c.playbackState?.state in listOf(PlaybackState.STATE_PLAYING,PlaybackState.STATE_BUFFERING,PlaybackState.STATE_CONNECTING)
    private fun publish() {
        val c=controllers.keys.firstOrNull{playing(it)}
            ?: controllers.keys.firstOrNull{it.sessionToken==selected?.sessionToken}
            ?: controllers.keys.firstOrNull{it.playbackState?.state==PlaybackState.STATE_PAUSED}
        selected=c
        if(c==null){current=null;return}
        val m=c.metadata;val p=c.playbackState
        val app=runCatching{context!!.packageManager.getApplicationLabel(context!!.packageManager.getApplicationInfo(c.packageName,0)).toString()}.getOrDefault(c.packageName)
        current=ExternalTrack(app.take(40),(m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Prehrávanie").take(140),
            (m?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "").take(120),playing(c),p?.actions ?: 0,c.packageName,
            MediaClock(p?.position ?: -1,m?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0,p?.lastPositionUpdateTime ?: 0,p?.playbackSpeed ?: 0f,p?.state==PlaybackState.STATE_PLAYING))
        if(playing(c) && HomeMusic.holdsAudio())HomeMusic.yieldToExternal()
    }
    fun supports(action:Long)=((current?.actions ?: 0) and action)!=0L
    fun artwork(pkg:String):android.graphics.Bitmap? {
        val c=selected?.takeIf{it.packageName==pkg} ?: return null
        val m=c.metadata ?: return null
        return runCatching{m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)}.getOrNull()
    }
    fun toggle(){val c=selected ?: return;runCatching{
        if(current?.playing==true){if(supports(PlaybackState.ACTION_PAUSE) || supports(PlaybackState.ACTION_PLAY_PAUSE))c.transportControls.pause()}
        else if(supports(PlaybackState.ACTION_PLAY) || supports(PlaybackState.ACTION_PLAY_PAUSE)){HomeMusic.yieldToExternal();c.transportControls.play()}
    }.onFailure{publish()}}
    fun next(){if(supports(PlaybackState.ACTION_SKIP_TO_NEXT))runCatching{selected?.transportControls?.skipToNext()}}
    fun previous(){if(supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS))runCatching{selected?.transportControls?.skipToPrevious()}}
    fun open(c:Context,pkg:String):Boolean {
        val pm=c.packageManager
        val intents=listOfNotNull(runCatching{pm.getLeanbackLaunchIntentForPackage(pkg)}.getOrNull(),
            runCatching{pm.getLaunchIntentForPackage(pkg)}.getOrNull()).distinctBy{it.component}
        for(intent in intents)if(runCatching{c.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))}.isSuccess){
            LastApp.observe(c,pkg);Experience.homePanel="";return true
        }
        val pending=controllers.keys.firstOrNull{it.packageName==pkg}?.sessionActivity
        if(pending!=null && runCatching{pending.send()}.isSuccess){LastApp.observe(c,pkg);Experience.homePanel="";return true}
        Actions.toast(c,"Aplikáciu sa nepodarilo otvoriť.");return false
    }
    fun stop(){watching=false;manager?.let{runCatching{it.removeOnActiveSessionsChangedListener(listener)}};manager=null
        controllers.forEach{(c,cb)->runCatching{c.unregisterCallback(cb)}};controllers.clear();selected=null;current=null;context=null}
}
