package ru.ytkab0bp.slicebeam.ui.navigation

import kotlin.math.ceil

/** Chooses compact bottom navigation or an expanded rail from the current app-window width. */
object AdaptiveNavigationPolicy {
    const val EXPANDED_WIDTH_DP = 600
    const val NAVIGATION_RAIL_WIDTH_DP = 80
    const val LARGE_TEXT_NAVIGATION_RAIL_WIDTH_DP = 96
    const val COMPACT_BOTTOM_LABEL_WIDTH_DP = 440
    const val NAVIGATION_BAR_HEIGHT_DP = 56
    const val COMPACT_LABEL_FONT_SCALE = 1.15f

    @JvmStatic
    fun usesCompactLabels(fontScale: Float): Boolean = fontScale >= COMPACT_LABEL_FONT_SCALE

    /** Shortens visible labels when six bottom destinations would wrap in a phone-width window. */
    @JvmStatic
    fun usesCompactBottomLabels(windowWidthDp: Int, fontScale: Float): Boolean =
        usesCompactLabels(fontScale) || (windowWidthDp in 1 until COMPACT_BOTTOM_LABEL_WIDTH_DP)

    /** Compact visible labels remain necessary in two-row navigation when a word cannot wrap. */
    @JvmStatic
    fun usesCompactWorkspaceLabels(windowWidthDp: Int, fontScale: Float, isPortrait: Boolean): Boolean =
        !isPortrait || usesCompactBottomLabels(windowWidthDp, fontScale)

    @JvmStatic
    fun usesTwoRowBottomNavigation(fontScale: Float, itemCount: Int): Boolean =
        usesCompactLabels(fontScale) && itemCount > 3

    @JvmStatic
    fun navigationBarHeightDp(fontScale: Float, itemCount: Int): Int =
        if (usesTwoRowBottomNavigation(fontScale, itemCount)) {
            (NAVIGATION_BAR_HEIGHT_DP * fontScale.coerceAtLeast(1f)).let { ceil(it).toInt() } * 2
        } else {
            NAVIGATION_BAR_HEIGHT_DP
        }

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
    const val TWO_ROW_MENU_HEIGHT_DP = 140
    const val LARGE_TEXT_TWO_ROW_MENU_HEIGHT_DP = 160

    @JvmStatic
    fun menuHeightDp(needsTwoRows: Boolean, fontScale: Float, additionalChromeDp: Int = 0): Int {
        val base = if (!needsTwoRows) {
            if (fontScale >= 1.3f) 88 else COMPACT_MENU_HEIGHT_DP
        } else {
            if (fontScale >= 1.3f) 176 else if (fontScale >= 1.15f) LARGE_TEXT_TWO_ROW_MENU_HEIGHT_DP else TWO_ROW_MENU_HEIGHT_DP
        }
        return base + additionalChromeDp.coerceAtLeast(0)
    }

    @JvmStatic
    fun reservedHeightDp(isPortrait: Boolean, expanded: Boolean, menuHeightDp: Int): Int =
        if (isPortrait && expanded) menuHeightDp.coerceAtLeast(0) else 0
}
