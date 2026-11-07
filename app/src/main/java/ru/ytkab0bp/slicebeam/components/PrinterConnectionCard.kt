package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.OrkanUiTokens
import ru.ytkab0bp.slicebeam.utils.PrinterWebOriginPolicy

/** Compact route summary with a fixed, accessible entry to the configured printer web UI. */
@Composable
fun PrinterConnectionCard(
    configuredHost: String?,
    onOpenWeb: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayHost = PrinterWebOriginPolicy.dashboardUrl(configuredHost)
    val openWebDescription = stringResource(R.string.PrinterOpenWebDescription)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(OrkanUiTokens.compactCornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.PrinterConnectionTitle),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = displayHost ?: stringResource(R.string.PrinterWebHostMissing),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(
                onClick = onOpenWeb,
                modifier = Modifier
                    .heightIn(min = OrkanUiTokens.touchTarget)
                    .semantics {
                        contentDescription = openWebDescription
                    },
                shape = RoundedCornerShape(OrkanUiTokens.compactCornerRadius),
            ) {
                Icon(
                    painter = painterResource(R.drawable.globe_cross_outline_28),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.PrinterOpenWebShort), maxLines = 1)
            }
        }
    }
}
