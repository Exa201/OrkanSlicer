package ru.ytkab0bp.slicebeam.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R

@Composable
fun ProfileFilterBar(
    initialQuery: String,
    initialLevel: Int,
    initialFavoritesOnly: Boolean,
    onQueryChanged: (String) -> Unit,
    onLevelChanged: (Int) -> Unit,
    onFavoritesChanged: (Boolean) -> Unit,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    showToggle: Boolean = true,
) {
    val levels = stringArrayResource(R.array.SettingsLevels)
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var selectedLevel by rememberSaveable {
        mutableIntStateOf(initialLevel.coerceIn(0, (levels.size - 1).coerceAtLeast(0)))
    }
    var favoritesOnly by rememberSaveable { mutableStateOf(initialFavoritesOnly) }
    LaunchedEffect(initialQuery) { query = initialQuery }
    LaunchedEffect(initialFavoritesOnly) { favoritesOnly = initialFavoritesOnly }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    val showFilters = expanded ?: filtersExpanded
    val stacked = ProfileSettingsMenuLayoutPolicy.usesStackedFilterControls(
        LocalConfiguration.current.screenWidthDp,
        ru.ytkab0bp.slicebeam.ui.LocalBeamLayoutFontScale.current,
    )
    val favoritesDescription = stringResource(R.string.ProfileSettingsFavoritesToggle)
    val favoritesStateDescription = stringResource(
        if (favoritesOnly) R.string.ProfileSettingsFavoritesOnlyEnabled
        else R.string.ProfileSettingsFavoritesOnlyDisabled,
    )

    val search: @Composable (Modifier) -> Unit = { modifier ->
        ProfileSearchField(
            query = query,
            onValueChanged = { value ->
                query = value
                onQueryChanged(value.trim())
            },
            modifier = modifier,
        )
    }
    val level: @Composable (Modifier) -> Unit = { modifier ->
        ProfileLevelPicker(
            levels = levels,
            selectedLevel = selectedLevel,
            stacked = stacked,
            onSelected = { index ->
                selectedLevel = index
                onLevelChanged(index)
            },
            modifier = modifier,
        )
    }
    val favorites: @Composable (Modifier) -> Unit = { modifier ->
        IconButton(
            onClick = {
                favoritesOnly = !favoritesOnly
                onFavoritesChanged(favoritesOnly)
            },
            modifier = modifier
                .size(52.dp)
                .semantics {
                    contentDescription = favoritesDescription
                    stateDescription = favoritesStateDescription
                },
        ) {
            Text(
                text = if (favoritesOnly) "★" else "☆",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showToggle) OutlinedButton(
            onClick = { if (onToggle != null) onToggle() else filtersExpanded = !filtersExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .heightIn(min = 48.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.ProfileSettingsSearchAndFilter),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    fontSize = 15.sp,
                )
                Text(
                    text = if (showFilters) "⌃" else "⌄",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showFilters) search(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp))
        if (showFilters && stacked) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    level(Modifier.weight(1f))
                    favorites(Modifier)
                }
            }
        } else if (showFilters) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                level(Modifier.weight(1f))
                favorites(Modifier)
            }
        }
    }
}

@Composable
private fun ProfileSearchField(
    query: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onValueChanged,
        modifier = modifier.heightIn(min = 56.dp),
        singleLine = true,
        textStyle = TextStyle(fontSize = 16.sp, lineHeight = 20.sp),
        label = {
            Text(
                stringResource(R.string.SettingsSearchHint),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun ProfileLevelPicker(
    levels: Array<String>,
    selectedLevel: Int,
    stacked: Boolean,
    onSelected: (Int) -> Unit,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(
                text = levels.getOrElse(selectedLevel) { levels.firstOrNull().orEmpty() },
                maxLines = if (stacked) 2 else 1,
                softWrap = stacked,
                overflow = if (stacked) TextOverflow.Clip else TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                lineHeight = 19.sp,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            levels.forEachIndexed { index, title ->
                DropdownMenuItem(
                    text = { Text(title, fontSize = 16.sp) },
                    onClick = {
                        expanded = false
                        onSelected(index)
                    },
                )
            }
        }
    }
}
