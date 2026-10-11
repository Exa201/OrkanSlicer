package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfilePointValueValidatorTest {
    @Test
    fun acceptsAndNormalizesTwoDimensionalPoint() {
        assertEquals(
            ProfilePointValueValidator.Result.VALID,
            ProfilePointValueValidator.validate("1,5; 2,5", ProfilePointValueValidator.Kind.POINT),
        )
        assertEquals(
            "1.5,2.5",
            ProfilePointValueValidator.normalizeForStorage("1,5; 2,5", ProfilePointValueValidator.Kind.POINT),
        )
    }

    @Test
    fun acceptsStoredTwoDimensionalAndThreeDimensionalPoints() {
        assertEquals(
            ProfilePointValueValidator.Result.VALID,
            ProfilePointValueValidator.validate("12.5,4", ProfilePointValueValidator.Kind.POINT),
        )
        assertEquals("1; 2; 3",
            ProfilePointValueValidator.formatForEditor("1,2,3", ProfilePointValueValidator.Kind.POINT3))
        assertEquals(
            "1,2,3",
            ProfilePointValueValidator.normalizeForStorage("1; 2; 3", ProfilePointValueValidator.Kind.POINT3),
        )
    }

    @Test
    fun rejectsMissingNonnumericAndNonfiniteCoordinates() {
        assertEquals(
            ProfilePointValueValidator.Result.INVALID_COORDINATE,
            ProfilePointValueValidator.validate("1;", ProfilePointValueValidator.Kind.POINT),
        )
        assertEquals(
            ProfilePointValueValidator.Result.INVALID_COORDINATE,
            ProfilePointValueValidator.validate("1; nope; 3", ProfilePointValueValidator.Kind.POINT3),
        )
        assertEquals(
            ProfilePointValueValidator.Result.INVALID_COORDINATE,
            ProfilePointValueValidator.validate("NaN; 1", ProfilePointValueValidator.Kind.POINT),
        )
    }

    @Test
    fun pointVectorsRoundTripBetweenEditorAndOrcaFormat() {
        val stored = "0x0,200x0,200x200,0x200"
        val editor = ProfilePointValueValidator.formatForEditor(stored, ProfilePointValueValidator.Kind.POINTS)
        assertEquals("0;0\n200;0\n200;200\n0;200", editor)
        assertEquals(ProfilePointValueValidator.Result.VALID,
            ProfilePointValueValidator.validate(editor, ProfilePointValueValidator.Kind.POINTS))
        assertEquals(stored,
            ProfilePointValueValidator.normalizeForStorage(editor, ProfilePointValueValidator.Kind.POINTS))
    }

    @Test
    fun pointVectorSupportsDecimalCommaAndEmptyLists() {
        val editor = "0,5; 0,5\n10,5; 20,5"
        assertEquals(ProfilePointValueValidator.Result.VALID,
            ProfilePointValueValidator.validate(editor, ProfilePointValueValidator.Kind.POINTS))
        assertEquals("0.5x0.5,10.5x20.5",
            ProfilePointValueValidator.normalizeForStorage(editor, ProfilePointValueValidator.Kind.POINTS))
        assertEquals(ProfilePointValueValidator.Result.VALID,
            ProfilePointValueValidator.validate("", ProfilePointValueValidator.Kind.POINTS))
        assertEquals("", ProfilePointValueValidator.normalizeForStorage("", ProfilePointValueValidator.Kind.POINTS))
    }

    @Test
    fun singlePointVectorAcceptsDecimalComma() {
        val editor = "1,25; 2,75"
        assertEquals(ProfilePointValueValidator.Result.VALID,
            ProfilePointValueValidator.validate(editor, ProfilePointValueValidator.Kind.POINTS))
        assertEquals("1.25x2.75",
            ProfilePointValueValidator.normalizeForStorage(editor, ProfilePointValueValidator.Kind.POINTS))
    }
}
