package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceToolPanelLayoutPolicyTest {
    @Test
    fun fullWindowMenuIsClampedToSpaceInsideSystemInsets() {
        assertEquals(720, WorkspaceToolPanelLayoutPolicy.constrainSide(requestedSize = 748, availableSize = 720))
    }

    @Test
    fun regularSheetKeepsItsRequestedSizeWhenItFits() {
        assertEquals(360, WorkspaceToolPanelLayoutPolicy.constrainSide(requestedSize = 360, availableSize = 720))
    }

    @Test
    fun invalidDimensionsNeverProduceANegativeSide() {
        assertEquals(0, WorkspaceToolPanelLayoutPolicy.constrainSide(requestedSize = -1, availableSize = 720))
        assertEquals(0, WorkspaceToolPanelLayoutPolicy.constrainSide(requestedSize = 100, availableSize = -4))
    }
}
