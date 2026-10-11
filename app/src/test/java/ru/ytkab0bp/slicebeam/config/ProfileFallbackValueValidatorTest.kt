package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef

class ProfileFallbackValueValidatorTest {
    @Test
    fun enforcesScalarIntegerAndPercentRanges() {
        val integer = definition(ConfigOptionDef.ConfigOptionType.INT)
        assertEquals(ProfileFallbackValueValidator.Result.VALID, ProfileFallbackValueValidator.validate(integer, "20"))
        assertEquals(ProfileFallbackValueValidator.Result.OUT_OF_RANGE, ProfileFallbackValueValidator.validate(integer, "21"))
        assertEquals(ProfileFallbackValueValidator.Result.INVALID_NUMBER, ProfileFallbackValueValidator.validate(integer, "2.5"))

        val percent = definition(ConfigOptionDef.ConfigOptionType.PERCENT, minimum = 0f, maximum = 100f)
        assertEquals(ProfileFallbackValueValidator.Result.VALID, ProfileFallbackValueValidator.validate(percent, "100%"))
        assertEquals(ProfileFallbackValueValidator.Result.INVALID_NUMBER, ProfileFallbackValueValidator.validate(percent, "100"))
        assertEquals(ProfileFallbackValueValidator.Result.OUT_OF_RANGE, ProfileFallbackValueValidator.validate(percent, "100.1%"))
    }

    @Test
    fun validatesNumericVectorsWithoutAcceptingEmptyEntries() {
        val vector = definition(ConfigOptionDef.ConfigOptionType.FLOATS, minimum = -10f, maximum = 10f)
        assertEquals(ProfileFallbackValueValidator.Result.VALID, ProfileFallbackValueValidator.validate(vector, "1.5, -2"))
        assertEquals(ProfileFallbackValueValidator.Result.INVALID_VECTOR, ProfileFallbackValueValidator.validate(vector, "1,,2"))
        assertEquals(ProfileFallbackValueValidator.Result.OUT_OF_RANGE, ProfileFallbackValueValidator.validate(vector, "1, 11"))
    }

    @Test
    fun nullableVectorsAcceptOnlyExplicitOrcaNilMarkers() {
        val nullable = definition(ConfigOptionDef.ConfigOptionType.FLOATS, minimum = 0f, maximum = 100f).apply {
            this.nullable = true
        }
        val regular = definition(ConfigOptionDef.ConfigOptionType.FLOATS, minimum = 0f, maximum = 100f)

        assertEquals(ProfileFallbackValueValidator.Result.VALID, ProfileFallbackValueValidator.validate(nullable, "nil,25"))
        assertEquals(ProfileFallbackValueValidator.Result.INVALID_NUMBER, ProfileFallbackValueValidator.validate(regular, "nil,25"))
    }

    @Test
    fun leavesFreeTextAndOrcaEnumValuesUntouched() {
        val string = definition(ConfigOptionDef.ConfigOptionType.STRINGS)
        val enum = definition(ConfigOptionDef.ConfigOptionType.ENUM)
        assertEquals(ProfileFallbackValueValidator.Result.VALID, ProfileFallbackValueValidator.validate(string, "G1 X1\nG1 X2"))
        assertEquals(ProfileFallbackValueValidator.Result.VALID, ProfileFallbackValueValidator.validate(enum, "custom"))
    }

    private fun definition(type: ConfigOptionDef.ConfigOptionType, minimum: Float = -20f, maximum: Float = 20f) =
        ConfigOptionDef().apply {
            this.type = type
            min = minimum
            max = maximum
        }
}
