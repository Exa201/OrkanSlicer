package ru.ytkab0bp.slicebeam.ui.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceShellLayoutPolicyTest {
    @Test
    fun landscapeMenuWidthPreservesReadableActionsAndRendererSpace() {
        assertEquals(256, WorkspaceShellLayoutPolicy.menuPanelWidthDp(800, 1f))
        assertEquals(200, WorkspaceShellLayoutPolicy.menuPanelWidthDp(600, 1f))
        assertEquals(256, WorkspaceShellLayoutPolicy.menuPanelWidthDp(800, 1.5f))
        assertEquals(320, WorkspaceShellLayoutPolicy.menuPanelWidthDp(1400, 1.5f))
    }

    @Test
    fun collapsedPanelKeepsViewportAndFixedControlsVisible() {
        assertEquals(
            640,
            WorkspaceShellLayoutPolicy.plateViewportHeightDp(
                availableHeightDp = 800,
                projectBarHeightDp = 40,
                workspaceHeaderHeightDp = 64,
                navigationHeightDp = 56,
                toolPanelHeightDp = 140,
                toolPanelExpanded = false,
            ),
        )
    }

    @Test
    fun expandedPanelConsumesOnlyItsReservedSpace() {
        assertEquals(
            500,
            WorkspaceShellLayoutPolicy.plateViewportHeightDp(
                availableHeightDp = 800,
                projectBarHeightDp = 40,
                workspaceHeaderHeightDp = 64,
                navigationHeightDp = 56,
                toolPanelHeightDp = 140,
                toolPanelExpanded = true,
            ),
        )
    }

    @Test
    fun shortAndInvalidMeasurementsNeverProduceNegativeViewport() {
        assertEquals(
            0,
            WorkspaceShellLayoutPolicy.plateViewportHeightDp(
                availableHeightDp = 100,
                projectBarHeightDp = 40,
                workspaceHeaderHeightDp = 64,
                navigationHeightDp = 56,
                toolPanelHeightDp = 140,
                toolPanelExpanded = true,
            ),
        )
        assertEquals(
            0,
            WorkspaceShellLayoutPolicy.plateViewportHeightDp(-1, -1, -1, -1, -1, true),
        )
    }
}
