package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SliceInfoValuesTest {
    @Test
    fun roleTimelineUsesNormalizedVisibleDurationsAndEndsAtOne() {
        val values = sliceInfoSegmentValues(
            floatArrayOf(1f, 3f),
            booleanArrayOf(true, true),
        )
        assertArrayEquals(floatArrayOf(0f, 0.25f, 1f, 1f), values, 0.0001f)
    }

    @Test
    fun hiddenAndInvalidRoleDurationsDoNotCreateGapsOrDecreasingBoundaries() {
        val values = sliceInfoSegmentValues(
            floatArrayOf(2f, 4f, Float.NaN, 6f, -1f),
            booleanArrayOf(true, false, true, true, true),
        )
        assertArrayEquals(floatArrayOf(0f, 0.25f, 0.25f, 0.25f, 1f, 1f, 1f), values, 0.0001f)
        assertTrueMonotonic(values)
    }

    @Test
    fun emptyTimelineRemainsFiniteAndHasExpectedLength() {
        val values = sliceInfoSegmentValues(
            floatArrayOf(0f, Float.POSITIVE_INFINITY),
            booleanArrayOf(true, true),
        )
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 1f), values, 0f)
        assertTrueMonotonic(values)
    }

    @Test
    fun durationFormattingHandlesHoursAndZero() {
        assertEquals("1 h 2 min 3 s", SliceInfoFormatter.formatTime(3723f, "h", "min", "s"))
        assertEquals("0 s", SliceInfoFormatter.formatTime(0f, "h", "min", "s"))
    }

    @Test
    fun roleVisibilityCannotHideTheOnlyVisibleExtrusionRole() {
        assertTrue(canToggleSliceRole(2, currentlyVisible = true))
        assertFalse(canToggleSliceRole(1, currentlyVisible = true))
        assertTrue(canToggleSliceRole(0, currentlyVisible = false))
    }

    private fun assertTrueMonotonic(values: FloatArray) {
        assertEquals(0f, values.first(), 0f)
        assertEquals(1f, values.last(), 0f)
        for (index in 1 until values.size) {
            assertTrue("Timeline boundaries must be monotonic", values[index] >= values[index - 1])
            assertTrue("Timeline boundaries must be finite", values[index].isFinite())
        }
    }
}
