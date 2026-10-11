package ru.ytkab0bp.slicebeam.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.config.ConfigObject

class ProfileSettingsViewModelTest {
    @Test
    fun selectedTabAndAccordionStateAreRetainedSeparatelyByProfileType() {
        val viewModel = ProfileSettingsViewModel()
        viewModel.setSelectedCategory(ConfigObject.PROFILE_LIST_PRINT, 3)
        viewModel.setSelectedCategory(ConfigObject.PROFILE_LIST_FILAMENT, 2)
        viewModel.setCollapsedGroup(ConfigObject.PROFILE_LIST_PRINT, "Quality/Layer height", false)
        viewModel.setSearchQuery(ConfigObject.PROFILE_LIST_PRINT, "layer height")
        viewModel.setFavoritesOnly(ConfigObject.PROFILE_LIST_PRINT, true)

        assertEquals(3, viewModel.selectedCategory(ConfigObject.PROFILE_LIST_PRINT))
        assertEquals(2, viewModel.selectedCategory(ConfigObject.PROFILE_LIST_FILAMENT))
        assertFalse(viewModel.collapsedGroup(ConfigObject.PROFILE_LIST_PRINT, "Quality/Layer height")!!)
        assertNull(viewModel.collapsedGroup(ConfigObject.PROFILE_LIST_FILAMENT, "Quality/Layer height"))
        assertEquals("layer height", viewModel.searchQuery(ConfigObject.PROFILE_LIST_PRINT))
        assertTrue(viewModel.favoritesOnly(ConfigObject.PROFILE_LIST_PRINT))
        assertFalse(viewModel.favoritesOnly(ConfigObject.PROFILE_LIST_FILAMENT))
    }

    @Test
    fun categoryIndexesCannotBecomeNegative() {
        val viewModel = ProfileSettingsViewModel()
        viewModel.setSelectedCategory(ConfigObject.PROFILE_LIST_PRINTER, -4)

        assertEquals(0, viewModel.selectedCategory(ConfigObject.PROFILE_LIST_PRINTER))
        assertTrue(viewModel.selectedCategory(ConfigObject.PROFILE_LIST_FILAMENT) == 0)
    }

    @Test
    fun expandedProfileCategoriesAreIsolatedByProfileAndDefaultClosed() {
        val viewModel = ProfileSettingsViewModel()
        val print = ConfigObject.PROFILE_LIST_PRINT
        val filament = ConfigObject.PROFILE_LIST_FILAMENT

        assertFalse(viewModel.categoryExpanded(print, "0.20mm Standard", "Quality"))
        viewModel.setCategoryExpanded(print, "0.20mm Standard", "Quality", true)

        assertTrue(viewModel.categoryExpanded(print, "0.20mm Standard", "Quality"))
        assertFalse(viewModel.categoryExpanded(print, "0.16mm Fine", "Quality"))
        assertFalse(viewModel.categoryExpanded(filament, "0.20mm Standard", "Quality"))
    }
}
