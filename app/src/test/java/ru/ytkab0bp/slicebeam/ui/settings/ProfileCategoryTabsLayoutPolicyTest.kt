package ru.ytkab0bp.slicebeam.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileCategoryTabsLayoutPolicyTest {
    @Test
    fun switchesToTwoColumnsWhenLargeTextNeedsMoreVerticalRoom() {
        assertFalse(ProfileCategoryTabsLayoutPolicy.usesStackedRows(1.0f))
        assertFalse(ProfileCategoryTabsLayoutPolicy.usesStackedRows(1.34f))
        assertTrue(ProfileCategoryTabsLayoutPolicy.usesStackedRows(1.35f))
        assertTrue(ProfileCategoryTabsLayoutPolicy.usesStackedRows(2.0f))
        assertFalse(ProfileCategoryTabsLayoutPolicy.usesStackedRows(Float.NaN))
    }
}
