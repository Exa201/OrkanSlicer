package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.FilamentTypes

/** Editor for one filament: choose its material type and color, or delete it. */
class FilamentSlotSheet(
    private val slot: FilamentSlot,
    private val canDelete: Boolean,
    private val listener: Listener?,
) {

    interface Listener {
        fun onChanged(slot: FilamentSlot)
        fun onDelete()
    }
    @Composable
    fun Content(onDismiss: () -> Unit) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                BeamComposeTheme {
                    FilamentSlotContent(
                        slot = slot,
                        canDelete = canDelete,
                        onSave = {
                            listener?.onChanged(slot)
                            onDismiss()
                        },
                        onCancel = onDismiss,
                        onDelete = {
                            listener?.onDelete()
                            onDismiss()
                        },
                    )
                }
            }
        }
    }

}

@Composable
private fun FilamentSlotContent(
    slot: FilamentSlot,
    canDelete: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    var color by remember(slot) { mutableStateOf(slot.color) }
    var material by remember(slot) { mutableStateOf(slot.type.orEmpty()) }
    var showMaterialPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    val materialTypes = remember { FilamentTypes.ALL.asList() }
    val previewShape = RoundedCornerShape(12.dp)
    val previewNeedsOutline = ColorUtils.calculateLuminance(color) > 0.85

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = screenHeight * 0.9f)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.SlotFilamentConfig),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(previewShape)
                .background(Color(color))
                .then(
                    if (previewNeedsOutline) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, previewShape)
                    } else {
                        Modifier
                    },
                ),
        )

        TextButton(
            onClick = { showColorPicker = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.FilamentSlotColor),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    fontSize = 16.sp,
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(color)),
                )
            }
        }

        TextButton(
            onClick = { showMaterialPicker = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.FilamentType),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    fontSize = 16.sp,
                )
                Text(
                    text = material.ifBlank { stringResource(R.string.FilamentType) },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                )
            }
        }

        if (canDelete) {
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.RemoveFilament), fontSize = 16.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(android.R.string.cancel))
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    slot.color = color
                    slot.type = material
                    onSave()
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag("filament-slot-save"),
            ) { Text(stringResource(R.string.FilamentSlotSave)) }
        }
    }

    if (showColorPicker) {
        ColorChooserDialog(
            initialColor = color,
            onDismiss = { showColorPicker = false },
            onColorChosen = { selectedColor ->
                color = selectedColor
            },
        )
    }

    if (showMaterialPicker) {
        AlertDialog(
            onDismissRequest = { showMaterialPicker = false },
            title = { Text(stringResource(R.string.FilamentType)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(materialTypes) { option ->
                        TextButton(
                            onClick = {
                                material = option
                                showMaterialPicker = false
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                                color = if (option == material) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMaterialPicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
