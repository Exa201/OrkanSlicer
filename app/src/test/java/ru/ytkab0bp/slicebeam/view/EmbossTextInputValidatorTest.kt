package ru.ytkab0bp.slicebeam.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmbossTextInputValidatorTest {
    @Test
    fun acceptsTrimmedTextAndLocalizedPositiveDimensions() {
        val result = EmbossTextInputValidator.validate("  ORKAN  ", "12,5", "0,8")

        assertNull(result.error)
        assertEquals(EmbossTextInputValidator.Parameters("ORKAN", 12.5f, 0.8f), result.value)
    }

    @Test
    fun rejectsEmptyAndOverlongText() {
        assertEquals(
            EmbossTextInputValidator.Error.EMPTY_TEXT,
            EmbossTextInputValidator.validate("   ", "10", "1").error,
        )
        assertEquals(
            EmbossTextInputValidator.Error.TEXT_TOO_LONG,
            EmbossTextInputValidator.validate("a".repeat(257), "10", "1").error,
        )
    }

    @Test
    fun rejectsNonPositiveNonFiniteAndMalformedDimensions() {
        listOf("0", "-1", "NaN", "Infinity", "bad").forEach { value ->
            assertEquals(
                "font size $value",
                EmbossTextInputValidator.Error.INVALID_SIZE,
                EmbossTextInputValidator.validate("text", value, "1").error,
            )
            assertEquals(
                "depth $value",
                EmbossTextInputValidator.Error.INVALID_DEPTH,
                EmbossTextInputValidator.validate("text", "10", value).error,
            )
        }
    }
}
