package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Mutually-exclusive transient workspace panels. All calls are made from the Android UI thread. */
internal enum class WorkspaceOverlayPanel {
    PLATE_PICKER,
    FILAMENT_PALETTE,
    PROFILE_PICKER,
    PRINTER_WEB,
}

/** Keeps one dismissible workspace panel active and routes Back to that panel first. */
internal class WorkspaceOverlayCoordinator {
    var activePanel by mutableStateOf<WorkspaceOverlayPanel?>(null)
        private set

    private var dismissActivePanel: (() -> Unit)? = null

    fun activate(panel: WorkspaceOverlayPanel, dismiss: () -> Unit) {
        if (activePanel == panel) {
            dismissActivePanel = dismiss
            return
        }

        val previousDismiss = dismissActivePanel
        activePanel = null
        dismissActivePanel = null
        previousDismiss?.invoke()

        activePanel = panel
        dismissActivePanel = dismiss
    }

    /** Registers a dialog that has already been constructed after the previous panel closed. */
    fun register(panel: WorkspaceOverlayPanel, dismiss: () -> Unit) {
        activate(panel, dismiss)
    }

    fun clear(panel: WorkspaceOverlayPanel): Boolean {
        if (activePanel != panel) return false
        activePanel = null
        dismissActivePanel = null
        return true
    }

    fun dismiss(panel: WorkspaceOverlayPanel): Boolean {
        if (activePanel != panel) return false
        val dismiss = dismissActivePanel
        activePanel = null
        dismissActivePanel = null
        dismiss?.invoke()
        return true
    }

    fun dismissActive(): Boolean {
        val panel = activePanel ?: return false
        return dismiss(panel)
    }
}
