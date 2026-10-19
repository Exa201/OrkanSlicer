package ru.ytkab0bp.slicebeam

import android.app.Application
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.fragment.FilamentConfigFragment
import ru.ytkab0bp.slicebeam.fragment.MakerFragment
import ru.ytkab0bp.slicebeam.fragment.PrintConfigFragment
import ru.ytkab0bp.slicebeam.fragment.PrinterConfigFragment
import ru.ytkab0bp.slicebeam.fragment.SettingsFragment
import ru.ytkab0bp.slicebeam.navigation.Fragment
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.navigation.NavigationDelegate
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Runs on an Android device to verify that a real workspace survives configuration changes. */
@RunWith(AndroidJUnit4::class)
class MainActivityRotationDeviceTest {
    @Test
    fun workspaceAndCurrentFragmentSurvivePortraitLandscapeRotation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        Prefs.getPrefs()?.edit()
            ?.remove("crash")
            // Keep the first-run folder picker from covering the activity under test.
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

        var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        var originalActivity: MainActivity? = null
        var originalDelegate: NavigationDelegate? = null
        var originalFragment: Fragment? = null
        var expectedBed: BedFragment? = null
        val loadedModelRef = AtomicReference<Model?>()
        val modelLoadFailure = AtomicReference<Exception?>()
        val modelLoadComplete = CountDownLatch(1)
        val modelFile = File(context.cacheDir, "rotation-test-cube.stl")
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity { activity ->
                originalOrientation = when (activity.resources.configuration.orientation) {
                    Configuration.ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
                originalActivity = activity
                originalDelegate = activity.navigationDelegate
                originalFragment = activity.navigationDelegate.getCurrentFragment()
                assertTrue("The model workspace should be the initial work area", originalFragment is BedFragment)
                val bed = originalFragment as BedFragment
                expectedBed = bed
                assertFragmentAttached(originalFragment)
                assertMenuNavigationIsCompose(bed)

                context.assets.open("models/xyz_cube.stl").use { source ->
                    modelFile.outputStream().use(source::copyTo)
                }
                bed.loadModel(modelFile, false, 1, object : BedFragment.ModelLoadCallback {
                    override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                        loadedModelRef.set(loadedModel)
                        modelLoadComplete.countDown()
                    }

                    override fun onFailed(error: Exception) {
                        modelLoadFailure.set(error)
                        modelLoadComplete.countDown()
                    }
                })
            }

            assertTrue("The workspace test model should finish loading", modelLoadComplete.await(60, TimeUnit.SECONDS))
            val model = loadedModelRef.get()
                ?: throw AssertionError("The workspace test model failed to load", modelLoadFailure.get())
            scenario.onActivity { activity ->
                originalFragment = activity.navigationDelegate.getCurrentFragment()
                assertSame("The model workspace should remain the active work area", expectedBed, originalFragment)
                assertSame("The model should be visible before rotation", model, expectedBed?.currentModel)
                assertTrue("The workspace test model should contain geometry", model.objectsCount > 0)
                assertFragmentAttached(originalFragment)
            }

            rotateAndAssert(scenario, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, originalActivity, originalDelegate, originalFragment, model)
            rotateAndAssert(scenario, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, originalActivity, originalDelegate, originalFragment, model)
            rotateAndAssert(scenario, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, originalActivity, originalDelegate, originalFragment, model)

            // Settings is reached from the workspace header rather than the five primary tabs.
            // Keep this separate route open while the adaptive navigation changes from bottom bar
            // to rail and back, and verify that its expanded panel state remains visible.
            scenario.onActivity { activity ->
                (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(5)
            }
            val settings = awaitFragment(scenario, SettingsFragment::class.java)
            val toggleSection = SettingsFragment::class.java
                .getDeclaredMethod("toggleSection", Int::class.javaPrimitiveType)
                .apply { isAccessible = true }
            toggleSection.invoke(settings, 0)
            assertTrue("The test should expand the first app-settings section", 0 in expandedSettingsSections(settings))
            scenario.onActivity { activity ->
                assertSame("App settings should be the visible route", settings, activity.navigationDelegate.getCurrentFragment())
                assertFragmentAttached(settings)
                assertTrue("App settings should be rendered by Compose", settings.getView() is androidx.compose.ui.platform.ComposeView)
            }
            rotateAndAssertFragment(
                scenario,
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                originalActivity,
                originalDelegate,
                settings,
            )
            assertTrue("The expanded app-settings section should survive landscape", 0 in expandedSettingsSections(settings))
            scenario.onActivity { activity ->
                // Re-entering the workspace from global settings in landscape must
                // measure its horizontally scrolling header with finite constraints.
                (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(0)
            }
            val workspaceAfterSettings = awaitFragment(scenario, BedFragment::class.java)
            scenario.onActivity { activity ->
                assertSame("The workspace should reopen from app settings in landscape", workspaceAfterSettings,
                    activity.navigationDelegate.getCurrentFragment())
                assertFragmentAttached(workspaceAfterSettings)
                assertMenuNavigationIsCompose(workspaceAfterSettings)
                assertNotNull("The workspace renderer must remain attached after landscape navigation",
                    workspaceAfterSettings.glView)
                assertTrue("The workspace header must receive a finite laid-out width",
                    workspaceContentWidth(workspaceAfterSettings) > 0)
            }
            rotateAndAssertFragment(
                scenario,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                originalActivity,
                originalDelegate,
                workspaceAfterSettings,
            )
            assertTrue("The workspace should remain usable after returning to portrait",
                workspaceAfterSettings.glView?.isAttachedToWindow == true)

            val primaryComposeRoutes = listOf(
                1 to MakerFragment::class.java,
                2 to PrintConfigFragment::class.java,
                3 to FilamentConfigFragment::class.java,
                4 to PrinterConfigFragment::class.java,
            )
            for ((slot, fragmentType) in primaryComposeRoutes) {
                scenario.onActivity { activity ->
                    (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(slot)
                }
                val screen = awaitFragment(scenario, fragmentType)
                scenario.onActivity { activity ->
                    assertSame("${fragmentType.simpleName} should be the active destination", screen, activity.navigationDelegate.getCurrentFragment())
                    assertFragmentAttached(screen)
                    assertTrue(
                        "${fragmentType.simpleName} should render from one direct Compose root",
                        screen.getView() is androidx.compose.ui.platform.ComposeView,
                    )
                }
                rotateAndAssertFragment(
                    scenario,
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                    originalActivity,
                    originalDelegate,
                    screen,
                )
                rotateAndAssertFragment(
                    scenario,
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                    originalActivity,
                    originalDelegate,
                    screen,
                )
            }
        } finally {
            runCatching {
                scenario.onActivity { activity -> activity.requestedOrientation = originalOrientation }
                instrumentation.waitForIdleSync()
            }
            scenario.close()
            modelFile.delete()
        }
    }

    private fun rotateAndAssert(
        scenario: ActivityScenario<MainActivity>,
        orientation: Int,
        expectedActivity: MainActivity?,
        expectedDelegate: NavigationDelegate?,
        expectedFragment: Fragment?,
        expectedModel: Model,
    ) {
        scenario.onActivity { activity -> activity.requestedOrientation = orientation }
        val expectedConfigurationOrientation = when (orientation) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> Configuration.ORIENTATION_PORTRAIT
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> Configuration.ORIENTATION_LANDSCAPE
            else -> Configuration.ORIENTATION_UNDEFINED
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val orientationDeadline = SystemClock.uptimeMillis() + 10_000L
        var actualOrientation = Configuration.ORIENTATION_UNDEFINED
        while (SystemClock.uptimeMillis() < orientationDeadline) {
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                actualOrientation = activity.resources.configuration.orientation
            }
            if (actualOrientation == expectedConfigurationOrientation) break
            // WindowManager applies requested orientations asynchronously on Android 16.
            SystemClock.sleep(50L)
        }
        scenario.onActivity { activity ->
            assertEquals("The requested screen orientation should be applied within 10 seconds", expectedConfigurationOrientation, actualOrientation)
            assertSame("The activity should handle configuration changes in place", expectedActivity, activity)
            assertSame("The screen stack should remain attached", expectedDelegate, activity.navigationDelegate)
            assertSame("The current work area should remain selected", expectedFragment, activity.navigationDelegate.getCurrentFragment())
            assertFragmentAttached(expectedFragment)
            val bed = expectedFragment as? BedFragment
                ?: throw AssertionError("The model workspace should stay active during rotation")
            val glView = bed.glView
            assertNotNull("The 3D viewport should be retained after rotation", glView)
            assertTrue("The 3D viewport should stay attached after rotation", glView!!.isAttachedToWindow)
            assertMenuNavigationIsCompose(bed)
            assertSame("The rendered model should survive rotation", expectedModel, bed.currentModel)
            assertTrue("The model geometry should still be available", expectedModel.objectsCount > 0)
        }
    }

    private fun rotateAndAssertFragment(
        scenario: ActivityScenario<MainActivity>,
        orientation: Int,
        expectedActivity: MainActivity?,
        expectedDelegate: NavigationDelegate?,
        expectedFragment: Fragment,
    ) {
        scenario.onActivity { activity -> activity.requestedOrientation = orientation }
        val expectedConfigurationOrientation = when (orientation) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> Configuration.ORIENTATION_PORTRAIT
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> Configuration.ORIENTATION_LANDSCAPE
            else -> Configuration.ORIENTATION_UNDEFINED
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val orientationDeadline = SystemClock.uptimeMillis() + 10_000L
        var actualOrientation = Configuration.ORIENTATION_UNDEFINED
        while (SystemClock.uptimeMillis() < orientationDeadline) {
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity -> actualOrientation = activity.resources.configuration.orientation }
            if (actualOrientation == expectedConfigurationOrientation) break
            SystemClock.sleep(50L)
        }
        scenario.onActivity { activity ->
            assertEquals("The requested screen orientation should be applied within 10 seconds", expectedConfigurationOrientation, actualOrientation)
            assertSame("The activity should handle configuration changes in place", expectedActivity, activity)
            assertSame("The screen stack should remain attached", expectedDelegate, activity.navigationDelegate)
            assertSame("The active route should stay selected during rotation", expectedFragment, activity.navigationDelegate.getCurrentFragment())
            assertFragmentAttached(expectedFragment)
        }
    }

    private fun <T : Fragment> awaitFragment(
        scenario: ActivityScenario<MainActivity>,
        type: Class<T>,
    ): T {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + 10_000L
        var current: T? = null
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val candidate = activity.navigationDelegate.getCurrentFragment()
                if (type.isInstance(candidate)) current = type.cast(candidate)
            }
            if (current != null) return requireNotNull(current)
            SystemClock.sleep(50L)
        }
        throw AssertionError("Timed out waiting for ${type.simpleName}; current route was ${current?.javaClass?.simpleName ?: "unknown"}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun expandedSettingsSections(fragment: SettingsFragment): Set<Int> {
        val stateField = SettingsFragment::class.java
            .getDeclaredField("expandedSections\$delegate")
            .apply { isAccessible = true }
        val state = stateField.get(fragment) as? MutableState<Set<Int>>
        return state?.value.orEmpty()
    }

    private fun assertFragmentAttached(fragment: Fragment?) {
        val view = fragment?.getView()
        assertNotNull("The active work area must retain its view", view)
        assertTrue("The active work area must remain attached after rotation", view!!.isAttachedToWindow)
    }

    private fun hasComposeView(view: View?): Boolean {
        if (view is ComposeView) return true
        if (view !is ViewGroup) return false
        return (0 until view.childCount).any { index -> hasComposeView(view.getChildAt(index)) }
    }

    private fun assertMenuNavigationIsCompose(fragment: BedFragment) {
        val field = BedFragment::class.java.getDeclaredField("contentView").apply { isAccessible = true }
        val workspaceRoot = field.get(fragment)
        assertTrue("The workspace navigation should be hosted by WorkspaceShell Compose", workspaceRoot is ComposeView)
        assertTrue(
            "The WorkspaceShell Compose root should stay attached",
            (workspaceRoot as ComposeView).isAttachedToWindow,
        )
    }

    private fun workspaceContentWidth(fragment: BedFragment): Int {
        val field = BedFragment::class.java.getDeclaredField("contentView").apply { isAccessible = true }
        return (field.get(fragment) as? View)?.width ?: 0
    }
}
