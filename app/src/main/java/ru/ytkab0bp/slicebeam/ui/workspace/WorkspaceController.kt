package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceDestination
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceNavigationState

/** Single UI state owner for workspace navigation, active plate, tools, and transient surfaces. */
internal class WorkspaceController(
    initialDestinationSlot: Int = WorkspaceDestination.PROJECT.ordinal,
) {
    private val navigationState = WorkspaceNavigationState(initialDestinationSlot)
    private val overlays = WorkspaceOverlayCoordinator()

    val selectedDestinationSlot: Int
        get() = navigationState.selectedDestinationSlot

    var toolPanelExpanded: Boolean
        get() = navigationState.toolPanelExpanded
        set(value) { navigationState.toolPanelExpanded = value }

    var activePlateIndex by mutableIntStateOf(0)
        private set

    var activeToolId by mutableStateOf<String?>(null)
        private set

    val activePanel: WorkspaceOverlayPanel?
        get() = overlays.activePanel

    fun selectDestination(slot: Int): Boolean = navigationState.selectDestination(
        slot,
        WorkspaceDestination.values().size,
    )

    fun setActivePlate(index: Int): Boolean {
        if (index < 0) return false
        activePlateIndex = index
        return true
    }

    fun selectTool(toolId: String, dismiss: () -> Unit) {
        activeToolId = toolId
        overlays.activate(WorkspaceOverlayPanel.SPECIAL_TOOL, dismiss)
    }

    fun clearTool(toolId: String) {
        if (activeToolId == toolId) activeToolId = null
        overlays.clear(WorkspaceOverlayPanel.SPECIAL_TOOL)
    }

    fun registerPanel(panel: WorkspaceOverlayPanel, dismiss: () -> Unit) =
        overlays.register(panel, dismiss)

    fun activatePanel(panel: WorkspaceOverlayPanel, dismiss: () -> Unit) =
        overlays.activate(panel, dismiss)

    fun clearPanel(panel: WorkspaceOverlayPanel): Boolean = overlays.clear(panel)

    fun dismissActivePanel(): Boolean = overlays.dismissActive()
}
