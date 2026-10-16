package ru.ytkab0bp.slicebeam

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.bed_menu.FileMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.ModelMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.CalibrationsMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.CalibrationModelsMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.SliceInfoMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.SliceLayersMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.SliceMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.OrientationPositionMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.OrientationRotationMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.TransformCutMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.TransformScaleMenu
import ru.ytkab0bp.slicebeam.components.UnfoldMenu
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Confirms workspace menu transitions and direct Compose tool routes clean up correctly. */
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
                selectWorkspaceRoute(bed, 1)
            }
            awaitMenuTransition(scenario)
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                assertMenuAttached(bed, ModelMenu::class.java)
                selectWorkspaceRoute(bed, 0)
            }
            awaitMenuTransition(scenario)
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                assertMenuAttached(bed, FileMenu::class.java)
            }
            exerciseComposeToolRoute(scenario, OrientationPositionMenu::class.java) { OrientationPositionMenu(it) }
            exerciseComposeToolRoute(scenario, OrientationRotationMenu::class.java) { OrientationRotationMenu(it) }
            exerciseComposeToolRoute(scenario, TransformScaleMenu::class.java) { TransformScaleMenu(it) }
            exerciseComposeToolRoute(scenario, TransformCutMenu::class.java) { TransformCutMenu(it) }
            exerciseComposeToolRoute(scenario, CalibrationsMenu::class.java) { CalibrationsMenu() }
            exerciseComposeToolRoute(scenario, CalibrationModelsMenu::class.java) { CalibrationModelsMenu() }
            exerciseComposeToolRoute(scenario, SliceInfoMenu::class.java) { SliceInfoMenu(SliceMenu()) }
            exerciseComposeToolRoute(scenario, SliceLayersMenu::class.java) { SliceLayersMenu() }
            exerciseComposeToolRoute(scenario, FileMenu.AIGeneratorMenu::class.java) { FileMenu.AIGeneratorMenu() }
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

    private fun selectWorkspaceRoute(bed: BedFragment, slot: Int) {
        val method = BedFragment::class.java.getDeclaredMethod(
            "onMenuNavigationSelected", Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType,
        ).apply { isAccessible = true }
        method.invoke(bed, true, slot)
    }

    private fun <T : UnfoldMenu> exerciseComposeToolRoute(
        scenario: ActivityScenario<MainActivity>,
        expectedType: Class<T>,
        create: (BedFragment) -> T,
    ) {
        var launchedMenu: T? = null
        scenario.onActivity { activity ->
            val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
            launchedMenu = create(bed)
            bed.showUnfoldMenu(requireNotNull(launchedMenu), requireNotNull(bed.glView))
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + 3_000L
        var attached = false
        while (SystemClock.uptimeMillis() < deadline && !attached) {
            instrumentation.waitForIdleSync()
            scenario.onActivity { attached = requireNotNull(launchedMenu).isAttached() }
            if (!attached) SystemClock.sleep(40L)
        }
        assertTrue("${expectedType.simpleName} Compose content should attach", attached)
        scenario.onActivity { activity ->
            val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
            val menu = requireNotNull(launchedMenu)
            assertTrue("${expectedType.simpleName} should be the active tool", expectedType.isInstance(menu))
            assertTrue("${expectedType.simpleName} direct Compose content should be visible", menu.isAttached())
            assertEquals("${expectedType.simpleName} should not allocate an Android route View", null,
                UnfoldMenu::class.java.getDeclaredField("workspaceContent")
                    .apply { isAccessible = true }.get(menu))
            assertTrue("${expectedType.simpleName} Compose lifecycle should be active", UnfoldMenu::class.java
                .getDeclaredField("workspaceCompositionVisible").apply { isAccessible = true }.getBoolean(menu))
            assertTrue("Android back should close ${expectedType.simpleName}", bed.onBackPressed())
        }
        instrumentation.waitForIdleSync()
        scenario.onActivity { activity ->
            val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
            assertEquals("${expectedType.simpleName} dismissal should clear the route", null,
                BedFragment::class.java.getDeclaredField("currentUnfoldMenu")
                    .apply { isAccessible = true }.get(bed))
        }
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
        val getView = expectedType.getMethod("getView")
        val activeView = getView.invoke(expectedMenu) as? android.view.View
        assertTrue("the active menu view should be attached to the workspace", activeView?.isAttachedToWindow == true)
        val attachedRouteViews = (0 until menuMap.size()).mapNotNull { index ->
            val menu = menuMap.valueAt(index) ?: return@mapNotNull null
            val routeView = menu.javaClass.getMethod("getView").invoke(menu) as? android.view.View
            routeView?.takeIf { it.isAttachedToWindow }
        }
        assertEquals("only the selected route should remain attached after transition", 1, attachedRouteViews.size)
        assertEquals(activeView, attachedRouteViews.single())
    }
}
