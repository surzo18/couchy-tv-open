package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.conreo.couchytv.Weather
import kotlin.math.roundToInt

/** Passive wallpaper overlay: native minute ticks, existing weather cache, no focus targets. */
@Composable
fun BoxScope.IdleInfo() {
    val plate = Modifier.background(Color(0x800C131D), RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp, vertical = 6.dp)
    IdleClock("EEE d. MMMM", Modifier.align(Alignment.BottomStart).padding(26.dp).then(plate))
    Row(Modifier.align(Alignment.BottomEnd).padding(26.dp).then(plate),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        val weather = Weather.data
        if (Weather.enabled && weather != null && System.currentTimeMillis() - weather.fetched in 0..10_800_000L) {
            WeatherIcon(weather.code, weather.daytime, Modifier.size(22.dp))
            Text("${Weather.temperature(weather.temperature,false)}", color = Color.White, fontSize = 15.sp)
            Spacer(Modifier.width(3.dp))
        }
        IdleClock("HH:mm", Modifier)
    }
}

@Composable
private fun IdleClock(pattern: String, modifier: Modifier) {
    AndroidView(factory = { context ->
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(com.conreo.couchytv.AppLanguage.locale())
        android.widget.TextClock(context.createConfigurationContext(config)).apply {
            format24Hour = pattern; format12Hour = pattern
            textSize = 17f
            setTextColor(android.graphics.Color.WHITE)
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            isFocusable = false
        }
    }, modifier = modifier)
}
