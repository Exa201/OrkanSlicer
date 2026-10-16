package ru.ytkab0bp.slicebeam

import android.app.Application
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.navigation.Fragment
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.view.GLView

/** Exercises the real GLView tap path on an Android device with a loaded model. */
@RunWith(AndroidJUnit4::class)
class MainActivityTouchDeviceTest {
    @Test
    fun tappingModelAndEmptyPlateDoesNotOpenModelMenuAndLongPressKeepsActionsCollapsed() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        Prefs.getPrefs()?.edit()
            ?.remove("crash")
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

        val modelFile = File(context.cacheDir, "touch-test-cube.stl")
        val loadedModelRef = AtomicReference<Model?>()
        val loadFailure = AtomicReference<Exception?>()
        val loadFinished = CountDownLatch(1)
        context.assets.open("models/xyz_cube.stl").use { source -> modelFile.outputStream().use(source::copyTo) }

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            var bed: BedFragment? = null
            scenario.onActivity { activity ->
                val current = activity.navigationDelegate.getCurrentFragment()
                assertTrue("The model workspace should be active", current is BedFragment)
                bed = current as BedFragment
                bed.loadModel(modelFile, false, 1, object : BedFragment.ModelLoadCallback {
                    override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                        loadedModelRef.set(loadedModel)
                        loadFinished.countDown()
                    }

                    override fun onFailed(error: Exception) {
                        loadFailure.set(error)
                        loadFinished.countDown()
                    }
                })
            }

            assertTrue("The touch test model should finish loading", loadFinished.await(60, TimeUnit.SECONDS))
            val model = loadedModelRef.get() ?: throw AssertionError("The touch test model failed to load", loadFailure.get())
            assertEquals("The workspace should contain the touch target", 1, model.objectsCount)
            var hitPoint: Pair<Float, Float>? = null
            var viewport: GLView? = null
            var viewportDescription = "not measured"
            val hitDeadline = SystemClock.uptimeMillis() + 10_000L
            while (hitPoint == null && SystemClock.uptimeMillis() < hitDeadline) {
                instrumentation.waitForIdleSync()
                scenario.onActivity {
                    val glView = requireNotNull(bed?.glView)
                    viewport = glView
                    if (glView.width > 0 && glView.height > 0) {
                        // GLSurfaceView draws only when requested. A completed model-load callback
                        // can run before the first frame has lazily built the GLModel raycaster.
                        glView.requestRender()
                        val renderScale = Prefs.getRenderScale()
                        val modelCount = glView.renderer.model?.objectsCount ?: -1
                        val meshCount = try {
                            val field = glView.renderer.javaClass.getDeclaredField("glModels")
                            field.isAccessible = true
                            (field.get(glView.renderer) as? List<*>)?.size ?: -1
                        } catch (_: ReflectiveOperationException) {
                            -1
                        }
                        viewportDescription = "view=${glView.width}x${glView.height}, renderer=${glView.renderer.viewportWidth}x${glView.renderer.viewportHeight}, renderScale=$renderScale, models=$modelCount, meshes=$meshCount"
                        if (glView.renderer.viewportWidth <= 0 || glView.renderer.viewportHeight <= 0) return@onActivity
                        // A 20 mm test cube can cover less than one eighth of a tall phone
                        // viewport, so coarse samples can miss it on different aspect ratios.
                        for (row in 1..63 step 2) {
                            for (column in 1..63 step 2) {
                                val x = glView.width * column / 64f
                                val y = glView.height * row / 64f
                                if (glView.renderer.raycastObjectIndex(x * renderScale, y * renderScale) == 0) {
                                    hitPoint = x to y
                                    break
                                }
                            }
                            if (hitPoint != null) break
                        }
                    }
                }
                if (hitPoint == null) SystemClock.sleep(100L)
            }

            val hit = hitPoint ?: throw AssertionError("Could not find the loaded model in the 3D viewport ($viewportDescription)")
            val glView = requireNotNull(viewport)
            scenario.onActivity {
                assertEquals("The GL renderer must track the actual surface width", glView.width, glView.renderer.viewportWidth)
                assertEquals("The GL renderer must track the actual surface height", glView.height, glView.renderer.viewportHeight)
                tap(glView, hit.first, hit.second)
                SystemClock.sleep(90L)
                tap(glView, hit.first, hit.second)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertSameModelSelection(bed, model)
                assertModelSelectedWithoutModal(activity)
            }

            val emptyPlatePointRef = AtomicReference<Pair<Float, Float>?>()
            scenario.onActivity { emptyPlatePointRef.set(findEmptyPlatePoint(glView)) }
            val emptyPlatePoint = requireNotNull(emptyPlatePointRef.get())
            scenario.onActivity {
                tap(glView, emptyPlatePoint.first, emptyPlatePoint.second)
                SystemClock.sleep(90L)
                tap(glView, emptyPlatePoint.first, emptyPlatePoint.second)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals("Tapping an empty plate area should clear the model selection", -1, glView.renderer.selectedObject)
                assertTrue("Tapping the empty plate must not open a modal action dialog", activity.hasWindowFocus())
            }

            // A deliberate tap on the model restores its selection without opening a dialog.
            scenario.onActivity { tap(glView, hit.first, hit.second) }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertSameModelSelection(bed, model)
                assertModelSelectedWithoutModal(activity)
            }

            val downTime = SystemClock.uptimeMillis()
            scenario.onActivity { dispatch(glView, MotionEvent.ACTION_DOWN, hit.first, hit.second, downTime, downTime) }
            // Let Android deliver the actual long-press callback before releasing the finger.
            SystemClock.sleep(650L)
            scenario.onActivity {
                val now = SystemClock.uptimeMillis()
                dispatch(glView, MotionEvent.ACTION_UP, hit.first, hit.second, downTime, now)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertSameModelSelection(bed, model)
                assertModelSelectedWithoutModal(activity)
            }

            // Explicitly opening the model action list should use the modal Compose surface;
            // system Back must dismiss that surface before leaving the workspace.
            scenario.onActivity { activity ->
                requireNotNull(bed).showSelectedModelContextMenu()
                assertTrue("The workspace activity should still be active", activity.isFinishing.not())
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue("The model action dialog should attach to the workspace", requireNotNull(bed).isModelContextMenuAttached())
                assertEquals("The selected model should remain active behind its actions", 0,
                    requireNotNull(bed).glView!!.renderer.selectedObject)
            }
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue("Back should leave the workspace activity active", activity.isFinishing.not())
                assertFalse("Back should dismiss the model action dialog", requireNotNull(bed).isModelContextMenuAttached())
            }
        } finally {
            scenario.close()
            modelFile.delete()
        }
    }

    private fun assertSameModelSelection(bed: BedFragment?, model: Model) {
        val current = requireNotNull(bed)
        assertTrue("The test model should still be on the active plate", current.currentModel === model)
        assertEquals("The touched model should remain the selected object", 0, current.glView!!.renderer.selectedObject)
    }

    private fun assertModelSelectedWithoutModal(activity: MainActivity) {
        // Object actions live in the fixed workspace menu; a tap must not open a modal dialog.
        assertTrue("Tapping the model must not open a modal action dialog", activity.hasWindowFocus())
    }

    private fun tap(view: GLView, x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        dispatch(view, MotionEvent.ACTION_DOWN, x, y, downTime, downTime)
        SystemClock.sleep(50L)
        dispatch(view, MotionEvent.ACTION_UP, x, y, downTime, SystemClock.uptimeMillis())
    }

    private fun findEmptyPlatePoint(view: GLView): Pair<Float, Float> {
        val renderScale = Prefs.getRenderScale()
        // Stay away from the model's center and scan inside the actual GL surface rather than
        // using an arbitrary screen coordinate that could hit a toolbar or an overlay.
        for (row in 2..22 step 2) {
            for (column in 2..22 step 2) {
                val x = view.width * column / 24f
                val y = view.height * row / 24f
                if (view.renderer.raycastObjectIndex(x * renderScale, y * renderScale) == -1) {
                    return x to y
                }
            }
        }
        throw AssertionError("Could not find an empty area on the visible build plate")
    }

    private fun dispatch(view: GLView, action: Int, x: Float, y: Float, downTime: Long, eventTime: Long) {
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        try {
            assertTrue("GLView should consume the test touch sequence", view.dispatchTouchEvent(event))
        } finally {
            event.recycle()
        }
    }

}
