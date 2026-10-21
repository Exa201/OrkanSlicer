package ru.ytkab0bp.slicebeam.components.bed_menu

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.MainActivity
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Exercises the read-only G-code result workspaces without starting a slice or printer operation. */
@RunWith(AndroidJUnit4::class)
class SliceResultWorkspaceDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun resultAndLayerRoutesUseTheExclusiveWorkspaceHost() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment

        activity.runOnUiThread { showSliceInfoWorkspace(bed, SliceMenu()) }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("slice-info-title").assertIsDisplayed()
        compose.onNodeWithTag("slice-info-scheme").assertIsDisplayed()
        compose.onNodeWithTag("slice-info-seams").assertIsDisplayed()
        compose.onNodeWithTag("slice-info-close").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }

        activity.runOnUiThread { showSliceLayersWorkspace(bed) }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceToolPanelAttached("slice-layer-preview") }
        compose.onNodeWithTag("slice-layers-panel").assertIsDisplayed()
        compose.onNodeWithTag("workspace-docked-tool-panel").assertIsDisplayed()
        compose.onNodeWithTag("slice-layers-title").assertIsDisplayed()
        compose.onNodeWithTag("slice-layers-range-title").assertIsDisplayed()
        val rendererBounds = compose.onNodeWithTag("workspace-renderer-area").fetchSemanticsNode().boundsInRoot
        val controlsBounds = compose.onNodeWithTag("workspace-docked-tool-panel").fetchSemanticsNode().boundsInRoot
        assertTrue("Layer controls must be outside the renderer instead of covering the build plate",
            rendererBounds.bottom <= controlsBounds.top + 1f)
        activity.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceToolPanelAttached("slice-layer-preview") }

        activity.runOnUiThread { showSliceLayersWorkspace(bed) }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceToolPanelAttached("slice-layer-preview") }
        compose.onNodeWithTag("slice-layers-close").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceToolPanelAttached("slice-layer-preview") }

        assertFalse("Closing result tools must not finish the app", activity.isFinishing)
    }

    @Test
    fun printerUploadProgressUsesBlockingWorkspaceDialogAndTracksStatus() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        lateinit var progress: ru.ytkab0bp.slicebeam.components.PrinterUploadProgressDialog

        activity.runOnUiThread {
            progress = requireNotNull(bed.showPrinterUploadProgress("Preparing a validated upload"))
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isPrinterUploadProgressAttached() }
        compose.onNodeWithTag("printer-upload-progress-title").assertIsDisplayed()
        compose.onNodeWithTag("printer-upload-progress-indicator").assertIsDisplayed()
        compose.onNodeWithText("Preparing a validated upload").assertIsDisplayed()

        progress.setProgress(37, "Uploading 37%")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Uploading 37%").fetchSemanticsNodes().isNotEmpty()
        }
        activity.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertTrue("Upload progress must stay visible until the network operation resolves", bed.isPrinterUploadProgressAttached())
        assertFalse("Back during upload must not exit the workspace", activity.isFinishing)

        activity.runOnUiThread { bed.dismissPrinterUploadProgress(progress) }
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isPrinterUploadProgressAttached() }
    }

    private fun createInitializedActivityRule() = run {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        Prefs.getPrefs()?.edit()?.remove("crash")?.putBoolean("converter_storage_first_prompt", true)?.commit()
        Prefs.setLastCommit()
        SliceBeam.clearLiveDiffs()

        val config = context.assets.open("orca_profiles/Anycubic.ini").use(::Slic3rConfigWrapper)
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", process.title)
            put("filament", filament.title)
        }
        SliceBeam.CONFIG = config
        createAndroidComposeRule<MainActivity>()
    }
}
