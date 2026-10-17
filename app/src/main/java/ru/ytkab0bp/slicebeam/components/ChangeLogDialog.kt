package ru.ytkab0bp.slicebeam.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

/** Release notes and optional supporter page, rendered in a Compose-owned dialog. */
class ChangeLogDialog(private val context: Context) {
    private var hasSupporterPage by mutableStateOf(BeamServerData.isBoostyAvailable())
    private var supporters by mutableStateOf(currentSupporters())
    private val releaseNotes by lazy { loadReleaseNotes(context) }

    @Composable
    fun Content(onDismiss: () -> Unit) {
        DisposableEffect(this) {
            SliceBeam.EVENT_BUS.registerListener(this@ChangeLogDialog)
            onDispose { SliceBeam.EVENT_BUS.unregisterListener(this@ChangeLogDialog) }
        }
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                BeamComposeTheme {
                    ChangeLogContent(
                        releaseNotes = releaseNotes,
                        hasSupporterPage = hasSupporterPage,
                        supporters = supporters,
                        onOpenSupport = ::openSupportPage,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }

    @EventHandler(runOnMainThread = true)
    fun onDataUpdated(event: BeamServerDataUpdatedEvent) {
        val data = SliceBeam.SERVER_DATA ?: return
        supporters = data.boostySubscribers.shuffled()
        hasSupporterPage = BeamServerData.isBoostyAvailable()
    }

    private fun openSupportPage() {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.ChangelogSupportLinkOpenFailed, Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, R.string.ChangelogSupportLinkOpenFailed, Toast.LENGTH_SHORT).show()
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
