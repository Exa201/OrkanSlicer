package ru.ytkab0bp.slicebeam.components

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.FilamentTypes

/** Editor for one filament: choose its material type and color, or delete it. */
open class FilamentSlotSheet(
    context: Context,
    private val slot: FilamentSlot,
    private val canDelete: Boolean,
    private val listener: Listener?,
) : BottomSheetDialog(context) {

    interface Listener {
        fun onChanged(slot: FilamentSlot)
        fun onDelete()
    }

    init {
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                BeamComposeTheme {
                    FilamentSlotContent(
                        slot = slot,
                        canDelete = canDelete,
                        onChanged = { listener?.onChanged(slot) },
                        onDelete = {
                            listener?.onDelete()
                            dismiss()
                        },
                    )
                }
            }
        }
        setContentView(
            composeView,
            android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }
}

@Composable
private fun FilamentSlotContent(
    slot: FilamentSlot,
    canDelete: Boolean,
    onChanged: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    var color by remember(slot) { mutableStateOf(slot.color) }
    var material by remember(slot) { mutableStateOf(slot.type.orEmpty()) }
    var showMaterialPicker by remember { mutableStateOf(false) }
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
            onClick = {
                ColorChooserSheet(context, color) { selectedColor ->
                    color = selectedColor
                    slot.color = selectedColor
                    onChanged()
                }.show()
            },
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
                                slot.type = option
                                onChanged()
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
