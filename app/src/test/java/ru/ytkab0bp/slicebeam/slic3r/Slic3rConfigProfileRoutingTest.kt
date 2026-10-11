package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.ui.settings.OrcaFilamentSettingsCatalog
import ru.ytkab0bp.slicebeam.ui.settings.OrcaProcessSettingsCatalog

class Slic3rConfigProfileRoutingTest {
    @Test
    fun everyCurrentProcessSettingSurvivesProfileImportClassification() {
        val processKeys = OrcaProcessSettingsCatalog.processKeys()
        val importKeys = Slic3rConfigWrapper.PRINT_CONFIG_KEYS
        val missing = processKeys.filterNot(importKeys::contains)

        assertTrue("Unrouted process settings: ${missing.joinToString()}", missing.isEmpty())
        assertEquals(importKeys.size, importKeys.distinct().size)
    }

    @Test
    fun everyCurrentFilamentSettingSurvivesProfileImportClassification() {
        val filamentKeys = OrcaFilamentSettingsCatalog.filamentKeys()
        val importKeys = Slic3rConfigWrapper.FILAMENT_CONFIG_KEYS
        // The importer accepts either the Orca key or its legacy SliceBeam alias.
        val missing = filamentKeys.filterNot { key ->
            key in importKeys || ConfigObject.legacyKey(key) in importKeys
        }

        assertTrue("Unrouted filament settings: ${missing.joinToString()}", missing.isEmpty())
        assertEquals(importKeys.size, importKeys.distinct().size)
    }

    @Test
    fun nullableFilamentIroningOverridesAreVisibleAndRoutedToFilamentProfiles() {
        val expected = setOf(
            "filament_ironing_flow", "filament_ironing_spacing", "filament_ironing_inset", "filament_ironing_speed",
        )
        val catalogKeys = OrcaFilamentSettingsCatalog.filamentKeys().toSet()
        val importKeys = Slic3rConfigWrapper.FILAMENT_CONFIG_KEYS

        assertTrue("Filament ironing settings are missing from the catalog", catalogKeys.containsAll(expected))
        assertTrue("Filament ironing settings are missing from profile import routing", importKeys.containsAll(expected))
    }
}
