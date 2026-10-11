package ru.ytkab0bp.slicebeam.ui.settings

import ru.ytkab0bp.slicebeam.fragment.ProfileSettingsComposeItem

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.function.BooleanSupplier
import java.util.function.Consumer

/** Compose replacement for a single boolean Orca profile option, hosted in the legacy list. */
object ComposeProfileBooleanOption {
    @Composable
    fun Content(
        title: String,
        initialValue: Boolean,
        onChanged: Consumer<Boolean>,
        onLongClick: Runnable,
    ) = BooleanOptionRow(title, initialValue, onChanged, onLongClick)

    @JvmStatic
    fun create(
        title: String,
        valueProvider: BooleanSupplier,
        onChanged: Consumer<Boolean>,
        onLongClick: Runnable,
    ): ProfileSettingsComposeItem = BooleanOptionItem(title, valueProvider, onChanged, onLongClick)

    private class BooleanOptionItem(
        private val title: String,
        private val valueProvider: BooleanSupplier,
        private val onChanged: Consumer<Boolean>,
        private val onLongClick: Runnable,
    ) : ProfileSettingsComposeItem {
        @Composable
        override fun Content(rowPosition: Int, onEditorStateChanged: (Boolean) -> Unit) {
            val initialValue = valueProvider.asBoolean
            ComposeProfileBooleanOption.Content(title, initialValue, onChanged, onLongClick)
        }
    }

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun BooleanOptionRow(
    title: String,
    initialValue: Boolean,
    onChanged: Consumer<Boolean>,
    onLongClick: Runnable,
) {
    val checkedState = remember(initialValue) { mutableStateOf(initialValue) }
    val checked = checkedState.value
    val toggle: (Boolean) -> Unit = { next ->
        if (checkedState.value != next) {
            checkedState.value = next
            onChanged.accept(next)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(onClick = { toggle(!checkedState.value) }, onLongClick = onLongClick::run)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Switch(checked = checked, onCheckedChange = toggle)
        }
    }
}
}
