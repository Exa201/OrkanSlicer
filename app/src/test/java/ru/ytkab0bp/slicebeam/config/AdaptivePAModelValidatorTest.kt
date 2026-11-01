package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdaptivePAModelValidatorTest {
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
