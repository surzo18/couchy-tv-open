package com.conreo.couchytv.ui

import com.conreo.couchytv.ui.LocalizedText as Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conreo.couchytv.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun DiagnosticLogPanel(onClose:()->Unit){
    var entries by remember{mutableStateOf(emptyList<DiagnosticEvent>())}
    var errorsOnly by remember{mutableStateOf(false)}
    var revision by remember{mutableIntStateOf(0)}
    var bytes by remember{mutableLongStateOf(0)}
    var detail by remember{mutableStateOf<DiagnosticEvent?>(null)}
    var confirmClear by remember{mutableStateOf(false)}
    LaunchedEffect(revision){entries=withContext(Dispatchers.IO){DiagnosticLog.snapshot()};bytes=DiagnosticLog.bytes()}
    val filtered=if(errorsOnly)entries.filter{it.level in listOf("CHYBA","VAROVANIE")} else entries
    fun back(){if(detail!=null)detail=null else if(confirmClear)confirmClear=false else onClose()}
    PanelShell("Diagnostika · záznamy",width=900.dp,onBack={back()},onClose={back()},closeLabel=if(detail!=null || confirmClear)"Späť na záznamy" else "Zavrieť"){
        if(confirmClear){
            Text("Vymazať iba diagnostické záznamy tejto aplikácie? Nastavenia, fotografie ani hudba sa nemenia.",fontSize=16.sp,color=Color.White)
            PanelButton("Vymazať záznamy",initial=true){DiagnosticLog.clear();entries=emptyList();bytes=0;confirmClear=false}
        }else if(detail!=null){
            val e=detail!!
            Text("${RuntimeHealth.time(e.at)} · ${e.level} · ${e.area}",fontSize=14.sp,color=Color(0xFFABCEEC))
            Text(e.message,fontSize=17.sp,color=Color.White)
            LazyColumn(Modifier.panelListNavigation().weight(1f,false).heightIn(max=240.dp)){item{Text(e.detail.ifBlank{"Ďalšie podrobnosti nie sú dostupné."},fontSize=12.sp,color=Color(0xFFCAD6E3))}}
        }else{
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){
                PanelButton(if(errorsOnly)"Filter: chyby" else "Filter: všetko",initial=true){errorsOnly=!errorsOnly}
                PanelButton("Obnoviť"){revision++}
                PanelButton("Vymazať…"){confirmClear=true}
            }
            Text("${entries.size} záznamov · ${bytes/1024} KB / 256 KB · najviac 30 dní a 400 udalostí",fontSize=12.sp,color=Color(0xFFBDD0E0))
            Text("Najnovšie záznamy sú hore. OK otvorí podrobnosti. Zobrazujú sa udalosti tejto aplikácie; staršie záznamy sa automaticky mažú.",fontSize=12.sp,color=Color(0xFFBDD0E0))
            if(filtered.isEmpty())Text(if(errorsOnly)"Žiadne uložené chyby ani varovania." else "Zatiaľ nie sú uložené žiadne záznamy.",color=Color.White,fontSize=15.sp)
            LazyColumn(Modifier.panelListNavigation().weight(1f,false).heightIn(max=245.dp),contentPadding=PaddingValues(10.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                itemsIndexed(filtered){_,e->
                    Column(Modifier.fillMaxWidth().background(Color(0xFF1D2C3D),RoundedCornerShape(10.dp)).padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        val color=when(e.level){"CHYBA"->Color(0xFFFFA3A3);"VAROVANIE"->Color(0xFFFFD08A);"OK"->Color(0xFF92DDB1);else->Color(0xFFB1CDEB)}
                        Text("${RuntimeHealth.time(e.at)} · ${e.level} · ${e.area}",fontSize=12.sp,color=color)
                        PanelButton(e.message,modifier=Modifier.fillMaxWidth()){detail=e}
                    }
                }
            }
        }
    }
}
