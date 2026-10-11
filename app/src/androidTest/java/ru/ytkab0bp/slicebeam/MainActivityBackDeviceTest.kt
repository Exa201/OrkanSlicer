package ru.ytkab0bp.slicebeam

import android.app.Application
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Keeps the app-exit confirmation on the AndroidX back-dispatch path used by Android 16. */
@RunWith(AndroidJUnit4::class)
class MainActivityBackDeviceTest {
    @Test
    fun dispatcherBackShowsOneExitConfirmationWithoutFinishingActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        Prefs.getPrefs()?.edit()
            ?.remove("crash")
            ?.putBoolean("converter_storage_first_prompt", true)
            ?.commit()
        Prefs.setLastCommit()
        SliceBeam.clearLiveDiffs()

        val config = context.assets.open("orca_profiles/Anycubic.ini").use { Slic3rConfigWrapper(it) }
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", process.title)
            put("filament", filament.title)
        }
        SliceBeam.CONFIG = config

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity { activity ->
                (activity.navigationDelegate as? MobileNavigationDelegate)?.switchToSlot(0)
                assertTrue("The model workspace should be the initial page", activity.navigationDelegate.getCurrentFragment() is BedFragment)
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
                activity.onBackPressedDispatcher.onBackPressed()
            }
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                assertFalse("Back from the root workspace must ask before closing", activity.isFinishing)
                val field = MainActivity::class.java.getDeclaredField("exitConfirmationDialog").apply { isAccessible = true }
                val dialog = field.get(activity) as? AlertDialog
                assertNotNull("Back should create an exit confirmation dialog", dialog)
                requireNotNull(dialog)
                assertTrue("The exit confirmation should be shown", dialog.isShowing)
                val titleId = activity.resources.getIdentifier("alertTitle", "id", activity.packageName)
                assertTrue("The Material dialog title should exist", titleId > 0)
                assertEquals(
                    activity.getString(R.string.AppCloseConfirmTitle),
                    dialog.findViewById<TextView>(titleId)?.text?.toString(),
                )
                assertEquals(
                    activity.getString(R.string.AppCloseConfirmMessage),
                    dialog.findViewById<TextView>(android.R.id.message)?.text?.toString(),
                )

                activity.onBackPressedDispatcher.onBackPressed()
                val repeatedDialog = field.get(activity) as? AlertDialog
                assertSame("Repeated back input must not stack a second exit dialog", dialog, repeatedDialog)
                assertTrue("The original exit confirmation should remain visible", dialog.isShowing)
            }
        } finally {
            scenario.close()
        }
    }
}
