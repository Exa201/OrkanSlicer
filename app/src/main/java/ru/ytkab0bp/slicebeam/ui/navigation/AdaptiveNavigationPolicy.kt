package ru.ytkab0bp.slicebeam.ui.navigation

/** Chooses compact bottom navigation or an expanded rail from the current app-window width. */
object AdaptiveNavigationPolicy {
    const val EXPANDED_WIDTH_DP = 600
    const val NAVIGATION_RAIL_WIDTH_DP = 80
    const val LARGE_TEXT_NAVIGATION_RAIL_WIDTH_DP = 96
    const val NAVIGATION_BAR_HEIGHT_DP = 56

    /** Wider rail space preserves system text scaling and readable destination names. */
    @JvmStatic
    fun navigationRailWidthDp(fontScale: Float): Int =
        if (fontScale >= 1.2f) LARGE_TEXT_NAVIGATION_RAIL_WIDTH_DP else NAVIGATION_RAIL_WIDTH_DP

    @JvmStatic
    fun usesBottomNavigation(windowWidthDp: Int, isPortrait: Boolean): Boolean =
        if (windowWidthDp > 0) windowWidthDp < EXPANDED_WIDTH_DP else isPortrait

    /** Uses the app window shape so split-screen and freeform windows do not inherit device shape. */
    @JvmStatic
    fun isPortraitWindow(windowWidthDp: Int, windowHeightDp: Int, isPortrait: Boolean): Boolean =
        if (windowWidthDp > 0 && windowHeightDp > 0) windowWidthDp < windowHeightDp else isPortrait

    /** Compact vertical controls are used only when the current window is both narrow and tall. */
    @JvmStatic
    fun usesCompactPortraitLayout(windowWidthDp: Int, windowHeightDp: Int, isPortrait: Boolean): Boolean {
        val portraitWindow = isPortraitWindow(windowWidthDp, windowHeightDp, isPortrait)
        return portraitWindow && usesBottomNavigation(windowWidthDp, portraitWindow)
    }
}

/** Reserves the plate viewport by keeping portrait action menus collapsed until requested. */
object WorkspaceToolPanelPolicy {
    const val COMPACT_MENU_HEIGHT_DP = 68
    const val TWO_ROW_MENU_HEIGHT_DP = 124
    const val LARGE_TEXT_TWO_ROW_MENU_HEIGHT_DP = 144

    @JvmStatic
    fun menuHeightDp(needsTwoRows: Boolean, fontScale: Float): Int {
        if (!needsTwoRows) return COMPACT_MENU_HEIGHT_DP
        return if (fontScale >= 1.15f) LARGE_TEXT_TWO_ROW_MENU_HEIGHT_DP else TWO_ROW_MENU_HEIGHT_DP
    }

    @JvmStatic
    fun reservedHeightDp(isPortrait: Boolean, expanded: Boolean, menuHeightDp: Int): Int =
        if (isPortrait && expanded) menuHeightDp.coerceAtLeast(0) else 0
}
