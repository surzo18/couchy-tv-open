package com.conreo.couchytv

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.json.JSONObject
import java.io.File

/** Home-only audio. No service, network requests, or player retained behind another app. */
object HomeMusic {
    data class Track(val file: File, val title: String, val detail: String)
    var enabled by mutableStateOf(false); private set
    var volume by mutableFloatStateOf(.2f); private set
    var randomFallback by mutableStateOf(true); private set
    var startupEnabled by mutableStateOf(true); private set
    var status by mutableStateOf("Vypnuté"); private set
    var title by mutableStateOf(""); private set
    var detail by mutableStateOf(""); private set
    private lateinit var app: Context
    private var loaded = false
    private var home = false
    private var wallpaper = ""
    private var selectedFor = ""
    private var track: Track? = null
    private var requestedTrack: String? = null
    private var position = 0L
    private var player: ExoPlayer? = null
    fun isAudioActive()=player?.isPlaying==true
    fun holdsAudio()=player!=null
    private var intro = false
    private var startupPending = false
    private var lastWake = 0L
    private val failed = mutableSetOf<String>()
    private val queue = SoundtrackQueue()
    private var queueFor: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val start = Runnable { play(false) }
    private var fadeStartPosition = 0L
    private var swapStarted = 0L
    private var swapGain = 1f
    private var afterFade: (() -> Unit)? = null
    private val envelope = object : Runnable {
        override fun run() {
            val p = player ?: return
            if (intro) return
            val action = afterFade
            val gain = if (action != null) {
                val elapsed = android.os.SystemClock.elapsedRealtime() - swapStarted
                if (elapsed >= 400) {
                    release(); action(); return
                }
                swapGain * (1f - elapsed / 400f).coerceIn(0f, 1f)
            } else AudioEnvelope.gain(p.currentPosition, p.duration, fadeStartPosition)
            p.volume = volume * gain
            if (home) handler.postDelayed(this, 50)
        }
    }
    private fun transition(action: () -> Unit) {
        val p = player
        if (p == null || intro || !p.isPlaying) { release(); action(); return }
        if (afterFade == null) {
            swapStarted = android.os.SystemClock.elapsedRealtime()
            swapGain = (p.volume / volume).coerceIn(0f, 1f)
        }
        afterFade = action
    }
    private fun prefs() = app.getSharedPreferences("home_music", 0)
    fun load(context: Context) {
        if (loaded) return
        app = context.applicationContext; loaded = true
        enabled = prefs().getBoolean("enabled", false)
        volume = prefs().getFloat("volume", .2f).coerceIn(.05f, .5f)
        randomFallback = prefs().getBoolean("randomFallback", true)
        startupEnabled = prefs().getBoolean("startupEnabled", true)
        val boot = android.provider.Settings.Global.getInt(app.contentResolver, "boot_count", -1)
        startupPending = prefs().getInt("lastIntroBoot", Int.MIN_VALUE) != boot
        prefs().edit().putInt("lastIntroBoot", boot).apply()
    }
    fun wake(context: Context) {
        load(context)
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastWake > 2_000) startupPending = true
        lastWake = now
    }
    fun toggleStartup() {
        startupEnabled = !startupEnabled
        prefs().edit().putBoolean("startupEnabled", startupEnabled).apply()
    }
    fun previewStartup() { startupPending = true }
    fun progress(): Pair<Long, Long> = player?.let { it.currentPosition to it.duration.coerceAtLeast(0) } ?: (0L to 0L)
    private fun catalog() = runCatching {
        MediaLibraryStore.catalog(app)
    }.getOrNull()
    private fun choices(): List<Track> {
        val data = catalog() ?: return emptyList()
        val folder = File(app.getExternalFilesDir(null), "music")
        if (queueFor != wallpaper) {
            queue.reset(MediaLibraryStore.binding(app, wallpaper, data))
            queueFor = wallpaper
        }
        return MediaLibraryStore.songs(app, data).filter { it.enabled && it.id !in failed }.map { item ->
            Track(File(folder, item.id), item.title.take(120),
                listOf(item.artist, item.origin)
                    .filter { it.isNotBlank() }.distinct().joinToString(" · ").take(220))
        }
    }
    fun libraryChanged() {
        if (!loaded) return
        handler.removeCallbacks(start); release(); track = null; position = 0; queueFor = null
        requestedTrack = null; failed.clear(); title = ""; detail = ""
        if (home) handler.postDelayed(start, 650)
    }
    fun requestSong(context: Context, id: String) {
        load(context); libraryChanged()
        require(MediaLibraryStore.songs(app).any { it.id == id && it.enabled }) { "Skladba nie je zapnutá alebo už neexistuje." }
        requestedTrack = id; enabled = true; prefs().edit().putBoolean("enabled", true).apply()
    }
    fun enterHome(context: Context, uri: String) {
        load(context); home = true
        setWallpaper(uri)
        handler.removeCallbacks(start)
        handler.postDelayed(start, 650)
    }
    fun setWallpaper(uri: String) {
        val name = Uri.parse(uri).lastPathSegment ?: ""
        if (wallpaper != name) {
            val wasPlaying = player != null
            wallpaper = name
            transition {
                track = null; position = 0; failed.clear(); queueFor = null
                if (home && loaded && wasPlaying) play(true)
            }
        }
        if (home && loaded) { handler.removeCallbacks(start); handler.postDelayed(start, 650) }
    }
    fun leaveHome() {
        home = false; handler.removeCallbacks(start); release()
        if (enabled) status = "Pripravené na prehrávanie na ploche"
    }
    fun yieldToExternal(){handler.removeCallbacks(start);release();status="Hudbu prehráva iná aplikácia"}
    fun toggle(context: Context) {
        load(context); enabled = !enabled
        prefs().edit().putBoolean("enabled", enabled).apply()
        failed.clear()
        if (enabled) { status = "Pripravené na prehrávanie na ploche"; play(true) }
        else { handler.removeCallbacks(start); release(); status = "Vypnuté" }
    }
    fun changeVolume() {
        val options = listOf(.1f, .2f, .3f, .5f)
        volume = options[(options.indexOf(volume) + 1).mod(options.size)]
        prefs().edit().putFloat("volume", volume).apply()
        if (intro) player?.volume = volume
    }
    fun toggleFallback() {
        randomFallback = !randomFallback
        prefs().edit().putBoolean("randomFallback", randomFallback).apply()
        release(); track = null; position = 0
    }
    fun next() {
        transition {
            position = 0; failed.clear()
            selectNext(); if (home) play(true)
        }
    }
    private fun selectNext() {
        val choices = choices()
        val requested = requestedTrack?.takeIf { id -> choices.any { it.file.name == id } }
        requestedTrack = null
        val selected = requested ?: queue.next(choices.map { it.file.name }, randomFallback)
        if (requested != null) queue.markPlayed(requested)
        track = choices.firstOrNull { it.file.name == selected }
        selectedFor = wallpaper; title = track?.title.orEmpty()
        detail = track?.detail.orEmpty()
        android.util.Log.i("CouchyMusic", "Queue wallpaper=$wallpaper next=${track?.file?.name ?: "none"}")
    }
    private fun release() {
        handler.removeCallbacks(envelope); afterFade = null
        player?.let { p ->
            if (!intro) position = p.currentPosition
            p.release()
            android.util.Log.i("CouchyMusic", "Released ${if (intro) "startup" else track?.file?.name} at $position")
        }
        player = null; intro = false
    }
    private fun play(explicit: Boolean) {
        if (!loaded || !home || (!enabled && !startupPending) || !Experience.enabled || player != null) return
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // Returning from Spotify must not steal its audio focus. An explicit toggle may.
        if (!explicit && audio.isMusicActive) {
            startupPending = false
            status = "Hudbu prehráva iná aplikácia"
            android.util.Log.i("CouchyMusic", "Deferred: other audio active")
            return
        }
        if (startupPending) {
            startupPending = false
            val file = EditionConfig.startupFile(app)
            if (startupEnabled && file != null && file.isFile) { playIntro(file); return }
        }
        if (!enabled) return
        if (track == null || selectedFor != wallpaper) { position = 0; selectNext() }
        val selected = track ?: run { status = "Pre toto pozadie nie je dostupná skladba"; return }
        val p = ExoPlayer.Builder(app).build()
        player = p
        p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
        p.setHandleAudioBecomingNoisy(true)
        fadeStartPosition = position
        p.volume = 0f
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (player !== p) return
                status = if (isPlaying) "Hrá: ${selected.title}" else "Pozastavené"
                android.util.Log.i("CouchyMusic", "Playing=$isPlaying ${selected.file.name}")
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && player === p) handler.post {
                    if (player === p && home) { release(); position = 0; selectNext(); play(true) }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                RuntimeHealth.mediaFailure("Hudba: ${error.errorCodeName}")
                android.util.Log.w("CouchyMusic", "Audio error ${error.errorCodeName}: ${selected.file.name}")
                handler.post {
                    if (player === p) {
                        failed.add(selected.file.name); release(); position = 0; selectNext(); play(true)
                    }
                }
            }
        })
        p.setMediaItem(MediaItem.fromUri(Uri.fromFile(selected.file)), position)
        p.prepare(); p.play()
        handler.post(envelope)
    }
    private fun playIntro(file: File) {
        val p = ExoPlayer.Builder(app).build()
        player = p; intro = true
        p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
        p.setHandleAudioBecomingNoisy(true); p.volume = volume
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                android.util.Log.i("CouchyMusic", "Startup playing=$isPlaying")
            }
            private fun complete() { handler.post {
                if (player === p) { release(); if (home) play(true) }
            } }
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) complete() }
            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.w("CouchyMusic", "Startup error ${error.errorCodeName}"); complete()
            }
        })
        p.setMediaItem(MediaItem.fromUri(Uri.fromFile(file))); p.prepare(); p.play()
    }
}
