package ru.ytkab0bp.slicebeam

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.SettingsFragment
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Exercises an app-settings action through MainActivity's real workspace destination. */
@RunWith(AndroidJUnit4::class)
class SettingsMenuActionDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun renderScaleSelectionPersistsAndAndroidBackLeavesItUnchanged() {
        val activity = compose.activity
        val originalScale = Prefs.getRenderScale()
        val targetScale = floatArrayOf(1f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f).first { it != originalScale }
        val targetLabel = "${(targetScale * 100).toInt()}%"

        try {
            activity.runOnUiThread {
                (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(5)
            }
            compose.waitUntil(timeoutMillis = 15_000) {
                activity.navigationDelegate.getCurrentFragment() is SettingsFragment
            }

            compose.onNodeWithText(activity.getString(R.string.SettingsInterface))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.SettingsInterfaceResolutionScale))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(targetLabel).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { Prefs.getRenderScale() == targetScale }
            assertEquals("Selecting a scale applies the selected app preference", targetScale, Prefs.getRenderScale(), 0f)

            compose.onNodeWithText(activity.getString(R.string.SettingsInterfaceResolutionScale))
                .assertIsDisplayed().performClick()
            assertEquals("The selected value appears in both the current setting and the open choice list", 2,
                compose.onAllNodesWithText(targetLabel).fetchSemanticsNodes().size)
            InstrumentationRegistry.getInstrumentation()
                .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            assertEquals("Android Back dismisses without changing the applied setting", targetScale, Prefs.getRenderScale(), 0f)
        } finally {
            Prefs.setRenderScale(originalScale)
        }
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
        val process = requireNotNull(config.findPrint("0.16mm High Quality"))
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
