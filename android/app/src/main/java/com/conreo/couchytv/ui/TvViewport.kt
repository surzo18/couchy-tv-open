package com.conreo.couchytv.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import com.conreo.couchytv.DisplayPolicy
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

fun ComponentActivity.setTvContent(content:@Composable ()->Unit) {
    setContent { TvViewport(content) }
}

/** Compose dialogs create a separate window; explicitly retain the TV design density. */
@Composable
fun TvDialog(onDismissRequest:()->Unit,properties:androidx.compose.ui.window.DialogProperties=androidx.compose.ui.window.DialogProperties(),content:@Composable ()->Unit) {
    val density=LocalDensity.current
    val configuration=LocalConfiguration.current
    androidx.compose.ui.window.Dialog(onDismissRequest,properties) {
        CompositionLocalProvider(LocalDensity provides density,LocalConfiguration provides configuration){content()}
    }
}

/** Keep the established 960x540 TV design usable on 720p, 1080p and UHD displays.
 * No bitmap scaling, polling or changes to video decoder resolution. */
@Composable
fun TvViewport(content:@Composable ()->Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val base=LocalDensity.current
        val config=LocalConfiguration.current
        val scale=DisplayPolicy.fit(maxWidth.value,maxHeight.value)
        val adjusted=remember(config,scale){android.content.res.Configuration(config).apply {
            screenWidthDp=(maxWidth.value/scale).toInt()
            screenHeightDp=(maxHeight.value/scale).toInt()
        }}
        CompositionLocalProvider(LocalDensity provides Density(base.density*scale,base.fontScale),LocalConfiguration provides adjusted) { content() }
    }
}
