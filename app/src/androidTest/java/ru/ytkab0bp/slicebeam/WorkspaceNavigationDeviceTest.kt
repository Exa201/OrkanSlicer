package ru.ytkab0bp.slicebeam

import android.os.SystemClock
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.bed_menu.FileMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.ModelMenu
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Confirms menu transitions remove the previous View-backed host after its animation. */
@RunWith(AndroidJUnit4::class)
class WorkspaceNavigationDeviceTest {
    @Test fun switchingBetweenFileAndModelRoutesKeepsOnlyTheSelectedMenuAttached() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = app
        Prefs.init(app)
        val prefs = requireNotNull(Prefs.getPrefs())
        // SafeStartActivity reads this key as a String; restoring a Boolean poisons later launches.
        val previousCrash = runCatching { prefs.getString("crash", null) }.getOrNull()
        val previousPrompt = prefs.getBoolean("converter_storage_first_prompt", false)
        val previousLastCommit = prefs.getString("last_commit", null)
        val previousConfig = SliceBeam.CONFIG
        prefs.edit().remove("crash")
            ?.putBoolean("converter_storage_first_prompt", true)?.commit()
        Prefs.setLastCommit()
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
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                selectWorkspaceRoute(bed, context, 1)
            }
            awaitMenuTransition(scenario)
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                assertMenuAttached(bed, ModelMenu::class.java)
                selectWorkspaceRoute(bed, context, 0)
            }
            awaitMenuTransition(scenario)
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                assertMenuAttached(bed, FileMenu::class.java)
            }
        } finally {
            scenario.close()
            SliceBeam.CONFIG = previousConfig
            prefs.edit()
                .also { editor ->
                    if (previousCrash == null) editor.remove("crash")
                    else editor.putString("crash", previousCrash)
                }
                .putBoolean("converter_storage_first_prompt", previousPrompt)
                .also { editor ->
                    if (previousLastCommit == null) editor.remove("last_commit")
                    else editor.putString("last_commit", previousLastCommit)
                }
                .commit()
        }
    }

    private fun selectWorkspaceRoute(bed: BedFragment, context: android.content.Context, slot: Int) {
        val method = BedFragment::class.java.getDeclaredMethod(
            "onMenuNavigationSelected", android.content.Context::class.java,
            Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType,
        ).apply { isAccessible = true }
        method.invoke(bed, context, true, slot)
    }

    private fun awaitMenuTransition(scenario: ActivityScenario<MainActivity>) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val field = BedFragment::class.java.getDeclaredField("isAnimatingMenu").apply { isAccessible = true }
        val deadline = SystemClock.uptimeMillis() + 5_000L
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            var animating = true
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                animating = field.getBoolean(bed)
            }
            if (!animating) return
            SystemClock.sleep(40L)
        }
        throw AssertionError("Workspace menu transition did not finish")
    }

    private fun assertMenuAttached(bed: BedFragment, expectedType: Class<*>) {
        val controller = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)
        val currentSlot = controller.javaClass.getDeclaredMethod("getSelectedDestinationSlot")
            .apply { isAccessible = true }.invoke(controller) as Int
        val menuMap = BedFragment::class.java.getDeclaredField("menuMap")
            .apply { isAccessible = true }.get(bed) as android.util.SparseArray<*>
        val expectedMenu = menuMap.get(currentSlot)
        assertTrue("slot $currentSlot should resolve to ${expectedType.simpleName}", expectedType.isInstance(expectedMenu))
        val menuView = BedFragment::class.java.getDeclaredField("menuView")
            .apply { isAccessible = true }.get(bed) as FrameLayout
        assertEquals("only one route view should remain attached", 1, menuView.childCount)
        val getView = expectedType.getMethod("getView")
        assertSame("the active menu view should be the only attached child", getView.invoke(expectedMenu), menuView.getChildAt(0))
    }
}
