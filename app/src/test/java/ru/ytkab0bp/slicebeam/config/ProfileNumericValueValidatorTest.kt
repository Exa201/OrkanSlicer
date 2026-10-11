package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileNumericValueValidatorTest {
    @Test
    fun acceptsInclusiveFloatBounds() {
        assertEquals(ProfileNumericValueValidator.Result.VALID,
            ProfileNumericValueValidator.validate("0.05", 0.05f, 0.3f, false))
        assertEquals(ProfileNumericValueValidator.Result.VALID,
            ProfileNumericValueValidator.validate("0.3", 0.05f, 0.3f, false))
    }

    @Test
    fun rejectsNonNumericAndNonFiniteValues() {
        for (value in listOf("", "NaN", "Infinity", "1.2.3", "1,2,3")) {
            assertEquals(value, ProfileNumericValueValidator.Result.INVALID_NUMBER,
                ProfileNumericValueValidator.validate(value, 0f, 10f, false))
        }
    }

    @Test
    fun distinguishesValuesOutsideOrcaRange() {
        assertEquals(ProfileNumericValueValidator.Result.OUT_OF_RANGE,
            ProfileNumericValueValidator.validate("0.04", 0.05f, 0.3f, false))
        assertEquals(ProfileNumericValueValidator.Result.OUT_OF_RANGE,
            ProfileNumericValueValidator.validate("0.31", 0.05f, 0.3f, false))
    }

    @Test
    fun integerSettingsRejectFractionalInput() {
        assertEquals(ProfileNumericValueValidator.Result.VALID,
            ProfileNumericValueValidator.validate("-2", -2f, 10f, true))
        assertEquals(ProfileNumericValueValidator.Result.INVALID_NUMBER,
            ProfileNumericValueValidator.validate("1.5", -2f, 10f, true))
    }

    @Test
    fun germanDecimalCommaIsNormalizedForOrcaStorage() {
        assertEquals(ProfileNumericValueValidator.Result.VALID,
            ProfileNumericValueValidator.validate("0,15", 0f, 1f, false))
        assertEquals("0.15", ProfileNumericValueValidator.normalizeForStorage(" 0,15 ", false))
    }

    @Test
    fun signToggleSupportsTypingNegativeValuesWithNumericKeyboard() {
        assertEquals("-0.5", ProfileNumericValueValidator.toggleSign("0.5"))
        assertEquals("0.5", ProfileNumericValueValidator.toggleSign("-0.5"))
        assertEquals("-", ProfileNumericValueValidator.toggleSign(""))
    }

    @Test
    fun percentageValuesKeepTheirSuffixAndNormalizeGermanDecimals() {
        assertEquals("12.5%", ProfileNumericValueValidator.normalizeForStorage(" 12,5 ", false, true))
        assertEquals("12.5%", ProfileNumericValueValidator.normalizeForStorage("12,5%", false, true))
        assertEquals("12.5", ProfileNumericValueValidator.normalizeForStorage("12,5%", false, false))
    }
}
