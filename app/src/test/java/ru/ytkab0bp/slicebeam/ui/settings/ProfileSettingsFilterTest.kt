package ru.ytkab0bp.slicebeam.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSettingsFilterTest {
    @Test
    fun simpleModeShowsOnlyCuratedOptions() {
        assertTrue(matches("layer_height", level = 0))
        assertFalse(matches("seam_position", level = 0))
    }

    @Test
    fun standardModeHidesExpertOptionsButExpertModeKeepsThem() {
        assertFalse(matches("filename_format", level = 1))
        assertTrue(matches("filename_format", level = 2))
        assertTrue(matches("flush_into_infill", level = 1))
        assertFalse(matches("flush_into_objects", level = 1))
    }

    @Test
    fun favoritesFilterAndSearchTermsAreCombined() {
        assertFalse(matches("layer_height", favorite = false, favoritesOnly = true))
        assertTrue(matches("layer_height", favorite = true, favoritesOnly = true))
        assertTrue(matches("layer_height", query = "schichthöhe", text = "layer height"))
        assertFalse(matches("layer_height", query = "layer speed", text = "layer height"))
        assertTrue(matches("flush_into_infill", query = "reinigung in der füllung", text = "flush into infill"))
    }

    @Test
    fun missingDefinitionsOnlyShowAsAdvancedPlaceholdersWithoutSearch() {
        assertTrue(matches(null, visibleInAdvanced = true, hasDefinition = false))
        assertFalse(matches(null, visibleInAdvanced = false, hasDefinition = false))
        assertFalse(matches(null, visibleInAdvanced = true, hasDefinition = false, query = "support"))
    }

    private fun matches(
        key: String?,
        level: Int = 1,
        favorite: Boolean = false,
        favoritesOnly: Boolean = false,
        query: String = "",
        text: String = "",
        hasDefinition: Boolean = true,
        visibleInAdvanced: Boolean = false,
    ) = ProfileSettingsFilter.matches(
        key = key,
        visibleInAdvanced = visibleInAdvanced,
        hasDefinition = hasDefinition,
        isFavorite = favorite,
        favoritesOnly = favoritesOnly,
        settingsLevel = level,
        query = query,
        searchableText = text.ifEmpty { key.orEmpty() },
    )
}
