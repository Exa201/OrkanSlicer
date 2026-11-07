package ru.ytkab0bp.slicebeam

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Exercises the root-workspace exit confirmation on Android's back-dispatch path. */
@RunWith(AndroidJUnit4::class)
class MainActivityBackDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun dispatcherBackShowsWorkspaceConfirmationAndCancelKeepsActivityOpen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as? BedFragment
        assertTrue("The model workspace should be the initial page", bed != null)
        (activity.navigationDelegate as? MobileNavigationDelegate)?.switchToSlot(0)

        val resultLaunchersField = MainActivity::class.java
            .getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        val registeredRequestCodes = (resultLaunchersField.get(activity) as Map<*, *>).keys
        assertEquals(
            setOf(
                MainActivity.REQUEST_CODE_OPEN_FILE,
                MainActivity.REQUEST_CODE_EXPORT_GCODE,
                MainActivity.REQUEST_CODE_IMPORT_PROFILES,
                MainActivity.REQUEST_CODE_EXPORT_PROFILES,
                MainActivity.REQUEST_CODE_EXPORT_3MF,
                MainActivity.REQUEST_CODE_AI_GENERATOR_TAKE_PHOTO,
                MainActivity.REQUEST_CODE_AI_GENERATOR_CHOOSE_PHOTO,
                MainActivity.REQUEST_CODE_IMAGE_TO_STL,
                MainActivity.REQUEST_CODE_SAVE_CONVERTED_FILE,
                MainActivity.REQUEST_CODE_CONVERTER_ROOT_FIRST_LAUNCH,
                MainActivity.REQUEST_CODE_CONVERTER_ROOT_SETTINGS,
                MainActivity.REQUEST_CODE_CONVERTER_ROOT_SAVE,
                ru.ytkab0bp.slicebeam.components.KromacutEditorMenu.REQUEST_CODE_FILE,
            ),
            registeredRequestCodes,
        )

        instrumentation.runOnMainSync { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(timeoutMillis = 5_000) { requireNotNull(bed).isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText(activity.getString(R.string.AppCloseConfirmTitle)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(R.string.AppCloseConfirmMessage)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(android.R.string.cancel)).assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !requireNotNull(bed).isWorkspaceMessageDialogAttached() }
        assertFalse("Cancelling the exit prompt must keep the activity open", activity.isFinishing)

        instrumentation.runOnMainSync { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(timeoutMillis = 5_000) { requireNotNull(bed).isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText(activity.getString(android.R.string.cancel)).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !requireNotNull(bed).isWorkspaceMessageDialogAttached() }
        assertFalse("A second cancel must also leave the activity open", activity.isFinishing)
    }

    private fun createInitializedActivityRule() = run {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        Prefs.getPrefs()?.edit()
            ?.remove("crash")
            ?.putBoolean("converter_storage_first_prompt", true)
            ?.commit()
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
