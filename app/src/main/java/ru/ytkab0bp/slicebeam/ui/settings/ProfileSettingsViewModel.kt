package ru.ytkab0bp.slicebeam.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.config.ProfileSettingsUiState

/** Lifecycle-aware screen adapter for the app-scoped profile draft repository. */
class ProfileSettingsViewModel : ViewModel() {
    val uiState: StateFlow<ProfileSettingsUiState> = ProfileSettingsRepository.uiState
    private val selectedCategories = HashMap<Int, Int>()
    private val collapsedGroups = HashMap<String, Boolean>()
    private val expandedCategories = HashMap<String, Boolean>()
    private val searchQueries = HashMap<Int, String>()
    private val favoriteFilters = HashMap<Int, Boolean>()

    fun bindActiveProfile(profileType: Int, title: String?) {
        ProfileSettingsRepository.bindActiveProfile(profileType, title)
    }

    @Synchronized
    fun selectedCategory(profileType: Int): Int = selectedCategories[profileType] ?: 0

    @Synchronized
    fun setSelectedCategory(profileType: Int, index: Int) {
        selectedCategories[profileType] = index.coerceAtLeast(0)
    }

    @Synchronized
    fun collapsedGroup(profileType: Int, key: String): Boolean? =
        collapsedGroups["$profileType:$key"]

    @Synchronized
    fun setCollapsedGroup(profileType: Int, key: String, collapsed: Boolean) {
        collapsedGroups["$profileType:$key"] = collapsed
    }

    @Synchronized
    fun categoryExpanded(profileType: Int, profileTitle: String?, category: String): Boolean =
        expandedCategories[categoryKey(profileType, profileTitle, category)] ?: false

    @Synchronized
    fun setCategoryExpanded(profileType: Int, profileTitle: String?, category: String, expanded: Boolean) {
        expandedCategories[categoryKey(profileType, profileTitle, category)] = expanded
    }

    @Synchronized
    fun searchQuery(profileType: Int): String = searchQueries[profileType].orEmpty()

    @Synchronized
    fun setSearchQuery(profileType: Int, query: String) {
        searchQueries[profileType] = query
    }

    @Synchronized
    fun favoritesOnly(profileType: Int): Boolean = favoriteFilters[profileType] ?: false

    @Synchronized
    fun setFavoritesOnly(profileType: Int, enabled: Boolean) {
        favoriteFilters[profileType] = enabled
    }

    private fun categoryKey(profileType: Int, profileTitle: String?, category: String): String =
        "$profileType:${profileTitle.orEmpty()}:$category"
}
