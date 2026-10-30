package ru.ytkab0bp.slicebeam.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceToolPanelPolicyTest {
    @Test
    fun collapsedPortraitPanelDoesNotReserveViewportHeight() {
        assertEquals(0, WorkspaceToolPanelPolicy.reservedHeightDp(true, false, 124))
    }

    @Test
    fun expandedPortraitPanelReservesItsActualHeight() {
        assertEquals(140, WorkspaceToolPanelPolicy.reservedHeightDp(true, true, 140))
    }

    @Test
    fun twoRowActionsGrowForLargeSystemText() {
        assertEquals(140, WorkspaceToolPanelPolicy.menuHeightDp(true, 1f))
        assertEquals(160, WorkspaceToolPanelPolicy.menuHeightDp(true, 1.15f))
        assertEquals(88, WorkspaceToolPanelPolicy.menuHeightDp(false, 1.5f))
    }

    @Test
    fun menuHeightIncludesExtraChromeBeforeScrollableActions() {
        assertEquals(196, WorkspaceToolPanelPolicy.menuHeightDp(true, 1f, additionalChromeDp = 56))
        assertEquals(248, WorkspaceToolPanelPolicy.menuHeightDp(true, 1.5f, additionalChromeDp = 72))
    }

    @Test
    fun landscapeSidePanelDoesNotReservePortraitBottomSpace() {
        assertEquals(0, WorkspaceToolPanelPolicy.reservedHeightDp(false, false, 124))
    }
}
