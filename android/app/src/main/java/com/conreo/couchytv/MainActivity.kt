package com.conreo.couchytv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.media.tv.TvInputManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.conreo.couchytv.ui.setTvContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.conreo.couchytv.ui.LauncherApp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import android.view.KeyEvent

class MainActivity : LocalizedActivity() {
    private val backHandler = Handler(Looper.getMainLooper())
    private var backHeld = false
    private var backReturned = false
    private val returnToApp = Runnable {
        if (backHeld && Experience.canReturnToApp && hasWindowFocus()) {
            backReturned = true
            LastApp.resume(this)
        }
    }
    private var swallowedKey = -1
    private var initialized = false
    private var sessionWallpaper by mutableStateOf("")
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) HomeMusic.leaveHome()
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                sessionWallpaper = WallpaperCycle.screenOn(context)
                if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))
                    HomeMusic.enterHome(this@MainActivity, sessionWallpaper)
            }
        }
    }
    var homeTick by mutableIntStateOf(0)
        private set
    var inputTick by mutableIntStateOf(0)
        private set
    private val inputManager by lazy { getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager }
    private val inputCallback = object : TvInputManager.TvInputCallback() {
        override fun onInputStateChanged(inputId: String, state: Int) { inputTick++ }
        override fun onInputAdded(inputId: String) { inputTick++ }
        override fun onInputRemoved(inputId: String) { inputTick++ }
        override fun onInputUpdated(inputId: String) { inputTick++ }
    }

    /** Bumped whenever a package is installed/removed so the app grid rescans. */
    var rescanTick by mutableIntStateOf(0)
        private set

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pkg = intent.data?.schemeSpecificPart
            // Removals always rescan (the app may be on the grid). For installs and
            // changes, skip the full scan unless the package is actually launchable.
            val launchable = pkg != null && (
                packageManager.getLeanbackLaunchIntentForPackage(pkg) != null ||
                    packageManager.getLaunchIntentForPackage(pkg) != null
                )
            if (intent.action == Intent.ACTION_PACKAGE_REMOVED || launchable) {
                rescanTick++
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Experience.load(this)
        HomeMusic.load(this)
        androidx.core.content.ContextCompat.registerReceiver(this, screenReceiver,
            IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_SCREEN_OFF) }, androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
        inputManager?.registerCallback(inputCallback, Handler(Looper.getMainLooper()))

        // A home screen never exits on Back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no-op */ }
        })

        androidx.core.content.ContextCompat.registerReceiver(
            this,
            packageReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            },
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        lifecycleScope.launch {
            com.conreo.couchytv.data.ConfigStore(this@MainActivity).flow.first()
            initialized = true
            sessionWallpaper = WallpaperCycle.current(this@MainActivity)
            setTvContent {
                androidx.compose.runtime.LaunchedEffect(sessionWallpaper) { HomeMusic.setWallpaper(sessionWallpaper) }
                LauncherApp(rescanTick, homeTick, inputTick, sessionWallpaper) {
                    val next = WallpaperCycle.next(this@MainActivity)
                    if (next == sessionWallpaper) HomeMusic.next()
                    sessionWallpaper = next
                }
            }
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))
                HomeMusic.enterHome(this@MainActivity, sessionWallpaper)
            openStudioIfRequested(intent)
        }
    }

    override fun onDestroy() {
        if (!Experience.gameActive) HomeMusic.leaveHome()
        inputManager?.unregisterCallback(inputCallback)
        unregisterReceiver(screenReceiver)
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        Experience.gameActive = false
        Experience.systemPanel = false
        Experience.touch()
        if (initialized) {
            sessionWallpaper = WallpaperCycle.current(this)
            HomeMusic.enterHome(this, sessionWallpaper)
        }
        inputTick++
    }

    override fun onPause() {
        backHandler.removeCallbacks(returnToApp)
        backHeld = false
        if (!Experience.systemPanel && !Experience.gameActive) HomeMusic.leaveHome()
        super.onPause()
    }

    override fun onStop() {
        if (!Experience.gameActive) HomeMusic.leaveHome()
        Experience.systemPanel = false
        com.conreo.couchytv.ui.clearPosterCache()
        super.onStop()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if(level>=android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)JellyfinLink.clearArtworkCache()
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) com.conreo.couchytv.ui.clearPosterCache()
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND || level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) Premieres.clearCache()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Experience.touch()
        if (intent.getBooleanExtra("home_press", false)) homeTick++
        openStudioIfRequested(intent)
    }

    private fun openStudioIfRequested(intent: Intent) {
        val tab = intent.getStringExtra("studio_tab") ?: return
        if (tab in setOf("Pozadia", "Vzhľad", "Jellyfin", "Fotoalbum", "Hudba", "Návrat", "Diagnostika", "Návod", "Systém"))
            { Experience.homePanel="";startActivity(Intent(this, StudioActivity::class.java).putExtra("tab", tab)) }
    }

    // Android Activity public callback; required for TV remote long presses.
    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && (backHeld || Experience.canReturnToApp)) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                Experience.touch(); backHeld = true; backReturned = false
                backHandler.postDelayed(returnToApp, 650)
            } else if (event.action == KeyEvent.ACTION_DOWN && event.isLongPress && !backReturned) {
                backHandler.removeCallbacks(returnToApp); returnToApp.run()
            } else if (event.action == KeyEvent.ACTION_UP) {
                backHandler.removeCallbacks(returnToApp)
                val shortPress = backHeld && !backReturned && !event.isCanceled
                backHeld = false
                if (shortPress) onBackPressedDispatcher.onBackPressed()
            }
            return true
        }
        val navigation = event.keyCode in setOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_MENU)
        if (event.keyCode == swallowedKey) {
            if (event.action == KeyEvent.ACTION_UP) swallowedKey = -1
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            val wasHidden = Experience.artOnly
            Experience.touch()
            if (wasHidden && navigation) { swallowedKey = event.keyCode; return true }
        }
        return super.dispatchKeyEvent(event)
    }
}
