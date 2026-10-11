package ru.ytkab0bp.slicebeam.ui.settings

import ru.ytkab0bp.slicebeam.fragment.ProfileSettingsComposeItem

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.function.Supplier

/** Material 3 profile row that opens the app's existing Orca-compatible color picker. */
object ComposeProfileColorOption {
    @Composable
    fun Content(
        title: String,
        initialValue: String,
        openPicker: Runnable,
        onLongClick: Runnable,
    ) = ColorOptionRow(title, initialValue, openPicker, onLongClick)

    @JvmStatic
    fun create(
        title: String,
        valueProvider: Supplier<String>,
        openPicker: Runnable,
        onLongClick: Runnable,
    ): ProfileSettingsComposeItem = ColorOptionItem(title, valueProvider, openPicker, onLongClick)

    private class ColorOptionItem(
        private val title: String,
        private val valueProvider: Supplier<String>,
        private val openPicker: Runnable,
        private val onLongClick: Runnable,
    ) : ProfileSettingsComposeItem {
        @Composable
        override fun Content(rowPosition: Int, onEditorStateChanged: (Boolean) -> Unit) {
            val initialValue = valueProvider.get().orEmpty()
            ComposeProfileColorOption.Content(title, initialValue, openPicker, onLongClick)
        }
    }

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ColorOptionRow(
    title: String,
    initialValue: String,
    openPicker: Runnable,
    onLongClick: Runnable,
) {
    val value = initialValue
    val fallbackColor = MaterialTheme.colorScheme.primary
    val swatch = remember(value, fallbackColor) {
        runCatching { Color(AndroidColor.parseColor(value)) }.getOrElse { fallbackColor }
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
                .combinedClickable(
                    onClick = openPicker::run,
                    onLongClick = onLongClick::run,
                )
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
            Surface(
                modifier = Modifier.size(width = 36.dp, height = 28.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(7.dp)),
                shape = RoundedCornerShape(7.dp),
                color = swatch,
            ) {}
            Text(
                text = value.ifBlank { "—" },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
}
