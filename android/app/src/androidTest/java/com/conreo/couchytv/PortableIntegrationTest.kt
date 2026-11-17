package com.conreo.couchytv

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject

/** Offline fixture tests: never depend on live providers, TV credentials or personal photos. */
class PortableIntegrationTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    @Test fun testRssEncodingAndImage() {
        val xml="""<rss><channel><item><title>Živé správy</title><link>https://example.org/article</link><pubDate>Mon, 16 Nov 2026 10:00:00 +0000</pubDate><description>Čitateľný popis</description><enclosure url="https://example.org/image.jpg"/></item></channel></rss>"""
        val items=HomeNews.parse(xml,"Test","Test source")
        assertEquals(1,items.size);assertEquals("Živé správy",items[0].title);assertTrue(items[0].published>0);assertEquals("https://example.org/image.jpg",items[0].image)
    }
    @Test fun testAtomLinksAndDates() {
        val xml="""<feed xmlns="http://www.w3.org/2005/Atom"><entry><title>Atom</title><link rel="self" href="https://example.org/feed/1"/><link rel="alternate" href="https://example.org/article"/><updated>2026-11-16T10:00:00Z</updated><summary>Summary</summary></entry></feed>"""
        val items=HomeNews.parse(xml,"Test","Atom");assertEquals(1,items.size);assertEquals("https://example.org/article",items[0].url);assertTrue(items[0].published>0)
    }
    @Test fun testUnsafeLinksAndDoctypeRejected() {
        for(url in listOf("javascript:alert(1)","file:///sdcard/private","https://user:pass@example.org/","http://example.org/"))assertFalse(NewsSources.validUrl(url))
        try{HomeNews.parse("<!DOCTYPE rss [<!ENTITY x SYSTEM 'file:///private'>]><rss/>","Test");fail("DOCTYPE accepted")}catch(expected:IllegalArgumentException){}
    }
    @Test fun testLanguagePersistenceAndInternalIds() {
        val c=instrumentation.targetContext
        AppLanguage.load(c)
        try{AppLanguage.set(c,"en");assertEquals("Close",AppLanguage.text("Zavrieť"));AppLanguage.load(c);assertEquals("en",AppLanguage.code);assertEquals("movie_123",AppLanguage.text("movie_123"))}finally{AppLanguage.set(c,"sk")}
    }
    @Test fun testSourceCrudAndLimits() {
        val c=instrumentation.targetContext;NewsSources.load(c)
        val original=NewsSources.all
        val n=NewsSource(NewsSources.newId(),"https://example.org/rss","Test","Fixture")
        try{NewsSources.save(n);assertTrue(NewsSources.all.any{it.id==n.id});NewsSources.save(n.copy(enabled=false));assertFalse(NewsSources.all.first{it.id==n.id}.enabled);NewsSources.remove(n.id);assertEquals(original,NewsSources.all)}finally{NewsSources.remove(n.id)}
    }
    @Test fun testNoPersonalPhotoNotesBundled(){assertEquals(0,JSONObject(instrumentation.targetContext.assets.open("photo-notes.json").bufferedReader().use{it.readText()}).length())}
    @Test fun testNoDefaultJellyfinServer(){assertEquals("",JellyfinLink.server(instrumentation.targetContext))}
    @Test fun clearingSourcesDoesNotRestoreEditionPresets() {
        NewsSources.load(instrumentation.targetContext)
        val original=NewsSources.all
        try{NewsSources.reset();assertTrue(NewsSources.all.isEmpty())}finally{original.forEach{NewsSources.save(it)}}
    }
    @Test fun documentedScheduleApiHandlesBothShapesAndMissingArt() {
        val show="""{"name":"Fixture series","type":"Scripted","url":"https://www.tvmaze.com/shows/1/fixture","summary":"<b>Summary</b>","rating":{"average":8.2}}"""
        val raw="""[{"number":1,"season":2,"airdate":"2026-11-16","show":$show},{"number":2,"season":2,"airdate":"2026-11-17","show":$show},{"number":1,"season":1,"airdate":"2026-11-18","_embedded":{"show":$show}}]"""
        val items=Premieres.parseSchedule(raw)
        assertEquals(2,items.size);assertEquals("",items[0].image);assertEquals("Summary",items[0].description)
        assertTrue(items[0].source.contains("CC BY-SA"));assertTrue(items[0].rating.contains("8"))
        assertTrue(Premieres.parseSchedule("[]").isEmpty())
    }
    @Test fun editionMediaAndLegalNoticesArePackaged() {
        val c=instrumentation.targetContext
        assertEquals(EditionConfig.wallpapers.map{it.first}.toSet(),c.assets.list("wallpapers")!!.toSet())
        for(name in listOf("PRIVACY.md","THIRD-PARTY-NOTICES.md","CC-BY-4.0.txt","OFL-Cookie.txt","GPL-3.0.txt"))
            assertTrue(c.assets.open("legal/$name").use{it.readBytes().size}>100)
        assertEquals(EditionConfig.wallpapers.size,WallpaperLibrary.all(c).count{it.builtIn})
    }
    @Test fun translatedCountsWorkOnAndroid() {
        val c=instrumentation.targetContext;val original=AppLanguage.code
        try { AppLanguage.set(c,"en");assertEquals("12 tracks",AppLanguage.text("12 skladieb")) }
        finally {AppLanguage.set(c,original)}
    }
    @Test fun optionalPremiereSelectionPersists() {
        val c=instrumentation.targetContext;PremiereSources.load(c);val original=PremiereSources.selected()
        try {PremiereSources.save(setOf("tvmaze","cinema"));PremiereSources.load(c);assertEquals(setOf("tvmaze","cinema"),PremiereSources.selected());PremiereSources.save(emptySet());assertTrue(PremiereSources.selected().isEmpty())}
        finally {PremiereSources.save(original)}
    }
    @Test fun wallpaperRotationDefaultsOnAndPersistsOptOut() {
        val c=instrumentation.targetContext;val p=c.getSharedPreferences("wallpaper_cycle",0)
        val existed=p.contains("autoRotate");val original=WallpaperCycle.autoRotate(c)
        try {
            p.edit().remove("autoRotate").commit();assertTrue(WallpaperCycle.autoRotate(c))
            assertEquals(120_000L,WallpaperCycle.AUTO_INTERVAL_MS)
            WallpaperCycle.setAutoRotate(c,false);assertFalse(WallpaperCycle.autoRotate(c))
            WallpaperCycle.setAutoRotate(c,true);assertTrue(WallpaperCycle.autoRotate(c))
        } finally { if(existed)p.edit().putBoolean("autoRotate",original).commit() else p.edit().remove("autoRotate").commit() }
    }
    @Test fun testMusicImportValidationAndDeletion() {
        val c=instrumentation.targetContext
        val resolver=c.contentResolver
        val values=android.content.ContentValues().apply{put(android.provider.MediaStore.Audio.Media.DISPLAY_NAME,"couchy-fixture.wav");put(android.provider.MediaStore.Audio.Media.MIME_TYPE,"audio/wav")}
        val uri=resolver.insert(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values)!!
        var imported:String?=null
        val before=MediaLibraryStore.songs(c).map{it.id}.toSet()
        try {
            resolver.openOutputStream(uri)!!.use{out->c.assets.open("game-audio/coin.wav").use{it.copyTo(out)}}
            imported=MediaLibraryStore.importSong(c,uri)
            assertTrue(MediaLibraryStore.songs(c).any{it.id==imported && it.duration>0})
            MediaLibraryStore.deleteSong(c,imported)
            assertEquals(before,MediaLibraryStore.songs(c).map{it.id}.toSet())
            imported=null
            resolver.openOutputStream(uri,"wt")!!.use{it.write("not an audio file".toByteArray())}
            try {MediaLibraryStore.importSong(c,uri);fail("invalid audio accepted")}catch(expected:Exception){}
            assertEquals(before,MediaLibraryStore.songs(c).map{it.id}.toSet())
            assertFalse(java.io.File(c.getExternalFilesDir(null),"music").listFiles().orEmpty().any{it.name.endsWith(".part")})
        }finally{imported?.let{MediaLibraryStore.deleteSong(c,it)};resolver.delete(uri,null,null)}
    }
}
