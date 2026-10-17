package ru.ytkab0bp.slicebeam.ui.workspace

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp

/** Compose owns printer panel controls; only the restricted WebView remains an Android view. */
@Composable
internal fun PrinterWebWorkspacePanel(
    panelView: View,
    loading: Boolean,
    errorDescription: String?,
    onRetry: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { panelView }, modifier = Modifier.fillMaxSize())
        if (errorDescription != null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                PrinterWebErrorPanel(errorDescription, onRetry, onOpenInBrowser)
            }
        } else if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            PrinterWebCloseButton(onClose)
        }
    }
}
