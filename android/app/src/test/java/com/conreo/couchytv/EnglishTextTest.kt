package com.conreo.couchytv

import org.junit.Assert.assertEquals
import org.junit.Test

class EnglishTextTest {
    private val text = EnglishText(mapOf("Rozpozerané" to "Continue watching", "Všetky" to "All", "Náhodný výber z" to "Shuffle category", "{0} skladieb" to "{0} tracks", "{0} povolených" to "{0} enabled", "Zbierka {0} / {1}" to "Collection {0} / {1}"))
    @Test fun keepsNavigationMarkersAndSpaces() = assertEquals("Continue watching  ›", text.text("Rozpozerané  ›"))
    @Test fun translatesDynamicCounts() = assertEquals("3 tracks · 2 enabled", text.text("3 skladieb · 2 povolených"))
    @Test fun translatesCategoryValue() = assertEquals("Shuffle category: All", text.text("Náhodný výber z: Všetky"))
    @Test fun translatesNumericSlots() = assertEquals("Collection 1 / 20", text.text("Zbierka 1 / 20"))
    @Test fun preservesUnknownMediaMetadata() = assertEquals("坂本龍一 · My album: 2026", text.text("坂本龍一 · My album: 2026"))
    @Test fun onlyMatchesNumbersInTemplates() = assertEquals("Blue Bird skladieb", text.text("Blue Bird skladieb"))
}
