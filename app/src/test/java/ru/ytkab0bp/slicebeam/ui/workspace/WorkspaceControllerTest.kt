package ru.ytkab0bp.slicebeam.ui.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceDestination

class WorkspaceControllerTest {
    @Test
    fun ownsNavigationAndPanelStateTogether() {
        val controller = WorkspaceController()
        assertEquals(WorkspaceDestination.PROJECT.ordinal, controller.selectedDestinationSlot)
        assertTrue(controller.selectDestination(WorkspaceDestination.MODEL.ordinal))
        controller.activatePanel(WorkspaceOverlayPanel.FILAMENT_PALETTE) {}

        assertEquals(WorkspaceDestination.MODEL.ordinal, controller.selectedDestinationSlot)
        assertEquals(WorkspaceOverlayPanel.FILAMENT_PALETTE, controller.activePanel)
    }

    @Test
    fun activePlateIsValidatedAndSharedAsSingleState() {
        val controller = WorkspaceController()
        assertFalse(controller.setActivePlate(-1))
        assertEquals(0, controller.activePlateIndex)
        assertTrue(controller.setActivePlate(5))
        assertEquals(5, controller.activePlateIndex)
    }

    @Test
    fun openingToolReplacesPanelAndStaleToolDismissDoesNotClearNewPanel() {
        val controller = WorkspaceController()
        var previousPanelDismissed = 0
        var toolDismissed = 0
        controller.activatePanel(WorkspaceOverlayPanel.PLATE_PICKER) { previousPanelDismissed++ }
        assertEquals(WorkspaceOverlayPanel.PLATE_PICKER, controller.activePanel)

        controller.selectTool("transform.scale") { toolDismissed++ }

        assertEquals(1, previousPanelDismissed)
        assertEquals("transform.scale", controller.activeToolId)
        assertEquals(WorkspaceOverlayPanel.SPECIAL_TOOL, controller.activePanel)
        controller.activatePanel(WorkspaceOverlayPanel.FILAMENT_PALETTE) {}
        assertEquals(1, toolDismissed)
        controller.clearTool("transform.scale")
        assertNull(controller.activeToolId)
        assertEquals(WorkspaceOverlayPanel.FILAMENT_PALETTE, controller.activePanel)
    }
}
