package ru.ytkab0bp.slicebeam.ui.navigation

/** Chooses compact bottom navigation or an expanded rail from the current app-window width. */
object AdaptiveNavigationPolicy {
    const val EXPANDED_WIDTH_DP = 600
    const val NAVIGATION_RAIL_WIDTH_DP = 64
    const val NAVIGATION_BAR_HEIGHT_DP = 56

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
