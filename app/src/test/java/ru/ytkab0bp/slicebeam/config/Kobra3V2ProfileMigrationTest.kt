package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
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
}
