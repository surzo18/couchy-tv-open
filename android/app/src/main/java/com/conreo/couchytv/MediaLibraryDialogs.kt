package com.conreo.couchytv

import android.app.Activity
import android.app.AlertDialog
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.view.ContextThemeWrapper
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.widget.BaseAdapter
import android.widget.TextView
import android.content.Context
import android.view.inputmethod.InputMethodManager

/** On-demand native TV lists: bounded views, DPAD navigation and no background work. */
class MediaLibraryDialogs(private val activity: Activity, private val changed: () -> Unit) {
    private val c = ContextThemeWrapper(activity, R.style.MediaLibraryDialog)
    private fun dp(n: Int) = (n * c.resources.displayMetrics.density).toInt()
    private fun builder(): AlertDialog.Builder = object: AlertDialog.Builder(c, R.style.MediaLibraryDialog) {
        private var heading: CharSequence = ""
        override fun setTitle(title: CharSequence?): AlertDialog.Builder { heading=AppLanguage.text(title?.toString().orEmpty()); return this }
        override fun create(): AlertDialog {
            val header = LinearLayout(c).apply { orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(20),dp(12),dp(20),dp(8)) }
            header.addView(TextView(c).apply { text=heading; textSize=20f; setTextColor(Color.rgb(242,246,252)); setTypeface(null,Typeface.BOLD) },LinearLayout.LayoutParams(0,-2,1f))
            val close = android.widget.ImageButton(c).apply {
                contentDescription="Zavrieť"; setImageResource(R.drawable.ic_library_close)
                scaleType=android.widget.ImageView.ScaleType.FIT_CENTER
                setPadding(dp(10),dp(10),dp(10),dp(10)); minimumWidth=0; minimumHeight=0
                background=rowBackground()
            }
            header.addView(close,LinearLayout.LayoutParams(dp(42),dp(42)).apply { leftMargin=dp(14) })
            setCustomTitle(header)
            val d = super.create()
            close.setOnClickListener { d.cancel() }
            d.setOnShowListener { d.listView?.requestFocus() }
            d.setOnKeyListener { _,key,event ->
                val list=d.listView
                if(event.action!=KeyEvent.ACTION_DOWN || list==null) false
                else if(list.hasFocus() && (key==KeyEvent.KEYCODE_DPAD_LEFT || key==KeyEvent.KEYCODE_DPAD_RIGHT)) {
                    val button=if(key==KeyEvent.KEYCODE_DPAD_RIGHT && d.getButton(AlertDialog.BUTTON_POSITIVE)?.visibility==View.VISIBLE)
                        d.getButton(AlertDialog.BUTTON_POSITIVE) else d.getButton(AlertDialog.BUTTON_NEGATIVE)
                    (button ?: close).requestFocus(); true
                } else if(list.hasFocus() && key==KeyEvent.KEYCODE_DPAD_UP && list.selectedItemPosition<=0) {
                    close.requestFocus(); true
                } else if(!list.hasFocus() && key==KeyEvent.KEYCODE_DPAD_UP && !close.hasFocus()) {
                    list.requestFocus(); true
                } else if(close.hasFocus() && key==KeyEvent.KEYCODE_DPAD_DOWN) {
                    list.requestFocus(); true
                } else false
            }
            return d
        }
    }
    private fun rowBackground() = StateListDrawable().apply {
        fun shape(color: Int, stroke: Boolean) = GradientDrawable().apply {
            setColor(color); cornerRadius = dp(12).toFloat()
            if(stroke) setStroke(dp(2), Color.rgb(168,216,255))
        }
        addState(intArrayOf(android.R.attr.state_selected),shape(Color.rgb(44,69,98),true))
        addState(intArrayOf(android.R.attr.state_focused),shape(Color.rgb(44,69,98),true))
        addState(intArrayOf(android.R.attr.state_pressed),shape(Color.rgb(44,69,98),true))
        addState(intArrayOf(),shape(Color.TRANSPARENT,false))
    }
    private fun adapter(labels: List<String>) = object: BaseAdapter() {
        override fun getCount() = labels.size
        override fun getItem(position: Int) = labels[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, recycled: View?, parent: ViewGroup): View {
            val row = (recycled as? LinearLayout) ?: LinearLayout(c).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(18),dp(8),dp(18),dp(8))
                background = rowBackground()
                addView(TextView(c).apply { textSize=18f; setTextColor(Color.rgb(242,246,252)); setTypeface(null,Typeface.BOLD) })
                addView(TextView(c).apply { textSize=14f; setTextColor(Color.rgb(189,204,223)); setPadding(0,dp(4),0,0) })
            }
            val parts = labels[position].split('\n',limit=2)
            (row.getChildAt(0) as TextView).text=AppLanguage.text(parts[0])
            (row.getChildAt(1) as TextView).apply { text=AppLanguage.text(parts.getOrElse(1){""}); visibility=if(parts.size>1)View.VISIBLE else View.GONE }
            return row
        }
    }
    private fun menu(title: String, labels: List<String>, back: String = "Zavrieť", onBack: (() -> Unit)? = null, action: (Int) -> Unit) {
        val d = builder().setTitle(title).setAdapter(adapter(labels)) { _,i -> action(i) }
            .setNegativeButton(AppLanguage.text(back)) { _,_ -> onBack?.invoke() }.create()
        d.setOnShowListener { d.listView?.apply { setPadding(dp(12),dp(4),dp(12),dp(4)); clipToPadding=false; dividerHeight=dp(4); setSelector(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)); requestFocus() } }
        d.show()
    }
    private fun run(action: () -> Unit) {
        try { action(); changed() }
        catch (e: Exception) { Toast.makeText(c, e.message ?: "Úprava sa nepodarila.", Toast.LENGTH_LONG).show() }
    }
    private fun confirm(name: String, kind: String, action: () -> Unit) {
        val d = builder().setTitle(if(AppLanguage.code=="en")"Permanently delete $name?" else "Natrvalo odstrániť $name?")
            .setMessage(if(AppLanguage.code=="en")"This item will be permanently deleted from the TV. This cannot be undone." else "$kind sa natrvalo odstráni z TV. Túto akciu nemožno vrátiť.")
            .setNegativeButton(AppLanguage.text("Zrušiť"), null).setPositiveButton(AppLanguage.text("Odstrániť")) { _, _ -> run(action) }.create()
        d.setOnShowListener { d.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus() }; d.show()
    }
    private fun form(title: String, fields: List<Pair<String, String>>, save: (List<String>) -> Unit) {
        val layout = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL; setPadding(32,16,32,16) }
        val inputs = fields.map { (label, value) -> EditText(c).apply {
            hint = AppLanguage.text(label); setText(value); setSingleLine(true)
            showSoftInputOnFocus=false
            val keyboard = { requestFocus(); (c.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(this,InputMethodManager.SHOW_IMPLICIT); Unit }
            setOnClickListener { keyboard() }
            setOnKeyListener { _,key,event ->
                if(key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER) {
                    if(event.action==KeyEvent.ACTION_UP) keyboard(); true
                } else false
            }
            contentDescription = label
            layout.addView(TextView(c).apply { text=AppLanguage.text(label); textSize=13f; setTextColor(Color.rgb(168,216,255)) })
            layout.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 })
        } }
        val d = builder().setTitle(title).setView(layout)
            .setNegativeButton(AppLanguage.text("Zrušiť"), null).setPositiveButton(AppLanguage.text("Uložiť"), null).create()
        d.setOnShowListener { d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            try { save(inputs.map { it.text.toString() }); changed(); d.dismiss() }
            catch (e: Exception) { inputs.first().error = e.message ?: "Skontroluj údaje." }
        } }; d.show()
    }
    fun songs() {
        val rows = MediaLibraryStore.songs(c).sortedBy { it.title.lowercase() }
        val labels = rows.map { song ->
            val seconds = song.duration / 1000
            "${if(song.enabled) "✓" else "○"} ${song.title}\n${song.artist} · ${seconds / 60}:${(seconds % 60).toString().padStart(2,'0')}" +
                if(song.origin.isBlank()) "" else " · ${song.origin}"
        }
        menu("Skladby · spolu ${rows.size} · povolené ${rows.count { it.enabled }}", labels) { song(rows[it]) }
    }
    private fun song(item: LibrarySong) {
        menu(item.title, listOf("Upraviť údaje\nNázov, interpret a pôvod skladby",
            if(item.enabled) "Vyradiť z prehrávania\nSkladba zostane uložená" else "Povoliť prehrávanie\nZaradiť skladbu späť do výberu", "Priradiť k pozadiam\nVyber pozadia, pri ktorých má hrať", "Prehrať na ploche\nSpustí skladbu a vráti sa na plochu", "Odstrániť z TV\nTrvalé odstránenie vyžaduje potvrdenie"), "Späť do knižnice", { songs() }) { i ->
            when(i) {
                0 -> form("Upraviť skladbu", listOf("Názov" to item.title,"Interpret" to item.artist,"Pôvod skladby (film, seriál alebo hra)" to item.origin)) {
                    MediaLibraryStore.saveSong(c,item,it[0],it[1],it[2]) }
                1 -> run { MediaLibraryStore.toggleSong(c,item.id); songs() }
                2 -> songBindings(item)
                3 -> run { HomeMusic.requestSong(c,item.id); Actions.openCustomHome(activity); activity.finish() }
                4 -> confirm(item.title, "Skladba") { MediaLibraryStore.deleteSong(c,item.id); songs() }
            }
        }
    }
    private fun songBindings(item: LibrarySong) {
        val walls = WallpaperLibrary.all(c)
        val selected = walls.map { item.id in MediaLibraryStore.binding(c,it.id) }.toBooleanArray()
        val original = selected.copyOf()
        builder().setTitle("Priradiť k pozadiam · ${item.title}")
            .setMultiChoiceItems(walls.map { it.title }.toTypedArray(),selected) { _, i, value -> selected[i] = value }
            .setNegativeButton(AppLanguage.text("Zrušiť"),null).setPositiveButton(AppLanguage.text("Uložiť")) { _, _ -> run {
                walls.forEachIndexed { i,w -> if(selected[i] != original[i]) {
                    val ids = MediaLibraryStore.binding(c,w.id).toMutableList(); ids.remove(item.id)
                    if(selected[i]) ids.add(item.id); MediaLibraryStore.saveBinding(c,w.id,ids)
                } }
            } }.show()
    }
    fun wallpaper(item: WallpaperItem) {
        val options = mutableListOf("Upraviť názov", "Zmeniť kategóriu", "Priradené skladby")
        if(!item.builtIn) options.add("Odstrániť z TV")
        menu(item.title,options) { i -> when(i) {
            0 -> form("Názov pozadia",listOf("Názov" to item.title)) { MediaLibraryStore.saveWallpaper(c,item,it[0],item.collection) }
            1 -> {
                val groups = WallpaperLibrary.collections.filter { it != "Všetky" }
                builder().setTitle("Kategória").setSingleChoiceItems(groups.map{AppLanguage.text(it)}.toTypedArray(),groups.indexOf(item.collection)) { d,n ->
                    run { MediaLibraryStore.saveWallpaper(c,item,item.title,groups[n]) }; d.dismiss()
                }.setNegativeButton(AppLanguage.text("Zrušiť"),null).show()
            }
            2 -> wallpaperBindings(item)
            3 -> confirm(item.title, "Pozadie") { MediaLibraryStore.deleteWallpaper(c,item) }
        } }
    }
    private fun wallpaperBindings(item: WallpaperItem) {
        val songs = MediaLibraryStore.songs(c).sortedBy { it.title.lowercase() }
        val ids = MediaLibraryStore.binding(c,item.id).toMutableSet()
        builder().setTitle("Hudba k pozadiu · ${item.title}")
            .setMultiChoiceItems(songs.map { "${it.title} · ${it.artist}${if(it.enabled) "" else " (vyradená)"}" }.toTypedArray(),
                songs.map { it.id in ids }.toBooleanArray()) { _,i,on -> if(on) ids.add(songs[i].id) else ids.remove(songs[i].id) }
            .setNegativeButton(AppLanguage.text("Zrušiť"),null).setPositiveButton(AppLanguage.text("Uložiť")) { _, _ -> run { MediaLibraryStore.saveBinding(c,item.id,ids.toList()) } }.show()
    }
}
