package ru.ytkab0bp.slicebeam.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbossTextInputValidatorTest {
    @Test
    fun leavingTextToolInvalidatesQueuedGlCallbacksAcrossRestart() {
        val gate = EmbossSessionGate()
        val firstSession = requireNotNull(gate.begin())

        assertTrue(gate.isCurrent(firstSession))
        assertEquals(null, gate.begin())
        assertEquals(true, gate.end())
        assertEquals(false, gate.isCurrent(firstSession))

        val secondSession = requireNotNull(gate.begin())
        assertTrue(gate.isCurrent(secondSession))
        assertEquals(false, gate.isCurrent(firstSession))
        assertEquals(true, gate.end())
        assertEquals(false, gate.end())
    }

    @Test
    fun embossModesKeepStandaloneAndSurfaceFollowingAsOneConsistentSnapshot() {
        val parameters = EmbossTextInputValidator.validate("Label", "10", "1")
        val standalone = EmbossModeOptions.create(parameters, addMode = true, standaloneMode = true, followSurface = true)
        val embossed = EmbossModeOptions.create(parameters, addMode = false, standaloneMode = false, followSurface = true)

        assertEquals(true, standalone.standaloneMode)
        assertEquals(true, standalone.followSurface)
        assertEquals(true, standalone.addMode)
        assertEquals(false, embossed.standaloneMode)
        assertEquals(false, embossed.followSurface)
        assertEquals(false, embossed.addMode)
    }

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
