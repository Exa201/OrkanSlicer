package ru.ytkab0bp.slicebeam.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrcaProfileGroupLocalizerTest {
    @Test
    fun sharedGermanGroupLabelsAreUniqueAndConsistent() {
        assertTrue(OrcaProfileGroupLocalizer.hasUniqueGermanKeys())
        assertEquals("Druckvorschub", OrcaProfileGroupLocalizer.mobileGroup("Pressure advance", "de"))
        assertEquals("Druckvorschub", OrcaProfileGroupLocalizer.filamentGroup("Pressure advance", "de"))
        assertEquals("Beschleunigungsgrenzen", OrcaProfileGroupLocalizer.mobileGroup("Acceleration limits", "de"))
        assertEquals("Profilkompatibilität", OrcaProfileGroupLocalizer.mobileGroup("Profile dependencies", "de"))
    }

    @Test
    fun englishMobileLabelsKeepTheirOrcaText() {
        assertEquals("Acceleration limits", OrcaProfileGroupLocalizer.mobileGroup("Acceleration limits", "en"))
        assertEquals("", OrcaProfileGroupLocalizer.mobileGroup(null, "de"))
    }

    @Test
    fun germanSearchWordsResolveToOrcaOptionTerms() {
        assertEquals(
            "layer height support infill cooling retract",
            OrcaProfileGroupLocalizer.expandGermanSearch("schichthöhe stütz füll kühl rückzug"),
        )
    }
}
