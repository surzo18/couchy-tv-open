package com.conreo.couchytv

import com.conreo.couchytv.ui.LocalizedText as Text
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import com.conreo.couchytv.ui.setTvContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.conreo.couchytv.data.ConfigStore
import com.conreo.couchytv.ui.tvFocusColors
import com.conreo.couchytv.ui.tvFocusBorder
import com.conreo.couchytv.ui.LiteTvTheme
import kotlinx.coroutines.*

class StudioActivity : LocalizedActivity() {
    override fun onStop(){super.onStop();JellyfinLink.clearArtworkCache()}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Experience.load(this)
        HomeMusic.load(this)
        setTvContent {
            val accent by produceState(Color(0xFF90BDFF)) {
                value = withContext(Dispatchers.IO) { Color(WallpaperAccent.color(this@StudioActivity, WallpaperCycle.current(this@StudioActivity), 0xFF90BDFF.toInt())) }
            }
            LiteTvTheme(accent = accent) { Studio() }
        }
    }
    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable private fun Studio() {
        var tab by remember { mutableStateOf((intent.getStringExtra("tab") ?: "Pozadia").let {
            if (it == "Návrat" && com.conreo.couchytv.data.V2Snapshot.discarded(this@StudioActivity)) "Pozadia" else it
        }) }
        var revision by remember { mutableIntStateOf(0) }
        var message by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var inputNames by remember{mutableStateOf(false)}
        var memorySettings by remember{mutableStateOf(intent.getBooleanExtra("memory_settings",false))}
        if(inputNames)com.conreo.couchytv.ui.InputNamesPanel(this,onClose={inputNames=false})
        if(memorySettings)com.conreo.couchytv.ui.MemorySettingsPanel(this,onClose={memorySettings=false})
        val scope = rememberCoroutineScope()
        val library = remember(revision) { WallpaperLibrary.all(this) }
        val disabled = remember(revision) { WallpaperLibrary.disabled(this) }
        var filter by remember { mutableStateOf("Všetky") }
        val tabFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { withFrameNanos { }; runCatching { tabFocus.requestFocus() } }
        val musicPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
            if(uris.isNotEmpty())scope.launch {
                busy=true;var count=0
                try {withContext(Dispatchers.IO){uris.take(50).forEach{MediaLibraryStore.importSong(this@StudioActivity,it);count++}};HomeMusic.libraryChanged();message=AppLanguage.text("Pridané skladby")+": $count"}
                catch(e:CancellationException){throw e}
                catch(e:Exception){message=AppLanguage.text("Pridané skladby")+": $count. "+e.message.orEmpty()}
                finally{busy=false;revision++}
            }
        }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) scope.launch {
                busy = true
                var count = 0
                try {
                    withContext(Dispatchers.IO) {
                        uris.forEach { uri ->
                            val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                                if (it.moveToFirst()) it.getString(0) else "Video"
                            } ?: "Video"
                            WallpaperLibrary.importVideo(this@StudioActivity, uri, name); count++
                        }
                    }
                    message = "Pridané videá: $count. Nájdeš ich v kategórii Vlastné."
                } catch (e: Exception) { message = "Pridané videá: $count. ${e.message ?: "Video sa nepodarilo pridať."}" }
                finally { busy = false; revision++; Experience.changed() }
            }
        }
        Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF172B3B),Color(0xFF0C1522)))).padding(32.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if(tab=="Jellyfin") "Nastavenia plochy" else "Nastavenia plochy", fontSize = 30.sp, color = Color.White)
                Spacer(Modifier.weight(1f))
                StudioButton("Späť na plochu") { finish() }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Pozadia", "Vzhľad", "Jellyfin", "Fotoalbum", "Hudba", "Diagnostika", "Návod", "Systém").forEach { name ->
                    StudioButton(if (tab == name) "● $name" else name,
                        if (tab == name) Modifier.focusRequester(tabFocus) else Modifier) { tab = name; message = "" }
                }
            }
            if (message.isNotBlank()) Text(message, color = Color(0xFFBDDAFF), modifier = Modifier.padding(bottom = 12.dp))
            if (busy) Text("Pridávam video…", color = Color.White)
            when (tab) {
                "Systém" -> PortableSettings(this@StudioActivity)
                "Pozadia" -> {
                    var randomAtWake by remember { mutableStateOf(WallpaperCycle.randomAtWake(this@StudioActivity)) }
                    var autoRotate by remember { mutableStateOf(WallpaperCycle.autoRotate(this@StudioActivity)) }
                    val displayed = remember(library, filter) { library.filter { filter == "Všetky" || it.collection == filter } }
                    val cardFocus = remember(displayed) { displayed.associate { it.id to FocusRequester() } }
                    val gridState = rememberLazyGridState()
                    Text("Pozadia: ${library.size} · Povolené: ${library.count { it.id !in disabled }} · V kategórii: ${displayed.size}", color = Color(0xFFBBC9DD), fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StudioButton("Náhodné pozadie") { WallpaperCycle.next(this@StudioActivity); finish() }
                        StudioButton(if (autoRotate) "Automatická zmena: každé 2 minúty" else "Automatická zmena: vypnutá") {
                            autoRotate = !autoRotate
                            WallpaperCycle.setAutoRotate(this@StudioActivity, autoRotate)
                        }
                        StudioButton("Pridať video") {
                            if (!busy) runCatching { picker.launch(arrayOf("video/*")) }.onFailure {
                                message = "Výber súborov sa nepodarilo otvoriť."
                            }
                        }
                        StudioButton("Náhodný výber z: ${WallpaperLibrary.collection(this@StudioActivity)}") {
                            val names = WallpaperLibrary.collections
                            val current = names.indexOf(WallpaperLibrary.collection(this@StudioActivity))
                            WallpaperLibrary.setCollection(this@StudioActivity, names[(current + 1) % names.size]); revision++
                        }
                        StudioButton(if (randomAtWake) "Pri zapnutí: náhodné" else "Pri zapnutí: ponechať posledné") {
                            WallpaperCycle.toggleWakeMode(this@StudioActivity)
                            randomAtWake = WallpaperCycle.randomAtWake(this@StudioActivity)
                        }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WallpaperLibrary.collections.forEach { group ->
                            StudioButton(if (filter == group) "● $group" else group) { filter = group }
                        }
                    }
                    // TV's default focus pivot can place this tall card partly below the viewport.
                    // Keep the whole card visible, including its focus border, without recentering it.
                    val focusMargin = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }
                    val cardVisibility = remember(focusMargin) {
                        object : androidx.compose.foundation.gestures.BringIntoViewSpec {
                            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = when {
                                size + 2 * focusMargin >= containerSize -> offset - focusMargin
                                offset < focusMargin -> offset - focusMargin
                                offset + size > containerSize - focusMargin -> offset + size - containerSize + focusMargin
                                else -> 0f
                            }
                        }
                    }
                    CompositionLocalProvider(androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides cardVisibility) {
                    LazyVerticalGrid(columns = GridCells.Fixed(3), state = gridState, modifier = Modifier.weight(1f).padding(top = 8.dp),
                        contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        gridItems(displayed, key = { it.id }) { item ->
                                    val bitmap by produceState<android.graphics.Bitmap?>(null, item.id) {
                                        value = withContext(Dispatchers.IO) { WallpaperLibrary.thumbnail(this@StudioActivity, item) }
                                    }
                                    Surface(
            border = tvFocusBorder(RoundedCornerShape(14.dp)),onClick = {
                                        android.app.AlertDialog.Builder(this@StudioActivity).setTitle(item.title)
                                            .setItems(arrayOf("Použiť pozadie", if (item.id in disabled) "Zaradiť do náhodného výberu" else "Vyradiť z náhodného výberu", "Spravovať pozadie", "Zrušiť").map{AppLanguage.text(it)}.toTypedArray()) { _, action ->
                                                when (action) {
                                                    0 -> { WallpaperCycle.select(this@StudioActivity, item.uri); finish() }
                                                    1 -> {
                                                        if (!WallpaperLibrary.toggle(this@StudioActivity, item)) message = "Ponechaj povolené aspoň jedno pozadie."
                                                        revision++
                                                    }
                                                    2 -> MediaLibraryDialogs(this@StudioActivity) { revision++ }.wallpaper(item)
                                                }
                                            }.show()
                                    }, modifier = Modifier.fillMaxWidth().focusRequester(cardFocus.getValue(item.id)).onPreviewKeyEvent { event ->
                                        val index = displayed.indexOf(item)
                                        val target = when (event.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> if (index / 3 < (displayed.size - 1) / 3) (index + 3).coerceAtMost(displayed.lastIndex) else index
                                            android.view.KeyEvent.KEYCODE_DPAD_UP -> if (index >= 3) index - 3 else null
                                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> if (index % 3 > 0) index - 1 else index
                                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> if (index % 3 < 2 && index < displayed.lastIndex) index + 1 else index
                                            else -> null
                                        }
                                        if (target == null) false else {
                                            if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN && target != index) scope.launch {
                                                if (target / 3 != index / 3) gridState.scrollToItem((target / 3) * 3)
                                                withFrameNanos { }
                                                cardFocus.getValue(displayed[target].id).requestFocus()
                                            }
                                            true
                                        }
                                    },
                                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                                        colors = tvFocusColors(Color(0xFF1C293D))) {
                                        Column {
                                            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF111927)), contentAlignment = Alignment.Center) {
                                                bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                                            }
                                            Text(item.title, color = Color.White, maxLines = 1, fontSize = 15.sp, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                            Text(if (item.id in disabled) "Vyradené z náhodného výberu" else "✓ ${item.collection}",
                                                color = Color(0xFFB6CCDF), fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp, bottom = 6.dp))
                                        }
                                    }
                        }
                    }
                    }
                    Text("Náhodne sa striedajú povolené pozadia z vybranej kategórie. Ak v nej nie sú žiadne, použijú sa ostatné povolené pozadia.", color = Color(0xFF93A7BF), fontSize = 12.sp)
                }
                "Vzhľad" -> {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    StudioButton("Názvy HDMI vstupov"){inputNames=true}
                    StudioButton("Počasie"){Experience.homePanel="weather"}
                    StudioButton("Spomienky na ploche"){memorySettings=true}
                    Text("Prispôsob si vzhľad a prvky domovskej plochy.", color = Color.White)
                    if (!Experience.enabled) StudioButton("Zapnúť upravenú plochu") { Experience.configure(this@StudioActivity, active = true) }
                    StudioButton("Jednotné ikony: ${if (Experience.unifiedIcons) "zapnuté" else "vypnuté"}") {
                        Experience.configure(this@StudioActivity, icons = !Experience.unifiedIcons)
                    }
                    StudioButton("Skryť ovládanie: ${if (Experience.idleSeconds == 0) "nikdy" else "po ${Experience.idleSeconds} sekundách"}") {
                        val options = listOf(0, 15, 30, 60)
                        Experience.configure(this@StudioActivity, idle = options[(options.indexOf(Experience.idleSeconds) + 1) % options.size])
                    }
                    Text("Šípkou alebo OK znovu zobrazíš ovládanie. Ďalšie stlačenie už vykoná vybranú akciu. V otvorených ponukách sa ovládanie neskrýva.", color = Color(0xFFBBC9DD),fontSize=13.sp)
                    }
                }
                "Jellyfin" -> JellyfinPanel()
                "Diagnostika" -> DiagnosticsPanel()
                "Návod" -> HelpPanel(Modifier.weight(1f))
                "Fotoalbum" -> {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Text("Fotografie a videá", color = Color.White, fontSize = 23.sp)
                    StudioButton("Spravovať fotografie") { startActivity(Intent(this@StudioActivity, AlbumActivity::class.java)) }
                    Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){
                        StudioButton("Albumy a šetrič"){startActivity(Intent(this@StudioActivity,AlbumActivity::class.java).putExtra("album_settings",true))}
                        StudioButton("Spomienky na ploche"){memorySettings=true}
                    }
                    Text("Vyber albumy pre šetrič a nastav spomienky na ploche. V šetriči prepínaj fotografie šípkami; OK alebo Späť ho zatvorí.", color = Color(0xFFBBC9DD),fontSize=13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StudioButton("Systémový šetrič") { AlbumStore.settings(this@StudioActivity) }
                    }
                    StudioButton("Nastavenia Aerial Views") { Actions.launchApp(this@StudioActivity, "com.neilturner.aerialviews") }
                    StudioButton("Spustiť šetrič fotografií") { startActivity(Intent(this@StudioActivity,AlbumPreviewActivity::class.java)) }
                    }
                }
                "Hudba" -> {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text("Hudba k pozadiu", color = Color.White, fontSize = 23.sp)
                    StudioButton("Pridať skladby"){if(!busy)runCatching{musicPicker.launch(arrayOf("audio/*"))}.onFailure{message="Výber súborov sa nepodarilo otvoriť."}}
                    val songs = remember(revision) { MediaLibraryStore.songs(this@StudioActivity) }
                    Spacer(Modifier.height(12.dp))
                    StudioButton("Správa skladieb · ${songs.size} skladieb · ${songs.count { it.enabled }} povolených") {
                        MediaLibraryDialogs(this@StudioActivity) { revision++ }.songs()
                    }
                    Text("Najprv sa v náhodnom poradí prehrajú skladby priradené k pozadiu. Potom pokračuje náhodná hudba z knižnice. Rovnaká skladba nezaznie dvakrát za sebou.",
                        color = Color(0xFFBBC9DD), modifier = Modifier.padding(vertical = 20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StudioButton(if (HomeMusic.enabled) "Hudba: zapnutá" else "Hudba: vypnutá") { HomeMusic.toggle(this@StudioActivity) }
                        StudioButton("Hlasitosť hudby: ${(HomeMusic.volume * 100).toInt()} %") { HomeMusic.changeVolume() }
                    }
                    Spacer(Modifier.height(16.dp))
                    StudioButton(if (HomeMusic.randomFallback) "Bez priradenej hudby: náhodná skladba" else "Bez priradenej hudby: ticho") { HomeMusic.toggleFallback() }
                    Spacer(Modifier.height(16.dp))
                    StudioButton("Ďalšia skladba a späť na plochu") {
                        HomeMusic.next(); Actions.openCustomHome(this@StudioActivity); finish()
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StudioButton(if (HomeMusic.startupEnabled) "Uvítací zvuk: ${EditionConfig.startupTitle}" else "Uvítací zvuk: vypnutý") { HomeMusic.toggleStartup() }
                        StudioButton("Vyskúšať uvítací zvuk") {
                            HomeMusic.previewStartup(); Actions.openCustomHome(this@StudioActivity); finish()
                        }
                    }
                    Text("Vybraná skladba: ${HomeMusic.title.ifBlank { "vyberie sa po návrate na plochu" }}",
                        color = Color.White, modifier = Modifier.padding(top = 20.dp))
                    Text("Na ikone hudby stlač OK pre zapnutie alebo vypnutie. Podrž OK pre hudobný panel. Hudba plochy sa zastaví po otvorení inej aplikácie, HDMI vstupu alebo šetriča a pri uspaní TV. Prehrávanie z inej aplikácie má prednosť.",
                        color = Color(0xFF93A7BF), modifier = Modifier.padding(top = 16.dp))
                    }
                }
                "Návrat" -> {
                    Text("Obnoviť uložený vzhľad", color = Color.White, fontSize = 23.sp)
                    Text("Obnoví uložené rozloženie, poradie aplikácií a vzhľad. Vypne jednotné ikony a automatické skrývanie ovládania. Fotografie ani aplikácie sa neodstránia.", color = Color(0xFFBBC9DD), modifier = Modifier.padding(vertical = 20.dp))
                    StudioButton("Obnoviť uložený vzhľad") {
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { ConfigStore(this@StudioActivity).restoreV2() } }
                                .onSuccess { WallpaperCycle.reset(this@StudioActivity); Actions.openCustomHome(this@StudioActivity); finish() }
                                .onFailure { message = "Obnova sa nepodarila: ${it.message}" }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        com.conreo.couchytv.ui.HomePanels()
    }

    @Composable private fun JellyfinPanel() {
        val scope = rememberCoroutineScope()
        var address by remember { mutableStateOf(JellyfinLink.server(this)) }
        var connected by remember { mutableStateOf(JellyfinLink.connected(this)) }
        var reconnect by remember{mutableStateOf(false)}
        var randomSelection by remember{mutableStateOf(intent.getStringExtra("jellyfin_mode")=="random")}
        val connectionLifecycle by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
        var code by remember { mutableStateOf<QuickCode?>(null) }
        var error by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var items by remember { mutableStateOf<List<ResumeItem>>(emptyList()) }
        var loaded by remember { mutableStateOf(false) }
        var refresh by remember { mutableIntStateOf(0) }
        var nextUp by remember { mutableStateOf(intent.getStringExtra("jellyfin_mode") == "next") }
        val firstItemFocus = remember { FocusRequester() }
        LaunchedEffect(loaded, items) {
            if (loaded && items.isNotEmpty()) {
                withFrameNanos { }; withFrameNanos { }
                runCatching { firstItemFocus.requestFocus() }
            }
        }
        LaunchedEffect(connected, refresh, nextUp,reconnect,randomSelection) {
            if (connected && !reconnect && !randomSelection) {
                loaded = false; items = emptyList(); error = ""
                try { items = withContext(Dispatchers.IO) {
                    if (nextUp) JellyfinLink.nextUp(this@StudioActivity) else JellyfinLink.resume(this@StudioActivity)
                }; error = "" }
                catch (cancel: CancellationException) { throw cancel }
                catch (e: Exception) { error = e.message ?: "Server nie je dostupný.";DiagnosticLog.report("Jellyfin","CHYBA","Prehľad sa nepodarilo obnoviť.",e);if(e is JellyfinAuthExpired)reconnect=true }
                loaded = true
            }
        }
        LaunchedEffect(code,connectionLifecycle) {
            val pending = code ?: return@LaunchedEffect
            if(connectionLifecycle!=androidx.lifecycle.Lifecycle.State.RESUMED)return@LaunchedEffect
            try {
                while(android.os.SystemClock.elapsedRealtime()<pending.expiresAt) {
                    delay(3000)
                    if (withContext(Dispatchers.IO) { JellyfinLink.finishIfReady(this@StudioActivity, pending) }) {
                        connected = true; reconnect=false;code = null;refresh++; return@LaunchedEffect
                    }
                }
                error = "Kód vypršal. Vytvor nový."; code = null
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { error = e.message ?: "Spojenie sa nepodarilo."; code = null }
        }
        if (!connected || reconnect) {
          Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text(if(reconnect)"Znovu pripojiť účet" else "Pripojiť Jellyfin k ploche", color = Color.White, fontSize = 22.sp)
            Text("Prihlás sa k rovnakému serveru a účtu ako v aplikácii Jellyfin. Prehľad na ploche potrebuje vlastné pripojenie. Použi HTTPS; pri HTTP sa prihlásenie prenáša nešifrovane, preto ho používaj iba v dôveryhodnej lokálnej sieti.", color = Color(0xFFBBC9DD), modifier = Modifier.padding(top = 8.dp))
            Text(address, color = Color(0xFFBBC9DD), modifier = Modifier.padding(vertical = 16.dp))
            if(!reconnect)StudioButton("Zmeniť adresu servera") {
                val field = android.widget.EditText(this@StudioActivity).apply { setText(address); setSingleLine(true) }
                android.app.AlertDialog.Builder(this@StudioActivity).setTitle(AppLanguage.text("Adresa servera Jellyfin"))
                    .setView(field).setPositiveButton(AppLanguage.text("Použiť")) { _, _ -> address = field.text.toString().trim() }
                    .setNegativeButton(AppLanguage.text("Zrušiť"), null).show()
            }
            StudioButton(if (busy) "Pripájam…" else if(code!=null)"Vytvoriť nový kód" else "Vytvoriť prihlasovací kód") {
                if (!busy) scope.launch {
                    busy = true; error = ""; code = null
                    try { code = withContext(Dispatchers.IO) { JellyfinLink.saveServer(this@StudioActivity, address); JellyfinLink.begin(this@StudioActivity) } }
                    catch(e:CancellationException){throw e}
                    catch (e: Exception) { error = e.message ?: "Server nie je dostupný.";DiagnosticLog.report("Jellyfin","CHYBA","Vytvorenie Quick Connect kódu zlyhalo.",e) }
                    finally { busy = false }
                }
            }
            code?.let {
                Text(it.code, color = Color(0xFF99C7FF), fontSize = 48.sp, modifier = Modifier.padding(vertical = 20.dp))
                Text("V prihlásenom Jellyfine na mobile alebo PC otvor profil → Quick Connect a zadaj tento kód.\nPouži rovnaký profil ako v aplikácii Jellyfin na TV.", color = Color.White)
            }
            if(reconnect){
                Text("Doterajší účet sa nahradí až po potvrdení nového kódu. Prihlásenie v aplikácii Jellyfin sa nezmení.",color=Color(0xFFBACDDE),fontSize=13.sp)
                StudioButton("Zrušiť nové pripojenie"){code=null;reconnect=false;error="";connected=JellyfinLink.connected(this@StudioActivity)}
            }
            if(error.isNotEmpty())Text(error,color=Color(0xFFFFC6BC),fontSize=14.sp)
          }
        } else if(randomSelection) {
            com.conreo.couchytv.ui.JellyfinRandom(onClose={randomSelection=false},onReconnect={randomSelection=false;reconnect=true})
        } else {
            com.conreo.couchytv.ui.JellyfinShelf(items,loaded,nextUp,error,firstItemFocus,
                onMode={nextUp=it},onRefresh={refresh++},onRandom={randomSelection=true},onManage={
                    android.app.AlertDialog.Builder(this@StudioActivity)
                        .setTitle(AppLanguage.text("Jellyfin · ${JellyfinLink.user(this@StudioActivity)}"))
                        .setMessage(AppLanguage.text("Tento účet používa prehľad Jellyfin na ploche. Jeho odpojenie nezmení prihlásenie v aplikácii Jellyfin."))
                        .setNegativeButton(AppLanguage.text("Zavrieť"),null)
                        .setPositiveButton(AppLanguage.text("Znovu pripojiť účet")){_,_->code=null;error="";reconnect=true}
                        .setNeutralButton(AppLanguage.text("Odpojiť účet")){_,_->
                            android.app.AlertDialog.Builder(this@StudioActivity).setTitle(AppLanguage.text("Odpojiť Jellyfin od plochy?"))
                                .setMessage(AppLanguage.text("Pre opätovné pripojenie bude potrebný nový prihlasovací kód."))
                                .setNegativeButton(AppLanguage.text("Zrušiť"),null)
                                .setPositiveButton(AppLanguage.text("Odpojiť")){_,_->JellyfinLink.disconnect(this@StudioActivity);connected=false;items=emptyList()}.show()
                        }.show()
                })
        }
    }
}

@Composable fun StudioButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
            border = tvFocusBorder(RoundedCornerShape(10.dp)),onClick = onClick, modifier = modifier, shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = tvFocusColors(Color(0xFF23334A))) {
        Text(label, color = Color.White, fontSize = 15.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp))
    }
}
