package ru.ytkab0bp.slicebeam.fragment

import android.util.SparseArray
import ru.ytkab0bp.slicebeam.ui.settings.ProfileSettingsViewModel
import java.util.function.BiConsumer
import java.util.function.Function

/** Builds the category and collapsible-group rows consumed by the shared Compose profile list. */
object ProfileSettingsListBuilder {
    data class State(
        val categories: SparseArray<out List<ProfileOptionWrapper>>,
        val headings: List<ProfileOptionWrapper>,
        val visibleItems: List<ProfileOptionWrapper>,
        val selectedTab: Int,
    )

    @JvmStatic
    fun build(
        items: List<ProfileOptionElement?>,
        useTabs: Boolean,
        selectedTab: Int,
        profileType: Int,
        collapsedGroups: Map<String, Boolean>,
        viewModel: ProfileSettingsViewModel?,
        createWrapper: Function<ProfileOptionElement, ProfileOptionWrapper>,
        onGroupToggle: BiConsumer<String, Boolean>,
        onRebuild: Runnable,
    ): State {
        val categories = SparseArray<MutableList<ProfileOptionWrapper>>()
        val headings = ArrayList<ProfileOptionWrapper>()
        var flatList = ArrayList<ProfileOptionWrapper>()
        var activeCategory: String? = null
        var hideUntilNextHeader = false
        var categoryIndex = 0

        for (element in items) {
            if (element == null) continue
            if (element.title != null || element.specialType != -1) {
                hideUntilNextHeader = false
                activeCategory = element.title
            }

            val groupHeader = element.simpleItem as? ProfileSubHeader
            if (groupHeader != null) {
                val groupKey = (activeCategory ?: "") + "/" + groupHeader.title
                val savedCollapsed = if (viewModel == null || profileType == -1) {
                    null
                } else {
                    viewModel.collapsedGroup(profileType, groupKey)
                }
                val collapsed = savedCollapsed
                    ?: collapsedGroups[groupKey]
                    ?: true
                element.simpleItem = ProfileSubHeader(
                    groupHeader.title,
                    collapsed,
                    Runnable { onGroupToggle.accept(groupKey, !collapsed); onRebuild.run() },
                )
                hideUntilNextHeader = collapsed
                val wrapper = createWrapper.apply(element)
                wrapper.categoryIndex = categoryIndex - 1
                categories.get(categoryIndex - 1)?.add(wrapper)
                continue
            }

            if (hideUntilNextHeader && element.title == null && element.specialType == -1) continue

            val wrapper = createWrapper.apply(element)
            if (element.title != null || element.specialType != -1) {
                wrapper.categoryIndex = categoryIndex
                categories.put(categoryIndex, ArrayList())
                categoryIndex++
                headings.add(wrapper)
                flatList.add(wrapper)
            } else {
                categories.get(categoryIndex - 1)?.add(wrapper)
            }
        }

        var activeTab = selectedTab
        if (useTabs && activeTab >= headings.size) activeTab = 0
        val visible = if (useTabs) {
            categories.get(activeTab) ?: emptyList()
        } else {
            flatList
        }
        return State(categories, headings, ArrayList(visible), activeTab)
    }

    /** Only primary categories collapse; subgroups are headings with their settings directly below. */
    @JvmStatic
    fun buildAccordion(
        items: List<ProfileOptionElement?>,
        profileType: Int,
        collapsedGroups: Map<String, Boolean>,
        viewModel: ProfileSettingsViewModel?,
        categoryExpanded: Function<String, Boolean>,
        onCategoryExpansionChanged: BiConsumer<String, Boolean>,
        createWrapper: Function<ProfileOptionElement, ProfileOptionWrapper>,
        onGroupToggle: BiConsumer<String, Boolean>,
        onRebuild: Runnable,
    ): List<ProfileOptionWrapper> {
        val visible = ArrayList<ProfileOptionWrapper>()
        var categoryKey: String? = null
        var categoryVisible = false
        var groupCollapsed = false
        var categoryIndex = 0
        var activeCategoryIndex = 0

        for (element in items) {
            if (element == null) continue
            if (element.title != null || element.specialType != -1) {
                categoryKey = element.title
                categoryVisible = true
                groupCollapsed = false
                val wrapper = createWrapper.apply(element)
                if (element.title != null) {
                    val key = element.title.orEmpty()
                    activeCategoryIndex = categoryIndex++
                    val expanded = categoryExpanded.apply(key)
                    wrapper.categoryIndex = activeCategoryIndex
                    wrapper.isExpandable = true
                    wrapper.isExpanded = expanded
                    wrapper.onClick = Runnable {
                        onCategoryExpansionChanged.accept(key, !expanded)
                        onRebuild.run()
                    }
                    categoryVisible = expanded
                } else {
                    wrapper.categoryIndex = activeCategoryIndex
                }
                visible.add(wrapper)
                continue
            }

            if (!categoryVisible) continue
            val groupHeader = element.simpleItem as? ProfileSubHeader
            if (groupHeader != null) {
                element.simpleItem = ProfileSubHeader(groupHeader.title, collapsed = false, onClick = null)
                visible.add(createWrapper.apply(element).also { it.categoryIndex = activeCategoryIndex })
                groupCollapsed = false
                continue
            }

            if (!groupCollapsed) {
                visible.add(createWrapper.apply(element).also { it.categoryIndex = activeCategoryIndex })
            }
        }
        return visible
    }
}
