package com.conreo.couchytv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

object Actions {
    var showingOriginalHome = false
        private set
    fun openCustomHome(context: Context) {
        showingOriginalHome = false
        context.startActivity(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("home_press", true))
    }

    fun openOriginalHome(context: Context) {
        val home = TvCompatibility.stockHome(context)
        if (home == null) { toast(context, "Pôvodná plocha nie je dostupná."); return }
        runCatching { context.startActivity(home) }
            .onSuccess { showingOriginalHome = true }
            .onFailure { showingOriginalHome = false; openSystemSettings(context) }
    }

    fun toast(context: Context, msg: String) {
        Toast.makeText(context, AppLanguage.text(msg), Toast.LENGTH_SHORT).show()
    }

    fun launchApp(context: Context, pkg: String) {
        if (com.conreo.couchytv.data.Inputs.isInput(pkg)) {
            launchInput(context, com.conreo.couchytv.data.Inputs.idOf(pkg))
            return
        }
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg)
            ?: pm.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            runCatching { context.startActivity(intent) }
                .onSuccess { LastApp.observe(context, pkg) }
                .onFailure { toast(context, context.getString(R.string.toast_cannot_open)) }
        } else toast(context, context.getString(R.string.toast_no_launchable))
    }

    /** Switches the TV to a passthrough input (HDMI, AV...). */
    fun launchInput(context: Context, inputId: String) {
        val uri = android.media.tv.TvContract.buildChannelUriForPassthroughInput(inputId)
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { toast(context, context.getString(R.string.toast_cannot_open)) }
    }

    fun openAppInfo(context: Context, pkg: String) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$pkg")
                )
            )
        }
    }

    fun uninstall(context: Context, pkg: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
        }
    }

    /** Best-effort: clears the app from cached memory. True force-stop is in App info. */
    fun close(context: Context, pkg: String) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        runCatching { am.killBackgroundProcesses(pkg) }
    }

    fun openSystemSettings(context: Context) {
        runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { toast(context, "Systémové nastavenia nie sú dostupné.") }
    }

    fun openPictureSettings(context: Context) {
        Experience.systemPanel = true
        runCatching { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { openSystemSettings(context) }
    }

    fun openSoundSettings(context: Context) {
        Experience.systemPanel = true
        runCatching { context.startActivity(Intent("com.android.tv.action.VIEW_SOUND_SETTINGS")
            .setPackage("com.android.tv.settings").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { openSystemSettings(context) }
    }

    fun openTvQuickSettings(context: Context) {
        Experience.systemPanel = true
        // Google TV's DashboardHandler requires a system-only permission.
        // The enabled accessibility service exposes the supported system action.
        if (!KeyRemapService.openTvPanel()) openSystemSettings(context)
    }

    fun openNetworkSettings(context: Context) {
        runCatching { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
            .onFailure { openSystemSettings(context) }
    }

    fun openVpnSettings(context: Context) {
        runCatching { context.startActivity(Intent("android.settings.VPN_SETTINGS")) }
            .onFailure { openSystemSettings(context) }
    }

    /** AerialViews screensaver (github.com/theothernt/AerialViews) */
    const val AERIAL_PKG = "com.neilturner.aerialviews"

    fun isInstalled(context: Context, pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess

    fun openAppStore(context: Context, pkg: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
        }.onFailure {
            toast(context, context.getString(R.string.toast_no_store))
        }
    }

}
