package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale

/** Visible progress and elapsed/remaining time for local converter jobs. */
object ConverterProgressDialog {
    @JvmStatic
    fun show(activity: Activity, title: String, bed: BedFragment? = null): Progress =
        Progress.create(activity, title, bed)

    class Progress private constructor(activity: Activity, title: String, bed: BedFragment?) {
        companion object {
            @JvmSynthetic
            internal fun create(activity: Activity, title: String, bed: BedFragment?): Progress =
                Progress(activity, title, bed)
        }

        private val activity = activity
        private val handler = Handler(Looper.getMainLooper())
        private val startedAt = System.currentTimeMillis()
        private val platformDialog: androidx.appcompat.app.AlertDialog?
        private var closeWorkspaceDialog: (() -> Unit)? = null

        @Volatile private var outputFile: File? = null
        @Volatile private var stageProgress = 3
        @Volatile private var stage: String = activity.getString(R.string.ConverterProgressPreparing)
        @Volatile private var finished = false
        private var displayState by mutableStateOf(ConverterProgressDisplay(stage = stage))

        private val refresh = object : Runnable {
            override fun run() {
                if (finished) return
                displayState = ConverterProgressModel.estimate(
                    stageProgress = stageProgress,
                    fileProgress = getFileProgress(),
                    elapsedMillis = System.currentTimeMillis() - startedAt,
                ).let { estimate ->
                    ConverterProgressDisplay(
                        percent = estimate.percent,
                        stage = stage,
                        elapsedMillis = estimate.elapsedMillis,
                        remainingMillis = estimate.remainingMillis,
                    )
                }
                handler.postDelayed(this, REFRESH_INTERVAL_MS)
            }
        }

        init {
            if (bed?.canShowWorkspaceComposeDialog() == true) {
                closeWorkspaceDialog = bed.showWorkspaceComposeDialog { _ ->
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {},
                        properties = androidx.compose.ui.window.DialogProperties(
                            dismissOnBackPress = false,
                            dismissOnClickOutside = false,
                        ),
                        title = { Text(title) },
                        text = { ConverterProgressContent(displayState) },
                        confirmButton = {},
                    )
                }
                platformDialog = null
            } else {
                val composeView = ComposeView(activity).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                    setContent {
                        BeamComposeTheme {
                            ConverterProgressContent(displayState)
                        }
                    }
                }
                val builder = BeamAlertDialogBuilder(activity)
                builder.setTitle(title)
                builder.setView(composeView)
                builder.setCancelable(false)
                platformDialog = builder.create()
                if (!activity.isFinishing && !activity.isDestroyed) platformDialog.show()
            }
            handler.post(refresh)
        }

        fun setOutputFile(file: File) {
            outputFile = file
        }

        fun setStage(percent: Int, label: String) {
            stageProgress = ConverterProgressModel.advanceStage(stageProgress, percent)
            stage = label
        }

        /** Completes the visible work phase and then opens the generated-file action. */
        fun complete(next: Runnable?) {
            if (finished) return
            finished = true
            handler.post {
                handler.removeCallbacks(refresh)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    val elapsed = ConverterProgressModel.formatTime(System.currentTimeMillis() - startedAt)
                    displayState = ConverterProgressDisplay(
                        percent = 100,
                        stage = activity.getString(R.string.ConverterProgressComplete, elapsed),
                        elapsedMillis = System.currentTimeMillis() - startedAt,
                    )
                    dismissVisibleProgress()
                    next?.let(handler::post)
                }
            }
        }

        fun fail() {
            if (finished) return
            finished = true
            handler.post {
                handler.removeCallbacks(refresh)
                dismissVisibleProgress()
            }
        }

        private fun dismissVisibleProgress() {
            closeWorkspaceDialog?.invoke()
            closeWorkspaceDialog = null
            platformDialog?.takeIf { it.isShowing }?.dismiss()
        }

        private fun getFileProgress(): Int {
            val file = outputFile ?: return 0
            if (!file.isFile || file.length() < STL_HEADER_SIZE || !file.name.lowercase(Locale.ROOT).endsWith(".stl")) return 0
            return try {
                RandomAccessFile(file, "r").use { input ->
                    input.seek(STL_FACET_COUNT_OFFSET)
                    val facets = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
                    ConverterProgressModel.stlFileProgress(file.length(), facets)
                }
            } catch (_: Exception) {
                0
            }
        }
    }

    private const val REFRESH_INTERVAL_MS = 500L
    private const val STL_HEADER_SIZE = 84L
    private const val STL_FACET_COUNT_OFFSET = 80L
}

private data class ConverterProgressDisplay(
    val percent: Int = 3,
    val stage: String,
    val elapsedMillis: Long = 0,
    val remainingMillis: Long? = null,
)

@Composable
private fun ConverterProgressContent(state: ConverterProgressDisplay) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.stage,
            modifier = Modifier.testTag("converter-progress-stage"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        LinearProgressIndicator(
            progress = { state.percent / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.ConverterProgressPercent, state.percent),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(
                    R.string.ConverterProgressElapsed,
                    ConverterProgressModel.formatTime(state.elapsedMillis),
                ),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
            )
        }
        state.remainingMillis?.let { remaining ->
            Text(
                text = stringResource(
                    R.string.ConverterProgressRemaining,
                    ConverterProgressModel.formatTime(remaining),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
    }
}
