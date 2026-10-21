package ru.ytkab0bp.slicebeam.ui.workspace

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.ytkab0bp.slicebeam.ui.settings.TemporaryProjectSettingsBarLayoutPolicy

class WorkspaceShellLayoutPolicyTest {
    @Test
    fun workspaceHeaderReservesTheMaximumAppZoomForEachSystemFontScale() {
        assertEquals(58, WorkspaceShellLayoutPolicy.workspaceHeaderHeightDp(1f))
        assertEquals(73, WorkspaceShellLayoutPolicy.workspaceHeaderHeightDp(1.3f))
        assertEquals(58, WorkspaceShellLayoutPolicy.workspaceHeaderHeightDp(Float.NaN))
    }

    @Test
    fun temporaryProjectBannerReservesTwoExplanationLinesAtTheMaximumAppZoom() {
        assertEquals(96, TemporaryProjectSettingsBarLayoutPolicy.heightDp(1f))
        assertEquals(116, TemporaryProjectSettingsBarLayoutPolicy.heightDp(1.3f))
        assertEquals(96, TemporaryProjectSettingsBarLayoutPolicy.heightDp(Float.NaN))
    }

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

    @Test
    fun compactPortraitMenuOverlaysTheRendererAndFallsBackToOneScrollableRow() {
        assertEquals(160, WorkspaceShellLayoutPolicy.portraitMenuOverlayHeightDp(200, 176, 1.5f))
        assertEquals(2, WorkspaceShellLayoutPolicy.portraitMenuRows(160, 1.5f))
        assertEquals(80, WorkspaceShellLayoutPolicy.portraitMenuOverlayHeightDp(100, 176, 1.5f))
        assertEquals(1, WorkspaceShellLayoutPolicy.portraitMenuRows(80, 1.5f))
        assertEquals(140, WorkspaceShellLayoutPolicy.portraitMenuOverlayHeightDp(320, 140, 1f))
        assertEquals(2, WorkspaceShellLayoutPolicy.portraitMenuRows(140, 1f))
    }

    @Test
    fun portraitMenuNeverConsumesMoreThanItsRendererViewport() {
        assertEquals(0, WorkspaceShellLayoutPolicy.portraitMenuOverlayHeightDp(0, 176, 1.5f))
        assertEquals(40, WorkspaceShellLayoutPolicy.portraitMenuOverlayHeightDp(50, 176, 1.5f))
    }

    @Test
    fun dockedLayerControlsReserveSpaceOutsideThePlateRenderer() {
        assertEquals(200, WorkspaceShellLayoutPolicy.dockedPortraitPanelHeightDp(
            500, WorkspacePanelDimension.Fraction(0.4f), 1f,
        ))
        assertEquals(100, WorkspaceShellLayoutPolicy.dockedPortraitPanelHeightDp(
            250, WorkspacePanelDimension.Fraction(0.8f), 1f,
        ))
        assertEquals(0, WorkspaceShellLayoutPolicy.dockedPortraitPanelHeightDp(
            120, WorkspacePanelDimension.Fixed(240f), 1.5f,
        ))
        assertEquals(184, WorkspaceShellLayoutPolicy.dockedLandscapePanelWidthDp(
            464, WorkspacePanelDimension.Fixed(380f),
        ))
        assertEquals(0, WorkspaceShellLayoutPolicy.dockedLandscapePanelWidthDp(
            240, WorkspacePanelDimension.Fixed(180f),
        ))
    }
}
