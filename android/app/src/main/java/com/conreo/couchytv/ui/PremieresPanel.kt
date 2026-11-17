package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.conreo.couchytv.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val posterCache = object : android.util.LruCache<String, android.graphics.Bitmap>(8 * 1024 * 1024) {
    override fun sizeOf(key: String, value: android.graphics.Bitmap) = value.byteCount
}
fun clearPosterCache() { posterCache.evictAll() }

@Composable
private fun Poster(url: String, modifier: Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, url) {
        value = withContext(Dispatchers.IO) {
            posterCache.get(url) ?: runCatching {
                val uri = java.net.URI(url)
                require(uri.scheme == "https" && uri.host in setOf("static.tvmaze.com","www.itafilm.sk","itafilm.sk","www.cine-max.sk","cine-max.sk"))
                val connection = uri.toURL().openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = false; connection.connectTimeout = 6000; connection.readTimeout = 6000
                try {
                    require(connection.responseCode == 200)
                    connection.inputStream.use { stream ->
                        val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                        while (true) { val n = stream.read(buffer); if (n < 0) break; require(out.size() + n < 2_000_000); out.write(buffer, 0, n) }
                        val data = out.toByteArray()
                        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                        require(bounds.outWidth > 0 && bounds.outHeight > 0)
                        var sample = 1
                        while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 768) sample *= 2
                        android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size,
                            android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
                    }
                } finally { connection.disconnect() }
            }.getOrNull()?.also { posterCache.put(url, it) }
        }
    }
    Box(modifier.background(Color(0xFF29313F)), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            ?: Icon(AppIcons.Movie, "Plagát nie je dostupný", Modifier.size(36.dp), tint = Color(0xFF9BA9B8))
    }
}

@Composable
fun PremieresPanel() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Premiere?>(null) }
    var rows by remember { mutableStateOf(emptyList<Premiere>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var retry by remember { mutableIntStateOf(0) }
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    var lastSelected by remember { mutableStateOf("") }
    LaunchedEffect(retry) {
        loading = true; error = ""; rows = emptyList()
        try { rows = Premieres.all { rows = it } }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { error = "Prehľad sa nepodarilo načítať. Skontroluj pripojenie alebo skús neskôr." }
        finally { loading = false }
    }
    val item = selected
    if (item != null) {
        var detail by remember(item) { mutableStateOf(item) }
        LaunchedEffect(item) { detail = Premieres.detail(item) }
        androidx.activity.compose.BackHandler { selected = null }
        PanelShell(detail.title, onBack = { selected = null }) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Poster(detail.image, Modifier.width(140.dp).height(210.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${Premieres.dateLabel(detail.date)} · ${detail.provider}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    if (detail.date <= java.time.LocalDate.now().toString() && detail.rating.isNotBlank())
                        Text(detail.rating, color = Color(0xFFFFD78B), style = MaterialTheme.typography.labelLarge)
                    Text(detail.description.ifBlank { "Podrobnosti nájdeš na stránke zdroja." }, maxLines = if (detail.rating.isNotBlank()) 4 else 5,
                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Text(detail.source, color = Color(0xFFAEB8C7), style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PanelButton(if (detail.trailer.isNotEmpty()) "Trailer na YouTube" else "Vyhľadať trailer", initial = true) {
                    val url = detail.trailer.ifBlank { "https://www.youtube.com/results?search_query=" + android.net.Uri.encode(detail.title.substringBefore(" · séria") + " official trailer") }
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).setPackage("com.google.android.youtube.tv")
                    runCatching { context.startActivity(intent) }.onFailure { openWeb(context, url) }
                }
                PanelButton("Zobraziť zdroj") { openWeb(context, detail.url) }
                PanelButton("Späť na prehľad") { selected = null }
            }
        }
    } else PanelShell("Novinky") {
        PanelButton("Zdroje noviniek") { com.conreo.couchytv.PremiereSources.show(context as android.app.Activity){retry++} }
        if(com.conreo.couchytv.PremiereSources.selected().isEmpty())Text("Žiadny zdroj nie je zapnutý.")
        PanelButton("TVmaze · zdroj a licencia") { openWeb(context, "https://www.tvmaze.com/api#licensing") }
        Text(if (loading && rows.isNotEmpty()) "Načítavam nové série…" else "Najprv nedávne premiéry, potom pripravované tituly. Dostupnosť závisí od vybraných zdrojov.", style = MaterialTheme.typography.labelMedium, color = Color(0xFFAEB8C7))
        if (loading && rows.isEmpty()) Text("Načítavam aktuálny prehľad…", Modifier.padding(vertical = 30.dp))
        if (error.isNotEmpty()) { Text(error); PanelButton("Skúsiť znova") { retry++ } }
        val firstFocus = remember { androidx.compose.ui.focus.FocusRequester() }
        LaunchedEffect(rows.isNotEmpty()) { if (rows.isNotEmpty()) { withFrameNanos { }; runCatching { firstFocus.requestFocus() } } }
        LazyVerticalGrid(GridCells.Fixed(5), Modifier.fillMaxWidth().height(310.dp), state = gridState, horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(12.dp)) {
            items(rows, key = { it.url + it.provider + it.date }) { film ->
                Surface(
            border = tvFocusBorder(RoundedCornerShape(12.dp)),onClick = { lastSelected = film.url + film.date; selected = film },
                    modifier = if (if (lastSelected.isEmpty()) film == rows.firstOrNull() else film.url + film.date == lastSelected) Modifier.focusRequester(firstFocus) else Modifier,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = tvFocusColors(Color(0xFF242C38)),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f)) {
                    Column(Modifier.fillMaxWidth()) {
                        Poster(film.image, Modifier.fillMaxWidth().height(165.dp))
                        Text(film.title, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Text(Premieres.dateLabel(film.date), Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = Color(0xFFEEF2F8))
                        val available = film.date <= java.time.LocalDate.now().toString()
                        val badge = if (film.category == "Kiná na Slovensku") "KINO" else film.provider.substringBefore(" · ")
                        Text(badge, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(if (available) "Po premiére" else "Čoskoro",
                            Modifier.padding(start = 8.dp, bottom = 7.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFFCAD4DF))
                    }
                }
            }
        }
    }
}
