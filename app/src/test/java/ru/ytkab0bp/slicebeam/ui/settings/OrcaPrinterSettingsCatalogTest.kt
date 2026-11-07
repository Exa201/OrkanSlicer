package ru.ytkab0bp.slicebeam.ui.settings

import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrcaPrinterSettingsCatalogTest {
    @Test
    fun fixedAndExtruderGroupsHaveUniqueNonEmptySections() {
        val fixed = (OrcaPrinterSettingsCatalog.leadingCategories() + OrcaPrinterSettingsCatalog.trailingCategories())
        val fixedKeys = fixed.flatMap { category -> category.sections.flatMap { it.keys } }
        val extruderKeys = OrcaPrinterSettingsCatalog.extruderSections().flatMap { it.keys }

        assertTrue("Repeated fixed printer options", OrcaPrinterSettingsCatalog.hasUniqueFixedKeys())
        assertTrue("Repeated per-extruder options", OrcaPrinterSettingsCatalog.hasUniqueExtruderKeys())
        assertTrue(fixed.all { it.title.isNotBlank() && it.sections.isNotEmpty() && it.sections.all { s -> s.title.isNotBlank() && s.keys.isNotEmpty() } })
        assertTrue(OrcaPrinterSettingsCatalog.extruderSections().all { it.title.isNotBlank() && it.keys.isNotEmpty() })
        assertFalse("Fixed and per-extruder keys must not overlap", fixedKeys.any { it in extruderKeys })
    }

    @Test
    fun printerSpecificValuesStayOutOfFilamentAndProcessProfiles() {
        val printerKeys = (
            OrcaPrinterSettingsCatalog.fixedKeys() +
                OrcaPrinterSettingsCatalog.extruderSections().flatMap { it.keys }
            ).toSet()
        val filamentKeys = OrcaFilamentSettingsCatalog.filamentKeys().toSet()
        val processKeys = OrcaProcessSettingsCatalog.processKeys().toSet()

        assertTrue("z_offset belongs to printer", "z_offset" in printerKeys)
        assertFalse("z_offset must not be duplicated in filament", "z_offset" in filamentKeys)
        assertFalse("z_offset must not be duplicated in process", "z_offset" in processKeys)
        assertTrue("toolchange retraction belongs to printer extruders", "retract_length_toolchange" in printerKeys)
        assertTrue("toolchange retraction belongs to printer extruders", "retract_restart_extra_toolchange" in printerKeys)
        assertTrue("chamber control support belongs to printer capability", "support_chamber_temp_control" in printerKeys)
        assertTrue("air filtration support belongs to printer capability", "support_air_filtration" in printerKeys)
        assertFalse("printer capabilities must not be duplicated in filament", "support_chamber_temp_control" in filamentKeys)
        assertFalse("printer capabilities must not be duplicated in process", "support_air_filtration" in processKeys)
        assertTrue(Slic3rConfigWrapper.PRINTER_CONFIG_KEYS.containsAll(
            listOf("support_chamber_temp_control", "support_air_filtration"),
        ))
    }

    @Test
    fun accelerationLimitsAreNotMislabelledAsFeedrates() {
        val sections = OrcaPrinterSettingsCatalog.leadingCategories().flatMap { it.sections }
        val accelerationSection = sections.single { it.title == "Acceleration limits" }
        val accelerationKeys = setOf(
            "machine_max_acceleration_x",
            "machine_max_acceleration_y",
            "machine_max_acceleration_z",
            "machine_max_acceleration_e",
            "machine_max_acceleration_extruding",
            "machine_max_acceleration_retracting",
        )

        assertTrue(accelerationSection.keys.containsAll(accelerationKeys))
        assertFalse(sections.any { it.title == "Maximum feedrates" })
    }
}
