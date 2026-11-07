package ru.ytkab0bp.slicebeam.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** UI state shared by adaptive workspace navigation and its portrait tool panel. */
internal class WorkspaceNavigationState(initialDestinationSlot: Int = 0) {
    var selectedDestinationSlot by mutableIntStateOf(initialDestinationSlot)
        private set

    var toolPanelExpanded by mutableStateOf(false)

    fun selectDestination(slot: Int, destinationCount: Int): Boolean {
        if (slot !in 0 until destinationCount) return false
        selectedDestinationSlot = slot
        return true
    }
}
