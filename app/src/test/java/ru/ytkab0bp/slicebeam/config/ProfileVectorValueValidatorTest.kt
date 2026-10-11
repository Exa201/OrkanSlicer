package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileVectorValueValidatorTest {
    @Test
    fun acceptsNativeCommaSeparatedFloatVectors() {
        assertEquals(ProfileVectorValueValidator.Result.VALID,
            ProfileVectorValueValidator.validate("0.4, 0.2, 0.1", 0f, 1f, ProfileVectorValueValidator.Kind.FLOATS))
        assertEquals("0.4,0.2,0.1",
            ProfileVectorValueValidator.normalizeForStorage("0.4, 0.2, 0.1", ProfileVectorValueValidator.Kind.FLOATS))
    }

    @Test
    fun semicolonsAllowGermanDecimalCommasWithoutAmbiguousSplitting() {
        assertEquals(ProfileVectorValueValidator.Result.VALID,
            ProfileVectorValueValidator.validate("1,25; 2,5", 0f, 3f, ProfileVectorValueValidator.Kind.FLOATS))
        assertEquals("1.25,2.5",
            ProfileVectorValueValidator.normalizeForStorage("1,25; 2,5", ProfileVectorValueValidator.Kind.FLOATS))
    }

    @Test
    fun integerVectorsRejectFractionsAndPercentSigns() {
        assertEquals(ProfileVectorValueValidator.Result.VALID,
            ProfileVectorValueValidator.validate("2, 4", 0f, 10f, ProfileVectorValueValidator.Kind.INTS))
        assertEquals(ProfileVectorValueValidator.Result.INVALID_NUMBER,
            ProfileVectorValueValidator.validate("2, 4.5", 0f, 10f, ProfileVectorValueValidator.Kind.INTS))
        assertEquals(ProfileVectorValueValidator.Result.INVALID_NUMBER,
            ProfileVectorValueValidator.validate("2%, 4%", 0f, 10f, ProfileVectorValueValidator.Kind.INTS))
    }

    @Test
    fun percentVectorsStorePercentOnEveryEntry() {
        assertEquals(ProfileVectorValueValidator.Result.VALID,
            ProfileVectorValueValidator.validate("15, 25%", 0f, 100f, ProfileVectorValueValidator.Kind.PERCENTS))
        assertEquals("15%,25%",
            ProfileVectorValueValidator.normalizeForStorage("15, 25%", ProfileVectorValueValidator.Kind.PERCENTS))
    }

    @Test
    fun floatOrPercentVectorsPreserveMixedUnits() {
        assertEquals(ProfileVectorValueValidator.Result.VALID,
            ProfileVectorValueValidator.validate("0.4, 25%", 0f, 100f, ProfileVectorValueValidator.Kind.FLOATS_OR_PERCENTS))
        assertEquals("0.4,25%",
            ProfileVectorValueValidator.normalizeForStorage("0.4, 25%", ProfileVectorValueValidator.Kind.FLOATS_OR_PERCENTS))
    }

    @Test
    fun rejectsEmptyElementsNonFiniteValuesAndOutOfRangeEntries() {
        assertEquals(ProfileVectorValueValidator.Result.EMPTY,
            ProfileVectorValueValidator.validate("1,,2", 0f, 5f, ProfileVectorValueValidator.Kind.FLOATS))
        assertEquals(ProfileVectorValueValidator.Result.INVALID_NUMBER,
            ProfileVectorValueValidator.validate("NaN, 2", 0f, 5f, ProfileVectorValueValidator.Kind.FLOATS))
        assertEquals(ProfileVectorValueValidator.Result.OUT_OF_RANGE,
            ProfileVectorValueValidator.validate("1, 6", 0f, 5f, ProfileVectorValueValidator.Kind.FLOATS))
    }

    @Test
    fun nullableVectorsAllowAndNormalizeNilWithoutAllowingEmptyEntries() {
        val kind = ProfileVectorValueValidator.Kind.FLOATS
        assertEquals(ProfileVectorValueValidator.Result.VALID,
            ProfileVectorValueValidator.validate("nil, 0.35, nil", 0f, 1f, kind, allowNil = true))
        assertEquals("nil,0.35,nil",
            ProfileVectorValueValidator.normalizeForStorage("nil, 0.35, nil", kind, allowNil = true))
        assertEquals(ProfileVectorValueValidator.Result.INVALID_NUMBER,
            ProfileVectorValueValidator.validate("NIL, 0.35", 0f, 1f, kind, allowNil = true))
        assertEquals(ProfileVectorValueValidator.Result.INVALID_NUMBER,
            ProfileVectorValueValidator.validate("nil, 0.35", 0f, 1f, kind))
        assertEquals(ProfileVectorValueValidator.Result.EMPTY,
            ProfileVectorValueValidator.validate("nil,,0.35", 0f, 1f, kind, allowNil = true))
        assertEquals("Druckprofil · 0.35 …", ProfileVectorValueValidator.preview("nil,0.35,1", "Druckprofil"))
        assertEquals("nil,nil,nil", ProfileVectorValueValidator.inheritAll("1.2, 0.3, nil"))
    }

    @Test
    fun previewKeepsLongVectorsReadable() {
        assertEquals("1.0 · 2.0 …", ProfileVectorValueValidator.preview("1.0,2.0,3.0"))
        assertEquals("—", ProfileVectorValueValidator.preview(""))
    }
}
