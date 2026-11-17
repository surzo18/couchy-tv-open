package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conreo.couchytv.*

@Composable private fun EditField(value:String,label:String,onChange:(String)->Unit){
    Text(label,color=Color(0xFFB8CBDF),fontSize=12.sp)
    BasicTextField(value,onChange,textStyle=TextStyle(color=Color.White,fontSize=17.sp),singleLine=true,modifier=Modifier.fillMaxWidth().background(Color(0xFF2C4053),RoundedCornerShape(8.dp)).padding(10.dp))
}
@Composable fun InputNamesPanel(c:Context,onClose:()->Unit){
    var revision by remember{mutableIntStateOf(0)}
    val inputs=remember(revision){com.conreo.couchytv.data.Inputs.load(c)}
    var selected by remember{mutableStateOf<String?>(null)};var name by remember{mutableStateOf("")}
    fun back(){if(selected!=null)selected=null else onClose()}
    PanelShell("Názvy HDMI vstupov",onBack={back()},onClose={back()},closeLabel=if(selected!=null)"Späť na vstupy" else "Zavrieť"){
        if(selected!=null){
            val info=(c.getSystemService(Context.TV_INPUT_SERVICE) as? android.media.tv.TvInputManager)?.tvInputList?.firstOrNull{it.id==selected}
            val reported=info?.loadCustomLabel(c)?.toString()?.takeIf{it.isNotBlank()} ?: info?.loadLabel(c)?.toString().orEmpty()
            Text("Názov z TV: $reported",color=Color.White,fontSize=17.sp)
            Text("Ak TV poskytne názov zariadenia, zobrazí sa automaticky. Inak zostane označenie HDMI a číslo vstupu.",fontSize=13.sp,color=Color(0xFFBACDDE))
            EditField(name,"Vlastný názov (voliteľné)"){name=it.take(40)}
            Text("Vlastný názov zostane pri tomto vstupe aj po pripojení iného zariadenia.",fontSize=12.sp,color=Color(0xFFBACDDE))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){
                PanelButton("Použiť názov z TV",initial=true,modifier=Modifier.weight(1f)){com.conreo.couchytv.data.Inputs.setAlias(c,selected!!,"");selected=null;revision++}
                PanelButton("Uložiť vlastný názov",modifier=Modifier.weight(1f)){if(name.isNotBlank()){com.conreo.couchytv.data.Inputs.setAlias(c,selected!!,name);selected=null;revision++}else Actions.toast(c,"Napíš názov alebo vyber Použiť názov z TV.")}
            }
        }else {
          Text("Vyber vstup a nastav vlastný názov alebo ponechaj automatický názov z TV.",fontSize=13.sp,color=Color(0xFFBACDDE))
          LazyColumn(Modifier.panelListNavigation().heightIn(max=250.dp),contentPadding=PaddingValues(10.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            items(inputs,key={it.pkg}){input->
                val id=com.conreo.couchytv.data.Inputs.idOf(input.pkg)
                val mode=if(com.conreo.couchytv.data.Inputs.alias(c,id).isBlank())"názov z TV" else "vlastný názov"
                PanelButton("${input.label}  ·  $mode",initial=input.pkg==inputs.firstOrNull()?.pkg,modifier=Modifier.fillMaxWidth()){selected=id;name=com.conreo.couchytv.data.Inputs.alias(c,id)}
            }
          }
        }
    }
}
@Composable fun MemorySettingsPanel(c:Context,onClose:()->Unit){
    var enabled by remember{mutableStateOf(AlbumStore.portal(c))}
    var interval by remember{mutableIntStateOf(AlbumStore.portalInterval(c))}
    var duration by remember{mutableIntStateOf(AlbumStore.portalDuration(c))}
    PanelShell("Spomienky na ploche",width=720.dp,onBack=onClose,onClose=onClose){
        Text("Na ploche sa občas zobrazí karta s fotografiou. OK otvorí detail spomienky.",fontSize=15.sp,color=Color(0xFFD0DCE8))
        PanelButton(if(enabled)"Spomienky: zapnuté" else "Spomienky: vypnuté",initial=true){enabled=!enabled;AlbumStore.setPortal(c,enabled)}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){
            PanelButton("Zobraziť každých $interval minút",modifier=Modifier.weight(1f)){val options=listOf(2,5,10,15);interval=options[(options.indexOf(interval)+1)%options.size];AlbumStore.setPortalTiming(c,interval=interval)}
            PanelButton("Ponechať na ploche $duration sekúnd",modifier=Modifier.weight(1f)){val options=listOf(8,12,20);duration=options[(options.indexOf(duration)+1)%options.size];AlbumStore.setPortalTiming(c,duration=duration)}
        }
        Text("Prvá spomienka sa môže zobraziť po 20 sekundách. Nezobrazuje sa cez otvorené menu ani pri skrytom ovládaní. Keď ju vyberieš, zostane zobrazená dlhšie.",fontSize=12.sp,color=Color(0xFFBACDDE))
        Text("Zobrazujú sa povolené fotografie z albumov vybraných pre šetrič. Vyradené fotografie sa nezobrazujú.",fontSize=12.sp,color=Color(0xFFBACDDE))
    }
}
@Composable fun PhotoEditPanel(c:Context,p:AlbumPhoto,onClose:()->Unit,onSaved:()->Unit){
    val original=remember(p){AlbumStore.notes(c,p)}
    var caption by remember{mutableStateOf(original.caption)};var date by remember{mutableStateOf(original.date)};var place by remember{mutableStateOf(original.place)};var album by remember{mutableStateOf(original.album)}
    var error by remember{mutableStateOf("")};val albums=remember{AlbumStore.albums(c)}
    PanelShell("Upraviť spomienku",width=760.dp,onBack=onClose,onClose=onClose,closeLabel="Zrušiť"){
        EditField(caption,"Popis (najviac 240 znakov)"){caption=it.take(240)}
        Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){
            Column(Modifier.weight(1f)){EditField(date,"Dátum (RRRR-MM-DD, nepovinné)"){date=it.take(10)}}
            Column(Modifier.weight(2f)){EditField(place,"Miesto (nepovinné)"){place=it.take(120)}}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            PanelButton("Album: "+(albums[album] ?: "Nezaradené")){val ids=albums.keys.toList();if(ids.isNotEmpty())album=ids[(ids.indexOf(album)+1).mod(ids.size)]}
            PanelButton("Uložiť",initial=true){if(date.isNotBlank() && runCatching{java.time.LocalDate.parse(date)}.isFailure)error="Použi dátum napríklad 2026-09-23." else {AlbumStore.save(c,p,PhotoNotes(album,caption.trim(),date,place.trim()));onSaved()}}
        }
        if(error.isNotEmpty())Text(error,color=Color(0xFFFFBBAA),fontSize=12.sp)
    }
}
@Composable fun AlbumSettingsPanel(c:Context,photos:List<AlbumPhoto>,onChanged:()->Unit,onClose:()->Unit){
    var revision by remember{mutableIntStateOf(0)};var editId by remember{mutableStateOf<String?>(null)};var name by remember{mutableStateOf("")}
    val albums=remember(revision){AlbumStore.albums(c)};val selected=remember(revision){AlbumStore.selection(c)}
    fun refresh(){revision++;onChanged()}
    PanelShell("Albumy a šetrič",width=800.dp,onBack=onClose,onClose=onClose){
        if(editId!=null){
            EditField(name,if(editId=="")"Názov nového albumu" else "Nový názov albumu"){name=it.take(60)}
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                PanelButton("Uložiť",initial=true){if(name.isNotBlank()){if(editId=="")AlbumStore.create(c,name) else AlbumStore.rename(c,editId!!,name);editId=null;refresh()}}
                PanelButton("Zrušiť úpravu"){editId=null}
            }
        }else{
            Text("Zvoľ jeden či viac albumov. Vyradené fotky sa neprehrávajú ani pri voľbe Všetky.",fontSize=12.sp,color=Color(0xFFBDCFDD))
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                PanelButton(if(selected==null)"✓ Všetky albumy" else "Všetky albumy",initial=true){AlbumStore.choose(c,null);refresh()}
                PanelButton("Nový album"){editId="";name=""}
                PanelButton("Spustiť šetrič"){c.startActivity(android.content.Intent(c,AlbumPreviewActivity::class.java))}
            }
            LazyColumn(Modifier.weight(1f,false).heightIn(max=145.dp),contentPadding=PaddingValues(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                items(albums.entries.toList(),key={it.key}){a->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    val count=photos.count{it.active && AlbumStore.notes(c,it).album==a.key}
                    PanelButton((if(selected==null || a.key in selected)"✓ " else "")+a.value+" · $count",modifier=Modifier.weight(1f)){
                        val ids=(selected ?: albums.keys).toMutableSet();if(!ids.add(a.key))ids.remove(a.key);AlbumStore.choose(c,ids);refresh()
                    }
                    PanelButton("Vybrať iba tento album"){AlbumStore.choose(c,setOf(a.key));refresh()}
                    PanelButton("Názov",modifier=Modifier.panelListNavigation()){editId=a.key;name=a.value}
                }}
            }
            if(AlbumStore.eligible(c,photos).isEmpty())Text("Výber nemá aktívne fotky ani videá. Šetrič zobrazí iba hodiny.",fontSize=12.sp,color=Color(0xFFFFCB9B))
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                PanelButton((if(AlbumStore.showInfo(c))"✓ " else "")+"Popisy v šetriči"){AlbumStore.setInfo(c,!AlbumStore.showInfo(c));refresh()}
                PanelButton((if(AlbumStore.portal(c))"✓ " else "")+"Spomienka na ploche"){AlbumStore.setPortal(c,!AlbumStore.portal(c));refresh()}
                PanelButton("Systémový šetrič"){AlbumStore.settings(c)}
            }
        }
    }
}
