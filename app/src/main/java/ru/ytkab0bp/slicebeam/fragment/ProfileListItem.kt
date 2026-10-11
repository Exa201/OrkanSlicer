package ru.ytkab0bp.slicebeam.fragment

/** Shared profile row contract used by profile storage, selection UI, and the settings host. */
interface ProfileListItem {
    val title: String?
    fun isSelected(): Boolean
}
