package ru.ytkab0bp.slicebeam

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.PrinterScopedProcessProfile
import ru.ytkab0bp.slicebeam.fragment.PrintConfigFragment
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

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
