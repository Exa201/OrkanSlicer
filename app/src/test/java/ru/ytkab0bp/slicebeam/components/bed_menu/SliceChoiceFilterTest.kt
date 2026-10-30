package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SliceChoiceFilterTest {
    @Test
    fun profileChoicesBecomeSearchableForLongListsOrLargeText() {
        assertFalse(SliceChoiceFilter.usesSearch(11, 1f))
        assertTrue(SliceChoiceFilter.usesSearch(12, 1f))
        assertTrue(SliceChoiceFilter.usesSearch(4, 1.3f))
        assertTrue(SliceChoiceFilter.usesSearch(4, 1.5f))
    }

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
