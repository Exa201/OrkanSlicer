package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.OrkanUiTokens
import ru.ytkab0bp.slicebeam.utils.FilamentSlot

/** Compact palette entry point; the existing assignment dialog remains the source of truth. */
@Composable
internal fun WorkspaceFilamentButton(
    slots: List<FilamentSlot>,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(R.string.WorkspaceFilamentButtonDescription, slots.size)
    Surface(
        modifier = Modifier
            .size(OrkanUiTokens.touchTarget)
            .alpha(if (enabled) 1f else 0.55f)
            .semantics { contentDescription = label }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(OrkanUiTokens.cornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(R.drawable.print_filament_sync_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(OrkanUiTokens.menuIconSize),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                slots.take(4).forEach { slot ->
                    Surface(
                        modifier = Modifier.size(6.dp),
                        shape = CircleShape,
                        color = Color(slot.color),
                        content = {},
                    )
                }
            }
        }
    }
}
