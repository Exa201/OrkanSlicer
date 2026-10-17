package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.runtime.Composable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceDestination

class WorkspaceControllerTest {
    @Test
    fun activeToolPanelContentIsOwnedAndClearedByWorkspaceController() {
        val controller = WorkspaceController()
        val panelContent: @Composable () -> Unit = {}

        controller.selectToolWithPanel("measure-mode", {}, panelContent, panelFraction = 0.72f)

        assertEquals("measure-mode", controller.activeToolId)
        assertSame(panelContent, controller.activeToolPanelContent)
        assertEquals(0.72f, controller.activeToolPanelFraction!!, 0f)

        controller.clearTool("measure-mode")

        assertNull(controller.activeToolId)
        assertNull(controller.activeToolPanelContent)
        assertNull(controller.activeToolPanelFraction)
    }

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

    @Test
    fun glModeAndUnfoldToolShareExclusiveBackDismissal() {
        val controller = WorkspaceController()
        var glModeClosed = 0
        var routeClosed = 0

        controller.selectTool("paint-mode") {
            glModeClosed++
            controller.clearTool("paint-mode")
        }
        assertEquals(WorkspaceOverlayPanel.SPECIAL_TOOL, controller.activePanel)
        assertTrue(controller.dismissActivePanel())
        assertEquals(1, glModeClosed)
        assertNull(controller.activeToolId)

        controller.selectTool("transform.scale") {
            routeClosed++
            controller.clearTool("transform.scale")
        }
        controller.selectTool("emboss-mode") {
            glModeClosed++
            controller.clearTool("emboss-mode")
        }
        assertEquals(1, routeClosed)
        assertEquals("emboss-mode", controller.activeToolId)
        assertTrue(controller.dismissActivePanel())
        assertEquals(2, glModeClosed)
        assertNull(controller.activeToolId)
    }

    @Test
    fun dismissActiveToolUsesItsRouteCallbackAndClearsOnlyToolState() {
        val controller = WorkspaceController()
        var dismissCount = 0
        var paletteDismissed = false
        controller.activatePanel(WorkspaceOverlayPanel.FILAMENT_PALETTE) { paletteDismissed = true }
        controller.selectTool("transform.position") {
            dismissCount++
            controller.clearTool("transform.position")
        }

        assertTrue(controller.dismissActiveTool())
        assertTrue(paletteDismissed)
        assertEquals(1, dismissCount)
        assertNull(controller.activeToolId)
        assertNull(controller.activeToolPanelContent)
        assertNull(controller.activePanel)
        assertFalse(controller.dismissActiveTool())
    }

    @Test
    fun delayedDismissalOfPreviousToolCannotClearTheReplacementTool() {
        val controller = WorkspaceController()
        var delayedCleanup: (() -> Unit)? = null
        controller.selectTool("paint-mode") { delayedCleanup = { controller.clearTool("paint-mode") } }

        controller.selectTool("emboss-mode") { controller.clearTool("emboss-mode") }
        delayedCleanup?.invoke()

        assertEquals("emboss-mode", controller.activeToolId)
        assertEquals(WorkspaceOverlayPanel.SPECIAL_TOOL, controller.activePanel)
    }

    @Test
    fun modelContextDialogReplacesPlatePickerAndBackDismissesIt() {
        val controller = WorkspaceController()
        var plateDismissed = false
        var dialogDismissed = false
        controller.activatePanel(WorkspaceOverlayPanel.PLATE_PICKER) { plateDismissed = true }

        controller.activatePanel(WorkspaceOverlayPanel.MODEL_CONTEXT_MENU) { dialogDismissed = true }

        assertTrue(plateDismissed)
        assertEquals(WorkspaceOverlayPanel.MODEL_CONTEXT_MENU, controller.activePanel)
        assertTrue(controller.dismissActivePanel())
        assertTrue(dialogDismissed)
        assertNull(controller.activePanel)
    }

    @Test
    fun composePanelContentIsOwnedReplacedAndClearedWithItsOverlay() {
        val controller = WorkspaceController()
        val firstContent: @Composable () -> Unit = {}
        val secondContent: @Composable () -> Unit = {}

        controller.activatePanel(WorkspaceOverlayPanel.MODEL_CONTEXT_MENU, firstContent) {}
        assertSame(firstContent, controller.activePanelContent)

        controller.activatePanel(WorkspaceOverlayPanel.MESSAGE_DIALOG, secondContent) {}
        assertSame(secondContent, controller.activePanelContent)
        assertEquals(WorkspaceOverlayPanel.MESSAGE_DIALOG, controller.activePanel)

        assertTrue(controller.dismissActivePanel())
        assertNull(controller.activePanelContent)
        assertNull(controller.activePanel)
    }

    @Test
    fun navigationChangeClosesTransientToolAndPanelAsOneTransition() {
        val controller = WorkspaceController()
        var dismissed = 0
        controller.selectTool("transform.scale") { dismissed++ }

        assertTrue(controller.navigateToDestination(WorkspaceDestination.EDIT.ordinal))

        assertEquals(WorkspaceDestination.EDIT.ordinal, controller.selectedDestinationSlot)
        assertNull(controller.activeToolId)
        assertNull(controller.activePanel)
        assertEquals(1, dismissed)
    }

    @Test
    fun invalidNavigationDoesNotDismissCurrentPanel() {
        val controller = WorkspaceController()
        var dismissed = 0
        controller.activatePanel(WorkspaceOverlayPanel.FILAMENT_PALETTE) { dismissed++ }

        assertFalse(controller.navigateToDestination(WorkspaceDestination.values().size))

        assertEquals(WorkspaceDestination.PROJECT.ordinal, controller.selectedDestinationSlot)
        assertEquals(WorkspaceOverlayPanel.FILAMENT_PALETTE, controller.activePanel)
        assertEquals(0, dismissed)
    }

    @Test
    fun messageDialogReplacesOtherOverlayAndBackDismissesIt() {
        val controller = WorkspaceController()
        var oldPanelDismissed = false
        var messageDismissed = false
        controller.activatePanel(WorkspaceOverlayPanel.PROFILE_PICKER) { oldPanelDismissed = true }

        controller.activatePanel(WorkspaceOverlayPanel.MESSAGE_DIALOG) { messageDismissed = true }

        assertTrue(oldPanelDismissed)
        assertEquals(WorkspaceOverlayPanel.MESSAGE_DIALOG, controller.activePanel)
        assertTrue(controller.dismissActivePanel())
        assertTrue(messageDismissed)
        assertNull(controller.activePanel)
    }
}
