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
        assertEquals(124, WorkspaceToolPanelPolicy.reservedHeightDp(true, true, 124))
    }

    @Test
    fun twoRowActionsGrowForLargeSystemText() {
        assertEquals(124, WorkspaceToolPanelPolicy.menuHeightDp(true, 1f))
        assertEquals(144, WorkspaceToolPanelPolicy.menuHeightDp(true, 1.15f))
        assertEquals(68, WorkspaceToolPanelPolicy.menuHeightDp(false, 1.5f))
    }

    @Test
    fun landscapeSidePanelDoesNotReservePortraitBottomSpace() {
        assertEquals(0, WorkspaceToolPanelPolicy.reservedHeightDp(false, false, 124))
    }
}
