package ru.ytkab0bp.slicebeam.ui.setup

internal data class SetupProfileOption(
    val key: String,
    val title: String,
    val thumbnailUrl: String?,
    val selected: Boolean,
)

internal data class SetupProfileSection(
    val key: String,
    val title: String,
    val options: List<SetupProfileOption>,
)

/** Small, UI-independent selection rules shared by the setup screens. */
internal object SetupProfileSelectionPolicy {
    fun shouldShowFilamentVendor(hasSelectedPrinter: Boolean, independentLibrary: Boolean): Boolean =
        hasSelectedPrinter || independentLibrary

    fun toggle(selectedKeys: Set<String>, key: String, selected: Boolean): Set<String> =
        LinkedHashSet(selectedKeys).apply {
            if (selected) add(key) else remove(key)
        }

    fun selectAll(keys: Collection<String>): Set<String> = LinkedHashSet(keys)

    fun canContinue(useCustomProfile: Boolean, selectedPrinterCount: Int): Boolean =
        useCustomProfile || selectedPrinterCount > 0
}
