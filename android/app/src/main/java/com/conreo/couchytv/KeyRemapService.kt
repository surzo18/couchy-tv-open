package com.conreo.couchytv

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/** Short Home opens Couchy; holding Home opens the original Google TV launcher. */
class KeyRemapService : AccessibilityService() {
    companion object {
        private var launchedHome = false
        private var connectedService: KeyRemapService? = null
        fun openTvPanel(): Boolean = connectedService
            ?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) == true
    }
    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            if (intent.action == android.content.Intent.ACTION_SCREEN_ON) {
                WallpaperCycle.screenOn(context)
                HomeMusic.wake(context)
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        androidx.core.content.ContextCompat.registerReceiver(this, screenReceiver,
            android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_ON),
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
    }
    private val handler = Handler(Looper.getMainLooper())
    private var pressed = false
    private var longHandled = false
    private var stockHome:android.content.ComponentName?=null
    private val longPress = Runnable { openOriginal() }

    private fun openOriginal() {
        if (!pressed || longHandled) return
        longHandled = true
        android.util.Log.i("CouchyHome", "long Home")
        if (HomeControls.longPressOriginal(this)) Actions.openOriginalHome(this) else Actions.openCustomHome(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        connectedService = this
        stockHome=TvCompatibility.stockHome(this)?.component
        android.util.Log.i("CouchyHome", "Home service connected")
        // Rebinding accessibility (including UI automation) must not steal focus
        // or jump out of another app. Only the first connection after process start.
        if (!launchedHome) {
            launchedHome = true
            Actions.openCustomHome(this)
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    handler.removeCallbacks(longPress)
                    pressed = true
                    longHandled = false
                    handler.postDelayed(longPress, 650)
                } else if (event.isLongPress) {
                    openOriginal()
                }
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(longPress)
                if (pressed && !longHandled && !event.isCanceled) {
                    android.util.Log.i("CouchyHome", "short Home")
                    Actions.openCustomHome(this)
                }
                pressed = false
            }
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            event.packageName?.toString()?.let { LastApp.observe(this, it) }
        // TCL can explicitly open its stock launcher at boot. Keep that fallback
        // enabled, while respecting a deliberate visit via long Home or our button.
        if (!Actions.showingOriginalHome &&
            stockHome!=null && event?.packageName?.toString() == stockHome?.packageName &&
            event?.className?.toString() == stockHome?.className) {
            Actions.openCustomHome(this)
        }
    }
    override fun onInterrupt() {
        handler.removeCallbacks(longPress)
        pressed = false
    }
    override fun onDestroy() {
        if (connectedService === this) connectedService = null
        handler.removeCallbacksAndMessages(null)
        unregisterReceiver(screenReceiver)
        super.onDestroy()
    }
}
