package ru.ytkab0bp.slicebeam.fragment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSettingsRowSectionsTest {
    @Test
    fun groupsRowsBetweenCategoryHeadingsAndUsesEachHeadingAccent() {
        val quality = ProfileOptionWrapper(1, "Quality", null, 0x00112233, false)
        val layerHeight = ProfileOptionWrapper(0, null, null, 0, false)
        val speed = ProfileOptionWrapper(2, "Speed", null, 0x00445566, false)
        val acceleration = ProfileOptionWrapper(0, null, null, 0, false)

        val result = ProfileSettingsRowSections.decorate(listOf(quality, layerHeight, speed, acceleration))

        assertEquals(4, result.size)
        assertTrue(result[0].startsSection)
        assertFalse(result[0].endsSection)
        assertEquals(quality.color, result[0].accentColor)
        assertFalse(result[1].startsSection)
        assertTrue(result[1].endsSection)
        assertEquals(quality.color, result[1].accentColor)
        assertTrue(result[2].startsSection)
        assertFalse(result[2].endsSection)
        assertEquals(speed.color, result[2].accentColor)
        assertFalse(result[3].startsSection)
        assertTrue(result[3].endsSection)
        assertEquals(speed.color, result[3].accentColor)
    }

    @Test
    fun treatsAnUnheadedTabAsOneSectionAndHandlesEmptyLists() {
        val first = ProfileOptionWrapper(0, null, null, 0, false)
        val second = ProfileOptionWrapper(0, null, null, 0, false)

        val result = ProfileSettingsRowSections.decorate(listOf(first, second))

        assertTrue(result.first().startsSection)
        assertFalse(result.first().endsSection)
        assertFalse(result.last().startsSection)
        assertTrue(result.last().endsSection)
        assertTrue(ProfileSettingsRowSections.decorate(emptyList()).isEmpty())
    }
}
