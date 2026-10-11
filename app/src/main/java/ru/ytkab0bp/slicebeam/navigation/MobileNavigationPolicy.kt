package ru.ytkab0bp.slicebeam.navigation

/** Keeps the phone's five main destinations aligned with SliceBeam's workspace structure. */
object MobileNavigationPolicy {
    const val PREVIEW_SLOT = 0
    const val MAKER_SLOT = 1
    const val PRINT_SLOT = 2
    const val FILAMENT_SLOT = 3
    const val PRINTER_SLOT = 4
    const val APP_SLOT = 5

    private val destinations = listOf(
        PREVIEW_SLOT,
        PRINT_SLOT,
        FILAMENT_SLOT,
        PRINTER_SLOT,
        APP_SLOT,
    )

    @JvmStatic
    fun destinations(): List<Int> = destinations

    @JvmStatic
    fun isDestination(slot: Int): Boolean = slot in destinations

    /** Maker opens from App, so the App destination stays highlighted while it is open. */
    @JvmStatic
    fun selectedDestination(slot: Int): Int = if (slot == MAKER_SLOT) APP_SLOT else slot
}
