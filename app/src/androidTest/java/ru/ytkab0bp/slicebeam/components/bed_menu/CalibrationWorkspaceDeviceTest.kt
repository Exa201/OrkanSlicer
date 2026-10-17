package ru.ytkab0bp.slicebeam.components.bed_menu

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.MainActivity
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Exercises calibration navigation without importing models or starting printer work. */
@RunWith(AndroidJUnit4::class)
class CalibrationWorkspaceDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun calibrationActionsFormsAndBundledModelListStayInWorkspace() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        activity.runOnUiThread { showCalibrationWorkspace(bed) }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }

        compose.onNodeWithTag("calibration-dialog-title").assertIsDisplayed()
        compose.onNodeWithTag("calibration-preset-${CalibrationPresets.PA_LINE}").performClick()
        compose.onNodeWithTag("calibration-prepare").assertIsDisplayed()
        compose.onNodeWithTag("calibration-dialog-back").performClick()

        compose.onNodeWithTag("calibration-actions-list").performScrollToNode(hasTestTag("calibration-record-result"))
        compose.onNodeWithTag("calibration-record-result").performClick()
        compose.onNodeWithTag("calibration-save-result").assertIsDisplayed()
        compose.onNodeWithTag("calibration-dialog-back").performClick()

        compose.onNodeWithTag("calibration-actions-list").performScrollToNode(hasTestTag("calibration-models-open"))
        compose.onNodeWithTag("calibration-models-open").performClick()
        compose.onNodeWithTag("calibration-model-Orca Cube").assertIsDisplayed()
        compose.onNodeWithTag("calibration-dialog-back").performClick()

        compose.onNodeWithTag("calibration-dialog-back").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertFalse("Closing calibration selection must not finish the app", activity.isFinishing)
        assertEquals("Opening/canceling the configuration must not set a calibration", 0, SliceBeam.PENDING_CALIB_MODE)
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
