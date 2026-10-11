package ru.ytkab0bp.slicebeam.ui.settings

/** Keeps profile filters readable when phone width or the chosen text size is limited. */
internal object ProfileSettingsMenuLayoutPolicy {
    const val STACKED_FILTER_MAX_WIDTH_DP = 480
    const val STACKED_FILTER_TEXT_SCALE = 1.35f
    const val CATEGORY_PANE_MIN_WIDTH_DP = 840
    const val CATEGORY_PANE_MAX_TEXT_SCALE = 1.4f

    fun usesStackedFilterControls(screenWidthDp: Int, textScale: Float): Boolean =
        screenWidthDp in 1 until STACKED_FILTER_MAX_WIDTH_DP ||
            textScale.isFinite() && textScale >= STACKED_FILTER_TEXT_SCALE

    /** Two-pane category navigation needs room for both the category list and useful setting rows. */
    fun usesCategoryPane(screenWidthDp: Int, textScale: Float): Boolean =
        screenWidthDp >= CATEGORY_PANE_MIN_WIDTH_DP &&
            textScale.isFinite() && textScale < CATEGORY_PANE_MAX_TEXT_SCALE
}
