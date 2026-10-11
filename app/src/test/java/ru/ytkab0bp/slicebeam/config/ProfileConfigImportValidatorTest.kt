package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef

class ProfileConfigImportValidatorTest {
    @Test fun nullableBooleansAcceptInheritanceButConcreteBooleansRejectIt() {
        val nullable = ConfigOptionDef().apply { key = "filament_retract_when_changing_layer"; type = ConfigOptionDef.ConfigOptionType.BOOLS; this.nullable = true }
        val profile = ConfigObject().apply { put(nullable.key, "nil,0,1") }
        assertTrue(ProfileConfigImportValidator.validate(listOf(profile), mapOf(nullable.key to nullable)).isEmpty())
        nullable.nullable = false
        assertEquals(1, ProfileConfigImportValidator.validate(listOf(profile), mapOf(nullable.key to nullable)).size)
    }
    @Test
    fun reportsInvalidAndOutOfRangeScalarValues() {
        val profile = ConfigObject("PLA").apply {
            put("nozzle_temperature", "hot")
            put("hot_plate_temp", "150")
            put("wall_loops", "2")
        }
        val warnings = ProfileConfigImportValidator.validate(
            listOf(profile),
            mapOf(
                "nozzle_temperature" to option("nozzle_temperature", ConfigOptionDef.ConfigOptionType.FLOAT, 150f, 300f),
                "hot_plate_temp" to option("hot_plate_temp", ConfigOptionDef.ConfigOptionType.INT, 0f, 120f),
                "wall_loops" to option("wall_loops", ConfigOptionDef.ConfigOptionType.INT, 1f, 10f),
            ),
        )

        assertEquals(
            listOf(
                ProfileConfigLoadWarning.InvalidValue(
                    "PLA", "nozzle_temperature", "hot", ProfileConfigLoadWarning.ValueIssue.INVALID_NUMBER,
                ),
                ProfileConfigLoadWarning.InvalidValue(
                    "PLA", "hot_plate_temp", "150", ProfileConfigLoadWarning.ValueIssue.OUT_OF_RANGE,
                ),
            ),
            warnings,
        )
    }

    @Test
    fun acceptsNativeEnumsAndLegacyIndicesButReportsUnknownChoices() {
        val profile = ConfigObject("Process").apply {
            put("fill_pattern", "grid")
            put("wall_generator", "1")
            put("sparse_infill_pattern", "not-a-pattern")
        }
        val warnings = ProfileConfigImportValidator.validate(
            listOf(profile),
            mapOf(
                "fill_pattern" to option("fill_pattern", ConfigOptionDef.ConfigOptionType.ENUM, values = arrayOf("lines", "grid")),
                "wall_generator" to option("wall_generator", ConfigOptionDef.ConfigOptionType.ENUM, values = arrayOf("classic", "arachne")),
                "sparse_infill_pattern" to option("sparse_infill_pattern", ConfigOptionDef.ConfigOptionType.ENUM, values = arrayOf("grid", "gyroid")),
            ),
        )

        assertEquals(1, warnings.size)
        assertEquals("sparse_infill_pattern", warnings.single().key)
        assertEquals(ProfileConfigLoadWarning.ValueIssue.UNKNOWN_ENUM, warnings.single().issue)
    }

    @Test
    fun validatesNumericVectorsAndAllowsBlankOptionalVectors() {
        val profile = ConfigObject("Machine").apply {
            put("nozzle_diameter", "0.4,broken")
            put("retract_length", "1, 99")
            put("empty_optional_vector", "")
        }
        val warnings = ProfileConfigImportValidator.validate(
            listOf(profile),
            mapOf(
                "nozzle_diameter" to option("nozzle_diameter", ConfigOptionDef.ConfigOptionType.FLOATS, 0.1f, 1f),
                "retract_length" to option("retract_length", ConfigOptionDef.ConfigOptionType.FLOATS, 0f, 10f),
                "empty_optional_vector" to option("empty_optional_vector", ConfigOptionDef.ConfigOptionType.INTS, 0f, 10f),
            ),
        )

        assertEquals(1, warnings.size)
        assertEquals("nozzle_diameter", warnings.single().key)
        assertEquals(ProfileConfigLoadWarning.ValueIssue.INVALID_NUMBER, warnings.single().issue)
    }

    @Test
    fun validatesNullableIroningOverridesAndRejectsNilForRegularVectors() {
        val profile = ConfigObject("PLA").apply {
            put("filament_ironing_flow", "nil,25,nil")
            put("filament_ironing_speed", "nil,45")
            put("nozzle_diameter", "nil,0.4")
            put("filament_ironing_spacing", "nil,,0.2")
        }
        val warnings = ProfileConfigImportValidator.validate(
            listOf(profile),
            mapOf(
                "filament_ironing_flow" to option("filament_ironing_flow", ConfigOptionDef.ConfigOptionType.PERCENTS, 0f, 100f, nullable = true),
                "filament_ironing_speed" to option("filament_ironing_speed", ConfigOptionDef.ConfigOptionType.FLOATS, 1f, 300f, nullable = true),
                "nozzle_diameter" to option("nozzle_diameter", ConfigOptionDef.ConfigOptionType.FLOATS, 0.1f, 1f),
                "filament_ironing_spacing" to option("filament_ironing_spacing", ConfigOptionDef.ConfigOptionType.FLOATS, 0f, 1f, nullable = true),
            ),
        )

        assertEquals(2, warnings.size)
        assertTrue(warnings.any { it.key == "nozzle_diameter" && it.issue == ProfileConfigLoadWarning.ValueIssue.INVALID_NUMBER })
        assertTrue(warnings.any { it.key == "filament_ironing_spacing" && it.issue == ProfileConfigLoadWarning.ValueIssue.INVALID_NUMBER })
    }

    @Test
    fun validatesBooleanVectorsAndCoordinates() {
        val profile = ConfigObject("Printer").apply {
            put("use_relative_e_distances", "yes")
            put("extruder_offset", "0,1,2")
            put("extruder_offsets", "0x0,1xbad")
        }
        val warnings = ProfileConfigImportValidator.validate(
            listOf(profile),
            mapOf(
                "use_relative_e_distances" to option("use_relative_e_distances", ConfigOptionDef.ConfigOptionType.BOOL),
                "extruder_offset" to option("extruder_offset", ConfigOptionDef.ConfigOptionType.POINT),
                "extruder_offsets" to option("extruder_offsets", ConfigOptionDef.ConfigOptionType.POINTS),
            ),
        )

        assertEquals(3, warnings.size)
        assertTrue(warnings.any { it.issue == ProfileConfigLoadWarning.ValueIssue.INVALID_BOOLEAN })
        assertEquals(2, warnings.count { it.issue == ProfileConfigLoadWarning.ValueIssue.INVALID_POINT })
    }

    @Test
    fun permitsCustomOpenEnumValuesAndDoesNotModifyImportedProfiles() {
        val profile = ConfigObject("Custom").apply { put("custom_speed", "137.5") }
        val originalValues = profile.values.toMap()
        val openEnum = option("custom_speed", ConfigOptionDef.ConfigOptionType.FLOAT, 10f, 200f).apply {
            guiType = ConfigOptionDef.GUIType.F_ENUM_OPEN
            enumValues = arrayOf("60", "100")
        }

        assertTrue(ProfileConfigImportValidator.validate(listOf(profile), mapOf("custom_speed" to openEnum)).isEmpty())
        assertEquals(originalValues, profile.values)
    }

    private fun option(
        key: String,
        type: ConfigOptionDef.ConfigOptionType,
        min: Float = Float.MIN_VALUE,
        max: Float = Float.MAX_VALUE,
        values: Array<String>? = null,
        nullable: Boolean = false,
    ) = ConfigOptionDef().apply {
        this.key = key
        this.type = type
        this.min = min
        this.max = max
        this.enumValues = values
        this.nullable = nullable
    }
}
