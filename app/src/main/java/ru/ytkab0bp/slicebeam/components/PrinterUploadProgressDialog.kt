package ru.ytkab0bp.slicebeam.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.utils.ViewUtils

/** Non-cancelable upload progress presentation; network and print-start decisions stay in the controller. */
class PrinterUploadProgressDialog(
    private val context: Context,
    initialMessage: String,
) {
    private var state by mutableStateOf(State(initialMessage))

    @Volatile
    private var uiAttached = true

    val isUiAttached: Boolean
        get() = uiAttached

    fun setMessage(message: String) {
        ViewUtils.postOnMainThread {
            if (uiAttached) state = state.copy(message = message)
        }
    }

    fun setProgress(percent: Int, message: String) {
        ViewUtils.postOnMainThread {
            if (uiAttached) state = state.copy(message = message, percent = percent.coerceIn(0, 100))
        }
    }

    internal fun detachFromWorkspace() {
        uiAttached = false
    }

    @Composable
    fun Content() {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                BeamComposeTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .background(
                                Color(ThemesRepo.getColor(R.attr.dialogBackground)),
                                RoundedCornerShape(24.dp),
                            )
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = context.getString(R.string.MenuSliceSendToPrinterLoading),
                            modifier = Modifier.fillMaxWidth().testTag("printer-upload-progress-title"),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = state.message,
                            modifier = Modifier.fillMaxWidth().testTag("printer-upload-progress-status"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val percent = state.percent
                        if (percent == null) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 4.dp)
                                    .testTag("printer-upload-progress-indicator"),
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { percent / 100f },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 4.dp)
                                    .testTag("printer-upload-progress-indicator"),
                            )
                        }
                    }
                }
            }
        }
    }

    private data class State(val message: String, val percent: Int? = null)
}
