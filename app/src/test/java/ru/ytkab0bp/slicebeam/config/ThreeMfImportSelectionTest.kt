package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.*
import org.junit.After
import org.junit.Test
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef.ConfigOptionType
import ru.ytkab0bp.slicebeam.utils.FilamentSlot

class ThreeMfImportSelectionTest {
    private val source = ThreeMfProjectSettings(
        sourceName = "colors.3mf",
        process = mapOf("layer_height" to "0.24", "curr_bed_type" to "Textured PEI Plate"),
        filaments = listOf(mapOf("filament_colour" to "#112233", "filament_type" to "PETG",
            "nozzle_temperature" to "240", "pressure_advance" to "0.05")),
        filamentTypes = mapOf("filament_colour" to ConfigOptionType.STRINGS,
            "filament_type" to ConfigOptionType.STRINGS, "nozzle_temperature" to ConfigOptionType.INTS,
            "pressure_advance" to ConfigOptionType.FLOATS),
        printerTitle = "File printer",
    )
    private val own = listOf(FilamentSlot(0xFF778899.toInt(), "PLA"))

    @After fun clear() = TemporaryProjectSettings.clear()

    @Test fun defaultKeepsOwnParametersButShowsModelColors() {
        val chosen = requireNotNull(ThreeMfImportSelection.select(source, false, false, own, false))
        TemporaryProjectSettings.activate(chosen)
        val effective = ConfigObject().apply {
            put("layer_height", "0.16")
            put("curr_bed_type", "Cool Plate")
            put("nozzle_temperature", "205")
            put("pressure_advance", "0.02")
        }
        TemporaryProjectSettings.applyTo(effective)
        assertEquals("0.16", effective["layer_height"])
        assertEquals("Cool Plate", effective["curr_bed_type"])
        assertEquals("205", effective["nozzle_temperature"])
        assertEquals("0.02", effective["pressure_advance"])
        assertEquals("#112233", TemporaryProjectSettings.slots()?.first()?.let { TemporaryProjectSettings.colorText(it.color) })
        assertEquals("PLA", effective["filament_type"])
        assertFalse(TemporaryProjectSettings.owns(ConfigObject.PROFILE_LIST_PRINT))
        assertFalse(TemporaryProjectSettings.owns(ConfigObject.PROFILE_LIST_FILAMENT))
    }

    @Test fun printOnlyDoesNotImportPlateOrFilament() {
        val chosen = requireNotNull(ThreeMfImportSelection.select(source, true, false, own, false))
        assertEquals(mapOf("layer_height" to "0.24"), chosen.process)
        assertNull(chosen.filaments[0]["nozzle_temperature"])
        assertTrue(chosen.printSettingsImported)
        assertFalse(chosen.filamentSettingsImported)
    }

    @Test fun filamentOnlyIncludesFileTemperaturesButNotPrintOrPrinter() {
        val chosen = requireNotNull(ThreeMfImportSelection.select(source, false, true, own, false))
        assertTrue(chosen.process.isEmpty())
        assertEquals("240", chosen.filaments[0]["nozzle_temperature"])
        assertFalse(chosen.preferOwnFilamentTemperatures)
        val ownProfile = ConfigObject("Own PLA").apply { put("nozzle_temperature", "205") }
        TemporaryProjectSettings.activate(chosen, listOf(ownProfile))
        val effective = ConfigObject(ownProfile)
        TemporaryProjectSettings.applyTo(effective)
        assertEquals("240", effective["nozzle_temperature"])
        assertTrue(TemporaryProjectSettings.owns(ConfigObject.PROFILE_LIST_FILAMENT))
        assertFalse(TemporaryProjectSettings.owns(ConfigObject.PROFILE_LIST_PRINT))
    }

    @Test fun printerOnlyImportsPlateChoiceButNoOtherFileValues() {
        val chosen = requireNotNull(ThreeMfImportSelection.select(source, false, false, own, true))
        assertEquals(mapOf("curr_bed_type" to "Textured PEI Plate"), chosen.process)
        assertFalse(chosen.printSettingsImported)
        assertFalse(chosen.filamentSettingsImported)
    }
}
