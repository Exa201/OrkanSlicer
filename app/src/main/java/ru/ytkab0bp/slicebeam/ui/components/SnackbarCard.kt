package ru.ytkab0bp.slicebeam.ui.components

import androidx.core.graphics.ColorUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.view.SnackbarsLayout

/** Compose presentation for status cards; the host owns stacking and timed dismissal. */
@Composable
internal fun SnackbarCard(
    type: SnackbarsLayout.Type,
    title: CharSequence,
    buttonTitle: CharSequence?,
    onButtonClick: () -> Unit,
    onHorizontalDrag: (Float) -> Unit,
    onDragEnd: (Float) -> Unit,
    onDragCancel: () -> Unit,
) {
    val (colorAttribute, iconResource) = when (type) {
        SnackbarsLayout.Type.DONE -> R.attr.snackbarDone to R.drawable.done_outline_28
        SnackbarsLayout.Type.WARNING -> R.attr.snackbarWarning to R.drawable.warning_triangle_outline_28
        SnackbarsLayout.Type.LOADING, SnackbarsLayout.Type.INFO -> R.attr.snackbarInfo to R.drawable.info_outline_28
        SnackbarsLayout.Type.ERROR -> R.attr.snackbarError to R.drawable.error_outline_28
    }
    // Resolve theme attributes during composition so ThemeAwareComposeView updates both colors
    // when the user switches between light and dark mode.
    val accentColorValue = ThemesRepo.getColor(colorAttribute)
    val baseColorValue = ThemesRepo.getColor(R.attr.snackbarBase)
    val cardColor = Color(
        ColorUtils.blendARGB(baseColorValue, accentColorValue, 0.15f),
    )
    val accentColor = Color(accentColorValue)
    var velocityTracker = VelocityTracker()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .pointerInput(type, title, buttonTitle) {
                velocityTracker = VelocityTracker()
                detectHorizontalDragGestures(
                    onDragStart = { velocityTracker = VelocityTracker() },
                    onHorizontalDrag = { change, dragAmount ->
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        change.consume()
                        onHorizontalDrag(dragAmount)
                    },
                    onDragEnd = { onDragEnd(velocityTracker.calculateVelocity().x) },
                    onDragCancel = onDragCancel,
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        contentColor = accentColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (type == SnackbarsLayout.Type.LOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        color = accentColor,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        painter = painterResource(iconResource),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = accentColor,
                    )
                }
            }

            Text(
                text = title.toString(),
                modifier = Modifier.weight(1f),
                color = accentColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (buttonTitle != null) {
                TextButton(
                    onClick = onButtonClick,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                ) {
                    Text(
                        text = buttonTitle.toString(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}
