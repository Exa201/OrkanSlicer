package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdaptivePAModelValidatorTest {
    @Test
    fun sliceWarningsAreLimitedToUsedEnabledFilamentsAndMapSelfIndices() {
        val config = mapOf(
            "adaptive_pressure_advance_model" to ProfileStringVectorCodec.serialize(listOf("0,0,0", "0.04,10")),
            "adaptive_pressure_advance" to "1;1",
            "enable_pressure_advance" to "1;1",
            "filament_self_index" to "2;4",
        )

        assertEquals(
            listOf(AdaptivePAModelValidator.SliceWarning(
                filamentNumber = 4,
                lineNumber = 1,
                problem = AdaptivePAModelValidator.Problem.VALUE_COUNT,
            )),
            AdaptivePAModelValidator.sliceWarnings(config, setOf(3)),
        )
        assertEquals(emptyList<AdaptivePAModelValidator.SliceWarning>(),
            AdaptivePAModelValidator.sliceWarnings(config, setOf(0)))
    }

    @Test
    fun sliceWarningsIgnoreDisabledAdaptivePressureAdvance() {
        val config = mapOf(
            "adaptive_pressure_advance_model" to "0.04,10",
            "adaptive_pressure_advance" to "0",
            "enable_pressure_advance" to "1",
        )
        assertEquals(emptyList<AdaptivePAModelValidator.SliceWarning>(),
            AdaptivePAModelValidator.sliceWarnings(config, setOf(0)))
    }

    @Test
    fun warnsWhenAnEnabledUsedFilamentHasNoAdaptivePressureAdvanceMeasurements() {
        val emptyModel = mapOf(
            "adaptive_pressure_advance_model" to ProfileStringVectorCodec.serialize(listOf("")),
            "adaptive_pressure_advance" to "1",
            "enable_pressure_advance" to "1",
        )
        val defaultPlaceholder = emptyModel + ("adaptive_pressure_advance_model" to "0,0,0")

        assertEquals(
            listOf(AdaptivePAModelValidator.SliceWarning(
                filamentNumber = 1,
                lineNumber = null,
                problem = AdaptivePAModelValidator.Problem.NO_MEASUREMENTS,
            )),
            AdaptivePAModelValidator.sliceWarnings(emptyModel, setOf(0)),
        )
        assertEquals(
            listOf(AdaptivePAModelValidator.SliceWarning(
                filamentNumber = 1,
                lineNumber = null,
                problem = AdaptivePAModelValidator.Problem.NO_MEASUREMENTS,
            )),
            AdaptivePAModelValidator.sliceWarnings(defaultPlaceholder, setOf(0)),
        )
    }

    @Test
    fun warnsForMissingVariantColumnOnlyWhenThatUsedFilamentEnablesAdaptivePressureAdvance() {
        val config = mapOf(
            "adaptive_pressure_advance_model" to ProfileStringVectorCodec.serialize(listOf("0.04,10,3000")),
            "adaptive_pressure_advance" to "1;1",
            "enable_pressure_advance" to "1;1",
        )

        assertEquals(
            listOf(AdaptivePAModelValidator.SliceWarning(
                filamentNumber = 2,
                lineNumber = null,
                problem = AdaptivePAModelValidator.Problem.NO_MEASUREMENTS,
            )),
            AdaptivePAModelValidator.sliceWarnings(config, setOf(1)),
        )
        assertEquals(emptyList<AdaptivePAModelValidator.SliceWarning>(),
            AdaptivePAModelValidator.sliceWarnings(config, setOf(0)))
    }

    @Test
    fun acceptsWhitespaceAroundMeasurementValues() {
        assertNull(AdaptivePAModelValidator.firstEditedVariantIssue(
            listOf("0,0,0"), listOf(" 0.027 , 8.1416 , 2000.0 \n 0.025, 12, 5000 "),
        ))
    }

    @Test
    fun validatesOnlyTheEditedVariantAndReportsMeasurementLine() {
        assertEquals(
            AdaptivePAModelValidator.Issue(
                variantIndex = 1,
                lineNumber = 2,
                problem = AdaptivePAModelValidator.Problem.VALUE_COUNT,
            ),
            AdaptivePAModelValidator.firstEditedVariantIssue(
                listOf("0,0,0", "0,0,0"),
                listOf("0,0,0", "0.04, 10, 3000\n0.03, 12"),
            ),
        )
    }

    @Test
    fun untouchedDefaultPlaceholdersDoNotBlockAnEditedVariant() {
        assertNull(AdaptivePAModelValidator.firstEditedVariantIssue(
            listOf("0,0,0", "0,0,0"),
            listOf("0,0,0", "0.04,10,3000\n0.03,20,5000"),
        ))
    }

    @Test
    fun rejectsMalformedNumbersAndInvalidMeasurementOrdering() {
        val invalid = listOf(
            "0.04x,10,3000" to AdaptivePAModelValidator.Problem.INVALID_CHARACTERS,
            "0.04,,3000" to AdaptivePAModelValidator.Problem.INVALID_NUMBER,
            "2,10,3000" to AdaptivePAModelValidator.Problem.PA_TOO_HIGH,
            "0.04,0.04,3000" to AdaptivePAModelValidator.Problem.FLOW_NOT_GREATER_THAN_PA,
            "0.04,10,10" to AdaptivePAModelValidator.Problem.ACCELERATION_NOT_GREATER_THAN_FLOW,
        )
        invalid.forEach { (value, problem) ->
            assertEquals(value, problem, AdaptivePAModelValidator.firstEditedVariantIssue(
                listOf("0,0,0"), listOf(value),
            )?.problem)
        }
    }
}
