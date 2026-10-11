package ru.ytkab0bp.slicebeam.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONObject
import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.BeamServerData
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.BeamServerDataUpdatedEvent
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.components.ChangeLogContent
import ru.ytkab0bp.slicebeam.ui.components.ChangeLogContentResolver
import java.util.Locale

/** Release notes and optional supporter page, rendered with the shared Compose theme. */
class ChangeLogBottomSheet(context: Context) : BottomSheetDialog(context) {
    private val sheetContext = context
    private var hasSupporterPage by mutableStateOf(BeamServerData.isBoostyAvailable())
    private var supporters by mutableStateOf(currentSupporters())

    init {
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                BeamComposeTheme {
                    ChangeLogContent(
                        releaseNotes = loadReleaseNotes(context),
                        hasSupporterPage = hasSupporterPage,
                        supporters = supporters,
                        onOpenSupport = ::openSupportPage,
                        onDismiss = ::dismiss,
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
        SliceBeam.EVENT_BUS.registerListener(this)
        setOnDismissListener { SliceBeam.EVENT_BUS.unregisterListener(this) }
    }

    @EventHandler(runOnMainThread = true)
    fun onDataUpdated(event: BeamServerDataUpdatedEvent) {
        val data = SliceBeam.SERVER_DATA ?: return
        supporters = data.boostySubscribers.shuffled()
        hasSupporterPage = BeamServerData.isBoostyAvailable()
    }

    override fun show() {
        super.show()
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
    }

    private fun openSupportPage() {
        try {
            sheetContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(sheetContext, R.string.ChangelogSupportLinkOpenFailed, Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(sheetContext, R.string.ChangelogSupportLinkOpenFailed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun currentSupporters(): List<String> =
        SliceBeam.SERVER_DATA?.boostySubscribers?.shuffled().orEmpty()

    private fun loadReleaseNotes(context: Context): String = try {
        val translations = context.assets.open("update.json").bufferedReader(Charsets.UTF_8).use { reader ->
            val json = JSONObject(reader.readText())
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, json.optString(key))
                }
            }
        }
        ChangeLogContentResolver.localizedText(Locale.getDefault().language, translations)
    } catch (error: Exception) {
        Log.e(TAG, "Failed to open update file", error)
        ""
    }

    private companion object {
        const val SUPPORT_URL = "https://boosty.to/ytkab0bp"
        const val TAG = "Changelog"
    }
}
