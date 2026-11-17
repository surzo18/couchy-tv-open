package com.conreo.couchytv

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conreo.couchytv.ui.PanelButton

internal data class HelpTopic(val name:String,val intro:String,val example:List<String>,val sections:List<Pair<String,String>>)
private val guide=listOf(
    HelpTopic("Knižnica hudby a pozadí","Prezeraj si skladby a pozadia, upravuj ich údaje a vyberaj, ktoré sa budú prehrávať.",listOf("Hudba → Správa skladieb","Pozadia → náhľad → Spravovať pozadie"),listOf(
        "Skladby" to "V Správe skladieb vidíš celkový počet a počet povolených skladieb. Vyber skladbu a uprav názov, interpreta, pôvod alebo priradenia k pozadiam. Vyradená skladba zostane uložená, ale nebude sa prehrávať.",
        "Pozadia" to "Nad náhľadmi je celkový počet pozadí, počet povolených pozadí a počet v zobrazenej kategórii. Vyber náhľad a Spravovať pozadie pre zmenu názvu, kategórie alebo hudby. Viac priradených skladieb sa prehráva v náhodnom poradí. Ak skladbu nepriradíš, platí nastavenie hudby pre ostatné pozadia.",
        "Trvalé mazanie" to "Trvalé odstránenie vyžaduje potvrdenie a nedá sa vrátiť. Zabudované pozadia nemožno vymazať, iba vyradiť z náhodného výberu. Aspoň jedno pozadie musí zostať povolené.",
        "Ukladanie" to "Úpravy sa zachovajú po reštarte TV aj aktualizácii aplikácie. Vymazaním dát aplikácie sa uložené nastavenia odstránia.")),
    HelpTopic("Ovládač","Najdôležitejšie skratky fungujú priamo na ovládači.",listOf("HOME → plocha","Podrž HOME → Google TV","Podrž SPÄŤ → posledná aplikácia"),listOf(
        "Pohyb a výber" to "Šípky presúvajú zvýraznenie. OK otvorí vybranú položku; Späť zatvorí aktuálny detail alebo ponuku.",
        "Návrat do aplikácie" to "Podrž Späť iba na domovskej ploche bez otvorenej ponuky. Obnoví poslednú dostupnú aplikáciu. Pri článku sa obnoví jeho odkaz; prehliadač nemusí zachovať pozíciu posúvania.",
        "Skryté ovládanie" to "Po nečinnosti sa ovládacie prvky skryjú. Zostane pozadie, dátum, čas a dostupné počasie. Šípkou alebo OK ovládanie znovu zobrazíš. Čas skrytia nastavíš v Nastavenia plochy → Vzhľad.")),
    HelpTopic("Plocha a aplikácie","Obľúbené aplikácie sú veľké dlaždice v spodnej časti.",listOf("YouTube","Jellyfin","Spotify","Voyo"),listOf(
        "Spustenie a ponuka" to "OK spustí aplikáciu. Podrž OK na jej dlaždici pre ďalšie možnosti. Zobraziť viac otvorí zoznam ostatných aplikácií.",
        "Obľúbené" to "Aplikáciu môžeš pridať medzi obľúbené alebo ju odtiaľ odobrať. Po odobraní zostane dostupná vo všetkých aplikáciách.",
        "Nastavenia plochy" to "Ikona ceruzky otvorí nastavenia plochy: pozadia, vzhľad, Jellyfin, fotografie, hudbu, diagnostiku a návod.",
        "Jellyfin na ploche" to "Rozpozerané ponúka nedokončené filmy a epizódy. Ďalšie epizódy ponúka nasledujúce diely podľa histórie sledovania. Prehľad na ploche sa pripája k Jellyfinu samostatne.",
        "Náhodný výber" to "Nastavenia plochy → Jellyfin → Náhodný výber ponúkne 12 titulov. Prepínač Filmy aj seriály / Filmy / Seriály a Celá knižnica / Obľúbené zúži výber; Premiešať vyberie ďalšie tituly. Seriál otvorí série a potom konkrétne epizódy s názvom, náhľadom a dostupným popisom. OK na filme alebo epizóde otvorí titul v TV aplikácii Jellyfin. Na ploche ho otvoríš aj skratkou Čo si pozrieť? v spodnom riadku vedľa Rozpozerané a Ďalšie epizódy.",
        "Popisy a náhľady" to "Vľavo je plagát a celý dostupný popis. Prejdi na rám s popisom a šípkami hore/dole ho posúvaj; na konci prejdeš na tlačidlo otvorenia. Vpravo sa zobrazujú široké obrázky: záber epizódy, pozadie titulu alebo seriálu a nakoniec náhradný plagát. Ak server nemá obrázok ani popis, menu to oznámi. Zoznam epizód je stránkovaný po 30.",
        "Znovu pripojiť Jellyfin" to "Nastavenia plochy → Jellyfin → Účet a pripojenie → Znovu pripojiť účet → Vytvoriť prihlasovací kód. Na rovnakom serveri otvor na mobile alebo počítači profil → Quick Connect a potvrď kód. Ak prihlásenie vyprší, menu ponúkne opätovné pripojenie. Pri výpadku siete zostane účet uložený.")),
    HelpTopic("Pozadia a hudba","K pozadiu môžeš priradiť vlastný výber hudby.",listOf("▧ Pozadia","⇄ Zmeniť","♫ Hudba"),listOf(
        "Automatická zmena" to "Predvolene sa každé dve minúty na filmovej ploche jemne vymení pozadie aj priradená hudba. Beží iba jeden videodekodér. Otvorenie ponuky, inej aplikácie alebo šetriča časovač pozastaví; po návrate začnú nové dve minúty. V Pozadia môžeš automatickú zmenu vypnúť a ponechať vybrané pozadie. Voľba pri zapnutí TV sa nastavuje samostatne.",
        "Výber pozadia" to "Ikona obrázka otvorí knižnicu. Vyber náhľad a Použiť pozadie. Pozadie možno aj vyradiť z náhodného výberu bez vymazania súboru.",
        "Pri zapnutí" to "V nastavení pozadí zvoľ náhodné pozadie alebo ponechanie posledného. Voľba sa ukladá. Dve šípky na ploche zmenia pozadie aj zodpovedajúcu hudbu.",
        "Hudba" to "Krátke OK na note zapína alebo vypína hudbu. Podrž OK pre hudobný panel. Skladby priradené k pozadiu sa premiešajú; po dohraní pokračuje ďalší výber podľa nastavení.",
        "Práve hrá" to "Pri zmene skladby sa na päť sekúnd zobrazí oznámenie vľavo. Ak je ovládanie skryté, oznámenie sa zobrazí hore uprostred.",
        "Uvítanie a hlasitosť" to "Nastavenia plochy → Hudba obsahuje hlasitosť soundtracku, voľbu hudby pre ostatné pozadia a zapnutie či vypnutie uvítacieho zvuku.")),
    HelpTopic("Spotify a prehrávanie","Pod horným panelom sa môže zobraziť hudba z inej aplikácie.",listOf("Spotify · názov skladby","1:25 / 3:40","Predošlá · Pauza · Ďalšia"),listOf(
        "Detail prehrávania" to "Vyber riadok prehrávania a stlač OK. Detail ponúkne dostupné informácie o skladbe, obal, čas a ovládanie, ktoré podporuje daná aplikácia.",
        "Otvoriť aplikáciu" to "Tlačidlo v detaile otvorí napríklad Spotify. Čas a obal sa zobrazujú iba vtedy, keď ich aplikácia poskytne.",
        "Keď sa riadok nezobrazí" to "Prehrávajúca aplikácia musí podporovať ovládanie z iných aplikácií. V hudobnom paneli povoľ prístup k prehrávaniu. Hudba na ploche sa pri prehrávaní z inej aplikácie pozastaví alebo stíši.")),
    HelpTopic("Správy a novinky","Správy sú články; Novinky sú prehľad filmov a seriálov.",listOf("Aktuality.sk","Sector.sk","Startitup","360tka.sk","TV Noviny"),listOf(
        "Spravodajský pás" to "OK otvorí zoznam. Zdroj má vlastnú farbu. Vo filtri si vyber konkrétny web alebo Všetky. Článok otvoríš cez OK v prehliadači.",
        "Zdroje a obnova" to "Aktuality, Sector, Refresher, Živé, BBC, Startitup, 360tka a TV Noviny. Bežná obnova je každých 15 minút na aktívnej ploche; úplne prázdny zoznam sa skúša znova o minútu. Ručné Obnoviť má krátku ochranu proti opakovanému klikaniu.",
        "Ako dlho zostávajú správy" to "Zobrazujú sa správy z posledných 48 hodín, najviac 180 článkov. Pri výpadku pripojenia môžu zostať dostupné už načítané správy. Nie každý zdroj poskytuje obrázok alebo súhrn.",
        "Filmy a seriály" to "Ikona filmového pásu otvorí Nové série z API TVmaze. Rozsah je americký program, tri dni dozadu a dopredu. Nejde o prehľad kín. Zdroj a licencia CC BY-SA sú uvedené v detaile. Obnova je po šiestich hodinách.")),
    HelpTopic("Počasie a TV","Počasie používa jednu spoločnú predpoveď pre plochu aj šetrič.",listOf("☁ 6°","13:45","15. novembra"),listOf(
        "Mesto a hodinová predpoveď" to "OK na počasí otvorí detail. Zmeniť mesto vyhľadá mesto cez Open-Meteo. Automatický odhad podľa IP je vypnutý. Výber dňa otvorí hodinovú predpoveď.",
        "Skrytie počasia" to "V detaile vypni Zobraziť počasie na ploche. Počasie sa skryje aj pri skrytom ovládaní a v šetriči. Bez aktuálnych údajov sa teplota nezobrazuje.",
        "Zarovnanie" to "So správami je počasie naľavo od pásu. Bez pásu je napravo pod horným panelom. Pri skrytom ovládaní je pred časom vpravo dole.",
        "Nastavenia televízora" to "Obraz a Zvuk otvoria systémové bočné ponuky. Ozubené koliesko otvorí panel TV. Wi-Fi ukazuje pripojenie a silu signálu; pri zvýraznení aj dostupný názov siete. Pôvodné Google TV otvoríš podržaním Home.")),
    HelpTopic("HDMI vstupy","Aktívny vstup otvoríš priamo z domovskej plochy alebo z ponuky vstupov.",listOf("HDMI 4","Názov z TV","Vlastný názov"),listOf(
        "Automatické názvy" to "Ak TV poskytne názov pripojeného zariadenia, zobrazí sa pri vstupe. Inak zostane označenie HDMI a číslo vstupu.",
        "Vlastný názov vstupu" to "Nastavenia plochy → Vzhľad → Názvy HDMI vstupov. Vyber vstup, napíš názov a Uložiť vlastný názov. Tento názov zostane na vstupe aj po výmene zariadenia.",
        "Návrat k automatike" to "Použiť názov z TV odstráni vlastný názov a obnoví automatické pomenovanie.")),
    HelpTopic("Fotografie a albumy","Fotografie, popisy a výber šetriča spravuješ v Nastavenia plochy → Fotoalbum.",listOf("Moje fotografie","Celá obrazovka","Upraviť popis a album"),listOf(
        "Spravovať fotografie" to "Otvor fotografiu pre detail, zobrazenie na celú obrazovku alebo úpravu popisu, dátumu, miesta a albumu. Mapa je dostupná pri vyplnenom mieste.",
        "Albumy a šetrič" to "Vyber jeden, viac alebo všetky albumy. Cez Nový album vytvoríš novú skupinu; fotografie do nej priradíš v ich detaile.",
        "Vyradiť nie je vymazať" to "Vyradená fotka zostáva uložená a cez filter Vyradené ju môžeš vrátiť. Nevstupuje do šetriča ani do spomienok.",
        "Import" to "Pridať fotografie alebo videá otvorí výber súborov. Podporované sú JPEG, PNG, WebP a MP4. Fotografie RAW najprv preveď na podporovaný formát.")),
    HelpTopic("Spomienky a šetrič","Vyskakovacia spomienka a šetrič fotografií sú dve samostatné funkcie.",listOf("SPOMIENKA","Album · krátky popis","OK → detail → celá fotografia"),listOf(
        "Spomienka na ploche" to "Nastavenia plochy → Fotoalbum → Spomienky na ploche; rovnaká voľba je vo Vzhľad. Zapneš ju, vypneš alebo zmeníš interval 2/5/10/15 minút a zobrazenie 8/12/20 sekúnd.",
        "Striedanie fotiek" to "Spomienky náhodne prechádzajú aktívne fotky z povolených albumov bez opakovania do konca kola. História sa uchováva aj po návrate na plochu alebo reštarte. Po prejdení výberu začne nové kolo; ak máš viac fotiek, prvá nebude rovnaká ako posledná z minulého kola.",
        "Kedy sa ukazuje" to "Prvá spomienka sa môže zobraziť po 20 sekundách na ploche. Nezobrazuje sa cez otvorené menu ani pri skrytom ovládaní. Po otvorení detailu môžeš fotografiu zobraziť celú.",
        "Šetrič fotografií" to "Fotky sa menia približne po 20 sekundách. Šípky listujú, OK alebo Späť šetrič zatvorí. Videá sa prehrávajú bez zvuku. Fotka otvorená cez Celá obrazovka sama nepreskakuje.",
        "Informácie v šetriči" to "Pri fotografii je názov albumu, popis a dátum. Hore sa zobrazuje čas a dostupné počasie.",
        "Systémový šetrič" to "Čas automatického spustenia riadi TV. Systémový šetrič otvorí jej nastavenia. Aerial Views je samostatný online šetrič s vlastnými nastaveniami.")),
    HelpTopic("Minihry","Krátke OK na hernej ikone otvorí poslednú hru; podrž OK pre výber hry.",listOf("Prejdi cestu","Kolobežka","Rekordy · mince · postavy"),listOf(
        "Prejdi cestu" to "Šípkami sa pohybuješ medzi pruhmi a prekážkami. OK alebo Späť otvorí pauzu. Náročnosť rastie počas postupu, zbieraš mince a odomykáš postavy.",
        "Kolobežka" to "Hore je skok; ďalšie stlačenie vo vzduchu pridá trik a druhý skok. Dole sa prikrčíš, vľavo brzdíš a vpravo zrýchľuješ. OK alebo Späť pozastaví hru.",
        "Uložený postup a zvuky" to "Rekordy, mince, odomknuté postavičky a splnené výzvy sa ukladajú automaticky. V ponuke hry nájdeš výzvy, výber postavičiek a hlasitosť zvukov. Pri prehrávaní hudby sa herné zvuky vypnú.",
        "Odchod" to "V ponuke pauzy môžeš prepnúť skladbu alebo ukončiť hru. Home ťa vráti na domovskú plochu.")),
    HelpTopic("Diagnostika","Pri probléme otvor Nastavenia plochy → Diagnostika.",listOf("Pripojenie","Obnoviť kontrolu","Záznamy a chyby"),listOf(
        "Systémový prehľad" to "Zobrazí voľné úložisko, dostupnú pamäť TV, využitie pamäte aplikáciou, pripojenie a posledné obnovenie údajov. Kontrola sa spustí pri otvorení alebo cez Obnoviť kontrolu.",
        "Chyby a detaily" to "Záznamy a chyby zobrazujú udalosti našej aplikácie, konkrétne zdroje správ, počasie a prehrávanie. Zapni filter chyby a cez OK otvor detail. Systémový záznam pádu sa môže doplniť po ďalšom spustení.",
        "Automatické čistenie" to "Záznamy sa automaticky mažú po 30 dňoch alebo skôr, ak ich je viac ako 400 či zaberajú viac ako 256 KB.",
        "Keď nejdú správy" to "Skontroluj sieť a v Správach stlač Obnoviť. Pozri filter konkrétneho zdroja a jeho záznam v diagnostike. Prázdny zdroj nemusí znamenať pád aplikácie; web mohol zmeniť formát alebo nemať nové položky.",
        "Pamäť a súkromie" to "Dočasné obrázky a zoznamy sa priebežne čistia. Fotografie, videá, hudba, nastavenia a herný postup zostávajú uložené. Diagnostické záznamy neobsahujú heslá ani prihlasovacie údaje."))
)

@Composable fun HelpPanel(modifier:Modifier=Modifier){
    var selected by remember{mutableIntStateOf(0)}
    val activeGuide=if(AppLanguage.code=="en")englishGuide else guide
    val topic=activeGuide[selected.coerceIn(0,activeGuide.lastIndex)]
    Row(modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(22.dp)){
        LazyColumn(Modifier.width(200.dp).fillMaxHeight(),contentPadding=PaddingValues(8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            itemsIndexed(activeGuide){i,item->PanelButton((if(i==selected)"› " else "")+item.name,modifier=Modifier.fillMaxWidth()){selected=i}}
        }
        key(selected){LazyColumn(Modifier.weight(1f).fillMaxHeight(),contentPadding=PaddingValues(10.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            item{Text(topic.name,fontSize=24.sp,color=Color.White);Spacer(Modifier.height(8.dp));Text(topic.intro,fontSize=15.sp,color=Color(0xFFCBDCEB))}
            item{Column(Modifier.fillMaxWidth().background(Color(0xFF20394C),RoundedCornerShape(14.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("UKÁŽKA · ilustračné údaje",fontSize=10.sp,color=Color(0xFF9DC8E9))
                topic.example.chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){row.forEach{label->Text(label,Modifier.weight(1f).background(Color(0xFF132331),RoundedCornerShape(8.dp)).padding(10.dp),fontSize=12.sp,color=Color.White)}}}
            }}
            itemsIndexed(topic.sections){_,section->
              var focused by remember{mutableStateOf(false)}
              Column(Modifier.fillMaxWidth().onFocusChanged{focused=it.isFocused}.focusable()
                .background(Color(0xFF182A3B),RoundedCornerShape(12.dp))
                .border(2.dp,if(focused)Color(0xFF9DC8E9) else Color.Transparent,RoundedCornerShape(12.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(section.first,fontSize=17.sp,color=Color.White)
                Text(section.second,fontSize=14.sp,color=Color(0xFFCAD8E5))
            }}
        }}
    }
}
