package ru.ytkab0bp.slicebeam.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.OrkanUiTokens
import ru.ytkab0bp.slicebeam.view.FilamentPaletteController
import ru.ytkab0bp.slicebeam.view.FilamentPalettePolicy
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync

/** Opens the shared filament palette as a phone sheet or a fixed side panel on wide windows. */
class WorkspaceFilamentPaletteSheet(
    private val context: Context,
    private val controller: FilamentPaletteController,
    private val hasModel: Boolean,
    private val onAssignModel: () -> Unit,
    private val onSyncFilaments: ((MmuFilamentSync.Callback) -> Unit)? = null,
) {
    @Composable
    fun Content(onDismissed: () -> Unit) {
        val sidePanel = FilamentPalettePolicy.workspacePresentation(context.resources.configuration.screenWidthDp) ==
            FilamentPalettePolicy.WorkspacePresentation.SIDE_PANEL
        Dialog(
            onDismissRequest = onDismissed,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val panelModifier = if (sidePanel) {
                    Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(12.dp)
                        .widthIn(max = minOf(OrkanUiTokens.workspaceSidePanelWidth, maxWidth - 24.dp))
                } else {
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                }
                Box(panelModifier) {
                    BeamComposeTheme {
                        controller.WorkspaceSheetContent(
                            hasModel = hasModel,
                            sidePanel = sidePanel,
                            onSyncFilaments = onSyncFilaments,
                            onAssignModel = {
                                onDismissed()
                                Handler(Looper.getMainLooper()).post { onAssignModel() }
                            },
                            onDismiss = onDismissed,
                        )
                    }
                }
            }
        }
    }
}
