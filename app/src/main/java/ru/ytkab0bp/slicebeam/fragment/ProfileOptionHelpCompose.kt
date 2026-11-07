package ru.ytkab0bp.slicebeam.fragment

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R

/** A profile option's localized help content and its existing favorite action. */
internal data class ProfileOptionHelpDialogState(
    val title: String,
    val message: String,
    val favoriteAction: Int,
    val onFavorite: () -> Unit,
)

@Composable
internal fun ProfileOptionHelpDialog(
    state: ProfileOptionHelpDialogState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            Text(
                state.message,
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    state.onFavorite()
                    onDismiss()
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(state.favoriteAction)) }
        },
    )
}
