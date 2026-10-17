package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.ViewUtils

/** Shared accessible shell for Maker forms hosted by the active Compose workspace. */
@Composable
internal fun GeneratorWorkspaceDialog(
    title: Int,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(title),
                modifier = Modifier.testTag("generator-dialog-title"),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
            ) { content() }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp).testTag("generator-dialog-cancel"),
            ) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

/** Delivers generator failures through the same workspace-owned dialog route as its form. */
internal fun showGeneratorFailure(activity: Activity, bed: BedFragment, message: String) {
    ViewUtils.postOnMainThread {
        bed.showWorkspaceMessageDialog(
            title = activity.getString(R.string.ConverterFailed),
            message = message,
            confirmLabel = activity.getString(android.R.string.ok),
        )
    }
}
