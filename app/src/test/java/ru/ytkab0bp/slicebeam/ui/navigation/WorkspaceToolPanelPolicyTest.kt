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
        assertEquals(192, WorkspaceToolPanelPolicy.reservedHeightDp(true, true, 192))
    }

    @Test
    fun twoRowActionsGrowForLargeSystemText() {
        assertEquals(192, WorkspaceToolPanelPolicy.menuHeightDp(true, 1f))
        assertEquals(236, WorkspaceToolPanelPolicy.menuHeightDp(true, 1.15f))
        assertEquals(236, WorkspaceToolPanelPolicy.menuHeightDp(true, 1.3f))
        assertEquals(88, WorkspaceToolPanelPolicy.menuHeightDp(false, 1.5f))
    }

    @Test
    fun menuHeightIncludesExtraChromeBeforeScrollableActions() {
        assertEquals(248, WorkspaceToolPanelPolicy.menuHeightDp(true, 1f, additionalChromeDp = 56))
        assertEquals(308, WorkspaceToolPanelPolicy.menuHeightDp(true, 1.5f, additionalChromeDp = 72))
    }

    @Test
    fun landscapeSidePanelDoesNotReservePortraitBottomSpace() {
        assertEquals(0, WorkspaceToolPanelPolicy.reservedHeightDp(false, false, 124))
    }
}
