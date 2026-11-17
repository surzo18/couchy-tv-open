package com.conreo.couchytv.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class NetStatus(
    val wifi: Boolean = false,
    val ethernet: Boolean = false,
    val vpn: Boolean = false,
    val wifiBars: Int? = null,
    val ssid: String? = null,
) {
    val connected get() = wifi || ethernet
}

/** Emits the current network status and updates on every connectivity change. */
fun networkStatusFlow(context: Context): Flow<NetStatus> = callbackFlow {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun compute(): NetStatus {
        var wifi = false
        var eth = false
        var vpn = false
        @Suppress("DEPRECATION")
        val networks = runCatching { cm.allNetworks }.getOrNull().orEmpty()
        for (network in networks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) eth = true
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) vpn = true
        }
        val bars = if (wifi) runCatching {
            @Suppress("DEPRECATION")
            val rssi = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo.rssi
            @Suppress("DEPRECATION")
            if (rssi in -126..-1) WifiManager.calculateSignalLevel(rssi, 4) + 1 else null
        }.getOrNull() else null
        val ssid=if(wifi)runCatching {
            @Suppress("DEPRECATION")
            val raw=(context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo.ssid
            raw?.removeSurrounding("\"")?.takeIf{it.isNotBlank() && it!="<unknown ssid>" && it!="0x"}?.take(64)
        }.getOrNull() else null
        return NetStatus(wifi, eth, vpn, bars,ssid)
    }

    trySend(compute())

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(compute()) }
        override fun onLost(network: Network) { trySend(compute()) }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            trySend(compute())
        }
    }
    val request = NetworkRequest.Builder().build()
    cm.registerNetworkCallback(request, callback)
    val rssiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { trySend(compute()) }
    }
    androidx.core.content.ContextCompat.registerReceiver(context, rssiReceiver,
        IntentFilter(WifiManager.RSSI_CHANGED_ACTION), androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
    awaitClose { cm.unregisterNetworkCallback(callback); context.unregisterReceiver(rssiReceiver) }
}
