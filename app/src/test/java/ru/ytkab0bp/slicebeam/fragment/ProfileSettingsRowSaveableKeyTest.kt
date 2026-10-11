package ru.ytkab0bp.slicebeam.fragment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef

class ProfileSettingsRowSaveableKeyTest {
    @Test
    fun optionRowsHaveStringKeysStableAcrossPositionChangesAndDistinctByVectorIndex() {
        val first = optionRow("nozzle_diameter", elementIndex = 0)
        val second = optionRow("nozzle_diameter", elementIndex = 1)

        val firstKey = ProfileSettingsRowSaveableKey.create(4, first)

        assertEquals(firstKey, ProfileSettingsRowSaveableKey.create(19, optionRow("nozzle_diameter", 0)))
        assertNotEquals(firstKey, ProfileSettingsRowSaveableKey.create(5, second))
        assertEquals("option:nozzle_diameter:0", firstKey)
    }

    @Test
    fun headingsAndFallbackRowsUseBundleSaveableStrings() {
        val heading = ProfileOptionWrapper(0, "Quality", null, 0, false).apply { categoryIndex = 2 }
        val fallback = ProfileOptionWrapper(0, null, null, 0, false)

        assertEquals("heading:2:Quality", ProfileSettingsRowSaveableKey.create(0, heading))
        assertEquals("row:0:0:empty", ProfileSettingsRowSaveableKey.create(0, fallback))
    }

    private fun optionRow(key: String, elementIndex: Int): ProfileOptionWrapper {
        val definition = ConfigOptionDef().apply { this.key = key }
        val element = ProfileOptionElement(null, definition, elementIndex, null, null, null, null)
        return ProfileOptionWrapper(element)
    }
}
