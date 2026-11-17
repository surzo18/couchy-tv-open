package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.conreo.couchytv.ui.TvDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.key.*
import androidx.tv.material3.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.conreo.couchytv.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitCancellation

internal object NewsUi {
    var items by mutableStateOf(emptyList<NewsItem>())
    var enabled by mutableStateOf(true)
    var index by mutableIntStateOf(0)
    @Volatile var offset = 0f
    fun replace(fresh: List<NewsItem>) {
        if (fresh == items) return
        val previous = items.getOrNull(index)?.url
        val current = fresh.firstOrNull { it.url == previous }
        items = if (current == null) fresh else listOf(current) + fresh.filter { it.url != previous }
        index = 0
        if (current == null) offset = 0f
    }
    fun advance() {
        index = (index + 1) % items.size
        android.util.Log.d("CouchyNews", "Ticker advanced: $index/${items.size}")
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NewsStrip(width: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle){lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
        ExternalMedia.start(context);try{awaitCancellation()}finally{ExternalMedia.stop()}
    }}
    LaunchedEffect(Unit) { NewsUi.enabled = HomeNews.enabled(context); Weather.load(context) }
    LaunchedEffect(lifecycle,Weather.enabled) {
        if(Weather.enabled)lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while(true){Weather.refresh(context);delay(30*60_000L)}
        }
    }
    LaunchedEffect(lifecycle, NewsUi.enabled, NewsSources.revision) {
        if (NewsUi.enabled) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { NewsUi.replace(HomeNews.load()); delay(if(NewsUi.items.isEmpty())60_000L else NewsSources.refreshMinutes*60_000L) }
        }
    }
    val showNews=NewsUi.enabled && NewsUi.items.isNotEmpty()
    if (!showNews && !Weather.enabled && ExternalMedia.current==null) return
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    val current = if(showNews)NewsUi.items[NewsUi.index % NewsUi.items.size] else null
    val next = if(showNews)NewsUi.items[(NewsUi.index + 1) % NewsUi.items.size] else null
    val active = lifecycleState == Lifecycle.State.RESUMED && !Experience.artOnly && Experience.homePanel.isEmpty()
    Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalArrangement = Arrangement.End) {
      Box(Modifier.weight(1f).padding(end=20.dp)){ExternalMediaBadge(Modifier.fillMaxWidth())}
      Row(Modifier.width(width),horizontalArrangement=Arrangement.spacedBy(8.dp, Alignment.End)) {
        if(Weather.enabled)WeatherBadge()
        if(current!=null && next!=null)Surface(onClick = { Experience.homePanel = "news" },modifier=Modifier.weight(1f),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            border = tvFocusBorder(RoundedCornerShape(10.dp)),
            colors = tvFocusColors(Color(0xD9121418))) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxWidth().height(26.dp).padding(horizontal = 12.dp)
                    .semantics { contentDescription = "Správy: ${current.section}, ${current.title}" },
                factory = { NewsTickerView(it) },
                update = { view -> view.bind(current, next, active, { NewsUi.offset }, { NewsUi.offset = it }) {
                    HomeNews.markSeen(current.url); NewsUi.advance()
                } },
                onRelease = { it.release() },
            )
        }
      }
    }
}

@Composable
fun HomePanels() {
    val lifecycleState by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    if (lifecycleState != Lifecycle.State.RESUMED) return
    val context = LocalContext.current
    when (Experience.homePanel) {
        "weather" -> WeatherPanel()
        "games" -> com.conreo.couchytv.GamePicker()
        "externalMusic" -> ExternalMediaPanel()
        "music" -> PanelShell("Hudba", waitForRelease = true, width = 560.dp) {
            Text(HomeMusic.title.ifBlank { "Hudba k pozadiu" }, style = MaterialTheme.typography.titleLarge, maxLines = 2)
            if (HomeMusic.detail.isNotBlank()) Text(HomeMusic.detail,
                style = MaterialTheme.typography.bodyMedium, maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if(ExternalMedia.current!=null)PanelButton("Prehrávanie · "+ExternalMedia.current!!.app){Experience.homePanel="externalMusic"}
            else if(!ExternalMedia.permitted)PanelButton("Povoliť ovládanie prehrávania") {
                runCatching{context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))}
                    .onFailure{Actions.toast(context,"Nastavenie prístupu nie je v tejto TV dostupné.")}
            }
            Text(if (HomeMusic.status.startsWith("Hrá:")) "Prehrávanie na ploche" else HomeMusic.status,
                color = Color(0xFFAEB8C7), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
            var progress by remember { mutableStateOf(0L to 0L) }
            LaunchedEffect(Unit) { while (true) { progress = HomeMusic.progress(); delay(1000) } }
            val (position, duration) = progress
            if (duration > 0) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = .12f))) {
                    Box(Modifier.fillMaxWidth((position.toFloat() / duration).coerceIn(0f, 1f)).height(3.dp).background(MaterialTheme.colorScheme.primary))
                }
                Text("${position / 60000}:${((position / 1000) % 60).toString().padStart(2, '0')} / ${duration / 60000}:${((duration / 1000) % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.labelMedium)
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PanelButton(if (HomeMusic.enabled) "Vypnúť hudbu" else "Zapnúť hudbu", initial = true) { HomeMusic.toggle(context) }
                PanelButton("Ďalšia skladba") { HomeMusic.next() }
                PanelButton("Hlasitosť ${(HomeMusic.volume * 100).toInt()} %") { HomeMusic.changeVolume() }
            }
            Text("Skladby priradené k pozadiu sa premiešajú; po nich pokračuje náhodný výber.",
                Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFFAEB8C7))
        }
        "news" -> NewsPanel()
        "premieres" -> PremieresPanel()
    }
}

fun openWeb(context: android.content.Context, url: String) {
    com.conreo.couchytv.LastApp.openWeb(context, url)
}

private val LocalPanelExit = staticCompositionLocalOf<FocusRequester?> { null }
@Composable
fun Modifier.panelListNavigation(): Modifier {
    val exit = LocalPanelExit.current ?: return this
    return this.onPreviewKeyEvent { event ->
        if(event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
            runCatching { exit.requestFocus() }; true
        } else false
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PanelShell(title: String, waitForRelease: Boolean = false,
    width: androidx.compose.ui.unit.Dp = 760.dp,
    closeLabel: String = "Zavrieť", onClose: (() -> Unit)? = null,
    onBack: () -> Unit = { Experience.homePanel = ""; Experience.touch() }, content: @Composable ColumnScope.() -> Unit) {
    var released by remember { mutableStateOf(!waitForRelease) }
    val exitFocus = remember { FocusRequester() }
    val margin = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }
    val visibility = remember(margin) {
        object : androidx.compose.foundation.gestures.BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = when {
                size + 2 * margin >= containerSize -> offset - margin
                offset < margin -> offset - margin
                offset + size > containerSize - margin -> offset + size - containerSize + margin
                else -> 0f
            }
        }
    }
    val panelDensity = LocalDensity.current
    Dialog(onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
      CompositionLocalProvider(LocalDensity provides panelDensity, LocalContentColor provides Color.White,
          androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides visibility) {
        val panelAccent=MaterialTheme.colorScheme.primary
        val panelBrush=remember(panelAccent){androidx.compose.ui.graphics.Brush.linearGradient(listOf(
            androidx.compose.ui.graphics.lerp(Color(0xFF172331),panelAccent,.15f),
            Color(0xFF101A27),
            androidx.compose.ui.graphics.lerp(Color(0xFF121C2B),panelAccent,.05f)))}
        Column(Modifier.width(width).heightIn(max = 490.dp).background(panelBrush, RoundedCornerShape(22.dp))
            .padding(24.dp).onPreviewKeyEvent { event ->
                val select = event.key == Key.DirectionCenter || event.key == Key.Enter
                if (!select || released) false else {
                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) { released = true; false }
                    else { if (event.type == KeyEventType.KeyUp) released = true; true }
                }
            }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.width(4.dp).height(25.dp).background(panelAccent,RoundedCornerShape(2.dp)))
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(panelAccent.copy(alpha=.45f),Color.Transparent))))
            CompositionLocalProvider(LocalPanelExit provides exitFocus) {
                content()
                PanelButton(closeLabel,focusOverride=exitFocus) { if (onClose != null) onClose() else { Experience.homePanel = ""; Experience.touch() } }
            }
        }
      }
    }
}

@Composable
fun PanelButton(label: String, initial: Boolean = false, modifier:Modifier=Modifier, focusOverride:FocusRequester?=null, onClick: () -> Unit) {
    val ownFocus = remember { FocusRequester() }
    val focus = focusOverride ?: ownFocus
    Surface(
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
            border = tvFocusBorder(RoundedCornerShape(12.dp)),onClick = onClick, modifier = modifier.focusRequester(focus),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = tvFocusColors(Color(0xFF29313F))) {
        Text(label, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = Color.White, style = MaterialTheme.typography.labelLarge,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
    LaunchedEffect(initial) { if (initial) { withFrameNanos { }; runCatching { focus.requestFocus() } } }
}
