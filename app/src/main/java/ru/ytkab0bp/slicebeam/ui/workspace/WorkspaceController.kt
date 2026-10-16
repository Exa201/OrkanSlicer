package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.runtime.Composable
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

    var activeToolPanelContent by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    val activePanel: WorkspaceOverlayPanel?
        get() = overlays.activePanel

    fun selectDestination(slot: Int): Boolean = navigationState.selectDestination(
        slot,
        WorkspaceDestination.values().size,
    )

    /**
     * Applies a user initiated workspace route change as one state transition.
     * Transient surfaces are dismissed before the destination changes so their
     * callbacks cannot leave a stale tool or panel selected on the next route.
     */
    fun navigateToDestination(slot: Int): Boolean {
        if (slot !in WorkspaceDestination.values().indices) return false
        if (slot == selectedDestinationSlot) return true

        dismissActivePanel()
        activeToolId = null
        activeToolPanelContent = null
        return selectDestination(slot)
    }

    fun setActivePlate(index: Int): Boolean {
        if (index < 0) return false
        activePlateIndex = index
        return true
    }

    fun selectTool(toolId: String, dismiss: () -> Unit) =
        activateTool(toolId, dismiss, null)

    fun selectToolWithPanel(
        toolId: String,
        dismiss: () -> Unit,
        panelContent: @Composable () -> Unit,
    ) = activateTool(toolId, dismiss, panelContent)

    private fun activateTool(
        toolId: String,
        dismiss: () -> Unit,
        panelContent: (@Composable () -> Unit)?,
    ) {
        // SPECIAL_TOOL is one panel kind, but selecting a different tool must still
        // dismiss the previous tool host instead of silently replacing its callback.
        if (activeToolId != null && activeToolId != toolId) dismissActivePanel()
        activeToolId = toolId
        activeToolPanelContent = panelContent
        overlays.activate(WorkspaceOverlayPanel.SPECIAL_TOOL, dismiss)
    }

    fun clearTool(toolId: String) {
        // A dismissed GL tool may finish cleanup asynchronously. Never let that stale
        // callback clear the exclusive panel that a newer tool has already claimed.
        if (activeToolId != toolId) return
        activeToolId = null
        activeToolPanelContent = null
        overlays.clear(WorkspaceOverlayPanel.SPECIAL_TOOL)
    }

    fun registerPanel(panel: WorkspaceOverlayPanel, dismiss: () -> Unit) =
        overlays.register(panel, dismiss)

    fun activatePanel(panel: WorkspaceOverlayPanel, dismiss: () -> Unit) =
        overlays.activate(panel, dismiss)

    fun clearPanel(panel: WorkspaceOverlayPanel): Boolean = overlays.clear(panel)

    fun dismissActivePanel(): Boolean = overlays.dismissActive()
}
