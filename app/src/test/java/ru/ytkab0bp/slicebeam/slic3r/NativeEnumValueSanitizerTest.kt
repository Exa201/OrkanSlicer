package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeEnumValueSanitizerTest {
    @Test
    fun legacyArcFittingBooleanValuesMigrateBeforeEnumSanitization() {
        val accepted = arrayOf("disabled", "enabled", "sparse_infill")
        assertEquals("disabled", NativeEnumValueSanitizer.normalize("enable_arc_fitting", "0", accepted, "disabled"))
        assertEquals("disabled", NativeEnumValueSanitizer.normalize("enable_arc_fitting", "false", accepted, "disabled"))
        assertEquals("enabled", NativeEnumValueSanitizer.normalize("enable_arc_fitting", "1", accepted, "disabled"))
        assertEquals("enabled", NativeEnumValueSanitizer.normalize("enable_arc_fitting", "true", accepted, "disabled"))
        assertEquals("sparse_infill", NativeEnumValueSanitizer.normalize("enable_arc_fitting", "sparse_infill", accepted, "disabled"))
        assertEquals("disabled", NativeEnumValueSanitizer.normalize("unrelated_option", "1", accepted, "disabled"))
    }

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
