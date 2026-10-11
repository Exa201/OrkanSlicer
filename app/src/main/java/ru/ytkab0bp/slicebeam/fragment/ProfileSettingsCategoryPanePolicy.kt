package ru.ytkab0bp.slicebeam.fragment

/** Extracts stable category targets and their rows for the expanded-width profile layout. */
internal object ProfileSettingsCategoryPanePolicy {
    fun categories(rows: List<ProfileOptionWrapper>): List<ProfileOptionWrapper> =
        rows.filter { it.isExpandable && !it.title.isNullOrBlank() }

    fun rowsForCategory(
        rows: List<ProfileOptionWrapper>,
        categoryIndex: Int,
    ): List<IndexedValue<ProfileOptionWrapper>> = rows.withIndex().filter { (_, row) ->
        row.categoryIndex == categoryIndex
    }
}
