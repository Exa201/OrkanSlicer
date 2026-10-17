package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.ytkab0bp.slicebeam.R
import java.util.Locale

/** A large, theme-aware selection field shared by the quick and smart slice forms. */
@Composable
internal fun SliceChoiceField(
    label: String,
    selectedIndex: Int,
    options: List<String>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(options) { mutableStateOf("") }
    val selectedLabel = options.getOrNull(selectedIndex).orEmpty().ifBlank { "—" }
    val searchable = options.size >= SliceChoiceFilter.SEARCH_THRESHOLD
    val visibleOptions = remember(options, query) { SliceChoiceFilter.indices(options, query) }
    val availableDialogHeight = (LocalConfiguration.current.screenHeightDp - 48).coerceAtLeast(180)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        Box {
            OutlinedButton(
                onClick = { query = ""; expanded = true },
                enabled = options.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = selectedLabel,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp)
                }
            }
            if (searchable) {
                if (expanded) {
                    Dialog(
                        onDismissRequest = { expanded = false; query = "" },
                        properties = DialogProperties(usePlatformDefaultWidth = false),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                .widthIn(max = 520.dp).heightIn(max = availableDialogHeight.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column(
                                Modifier.fillMaxWidth()
                                    .heightIn(min = minOf(260, availableDialogHeight).dp, max = availableDialogHeight.dp)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(label, style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    label = { Text(stringResource(R.string.SliceChoiceSearchHint)) },
                                )
                                if (visibleOptions.isEmpty()) {
                                    Text(
                                        stringResource(R.string.SliceChoiceNoMatches),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 12.dp),
                                    )
                                } else {
                                    LazyColumn(Modifier.weight(1f).heightIn(min = 0.dp)) {
                                        items(visibleOptions, key = { it }) { index ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = options[index],
                                                        color = if (index == selectedIndex) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 16.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                },
                                                onClick = {
                                                    onSelected(index)
                                                    expanded = false
                                                    query = ""
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.heightIn(max = 360.dp),
                ) {
                    Column(Modifier.widthIn(min = 240.dp)) {
                        options.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        color = if (index == selectedIndex) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                onClick = {
                                    onSelected(index)
                                    expanded = false
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Keeps long profile menus cheap to compose while preserving the source indices for selection. */
internal object SliceChoiceFilter {
    const val SEARCH_THRESHOLD = 12

    fun indices(options: List<String>, query: String): List<Int> {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        if (normalizedQuery.isEmpty()) return options.indices.toList()
        return options.indices.filter { options[it].lowercase(Locale.ROOT).contains(normalizedQuery) }
    }
}
