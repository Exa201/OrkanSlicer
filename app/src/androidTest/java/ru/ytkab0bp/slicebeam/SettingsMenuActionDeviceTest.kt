package ru.ytkab0bp.slicebeam

import android.app.Activity
import android.app.Application
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.SettingsFragment
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.navigation.NavigationDelegate
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.components.ColorChooserPalette
import java.util.Locale

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
            compose.onNodeWithText(targetLabel, substring = true).assertIsDisplayed().performClick()
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

    @Test
    fun themeSelectionAppliesAndBackDoesNotChangeTheCommittedTheme() {
        val activity = compose.activity
        val originalTheme = Prefs.getThemeMode()
        val targetTheme = Prefs.ThemeMode.values().first { it != originalTheme }
        val targetLabel = activity.getString(targetTheme.title)

        try {
            activity.runOnUiThread {
                (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(5)
            }
            compose.waitUntil(timeoutMillis = 15_000) {
                activity.navigationDelegate.getCurrentFragment() is SettingsFragment
            }

            compose.onNodeWithText(activity.getString(R.string.SettingsInterface))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.SettingsInterfaceTheme))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(targetLabel).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { Prefs.getThemeMode() == targetTheme }
            assertEquals(targetTheme, Prefs.getThemeMode())

            compose.onNodeWithText(activity.getString(R.string.SettingsInterfaceTheme))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation()
                .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            assertEquals("Android Back dismisses the theme picker without changing its committed value",
                targetTheme, Prefs.getThemeMode())
        } finally {
            activity.runOnUiThread {
                Prefs.setThemeMode(originalTheme.ordinal)
                ThemesRepo.invalidate(activity)
            }
        }
    }

    @Test
    fun textScaleSelectionUpdatesTheLiveSettingsLayoutAndCanBeRestored() {
        val activity = compose.activity
        val originalScale = Prefs.getUiTextScale()
        val targetScale = if (originalScale < 1.5f) 1.5f else 1.4f
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

            val workspaceRevisionBefore = workspaceThemeRevision(activity)
            val heading = compose.onNodeWithText(activity.getString(R.string.SlotAppSettings))
            heading.assertIsDisplayed()
            val beforeHeight = heading.fetchSemanticsNode().size.height

            compose.onNodeWithText(activity.getString(R.string.SettingsInterfaceTextSize))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(targetLabel, substring = true).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { Prefs.getUiTextScale() == targetScale }
            compose.waitForIdle()

            val afterHeight = compose.onNodeWithText(activity.getString(R.string.SlotAppSettings))
                .fetchSemanticsNode().size.height
            assertEquals(targetScale, Prefs.getUiTextScale(), 0f)
            assertTrue("The already-created workspace must receive the global theme/text-scale invalidation",
                workspaceThemeRevision(activity) > workspaceRevisionBefore)
            if (targetScale > originalScale) {
                assertTrue("The live Compose heading must grow with the selected text scale ($beforeHeight → $afterHeight)",
                    afterHeight > beforeHeight)
            } else {
                assertTrue("The live Compose heading must shrink with the selected text scale ($beforeHeight → $afterHeight)",
                    afterHeight < beforeHeight)
            }
            compose.onNodeWithText(targetLabel).performScrollTo().assertIsDisplayed()
        } finally {
            Prefs.setUiTextScale(originalScale)
        }
    }

    @Test
    fun aboutActionOpensComposeAboutRouteAndAndroidBackReturnsToSettings() {
        val mainActivity = compose.activity
        mainActivity.runOnUiThread {
            (mainActivity.navigationDelegate as MobileNavigationDelegate).switchToSlot(5)
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            mainActivity.navigationDelegate.getCurrentFragment() is SettingsFragment
        }

        compose.onNodeWithText(mainActivity.getString(R.string.SettingsAppInformation))
            .performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText(mainActivity.getString(R.string.SettingsAbout))
            .performScrollTo().assertIsDisplayed().performClick()

        compose.waitUntil(timeoutMillis = 10_000) { resumedActivity() is SetupActivity }
        val aboutActivity = resumedActivity() as SetupActivity
        assertEquals(true, aboutActivity.intent.getBooleanExtra(SetupActivity.EXTRA_ABOUT, false))
        assertTrue("About route should attach its Compose screen", containsComposeView(aboutActivity.window.decorView))

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitUntil(timeoutMillis = 10_000) {
            resumedActivity() === mainActivity && mainActivity.navigationDelegate.getCurrentFragment() is SettingsFragment
        }
        compose.onNodeWithText(mainActivity.getString(R.string.SlotAppSettings)).assertIsDisplayed()
    }

    @Test
    fun addPrinterActionOpensPresetSetupAndBackPreservesTheActivePrinter() {
        val mainActivity = compose.activity
        val config = requireNotNull(SliceBeam.CONFIG)
        val activePrinter = config.presets?.get("printer")
        mainActivity.runOnUiThread {
            (mainActivity.navigationDelegate as MobileNavigationDelegate).switchToSlot(5)
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            mainActivity.navigationDelegate.getCurrentFragment() is SettingsFragment
        }

        compose.onNodeWithText(mainActivity.getString(R.string.SettingsDevices))
            .performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText(mainActivity.getString(R.string.SettingsAddPrinter))
            .performScrollTo().assertIsDisplayed().performClick()

        compose.waitUntil(timeoutMillis = 10_000) { resumedActivity() is SetupActivity }
        val setupActivity = resumedActivity() as SetupActivity
        assertTrue("Add printer must enter the preset setup flow", setupActivity.intent.getBooleanExtra(
            SetupActivity.EXTRA_ADD_PRINTER, false,
        ))
        assertTrue("Printer preset selection should be hosted by Compose", containsComposeView(setupActivity.window.decorView))

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitUntil(timeoutMillis = 10_000) {
            resumedActivity() === mainActivity && mainActivity.navigationDelegate.getCurrentFragment() is SettingsFragment
        }
        compose.onNodeWithText(mainActivity.getString(R.string.SlotAppSettings)).assertIsDisplayed()
        assertEquals("Opening and backing out of preset selection must not switch the active printer",
            activePrinter, config.presets?.get("printer"))
    }

    @Test
    fun accentColorPaletteBackCancelsAndSelectingASwatchAppliesTheColor() {
        val activity = compose.activity
        val originalColor = Prefs.getAccentColor()
        val selectedSwatch = ColorChooserPalette.commonColors().first { (it or -0x1000000) != originalColor }
        val expectedColor = selectedSwatch or -0x1000000
        val colorLabel = activity.getString(
            R.string.ColorChooserSwatchDescription,
            String.format(Locale.ROOT, "#%06X", selectedSwatch and 0x00FFFFFF),
        )

        try {
            activity.runOnUiThread {
                (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(5)
            }
            compose.waitUntil(timeoutMillis = 15_000) {
                activity.navigationDelegate.getCurrentFragment() is SettingsFragment
            }
            compose.onNodeWithText(activity.getString(R.string.SettingsInterface))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.SettingsInterfaceColor))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.ColorChooserCommon)).assertIsDisplayed()

            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            assertEquals("Back closes the color palette without changing the accent", originalColor, Prefs.getAccentColor())

            compose.onNodeWithText(activity.getString(R.string.SettingsInterfaceColor))
                .assertIsDisplayed().performClick()
            compose.onAllNodesWithContentDescription(colorLabel).get(0)
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { Prefs.getAccentColor() == expectedColor }
            assertEquals("Selecting an accessible swatch applies its opaque color", expectedColor, Prefs.getAccentColor())
        } finally {
            Prefs.setAccentColor(originalColor)
            ThemesRepo.invalidate(activity)
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

    private fun workspaceThemeRevision(activity: MainActivity): Int {
        val stacksField = NavigationDelegate::class.java.getDeclaredField("fragmentStack").apply {
            isAccessible = true
        }
        val stacks = stacksField.get(activity.navigationDelegate) as SparseArray<*>
        val bedStack = stacks.get(0) as java.util.Stack<*>
        val bedFragment = bedStack.peek() as BedFragment
        val revisionField = BedFragment::class.java.getDeclaredField("workspaceThemeRevision\$delegate").apply {
            isAccessible = true
        }
        return (revisionField.get(bedFragment) as MutableIntState).intValue
    }

    private fun resumedActivity(): Activity? {
        val resumed = AtomicReference<Activity?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            resumed.set(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull())
        }
        return resumed.get()
    }

    private fun containsComposeView(view: View): Boolean = when (view) {
        is AbstractComposeView -> view.isAttachedToWindow
        is ViewGroup -> (0 until view.childCount).any { index -> containsComposeView(view.getChildAt(index)) }
        else -> false
    }
}
