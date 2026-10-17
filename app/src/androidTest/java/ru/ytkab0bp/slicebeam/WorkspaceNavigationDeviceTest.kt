package ru.ytkab0bp.slicebeam

import android.os.SystemClock
import android.webkit.WebView
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.bed_menu.FileMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.ModelMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.TransformMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.OptionsMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.WorkspaceComposeBedMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.SliceMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.OrientationPositionMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.OrientationRotationMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.TransformCutMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.TransformScaleMenu
import ru.ytkab0bp.slicebeam.components.WorkspaceComposeToolRoute
import ru.ytkab0bp.slicebeam.components.KromacutEditorMenu
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceDestination
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Confirms workspace menu transitions and direct Compose tool routes clean up correctly. */
@RunWith(AndroidJUnit4::class)
class WorkspaceNavigationDeviceTest {
    private var workspaceRenderer: android.view.View? = null

    @Test fun switchingBetweenFileAndModelRoutesKeepsOnlyTheSelectedMenuAttached() {
        workspaceRenderer = null
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
                workspaceRenderer = requireNotNull(bed.glView)
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
                selectWorkspaceRoute(bed, WorkspaceDestination.CAMERA.ordinal)
            }
            awaitMenuTransition(scenario)
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                assertMenuAttached(bed, CameraMenu::class.java)
                selectWorkspaceRoute(bed, WorkspaceDestination.EDIT.ordinal)
            }
            awaitMenuTransition(scenario)
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                assertMenuAttached(bed, TransformMenu::class.java)
                selectWorkspaceRoute(bed, WorkspaceDestination.PROJECT.ordinal)
            }
            awaitMenuTransition(scenario)
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                selectWorkspaceRoute(bed, WorkspaceDestination.TOOLS.ordinal)
            }
            awaitMenuTransition(scenario)
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                assertMenuAttached(bed, OptionsMenu::class.java)
                selectWorkspaceRoute(bed, WorkspaceDestination.SLICE.ordinal)
            }
            awaitMenuTransition(scenario)
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                assertMenuAttached(bed, SliceMenu::class.java)
                selectWorkspaceRoute(bed, WorkspaceDestination.PROJECT.ordinal)
            }
            awaitMenuTransition(scenario)
            exerciseComposeToolRoute(scenario, OrientationPositionMenu::class.java) { OrientationPositionMenu(it) }
            exerciseComposeToolRoute(scenario, OrientationRotationMenu::class.java) { OrientationRotationMenu(it) }
            exerciseComposeToolRoute(scenario, TransformScaleMenu::class.java) { TransformScaleMenu(it) }
            exerciseComposeToolRoute(scenario, TransformCutMenu::class.java) { TransformCutMenu(it) }
            exerciseComposeToolRoute(scenario, KromacutEditorMenu::class.java) { KromacutEditorMenu() }
            exercisePaintCancellationAgainstLoadedModel(scenario, context)
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

    private fun <T : WorkspaceComposeToolRoute> exerciseComposeToolRoute(
        scenario: ActivityScenario<MainActivity>,
        expectedType: Class<T>,
        create: (BedFragment) -> T,
    ) {
        var launchedMenu: T? = null
        scenario.onActivity { activity ->
            val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
            launchedMenu = create(bed)
            bed.showWorkspaceComposeTool(requireNotNull(launchedMenu))
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
            assertTrue(
                "${expectedType.simpleName} lifecycle adapter must not retain an Android route View",
                WorkspaceComposeToolRoute::class.java.declaredFields.none { android.view.View::class.java.isAssignableFrom(it.type) },
            )
            assertTrue("${expectedType.simpleName} Compose lifecycle should be active", WorkspaceComposeToolRoute::class.java
                .getDeclaredField("workspaceCompositionVisible").apply { isAccessible = true }.getBoolean(menu))
            if (expectedType == KromacutEditorMenu::class.java) {
                val webView = findViews(requireNotNull(bed.overlayLayout))
                    .filterIsInstance<WebView>()
                    .firstOrNull { it.isAttachedToWindow }
                assertTrue("Kromacut should embed its editor WebView in the Compose workspace", webView != null)
                val editor = requireNotNull(webView)
                assertTrue("Kromacut editor must keep file access disabled", editor.settings.allowFileAccess.not())
                assertTrue("Kromacut editor must keep JavaScript enabled for its bundled editor", editor.settings.javaScriptEnabled)
            }
            assertTrue("Android back should close ${expectedType.simpleName}", bed.onBackPressed())
        }
        instrumentation.waitForIdleSync()
        scenario.onActivity { activity ->
            val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
            assertEquals("${expectedType.simpleName} dismissal should clear the route", null,
                BedFragment::class.java.getDeclaredField("activeWorkspaceComposeTool")
                    .apply { isAccessible = true }.get(bed))
        }
    }

    private fun awaitMenuTransition(scenario: ActivityScenario<MainActivity>) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val field = BedFragment::class.java.getDeclaredField("isAnimatingMenu").apply { isAccessible = true }
        val deadline = SystemClock.uptimeMillis() + 5_000L
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            var settled = false
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                val menuMap = BedFragment::class.java.getDeclaredField("menuMap")
                    .apply { isAccessible = true }.get(bed) as android.util.SparseArray<*>
                val attachedRoutes = (0 until menuMap.size()).count { index ->
                    val menu = menuMap.valueAt(index) ?: return@count false
                    val routeView = menu.javaClass.getMethod("getView").invoke(menu) as? android.view.View
                    routeView?.isAttachedToWindow == true
                }
                val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
                    .apply { isAccessible = true }.get(bed)
                val currentSlot = workspaceController.javaClass.getMethod("getSelectedDestinationSlot")
                    .invoke(workspaceController) as Int
                val currentMenu = menuMap.get(currentSlot)
                val directComposeRoute = currentMenu is WorkspaceComposeBedMenu
                settled = !field.getBoolean(bed) && attachedRoutes == if (directComposeRoute) 0 else 1
            }
            if (settled) return
            SystemClock.sleep(40L)
        }
        throw AssertionError("Workspace menu transition did not finish with exactly one route attached")
    }

    private fun findViews(view: android.view.View): Sequence<android.view.View> = sequence {
        yield(view)
        if (view is android.view.ViewGroup) {
            for (index in 0 until view.childCount) yieldAll(findViews(view.getChildAt(index)))
        }
    }

    private fun exercisePaintCancellationAgainstLoadedModel(
        scenario: ActivityScenario<MainActivity>,
        context: android.content.Context,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val modelFile = File(context.cacheDir, "workspace-paint-cancel-fixture.3mf")
        instrumentation.context.assets.open("models/two-color-project.3mf").use { input ->
            modelFile.outputStream().use { input.copyTo(it) }
        }
        try {
            val loaded = CountDownLatch(1)
            var loadFailure: Exception? = null
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                bed.loadModel(modelFile, false, 1, object : BedFragment.ModelLoadCallback {
                    override fun onLoaded(loadedModel: ru.ytkab0bp.slicebeam.slic3r.Model, firstNewObject: Int, addedObjects: Int) {
                        loaded.countDown()
                    }

                    override fun onFailed(error: Exception) {
                        loadFailure = error
                        loaded.countDown()
                    }
                })
            }
            assertTrue("bundled paint fixture should load", loaded.await(60, TimeUnit.SECONDS))
            assertEquals("fixture import failed", null, loadFailure)

            val modelRef = arrayOfNulls<ru.ytkab0bp.slicebeam.slic3r.Model>(1)
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                modelRef[0] = bed.currentModel
                assertTrue("paint fixture must contain a model object", requireNotNull(bed.currentModel).objectsCount > 0)
                bed.enterPaintMode(0)
            }
            val model = requireNotNull(modelRef[0])
            awaitRendererPaintMode(scenario, expected = true)
            assertFalse("starting a paint session must not mutate facets", model.hasPaint(0))

            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                assertTrue("Android Back should close the active paint tool", bed.onBackPressed())
            }
            awaitRendererPaintMode(scenario, expected = false)
            assertFalse("cancelling paint with Android Back must discard the native preview", model.hasPaint(0))
            exerciseWorkspaceModeSwitch(scenario)
        } finally {
            modelFile.delete()
        }
    }

    private fun awaitRendererPaintMode(scenario: ActivityScenario<MainActivity>, expected: Boolean) {
        val reached = CountDownLatch(1)
        val actual = booleanArrayOf(!expected)
        scenario.onActivity { activity ->
            val glView = requireNotNull((activity.navigationDelegate.getCurrentFragment() as BedFragment).glView)
            glView.queueEvent {
                actual[0] = glView.renderer.isPaintMode
                reached.countDown()
            }
        }
        assertTrue("GL renderer did not reach paint mode=$expected", reached.await(10, TimeUnit.SECONDS))
        assertEquals(expected, actual[0])
    }

    private fun exerciseWorkspaceModeSwitch(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { activity ->
            (activity.navigationDelegate.getCurrentFragment() as BedFragment).enterMeasureMode()
        }
        awaitRendererFlag(scenario, expected = true) { isInMeasureMode }
        scenario.onActivity { activity ->
            val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
            assertTrue("measure mode should own the exclusive tool panel", bed.isMeasureMode)
            bed.enterVariableLayerHeightMode()
            assertFalse("switching tools should clear measure mode first", bed.isMeasureMode)
            assertTrue("variable layer height should replace the measure panel", bed.isVariableLayerHeightMode)
        }
        awaitRendererFlag(scenario, expected = false) { isInMeasureMode }
        scenario.onActivity { activity ->
            val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
            assertTrue("Android Back should close variable layer height", bed.onBackPressed())
            assertFalse("variable layer panel should release its active tool state", bed.isVariableLayerHeightMode)
        }
    }

    private fun awaitRendererFlag(
        scenario: ActivityScenario<MainActivity>,
        expected: Boolean,
        read: ru.ytkab0bp.slicebeam.render.GLRenderer.() -> Boolean,
    ) {
        val reached = CountDownLatch(1)
        val actual = booleanArrayOf(!expected)
        scenario.onActivity { activity ->
            val glView = requireNotNull((activity.navigationDelegate.getCurrentFragment() as BedFragment).glView)
            glView.queueEvent {
                actual[0] = glView.renderer.read()
                reached.countDown()
            }
        }
        assertTrue("GL renderer did not reach expected mode flag=$expected", reached.await(10, TimeUnit.SECONDS))
        assertEquals(expected, actual[0])
    }

    private fun assertMenuAttached(bed: BedFragment, expectedType: Class<*>) {
        assertSame("menu navigation must keep the GLView instance stable", workspaceRenderer, bed.glView)
        val workspaceRoot = BedFragment::class.java.getDeclaredField("contentView")
            .apply { isAccessible = true }.get(bed) as android.view.View
        assertEquals(
            "workspace shell should not host nested ComposeView wrappers for its controls",
            1,
            findViews(workspaceRoot).filterIsInstance<ComposeView>().count(),
        )
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
        val attachedRouteViews = (0 until menuMap.size()).mapNotNull { index ->
            val menu = menuMap.valueAt(index) ?: return@mapNotNull null
            val routeView = menu.javaClass.getMethod("getView").invoke(menu) as? android.view.View
            routeView?.takeIf { it.isAttachedToWindow }
        }
        if (expectedMenu is WorkspaceComposeBedMenu) {
            assertEquals("direct Compose menu route must not create an Android menu view", null, activeView)
            assertTrue("no legacy menu view should remain attached", attachedRouteViews.isEmpty())
        } else {
            assertTrue("the active menu view should be attached to the workspace", activeView?.isAttachedToWindow == true)
            assertEquals("only the selected route should remain attached after transition", 1, attachedRouteViews.size)
            assertEquals(activeView, attachedRouteViews.single())
        }
    }
}
