package com.conreo.couchytv.ui

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.conreo.couchytv.*
import com.conreo.couchytv.data.ConfigStore
import kotlinx.coroutines.launch
import com.conreo.couchytv.ui.LocalizedText as Text

/** Resumable setup. No optional settings are changed merely by visiting a page. */
@Composable
fun SetupWizard(onDone:()->Unit,onVpnChosen:(String?)->Unit) {
    val c=LocalContext.current
    val activity=c as? Activity
    val saved=remember{c.getSharedPreferences("onboarding",0)}
    var step by remember{mutableIntStateOf(saved.getInt("step",0).coerceIn(0,7))}
    var revision by remember{mutableIntStateOf(0)}
    val scope=rememberCoroutineScope()
    val store=remember{ConfigStore(c)}
    val config by store.flow.collectAsState(initial=null)
    fun tr(sk:String,en:String)=if(AppLanguage.code=="en")en else sk
    val titles=listOf(tr("Vitaj v Couchy","Welcome to Couchy"),tr("Ovládanie TV","TV controls"),tr("Pozadia a hudba","Wallpapers and music"),tr("Tvoje správy","Your news"),tr("Počasie","Weather"),"Jellyfin",tr("Fotografie a spomienky","Photos and memories"),tr("Všetko je pripravené","Ready to start"))
    val system=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){revision++}
    val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner){val observer=androidx.lifecycle.LifecycleEventObserver{_,e->if(e==androidx.lifecycle.Lifecycle.Event.ON_RESUME)revision++};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer)}}
    LaunchedEffect(Unit){Weather.load(c)}
    LaunchedEffect(step){saved.edit().putInt("step",step).apply()}
    BackHandler{if(step>0)step--}
    fun studio(tab:String){c.startActivity(Intent(c,StudioActivity::class.java).putExtra("tab",tab))}
    Box(Modifier.fillMaxSize().background(WALLPAPERS[0].brush())) {
        Column(Modifier.fillMaxSize().padding(horizontal=48.dp,vertical=28.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Couchy TV",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.weight(1f));Text("${step+1} / 8",color=Color(0xFFBACBDC))}
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF29394A))){Box(Modifier.fillMaxWidth((step+1)/8f).height(3.dp).background(MaterialTheme.colorScheme.primary))}
            Text(titles[step],style=MaterialTheme.typography.headlineMedium)
            key(step) {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    when(step) {
                        0->{
                            Text(tr("Nastavíme plochu pre tvoju TV. Šípkami vyberáš, OK potvrdzuje a Späť sa vracia o krok. Voliteľné kroky môžeš preskočiť tlačidlom Pokračovať.","Let's set up your TV home screen. Use arrows to navigate, OK to select and Back for the previous step. Use Continue to skip optional steps."))
                            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                PanelButton((if(AppLanguage.code=="sk")"✓ " else "")+"Slovenčina"){AppLanguage.set(c,"sk");activity?.recreate()}
                                PanelButton((if(AppLanguage.code=="en")"✓ " else "")+"English"){AppLanguage.set(c,"en");activity?.recreate()}
                            }
                            Text(tr("Povolenia vyžiadame až pri zapnutí konkrétnej funkcie. Osobné fotografie ani prihlásenia nie sú súčasťou balíka.","Permissions are requested only for features you choose. Personal photos and signed-in accounts are not included in the package."),color=Color(0xFFBACBDC))
                        }
                        1->{
                            val enabled=remember(revision){HomeControls.enabled(c)}
                            val default=remember(revision){defaultHomePackage(c)==c.packageName}
                            Text(tr("Krátke Home otvorí Couchy; podržanie môže otvoriť pôvodnú plochu. Niektorí výrobcovia skratku blokujú. Pôvodnú plochu ponechaj zapnutú.","Short Home opens Couchy; holding it can open the original launcher. Some manufacturers block remapping. Keep the original launcher enabled."))
                            Text(tr("Predvolená plocha: ","Default home: ")+(if(default)"✓ Couchy" else "—"))
                            Text(tr("Skratka Home: ","Home shortcut: ")+(if(enabled)tr("zapnutá","enabled") else tr("vypnutá","disabled")))
                            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                PanelButton(tr("Vybrať domovskú aplikáciu","Choose home app")) {
                                    val manager=c.getSystemService(RoleManager::class.java)
                                    val intent=if(manager?.isRoleAvailable(RoleManager.ROLE_HOME)==true && !manager.isRoleHeld(RoleManager.ROLE_HOME))manager.createRequestRoleIntent(RoleManager.ROLE_HOME) else Intent(Settings.ACTION_HOME_SETTINGS)
                                    runCatching{system.launch(intent)}.onFailure{Actions.openSystemSettings(c)}
                                }
                                PanelButton(tr("Zapnúť skratku Home","Enable Home shortcut")){runCatching{system.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}.onFailure{Actions.openSystemSettings(c)}}
                            }
                        }
                        2->{
                            Text(tr("Pozadia sa môžu pri zapnutí náhodne meniť. K videám si priradíš hudbu a pridáš vlastné súbory. Na slabšej TV môžeš použiť statickú plochu.","Wallpapers can shuffle on wake. Assign music and import your own media. Use a static background on less powerful hardware."))
                            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                PanelButton(tr("Živé pozadie","Live wallpaper")){scope.launch{store.update{it.copy(useVideoWallpaper=true)}}}
                                PanelButton(tr("Statické pozadie","Static background")){scope.launch{store.update{it.copy(useVideoWallpaper=false,useBuiltinAerials=false)}}}
                                PanelButton(tr("Vybrať pozadia","Choose wallpapers")){studio("Pozadia")}
                            }
                            Text(tr("Režim: ","Mode: ")+(if(config?.useVideoWallpaper==true)tr("živé pozadie","live wallpaper") else tr("statické pozadie","static background")))
                            PanelButton(tr("Hudba a vlastné skladby","Music and imported tracks")){studio("Hudba")}
                        }
                        3->{
                            var enabled by remember(revision){mutableStateOf(HomeNews.enabled(c))}
                            Text(tr("Správy sa zobrazujú v páse a v zozname. Zdroje môžeš pridávať cez RSS/Atom, vypínať, mazať a meniť ich farby.","Read news in the ticker or illustrated list. Add RSS/Atom sources, disable or remove them and customize their colors."))
                            PanelButton(if(enabled)tr("Správy: zapnuté","News: on") else tr("Správy: vypnuté","News: off")){enabled=!enabled;HomeNews.setEnabled(c,enabled);NewsUi.enabled=enabled}
                            PanelButton(tr("Vybrať a overiť zdroje","Choose and test sources")){activity?.let{NewsSourceDialogs(it){revision++}.show()}}
                            Text(tr("Počet povolených zdrojov: ","Enabled feeds: ")+NewsSources.all.count{it.enabled})
                        }
                        4->{
                            Text(tr("Vyber mesto. Predpoveď poskytuje Open-Meteo (CC BY 4.0); bezplatné API je určené na nekomerčné používanie. Mesto a IP adresa sa odošlú poskytovateľovi až pri vyhľadaní alebo obnove.","Choose a city. Open-Meteo forecasts use CC BY 4.0; its free API is for non-commercial use. Search or refresh sends the city/location and your IP address to the provider."))
                            PanelButton(if(Weather.enabled)tr("Počasie: zapnuté","Weather: on") else tr("Počasie: vypnuté","Weather: off")){Weather.setEnabled(c,!Weather.enabled)}
                            PanelButton(tr("Nastaviť mesto a jednotky","Set city and units")){Experience.homePanel="weather"}
                            Text(Weather.place?.let{tr("Vybrané mesto: ","Selected city: ")+it.name} ?: tr("Mesto zatiaľ nie je vybrané.","No city selected yet."))
                        }
                        5->{
                            Text(tr("Zadaj adresu svojho Jellyfin servera a pripoj účet cez Quick Connect. Získaš prístup k rozpozeraným titulom, ďalším epizódam a náhodnému výberu.","Enter your Jellyfin server URL and authorize Quick Connect for continue watching, next episodes and random picks."))
                            PanelButton(tr("Pripojiť Jellyfin","Connect Jellyfin")){studio("Jellyfin")}
                            Text(if(JellyfinLink.connected(c))tr("Účet je pripojený.","Account connected.") else tr("Voliteľné — môžeš pripojiť neskôr.","Optional — connect later if you prefer."))
                        }
                        6->{
                            Text(tr("Pridaj fotografie do albumov, vyber ich pre šetrič a nastav občasné spomienky na ploche. Popisy aj názvy albumov sa dajú upraviť.","Import photos into albums, choose screensaver albums and enable occasional home memories. Album names and captions are editable."))
                            PanelButton(tr("Pridať fotografie a albumy","Add photos and albums")){c.startActivity(Intent(c,AlbumActivity::class.java))}
                            PanelButton(tr("Nastaviť spomienky","Configure memories")){c.startActivity(Intent(c,StudioActivity::class.java).putExtra("tab","Fotoalbum").putExtra("memory_settings",true))}
                            Text(tr("Prístup k fotografiám je voliteľný. Ostatné funkcie fungujú aj bez neho.","Photo access is optional. Other features work without it."),color=Color(0xFFBACBDC))
                        }
                        else->{
                            Text(tr("Na ploche podrž OK pre možnosti aplikácie. Podrž Späť pre návrat do poslednej aplikácie. Podrž ikonu hier a vyber si hru.","Hold OK on an app for options. Hold Back on home to return to the last app. Hold the games icon to choose a game."))
                            Text(tr("Ceruzka otvorí nastavenia. Návod vysvetľuje funkcie a Diagnostika pomáha pri problémoch. Sprievodcu nájdeš znovu v Systém.","The pencil opens settings. Help explains features and Diagnostics helps investigate problems. Reopen this wizard in System."))
                            Text(tr("Jazyk: ","Language: ")+AppLanguage.code.uppercase()+" · "+tr("Zdroje správ: ","News feeds: ")+NewsSources.all.count{it.enabled})
                            Text(tr("Dostupnosť HDMI, systémových panelov a prehrávania 4K závisí od konkrétnej TV.","HDMI inputs, system panels and 4K decoding depend on the TV hardware."),color=Color(0xFFBACBDC))
                        }
                    }
                }
            }
            key(step) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                if(step>0)PanelButton(tr("Späť","Back")){step--}
                Spacer(Modifier.weight(1f))
                PanelButton(if(step==7)tr("Otvoriť plochu","Open home") else tr("Pokračovať","Continue"),initial=true) {
                    if(step==7){saved.edit().putInt("step",0).putBoolean("completed",true).apply();onDone()}else step++
                }
            }}
        }
        if(Experience.homePanel=="weather")WeatherPanel()
    }
}
