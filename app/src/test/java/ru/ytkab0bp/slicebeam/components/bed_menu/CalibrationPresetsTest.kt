package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationPresetsTest {
    @Test
    fun onlyModesWithBundledGcodeGenerationAreExposed() {
        assertEquals(
            listOf(1, 3, 6, 7, 8, 9),
            CalibrationPresets.supported().map(CalibrationPreset::mode),
        )
    }

    @Test
    fun defaultsAreValidAndKeepTemperatureWithinMaterialRange() {
        CalibrationPresets.supported().forEach { preset ->
            assertTrue(
                "Invalid default for mode ${preset.mode}",
                CalibrationPresets.validate(preset, preset.start, preset.endOrStep, preset.step),
            )
        }
        val temperature = CalibrationPresets.supported().first { it.mode == CalibrationPresets.TEMP_TOWER }
        assertEquals(210.0, temperature.start, 0.0)
        assertEquals(190.0, temperature.endOrStep, 0.0)
    }

    @Test
    fun rejectsNonFiniteOutOfRangeAndOversizedRequests() {
        val paLine = CalibrationPresets.supported().first { it.mode == CalibrationPresets.PA_LINE }
        assertFalse(CalibrationPresets.validate(paLine, Double.NaN, 0.1, 0.002))
        assertFalse(CalibrationPresets.validate(paLine, 0.0, 1.1, 0.002))
        assertFalse(CalibrationPresets.validate(paLine, 0.0, 0.1, 0.00001))

        val temp = CalibrationPresets.supported().first { it.mode == CalibrationPresets.TEMP_TOWER }
        assertFalse(CalibrationPresets.validate(temp, 330.0, 190.0, 5.0))

        val retract = CalibrationPresets.supported().first { it.mode == CalibrationPresets.RETRACTION_TOWER }
        assertFalse(CalibrationPresets.validate(retract, 30.0, 0.1, null))
    }
}
