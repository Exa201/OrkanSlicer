package ru.ytkab0bp.slicebeam.fragment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.function.BiConsumer
import java.util.function.Function

class ProfileSettingsCategoryPanePolicyTest {
    @Test
    fun categoryRowsKeepTheirSourceIndexAndParentCategory() {
        val items = listOf(
            ProfileOptionElement(0, "Quality"),
            ProfileOptionElement(ProfileSubHeader("Layer height")),
            optionRow(),
            ProfileOptionElement(0, "Strength"),
            ProfileOptionElement(ProfileSubHeader("Infill")),
            optionRow(),
        )
        val rows = ProfileSettingsListBuilder.buildAccordion(
            items = items,
            profileType = -1,
            collapsedGroups = mapOf("Quality/Layer height" to false, "Strength/Infill" to false),
            viewModel = null,
            categoryExpanded = Function { true },
            onCategoryExpansionChanged = BiConsumer { _, _ -> },
            createWrapper = Function { ProfileOptionWrapper.fromOption(it) },
            onGroupToggle = BiConsumer { _, _ -> },
            onRebuild = Runnable {},
        )

        assertEquals(listOf("Quality", "Strength"), ProfileSettingsCategoryPanePolicy.categories(rows).map { it.title })
        assertEquals(listOf(0, 1, 2), ProfileSettingsCategoryPanePolicy.rowsForCategory(rows, 0).map { it.index })
        assertEquals(listOf(3, 4, 5), ProfileSettingsCategoryPanePolicy.rowsForCategory(rows, 1).map { it.index })
        assertTrue(rows.all { it.categoryIndex in 0..1 })
    }

    @Test
    fun openedCategoryShowsOptionsDespiteOldCollapsedSubgroupPreferences() {
        val rows = ProfileSettingsListBuilder.buildAccordion(
            items = listOf(ProfileOptionElement(0, "Quality"),
                ProfileOptionElement(ProfileSubHeader("Layer height")), optionRow()),
            profileType = -1, collapsedGroups = mapOf("Quality/Layer height" to true),
            viewModel = null, categoryExpanded = Function { true },
            onCategoryExpansionChanged = BiConsumer { _, _ -> },
            createWrapper = Function { ProfileOptionWrapper.fromOption(it) },
            onGroupToggle = BiConsumer { _, _ -> }, onRebuild = Runnable {},
        )
        assertEquals(3, rows.size)
        val heading = rows[1].optionEl!!.simpleItem as ProfileSubHeader
        assertTrue(!heading.collapsed)
        assertTrue(heading.onClick == null)
    }

    private fun optionRow() = ProfileOptionElement(null, null, 0, null, null, null, null)
}
