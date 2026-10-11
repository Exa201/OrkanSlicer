package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileEnumOptionResolverTest {
    private val values = arrayOf("octoprint", "moonraker")
    private val labels = arrayOf("OctoPrint", "Moonraker / Klipper")

    @Test
    fun resolvesNativeValueAndLegacyIndex() {
        assertEquals(1, ProfileEnumOptionResolver.selectedIndex("moonraker", values))
        assertEquals(1, ProfileEnumOptionResolver.selectedIndex("1", values))
    }

    @Test
    fun unknownAndOutOfRangeValuesStayUnselected() {
        assertEquals(-1, ProfileEnumOptionResolver.selectedIndex("custom", values))
        assertEquals(-1, ProfileEnumOptionResolver.selectedIndex("2", values))
        assertEquals(-1, ProfileEnumOptionResolver.selectedIndex("1", values, 1))
    }

    @Test
    fun labelFallsBackToTheOriginalValueWhenItIsUnknown() {
        assertEquals("Moonraker / Klipper", ProfileEnumOptionResolver.displayLabel("moonraker", labels, values))
        assertEquals("custom", ProfileEnumOptionResolver.displayLabel("custom", labels, values))
    }

    @Test
    fun parsesAndSerializesEnumVectorsWithoutLosingExistingOrderOrUnknownValues() {
        assertEquals(linkedSetOf("hardened_steel", "brass"),
            ProfileEnumOptionResolver.parseValues(" hardened_steel,brass,hardened_steel "))
        assertEquals("brass,hardened_steel,custom,stainless_steel",
            ProfileEnumOptionResolver.serializeSelection(
                arrayOf("hardened_steel", "stainless_steel", "brass"),
                linkedSetOf("brass", "hardened_steel", "stainless_steel", "custom"),
                "brass,hardened_steel,custom",
            ))
    }

    @Test
    fun emptyEnumVectorSerializesAsAnEmptyValue() {
        assertEquals("", ProfileEnumOptionResolver.serializeSelection(values, emptySet(), "moonraker"))
    }
}
