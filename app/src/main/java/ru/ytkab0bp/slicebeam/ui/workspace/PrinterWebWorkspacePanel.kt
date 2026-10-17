package ru.ytkab0bp.slicebeam.ui.workspace

import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Hosts the printer's technical WebView inside the exclusive Compose workspace overlay. */
@Composable
internal fun PrinterWebWorkspacePanel(panelView: View) {
    AndroidView(
        factory = { panelView },
        modifier = Modifier.fillMaxSize(),
    )
}
