package com.conreo.couchytv

import org.junit.Test
import org.junit.Assert.*

class PortablePolicyTest {
    @Test fun resolutionsPreserveReachableDesign() {
        for((w,h) in listOf(640f to 360f,960f to 540f,1280f to 720f,1920f to 1080f,3840f to 2160f,853f to 480f,1280f to 800f)) {
            val scale=DisplayPolicy.fit(w,h)
            assertTrue(960f*scale<=w+.01f);assertTrue(540f*scale<=h+.01f)
            assertTrue(scale>0)
        }
    }
    @Test fun invalidViewportRejected(){for(v in listOf(0f,-1f,Float.NaN,Float.POSITIVE_INFINITY)){try{DisplayPolicy.fit(v,540f);fail("invalid size")}catch(expected:IllegalArgumentException){}}}
    @Test fun selectionFitsAfterScrollAcrossHeights() {
        for(viewport in listOf(200f,310f,540f))for(size in listOf(60f,100f,180f))for(offset in listOf(-100f,0f,40f,180f,500f)) {
            val shift=DisplayPolicy.scrollDistance(offset,size,viewport,8f)
            assertTrue(offset-shift>=7.99f)
            assertTrue(offset-shift+size<=viewport-7.99f)
        }
    }
    @Test fun alreadyVisibleDoesNotJump(){assertEquals(0f,DisplayPolicy.scrollDistance(20f,100f,300f,8f),0f)}
    @Test fun tallCardAlignsStart(){assertEquals(12f,DisplayPolicy.scrollDistance(20f,500f,300f,8f),0f)}
    @Test fun weatherConversions(){assertEquals(32.0,WeatherUnits.temperature(0.0,true),.001);assertEquals(-40.0,WeatherUnits.temperature(-40.0,true),.001);assertEquals(20.0,WeatherUnits.temperature(20.0,false),.001);assertEquals(60.0,WeatherUnits.wind(96.56064,true),.001)}
    @Test fun themedQueueExhaustsBeforeRandom() {
        val q=SoundtrackQueue{it.last()};q.reset(listOf("a","b","b","missing"))
        assertEquals("b",q.next(listOf("a","b","c"),true));assertEquals("a",q.next(listOf("a","b","c"),true));assertEquals("c",q.next(listOf("a","b","c"),true))
    }
    @Test fun noImmediateMusicRepeatsAcrossLongSession(){val q=SoundtrackQueue{it.first()};q.reset(listOf("a","b"));var last:String?=null;repeat(1000){val n=q.next(listOf("a","b","c"),true);assertNotEquals(last,n);last=n}}
    @Test fun singleSongStopsInsteadOfLooping(){val q=SoundtrackQueue();q.reset(listOf("a"));assertEquals("a",q.next(listOf("a"),true));assertNull(q.next(listOf("a"),true))}
    @Test fun disabledTracksNeverReturned(){val q=SoundtrackQueue();q.reset(listOf("disabled"));repeat(50){assertNotEquals("disabled",q.next(listOf("a","b"),true))}}
    @Test fun memoryCycleUsesEveryPhotoAndAvoidsBoundaryRepeat(){val ids=setOf("a","b","c");var seen=emptySet<String>();var last:String?=null;val cycle=mutableSetOf<String>();repeat(90){val next=MemoryRotation.next(ids,seen,last)!!;assertNotEquals(last,next.selected);cycle.add(next.selected);if(it%3==2){assertEquals(ids,cycle);cycle.clear()};seen=next.seen;last=next.selected}}
    @Test fun removedPhotosCannotReappear(){val choice=MemoryRotation.next(setOf("a"),setOf("deleted"),"deleted")!!;assertEquals("a",choice.selected);assertEquals(setOf("a"),choice.seen)}
    @Test fun emptyAlbumSafe(){assertNull(MemoryRotation.next(emptySet(),setOf("a"),"a"))}
}
