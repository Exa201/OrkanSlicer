package ru.ytkab0bp.slicebeam.fragment

import java.util.function.Predicate

/** Groups profile options by category and removes headings for sections with no matching rows. */
object ProfileOptionSectionFilter {
    @JvmStatic
    fun <T : Any> filter(
        items: List<T?>,
        isSectionHeader: Predicate<T>,
        matches: Predicate<T>,
    ): List<T> {
        val visible = ArrayList<T>()
        var header: T? = null
        var matchingRows = ArrayList<T>()

        fun appendSection() {
            val currentHeader = header ?: return
            if (matchingRows.isEmpty()) return
            visible.add(currentHeader)
            visible.addAll(matchingRows)
        }

        for (item in items) {
            if (item == null) continue
            if (isSectionHeader.test(item)) {
                appendSection()
                header = item
                matchingRows = ArrayList()
            } else if (matches.test(item)) {
                matchingRows.add(item)
            }
        }
        appendSection()
        return visible
    }

    /** Keeps a parent category and subgroup only when at least one descendant option matches. */
    @JvmStatic
    fun <T : Any> filterHierarchical(
        items: List<T?>,
        isCategoryHeader: Predicate<T>,
        isGroupHeader: Predicate<T>,
        matches: Predicate<T>,
    ): List<T> {
        val visible = ArrayList<T>()
        var categoryHeader: T? = null
        var categoryRows = ArrayList<T>()
        var groupHeader: T? = null
        var groupRows = ArrayList<T>()

        fun appendGroup() {
            val currentHeader = groupHeader
            if (currentHeader != null && groupRows.isNotEmpty()) {
                categoryRows.add(currentHeader)
                categoryRows.addAll(groupRows)
            }
            groupHeader = null
            groupRows = ArrayList()
        }

        fun appendCategory() {
            appendGroup()
            val currentHeader = categoryHeader
            if (currentHeader != null && categoryRows.isNotEmpty()) {
                visible.add(currentHeader)
                visible.addAll(categoryRows)
            }
            categoryHeader = null
            categoryRows = ArrayList()
        }

        for (item in items) {
            if (item == null) continue
            when {
                isCategoryHeader.test(item) -> {
                    appendCategory()
                    categoryHeader = item
                }
                isGroupHeader.test(item) -> {
                    appendGroup()
                    groupHeader = item
                }
                matches.test(item) -> {
                    if (groupHeader == null) categoryRows.add(item) else groupRows.add(item)
                }
            }
        }
        appendCategory()
        return visible
    }
}
