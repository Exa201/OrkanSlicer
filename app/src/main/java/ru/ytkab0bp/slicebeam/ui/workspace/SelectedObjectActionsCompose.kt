package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R

internal enum class SelectedObjectQuickAction(val labelRes: Int) {
    MOVE(R.string.ObjectActionMove),
    ROTATE(R.string.ObjectActionRotate),
    SCALE(R.string.ObjectActionScale),
    DUPLICATE(R.string.ObjectActionDuplicate),
}

internal object SelectedObjectQuickActionPolicy {
    val actions = listOf(
        SelectedObjectQuickAction.MOVE,
        SelectedObjectQuickAction.ROTATE,
        SelectedObjectQuickAction.SCALE,
        SelectedObjectQuickAction.DUPLICATE,
    )
}

@Composable
internal fun SelectedObjectActionsBar(
    title: String,
    selectionKey: Int,
    onAction: (SelectedObjectQuickAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            androidx.compose.runtime.key(selectionKey) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelectedObjectQuickActionPolicy.actions.forEach { action ->
                        SelectedObjectActionButton(
                            label = stringResource(action.labelRes),
                            onClick = { onAction(action) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedObjectActionButton(
    label: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
