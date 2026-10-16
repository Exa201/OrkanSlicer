package ru.ytkab0bp.slicebeam.ui.workspace

import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Stable Compose boundary for renderer-bound mode views that still own their GL callbacks. */
@Composable
internal fun WorkspaceLegacyModeHost(view: View) {
    AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
}
