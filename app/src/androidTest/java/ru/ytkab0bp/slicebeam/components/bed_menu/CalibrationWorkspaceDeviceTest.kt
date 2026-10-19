package ru.ytkab0bp.slicebeam.components.bed_menu

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.MainActivity
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
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
        val firstPreset = CalibrationPresets.supported().first()
        val fullActionDescription = activity.getString(firstPreset.titleRes) + ". " +
            activity.getString(firstPreset.descriptionRes)
        compose.onNodeWithContentDescription(fullActionDescription).assertIsDisplayed()
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

    @Test
    fun recordingCalibrationResultsStagesValuesForTheCorrectProfileWithoutSavingProfiles() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val config = SliceBeam.requireConfig()
        val filament = requireNotNull(config.findFilament(config.presets?.get("filament")))
        val process = requireNotNull(config.findPrint(config.presets?.get("print")))
        val profileTypes = listOf(ConfigObject.PROFILE_LIST_FILAMENT, ConfigObject.PROFILE_LIST_PRINT)
        val oldDrafts = profileTypes.associateWith(ProfileSettingsRepository::pendingEntries)
        val oldFilamentPressureAdvance = filament.get("pressure_advance")
        val oldProcessOuterWallSpeed = process.get("outer_wall_speed")

        try {
            compose.onNodeWithContentDescription(activity.getString(ru.ytkab0bp.slicebeam.R.string.MenuOptions))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithContentDescription(
                    activity.getString(ru.ytkab0bp.slicebeam.R.string.MenuFileCalibrations),
                ).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription(activity.getString(ru.ytkab0bp.slicebeam.R.string.MenuFileCalibrations))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
            compose.onNodeWithTag("calibration-actions-list")
                .performScrollToNode(hasTestTag("calibration-record-result"))
            compose.onNodeWithTag("calibration-record-result").performClick()
            compose.onNodeWithTag("calibration-result-value").performTextInput("0.042")
            compose.onNodeWithTag("calibration-save-result").performClick()

            compose.waitUntil(timeoutMillis = 5_000) {
                ProfileSettingsRepository.pendingEntries(ConfigObject.PROFILE_LIST_FILAMENT)["pressure_advance"] == "0.042"
            }
            assertEquals("A positive PA result should enable PA in the staged filament draft", "1",
                ProfileSettingsRepository.pendingEntries(ConfigObject.PROFILE_LIST_FILAMENT)["enable_pressure_advance"])
            assertEquals("Recording a result must stage, not directly mutate, the saved filament", oldFilamentPressureAdvance,
                filament.get("pressure_advance"))

            compose.onNodeWithTag("calibration-actions-list")
                .performScrollToNode(hasTestTag("calibration-record-result"))
            compose.onNodeWithTag("calibration-record-result").performClick()
            compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.CalibrationPressureAdvanceShort))
                .performClick()
            compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.CalibrationVfaTower))
                .performClick()
            compose.onNodeWithTag("calibration-result-value").performTextInput("130")
            compose.onNodeWithTag("calibration-save-result").performClick()

            compose.waitUntil(timeoutMillis = 5_000) {
                ProfileSettingsRepository.pendingEntries(ConfigObject.PROFILE_LIST_PRINT)["outer_wall_speed"] == "130.0"
            }
            assertEquals("The VFA result belongs to the print-process draft", "130.0",
                ProfileSettingsRepository.pendingEntries(ConfigObject.PROFILE_LIST_PRINT)["outer_wall_speed"])
            assertEquals("Recording VFA must not directly mutate the saved process", oldProcessOuterWallSpeed,
                process.get("outer_wall_speed"))
            assertTrue("The calibration workspace should remain open after staging a result",
                bed.isWorkspaceMessageDialogAttached())
        } finally {
            profileTypes.forEach { profileType ->
                ProfileSettingsRepository.clearDraft(profileType)
                oldDrafts[profileType].orEmpty().takeIf { it.isNotEmpty() }?.let {
                    ProfileSettingsRepository.setPendingValues(profileType, it)
                }
            }
            SliceBeam.PENDING_CALIB_MODE = 0
            SliceBeam.PENDING_CALIB_START = 0.0
            SliceBeam.PENDING_CALIB_END = 0.0
            SliceBeam.PENDING_CALIB_STEP = 0.0
            if (bed.isWorkspaceMessageDialogAttached()) activity.onBackPressedDispatcher.onBackPressed()
        }
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
