package ru.ytkab0bp.slicebeam.fragment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.ytkab0bp.slicebeam.ui.settings.ProfileCategoryTabs
import ru.ytkab0bp.slicebeam.ui.settings.ProfileSettingsViewModel
import ru.ytkab0bp.slicebeam.ui.settings.OrcaProfileGroupLocalizer
import java.util.Locale
import java.util.function.IntConsumer

/** Keeps profile category selection and its Compose tab presentation in sync. */
class ProfileSettingsCategoryTabsController(
    private val profileType: Int,
    private val viewModel: ProfileSettingsViewModel?,
    private val onSelected: IntConsumer,
) {
    private var sourceTitles: List<String> = emptyList()

    var selectedIndex by mutableIntStateOf(if (viewModel != null && profileType != -1) {
        viewModel.selectedCategory(profileType)
    } else 0
    )
        private set
    private var labels by mutableStateOf(emptyList<String>())

    @Composable
    fun Content() {
        ProfileCategoryTabs(labels, selectedIndex) { index -> onSelected.accept(index) }
    }

    fun updateHeadings(titles: List<String>, selected: Int) {
        sourceTitles = titles.toList()
        selectedIndex = selected.coerceIn(0, (sourceTitles.size - 1).coerceAtLeast(0))
        labels = sourceTitles.map(::localize)
        persistSelection()
    }

    fun select(index: Int): Boolean {
        if (index < 0 || index >= sourceTitles.size) return false
        selectedIndex = index
        persistSelection()
        return true
    }

    fun resetSelection() {
        selectedIndex = 0
        persistSelection()
    }

    private fun persistSelection() {
        if (viewModel != null && profileType != -1) {
            viewModel.setSelectedCategory(profileType, selectedIndex)
        }
    }

    private fun localize(value: String): String =
        OrcaProfileGroupLocalizer.mobileGroup(value, Locale.getDefault().language)
}
