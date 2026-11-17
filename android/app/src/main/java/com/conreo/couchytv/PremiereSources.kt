package com.conreo.couchytv

import android.content.Context
import android.app.Activity
import android.app.AlertDialog
import kotlinx.coroutines.*

object PremiereSources {
    private lateinit var context:Context
    val options=linkedMapOf("tvmaze" to "TVmaze · API", "cinema" to "ITAFILM / CINEMAX · kiná", "calendar" to "TVmaze · webový kalendár")
    fun load(c:Context){context=c.applicationContext}
    fun selected():Set<String> = context.getSharedPreferences("premiere_sources",0).getStringSet("enabled",null)?.intersect(options.keys) ?: EditionConfig.premiereSources.intersect(options.keys)
    fun save(ids:Set<String>){require(options.keys.containsAll(ids));context.getSharedPreferences("premiere_sources",0).edit().putStringSet("enabled",ids).apply();Premieres.clearCache()}
    fun show(a:Activity,changed:()->Unit) {
        val keys=options.keys.toList();val chosen=selected().toMutableSet()
        AlertDialog.Builder(a,R.style.MediaLibraryDialog).setTitle(AppLanguage.text("Zdroje noviniek"))
            .setMultiChoiceItems(options.values.map{AppLanguage.text(it)}.toTypedArray(),keys.map{it in chosen}.toBooleanArray()){_,i,on->if(on)chosen.add(keys[i])else chosen.remove(keys[i])}
            .setPositiveButton(AppLanguage.text("Uložiť")){_,_->
                if(chosen.any{it!="tvmaze"} && chosen!=selected()) AlertDialog.Builder(a,R.style.MediaLibraryDialog)
                    .setTitle(AppLanguage.text("Podmienky zdrojov"))
                    .setMessage(AppLanguage.text("Webové zdroje zapni iba ak ich podmienky povoľujú tvoje použitie. Obsah a obrázky patria poskytovateľom; nejde o oficiálne API a dostupnosť sa môže meniť."))
                    .setPositiveButton(AppLanguage.text("Zapnúť vybrané")){_,_->save(chosen);changed()}.setNegativeButton(AppLanguage.text("Zrušiť"),null).show()
                else {save(chosen);changed()}
            }.setNegativeButton(AppLanguage.text("Zrušiť"),null).show()
    }
}

object Premieres {
    fun clearCache(){ApiPremieres.clearCache();LegacyPremieres.clearCache()}
    fun dateLabel(date:String)=ApiPremieres.dateLabel(date)
    fun parseSchedule(raw:String)=ApiPremieres.parseSchedule(raw)
    suspend fun detail(item:Premiere)=LegacyPremieres.detail(item)
    suspend fun all(onPartial:(List<Premiere>)->Unit={}):List<Premiere> = coroutineScope {
        val selected=PremiereSources.selected();if(selected.isEmpty())return@coroutineScope emptyList()
        val jobs=selected.map { source->async {
            try { when(source){"tvmaze"->ApiPremieres.all();"cinema"->LegacyPremieres.load("Kiná na Slovensku");else->LegacyPremieres.load("Seriály")} }
            catch(e:CancellationException){throw e}
            catch(e:Exception){DiagnosticLog.report("Novinky · $source","CHYBA","Načítanie zdroja zlyhalo.",e);null}
        } }
        val rows=mutableListOf<Premiere>();var successes=0
        for(job in jobs){job.await()?.let{successes++;rows+=it};onPartial(order(rows))}
        check(successes>0){"Zdroje nie sú dostupné."};order(rows)
    }
    private fun order(rows:List<Premiere>):List<Premiere> {
        val today=java.time.LocalDate.now().toString();val unique=rows.distinctBy{it.url+it.title}.take(200)
        return unique.filter{it.date<=today}.sortedByDescending{it.date}+unique.filter{it.date>today}.sortedBy{it.date}
    }
}
