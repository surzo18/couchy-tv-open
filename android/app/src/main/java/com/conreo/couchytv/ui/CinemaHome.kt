package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import com.conreo.couchytv.data.AppEntry

/** Artwork stays visible; only the expanded library composes a scrolling grid. */
@Composable
fun CinemaHome(
    modifier: Modifier, favorites: List<AppEntry>, others: List<AppEntry>, inputs: List<AppEntry>,
    accent: Color, movePkg: String?, moveFocus: FocusRequester, expanded: Boolean, homeTick: Int,
    onExpand: (Boolean) -> Unit, onLaunch: (AppEntry) -> Unit, onMenu: (AppEntry) -> Unit,
) {
    val context = LocalContext.current
    val firstFocus = remember { FocusRequester() }
    val moreFocus = remember { FocusRequester() }
    val listFocus = remember { FocusRequester() }
    var wasExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val latestExpanded by rememberUpdatedState(expanded)
    LaunchedEffect(homeTick) {
        withFrameNanos { }
        withFrameNanos { }
        if (!latestExpanded) runCatching { if (favorites.isNotEmpty()) firstFocus.requestFocus() else moreFocus.requestFocus() }
    }
    LaunchedEffect(expanded) {
        if (expanded && movePkg == null) listState.scrollToItem(0)
        withFrameNanos { }
        withFrameNanos { }
        if (expanded && movePkg == null) runCatching { listFocus.requestFocus() }
        else if (!expanded && wasExpanded) runCatching { moreFocus.requestFocus() }
        wasExpanded = expanded
    }
    LaunchedEffect(expanded, movePkg, others) {
        if (expanded && movePkg != null) {
            val index = others.indexOfFirst { it.pkg == movePkg }
            if (index >= 0) {
                listState.scrollToItem(index / 4)
                withFrameNanos { }
                runCatching { moveFocus.requestFocus() }
            }
        }
    }
    Box(modifier) {
        if (expanded) {
            Column(Modifier.fillMaxSize().background(Color(0xEC10131B)).padding(horizontal = 44.dp, vertical = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ďalšie aplikácie", color = Color.White, fontSize = 23.sp)
                    Spacer(Modifier.weight(1f))
                    CinemaButton("Zavrieť", onClick = { onExpand(false) })
                }
                Text(
                    if (movePkg == null) "Podrž OK: obľúbené alebo poradie. Prvé tri ďalšie aplikácie sa zobrazia na ploche."
                    else "Šípkami zmeň poradie · OK alebo Späť dokončí presun",
                    color = Color(0xFFC2CBD8), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp),
                )
                LazyColumn(state = listState, contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                    val rows = others.chunked(4)
                    items(rows.size) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally)) {
                            rows[row].forEachIndexed { col, app ->
                                key(app.pkg) {
                                    AppCard(app, app.pkg == movePkg, false, accent, cardWidth = 150.dp,
                                        modifier = when {
                                            app.pkg == movePkg -> Modifier.focusRequester(moveFocus)
                                            row == 0 && col == 0 -> Modifier.focusRequester(listFocus)
                                            else -> Modifier
                                        },
                                        onLaunch = { onLaunch(app) }, onLongPress = { onMenu(app) })
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 44.dp, vertical = if (com.conreo.couchytv.Experience.enabled) 18.dp else 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                run {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        inputs.forEach { app -> key(app.pkg) { CinemaButton(app.label, onClick = { onLaunch(app) }) } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    favorites.take(4).forEachIndexed { index, app ->
                        key(app.pkg) {
                            AppCard(app, app.pkg == movePkg, false, accent, cardWidth = 150.dp, showLabel = false,
                                modifier = when {
                                    app.pkg == movePkg -> Modifier.focusRequester(moveFocus)
                                    index == 0 -> Modifier.focusRequester(firstFocus)
                                    else -> Modifier
                                },
                                onLaunch = { onLaunch(app) }, onLongPress = { onMenu(app) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
                    others.take(3).forEach { app -> key(app.pkg) {
                        AppCard(app, false, false, accent, cardWidth = 110.dp,
                            onLaunch = { onLaunch(app) }, onLongPress = { onMenu(app) })
                    } }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
            border = tvFocusBorder(SmoothCornerShape(16.dp)),
                            onClick = { onExpand(true) }, modifier = Modifier.width(110.dp).height(110.dp * 9f / 16f).focusRequester(moreFocus),
                            shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(16.dp)),
                            colors = tvFocusColors(Color(0xDD182333)),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
                        ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("•••", fontSize = 30.sp, color = Color.White) } }
                        Text("Zobraziť viac", color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.width(110.dp).height(CardLabelHeight).padding(top = 5.dp))
                    }
                }
                if (com.conreo.couchytv.Experience.enabled && movePkg == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Rozpozerané" to "resume", "Ďalšie epizódy" to "next", "Čo si pozrieť?" to "random").forEach { (label, mode) ->
                            Surface(
            border = tvFocusBorder(SmoothCornerShape(8.dp)),onClick = {
                                context.startActivity(android.content.Intent(context, com.conreo.couchytv.StudioActivity::class.java)
                                    .putExtra("tab", "Jellyfin").putExtra("jellyfin_mode", mode))
                            }, colors = tvFocusColors(Color(0x88152030)),
                                shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(8.dp)),
                                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f)) {
                                Text("$label  ›", fontSize = 12.sp, color = Color(0xFFCCDDF3),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp))
                            }
                        }
                    }
                } else Text(
                    if (movePkg == null) "Podrž OK: upraviť poradie   ·   Podrž Home: Google TV"
                    else "Šípky: poradie   ·   OK alebo Späť: hotovo",
                    fontSize = 11.sp, color = Color(0xFFCCD4DF),
                )
            }
        }
    }
}

@Composable
private fun CinemaButton(label: String, onClick: () -> Unit) {
    Surface(
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
            border = tvFocusBorder(SmoothCornerShape(12.dp)),onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(SmoothCornerShape(12.dp)),
        colors = tvFocusColors(Color(0xCC152030)),
    ) { Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 15.dp, vertical = 7.dp)) }
}
