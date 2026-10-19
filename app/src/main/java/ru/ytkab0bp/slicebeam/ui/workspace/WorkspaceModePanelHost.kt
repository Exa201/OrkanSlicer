package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Responsive sheet for native-renderer tool controls now presented by Compose. */
@Composable
internal fun WorkspaceModePanelHost(
    onDismiss: () -> Unit,
    panelSize: WorkspacePanelSize? = null,
    allowBackgroundInteraction: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dismissLabel = stringResource(android.R.string.cancel)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxWidth < maxHeight
        if (allowBackgroundInteraction) {
            // Paint mode must receive direct touches on the GL canvas while its controls
            // remain visible. A pointer-input scrim would swallow every brush stroke.
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.08f)))
        } else {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(onClick = onDismiss)
                    .semantics {
                        contentDescription = dismissLabel
                        role = Role.Button
                    },
            )
        }
        val dimension = panelSize?.let { if (portrait) it.portrait else it.landscape }
        val availableDp = if (portrait) maxHeight.value else maxWidth.value
        val requestedDp = dimension?.let { WorkspacePanelSizePolicy.resolve(it, availableDp) }
        val panelModifier = if (portrait) {
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(
                    if (requestedDp != null) Modifier.height(requestedDp.dp)
                    else Modifier.heightIn(max = maxHeight * 0.82f),
                )
        } else {
            Modifier.align(Alignment.CenterEnd)
                .then(
                    if (requestedDp != null) Modifier.width(requestedDp.dp)
                    else Modifier.widthIn(max = 420.dp),
                )
                .fillMaxHeight()
        }
        Surface(
            modifier = panelModifier.testTag("workspace-mode-panel"),
            shape = if (portrait) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                else RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
        ) {
            content()
        }
    }
}
