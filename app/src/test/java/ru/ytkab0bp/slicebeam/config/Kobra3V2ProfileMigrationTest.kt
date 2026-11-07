package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Kobra3V2ProfileMigrationTest {
    @Test
    fun removesOnlyTheBundledFixedPressureAdvanceCommand() {
        val source = "G9111\r\nM117\r\nM900 K0.051\r\nG90\r\n"

        assertEquals("G9111\r\nM117\r\nG90\r\n", Kobra3V2ProfileMigration.removeLegacyPressureAdvance(source))
    }

    @Test
    fun preservesOtherPressureAdvanceValuesAndIsIdempotent() {
        val source = "M900 K0.075\nM900 K0.051 T1\nG90\n"

        val migrated = Kobra3V2ProfileMigration.removeLegacyPressureAdvance(source)
        assertEquals(source, migrated)
        assertEquals(migrated, Kobra3V2ProfileMigration.removeLegacyPressureAdvance(migrated))
    }

    @Test
    fun removesTheLegacyValueWithToolParametersAndEquivalentDecimalFormatting() {
        val source = "G90\nM900 S0 K0.051 T0 ; legacy V2 value\nM900 K.0510\nM900 K0.0515\n"

        assertEquals("G90\nM900 K0.0515\n", Kobra3V2ProfileMigration.removeLegacyPressureAdvance(source))
    }

    @Test
    fun detectsRenamedKobra3V2PrinterProfiles() {
        assertTrue(Kobra3V2ProfileMigration.isKobra3V2Profile("Custom Kobra", "Anycubic Kobra 3 V2"))
        assertTrue(Kobra3V2ProfileMigration.isKobra3V2Profile("Anycubic Kobra 3 v2 0.4 nozzle - Stock", null))
        assertFalse(Kobra3V2ProfileMigration.isKobra3V2Profile("Anycubic Kobra 3 0.4 nozzle", "Anycubic Kobra 3"))
    }
}
