package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Test

class SliceChoiceFilterTest {
    @Test
    fun queryKeepsOriginalIndicesForProfileSelection() {
        val options = listOf("Draft 0.20", "Fine 0.12", "Fine 0.08")

        assertEquals(listOf(1, 2), SliceChoiceFilter.indices(options, "FINE"))
        assertEquals(listOf(2), SliceChoiceFilter.indices(options, "0.08"))
    }

    @Test
    fun blankQueryReturnsAllOptions() {
        assertEquals(listOf(0, 1), SliceChoiceFilter.indices(listOf("A", "B"), "  "))
    }
}
