package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.ytkab0bp.slicebeam.config.ConfigObject

class CalibrationResultPolicyTest {
    @Test
    fun supportedCalibrationModesTargetTheirOrcaProfiles() {
        assertEquals("pressure_advance", CalibrationResultPolicy.targetFor(CalibrationPresets.PA_LINE)?.key)
        assertEquals("pressure_advance", CalibrationResultPolicy.targetFor(CalibrationPresets.PA_TOWER)?.key)
        assertEquals("nozzle_temperature", CalibrationResultPolicy.targetFor(CalibrationPresets.TEMP_TOWER)?.key)
        assertEquals("filament_max_volumetric_speed", CalibrationResultPolicy.targetFor(CalibrationPresets.VOLUMETRIC_SPEED_TOWER)?.key)
        assertEquals("outer_wall_speed", CalibrationResultPolicy.targetFor(CalibrationPresets.VFA_TOWER)?.key)
        assertEquals("filament_retraction_length", CalibrationResultPolicy.targetFor(CalibrationPresets.RETRACTION_TOWER)?.key)
        assertEquals(ConfigObject.PROFILE_LIST_PRINT,
            CalibrationResultPolicy.targetFor(CalibrationPresets.VFA_TOWER)?.profileType)
    }

    @Test
    fun resultValuesAcceptLocaleDecimalsAndRejectOutOfRangeOrInvalidValues() {
        val pressureAdvance = requireNotNull(CalibrationResultPolicy.targetFor(CalibrationPresets.PA_LINE))
        assertEquals("0.042", CalibrationResultPolicy.parseValue("0,042", pressureAdvance))
        assertNull(CalibrationResultPolicy.parseValue("2.01", pressureAdvance))
        assertNull(CalibrationResultPolicy.parseValue("NaN", pressureAdvance))

        val temperature = requireNotNull(CalibrationResultPolicy.targetFor(CalibrationPresets.TEMP_TOWER))
        assertEquals("205", CalibrationResultPolicy.parseValue("205", temperature))
        assertNull(CalibrationResultPolicy.parseValue("205.5", temperature))
        assertNull(CalibrationResultPolicy.parseValue("99", temperature))
    }
}
