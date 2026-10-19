package ru.ytkab0bp.slicebeam

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.fragment.FilamentConfigFragment
import ru.ytkab0bp.slicebeam.fragment.MakerFragment
import ru.ytkab0bp.slicebeam.fragment.PrintConfigFragment
import ru.ytkab0bp.slicebeam.fragment.PrinterConfigFragment
import ru.ytkab0bp.slicebeam.fragment.SettingsFragment
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Exercises the five global Compose navigation destinations as a user taps them. */
@RunWith(AndroidJUnit4::class)
class MobileNavigationDeviceTest {
    private var previousConfig: Slic3rConfigWrapper? = null
    private var previousPreferences: Map<String, Any?> = emptyMap()

    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun globalDestinationsOpenByAccessibleActionAndAppBackReturnsToLastPrimaryRoute() {
        val activity = compose.activity
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val navigation = activity.navigationDelegate as MobileNavigationDelegate
        assertTrue("Preview should be the initial global destination",
            activity.navigationDelegate.getCurrentFragment() is BedFragment)

        openDestination(context.getString(R.string.SlotPrintConfig), PrintConfigFragment::class.java)
        openDestination(context.getString(R.string.NavigationFilament), FilamentConfigFragment::class.java)
        openDestination(context.getString(R.string.SlotPrinterConfig), PrinterConfigFragment::class.java)
        openDestination(context.getString(R.string.NavigationApp), SettingsFragment::class.java)

        compose.onNodeWithText(context.getString(R.string.SettingsAppMakerTitle)).assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            activity.navigationDelegate.getCurrentFragment() is MakerFragment
        }
        compose.onNodeWithText(context.getString(R.string.MakerTitle)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.MakerBack)).assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            activity.navigationDelegate.getCurrentFragment() is SettingsFragment
        }
        compose.onNodeWithText(context.getString(R.string.SettingsAppMakerTitle)).assertIsDisplayed()

        compose.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(timeoutMillis = 10_000) {
            activity.navigationDelegate.getCurrentFragment() is PrinterConfigFragment
        }
        compose.onNodeWithContentDescription(context.getString(R.string.SlotPrinterConfig))
            .assertIsDisplayed()

        openDestination(context.getString(R.string.NavigationPreview), BedFragment::class.java)
        compose.runOnIdle {
            assertSame("The navigation delegate remains the active root", navigation, activity.navigationDelegate)
        }
    }

    private fun <T : ru.ytkab0bp.slicebeam.navigation.Fragment> openDestination(
        accessibleLabel: String,
        destination: Class<T>,
    ) {
        compose.onNodeWithContentDescription(accessibleLabel).assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            destination.isInstance(compose.activity.navigationDelegate.getCurrentFragment())
        }
        compose.onNodeWithContentDescription(accessibleLabel).assertIsDisplayed()
    }

    private fun createInitializedActivityRule() = run {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        val preferences = requireNotNull(Prefs.getPrefs())
        previousConfig = SliceBeam.CONFIG
        previousPreferences = preferences.all.filterKeys {
            it == "crash" || it == "converter_storage_first_prompt" || it == "last_commit"
        }
        preferences.edit()
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

    @After
    fun restoreApplicationState() {
        SliceBeam.CONFIG = previousConfig
        val preferences = Prefs.getPrefs() ?: return
        val editor = preferences.edit()
            .remove("crash")
            .remove("converter_storage_first_prompt")
            .remove("last_commit")
        previousPreferences.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.commit()
    }
}
