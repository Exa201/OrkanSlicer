package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileOpenOptionResolverTest {
    @Test
    fun pairsNativeLabelsAndValuesWithoutChangingStoredValues() {
        val choices = ProfileOpenOptionResolver.choices(
            arrayOf("Standard", "High flow"), arrayOf("normal", "high_flow"), false,
        )
        assertEquals(listOf(
            ProfileOpenOptionResolver.Choice("Standard", "normal"),
            ProfileOpenOptionResolver.Choice("High flow", "high_flow"),
        ), choices)
    }

    @Test
    fun numericOpenEnumCanUseIndexValuesWhenOrcaOnlyProvidesLabels() {
        val choices = ProfileOpenOptionResolver.choices(
            arrayOf("Standard", "Tool 1", "Tool 2"), emptyArray(), true,
        )
        assertEquals(listOf("0", "1", "2"), choices.map { it.value })
        assertEquals("Tool 2", ProfileOpenOptionResolver.displayValue("2", choices))
    }

    @Test
    fun missingLabelsFallBackToTheNativeValue() {
        val choices = ProfileOpenOptionResolver.choices(
            arrayOf("First"), arrayOf("a", "b"), false,
        )
        assertEquals(listOf("First", "b"), choices.map { it.label })
        assertEquals(listOf("a", "b"), choices.map { it.value })
    }

    @Test
    fun unknownCustomValuesRemainVisibleAndAreNotReplaced() {
        val choices = ProfileOpenOptionResolver.choices(
            arrayOf("PLA", "PETG"), arrayOf("PLA", "PETG"), false,
        )
        assertEquals("ASA", ProfileOpenOptionResolver.displayValue("ASA", choices))
    }

    @Test
    fun emptyMetadataDoesNotInventChoices() {
        assertEquals(emptyList<ProfileOpenOptionResolver.Choice>(),
            ProfileOpenOptionResolver.choices(null, null, true))
    }
}
