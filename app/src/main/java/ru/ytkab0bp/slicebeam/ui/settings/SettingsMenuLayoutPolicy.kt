package ru.ytkab0bp.slicebeam.ui.settings

/** Keeps the app-settings hierarchy readable without wasting the narrow phone viewport. */
internal object SettingsMenuLayoutPolicy {
    const val COMPACT_MAX_WIDTH_DP = 599
    const val COMPACT_MAX_HEIGHT_DP = 479
    const val TWO_COLUMN_MIN_WIDTH_DP = 600

    @JvmStatic
    fun usesCompactLayout(screenWidthDp: Int, screenHeightDp: Int): Boolean =
        screenWidthDp in 1..COMPACT_MAX_WIDTH_DP ||
            screenHeightDp in 1..COMPACT_MAX_HEIGHT_DP

    @JvmStatic
    fun usesTwoColumnLayout(screenWidthDp: Int, screenHeightDp: Int): Boolean =
        screenWidthDp >= TWO_COLUMN_MIN_WIDTH_DP &&
            screenHeightDp in 1..COMPACT_MAX_HEIGHT_DP
}
