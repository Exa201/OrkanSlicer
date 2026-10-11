package ru.ytkab0bp.slicebeam.components

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.SlicingProgressEvent
import ru.ytkab0bp.slicebeam.slic3r.Slic3rLocalization
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import java.util.Locale

class SliceProgressBottomSheet(context: Context) : BottomSheetDialog(context) {
    private var progressState by mutableStateOf(SliceProgressState(context.getString(R.string.SlicePreparing)))
    private val eventBridge = SliceProgressEventBridge { event -> onProgressChanged(event) }

    init {
        setCancelable(false)
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                BeamComposeTheme {
                    SliceProgressContent(progressState)
                }
            }
        }
        setContentView(
            composeView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
    }

    override fun show() {
        super.show()
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
    }

    fun onProgressChanged(event: SlicingProgressEvent) {
        progressState = progressState.apply(event) { message ->
            localizeProgressMessage(Slic3rLocalization.getString(message))
        }
        if (progressState.completed) dismiss()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SliceBeam.EVENT_BUS.registerListener(eventBridge)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        SliceBeam.EVENT_BUS.unregisterListener(eventBridge)
    }

    private fun localizeProgressMessage(message: String): String {
        val lower = message.lowercase(Locale.ROOT)
        return when {
            "perimeter" in lower -> context.getString(R.string.SliceStepPerimeters)
            "infill" in lower -> context.getString(R.string.SliceStepInfill)
            "support" in lower -> context.getString(R.string.SliceStepSupports)
            "export" in lower || "g-code" in lower || "gcode" in lower -> context.getString(R.string.SliceStepExport)
            "slice" in lower || "slicing" in lower -> context.getString(R.string.SliceStepGeometry)
            "skirt" in lower || "brim" in lower -> context.getString(R.string.SliceStepAdhesion)
            else -> message
        }
    }
}

@Composable
private fun SliceProgressContent(state: SliceProgressState) {
    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val maxLogHeight = (screenHeight * if (screenWidth < screenHeight) 0.4f else 0.6f).coerceAtLeast(160.dp)
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(ThemesRepo.getColor(R.attr.dialogBackground)),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            )
            .padding(top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.SliceInProgress),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = state.statusMessage,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.SliceProgressPercent, state.progress),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        LinearProgressIndicator(
            progress = { state.progress / 100f },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp, max = maxLogHeight),
        ) {
            itemsIndexed(state.messages, key = { index, message -> "$index:$message" }) { _, message ->
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
