package com.conreo.couchytv

import android.app.Application
import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class CouchyApplication:Application(){override fun onCreate(){super.onCreate();EditionConfig.load(this);PremiereSources.load(this);DiagnosticLog.init(this); AppLanguage.load(this); NewsSources.load(this)}}
data class DiagnosticEvent(val at:Long,val area:String,val level:String,val message:String,val detail:String="")

/** App-owned diagnostics only. No URLs, credentials, article titles, or media metadata. */
object DiagnosticLog {
    private const val MAX_BYTES=256*1024
    private const val MAX_ITEMS=400
    private val lock=Any()
    private val worker=ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,ArrayBlockingQueue<Runnable>(64),{r->Thread(r,"CouchyDiagnostics").apply{isDaemon=true}},ThreadPoolExecutor.DiscardPolicy()).apply{allowCoreThreadTimeOut(true)}
    @Volatile private var folder:File?=null
    fun init(c:Context){
        if(folder!=null)return
        folder=File(c.noBackupFilesDir,"diagnostics").apply{mkdirs()}
        val previous=Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler{thread,error->
            runCatching{
                val crash=eventJson(DiagnosticEvent(System.currentTimeMillis(),"Aplikácia","CHYBA","Neošetrená výnimka",safeError(error)))
                File(folder,"last-crash.json").writeText(crash.toString().take(8192),Charsets.UTF_8)
            }
            if(previous!=null)previous.uncaughtException(thread,error) else {android.os.Process.killProcess(android.os.Process.myPid());kotlin.system.exitProcess(1)}
        }
        worker.execute{
            synchronized(lock){
                val pending=File(folder,"last-crash.json")
                if(pending.exists()){
                    runCatching{if(pending.length()<=8192)parseEvent(JSONObject(pending.readText()))?.let{writeLocked(readLocked()+it)}}
                    pending.delete()
                }
                writeLocked(readLocked())
            }
            if(android.os.Build.VERSION.SDK_INT>=30)runCatching{
                val prefs=c.getSharedPreferences("diagnostic_exits",0)
                val since=prefs.getLong("last",System.currentTimeMillis())
                val exits=(c.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getHistoricalProcessExitReasons(c.packageName,0,8)
                exits.filter{it.timestamp>since}.forEach{info->
                    val reason=when(info.reason){android.app.ApplicationExitInfo.REASON_ANR->"Android zaznamenal nereagujúcu aplikáciu (ANR)";android.app.ApplicationExitInfo.REASON_CRASH->"Android zaznamenal pád aplikácie";android.app.ApplicationExitInfo.REASON_CRASH_NATIVE->"Android zaznamenal natívny pád";android.app.ApplicationExitInfo.REASON_LOW_MEMORY->"Android ukončil aplikáciu pre nedostatok pamäte";else->null}
                    if(reason!=null)synchronized(lock){writeLocked(readLocked()+DiagnosticEvent(info.timestamp,"Android","CHYBA",reason))}
                }
                prefs.edit().putLong("last",System.currentTimeMillis()).apply()
            }
            report("Aplikácia","INFO","Spustenie aplikácie")
        }
    }
    private fun safeError(error:Throwable):String {
        val cause=error.cause ?: error
        val http=Regex("HTTP \\d{3}").find(cause.message.orEmpty())?.value.orEmpty()
        return (cause.javaClass.simpleName+" "+http+"\n"+cause.stackTrace.take(8).joinToString("\n"){"${it.className}.${it.methodName}:${it.lineNumber}"}).take(1200)
    }
    fun report(area:String,level:String,message:String,error:Throwable?=null){
        if(folder==null)return
        val event=DiagnosticEvent(System.currentTimeMillis(),area.take(60),level.take(12),message.take(220),error?.let{safeError(it)}.orEmpty())
        worker.execute{runCatching{synchronized(lock){
            val old=readLocked()
            val previous=old.lastOrNull{it.area==event.area}
            if(previous==null || previous.level!=level || previous.message!=event.message || previous.detail!=event.detail || event.at-previous.at>10*60_000L)writeLocked(old+event)
        }}}
    }
    fun snapshot():List<DiagnosticEvent> = synchronized(lock){val entries=readLocked();writeLocked(entries);entries.asReversed()}
    fun clear(){worker.execute{runCatching{synchronized(lock){writeLocked(emptyList())}}}}
    fun bytes():Long=folder?.resolve("events.json")?.length() ?: 0L
    private fun file()=folder?.let{AtomicFile(File(it,"events.json"))}
    private fun readLocked():List<DiagnosticEvent>{
        val file=file() ?: return emptyList()
        return runCatching{
            if(file.baseFile.length()>MAX_BYTES*2)return@runCatching emptyList()
            val array=JSONArray(String(file.readFully(),Charsets.UTF_8))
            val now=System.currentTimeMillis()
            DiagnosticRetention.retain((0 until minOf(array.length(),MAX_ITEMS)).mapNotNull{array.optJSONObject(it)?.let(::parseEvent)},now)
        }.getOrDefault(emptyList())
    }
    private fun parseEvent(j:JSONObject)=j.optLong("at",0).takeIf{it>0}?.let{DiagnosticEvent(it,j.optString("area").take(60),j.optString("level").take(12),j.optString("message").take(220),j.optString("detail").take(1200))}
    private fun eventJson(e:DiagnosticEvent)=JSONObject().put("at",e.at).put("area",e.area).put("level",e.level).put("message",e.message).put("detail",e.detail)
    private fun writeLocked(entries:List<DiagnosticEvent>){
        val file=file() ?: return
        val now=System.currentTimeMillis()
        val bounded=DiagnosticRetention.retain(entries,now).toMutableList()
        fun encoded()=JSONArray(bounded.map(::eventJson)).toString().toByteArray(Charsets.UTF_8)
        var bytes=encoded()
        while(bytes.size>MAX_BYTES && bounded.isNotEmpty()){bounded.removeAt(0);bytes=encoded()}
        var stream:java.io.FileOutputStream?=null
        try{stream=file.startWrite();stream.write(bytes);file.finishWrite(stream)}catch(e:Exception){if(stream!=null)file.failWrite(stream)}
    }
}
