package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R

/** Per-install menu customization. Action handlers stay owned by their original menus. */
@Composable
internal fun CustomizableBedMenuActionsContent(
    menuKey: String,
    actions: List<BedComposeMenuAction>,
    portrait: Boolean,
    multiRow: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val actionKeys = remember(actions) { actions.map { context.resources.getResourceEntryName(it.titleRes) } }
    val preferences = remember(context) { context.getSharedPreferences("workspace_menu_customization", 0) }
    fun orderFromPreferences(keys: List<String>): List<String> {
        val saved = preferences.getString("order_$menuKey", null).orEmpty().split('|').filter(String::isNotBlank)
        return saved.filter { it in keys } + keys.filter { it !in saved }
    }
    var order by remember(menuKey, actionKeys) { mutableStateOf(orderFromPreferences(actionKeys)) }
    var hidden by remember(menuKey, actionKeys) {
        mutableStateOf(preferences.getStringSet("hidden_$menuKey", emptySet()).orEmpty().intersect(actionKeys.toSet()))
    }
    var editing by remember(menuKey) { mutableStateOf(false) }
    var draftOrder by remember(menuKey) { mutableStateOf(emptyList<String>()) }
    var draftHidden by remember(menuKey) { mutableStateOf(emptySet<String>()) }
    val actionByKey = remember(actions, actionKeys) { actionKeys.zip(actions).toMap() }
    val visibleActions = order.filterNot { it in hidden }.mapNotNull(actionByKey::get)

    val openCustomization = {
        draftOrder = order.toList()
        draftHidden = hidden.toSet()
        editing = true
    }
    if (portrait) {
        // Keep the customization control beside the horizontal action strip. A separate
        // header row consumed most of the short workspace panel on compact phones and
        // reduced every action to an unusable touch target.
        Row(modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            BedMenuActionsContent(visibleActions, portrait, multiRow, Modifier.weight(1f))
            TextButton(
                onClick = openCustomization,
                modifier = Modifier.width(48.dp).fillMaxSize().testTag("menu-customize-$menuKey"),
            ) {
                Text("⋮", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { contentDescription = context.getString(R.string.MenuCustomize) })
            }
        }
    } else {
        Column(modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = openCustomization,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("menu-customize-$menuKey"),
                ) { Text(stringResource(R.string.MenuCustomize)) }
            }
            BedMenuActionsContent(visibleActions, portrait, multiRow, Modifier.weight(1f))
        }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.MenuCustomize)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 520.dp).testTag("menu-customize-list-$menuKey")) {
                    itemsIndexed(draftOrder, key = { _, key -> key }) { index, key ->
                        val action = actionByKey[key] ?: return@itemsIndexed
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = key !in draftHidden,
                                onCheckedChange = { checked ->
                                    draftHidden = if (checked) draftHidden - key else draftHidden + key
                                },
                                modifier = Modifier
                                    .semantics { contentDescription = context.getString(action.titleRes) }
                                    .testTag("menu-customize-visible-$menuKey-$key"),
                            )
                            Text(stringResource(action.titleRes), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(enabled = index > 0, onClick = {
                                val moved = draftOrder.toMutableList()
                                moved.add(index - 1, moved.removeAt(index))
                                draftOrder = moved
                            }, modifier = Modifier.heightIn(min = 48.dp)
                                .semantics { contentDescription = context.getString(R.string.MenuCustomizeMoveUp) }
                                .testTag("menu-customize-up-$menuKey-$key")) { Text("↑") }
                            TextButton(enabled = index < draftOrder.lastIndex, onClick = {
                                val moved = draftOrder.toMutableList()
                                moved.add(index + 1, moved.removeAt(index))
                                draftOrder = moved
                            }, modifier = Modifier.heightIn(min = 48.dp)
                                .semantics { contentDescription = context.getString(R.string.MenuCustomizeMoveDown) }
                                .testTag("menu-customize-down-$menuKey-$key")) { Text("↓") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    order = draftOrder
                    hidden = draftHidden
                    preferences.edit()
                        .putString("order_$menuKey", draftOrder.joinToString("|"))
                        .putStringSet("hidden_$menuKey", draftHidden)
                        .apply()
                    editing = false
                }, modifier = Modifier.heightIn(min = 48.dp).testTag("menu-customize-save-$menuKey")) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        draftOrder = actionKeys
                        draftHidden = emptySet()
                    }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.MenuCustomizeReset)) }
                    TextButton(onClick = { editing = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            },
        )
    }
}
