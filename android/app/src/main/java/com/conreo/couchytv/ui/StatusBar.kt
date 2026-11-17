package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.conreo.couchytv.R
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import com.conreo.couchytv.data.NetStatus

@Composable
fun StatusBar(
    net: NetStatus,
    time: String,
    date: String,
    showClock: Boolean,
    showInputs: Boolean,
    showStore: Boolean,
    showSleepTimer: Boolean,
    showVpn: Boolean,
    glass: Boolean,
    onInputsClick: () -> Unit,
    onStoreClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onVpnClick: () -> Unit,
    onNetworkClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOriginalClick: () -> Unit,
    onWallpaperClick: (() -> Unit)?,
    onShuffleClick: () -> Unit,
    onPictureClick: () -> Unit,
    onSoundClick: () -> Unit,
    onTvSettingsClick: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    var clusterWidth by remember { mutableStateOf(520.dp) }
    // Optional glass panel behind the icon cluster — same look as the dock
    // panel, corners follow the roundness setting.
    val cluster = if (glass) {
        Modifier
            .background(Color(0x99121418), SmoothCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    } else Modifier
    Box {
    Column {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp).padding(top = 18.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
            border = tvFocusBorder(SmoothCornerShape(16.dp)),
          onClick = onOriginalClick,
          modifier=Modifier.focusHint("Pôvodná domovská obrazovka"),
          shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(16.dp)),
          colors = tvFocusColors(Color(0x99121418)),
      ) {
          Text("TV Home", modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = Color.White)
      }
      if (onWallpaperClick != null) {
          Surface(
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
            border = tvFocusBorder(SmoothCornerShape(16.dp)),onClick = onWallpaperClick,
              onLongClick = { context.startActivity(android.content.Intent(context, com.conreo.couchytv.StudioActivity::class.java)) },
              modifier = Modifier.padding(start = 10.dp).focusHint("Pozadia · OK: knižnica · podrž OK: nastavenia plochy"),
              shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(16.dp)),
              colors = tvFocusColors(Color(0x99121418)),
          ) { Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                  Icon(AppIcons.Image, "Pozadia", tint = Color.White, modifier = Modifier.size(22.dp))
              } }
      }
      if (com.conreo.couchytv.Experience.enabled) {
          Surface(
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
            border = tvFocusBorder(SmoothCornerShape(16.dp)),onClick = onShuffleClick, modifier = Modifier.padding(start = 10.dp).focusHint("Náhodné pozadie a hudba"),
              shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(16.dp)),
              colors = tvFocusColors(Color(0x99121418))) {
              Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                  Icon(AppIcons.Shuffle, "Zmeniť pozadie a hudbu", tint = Color.White, modifier = Modifier.size(22.dp))
              }
          }
          Surface(
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
            border = tvFocusBorder(SmoothCornerShape(16.dp)),onClick = {
              com.conreo.couchytv.HomeMusic.toggle(context)
          }, onLongClick = {
              com.conreo.couchytv.Experience.homePanel = if(com.conreo.couchytv.ExternalMedia.current!=null)"externalMusic" else "music"
          }, modifier = Modifier.padding(start = 10.dp).focusHint("Hudba · OK: zapnúť/vypnúť · podrž OK: ovládanie"),
              shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(16.dp)),
              colors = tvFocusColors(Color(0x99121418))) {
              Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                  Icon(if (com.conreo.couchytv.HomeMusic.enabled) AppIcons.Music else AppIcons.MusicOff,
                      if (com.conreo.couchytv.HomeMusic.enabled) "Hudba zapnutá · podrž OK pre ovládanie" else "Hudba vypnutá · podrž OK pre ovládanie",
                      tint = if (com.conreo.couchytv.HomeMusic.enabled) MaterialTheme.colorScheme.primary else Color(0xFF99A2AF),
                      modifier = Modifier.size(22.dp))
              }
          }
      }
      TopPanelIcon(AppIcons.Movie, "Novinky · filmy a seriály") { com.conreo.couchytv.Experience.homePanel = "premieres" }
      TopPanelIcon(AppIcons.News, "Správy") { com.conreo.couchytv.Experience.homePanel = "news" }
      TopPanelIcon(AppIcons.Game, "Minihry · OK: posledná hra · podrž OK: výber hry",longClick={com.conreo.couchytv.Experience.homePanel="games"}) {
          com.conreo.couchytv.GameHub.launch(context)
      }
      Spacer(Modifier.weight(1f))
      Row(
        modifier = Modifier.onSizeChanged { clusterWidth = with(density) { it.width.toDp() } }.then(cluster),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Order: inputs · sleep timer · store · VPN · network · settings · date · clock
        if (showInputs) {
            StatusIcon(
                icon = AppIcons.Hdmi,
                active = true,
                contentDescription = stringResource(R.string.cd_inputs),
                onClick = onInputsClick,
            )
        }
        if (showSleepTimer) {
            StatusIcon(
                icon = AppIcons.Timer,
                active = true,
                contentDescription = stringResource(R.string.cd_sleep_timer),
                onClick = onSleepTimerClick,
            )
        }
        if (showStore) {
            StatusIcon(
                icon = AppIcons.Store,
                active = true,
                contentDescription = stringResource(R.string.cd_store),
                onClick = onStoreClick,
            )
        }
        if (showVpn) {
            StatusIcon(
                icon = AppIcons.Vpn,
                active = net.vpn,
                contentDescription = stringResource(R.string.cd_vpn),
                onClick = onVpnClick,
            )
        }
        NetworkIcon(net, onNetworkClick)
        StatusText("Obraz", onPictureClick)
        StatusText("Zvuk", onSoundClick)
        StatusIcon(AppIcons.Gear,true,"Nastavenia TV",onTvSettingsClick)
        StatusIcon(
            icon = AppIcons.Pencil,
            active = true,
            contentDescription = "Nastavenia plochy",
            onClick = onSettingsClick,
        )
        if (showClock && date.isNotEmpty()) {
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        if (showClock) Text(
            // Tabular figures: digits keep a fixed width so the clock never jitters.
            text = time,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = Color.White,
            modifier = Modifier.padding(start = 10.dp),
        )
      }
    }
    NewsStrip(clusterWidth)
    }
    }
    HomePanels()
}

@Composable
private fun TopPanelIcon(icon: ImageVector, label: String, longClick:(()->Unit)?=null, click: () -> Unit) {
    Surface(
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
            border = tvFocusBorder(SmoothCornerShape(16.dp)),onClick = click,onLongClick=longClick,modifier = Modifier.padding(start = 10.dp).focusHint(label),
        shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(16.dp)),
        colors = tvFocusColors(Color(0x99121418))) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun StatusText(label: String, onClick: () -> Unit) {
    Surface(
        modifier=Modifier.focusHint("$label · rýchle nastavenia televízora",right=true),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
            border = tvFocusBorder(SmoothCornerShape(16.dp)),onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(16.dp)),
        colors = tvFocusColors(Color.Transparent),
    ) { Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) }
}

@Composable
private fun NetworkIcon(net: NetStatus, onClick: () -> Unit) {
    val green = Color(0xFF56D98A)
    val muted = Color(0xFF647080)
    val label = when {
        net.ethernet -> "Pripojené káblom"
        net.wifi -> (net.ssid?.let{"Wi-Fi · $it"} ?: "Wi-Fi pripojená") + (net.wifiBars?.let { ", signál $it zo 4" } ?: "")
        else -> "Bez pripojenia"
    }
    Surface(
            border = tvFocusBorder(SmoothCornerShape(16.dp)),onClick = onClick,modifier=Modifier.focusHint("$label · OK otvorí nastavenia siete",right=true),shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(16.dp)),
        colors = tvFocusColors(Color.Transparent),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f)) {
        Box(Modifier.size(40.dp).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
            if (net.wifi && !net.ethernet && net.wifiBars != null) {
                Canvas(Modifier.size(24.dp)) {
                    val unit = size.width / 24f
                    val center = Offset(12f * unit, 20f * unit)
                    drawCircle(green, 1.8f * unit, center)
                    listOf(6f, 11f, 16f).forEachIndexed { index, radius ->
                        val r = radius * unit
                        drawArc(if (net.wifiBars >= index + 2) green else muted, 225f, 90f, false,
                            Offset(center.x - r, center.y - r), Size(r * 2, r * 2), style = Stroke(2.1f * unit, cap = StrokeCap.Round))
                    }
                }
            } else Icon(if (net.ethernet) AppIcons.Ethernet else if (net.wifi) AppIcons.Wifi else AppIcons.WifiOff,
                null, tint = if (net.connected) green else muted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun StatusIcon(
    icon: ImageVector,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier=Modifier.focusHint(contentDescription,right=true),
            border = tvFocusBorder(SmoothCornerShape(16.dp)),
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(16.dp)),
        colors = tvFocusColors(Color.Transparent),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = if (active) 0.95f else 0.35f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
