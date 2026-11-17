package com.conreo.couchytv

import com.conreo.couchytv.ui.LocalizedText as Text
import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import com.conreo.couchytv.ui.setTvContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.conreo.couchytv.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val ACTIVE_ALBUM get()=EditionConfig.albumActive
private val HIDDEN_ALBUM get()=EditionConfig.albumHidden
data class AlbumPhoto(val uri:Uri,val name:String,val active:Boolean,val date:String="",val video:Boolean=false)
fun albumPhotos(c:Context):List<AlbumPhoto> {
    val result=mutableListOf<AlbumPhoto>()
    listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,MediaStore.Video.Media.EXTERNAL_CONTENT_URI).forEachIndexed{index,collection->
        c.contentResolver.query(collection,arrayOf("_id","_display_name","relative_path","datetaken"),"relative_path IN (?, ?)",arrayOf(ACTIVE_ALBUM,HIDDEN_ALBUM),"_display_name ASC")?.use { cursor ->
            while(cursor.moveToNext() && result.size<5000){val taken=cursor.getLong(3);val date=if(taken>0)java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date(taken)) else "";result.add(AlbumPhoto(ContentUris.withAppendedId(collection,cursor.getLong(0)),cursor.getString(1),cursor.getString(2)==ACTIVE_ALBUM,date,index==1))}
        }
    }
    return result
}
private fun movePhoto(c:Context,p:AlbumPhoto) {
    // Check current membership again before touching the one selected MediaStore item.
    val current=c.contentResolver.query(p.uri,arrayOf("relative_path"),null,null,null)?.use{if(it.moveToFirst())it.getString(0) else null}
    check(current==if(p.active)ACTIVE_ALBUM else HIDDEN_ALBUM){"Fotografia sa medzičasom zmenila. Obnov zoznam."}
    val count=c.contentResolver.update(p.uri,ContentValues().apply{put(MediaStore.Images.Media.RELATIVE_PATH,if(p.active)HIDDEN_ALBUM else ACTIVE_ALBUM)},null,null)
    check(count==1){"Fotografiu sa nepodarilo upraviť."}
}

class AlbumActivity:LocalizedActivity() {
    override fun onCreate(state:Bundle?){super.onCreate(state);setTvContent {LiteTvTheme(accent=Color(0xFF90BDFF)) { AlbumScreen() }}}
    @Composable private fun AlbumScreen() {
        val scope=rememberCoroutineScope()
        var photos by remember{mutableStateOf(emptyList<AlbumPhoto>())}
        var selected by remember{mutableStateOf<AlbumPhoto?>(null)}
        var pending by remember{mutableStateOf<AlbumPhoto?>(null)}
        var busy by remember{mutableStateOf(false)}
        var message by remember{mutableStateOf("")}
        var revision by remember{mutableIntStateOf(0)}
        var filter by remember{mutableStateOf("Všetky")}
        var albumFilter by remember{mutableStateOf(intent.getStringExtra("album").orEmpty())}
        var dayFilter by remember{mutableStateOf(intent.getStringExtra("day").orEmpty())}
        var albumSettings by remember{mutableStateOf(intent.getBooleanExtra("album_settings",false))}
        var editing by remember{mutableStateOf(false)}
        val albums=remember(revision){AlbumStore.albums(this)}
        val gate=remember{Semaphore(2)}
        val permission=if(android.os.Build.VERSION.SDK_INT>=33)Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        val permissions=if(android.os.Build.VERSION.SDK_INT>=34)arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) else if(android.os.Build.VERSION.SDK_INT>=33)arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO) else arrayOf(permission)
        val permissionRequest=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){if(it.values.any{v->v})revision++ else message="Na zobrazenie albumu povoľ prístup k fotografiám a videám."}
        fun change(p:AlbumPhoto,request:((IntentSenderRequest)->Unit)?=null) {
            if(busy)return
            if(p.active && photos.count{it.active}<=1){message="V albume ponechaj aspoň jednu fotografiu.";return}
            busy=true
            scope.launch {
                try{withContext(Dispatchers.IO){movePhoto(this@AlbumActivity,p)};selected=p.copy(active=!p.active);revision++;message="Uložené. Zmena sa prejaví pri ďalšom spustení šetriča."}
                catch(e:CancellationException){throw e}
                catch(e:SecurityException){
                    if(request!=null && android.os.Build.VERSION.SDK_INT>=30) {
                        try{pending=p;request(IntentSenderRequest.Builder(MediaStore.createWriteRequest(contentResolver,listOf(p.uri)).intentSender).build())}
                        catch(e:Exception){pending=null;message="Android nepovolil úpravu fotografie."}
                    }else message="Android nepovolil úpravu fotografie."
                }
                catch(e:Exception){message="Fotografiu sa nepodarilo upraviť. Obnov zoznam a skús znova."}
                finally{busy=false}
            }
        }
        val consent=rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){result->val p=pending;pending=null;if(result.resultCode==RESULT_OK && p!=null)change(p) else message="Úprava bola zrušená."}
        val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
            if(uris.isNotEmpty() && !busy)scope.launch {
                busy=true;var added=0
                try{withContext(Dispatchers.IO){
                    for(uri in uris.take(100)) {
                        ensureActive()
                        var target:Uri?=null
                        try{
                            val mime=contentResolver.getType(uri)
                            if(mime !in setOf("image/jpeg","image/png","image/webp","video/mp4"))continue
                            val video=mime=="video/mp4"
                            val ext=when(mime){"image/png"->"png";"image/webp"->"webp";"video/mp4"->"mp4";else->"jpg"}
                            val values=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,"Foto-${java.util.UUID.randomUUID()}.$ext");put(MediaStore.Images.Media.MIME_TYPE,mime);put(MediaStore.Images.Media.RELATIVE_PATH,ACTIVE_ALBUM);put(MediaStore.Images.Media.IS_PENDING,1)}
                            target=contentResolver.insert(if(video)MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: continue
                            contentResolver.openInputStream(uri)!!.use{input->contentResolver.openOutputStream(target)!!.use{out->val buffer=ByteArray(32768);var total=0L;while(true){ensureActive();val n=input.read(buffer);if(n<0)break;total+=n;check(total<=if(video)512L*1024*1024 else 32L*1024*1024);out.write(buffer,0,n)}}}
                            if(!video)contentResolver.openInputStream(target)!!.use{input->val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true};android.graphics.BitmapFactory.decodeStream(input,null,bounds);check(bounds.outWidth>0 && bounds.outHeight>0)}
                            AlbumStore.save(this@AlbumActivity,AlbumPhoto(target,"",true),PhotoNotes(album=albumFilter.ifEmpty{"asia2026"}))
                            contentResolver.update(target,ContentValues().apply{put(MediaStore.Images.Media.IS_PENDING,0)},null,null);target=null;added++
                        }catch(e:CancellationException){throw e}catch(e:Exception){/* One failed import must not discard successful photos. */}
                        finally{target?.let{contentResolver.delete(it,null,null)}} // Only our incomplete new import.
                    }
                }}finally{busy=false;revision++;message="Pridané: $added z ${uris.size}. Povolené sú fotografie do 32 MB a videá MP4 do 512 MB, najviac 100 súborov naraz."}
            }
        }
        LaunchedEffect(revision){
            if(permissions.none{checkSelfPermission(it)==android.content.pm.PackageManager.PERMISSION_GRANTED}){permissionRequest.launch(permissions);return@LaunchedEffect}
            try{photos=withContext(Dispatchers.IO){albumPhotos(this@AlbumActivity)}}catch(e:CancellationException){throw e}catch(e:Exception){message="Album sa nepodarilo načítať."}
        }
        Column(Modifier.fillMaxSize().background(Color(0xFF121D2A)).padding(30.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Fotoalbum",fontSize=28.sp,color=Color.White)
            Text("V šetriči: ${photos.count{it.active}} · Vyradené: ${photos.count{!it.active}} · OK: otvoriť",fontSize=14.sp,color=Color(0xFFBACBD9))
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                PanelButton("Späť",initial=true){finish()}
                PanelButton("Filter: $filter"){filter=when(filter){"Všetky"->"V šetriči";"V šetriči"->"Vyradené";else->"Všetky"}}
                PanelButton(if(busy)"Pracujem…" else "Pridať fotografie alebo videá"){if(!busy)runCatching{picker.launch(arrayOf("image/jpeg","image/png","image/webp","video/mp4"))}.onFailure{message="Výber súborov sa nepodarilo otvoriť."}}
                PanelButton("Obnoviť"){if(!busy)revision++}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                PanelButton("Album: "+(albums[albumFilter] ?: "Všetky")){val ids=listOf("")+albums.keys;albumFilter=ids[(ids.indexOf(albumFilter)+1).mod(ids.size)];dayFilter=""}
                PanelButton("Albumy a šetrič"){albumSettings=true}
                if(dayFilter.isNotEmpty())PanelButton("Deň: $dayFilter · zrušiť filter"){dayFilter=""}
            }
            if(message.isNotBlank())Text(message,fontSize=12.sp,color=Color(0xFFD4E3EF))
            val visible=remember(photos,filter,albumFilter,dayFilter,revision){photos.filter{p->val n=AlbumStore.notes(this@AlbumActivity,p);(filter=="Všetky" || p.active==(filter=="V šetriči")) && (albumFilter.isEmpty() || n.album==albumFilter) && (dayFilter.isEmpty() || n.date==dayFilter)}}
            LazyVerticalGrid(GridCells.Fixed(4),Modifier.weight(1f),contentPadding=PaddingValues(10.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                items(visible,key={it.uri.toString()}){p->
                    Surface(onClick={selected=p},colors=tvFocusColors(Color(0xFF25394B)),shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),scale=ClickableSurfaceDefaults.scale(focusedScale=1.025f),border=tvFocusBorder(RoundedCornerShape(12.dp))) {
                        Column(Modifier.padding(8.dp)){
                            AlbumThumbnail(p,gate,Modifier.fillMaxWidth().height(100.dp))
                            Text(AlbumStore.notes(this@AlbumActivity,p).caption.ifEmpty{p.name},maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,fontSize=12.sp,color=Color.White)
                            Text(if(p.active)"✓ V šetriči" else "Vyradená",fontSize=11.sp,color=if(p.active)Color(0xFF8DE1B1) else Color(0xFFBACBD9))
                        }
                    }
                }
            }
            Text("Vyradená fotografia zostane uložená. Zo šetriča zmizne pri jeho ďalšom spustení.",fontSize=11.sp,color=Color(0xFFBACBD9))
        }
        if(albumSettings)AlbumSettingsPanel(this,photos,onChanged={revision++},onClose={albumSettings=false})
        selected?.let{p->if(editing)PhotoEditPanel(this,p,onClose={editing=false},onSaved={editing=false;revision++}) else PanelShell(p.name,width=700.dp,onBack={selected=null},onClose={selected=null},closeLabel="Späť do albumu") {
            val notes=AlbumStore.notes(this@AlbumActivity,p)
            AlbumThumbnail(p,gate,Modifier.fillMaxWidth().height(165.dp),large=true)
            Text(AlbumStore.name(this@AlbumActivity,notes.album)+" · "+notes.date,color=Color(0xFF9ACBEE),fontSize=13.sp)
            if(notes.caption.isNotEmpty())Text(notes.caption,color=Color.White,fontSize=15.sp,maxLines=2)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){
                PanelButton("Celá obrazovka",initial=true,modifier=Modifier.weight(1f)){startActivity(android.content.Intent(this@AlbumActivity,AlbumPreviewActivity::class.java).putExtra("photo",p.uri.toString()))}
                PanelButton("Upraviť popis a album",modifier=Modifier.weight(1f)){editing=true}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){
                PanelButton(if(busy)"Ukladám…" else if(p.active)"Vyradiť zo šetriča" else "Vrátiť do šetriča"){change(p){consent.launch(it)}}
                if(notes.place.isNotBlank())PanelButton("Mapa"){AlbumStore.map(this@AlbumActivity,notes.place)}
                if(p.video)PanelButton("Prehrať"){runCatching{startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(p.uri,"video/mp4").addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION))}.onFailure{message="Nie je dostupný prehrávač videa."}}
            }
        }}
    }
    @Composable private fun AlbumThumbnail(p:AlbumPhoto,gate:Semaphore,modifier:Modifier,large:Boolean=false) {
        val bitmap by produceState<android.graphics.Bitmap?>(null,p.uri,large){
            value=withContext(Dispatchers.IO){gate.withPermit{val cancel=android.os.CancellationSignal();try{contentResolver.loadThumbnail(p.uri,android.util.Size(if(large)960 else 320,if(large)540 else 180),cancel)}catch(e:Exception){null}}}
        }
        Box(modifier.background(Color(0xFF172330)),contentAlignment=Alignment.Center){bitmap?.let{Image(it.asImageBitmap(),p.name,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)} ?: Text("Fotografia",color=Color(0xFFBACBD9),fontSize=12.sp)}
    }
}
