package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.dp

/** Responsive sheet for native-renderer tool controls now presented by Compose. */
@Composable
internal fun WorkspaceModePanelHost(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissLabel = stringResource(android.R.string.cancel)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxWidth < maxHeight
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(onClick = onDismiss)
                .semantics {
                    contentDescription = dismissLabel
                    role = Role.Button
                },
        )
        Surface(
            modifier = (if (portrait) Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth().heightIn(max = maxHeight * 0.82f)
            else Modifier.align(Alignment.CenterEnd).widthIn(max = 420.dp).fillMaxSize()),
            shape = if (portrait) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                else RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
        ) {
            content()
        }
    }
}
