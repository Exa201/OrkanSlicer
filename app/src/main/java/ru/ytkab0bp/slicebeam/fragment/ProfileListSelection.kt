package ru.ytkab0bp.slicebeam.fragment

/** Selects the active preset row from the profile list shared by the header and editor. */
object ProfileListSelection {
    @JvmStatic
    fun selected(items: List<ProfileListItem>): ProfileListItem? =
        items.firstOrNull { it.isSelected() }
}
