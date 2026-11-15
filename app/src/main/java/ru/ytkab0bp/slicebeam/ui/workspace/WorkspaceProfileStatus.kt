package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.OrkanUiTokens
import ru.ytkab0bp.slicebeam.ui.ColorFlow
import ru.ytkab0bp.slicebeam.ui.OrkanIcon

@Composable
internal fun WorkspaceProfileStatus(
    title: String,
    summary: String,
    accessibilityDescription: String,
    onOpen: () -> Unit,
) {
    val visibleTitle = if (title.isBlank()) stringResource(R.string.WorkspaceProfilesOpenLabel) else title
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = OrkanUiTokens.touchTarget)
            .semantics { contentDescription = accessibilityDescription }
            .clickable(role = Role.Button, onClick = onOpen),
        shape = RoundedCornerShape(OrkanUiTokens.cornerRadius),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = OrkanUiTokens.controlHorizontalPadding,
            ),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            OrkanIcon(R.drawable.printer_outline_28, null, size = OrkanUiTokens.menuIconSize,
                accent = ColorFlow.printer, secondaryAccent = ColorFlow.model)
            Spacer(Modifier.width(OrkanUiTokens.controlGap))
            Column(Modifier.weight(1f)) {
                Text(
                    text = visibleTitle,
                    modifier = Modifier.testTag("workspace-profile-title"),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    // Let long printer names use a second line before truncating. The
                    // profile picker and parent accessibility label retain the full value.
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.dropdown_24),
                contentDescription = null,
                tint = ColorFlow.printer,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
