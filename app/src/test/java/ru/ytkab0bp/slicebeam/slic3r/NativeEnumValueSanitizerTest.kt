package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeEnumValueSanitizerTest {
    @Test
    fun preservesValuesAcceptedByBundledEngine() {
        assertEquals(
            "topmost",
            NativeEnumValueSanitizer.normalize("topmost", arrayOf("top", "topmost", "solid"), "top"),
        )
    }

    @Test
    fun replacesUnsupportedStoredValueWithEngineDefault() {
        assertEquals(
            "top",
            NativeEnumValueSanitizer.normalize("no ironing", arrayOf("top", "topmost", "solid"), "top"),
        )
    }

    @Test
    fun usesFirstSupportedValueWhenDefaultDoesNotMatchEngineSchema() {
        assertEquals(
            "top",
            NativeEnumValueSanitizer.normalize("unknown", arrayOf("top", "solid"), "no ironing"),
        )
    }

    @Test
    fun leavesValuesAloneWhenEngineDoesNotExposeAnEnumSchema() {
        assertEquals(
            "no ironing",
            NativeEnumValueSanitizer.normalize("no ironing", emptyArray(), "top"),
        )
    }
}
