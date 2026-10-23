package ru.ytkab0bp.slicebeam

import android.app.Application
import android.os.SystemClock
import android.view.MotionEvent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import ru.ytkab0bp.slicebeam.components.bed_menu.FileMenuObjectActions
import ru.ytkab0bp.slicebeam.components.bed_menu.FileMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.TransformMenuItems
import ru.ytkab0bp.slicebeam.components.bed_menu.OrientationPositionMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.ModelMenuItems
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.SliceUploadController
import ru.ytkab0bp.slicebeam.components.FastenerFormLogic
import ru.ytkab0bp.slicebeam.utils.FastenerStandards
import ru.ytkab0bp.slicebeam.utils.FastenerStlWriter
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.navigation.Fragment
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.Vec3d
import ru.ytkab0bp.slicebeam.view.GLView

/** Exercises the real GLView tap path on an Android device with a loaded model. */
@RunWith(AndroidJUnit4::class)
class MainActivityTouchDeviceTest {
    @Test
    fun tappingGeneratedM12AndEmptyPlateDoesNotCrashAndLongPressKeepsActionsCollapsed() {
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

        val modelFile = File(context.cacheDir, "touch-test-m12-bolt.stl")
        val loadedModelRef = AtomicReference<Model?>()
        val loadFailure = AtomicReference<Exception?>()
        val loadFinished = CountDownLatch(1)
        val fastenerValues = FastenerFormLogic.defaults(size = FastenerStandards.count() - 1)
        assertEquals(null, FastenerFormLogic.validate(fastenerValues))
        val expectedFacets = FastenerStlWriter.writeBolt(
            modelFile,
            FastenerStandards.diameter(fastenerValues.size),
            FastenerFormLogic.parse(fastenerValues.second),
            FastenerFormLogic.parse(fastenerValues.first),
            FastenerFormLogic.parse(fastenerValues.fourth),
            FastenerFormLogic.parse(fastenerValues.third),
        )

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
            assertEquals("The M12 touch target should retain its generated facets", expectedFacets,
                model.getTriangleCount(0))
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

            // Exercise the real Edit-menu Move action with a loaded model. The preview is
            // intentionally cancelled with Android Back; persisted model coordinates must stay put.
            val translationBeforeEdit = Vec3d().also { model.getTranslation(0, it) }
            val positionToolRef = AtomicReference<OrientationPositionMenu?>()
            scenario.onActivity {
                val workspace = requireNotNull(bed)
                val openPosition: java.util.function.Consumer<android.view.View> =
                    java.util.function.Consumer {
                        OrientationPositionMenu(workspace).also { tool ->
                            positionToolRef.set(tool)
                            workspace.showWorkspaceComposeTool(tool)
                        }
                    }
                val noOp: java.util.function.Consumer<android.view.View> = java.util.function.Consumer { }
                val moveAction = TransformMenuItems(workspace, openPosition, noOp, noOp, noOp)
                    .createItems().single { it.titleRes == R.string.MenuToolbarMove }
                assertTrue("Move should be enabled for the selected model", moveAction.enabled)
                moveAction.onClick(requireNotNull(viewport))
            }
            instrumentation.waitForIdleSync()
            val positionTool = requireNotNull(positionToolRef.get())
            val attachDeadline = SystemClock.uptimeMillis() + 3_000L
            while (!positionTool.isAttached() && SystemClock.uptimeMillis() < attachDeadline) {
                instrumentation.waitForIdleSync()
                SystemClock.sleep(40L)
            }
            assertTrue("The Edit menu Move action should open its Compose tool", positionTool.isAttached())
            scenario.onActivity {
                positionTool.translateVisual(
                    translationBeforeEdit.x + 12.0,
                    translationBeforeEdit.y + 7.0,
                    translationBeforeEdit.z,
                )
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                val controller = BedFragment::class.java.getDeclaredField("workspaceController")
                    .apply { isAccessible = true }.get(requireNotNull(bed))
                assertEquals("Move should be the only active workspace tool",
                    OrientationPositionMenu::class.java.name,
                    controller.javaClass.getMethod("getActiveToolId").invoke(controller))
                assertTrue("Android Back should cancel the Move preview", requireNotNull(bed).onBackPressed())
            }
            instrumentation.waitForIdleSync()
            val translationAfterCancel = Vec3d().also { model.getTranslation(0, it) }
            assertEquals("Cancelling Move must preserve model X", translationBeforeEdit.x, translationAfterCancel.x, 0.0001)
            assertEquals("Cancelling Move must preserve model Y", translationBeforeEdit.y, translationAfterCancel.y, 0.0001)
            assertEquals("Cancelling Move must preserve model Z", translationBeforeEdit.z, translationAfterCancel.z, 0.0001)
            scenario.onActivity {
                assertFalse("Cancelled Move should clear its active tool state",
                    requireNotNull(positionToolRef.get()).isAttached())
            }
            SystemClock.sleep(350L)

            val objectCountBeforeDeleteCancel = model.objectsCount
            scenario.onActivity {
                val menuItems = ModelMenuItems(requireNotNull(bed)) {}.createItems()
                val deleteSelected = requireNotNull(menuItems.firstOrNull {
                    it.titleRes == R.string.ModelMenuDeleteSelected
                })
                assertTrue("Delete selected should be available for the selected object", deleteSelected.enabled)
                deleteSelected.onClick(requireNotNull(viewport))
            }
            val deleteDialogDeadline = SystemClock.uptimeMillis() + 5_000L
            while (!requireNotNull(bed).isWorkspaceMessageDialogAttached() &&
                SystemClock.uptimeMillis() < deleteDialogDeadline
            ) {
                instrumentation.waitForIdleSync()
                SystemClock.sleep(40L)
            }
            assertTrue("Model menu delete confirmation should use the Compose workspace dialog",
                requireNotNull(bed).isWorkspaceMessageDialogAttached())
            instrumentation.waitForIdleSync()
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue("Back cancellation must keep the workspace active", !activity.isFinishing)
                assertFalse("Back should cancel model deletion", requireNotNull(bed).isWorkspaceMessageDialogAttached())
                assertSameModelSelection(bed, model)
                assertEquals("Cancel must preserve all model objects", objectCountBeforeDeleteCancel,
                    requireNotNull(bed).currentModel?.objectsCount)
            }
            SystemClock.sleep(350L)

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
            SystemClock.sleep(350L)

            // Model object workflows now use the exclusive Compose workspace-dialog host.
            scenario.onActivity {
                val workspace = requireNotNull(bed)
                val actions = FileMenuObjectActions(workspace)
                actions.showObjectsDialog(requireNotNull(viewport))
            }
            val objectListDialogDeadline = SystemClock.uptimeMillis() + 5_000L
            while (!requireNotNull(bed).isWorkspaceMessageDialogAttached() &&
                SystemClock.uptimeMillis() < objectListDialogDeadline
            ) {
                instrumentation.waitForIdleSync()
                SystemClock.sleep(40L)
            }
            assertTrue("The object list should attach as a Compose workspace dialog",
                requireNotNull(bed).isWorkspaceMessageDialogAttached())
            instrumentation.waitForIdleSync()
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue("Closing the object list must keep the workspace active", !activity.isFinishing)
                assertFalse("Back should close the object list", requireNotNull(bed).isWorkspaceMessageDialogAttached())
            }

            scenario.onActivity {
                FileMenuObjectActions(requireNotNull(bed)).showDuplicateDialog(requireNotNull(viewport))
            }
            val duplicateDialogDeadline = SystemClock.uptimeMillis() + 5_000L
            while (!requireNotNull(bed).isWorkspaceMessageDialogAttached() &&
                SystemClock.uptimeMillis() < duplicateDialogDeadline
            ) {
                instrumentation.waitForIdleSync()
                SystemClock.sleep(40L)
            }
            assertTrue("Duplicate count input should attach as a Compose workspace dialog",
                requireNotNull(bed).isWorkspaceMessageDialogAttached())
            instrumentation.waitForIdleSync()
            var duplicateKeyboardVisible = false
            val duplicateImeDeadline = SystemClock.uptimeMillis() + 5_000L
            while (!duplicateKeyboardVisible && SystemClock.uptimeMillis() < duplicateImeDeadline) {
                scenario.onActivity { activity ->
                    duplicateKeyboardVisible = ViewCompat.getRootWindowInsets(activity.window.decorView)
                        ?.isVisible(WindowInsetsCompat.Type.ime()) == true
                }
                if (!duplicateKeyboardVisible) SystemClock.sleep(50L)
            }
            assertTrue("Duplicate count input should request the system keyboard", duplicateKeyboardVisible)
            // Back hides the numeric keyboard before dismissing the focused duplicate-count dialog.
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            instrumentation.waitForIdleSync()
            scenario.onActivity { assertTrue("First Back hides the keyboard and keeps duplicate input open",
                requireNotNull(bed).isWorkspaceMessageDialogAttached()) }
            var duplicateKeyboardHidden = false
            val hiddenImeDeadline = SystemClock.uptimeMillis() + 5_000L
            while (!duplicateKeyboardHidden && SystemClock.uptimeMillis() < hiddenImeDeadline) {
                scenario.onActivity { activity ->
                    duplicateKeyboardHidden = ViewCompat.getRootWindowInsets(activity.window.decorView)
                        ?.isVisible(WindowInsetsCompat.Type.ime()) != true
                }
                if (!duplicateKeyboardHidden) SystemClock.sleep(50L)
            }
            assertTrue("First Back should hide the duplicate input keyboard", duplicateKeyboardHidden)
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue("Closing duplicate input must keep the workspace active", !activity.isFinishing)
                assertFalse("Back should close duplicate input", requireNotNull(bed).isWorkspaceMessageDialogAttached())
            }
            // The Compose AlertDialog is backed by its own Android dialog window. Let its
            // dismissal finish before replacing it with the next transient dialog.
            SystemClock.sleep(350L)

            scenario.onActivity {
                val fileMenu = FileMenu().apply { onSetBed(requireNotNull(bed)) }
                val export = FileMenu::class.java.getDeclaredMethod("showProfileExportDialog", android.view.View::class.java)
                    .apply { isAccessible = true }
                export.invoke(fileMenu, requireNotNull(viewport))
                assertTrue("Profile export selection should attach as a Compose workspace dialog",
                    requireNotNull(bed).isWorkspaceMessageDialogAttached())
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(250L)
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue("Closing profile selection must keep the workspace active", !activity.isFinishing)
                assertFalse("Back should close profile selection", requireNotNull(bed).isWorkspaceMessageDialogAttached())
            }
            SystemClock.sleep(350L)

            scenario.onActivity {
                val workspace = requireNotNull(bed)
                val noOp = java.util.function.Consumer<android.view.View> { }
                val items = TransformMenuItems(workspace, noOp, noOp, noOp, noOp)
                val simplify = TransformMenuItems::class.java
                    .getDeclaredMethod("showSimplifyDialog", android.view.View::class.java)
                    .apply { isAccessible = true }
                simplify.invoke(items, requireNotNull(viewport))
            }
            val simplifyDialogDeadline = SystemClock.uptimeMillis() + 5_000L
            while (!requireNotNull(bed).isWorkspaceMessageDialogAttached() &&
                SystemClock.uptimeMillis() < simplifyDialogDeadline
            ) {
                instrumentation.waitForIdleSync()
                SystemClock.sleep(40L)
            }
            assertTrue("Simplification choices should attach as a Compose workspace dialog",
                requireNotNull(bed).isWorkspaceMessageDialogAttached())
            instrumentation.waitForIdleSync()
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue("Closing simplification choices must keep the workspace active", !activity.isFinishing)
                assertFalse("Back should close simplification choices", requireNotNull(bed).isWorkspaceMessageDialogAttached())
                assertEquals("Cancel must leave the model geometry unchanged", model.getTriangleCount(0),
                    requireNotNull(bed).currentModel?.getTriangleCount(0))
            }
            SystemClock.sleep(350L)

            val cameraControlModeBefore = Prefs.getCameraControlMode()
            scenario.onActivity { activity ->
                val cameraMenu = CameraMenu().apply { onSetBed(requireNotNull(bed)) }
                val controlMode = CameraMenu::class.java
                    .getDeclaredMethod("showControlModeDialog", android.content.Context::class.java)
                    .apply { isAccessible = true }
                controlMode.invoke(cameraMenu, activity)
                assertTrue("Camera control mode should attach as a Compose workspace dialog",
                    requireNotNull(bed).isWorkspaceMessageDialogAttached())
            }
            instrumentation.waitForIdleSync()
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue("Closing camera control mode must keep the workspace active", !activity.isFinishing)
                assertFalse("Back should close camera control mode", requireNotNull(bed).isWorkspaceMessageDialogAttached())
                assertEquals("Cancel must not change the stored camera control mode", cameraControlModeBefore,
                    Prefs.getCameraControlMode())
            }
            SystemClock.sleep(350L)

            val workspaceDialogDismissed = java.util.concurrent.atomic.AtomicBoolean(false)
            scenario.onActivity { activity ->
                val workspace = requireNotNull(bed)
                workspace.showWorkspaceMessageDialog(
                    title = "Test confirmation",
                    message = "Confirm route",
                    confirmLabel = "OK",
                    dismissLabel = "Cancel",
                    onDismiss = { workspaceDialogDismissed.set(true) },
                )
                assertTrue("Compose confirmation should attach to the workspace", workspace.isWorkspaceMessageDialogAttached())
            }
            instrumentation.waitForIdleSync()
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue("Back should leave the workspace activity active", activity.isFinishing.not())
                assertFalse("Back should close the Compose confirmation", requireNotNull(bed).isWorkspaceMessageDialogAttached())
                assertTrue("Back should resolve the Compose confirmation as dismissed", workspaceDialogDismissed.get())
            }
            SystemClock.sleep(350L)

            scenario.onActivity {
                SliceUploadController.showPreparationFailure(requireNotNull(bed), "Test preparation failure")
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertTrue(
                    "Slice preparation failures should use the Compose workspace dialog",
                    requireNotNull(bed).isWorkspaceMessageDialogAttached(),
                )
            }
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertFalse(
                    "Back should dismiss the slice preparation failure dialog",
                    requireNotNull(bed).isWorkspaceMessageDialogAttached(),
                )
            }
            SystemClock.sleep(350L)

            assertImportPromptDismissesWithoutApproval(
                scenario, instrumentation, requireNotNull(bed), "confirmImportSimplification",
                emptyArray<Class<*>>(), emptyArray<Any>(),
            )
            assertImportPromptDismissesWithoutApproval(
                scenario,
                instrumentation,
                requireNotNull(bed),
                "confirmLargeStlImport",
                arrayOf(Long::class.javaPrimitiveType!!),
                arrayOf(1_000_000L),
            )
        } finally {
            scenario.close()
            modelFile.delete()
        }
    }

    private fun assertImportPromptDismissesWithoutApproval(
        scenario: ActivityScenario<MainActivity>,
        instrumentation: android.app.Instrumentation,
        bed: BedFragment,
        methodName: String,
        parameterTypes: Array<Class<*>>,
        arguments: Array<Any>,
    ) {
        val decision = AtomicReference<Boolean?>()
        val method = BedFragment::class.java.getDeclaredMethod(methodName, *parameterTypes).apply { isAccessible = true }
        val worker = Thread { decision.set(method.invoke(bed, *arguments) as Boolean) }
        worker.start()

        val deadline = SystemClock.uptimeMillis() + 5_000L
        var attached = false
        while (!attached && SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            scenario.onActivity { attached = bed.isWorkspaceMessageDialogAttached() }
            if (!attached) SystemClock.sleep(50L)
        }
        assertTrue("$methodName should show the Compose workspace dialog", attached)
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        worker.join(5_000L)
        assertTrue("$methodName should release its waiting import worker", !worker.isAlive)
        assertEquals("Back must refuse $methodName unless the user explicitly confirms", false, decision.get())
        // The worker is released from the dialog callback before Compose has finished
        // disposing the Dialog window. Let that dismissal settle before the next prompt.
        SystemClock.sleep(350L)
        instrumentation.waitForIdleSync()
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
