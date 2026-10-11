package ru.ytkab0bp.slicebeam.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSettingsMenuLayoutPolicyTest {
    @Test
    fun honorSizedPhoneStacksSearchLevelAndFavorites() {
        assertTrue(ProfileSettingsMenuLayoutPolicy.usesStackedFilterControls(432, 1.2f))
        assertTrue(ProfileSettingsMenuLayoutPolicy.usesStackedFilterControls(479, 1.0f))
    }

    @Test
    fun largeTextKeepsControlsStackedOnWideDisplays() {
        assertFalse(ProfileSettingsMenuLayoutPolicy.usesStackedFilterControls(720, 1.2f))
        assertTrue(ProfileSettingsMenuLayoutPolicy.usesStackedFilterControls(720, 1.4f))
        assertTrue(ProfileSettingsMenuLayoutPolicy.usesStackedFilterControls(1200, 1.5f))
    }

    @Test
    fun normalTextCanUseWideLayoutOnTablet() {
        assertFalse(ProfileSettingsMenuLayoutPolicy.usesStackedFilterControls(600, 1.1f))
        assertFalse(ProfileSettingsMenuLayoutPolicy.usesStackedFilterControls(1024, 1.0f))
    }

    @Test
    fun categoryPaneRequiresExpandedWidthAndReadableTextScale() {
        assertFalse(ProfileSettingsMenuLayoutPolicy.usesCategoryPane(839, 1.0f))
        assertTrue(ProfileSettingsMenuLayoutPolicy.usesCategoryPane(840, 1.0f))
        assertFalse(ProfileSettingsMenuLayoutPolicy.usesCategoryPane(1024, 1.4f))
        assertFalse(ProfileSettingsMenuLayoutPolicy.usesCategoryPane(1024, 1.5f))
        assertFalse(ProfileSettingsMenuLayoutPolicy.usesCategoryPane(1024, Float.NaN))
    }
}
