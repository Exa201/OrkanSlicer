package ru.ytkab0bp.slicebeam

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.config.PrinterScopedProcessProfile
import ru.ytkab0bp.slicebeam.fragment.PrinterConfigFragment
import ru.ytkab0bp.slicebeam.fragment.ProfileConfigValueController
import ru.ytkab0bp.slicebeam.fragment.PrintConfigFragment
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.slic3r.Slic3rLocalization
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.PrinterProfileSelectionStore

/** Checks that the active printer-compatible process preset is marked in the real dropdown model. */
@RunWith(AndroidJUnit4::class)
class PrintProfileDropdownIntegrationDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun activeCompatibleProcessPresetIsSelectedInFilteredMenuSnapshot() {
        val activity = compose.activity
        val config = SliceBeam.requireConfig()
        assertEquals("The fixture starts from the bundled process preset", "0.16mm High Quality", config.presets?.get("print"))

        activity.runOnUiThread {
            (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(2)
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            activity.navigationDelegate.getCurrentFragment() is PrintConfigFragment
        }
        val activeTitle = requireNotNull(SliceBeam.requireConfig().presets?.get("print"))

        val fragment = activity.navigationDelegate.getCurrentFragment() as PrintConfigFragment
        val visibleItems = activity.runOnUiThreadAndWait {
            PrintConfigFragment::class.java
                .getDeclaredMethod("getItems", Boolean::class.javaPrimitiveType)
                .apply { isAccessible = true }
                .invoke(fragment, true) as List<*>
        }
        val visibleTitles = visibleItems.mapNotNull { item ->
            item?.javaClass?.getMethod("getTitle")?.invoke(item) as? String
        }
        val selectedItems = visibleItems.filter { item ->
            item?.javaClass?.getMethod("isSelected")?.invoke(item) as? Boolean == true
        }

        assertNotNull("The active preset must be present in the printer-compatible menu", visibleTitles.firstOrNull { it == activeTitle })
        assertEquals("Exactly one visible process preset should be selected", 1, selectedItems.size)
        assertEquals(
            "The selected menu row must match the active preset",
            activeTitle,
            selectedItems.single()?.javaClass?.getMethod("getTitle")?.invoke(selectedItems.single()),
        )
        assertEquals("Inspecting the filtered list must not change the active preset", activeTitle, SliceBeam.requireConfig().presets?.get("print"))
    }

    @Test
    fun selectingCompatibleProcessPresetThroughMenuChangesActiveSelection() {
        val activity = compose.activity
        val config = SliceBeam.requireConfig()
        activity.runOnUiThread {
            (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(2)
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            activity.navigationDelegate.getCurrentFragment() is PrintConfigFragment
        }

        val activeTitle = requireNotNull(config.presets?.get("print"))
        val fragment = activity.navigationDelegate.getCurrentFragment() as PrintConfigFragment
        val alternateTitle = activity.runOnUiThreadAndWait {
            val items = PrintConfigFragment::class.java
                .getDeclaredMethod("getItems", Boolean::class.javaPrimitiveType)
                .apply { isAccessible = true }
                .invoke(fragment, true) as List<*>
            items.asSequence()
                .mapNotNull { item -> item?.javaClass?.getMethod("getTitle")?.invoke(item) as? String }
                .firstOrNull { it != activeTitle }
        }
        assertNotNull("The bundled printer fixture needs another compatible process profile", alternateTitle)
        val target = requireNotNull(alternateTitle)
        val printer = requireNotNull(config.presets?.get("printer"))
        val expectedScopedTitle = PrinterScopedProcessProfile.title(target, printer)

        compose.onNodeWithText(activeTitle).assertIsDisplayed().performClick()
        compose.onNodeWithText(target).assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 10_000) { config.presets?.get("print") == expectedScopedTitle }

        assertEquals("Selecting a bundled menu row activates its printer-scoped copy", expectedScopedTitle, config.presets?.get("print"))
        assertNotNull("The source preset remains available", config.findPrint(target))
        assertNotNull("The active printer owns the selected process copy", config.findPrint(expectedScopedTitle))
        val visibleItems = activity.runOnUiThreadAndWait {
            PrintConfigFragment::class.java
                .getDeclaredMethod("getItems", Boolean::class.javaPrimitiveType)
                .apply { isAccessible = true }
                .invoke(fragment, true) as List<*>
        }
        val selectedTitles = visibleItems.filter { item ->
            item?.javaClass?.getMethod("isSelected")?.invoke(item) as? Boolean == true
        }.mapNotNull { item -> item?.javaClass?.getMethod("getTitle")?.invoke(item) as? String }
        assertEquals("Exactly the printer-scoped active profile is marked in the filtered list", listOf(expectedScopedTitle), selectedTitles)
    }

    @Test
    fun selectingPrinterProfileThroughMenuScopesItsActiveProcess() {
        val activity = compose.activity
        val config = SliceBeam.requireConfig()
        val previousPrinter = requireNotNull(config.presets?.get("printer"))
        val targetPrinter = "Anycubic 4Max Pro 0.4 nozzle"
        assertNotNull("The bundled test configuration must contain the target printer", config.findPrinter(targetPrinter))
        val previousPrinterSelection = PrinterProfileSelectionStore.read(previousPrinter)
        val previousTargetSelection = PrinterProfileSelectionStore.read(targetPrinter)

        try {
            activity.runOnUiThread {
                (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(4)
            }
            compose.waitUntil(timeoutMillis = 15_000) {
                activity.navigationDelegate.getCurrentFragment() is PrinterConfigFragment
            }

            compose.onNodeWithText(previousPrinter).assertIsDisplayed().performClick()
            compose.onNodeWithText(targetPrinter).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 15_000) {
                SliceBeam.requireConfig().presets?.get("printer") == targetPrinter
            }

            val selectedConfig = SliceBeam.requireConfig()
            val selectedProcess = requireNotNull(selectedConfig.presets?.get("print"))
            val scopedProcess = requireNotNull(selectedConfig.findPrint(selectedProcess))
            assertTrue(
                "Changing printers must scope the active process to the selected printer",
                PrinterScopedProcessProfile.belongsTo(scopedProcess, targetPrinter),
            )
            assertNotNull("The synchronized active filament must remain present", selectedConfig.findFilament(selectedConfig.presets?.get("filament")))
        } finally {
            previousPrinterSelection?.let { PrinterProfileSelectionStore.write(previousPrinter, it.process, it.filament) }
                ?: PrinterProfileSelectionStore.write(previousPrinter, null, null)
            previousTargetSelection?.let { PrinterProfileSelectionStore.write(targetPrinter, it.process, it.filament) }
                ?: PrinterProfileSelectionStore.write(targetPrinter, null, null)
        }
    }

    @Test
    fun printerBedSurfaceEditIsDraftUntilSavedAndCanBeDiscardedWithBackOrReset() {
        val activity = compose.activity
        val config = SliceBeam.requireConfig()
        val printerTitle = requireNotNull(config.presets?.get("printer"))
        val storedPrinter = requireNotNull(config.findPrinter(printerTitle))
        val storedBedSurface = storedPrinter.get("curr_bed_type")
        val option = requireNotNull(PrintConfigDef.getInstance().options["curr_bed_type"])
        val optionTitle = Slic3rLocalization.getString(option.getLabel())
        val optionValues = requireNotNull(option.enumValues)
        val optionLabels = requireNotNull(option.enumLabels).map(Slic3rLocalization::getString)

        activity.runOnUiThread {
            (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(4)
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            activity.navigationDelegate.getCurrentFragment() is PrinterConfigFragment
        }

        val draft = activity.runOnUiThreadAndWait {
            val fragment = activity.navigationDelegate.getCurrentFragment() as PrinterConfigFragment
            PrinterConfigFragment::class.java.getDeclaredMethod("getCurrentConfig")
                .apply { isAccessible = true }
                .invoke(fragment) as ConfigObject
        }
        val currentValue = requireNotNull(draft.get("curr_bed_type"))
        val targetIndex = optionValues.indices.first { optionValues[it] != currentValue }
        val targetValue = optionValues[targetIndex]
        val targetLabel = optionLabels[targetIndex]

        val generalCategory = Slic3rLocalization.getString("General")
        compose.onNodeWithText(generalCategory).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText(optionTitle).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText(targetLabel).assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            ProfileSettingsRepository.pendingCountFor(ConfigObject.PROFILE_LIST_PRINTER) > 0
        }

        assertEquals("The user-selected surface is reflected in the open profile draft", targetValue,
            ProfileConfigValueController.valueFor(ConfigObject.PROFILE_LIST_PRINTER, draft, "curr_bed_type",
                option.guiType, option.defaultValue, -1, false))
        assertEquals("Editing a profile must not write to the stored printer preset", storedBedSurface,
            storedPrinter.get("curr_bed_type"))

        // Android Back dismisses the settings route. The uncommitted edit remains a draft, while
        // the persisted preset continues to retain its original value.
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitUntil(timeoutMillis = 10_000) {
            activity.navigationDelegate.getCurrentFragment() !is PrinterConfigFragment
        }
        assertEquals(storedBedSurface, storedPrinter.get("curr_bed_type"))

        activity.runOnUiThread {
            (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(4)
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            activity.navigationDelegate.getCurrentFragment() is PrinterConfigFragment
        }
        compose.onNodeWithContentDescription(activity.getString(R.string.SettingsResetProfileAction))
            .assertIsDisplayed().performClick()
        compose.onNodeWithText(activity.getString(R.string.SettingsResetProfileTitle)).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertTrue("Dismissing reset must leave the edit draft pending",
            ProfileSettingsRepository.pendingCountFor(ConfigObject.PROFILE_LIST_PRINTER) > 0)
        assertEquals("Dismissing reset keeps the stored printer preset unchanged", storedBedSurface,
            storedPrinter.get("curr_bed_type"))

        compose.onNodeWithContentDescription(activity.getString(R.string.SettingsResetProfileAction))
            .performClick()
        compose.onNodeWithText(activity.getString(android.R.string.ok)).assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            ProfileSettingsRepository.pendingCountFor(ConfigObject.PROFILE_LIST_PRINTER) == 0
        }
        assertEquals("Confirmed reset discards the draft without persisting it", storedBedSurface,
            storedPrinter.get("curr_bed_type"))
    }

    @Test
    fun printerProfilePickerBackDismissesWithoutChangingAnyActivePreset() {
        val activity = compose.activity
        val config = SliceBeam.requireConfig()
        val printerBefore = requireNotNull(config.presets?.get("printer"))
        val processBefore = requireNotNull(config.presets?.get("print"))
        val filamentBefore = requireNotNull(config.presets?.get("filament"))
        val targetPrinter = "Anycubic 4Max Pro 0.4 nozzle"
        assertNotNull(config.findPrinter(targetPrinter))

        activity.runOnUiThread {
            (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(4)
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            activity.navigationDelegate.getCurrentFragment() is PrinterConfigFragment
        }

        compose.onNodeWithText(printerBefore).assertIsDisplayed().performClick()
        compose.onNodeWithText(targetPrinter).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()

        assertEquals("Back dismisses the printer picker without applying its highlighted alternative",
            printerBefore, config.presets?.get("printer"))
        assertEquals("Dismissing printer selection leaves the process preset unchanged",
            processBefore, config.presets?.get("print"))
        assertEquals("Dismissing printer selection leaves the filament preset unchanged",
            filamentBefore, config.presets?.get("filament"))
        compose.onNodeWithText(printerBefore).assertIsDisplayed()
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

private fun <T> MainActivity.runOnUiThreadAndWait(block: () -> T): T {
    val result = java.util.concurrent.atomic.AtomicReference<T>()
    val failure = java.util.concurrent.atomic.AtomicReference<Throwable?>()
    val latch = java.util.concurrent.CountDownLatch(1)
    runOnUiThread {
        try {
            result.set(block())
        } catch (error: Throwable) {
            failure.set(error)
        } finally {
            latch.countDown()
        }
    }
    check(latch.await(10, java.util.concurrent.TimeUnit.SECONDS)) { "Timed out waiting for the UI thread" }
    failure.get()?.let { throw AssertionError("Could not inspect the print profile dropdown", it) }
    return requireNotNull(result.get())
}
