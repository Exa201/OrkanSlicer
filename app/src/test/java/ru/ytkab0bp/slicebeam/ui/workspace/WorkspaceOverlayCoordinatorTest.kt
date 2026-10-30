package ru.ytkab0bp.slicebeam.ui.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceOverlayCoordinatorTest {
    @Test
    fun activatingDifferentPanelClosesPreviousPanel() {
        val coordinator = WorkspaceOverlayCoordinator()
        var previousDismissed = 0

        coordinator.activate(WorkspaceOverlayPanel.FILAMENT_PALETTE) { previousDismissed++ }
        coordinator.activate(WorkspaceOverlayPanel.PLATE_PICKER) { }

        assertEquals(1, previousDismissed)
        assertEquals(WorkspaceOverlayPanel.PLATE_PICKER, coordinator.activePanel)
    }

    @Test
    fun backDismissesCurrentPanelAndClearsItsState() {
        val coordinator = WorkspaceOverlayCoordinator()
        var dismissed = 0
        coordinator.register(WorkspaceOverlayPanel.PROFILE_PICKER) { dismissed++ }

        assertTrue(coordinator.dismissActive())
        assertEquals(1, dismissed)
        assertEquals(null, coordinator.activePanel)
        assertFalse(coordinator.dismissActive())
    }

    @Test
    fun staleDismissFromPreviousPanelCannotClearNewPanel() {
        val coordinator = WorkspaceOverlayCoordinator()
        coordinator.activate(WorkspaceOverlayPanel.FILAMENT_PALETTE) { }
        coordinator.activate(WorkspaceOverlayPanel.PLATE_PICKER) { }

        assertFalse(coordinator.clear(WorkspaceOverlayPanel.FILAMENT_PALETTE))
        assertEquals(WorkspaceOverlayPanel.PLATE_PICKER, coordinator.activePanel)
    }
}
