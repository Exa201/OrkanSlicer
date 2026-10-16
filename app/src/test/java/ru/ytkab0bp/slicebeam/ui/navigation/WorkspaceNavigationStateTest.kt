package ru.ytkab0bp.slicebeam.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceNavigationStateTest {
    @Test
    fun validDestinationUpdatesSelectedSlot() {
        val state = WorkspaceNavigationState()

        assertTrue(state.selectDestination(2, 6))

        assertEquals(2, state.selectedDestinationSlot)
    }

    @Test
    fun invalidDestinationKeepsPreviousSelection() {
        val state = WorkspaceNavigationState(initialDestinationSlot = 1)

        assertFalse(state.selectDestination(6, 6))
        assertFalse(state.selectDestination(-1, 6))

        assertEquals(1, state.selectedDestinationSlot)
    }

    @Test
    fun toolPanelStateCanBeExpandedAndCollapsed() {
        val state = WorkspaceNavigationState()

        state.toolPanelExpanded = true
        assertTrue(state.toolPanelExpanded)

        state.toolPanelExpanded = false
        assertFalse(state.toolPanelExpanded)
    }
}
