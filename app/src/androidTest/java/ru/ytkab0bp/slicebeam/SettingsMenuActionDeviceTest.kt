package ru.ytkab0bp.slicebeam

import android.app.Activity
import android.app.Application
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ThreeMfProjectSettings
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.fragment.SettingsFragment
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.fragment.MakerFragment
import ru.ytkab0bp.slicebeam.components.KromacutEditorMenu
import ru.ytkab0bp.slicebeam.navigation.NavigationDelegate
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.render.DevicePerformancePolicy
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.components.ColorChooserPalette
import java.util.Locale

/** Exercises an app-settings action through MainActivity's real workspace destination. */
@RunWith(AndroidJUnit4::class)
class SettingsMenuActionDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun renderResolutionUsesDevicePolicyAndHasNoManualPicker() {
        val activity = compose.activity
        val prefs = requireNotNull(Prefs.getPrefs())
        try {
            prefs.edit().putFloat("render_scale", 0.5f).commit()
            Prefs.init(activity.application as Application)
            navigateToSettings(activity)

            compose.onNodeWithText(activity.getString(R.string.SettingsInterface))
                .performScrollTo().assertIsDisplayed().performClick()
            assertTrue("Manual render-resolution row was removed from the App menu",
                compose.onAllNodesWithText(activity.getString(R.string.SettingsInterfaceResolutionScale))
                    .fetchSemanticsNodes().isEmpty())
            assertEquals("The legacy override is cleared at preference initialization", false,
                prefs.contains("render_scale"))
            assertEquals(
                "Render scale is calculated by the current device performance policy",
                DevicePerformancePolicy.defaultRenderScale(Prefs.isLowRamDevice()),
                Prefs.getRenderScale(),
                0f,
            )
        } finally {
            prefs.edit().remove("render_scale").commit()
            Prefs.init(activity.application as Application)
        }
    }

    @Test
    fun themeSelectionAppliesAndBackDoesNotChangeTheCommittedTheme() {
        val activity = compose.activity
        val originalTheme = Prefs.getThemeMode()
        val targetTheme = Prefs.ThemeMode.values().first { it != originalTheme }
        val targetLabel = activity.getString(targetTheme.title)

        try {
            navigateToSettings(activity)

            compose.onNodeWithText(activity.getString(R.string.SettingsInterface))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.SettingsInterfaceTheme))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(targetLabel).performScrollTo().assertIsDisplayed().performClick()
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
            TemporaryProjectSettings.clear()
            TemporaryProjectSettings.activate(
                ThreeMfProjectSettings(
                    sourceName = "responsive-layout-fixture.3mf",
                    process = mapOf("layer_height" to "0.2"),
                    filaments = emptyList(),
                    filamentTypes = emptyMap(),
                    printSettingsImported = true,
                    filamentSettingsImported = false,
                    printerSettingsImported = false,
                ),
            )
            navigateToSettings(activity)
            showPreviewWorkspace(activity)
            val viewportBeforeZoom = workspaceViewportBounds(activity)
            val regionsBeforeZoom = workspaceRegionBounds()
            navigateToSettings(activity)
            compose.onNodeWithText(activity.getString(R.string.SettingsInterface))
                .performScrollTo().assertIsDisplayed().performClick()

            val workspaceRevisionBefore = workspaceThemeRevision(activity)
            val heading = compose.onNodeWithText(activity.getString(R.string.SlotAppSettings))
            heading.assertIsDisplayed()
            val beforeHeight = heading.fetchSemanticsNode().size.height

            compose.onNodeWithText(activity.getString(R.string.SettingsInterfaceTextSize))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("settings-ui-text-scale-slider")
                .assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                    assertTrue("Slider accepts an accessible target value", setProgress(targetScale))
                }
            compose.onNodeWithTag("settings-ui-text-scale-value").assertTextEquals(targetLabel)
            assertEquals("Previewing a value does not persist it before confirmation", originalScale, Prefs.getUiTextScale(), 0f)
            compose.onNodeWithTag("settings-ui-text-scale-preview-card").assertIsDisplayed()
            compose.onNodeWithTag("settings-ui-text-scale-reset").performClick()
            compose.onNodeWithTag("settings-ui-text-scale-value").assertTextEquals("100%")
            compose.onNodeWithTag("settings-ui-text-scale-slider")
                .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                    assertTrue("Slider accepts the selected value after reset", setProgress(targetScale))
                }
            compose.onNodeWithTag("settings-ui-text-scale-apply").performClick()
            compose.waitUntil(timeoutMillis = 5_000) { Prefs.getUiTextScale() == targetScale }
            compose.waitForIdle()

            val afterHeight = compose.onNodeWithText(activity.getString(R.string.SlotAppSettings))
                .fetchSemanticsNode().size.height
            assertEquals(targetScale, Prefs.getUiTextScale(), 0f)
            showPreviewWorkspace(activity)
            val regionsAfterZoom = workspaceRegionBounds()
            assertEquals("Workspace rows must keep the same geometry at different app text zoom values",
                regionsBeforeZoom, regionsAfterZoom)
            assertEquals(
                "Changing app text zoom must not move or resize the build plate viewport. Regions before=$regionsBeforeZoom, after=$regionsAfterZoom",
                viewportBeforeZoom,
                workspaceViewportBounds(activity),
            )
            navigateToSettings(activity)
            assertTrue("The already-created workspace must receive the global theme/text-scale invalidation",
                workspaceThemeRevision(activity) > workspaceRevisionBefore)
            if (targetScale > originalScale) {
                assertTrue("The live Compose heading must grow with the selected text scale ($beforeHeight → $afterHeight)",
                    afterHeight > beforeHeight)
            } else {
                assertTrue("The live Compose heading must shrink with the selected text scale ($beforeHeight → $afterHeight)",
                    afterHeight < beforeHeight)
            }

            navigateToSettings(activity)
            compose.onNodeWithText(activity.getString(R.string.SettingsInterfaceTextSize))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("settings-ui-text-scale-slider")
                .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                    assertTrue("Slider accepts a disposable preview value", setProgress(0.9f))
                }
            compose.onNodeWithTag("settings-ui-text-scale-cancel").performClick()
            compose.waitForIdle()
            assertEquals("Cancel discards the preview and preserves the applied value", targetScale, Prefs.getUiTextScale(), 0f)

            compose.onNodeWithText(activity.getString(R.string.SettingsInterfaceTextSize))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("settings-ui-text-scale-slider").assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            assertEquals("Android Back closes the preview without changing the applied text size", targetScale, Prefs.getUiTextScale(), 0f)
            showPreviewWorkspace(activity)
            assertEquals("App text zoom must not move or resize the build plate viewport after Back navigation",
                viewportBeforeZoom, workspaceViewportBounds(activity))
        } finally {
            activity.runOnUiThread {
                TemporaryProjectSettings.clear()
                Prefs.setUiTextScale(originalScale)
                ThemesRepo.invalidate(activity)
            }
        }
    }

    @Test
    fun aboutActionOpensComposeAboutRouteAndAndroidBackReturnsToSettings() {
        val mainActivity = compose.activity
        navigateToSettings(mainActivity)

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
        waitForSettingsContent(mainActivity)
        compose.onNodeWithText(mainActivity.getString(R.string.SlotAppSettings)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun addPrinterActionOpensPresetSetupAndBackPreservesTheActivePrinter() {
        val mainActivity = compose.activity
        val config = requireNotNull(SliceBeam.CONFIG)
        val activePrinter = config.presets?.get("printer")
        navigateToSettings(mainActivity)

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
        waitForSettingsContent(mainActivity)
        compose.onNodeWithText(mainActivity.getString(R.string.SlotAppSettings)).performScrollTo().assertIsDisplayed()
        assertEquals("Opening and backing out of preset selection must not switch the active printer",
            activePrinter, config.presets?.get("printer"))
    }

    @Test
    fun appResetActionCanBeCancelledOrDismissedWithBackWithoutChangingConfiguration() {
        val activity = compose.activity
        val config = requireNotNull(SliceBeam.CONFIG)
        val originalPrinter = config.presets?.get("printer")
        val originalProcess = config.presets?.get("print")
        val originalFilament = config.presets?.get("filament")
        navigateToSettings(activity)

        fun openResetConfirmation() {
            compose.onNodeWithText(activity.getString(R.string.SettingsResetToDefault))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.SettingsResetToDefaultTitle)).assertIsDisplayed()
        }
        fun assertConfigurationUnchanged() {
            assertTrue("Reset cancellation must retain the same active config", SliceBeam.CONFIG === config)
            assertEquals(originalPrinter, config.presets?.get("printer"))
            assertEquals(originalProcess, config.presets?.get("print"))
            assertEquals(originalFilament, config.presets?.get("filament"))
        }

        openResetConfirmation()
        compose.onNodeWithText(activity.getString(android.R.string.cancel)).performClick()
        compose.waitForIdle()
        assertConfigurationUnchanged()

        openResetConfirmation()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertConfigurationUnchanged()
        compose.onNodeWithText(activity.getString(R.string.SlotAppSettings)).performScrollTo().assertIsDisplayed()
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
            navigateToSettings(activity)
            compose.onNodeWithText(activity.getString(R.string.SettingsInterface))
                .performScrollTo().assertIsDisplayed().performClick()
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

    @Test
    fun makerSettingsRouteOpensKromacutAndBackRestoresWorkspaceRenderer() {
        val activity = compose.activity
        navigateToSettings(activity)
        compose.onNodeWithText(activity.getString(R.string.SettingsAppMakerTitle))
            .performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            activity.navigationDelegate.getCurrentFragment() is MakerFragment
        }
        compose.onNodeWithText(activity.getString(R.string.MakerKromacut))
            .performScrollTo().assertIsDisplayed().performClick()

        lateinit var bed: BedFragment
        var rendererBefore: Any? = null
        var modelBefore: Any? = null
        val expectedToolId = KromacutEditorMenu::class.java.name
        compose.waitUntil(timeoutMillis = 30_000) {
            val current = activity.navigationDelegate.getCurrentFragment()
            if (current !is BedFragment) return@waitUntil false
            bed = current
            val controller = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            val activeToolId = controller.javaClass.getMethod("getActiveToolId").invoke(controller) as? String
            if (activeToolId == expectedToolId) {
                rendererBefore = bed.glView?.renderer
                modelBefore = bed.glView?.renderer?.model
                bed.glView?.visibility == View.GONE
            } else false
        }

        activity.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(timeoutMillis = 30_000) {
            val controller = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            controller.javaClass.getMethod("getActiveToolId").invoke(controller) == null &&
                bed.glView?.visibility == View.VISIBLE
        }
        assertSame("KromaCut dismissal must retain the native GL renderer", rendererBefore, bed.glView?.renderer)
        assertEquals("Returning from KromaCut must retain the same native model",
            modelBefore, bed.glView?.renderer?.model)
    }

    private fun navigateToSettings(activity: MainActivity) {
        activity.runOnUiThread {
            (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(5)
        }
        waitForSettingsContent(activity)
    }

    private fun showPreviewWorkspace(activity: MainActivity) {
        activity.runOnUiThread {
            (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(0)
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            val bed = activity.navigationDelegate.getCurrentFragment() as? BedFragment ?: return@waitUntil false
            val view = bed.glView ?: return@waitUntil false
            val fragmentView = bed.getView() ?: return@waitUntil false
            // switchToSlot updates currentFragment before its spring transition has
            // finished. Measuring during that transition reports translated GLView
            // bounds and makes a stable-layout check flaky.
            val container = activity.navigationDelegate.getContainerView()
            view.visibility == View.VISIBLE && view.width > 0 && view.height > 0 &&
                container?.childCount == 1 &&
                fragmentView.translationX == 0f && fragmentView.translationY == 0f &&
                fragmentView.alpha >= 0.999f
        }
    }

    private fun waitForSettingsContent(activity: MainActivity) {
        val label = activity.getString(R.string.SettingsInterface)
        compose.waitUntil(timeoutMillis = 15_000) {
            activity.navigationDelegate.getCurrentFragment() is SettingsFragment &&
                compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
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
        SliceBeam.hasUpdateInfo = false
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

    private fun workspaceViewportBounds(activity: MainActivity): List<Int> {
        val result = AtomicReference<List<Int>>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val stacksField = NavigationDelegate::class.java.getDeclaredField("fragmentStack").apply {
                isAccessible = true
            }
            val stacks = stacksField.get(activity.navigationDelegate) as SparseArray<*>
            val bedStack = stacks.get(0) as java.util.Stack<*>
            val bedFragment = bedStack.peek() as BedFragment
            val viewport = requireNotNull(bedFragment.glView)
            val location = IntArray(2)
            viewport.getLocationInWindow(location)
            result.set(listOf(location[0], location[1], viewport.width, viewport.height))
        }
        return requireNotNull(result.get())
    }

    private fun workspaceRegionBounds(): Map<String, String> = listOf(
        "workspace-project-settings-area",
        "workspace-header",
        "workspace-renderer-area",
        "workspace-navigation",
    ).associateWith { tag ->
        compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.toString()
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
