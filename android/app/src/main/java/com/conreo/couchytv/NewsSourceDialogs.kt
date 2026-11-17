package com.conreo.couchytv

import android.app.Activity
import android.app.AlertDialog
import android.widget.*
import android.graphics.Color
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class NewsSourceDialogs(private val a:Activity,private val changed:()->Unit) {
    private fun builder()=AlertDialog.Builder(a,R.style.MediaLibraryDialog)
    fun show() {
        NewsSources.load(a)
        val items=NewsSources.all
        val labels=items.map{(if(it.enabled)"✓ " else "○ ")+it.source+" · "+it.section}.toTypedArray()
        builder().setTitle(AppLanguage.text("Zdroje správ"))
            .setItems(labels){_,i->edit(items[i])}
            .setPositiveButton(AppLanguage.text("Pridať zdroj")){_,_->edit(null)}
            .setNeutralButton(AppLanguage.text("Vymazať zdroje")){_,_->builder().setTitle(AppLanguage.text("Odstrániť všetky zdroje?"))
                .setMessage(AppLanguage.text("Vlastné zdroje sa odstránia."))
                .setPositiveButton(AppLanguage.text("Odstrániť")){_,_->NewsSources.reset();changed();show()}
                .setNegativeButton(AppLanguage.text("Zrušiť")){_,_->show()}.show()}
            .setNegativeButton(AppLanguage.text("Zavrieť"),null).show()
    }
    private fun edit(item:NewsSource?) {
        val density=a.resources.displayMetrics.density
        val fields=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;setPadding((24*density).toInt(),12,(24*density).toInt(),12)}
        fun field(label:String,value:String)=EditText(a).apply {
            hint=AppLanguage.text(label);setText(value);setTextColor(Color.WHITE);setHintTextColor(0xFFB9CADD.toInt());isSingleLine=true
            fields.addView(this)
        }
        val name=field("Názov zdroja",item?.source.orEmpty())
        val url=field("HTTPS adresa RSS alebo Atom",item?.url.orEmpty())
        val section=field("Kategória",item?.section.orEmpty())
        val color=field("Farba #RRGGBB",String.format("#%06X",(item?.color ?: 0xFFD4DEE8.toInt()) and 0xFFFFFF))
        val enabled=CheckBox(a).apply{text=AppLanguage.text("Povolený");isChecked=item?.enabled ?: true;setTextColor(Color.WHITE);fields.addView(this)}
        val status=TextView(a).apply{text=AppLanguage.text("Použi zdroj, ktorého podmienky povoľujú čítanie v tejto aplikácii. Obsah zostáva majetkom vydavateľa.");setTextColor(Color.WHITE);fields.addView(this)}
        fun value():NewsSource {
            require(name.text.trim().isNotEmpty()){AppLanguage.text("Zadaj názov zdroja.")}
            require(NewsSources.validUrl(url.text.toString().trim())){AppLanguage.text("Zadaj platnú HTTPS adresu bez hesla.")}
            require(Regex("#[0-9a-fA-F]{6}").matches(color.text.toString())){AppLanguage.text("Farba musí byť vo formáte #RRGGBB.")}
            return NewsSource(item?.id ?: NewsSources.newId(),url.text.toString().trim(),section.text.toString().trim(),name.text.toString().trim(),Color.parseColor(color.text.toString()),enabled.isChecked,
                if(item?.url==url.text.toString().trim())item?.parser ?: "rss" else "rss")
        }
        var testing=false
        val test=Button(a).apply{text=AppLanguage.text("Overiť zdroj");fields.addView(this)}
        val d=builder().setTitle(AppLanguage.text(if(item==null)"Pridať zdroj" else "Upraviť zdroj"))
            .setView(ScrollView(a).apply{addView(fields)})
            .setPositiveButton(AppLanguage.text("Uložiť"),null).setNegativeButton(AppLanguage.text("Zrušiť"),null)
            .apply{if(item!=null)setNeutralButton(AppLanguage.text("Odstrániť")){_,_->builder().setTitle(AppLanguage.text("Odstrániť zdroj?")).setMessage(item.source)
                .setPositiveButton(AppLanguage.text("Odstrániť")){_,_->NewsSources.remove(item.id);changed();show()}.setNegativeButton(AppLanguage.text("Zrušiť")){_,_->edit(item)}.show()}}.create()
        var job:kotlinx.coroutines.Job?=null
        d.setOnDismissListener{job?.cancel()}
        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                runCatching{NewsSources.save(value())}.onSuccess{changed();d.dismiss();show()}.onFailure{status.text=it.message}
            }
            test.setOnClickListener {
                if(testing)return@setOnClickListener
                val source=runCatching{value()}.getOrElse{status.text=it.message;return@setOnClickListener}
                job=(a as? LifecycleOwner)?.lifecycleScope?.launch {
                    testing=true;test.isEnabled=false;status.text=AppLanguage.text("Načítavam…")
                    try {val count=withContext(Dispatchers.IO){HomeNews.fetch(source).size};status.text=AppLanguage.text("Počet článkov")+": $count"}
                    catch(e:kotlinx.coroutines.CancellationException){throw e}
                    catch(e:Exception){status.text=AppLanguage.text("Zdroj sa nepodarilo načítať.")}
                    finally{testing=false;test.isEnabled=true}
                }
            }
        }
        d.show()
    }
}
