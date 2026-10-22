package ru.ytkab0bp.slicebeam

import android.app.Application
import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertArrayEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.os.SystemClock
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import ru.ytkab0bp.slicebeam.components.bed_menu.FileMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.ListBedMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraPreset
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraPresetGeometry
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraVector
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceViewCubeGeometry
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceViewFace
import ru.ytkab0bp.slicebeam.components.PreSliceFilamentDialog
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.GLModel
import ru.ytkab0bp.slicebeam.slic3r.PaintSession
import ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult
import ru.ytkab0bp.slicebeam.render.GLRenderer
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceDestination
import ru.ytkab0bp.slicebeam.ui.navigation.AdaptiveNavigationPolicy
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.Vec3d
import java.util.concurrent.atomic.AtomicReference

/** Reproduces the first Project tap after a cold workspace launch. */
@RunWith(AndroidJUnit4::class)
class InitialProjectMenuDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()
    private var lastDeviceTapDiagnostic = "not tapped"
    private var previousRequestedOrientation: Int? = null

    /** Allows a true Activity configuration change for the full menu class on real devices. */
    @Before
    fun applyRequestedWorkspaceTestOrientation() {
        val requested = InstrumentationRegistry.getArguments().getString("ui2.testOrientation")
        val target = when (requested?.lowercase()) {
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> return
        }
        val activity = compose.activity
        previousRequestedOrientation = activity.requestedOrientation
        activity.runOnUiThread { activity.requestedOrientation = target }
        val expected = if (target == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            Configuration.ORIENTATION_LANDSCAPE
        } else {
            Configuration.ORIENTATION_PORTRAIT
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.activity.resources.configuration.orientation == expected
        }
    }

    @After
    fun restoreRequestedWorkspaceTestOrientation() {
        val previous = previousRequestedOrientation ?: return
        previousRequestedOrientation = null
        val activity = compose.activity
        activity.runOnUiThread { activity.requestedOrientation = previous }
        val expected = when (previous) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> Configuration.ORIENTATION_LANDSCAPE
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> Configuration.ORIENTATION_PORTRAIT
            else -> null
        }
        if (expected != null) {
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.activity.resources.configuration.orientation == expected
            }
        }
    }

    @Test
    fun compactViewCubeIsVisibleAccessibleAndAtLeast48Dp() {
        // A fresh debug install can show the in-app changelog above the workspace.
        // Dismiss it so this checks the header at the requested display size.
        val changelogTitle = compose.activity.getString(R.string.Changelog)
        val changelogOk = compose.activity.getString(R.string.ChangelogOK)
        fun cubeIsDisplayed(): Boolean = try {
            compose.onNodeWithTag("workspace-view-cube").assertIsDisplayed()
            true
        } catch (_: AssertionError) {
            false
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(changelogTitle).fetchSemanticsNodes().isNotEmpty() || cubeIsDisplayed()
        }
        if (compose.onAllNodesWithText(changelogTitle).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText(changelogOk).performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(changelogTitle).fetchSemanticsNodes().isEmpty() && cubeIsDisplayed()
            }
        }
        val cube = compose.onNodeWithTag("workspace-view-cube").assertIsDisplayed()
        val node = cube.fetchSemanticsNode()
        val printerActionDescription = listOf(
            R.string.PrinterOpenWebDescription,
            R.string.PrinterWebConfigureDescription,
        ).map(compose.activity::getString).firstOrNull { description ->
            compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        } ?: error("Printer header action should expose an accessibility description")
        val printerAction = compose.onNodeWithContentDescription(printerActionDescription)
            .assertIsDisplayed().fetchSemanticsNode()
        val screenRight = compose.activity.window.decorView.width.toFloat()
        assertTrue("Printer header action must remain on screen", printerAction.boundsInRoot.right <= screenRight)
        val density = compose.activity.resources.displayMetrics.density
        assertTrue("View cube must have a 48 dp touch target", node.size.width / density >= 48f)
        assertTrue("View cube must have a 48 dp touch target", node.size.height / density >= 48f)
        assertEquals(6, node.config[SemanticsActions.CustomActions].size)
    }

    @Test
    fun modelMenuSeparatesSelectedObjectAndWholePlateActions() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "model-menu-scope-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            requireNotNull(bed.glView).selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(10)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarMove))
                .performScrollTo().assertIsDisplayed()
            scrollMenuToIndex(1)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDuplicate))
                .performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuArrange))
                .assertDoesNotExist()

            selectAllObjectsScope()
            scrollMenuToIndex(0)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuArrange))
                .performScrollTo().assertIsDisplayed()
            scrollMenuToIndex(2)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuSelectAll))
                .performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarMove))
                .assertDoesNotExist()

            openEditMenu(activity)
            revealMenuAction(activity, R.string.ModelUndo)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelUndo))
                .assertIsDisplayed()
            revealMenuAction(activity, R.string.MenuToolbarMove)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarMove))
                .assertIsDisplayed().assertIsEnabled()
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuBooleanUnionRunsForAllThreeSelectedObjects() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-menu-three-boolean-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            val width = AtomicReference<Double>()
            glView.queueEvent {
                val model = requireNotNull(glView.renderer.model)
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(0, min, max)
                width.set(max.x - min.x)
                model.addObject(model, 0)
                model.addObject(model, 0)
                model.translate(1, requireNotNull(width.get()) * 0.5, 0.0, 0.0)
                model.translate(2, requireNotNull(width.get()), 0.0, 0.0)
                glView.renderer.selectAllObjects()
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("test objects should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            selectAllObjectsScope()
            revealMenuAction(activity, R.string.ModelMenuBoolean)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuBoolean))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelBooleanUnion)).performClick()

            compose.waitUntil(timeoutMillis = 30_000) { bed.currentModel?.objectsCount == 1 }
            val widthAfter = AtomicReference<Double>()
            val checked = CountDownLatch(1)
            glView.queueEvent {
                val model = requireNotNull(glView.renderer.model)
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(0, min, max)
                widthAfter.set(max.x - min.x)
                checked.countDown()
            }
            assertTrue("Boolean result bounds should be readable on the GL thread", checked.await(10, TimeUnit.SECONDS))
            assertEquals("UI should union all selected solids, not just two", 2.0 * requireNotNull(width.get()),
                requireNotNull(widthAfter.get()), 0.03)
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 3 }
            assertEquals("Undo should restore all three source objects", 3, readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            assertEquals("Redo should restore the single Boolean result", 1, readObjectCountOnGl(glView))
            val redoneBounds = readExactBoundsOnGl(glView, 0).single()
            assertEquals("Redo should preserve the measured union width", 2.0 * requireNotNull(width.get()),
                redoneBounds[3] - redoneBounds[0], 0.03)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun objectListCanSelectAndPlaceAGroupOnAnotherModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-menu-group-placement-test.stl")
        val roundTripFile = File(activity.cacheDir, "model-menu-group-placement-test.3mf")
        val gcodeFile = File(activity.cacheDir, "model-menu-group-placement-test.gcode")
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        val testSlots = listOf(
            FilamentSlot(0xFF303030.toInt(), "Placement T0"),
            FilamentSlot(0xFF3366CC.toInt(), "Placement T1"),
            FilamentSlot(0xFFF2F2F2.toInt(), "Placement T2"),
        )
        Prefs.setStoredFilamentSlots(testSlots)
        SliceBeam.INSTANCE = activity.applicationContext as SliceBeam
        Prefs.init(activity.applicationContext as SliceBeam)
        TemporaryProjectSettings.clear()
        val previousConfig = SliceBeam.CONFIG
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigFile = configFile.takeIf { it.isFile }?.readBytes()
        val prefs = requireNotNull(Prefs.getPrefs())
        val snapshotKeys = listOf("mmu_snapshot_printer", "mmu_snapshot")
        val previousSnapshots = snapshotKeys.associateWith { prefs.getString(it, null) }
        var roundTrippedModel: Model? = null
        var sliceResult: GCodeProcessorResult? = null
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            val width = AtomicReference<Double>()
            glView.queueEvent {
                val renderer = glView.renderer
                val model = requireNotNull(renderer.model)
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(0, min, max)
                width.set(max.x - min.x)
                model.addObject(model, 0)
                model.addObject(model, 0)
                model.translate(1, requireNotNull(width.get()) * 1.5, 0.0, 0.0)
                model.translate(2, requireNotNull(width.get()) * 3.0, requireNotNull(width.get()) * 2.0, 0.0)
                model.setUniformFilament(0, 1)
                model.setUniformFilament(1, 2)
                model.setUniformFilament(2, 3)
                renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("group-placement objects should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.WorkspaceObjects)
            compose.onNodeWithContentDescription(activity.getString(R.string.WorkspaceObjects))
                .assertIsDisplayed().performClick()
            compose.onNodeWithTag("model-objects-group-selection-toggle")
                .assertIsDisplayed().performClick()
            compose.onNodeWithTag("model-object-row-1").performClick()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag("model-objects-selection-confirm").fetchSemanticsNodes().isEmpty()
            }
            val selectionAfterBack = AtomicReference<IntArray>()
            val backSelectionReady = CountDownLatch(1)
            glView.queueEvent {
                selectionAfterBack.set(glView.renderer.selectedObjectIndices())
                backSelectionReady.countDown()
            }
            assertTrue("cancelled group selection should be readable on GL", backSelectionReady.await(10, TimeUnit.SECONDS))
            assertArrayEquals("Android Back discards the staged group selection", intArrayOf(0),
                requireNotNull(selectionAfterBack.get()))

            openModelMenu(activity)
            revealMenuAction(activity, R.string.WorkspaceObjects)
            compose.onNodeWithContentDescription(activity.getString(R.string.WorkspaceObjects))
                .assertIsDisplayed().performClick()
            compose.onNodeWithTag("model-objects-group-selection-toggle")
                .assertIsDisplayed().performClick()
            compose.onNodeWithTag("model-object-row-1").performClick()
            compose.onNodeWithTag("model-objects-selection-confirm").performClick()
            compose.waitUntil(timeoutMillis = 10_000) { glView.renderer.selectedObjectsCount == 2 }
            val selectedIndices = AtomicReference<IntArray>()
            val selectionReady = CountDownLatch(1)
            glView.queueEvent {
                selectedIndices.set(glView.renderer.selectedObjectIndices())
                selectionReady.countDown()
            }
            assertTrue("selected group should be readable on the GL thread", selectionReady.await(10, TimeUnit.SECONDS))
            assertArrayEquals("the object list should select only the two chosen models",
                intArrayOf(0, 1), requireNotNull(selectedIndices.get()))

            val before = AtomicReference<List<DoubleArray>>()
            val boundsReady = CountDownLatch(1)
            glView.queueEvent {
                val model = requireNotNull(glView.renderer.model)
                before.set((0..2).map { index ->
                    val min = Vec3d(); val max = Vec3d()
                    model.getBoundingBoxExact(index, min, max)
                    doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z)
                })
                boundsReady.countDown()
            }
            assertTrue("pre-placement bounds should be collected on the GL thread", boundsReady.await(10, TimeUnit.SECONDS))

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.ModelMenuPlaceOnObject)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuPlaceOnObject))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithTag("model-choice-option-0").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 15_000) { readObjectCountOnGl(glView) == 1 }

            val after = AtomicReference<List<DoubleArray>>()
            val placedFilaments = AtomicReference<IntArray>()
            val afterReady = CountDownLatch(1)
            glView.queueEvent {
                val model = glView.renderer.model
                if (model != null && model.objectsCount == 1) {
                    after.set((0 until model.objectsCount).map { index ->
                        val min = Vec3d(); val max = Vec3d()
                        model.getBoundingBoxExact(index, min, max)
                        doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z)
                    })
                    placedFilaments.set(model.getUsedFilaments(0))
                }
                afterReady.countDown()
            }
            assertTrue("post-placement bounds should be collected on the GL thread", afterReady.await(15, TimeUnit.SECONDS))
            val original = requireNotNull(before.get())
            val placed = requireNotNull(after.get())
            assertEquals("placing selected objects on a model should create one printable multipart object", 1, placed.size)
            assertArrayEquals("the combined model should retain each source object's filament assignment",
                intArrayOf(1, 2, 3), requireNotNull(placedFilaments.get()))
            assertEquals("group placement centers the selected group over the target on X",
                (original[2][0] + original[2][3]) / 2.0, (placed[0][0] + placed[0][3]) / 2.0, 0.02)
            assertEquals("group placement centers the selected group over the target on Y",
                (original[2][1] + original[2][4]) / 2.0, (placed[0][1] + placed[0][4]) / 2.0, 0.02)
            assertEquals("the assembly still includes the target down to its original bottom",
                original[2][2], placed[0][2], 0.02)
            assertEquals("the selected group is stacked above the target object's top",
                original[2][5] + (original[0][5] - original[0][2]), placed[0][5], 0.02)

            // Verify the exact real menu operation survives project serialization and slicing.
            activity.assets.open("orca_profiles/Anycubic.ini").use {
                SliceBeam.CONFIG = Slic3rConfigWrapper(it)
            }
            SliceBeam.requireConfig().presets = ConfigObject("presets").apply {
                put("printer", "Anycubic Kobra 3 0.4 nozzle")
                put("print", "0.16mm Standard")
                put("filament", "Anycubic Generic PLA")
            }
            val clearSnapshots = prefs.edit()
            snapshotKeys.forEach(clearSnapshots::remove)
            assertTrue("stale printer filament snapshots should be cleared for this fixture",
                clearSnapshots.commit())
            val exportReady = CountDownLatch(1)
            val exportFailure = AtomicReference<Throwable?>()
            glView.queueEvent {
                try {
                    val model = requireNotNull(glView.renderer.model)
                    SliceBeam.genCurrentConfig(model)
                    model.export3mf(configFile.absolutePath, roundTripFile.absolutePath)
                } catch (error: Throwable) {
                    exportFailure.set(error)
                } finally {
                    exportReady.countDown()
                }
            }
            assertTrue("placed group should export through the real renderer model",
                exportReady.await(60, TimeUnit.SECONDS))
            exportFailure.get()?.let { throw AssertionError("placed group 3MF export failed", it) }
            assertTrue("3MF export should produce a non-empty project", roundTripFile.isFile && roundTripFile.length() > 0L)
            roundTrippedModel = Model(roundTripFile, 1)
            assertEquals("the grouped placement should remain one multipart object after reload",
                1, roundTrippedModel!!.objectsCount)
            assertArrayEquals("the UI placement's three material assignments should survive 3MF reload",
                intArrayOf(1, 2, 3), roundTrippedModel!!.getUsedFilaments(0))
            SliceBeam.genCurrentConfig(roundTrippedModel!!)
            sliceResult = roundTrippedModel!!.slice(configFile.absolutePath, gcodeFile.absolutePath, null)
            assertTrue("the reloaded UI placement should produce non-empty G-code",
                gcodeFile.isFile && gcodeFile.length() > 0L)
            val gcode = gcodeFile.readText()
            val positiveExtrusion = Regex("""(?m)^G1[^\r\n]*\bE([0-9]+(?:\.[0-9]+)?)""")
                .findAll(gcode).any { it.groupValues[1].toDoubleOrNull()?.let { value -> value > 0.0 } == true }
            assertTrue("the reloaded UI placement should produce positive deposited extrusion", positiveExtrusion)
            val emittedTools = Regex("(?m)^T(\\d+)(?:\\s|$)").findAll(gcode)
                .map { it.groupValues[1].toInt() }.toSet()
            assertTrue("placed target filament T0 must be used, tools=$emittedTools", 0 in emittedTools)
            assertTrue("placed group filament T1 must be used, tools=$emittedTools", 1 in emittedTools)
            assertTrue("placed group filament T2 must be used, tools=$emittedTools", 2 in emittedTools)

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 3 }
            val restoredFilaments = AtomicReference<List<Int>>()
            val restoredReady = CountDownLatch(1)
            glView.queueEvent {
                val model = requireNotNull(glView.renderer.model)
                restoredFilaments.set((0..2).flatMap { model.getUsedFilaments(it).toList() })
                restoredReady.countDown()
            }
            assertTrue("Undo should restore source-object materials", restoredReady.await(10, TimeUnit.SECONDS))
            assertEquals(listOf(1, 2, 3), requireNotNull(restoredFilaments.get()))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            val redoneFilaments = AtomicReference<IntArray>()
            val redoReady = CountDownLatch(1)
            glView.queueEvent {
                redoneFilaments.set(glView.renderer.model!!.getUsedFilaments(0))
                redoReady.countDown()
            }
            assertTrue("Redo should restore multipart material assignments", redoReady.await(10, TimeUnit.SECONDS))
            assertArrayEquals(intArrayOf(1, 2, 3), requireNotNull(redoneFilaments.get()))
        } finally {
            sliceResult?.release()
            roundTrippedModel?.release()
            gcodeFile.delete()
            roundTripFile.delete()
            val restoreSnapshots = prefs.edit()
            previousSnapshots.forEach { (key, value) ->
                if (value == null) restoreSnapshots.remove(key) else restoreSnapshots.putString(key, value)
            }
            restoreSnapshots.commit()
            SliceBeam.CONFIG = previousConfig
            if (previousConfigFile == null) configFile.delete() else configFile.writeBytes(previousConfigFile)
            TemporaryProjectSettings.clear()
            Prefs.setStoredFilamentSlots(previousSlots)
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuPlacesSelectedObjectAtTappedTargetSurfaceAndSupportsUndoRedo() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-menu-picked-surface-placement-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            val sourceBounds = AtomicReference<List<DoubleArray>>()
            glView.queueEvent {
                model.addObject(model, 0)
                model.translate(1, 38.0, 0.0, 0.0)
                model.setUniformFilament(0, 1)
                model.setUniformFilament(1, 2)
                glView.renderer.setSelectedObject(1)
                sourceBounds.set((0..1).map { index ->
                    val min = Vec3d(); val max = Vec3d()
                    model.getBoundingBoxExact(index, min, max)
                    doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z)
                })
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("surface-placement models should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.ModelMenuPlaceOnSurface)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuPlaceOnSurface))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithTag("model-choice-option-0").assertIsDisplayed().performClick()

            val tappedPoint = AtomicReference<DoubleArray>()
            val tapReady = CountDownLatch(1)
            var targetPixels = 0
            var exactTargetHits = 0
            var largestNormalZ = -2.0
            var sampleHit: String? = null
            glView.queueEvent {
                val renderer = glView.renderer
                val glModelsField = GLRenderer::class.java.getDeclaredField("glModels").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                val glModels = glModelsField.get(renderer) as List<GLModel>
                val targetHits = ArrayList<GLModel.HitResult>()
                val xStep = (renderer.viewportWidth / 64).coerceAtLeast(1)
                val yStep = (renderer.viewportHeight / 64).coerceAtLeast(1)
                loop@ for (y in yStep until renderer.viewportHeight step yStep) {
                    for (x in xStep until renderer.viewportWidth step xStep) {
                        if (renderer.raycastObjectIndex(x.toFloat(), y.toFloat()) != 0) continue
                        targetPixels++
                        targetHits.clear()
                        glModels[0].raycaster.raycast(renderer, targetHits, x.toFloat(), y.toFloat())
                        targetHits.filter { !it.approximate }.forEach { hit ->
                            exactTargetHits++
                            largestNormalZ = maxOf(largestNormalZ, hit.normal.z)
                            if (sampleHit == null) sampleHit = "p=${hit.position}, n=${hit.normal}"
                        }
                        val hit = targetHits.firstOrNull { !it.approximate && it.normal.z >= 0.7 } ?: continue
                        if (!renderer.onClick(x.toFloat(), y.toFloat())) continue
                        tappedPoint.set(doubleArrayOf(hit.position.x, hit.position.y, hit.position.z))
                        break@loop
                    }
                }
                tapReady.countDown()
            }
            assertTrue("surface tap search should complete on GL", tapReady.await(20, TimeUnit.SECONDS))
            val point = requireNotNull(tappedPoint.get()) {
                "test camera should expose a precise upward target; targetPixels=$targetPixels, " +
                    "exactHits=$exactTargetHits, largestNormalZ=$largestNormalZ, sample=$sampleHit"
            }
            compose.waitUntil(timeoutMillis = 15_000) { readObjectCountOnGl(glView) == 1 }

            val combinedBounds = readExactBoundsOnGl(glView, 0).single()
            val originals = requireNotNull(sourceBounds.get())
            val target = originals[0]
            val moving = originals[1]
            val dx = point[0] - (moving[0] + moving[3]) / 2.0
            val dy = point[1] - (moving[1] + moving[4]) / 2.0
            val dz = point[2] - moving[2]
            val expectedBounds = doubleArrayOf(
                minOf(target[0], moving[0] + dx),
                minOf(target[1], moving[1] + dy),
                minOf(target[2], moving[2] + dz),
                maxOf(target[3], moving[3] + dx),
                maxOf(target[4], moving[4] + dy),
                maxOf(target[5], moving[5] + dz),
            )
            expectedBounds.forEachIndexed { index, expected ->
                assertEquals("combined bounds must reflect the actual surface hit on axis $index",
                    expected, combinedBounds[index], 0.04)
            }
            assertEquals("the grouped result should retain both material assignments", listOf(1, 2),
                readUsedFilamentsOnGl(glView, 0).toList())

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 2 }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            assertEquals(listOf(1, 2), readUsedFilamentsOnGl(glView, 0).toList())
            val redonePlacementBounds = readExactBoundsOnGl(glView, 0).single()
            expectedBounds.forEachIndexed { index, expected ->
                assertEquals("Redo must restore surface placement geometry on axis $index",
                    expected, redonePlacementBounds[index], 0.04)
            }
        } finally {
            if (glView.renderer.isSurfacePlacementActive) bed.onBackPressed()
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuPlacesSelectedGroupOnTappedSurfaceAndPreservesSpacingAndMaterials() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-menu-group-surface-placement-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val sourceBounds = AtomicReference<List<DoubleArray>>()
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.addObject(model, 0)
                model.translate(1, 45.0, 12.0, 7.0)
                model.translate(2, 70.0, 28.0, 16.0)
                model.setUniformFilament(0, 1)
                model.setUniformFilament(1, 2)
                model.setUniformFilament(2, 3)
                glView.renderer.setSelectedObjects(intArrayOf(1, 2))
                sourceBounds.set((0..2).map { index ->
                    val min = Vec3d(); val max = Vec3d()
                    model.getBoundingBoxExact(index, min, max)
                    doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z)
                })
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("the target and moving group should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.ModelMenuPlaceOnSurface)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuPlaceOnSurface))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithTag("model-choice-option-0").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { glView.renderer.isSurfacePlacementActive }

            val tappedPoint = AtomicReference<DoubleArray>()
            val tapReady = CountDownLatch(1)
            var exactTargetHits = 0
            glView.queueEvent {
                val renderer = glView.renderer
                val glModelsField = GLRenderer::class.java.getDeclaredField("glModels").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                val glModels = glModelsField.get(renderer) as List<GLModel>
                val targetHits = ArrayList<GLModel.HitResult>()
                val xStep = (renderer.viewportWidth / 64).coerceAtLeast(1)
                val yStep = (renderer.viewportHeight / 64).coerceAtLeast(1)
                loop@ for (y in yStep until renderer.viewportHeight step yStep) {
                    for (x in xStep until renderer.viewportWidth step xStep) {
                        if (renderer.raycastObjectIndex(x.toFloat(), y.toFloat()) != 0) continue
                        targetHits.clear()
                        glModels[0].raycaster.raycast(renderer, targetHits, x.toFloat(), y.toFloat())
                        val hit = targetHits.firstOrNull { !it.approximate && it.normal.z >= 0.7 } ?: continue
                        exactTargetHits++
                        if (!renderer.onClick(x.toFloat(), y.toFloat())) continue
                        tappedPoint.set(doubleArrayOf(hit.position.x, hit.position.y, hit.position.z))
                        break@loop
                    }
                }
                tapReady.countDown()
            }
            assertTrue("group surface-pick search should complete on GL", tapReady.await(20, TimeUnit.SECONDS))
            val point = requireNotNull(tappedPoint.get()) {
                "the group should be placeable on an exposed upward target face; exactHits=$exactTargetHits"
            }
            compose.waitUntil(timeoutMillis = 15_000) { readObjectCountOnGl(glView) == 1 }

            val original = requireNotNull(sourceBounds.get())
            val groupMinX = minOf(original[1][0], original[2][0])
            val groupMinY = minOf(original[1][1], original[2][1])
            val groupMinZ = minOf(original[1][2], original[2][2])
            val groupMaxX = maxOf(original[1][3], original[2][3])
            val groupMaxY = maxOf(original[1][4], original[2][4])
            val groupMaxZ = maxOf(original[1][5], original[2][5])
            val dx = point[0] - (groupMinX + groupMaxX) / 2.0
            val dy = point[1] - (groupMinY + groupMaxY) / 2.0
            val dz = point[2] - groupMinZ
            val expectedBounds = doubleArrayOf(
                minOf(original[0][0], groupMinX + dx),
                minOf(original[0][1], groupMinY + dy),
                minOf(original[0][2], groupMinZ + dz),
                maxOf(original[0][3], groupMaxX + dx),
                maxOf(original[0][4], groupMaxY + dy),
                maxOf(original[0][5], groupMaxZ + dz),
            )
            expectedBounds.forEachIndexed { axis, expected ->
                assertEquals("group surface placement should match the picked point on axis $axis",
                    expected, readExactBoundsOnGl(glView, 0).single()[axis], 0.04)
            }
            assertEquals("target and moving group should keep their separate material assignments", listOf(1, 2, 3),
                readUsedFilamentsOnGl(glView, 0).toList())

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 3 }
            assertEquals("Undo should restore each source object's material assignment", listOf(1, 2, 3),
                (0..2).map { readUsedFilamentsOnGl(glView, it).single() })
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            assertEquals(listOf(1, 2, 3), readUsedFilamentsOnGl(glView, 0).toList())
            expectedBounds.forEachIndexed { axis, expected ->
                assertEquals("Redo should preserve the picked group placement on axis $axis",
                    expected, readExactBoundsOnGl(glView, 0).single()[axis], 0.04)
            }
        } finally {
            if (glView.renderer.isSurfacePlacementActive) bed.onBackPressed()
            modelFile.delete()
        }
    }

    @Test
    fun androidBackCancelsSurfacePlacementWithoutChangingEitherObject() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-menu-surface-placement-back-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.translate(1, 38.0, 0.0, 0.0)
                model.setUniformFilament(0, 1)
                model.setUniformFilament(1, 2)
                glView.renderer.setSelectedObject(1)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("models should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val beforeBounds = readExactBoundsOnGl(glView, 0, 1)
            val beforeFilaments = (0..1).map { readUsedFilamentsOnGl(glView, it).toList() }

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.ModelMenuPlaceOnSurface)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuPlaceOnSurface))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithTag("model-choice-option-0").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { glView.renderer.isSurfacePlacementActive }

            assertTrue("Android Back should cancel the waiting surface-pick operation", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 10_000) { !glView.renderer.isSurfacePlacementActive }
            assertEquals("cancellation must leave both input objects in place", 2, readObjectCountOnGl(glView))
            val afterBounds = readExactBoundsOnGl(glView, 0, 1)
            beforeBounds.zip(afterBounds).forEachIndexed { objectIndex, (before, after) ->
                before.indices.forEach { axis ->
                    assertEquals("object $objectIndex bounds must survive Back on axis $axis",
                        before[axis], after[axis], 0.001)
                }
            }
            assertEquals("cancellation must preserve both objects' material assignments", beforeFilaments,
                (0..1).map { readUsedFilamentsOnGl(glView, it).toList() })
        } finally {
            if (glView.renderer.isSurfacePlacementActive) bed.onBackPressed()
            modelFile.delete()
        }
    }

    @Test
    fun selectedGroupCanBeDuplicatedWithItsFilamentsAndDroppedTogetherOnBed() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-menu-group-duplicate-drop-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            val bedCenter = AtomicReference<DoubleArray>()
            val bedMinZ = AtomicReference<Double>()
            glView.queueEvent {
                val renderer = glView.renderer
                val activeBed = requireNotNull(renderer.bed)
                val min = requireNotNull(activeBed.getVolumeMin())
                val max = requireNotNull(activeBed.getVolumeMax())
                bedCenter.set(doubleArrayOf((min.x + max.x) / 2.0, (min.y + max.y) / 2.0))
                bedMinZ.set(min.z)
                val widthBounds = Vec3d(); val widthMax = Vec3d()
                model.getBoundingBoxExact(0, widthBounds, widthMax)
                val width = widthMax.x - widthBounds.x
                model.translate(0, 35.0, 20.0, 8.0)
                model.addObject(model, 0)
                model.translate(1, width * 1.5, 7.0, 18.0)
                model.setUniformFilament(0, 2)
                model.setUniformFilament(1, 3)
                renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("the selected group should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.ModelMenuDuplicate)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuDuplicateShort))
                .assertIsDisplayed().assertIsEnabled().performClick()
            val countField = compose.onNodeWithTag("duplicate-model-count")
            countField.performTextClearance()
            countField.performTextInput("1")
            compose.onNodeWithTag("duplicate-model-confirm").performClick()
            compose.waitUntil(timeoutMillis = 15_000) { readObjectCountOnGl(glView) == 4 }
            val duplicatedFilaments = AtomicReference<List<IntArray>>()
            val duplicateReady = CountDownLatch(1)
            glView.queueEvent {
                val current = requireNotNull(glView.renderer.model)
                duplicatedFilaments.set(listOf(current.getUsedFilaments(2), current.getUsedFilaments(3)))
                duplicateReady.countDown()
            }
            assertTrue("duplicated group filaments should be readable", duplicateReady.await(10, TimeUnit.SECONDS))
            assertArrayEquals("the first group copy keeps its source filament", intArrayOf(2),
                requireNotNull(duplicatedFilaments.get())[0])
            assertArrayEquals("the second group copy keeps its source filament", intArrayOf(3),
                requireNotNull(duplicatedFilaments.get())[1])
            assertArrayEquals("group selection survives duplication", intArrayOf(0, 1), readSelectedIndicesOnGl(glView))

            val beforeCenter = readExactBoundsOnGl(glView, 0, 1, 2, 3)
            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.ModelMenuCenterOnBed)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuCenterOnBedShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            instrumentationWaitForGlIdle()
            val centeredBounds = readExactBoundsOnGl(glView, 0, 1, 2, 3)
            val expectedCenter = requireNotNull(bedCenter.get())
            assertEquals("the selected group's combined X bounds are centered on the bed", expectedCenter[0],
                (centeredBounds[0][0] + centeredBounds[1][3]) / 2.0, 0.03)
            assertEquals("the selected group's combined Y bounds are centered on the bed", expectedCenter[1],
                (centeredBounds[0][1] + centeredBounds[1][4]) / 2.0, 0.03)
            assertEquals("group centering preserves internal X spacing",
                (beforeCenter[1][0] + beforeCenter[1][3] - beforeCenter[0][0] - beforeCenter[0][3]) / 2.0,
                (centeredBounds[1][0] + centeredBounds[1][3] - centeredBounds[0][0] - centeredBounds[0][3]) / 2.0,
                0.03)

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.ModelMenuPlaceGroupOnBed)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuPlaceGroupOnBedShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            instrumentationWaitForGlIdle()
            val droppedBounds = readExactBoundsOnGl(glView, 0, 1, 2, 3)
            assertEquals("the lowest selected object rests on the printable bed", requireNotNull(bedMinZ.get()),
                minOf(droppedBounds[0][2], droppedBounds[1][2]), 0.03)
            assertEquals("dropping preserves the selected group's relative height",
                centeredBounds[1][2] - centeredBounds[0][2], droppedBounds[1][2] - droppedBounds[0][2], 0.03)
            assertArrayEquals("unselected group copies stay untouched", centeredBounds[2], droppedBounds[2], 0.03)
            assertArrayEquals("the second unselected group copy stays untouched", centeredBounds[3], droppedBounds[3], 0.03)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun objectListGroupCanBeCombinedAsPartsFromTheModelMenu() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-menu-group-combine-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val renderer = glView.renderer
                val model = requireNotNull(renderer.model)
                model.addObject(model, 0)
                model.addObject(model, 0)
                model.setUniformFilament(0, 1)
                model.setUniformFilament(1, 2)
                model.setUniformFilament(2, 3)
                renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("multipart test objects should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.WorkspaceObjects)
            compose.onNodeWithContentDescription(activity.getString(R.string.WorkspaceObjects))
                .assertIsDisplayed().performClick()
            compose.onNodeWithTag("model-objects-group-selection-toggle").performClick()
            compose.onNodeWithTag("model-object-row-1").performClick()
            compose.onNodeWithTag("model-object-row-2").performClick()
            compose.onNodeWithTag("model-objects-selection-confirm").performClick()
            compose.waitUntil(timeoutMillis = 10_000) { glView.renderer.selectedObjectsCount == 3 }

            openModelMenu(activity)
            selectAllObjectsScope()
            revealMenuAction(activity, R.string.ModelMenuCombineSelected)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuCombineSelected))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithTag("workspace-message-confirm").assertIsDisplayed().performClick()

            compose.waitUntil(timeoutMillis = 15_000) { bed.currentModel?.objectsCount == 1 }
            val combinedFilaments = AtomicReference<IntArray>()
            val combinedReady = CountDownLatch(1)
            glView.queueEvent {
                combinedFilaments.set(requireNotNull(glView.renderer.model).getUsedFilaments(0))
                combinedReady.countDown()
            }
            assertTrue("combined part filaments should be read on the GL thread", combinedReady.await(10, TimeUnit.SECONDS))
            assertArrayEquals("combining the selected group preserves all three source materials",
                intArrayOf(1, 2, 3), requireNotNull(combinedFilaments.get()))

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 3 }
            assertEquals("Undo should restore the three separate source objects", 3,
                readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            assertArrayEquals("Redo should restore all multipart filament assignments", intArrayOf(1, 2, 3),
                readUsedFilamentsOnGl(glView, 0))

            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.MenuToolbarSplit)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarSplit))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 15_000) { readObjectCountOnGl(glView) == 3 }
            val splitAssignments = (0 until 3).map { index -> readUsedFilamentsOnGl(glView, index).single() }
            assertEquals("Splitting a combined assembly must restore each source filament", listOf(1, 2, 3),
                splitAssignments)
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            assertArrayEquals("Undo split must restore the multipart assembly", intArrayOf(1, 2, 3),
                readUsedFilamentsOnGl(glView, 0))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 3 }
            assertEquals("Redo split must restore all three independent source bodies", listOf(1, 2, 3),
                (0 until 3).map { index -> readUsedFilamentsOnGl(glView, index).single() })
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun initialProjectRouteShowsFileActionsOnItsFirstOpen() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val controller = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)
        assertEquals(WorkspaceDestination.PROJECT.ordinal,
            controller.javaClass.getMethod("getSelectedDestinationSlot").invoke(controller))
        assertFalse(controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) as Boolean)

        val menuMap = BedFragment::class.java.getDeclaredField("menuMap")
            .apply { isAccessible = true }.get(bed) as android.util.SparseArray<*>
        val projectMenu = menuMap.get(WorkspaceDestination.PROJECT.ordinal)
        assertTrue(projectMenu is FileMenu)
        val boundFragment = ListBedMenu::class.java.getDeclaredField("fragment")
            .apply { isAccessible = true }.get(projectMenu)
        assertSame("the initially selected Project route must receive its BedFragment", bed, boundFragment)

        val compactPortrait = usesCompactPortraitLayout(activity)
        if (compactPortrait) {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuProject))
                .assertIsDisplayed()
                .performClick()
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(activity.getString(R.string.WorkspaceAddModelAction))
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithContentDescription(activity.getString(R.string.MenuProject)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(R.string.WorkspaceAddModelAction)).assertIsDisplayed()
        if (compactPortrait) revealMenuAction(activity, R.string.MenuFileExport3mf)
        compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileExport3mf)).assertIsDisplayed()
        assertEquals("only compact portrait expands Project actions as a separate panel",
            compactPortrait,
            controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) as Boolean)
    }

    @Test
    fun projectMenuAddModelBuildsMultiSelectOpenDocumentRequest() {
        val activity = compose.activity
        val requestCode = MainActivity.REQUEST_CODE_OPEN_FILE
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }

        try {
            openWorkspaceMenu(activity, WorkspaceDestination.PROJECT, R.string.MenuProject)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.WorkspaceAddModelAction))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.WorkspaceAddModelAction))
                .assertIsDisplayed().performClick()

            val request = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, request.action)
            assertEquals("*/*", request.type)
            assertTrue(request.categories?.contains(Intent.CATEGORY_OPENABLE) == true)
            assertTrue("the project route must allow selecting multiple models",
                request.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
        } finally {
            launchers[requestCode] = previousLauncher
        }
    }

    @Test
    fun projectMenuImportsSelectedStlThroughTheActualResultHandler() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val inputFile = File(activity.cacheDir, "menu-import-${System.nanoTime()}.stl")
        val cachedImport = File(SliceBeam.getModelCacheDir(), inputFile.name)
        val requestCode = MainActivity.REQUEST_CODE_OPEN_FILE
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                inputFile.outputStream().use(source::copyTo)
            }
            assertEquals(0, bed.currentModel?.objectsCount ?: 0)

            openWorkspaceMenu(activity, WorkspaceDestination.PROJECT, R.string.MenuProject)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.WorkspaceAddModelAction))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.WorkspaceAddModelAction))
                .assertIsDisplayed().performClick()
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, requireNotNull(capturedIntent.get()).action)

            val inputUri = FileProvider.getUriForFile(activity, authority, inputFile)
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_OK, Intent().setData(inputUri))
            }
            compose.waitUntil(timeoutMillis = 60_000) {
                bed.currentModel?.objectsCount == 1
            }
            assertEquals("the selected STL should become the active native model", 1,
                requireNotNull(bed.currentModel).objectsCount)
            assertTrue("the actual result handler should copy the selected file into the model cache",
                cachedImport.isFile && cachedImport.length() > 0L)
        } finally {
            launchers[requestCode] = previousLauncher
            inputFile.delete()
            cachedImport.delete()
        }
    }

    @Test
    fun stepImportFromFileUriFallsBackToDocumentPickerAndWaitsForWorkspaceHost() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        // Simulate providers that expose a generated display name and no STEP MIME type.
        val fileName = "menu-step-import-${System.nanoTime()}-document"
        val inputFile = File(activity.cacheDir, fileName)
        val cachedImport = File(SliceBeam.getModelCacheDir(), "$fileName.step")
        val requestCode = MainActivity.REQUEST_CODE_OPEN_FILE
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        val originalOverlay = bed.overlayLayout
        try {
            InstrumentationRegistry.getInstrumentation().context.assets
                .open("step_cube_ccby4.step").use { source ->
                    inputFile.outputStream().use(source::copyTo)
                }
            assertEquals(0, bed.currentModel?.objectsCount ?: 0)

            openWorkspaceMenu(activity, WorkspaceDestination.PROJECT, R.string.MenuProject)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.WorkspaceAddModelAction))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.WorkspaceAddModelAction))
                .assertIsDisplayed().performClick()
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, requireNotNull(capturedIntent.get()).action)

            // A file manager may send a shared-storage file:// URL, which targetSdk 36 cannot read
            // without a SAF grant. The app must request a content:// document instead of failing.
            capturedIntent.set(null)
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod("loadFile", Uri::class.java)
                    .apply { isAccessible = true }
                    .invoke(activity, Uri.parse("file:///sdcard/Download/step-file-manager-test.step"))
            }
            compose.waitUntil(timeoutMillis = 5_000) { capturedIntent.get() != null }
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, requireNotNull(capturedIntent.get()).action)
            assertEquals("*/*", requireNotNull(capturedIntent.get()).type)
            assertEquals("The inaccessible file:// URL must not start import",
                0, bed.currentModel?.objectsCount ?: 0)

            val inputUri = FileProvider.getUriForFile(activity, authority, inputFile)
            activity.runOnUiThread {
                // Simulate the STEP file finishing its cache copy before the workspace overlay attaches.
                bed.overlayLayout = null
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, Activity.RESULT_OK, Intent().setData(inputUri))
                activity.window.decorView.postDelayed({ bed.overlayLayout = originalOverlay }, 350)
            }

            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithTag("step-import-confirm").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals("STEP geometry must wait for the user's meshing choice", 0,
                bed.currentModel?.objectsCount ?: 0)
            compose.onNodeWithTag("step-linear-deflection").assertIsDisplayed()
            compose.onNodeWithTag("step-angular-deflection").assertIsDisplayed()
            compose.onNodeWithTag("step-import-confirm").performClick()

            compose.waitUntil(timeoutMillis = 90_000) {
                val model = bed.currentModel
                model != null && model.objectsCount == 1 && model.getTriangleCount(0) > 0
            }
            assertTrue("the chosen STEP file should be cached for the native import",
                cachedImport.isFile && cachedImport.length() > 0L)
            assertTrue("the loaded model must contain renderable triangles",
                requireNotNull(bed.currentModel).getTriangleCount(0) > 0)
        } finally {
            activity.runOnUiThread { bed.overlayLayout = originalOverlay }
            launchers[requestCode] = previousLauncher
            inputFile.delete()
            cachedImport.delete()
        }
    }

    @Test
    fun projectMenuImportsMultipleSelectedStlsSequentially() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val firstFile = File(activity.cacheDir, "menu-multi-import-${System.nanoTime()}-a.stl")
        val secondFile = File(activity.cacheDir, "menu-multi-import-${System.nanoTime()}-b.stl")
        val cachedFiles = listOf(
            File(SliceBeam.getModelCacheDir(), firstFile.name),
            File(SliceBeam.getModelCacheDir(), secondFile.name),
        )
        val requestCode = MainActivity.REQUEST_CODE_OPEN_FILE
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        try {
            listOf(firstFile, secondFile).forEach { file ->
                activity.assets.open("models/xyz_cube.stl").use { source ->
                    file.outputStream().use(source::copyTo)
                }
            }
            assertEquals(0, bed.currentModel?.objectsCount ?: 0)

            openWorkspaceMenu(activity, WorkspaceDestination.PROJECT, R.string.MenuProject)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.WorkspaceAddModelAction))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.WorkspaceAddModelAction))
                .assertIsDisplayed().performClick()
            assertTrue(requireNotNull(capturedIntent.get()).getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))

            val firstUri = FileProvider.getUriForFile(activity, authority, firstFile)
            val secondUri = FileProvider.getUriForFile(activity, authority, secondFile)
            val clipData = android.content.ClipData.newUri(
                activity.contentResolver,
                "selected STL models",
                firstUri,
            ).apply { addItem(android.content.ClipData.Item(secondUri)) }
            val resultIntent = Intent().apply { setClipData(clipData) }
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_OK, resultIntent)
            }
            compose.waitUntil(timeoutMillis = 60_000) {
                bed.currentModel?.objectsCount == 2
            }
            assertEquals("both selected STLs should be imported in order", 2,
                requireNotNull(bed.currentModel).objectsCount)
            compose.waitUntil(timeoutMillis = 10_000) { cachedFiles.none(File::exists) }
            assertTrue("the sequential importer should remove each temporary cached STL after loading",
                cachedFiles.none(File::exists))
        } finally {
            launchers[requestCode] = previousLauncher
            firstFile.delete()
            secondFile.delete()
            cachedFiles.forEach(File::delete)
        }
    }

    @Test
    fun singlePlateProjectExportWritesReloadableThreeMfForTheActiveModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "single-plate-export-model-${System.nanoTime()}.stl")
        val output = File(activity.cacheDir, "single-plate-export-${System.nanoTime()}.3mf")
        val currentConfigFile = SliceBeam.getCurrentConfigFile()
        val previousCurrentConfig = currentConfigFile.takeIf(File::isFile)?.readBytes()
        val requestCode = MainActivity.REQUEST_CODE_EXPORT_3MF
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        val outputUri = FileProvider.getUriForFile(activity, authority, output)
        var model: Model? = null

        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            model = loadSingleTestModel(bed, modelFile)
            assertEquals(1, requireNotNull(model).objectsCount)

            openWorkspaceMenu(activity, WorkspaceDestination.PROJECT, R.string.MenuProject)
            revealMenuAction(activity, R.string.MenuFileExport3mf)
            val exportAction = compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileExport3mf))
            compose.waitUntil(timeoutMillis = 5_000) {
                try {
                    exportAction.assertIsEnabled()
                    true
                } catch (_: AssertionError) {
                    false
                }
            }
            assertSame(activity, bed.getContext())
            assertSame(bed, activity.navigationDelegate.getCurrentFragment())
            tapSemanticsNodeOnDevice(exportAction)
            try {
                compose.waitUntil(timeoutMillis = 5_000) { capturedIntent.get() != null }
            } catch (error: Throwable) {
                val actionBounds = exportAction.fetchSemanticsNode().boundsInRoot
                val actionWindowBounds = exportAction.fetchSemanticsNode().boundsInWindow
                val panelNode = compose.onNodeWithTag("workspace-menu-panel").fetchSemanticsNode()
                val panelBounds = panelNode.boundsInRoot
                val panelWindowBounds = panelNode.boundsInWindow
                val rootBounds = compose.onAllNodes(isRoot()).fetchSemanticsNodes().map { it.boundsInRoot }
                throw AssertionError(
                    "Export click did not launch picker; action=$actionBounds " +
                        "windowAction=$actionWindowBounds panel=$panelBounds windowPanel=$panelWindowBounds " +
                        "root=$rootBounds deviceTap=$lastDeviceTapDiagnostic",
                    error,
                )
            }

            val cancelledRequest = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, cancelledRequest.action)
            assertEquals("application/3mf", cancelledRequest.type)
            assertEquals("OrkanSlicer_project.3mf", cancelledRequest.getStringExtra(Intent.EXTRA_TITLE))
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_CANCELED, Intent())
            }
            assertFalse("cancelling the save picker must not create an export file", output.exists())
            capturedIntent.set(null)

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileExport3mf)).performClick()
            compose.waitUntil(timeoutMillis = 5_000) { capturedIntent.get() != null }
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_OK, Intent().setData(outputUri))
            }
            compose.waitUntil(timeoutMillis = 15_000) { output.isFile && output.length() > 0 }
            java.util.zip.ZipFile(output).use { archive ->
                assertTrue("the output must contain a 3MF model entry", archive.getEntry("3D/3dmodel.model") != null)
            }
            val reloaded = Model(output, 1)
            try {
                assertEquals("exported 3MF must reload the active plate's object", 1, reloaded.objectsCount)
            } finally {
                reloaded.release()
            }
        } finally {
            launchers[requestCode] = previousLauncher
            output.delete()
            modelFile.delete()
            model?.release()
            if (previousCurrentConfig == null) currentConfigFile.delete()
            else currentConfigFile.writeBytes(previousCurrentConfig)
        }
    }

    @Test
    fun sliceMenuManualActionOpensFilamentAssignmentAndCancelOrBackPreservesModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "slice-menu-filament-cancel-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val originalExtruder = model.getExtruder(0)
            assertFalse("test fixture must start without a slice result", bed.hasSliceResult())

            if (!usesCompactPortraitLayout(activity)) {
                // The landscape rail is a vertically scrolling Column (not a LazyColumn);
                // performScrollTo() cannot reliably bring its last child into view.
                compose.onNodeWithTag("workspace-navigation-rail")
                    .performTouchInput { swipeUp() }
            }
            openWorkspaceMenu(activity, WorkspaceDestination.SLICE, R.string.MenuSlice)
            waitForWorkspaceMenuGrid()
            revealMenuAction(activity, R.string.QuickSliceTitle)
            compose.onNodeWithText(activity.getString(R.string.QuickSliceTitle))
                .assertIsDisplayed().assertIsEnabled()
            revealMenuAction(activity, R.string.SmartSliceTitle)
            compose.onNodeWithText(activity.getString(R.string.SmartSliceTitle))
                .assertIsDisplayed().assertIsEnabled()
            revealMenuAction(activity, R.string.MenuSliceCurrentSettings)
            compose.waitForIdle()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSliceCurrentSettings))
                .assertIsDisplayed().assertIsEnabled().performClick()
            awaitPreSliceDialog(bed, "pre-slice-title")

            compose.onNodeWithTag("pre-slice-title").assertIsDisplayed()
            compose.onNodeWithTag("pre-slice-cancel").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("pre-slice-title").assertDoesNotExist()
            assertEquals("Cancel must not alter the model's filament tool", originalExtruder, model.getExtruder(0))
            assertFalse("Cancel must not start native slicing", bed.hasSliceResult())

            revealMenuAction(activity, R.string.MenuSliceCurrentSettings)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSliceCurrentSettings))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
            compose.onNodeWithTag("pre-slice-title").assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            compose.onNodeWithTag("pre-slice-title").assertDoesNotExist()
            assertEquals("Back must not alter the model's filament tool", originalExtruder, model.getExtruder(0))
            assertFalse("Back must not start native slicing", bed.hasSliceResult())
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun sliceMenuProducesResultAndExposesSafeResultActions() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "slice-menu-result-actions-test.stl")
        val prefs = requireNotNull(Prefs.getPrefs())
        val oldPerformanceMode = Prefs.isPerformanceModeEnabled()
        val oldGatePrinter = prefs.getString("mmu_choice_printer", null)
        val oldGateChoices = prefs.getString("mmu_choice_gates", null)
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            assertFalse("the fixture must start unsliced", bed.hasSliceResult())

            if (!usesCompactPortraitLayout(activity)) {
                // The workspace rail is a regular scrolling Column in landscape;
                // swipe it to reveal the last destination before invoking slicing.
                compose.onNodeWithTag("workspace-navigation-rail")
                    .performTouchInput { swipeUp() }
            }
            openWorkspaceMenu(activity, WorkspaceDestination.SLICE, R.string.MenuSlice)
            waitForWorkspaceMenuGrid()
            revealMenuAction(activity, R.string.MenuSliceCurrentSettings)
            compose.waitForIdle()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSliceCurrentSettings))
                .assertIsDisplayed().assertIsEnabled().performClick()
            awaitPreSliceDialog(bed, "pre-slice-title")
            compose.onNodeWithTag("pre-slice-title").assertIsDisplayed()
            compose.onNodeWithTag("pre-slice-confirm").performClick()

            // The configured test printer may expose a cached physical-slot/material mismatch.
            val mismatchContinue = compose.onAllNodesWithText(
                activity.getString(R.string.PreSliceMaterialMismatchContinue),
            )
            if (mismatchContinue.fetchSemanticsNodes().isNotEmpty()) {
                mismatchContinue.get(0).assertIsDisplayed().performClick()
            }

            compose.waitUntil(timeoutMillis = 120_000) { bed.hasSliceResult() }
            assertTrue("native slicing must produce a non-empty G-code artifact",
                BedFragment.tempGCodePath.isFile && BedFragment.tempGCodePath.length() > 0L)
            val glView = requireNotNull(bed.glView)
            compose.waitUntil(timeoutMillis = 10_000) { glView.renderer.gcodeResult != null }
            val result = requireNotNull(glView.renderer.gcodeResult)
            compose.waitUntil(timeoutMillis = 10_000) { glView.renderer.viewer != null }
            assertTrue("sliced model should retain at least one assigned filament", model.getUsedFilaments(0).isNotEmpty())
            val viewer = requireNotNull(glView.renderer.viewer)
            val originalViewType = viewer.viewType
            val originalSeamsVisible = readGCodeOptionOnGl(
                glView,
                ru.ytkab0bp.slicebeam.slic3r.GCodeViewer.OPTION_TYPE_SEAMS,
            )

            // Slicing refreshes the same Slice menu from pre-slice actions to result actions.
            scrollMenuToIndex(0)
            compose.waitForIdle()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSliceInfo))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("slice-info-title").assertIsDisplayed()
            compose.onNodeWithTag("slice-info-scheme").assertIsDisplayed()
            compose.onNodeWithTag("slice-info-seams").assertIsDisplayed()
            val alternateScheme = activity.resources.getStringArray(R.array.GCodeColorSchemes).getOrNull(1)
            assertTrue("the result should offer multiple G-code color schemes", !alternateScheme.isNullOrBlank())
            compose.onNodeWithTag("slice-info-scheme").performClick()
            compose.onNodeWithText(requireNotNull(alternateScheme)).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { viewer.viewType != originalViewType }
            assertEquals("choosing a color scheme must update the rendered G-code view", 1, viewer.viewType)
            compose.onNodeWithTag("slice-info-seams").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                readGCodeOptionOnGl(glView, ru.ytkab0bp.slicebeam.slic3r.GCodeViewer.OPTION_TYPE_SEAMS) != originalSeamsVisible
            }
            compose.onNodeWithTag("slice-info-close").performClick()
            compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag("slice-info-title").fetchSemanticsNodes().isEmpty()
            }
            compose.waitForIdle()

            revealMenuAction(activity, R.string.MenuSliceLayers)
            val layersAction = compose.onNodeWithContentDescription(activity.getString(R.string.MenuSliceLayers))
                .assertIsDisplayed()
            tapSemanticsNodeOnDevice(layersAction)
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceToolPanelAttached("slice-layer-preview") }
            compose.onNodeWithTag("slice-layers-panel").assertIsDisplayed()
            compose.onNodeWithTag("workspace-docked-tool-panel").assertIsDisplayed()
            val rendererBounds = compose.onNodeWithTag("workspace-renderer-area").fetchSemanticsNode().boundsInRoot
            val controlsBounds = compose.onNodeWithTag("workspace-docked-tool-panel").fetchSemanticsNode().boundsInRoot
            assertTrue("The layer controls must not cover the build plate renderer",
                rendererBounds.bottom <= controlsBounds.top + 1f)
            compose.onNodeWithTag("slice-layers-title").assertIsDisplayed()
            compose.onNodeWithTag("slice-layers-range-title").assertIsDisplayed()
            compose.onNodeWithText("25").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { result.previewSelection?.blockSize == 25 }
            assertEquals("layer preview control must update the active G-code range policy", 25,
                result.previewSelection?.blockSize)
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceToolPanelAttached("slice-layer-preview") }

            scrollMenuToIndex(4)
            compose.waitForIdle()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSlicePerformance))
                .performScrollTo().assertIsDisplayed().performClick()
            assertEquals("performance result action must toggle the preference", !oldPerformanceMode,
                Prefs.isPerformanceModeEnabled())
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSlicePerformance))
                .assertIsDisplayed().performClick()
            assertEquals("second toggle must restore the original preference", oldPerformanceMode,
                Prefs.isPerformanceModeEnabled())
            assertTrue("result actions must not replace or release the active G-code result",
                glView.renderer.gcodeResult === result)

            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val chooserMonitor = instrumentation.addMonitor(
                IntentFilter(Intent.ACTION_CHOOSER),
                ActivityResult(Activity.RESULT_CANCELED, Intent()),
                true,
            )
            try {
                scrollMenuToIndex(3)
                compose.waitForIdle()
                compose.onNodeWithContentDescription(activity.getString(R.string.MenuSliceShare))
                    .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
                compose.waitUntil(timeoutMillis = 5_000) { chooserMonitor.hits > 0 }
                assertTrue("Share should route through the Android system chooser", chooserMonitor.hits > 0)
                assertTrue("opening Share must keep the generated G-code available", bed.hasSliceResult() &&
                    BedFragment.tempGCodePath.isFile && BedFragment.tempGCodePath.length() > 0L)
                assertTrue("opening Share must preserve the active rendered result",
                    glView.renderer.gcodeResult === result)
            } finally {
                instrumentation.removeMonitor(chooserMonitor)
            }
        } finally {
            Prefs.setPerformanceModeEnabled(oldPerformanceMode)
            prefs.edit().apply {
                if (oldGatePrinter == null) remove("mmu_choice_printer") else putString("mmu_choice_printer", oldGatePrinter)
                if (oldGateChoices == null) remove("mmu_choice_gates") else putString("mmu_choice_gates", oldGateChoices)
            }.commit()
            modelFile.delete()
        }
    }

    @Test
    fun sliceResultExportUsesDocumentPickerAndWritesTheGeneratedGcode() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "slice-result-export-model-${System.nanoTime()}.stl")
        val output = File(activity.cacheDir, "slice-result-export-${System.nanoTime()}.gcode")
        val requestCode = MainActivity.REQUEST_CODE_EXPORT_GCODE
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        val outputUri = FileProvider.getUriForFile(activity, authority, output)
        val prefs = requireNotNull(Prefs.getPrefs())
        val oldPerformanceMode = Prefs.isPerformanceModeEnabled()
        val oldGatePrinter = prefs.getString("mmu_choice_printer", null)
        val oldGateChoices = prefs.getString("mmu_choice_gates", null)

        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            if (!usesCompactPortraitLayout(activity)) {
                compose.onNodeWithTag("workspace-navigation-rail")
                    .performTouchInput { swipeUp() }
            }
            openWorkspaceMenu(activity, WorkspaceDestination.SLICE, R.string.MenuSlice)
            waitForWorkspaceMenuGrid()
            revealMenuAction(activity, R.string.MenuSliceCurrentSettings)
            compose.waitForIdle()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSliceCurrentSettings))
                .assertIsDisplayed().performClick()
            awaitPreSliceDialog(bed, "pre-slice-confirm")
            compose.onNodeWithTag("pre-slice-confirm").performClick()

            val mismatchContinue = compose.onAllNodesWithText(
                activity.getString(R.string.PreSliceMaterialMismatchContinue),
            )
            if (mismatchContinue.fetchSemanticsNodes().isNotEmpty()) {
                mismatchContinue.get(0).assertIsDisplayed().performClick()
            }
            compose.waitUntil(timeoutMillis = 120_000) {
                bed.hasSliceResult() && BedFragment.tempGCodePath.isFile && BedFragment.tempGCodePath.length() > 0
            }

            revealMenuAction(activity, R.string.MenuSliceExportToFile)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSliceExportToFile))
                .assertIsDisplayed().assertIsEnabled().performClick()
            val request = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, request.action)
            assertEquals("application/x-gcode", request.type)
            assertEquals(requireNotNull(bed.glView).renderer.gcodeResult!!.getRecommendedName(),
                request.getStringExtra(Intent.EXTRA_TITLE))

            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_CANCELED, Intent())
            }
            assertFalse("canceling the G-code save must not create an output file", output.exists())

            capturedIntent.set(null)
            compose.onNodeWithText(activity.getString(R.string.GCodeExportShort))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { capturedIntent.get() != null }
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_OK, Intent().setData(outputUri))
            }
            compose.waitUntil(timeoutMillis = 10_000) { output.isFile && output.length() > 0L }
            assertTrue("export must preserve the generated G-code bytes",
                output.readBytes().contentEquals(BedFragment.tempGCodePath.readBytes()))
        } finally {
            launchers[requestCode] = previousLauncher
            Prefs.setPerformanceModeEnabled(oldPerformanceMode)
            prefs.edit().apply {
                if (oldGatePrinter == null) remove("mmu_choice_printer") else putString("mmu_choice_printer", oldGatePrinter)
                if (oldGateChoices == null) remove("mmu_choice_gates") else putString("mmu_choice_gates", oldGateChoices)
            }.commit()
            output.delete()
            modelFile.delete()
        }
    }

    @Test
    fun multiPlateProjectExportWarnsAndCancelDoesNotOpenDocumentPicker() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-multiplate-export-warning-test.stl")
        val output = File(activity.cacheDir, "menu-multiplate-export-warning-${System.nanoTime()}.3mf")
        val requestCode = MainActivity.REQUEST_CODE_EXPORT_3MF
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            assertEquals(1, bed.plateCount)

            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "1"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuAddPlate))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { bed.plateCount == 2 && bed.currentPlateIndex == 1 }
            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlatePickerSelectDescription, "1"),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { bed.currentPlateIndex == 0 }

            openWorkspaceMenu(activity, WorkspaceDestination.PROJECT, R.string.MenuProject)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag("workspace-menu-panel").fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitForIdle()
            revealMenuAction(activity, R.string.MenuFileExport3mf)
            val exportAction = compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileExport3mf))
            exportAction.assertIsDisplayed().assertIsEnabled()
            tapSemanticsNodeOnDevice(exportAction)
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }

            compose.onNodeWithText(activity.getString(R.string.Project3mfExportActivePlateTitle))
                .assertIsDisplayed()
            // Long translations at large system font sizes must remain scrollable. Assert the
            // bounded scroll viewport is visible and can scroll instead of requiring the full
            // message's tall semantics bounds to fit on screen at once.
            compose.onNodeWithTag("workspace-message-scroll")
                .assertIsDisplayed()
                .performTouchInput { swipeUp() }
            compose.waitForIdle()
            compose.onNodeWithTag("workspace-message-scroll").assertIsDisplayed()
            compose.onNodeWithText(activity.getString(R.string.Project3mfExportActivePlateConfirm))
                .assertIsDisplayed()
            compose.onNodeWithText(activity.getString(android.R.string.cancel))
                .assertIsDisplayed().performClick()
            compose.waitForIdle()

            compose.onNodeWithText(activity.getString(R.string.Project3mfExportActivePlateTitle))
                .assertDoesNotExist()
            assertEquals("cancel must preserve the two-plate workspace", 2, bed.plateCount)
            assertEquals("cancel must preserve the active plate", 0, bed.currentPlateIndex)
            assertEquals("cancel must not mutate the model", 1, bed.currentModel?.objectsCount)
            assertEquals("dismissing the warning must not launch the document picker", null, capturedIntent.get())

            compose.onNodeWithText(activity.getString(R.string.MenuFileExport3mf))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.Project3mfExportActivePlateConfirm))
                .assertIsDisplayed().performClick()

            val request = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, request.action)
            assertEquals("application/3mf", request.type)
            assertEquals("OrkanSlicer_plate_1.3mf", request.getStringExtra(Intent.EXTRA_TITLE))

            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_CANCELED, Intent())
            }
            assertFalse("cancelling the document picker must not create an export file", output.exists())
            assertEquals("the confirmed export is limited to the selected plate", 2, bed.plateCount)
            assertEquals("the confirmed export must not switch the selected plate", 0, bed.currentPlateIndex)
            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "1"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlateCloseDescription,
                    activity.getString(R.string.PlateNumber, "2")),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.plateCount == 1 && bed.currentPlateIndex == 0
            }
            assertEquals("closing the empty second plate must retain the model on the first plate",
                1, bed.currentModel?.objectsCount)
            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "1"))
                .assertIsDisplayed()
        } finally {
            launchers[requestCode] = previousLauncher
            output.delete()
            modelFile.delete()
        }
    }

    @Test
    fun toolsMenuProfileExportActionOpensComposeFlowAndAndroidBackDismissesIt() {
        val activity = compose.activity
        compose.onNodeWithContentDescription(activity.getString(R.string.MenuOptions))
            .assertIsDisplayed()
            .performClick()
        revealMenuAction(activity, R.string.MenuFileExportProfiles)
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription(activity.getString(R.string.MenuFileExportProfiles))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileExportProfiles))
            .assertIsDisplayed()
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(activity.getString(R.string.MenuFileExportProfilesPrints))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(activity.getString(R.string.MenuFileExportProfilesPrints)).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithText(activity.getString(R.string.MenuFileExportProfilesPrints)).assertDoesNotExist()
    }

    @Test
    fun toolsMenuProfileImportBuildsCorrectDocumentPickerRequest() {
        val activity = compose.activity
        val requestCode = MainActivity.REQUEST_CODE_IMPORT_PROFILES
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        try {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuOptions))
                .assertIsDisplayed()
                .performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithContentDescription(activity.getString(R.string.MenuFileImportProfiles))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileImportProfiles))
                .assertIsDisplayed()
                .performClick()
            val request = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, request.action)
            assertEquals("*/*", request.type)
            assertTrue(request.categories?.contains(Intent.CATEGORY_OPENABLE) == true)
            assertFalse(request.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
            assertTrue(request.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.contains("text/plain") == true)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileImportProfiles)).assertIsDisplayed()
            assertFalse("opening the picker must not show an import dialog before a result is selected",
                (activity.navigationDelegate.getCurrentFragment() as BedFragment).isWorkspaceMessageDialogAttached())
        } finally {
            launchers[requestCode] = previousLauncher
        }
    }

    @Test
    fun toolsMenuProfileImportAppliesSelectedProfileAndRestoresFixtureState() {
        val activity = compose.activity
        val requestCode = MainActivity.REQUEST_CODE_IMPORT_PROFILES
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = AtomicReference<Intent?>()
        val importedTitle = "Codex UI Import ${System.nanoTime()}"
        val input = File(activity.cacheDir, "codex-profile-import.ini")
        input.writeText("[print:$importedTitle]\nlayer_height = 0.17\n", StandardCharsets.UTF_8)

        val originalConfig = SliceBeam.requireConfig()
        val configSnapshot = Slic3rConfigWrapper(originalConfig.serialize().byteInputStream(StandardCharsets.UTF_8))
        val configFile = SliceBeam.getConfigFile()
        val persistedConfig = configFile.takeIf { it.isFile }?.readBytes()
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        val inputUri = FileProvider.getUriForFile(activity, authority, input)

        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        try {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuOptions))
                .assertIsDisplayed().performClick()
            fun chooseImportFile() {
                capturedIntent.set(null)
                compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileImportProfiles))
                    .assertIsDisplayed().performClick()
                val pickerIntent = requireNotNull(capturedIntent.get())
                assertEquals(Intent.ACTION_OPEN_DOCUMENT, pickerIntent.action)
                val result = Intent().setData(inputUri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                activity.runOnUiThread {
                    MainActivity::class.java.getDeclaredMethod(
                        "dispatchActivityResult", Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType, Intent::class.java,
                    ).apply { isAccessible = true }
                        .invoke(activity, requestCode, Activity.RESULT_OK, result)
                }
            }

            assertTrue("profile must not exist before selecting the import result",
                SliceBeam.requireConfig().findPrint(importedTitle) == null)
            chooseImportFile()
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText(importedTitle).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.MenuFileExportProfilesPrints))
                .assertIsDisplayed()
            compose.onNodeWithText(importedTitle).assertIsDisplayed()

            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(importedTitle).fetchSemanticsNodes().isEmpty()
            }
            assertTrue("Android Back must cancel profile import without mutation",
                SliceBeam.requireConfig().findPrint(importedTitle) == null)

            chooseImportFile()
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText(importedTitle).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(android.R.string.cancel))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(importedTitle).fetchSemanticsNodes().isEmpty()
            }
            assertTrue("Cancel must discard the selected profile without mutation",
                SliceBeam.requireConfig().findPrint(importedTitle) == null)

            chooseImportFile()
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText(importedTitle).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(android.R.string.ok)).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                SliceBeam.requireConfig().findPrint(importedTitle) != null
            }
            assertEquals("selected profile settings must be imported", "0.17",
                SliceBeam.requireConfig().findPrint(importedTitle)?.get("layer_height"))

            val existingProfileName = "0.16mm Standard"
            val existingProfile = requireNotNull(SliceBeam.requireConfig().findPrint(existingProfileName))
            val existingLayerHeight = existingProfile["layer_height"]
            assertTrue("the selected printer fixture should include the conflict target", existingLayerHeight != null)
            input.writeText("[print:$existingProfileName]\nlayer_height = 0.19\n", StandardCharsets.UTF_8)
            capturedIntent.set(null)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileImportProfiles))
                .assertIsDisplayed().performClick()
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, requireNotNull(capturedIntent.get()).action)

            val conflictResult = Intent().setData(inputUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, Activity.RESULT_OK, conflictResult)
            }
            val existingLabel = activity.getString(R.string.MenuFileImportProfilesExisting, existingProfileName)
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText(existingLabel).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(existingLabel).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(android.R.string.ok)).assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfilesConflictTitle))
                .assertIsDisplayed()
            compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfilesSkipExisting))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuFileImportProfilesConflictTitle))
                    .fetchSemanticsNodes().isEmpty()
            }
            assertEquals("skip existing must keep the stored process value", existingLayerHeight,
                SliceBeam.requireConfig().findPrint(existingProfileName)?.get("layer_height"))

            input.writeText("[print:$existingProfileName]\nlayer_height = 0.19\n", StandardCharsets.UTF_8)
            capturedIntent.set(null)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileImportProfiles))
                .assertIsDisplayed().performClick()
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, requireNotNull(capturedIntent.get()).action)
            val overwriteResult = Intent().setData(inputUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, Activity.RESULT_OK, overwriteResult)
            }
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText(existingLabel).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(android.R.string.ok)).assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfilesConflictTitle))
                .assertIsDisplayed()
            compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfilesOverwrite))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                SliceBeam.requireConfig().findPrint(existingProfileName)?.get("layer_height") == "0.19"
            }
            assertEquals("overwrite must apply only after the explicit overwrite choice", "0.19",
                SliceBeam.requireConfig().findPrint(existingProfileName)?.get("layer_height"))
        } finally {
            launchers[requestCode] = previousLauncher
            SliceBeam.CONFIG = configSnapshot
            if (persistedConfig == null) configFile.delete() else configFile.writeBytes(persistedConfig)
            input.delete()
        }
    }

    @Test
    fun toolsMenuProfileExportWritesSelectedProfilesToTheRequestedDocument() {
        val activity = compose.activity
        val requestCode = MainActivity.REQUEST_CODE_EXPORT_PROFILES
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }

        val previousPrints = MainActivity.EXPORTING_PRINTS
        val previousFilaments = MainActivity.EXPORTING_FILAMENTS
        val previousPrinters = MainActivity.EXPORTING_PRINTERS
        val output = File(activity.cacheDir, "codex-profile-export-${System.nanoTime()}.ini")
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        val outputUri = FileProvider.getUriForFile(activity, authority, output)

        try {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuOptions))
                .assertIsDisplayed().performClick()
            revealMenuAction(activity, R.string.MenuFileExportProfiles)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithContentDescription(activity.getString(R.string.MenuFileExportProfiles))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileExportProfiles)).performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuFileExportProfilesPrints))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            repeat(2) {
                compose.onNodeWithText(activity.getString(R.string.IntroNext)).performClick()
                compose.waitForIdle()
            }
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileExportProfiles))
                .assertIsDisplayed()
            val exportDialogActions = compose.onAllNodesWithText(activity.getString(R.string.MenuFileExportProfiles))
            val exportDialogActionIndex = exportDialogActions.fetchSemanticsNodes().lastIndex
            assertTrue("the profile export dialog should expose its final action", exportDialogActionIndex >= 0)
            exportDialogActions.get(exportDialogActionIndex).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                capturedIntent.get() != null
            }

            val request = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, request.action)
            assertEquals("application/ini", request.type)
            assertEquals("OrcaSlicerMobile_config_bundle.ini", request.getStringExtra(Intent.EXTRA_TITLE))
            assertTrue(MainActivity.EXPORTING_PRINTS.orEmpty().isNotEmpty())
            assertTrue(MainActivity.EXPORTING_FILAMENTS.orEmpty().isNotEmpty())
            assertTrue(MainActivity.EXPORTING_PRINTERS.orEmpty().isNotEmpty())
            val selectedPrintCount = MainActivity.EXPORTING_PRINTS.orEmpty().size
            val selectedFilamentCount = MainActivity.EXPORTING_FILAMENTS.orEmpty().size
            val selectedPrinterCount = MainActivity.EXPORTING_PRINTERS.orEmpty().size

            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_OK, Intent().setData(outputUri))
            }
            compose.waitUntil(timeoutMillis = 10_000) { output.isFile && output.length() > 0 }
            val exported = Slic3rConfigWrapper(output)
            assertEquals("export must contain all selected print profiles",
                selectedPrintCount, exported.printConfigs.size)
            assertEquals("export must contain all selected filament profiles",
                selectedFilamentCount, exported.filamentConfigs.size)
            assertEquals("export must contain all selected printer profiles",
                selectedPrinterCount, exported.printerConfigs.size)
        } finally {
            launchers[requestCode] = previousLauncher
            MainActivity.EXPORTING_PRINTS = previousPrints
            MainActivity.EXPORTING_FILAMENTS = previousFilaments
            MainActivity.EXPORTING_PRINTERS = previousPrinters
            output.delete()
        }
    }

    @Test
    fun cameraMenuActionsUpdateProjectionAndCameraControlPreference() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val initialGlView = requireNotNull(bed.glView)
        val projectionBefore = readProjectionMatrixOnGl(initialGlView)
        val previousProjection = Prefs.isOrthoProjectionEnabled()
        val previousControlMode = Prefs.getCameraControlMode()
        try {
            // Camera options moved from the removed workspace destination into App settings;
            // the view cube remains the persistent way to change standard camera direction.
            activity.runOnUiThread {
                (activity.navigationDelegate as ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate).switchToSlot(5)
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                activity.navigationDelegate.getCurrentFragment() is ru.ytkab0bp.slicebeam.fragment.SettingsFragment &&
                    compose.onAllNodesWithText(activity.getString(R.string.SettingsInterface))
                        .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.MenuCamera))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.SettingsCameraProjection))
                .performScrollTo().assertIsDisplayed()

            compose.onNodeWithText(activity.getString(R.string.SettingsCameraProjection))
                .performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                Prefs.isOrthoProjectionEnabled() != previousProjection
            }
            activity.runOnUiThread {
                (activity.navigationDelegate as ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate)
                    .switchToSlot(0)
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                activity.navigationDelegate.getCurrentFragment() is BedFragment
            }
            val resumedGlView = requireNotNull(
                (activity.navigationDelegate.getCurrentFragment() as BedFragment).glView,
            )
            compose.waitUntil(timeoutMillis = 5_000) {
                val projectionAfter = readProjectionMatrixOnGl(resumedGlView)
                projectionAfter.indices.any { index ->
                    kotlin.math.abs(projectionAfter[index] - projectionBefore[index]) > 1e-9
                }
            }
            assertEquals("App camera projection setting should update the persisted preference",
                !previousProjection, Prefs.isOrthoProjectionEnabled())

            activity.runOnUiThread {
                (activity.navigationDelegate as ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate)
                    .switchToSlot(5)
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                activity.navigationDelegate.getCurrentFragment() is ru.ytkab0bp.slicebeam.fragment.SettingsFragment
            }
            if (compose.onAllNodesWithText(activity.getString(R.string.SettingsCameraProjection))
                    .fetchSemanticsNodes().isEmpty()
            ) {
                compose.onNodeWithText(activity.getString(R.string.MenuCamera))
                    .performScrollTo().assertIsDisplayed().performClick()
            }
            compose.onNodeWithText(activity.getString(R.string.MenuCameraControlMode))
                .performScrollTo().assertIsDisplayed().performClick()
            val targetMode = (previousControlMode + 1) % 3
            val targetLabel = activity.getString(
                when (targetMode) {
                    0 -> R.string.MenuCameraControlModeOne
                    1 -> R.string.MenuCameraControlModeTwo
                    else -> R.string.MenuCameraControlModeThree
                },
            )
            compose.onNodeWithText(targetLabel, substring = true).assertIsDisplayed().performClick()
            compose.waitForIdle()
            assertEquals("App camera control setting should update its preference",
                targetMode, Prefs.getCameraControlMode())
        } finally {
            Prefs.setOrthoProjectionEnabled(previousProjection)
            Prefs.setCameraControlMode(previousControlMode)
            val restoreGlView = (activity.navigationDelegate.getCurrentFragment() as? BedFragment)?.glView
                ?: initialGlView
            restoreGlView.queueEvent {
                restoreGlView.renderer.updateProjection()
                restoreGlView.requestRender()
            }
        }
    }

    @Test
    fun cameraPresetTilesUpdateTheRendererPoseForEveryPreset() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        compose.onNodeWithTag("workspace-view-cube").assertIsDisplayed()

        val glView = requireNotNull(bed.glView)
        val modelBed = glView.renderer.bed!!
        val min = requireNotNull(modelBed.getVolumeMin())
        val max = requireNotNull(modelBed.getVolumeMax())
        val minVector = CameraVector(min.x, min.y, min.z)
        val maxVector = CameraVector(max.x, max.y, max.z)
        val presets = listOf(
            R.string.ViewCubeTop to CameraPreset.TOP,
            R.string.ViewCubeBottom to CameraPreset.BOTTOM,
            R.string.ViewCubeFront to CameraPreset.FRONT,
            R.string.ViewCubeBack to CameraPreset.BACK,
            R.string.ViewCubeLeft to CameraPreset.LEFT,
            R.string.ViewCubeRight to CameraPreset.RIGHT,
        )
        presets.forEach { (faceLabel, preset) ->
            val target = CameraPresetGeometry.resolve(preset, minVector, maxVector)
            performViewCubeFace(faceLabel)
            compose.waitUntil(timeoutMillis = 5_000) {
                val pose = readCameraPoseOnGl(glView)
                kotlin.math.abs(pose[0] - target.origin.x) < 0.1 &&
                    kotlin.math.abs(pose[1] - target.origin.y) < 0.1 &&
                    kotlin.math.abs(pose[2] - target.origin.z) < 0.1 &&
                    kotlin.math.abs(pose[3] - target.position.x) < 0.1 &&
                    kotlin.math.abs(pose[4] - target.position.y) < 0.1 &&
                    kotlin.math.abs(pose[5] - target.position.z) < 0.1
            }
            // Let the renderer's spring animation settle before requesting the next preset.
            SystemClock.sleep(900L)
            compose.waitForIdle()
            val pose = readCameraPoseOnGl(glView)
            assertTrue(
                "${activity.getString(faceLabel)} must apply its configured renderer camera pose",
                kotlin.math.abs(pose[0] - target.origin.x) < 0.1 &&
                    kotlin.math.abs(pose[1] - target.origin.y) < 0.1 &&
                    kotlin.math.abs(pose[2] - target.origin.z) < 0.1 &&
                    kotlin.math.abs(pose[3] - target.position.x) < 0.1 &&
                    kotlin.math.abs(pose[4] - target.position.y) < 0.1 &&
                    kotlin.math.abs(pose[5] - target.position.z) < 0.1,
            )
        }
    }

    @Test
    fun rapidCameraPresetChangesKeepTheLatestSelection() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelBed = requireNotNull(glView.renderer.bed)
        val min = requireNotNull(modelBed.getVolumeMin())
        val max = requireNotNull(modelBed.getVolumeMax())
        val target = CameraPresetGeometry.resolve(
            CameraPreset.RIGHT,
            CameraVector(min.x, min.y, min.z),
            CameraVector(max.x, max.y, max.z),
        )

        compose.onNodeWithTag("workspace-view-cube").assertIsDisplayed()
        performViewCubeFace(R.string.ViewCubeTop)
        performViewCubeFace(R.string.ViewCubeRight)

        compose.waitUntil(timeoutMillis = 5_000) {
            val pose = readCameraPoseOnGl(glView)
            kotlin.math.abs(pose[0] - target.origin.x) < 0.1 &&
                kotlin.math.abs(pose[1] - target.origin.y) < 0.1 &&
                kotlin.math.abs(pose[2] - target.origin.z) < 0.1 &&
                kotlin.math.abs(pose[3] - target.position.x) < 0.1 &&
                kotlin.math.abs(pose[4] - target.position.y) < 0.1 &&
                kotlin.math.abs(pose[5] - target.position.z) < 0.1
        }
        SystemClock.sleep(900L)
        val finalPose = readCameraPoseOnGl(glView)
        assertTrue(
            "a queued earlier preset must not replace the latest camera selection",
            kotlin.math.abs(finalPose[3] - target.position.x) < 0.1 &&
                kotlin.math.abs(finalPose[4] - target.position.y) < 0.1 &&
                kotlin.math.abs(finalPose[5] - target.position.z) < 0.1,
        )
    }

    @Test
    fun viewCubeFaceTapSelectsThatViewAndDragRotatesTheRendererCamera() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelBed = requireNotNull(glView.renderer.bed)
        val min = requireNotNull(modelBed.getVolumeMin())
        val max = requireNotNull(modelBed.getVolumeMax())
        val target = CameraPresetGeometry.resolve(
            CameraPreset.FRONT,
            CameraVector(min.x, min.y, min.z),
            CameraVector(max.x, max.y, max.z),
        )
        val cube = compose.onNodeWithTag("workspace-view-cube").assertIsDisplayed()
        val size = cube.fetchSemanticsNode().size
        val polygons = WorkspaceViewCubeGeometry.project(
            size.width.toFloat(), size.height.toFloat(),
            Math.toRadians(-35.0).toFloat(), Math.toRadians(-25.0).toFloat(),
        )
        val front = requireNotNull(polygons.firstOrNull { it.face == WorkspaceViewFace.FRONT })
        val localX = front.points.map { it.x }.average().toFloat()
        val localY = front.points.map { it.y }.average().toFloat()
        assertEquals(WorkspaceViewFace.FRONT,
            WorkspaceViewCubeGeometry.hitTest(polygons, localX, localY))

        cube.performTouchInput {
            down(Offset(localX, localY))
            up()
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            val pose = readCameraPoseOnGl(glView)
            kotlin.math.abs(pose[3] - target.position.x) < 0.1 &&
                kotlin.math.abs(pose[4] - target.position.y) < 0.1 &&
                kotlin.math.abs(pose[5] - target.position.z) < 0.1
        }
        SystemClock.sleep(900L)
        val beforeDrag = readCameraPoseOnGl(glView)
        cube.performTouchInput {
            val start = Offset(size.width * 0.5f, size.height * 0.5f)
            down(start)
            moveTo(Offset(size.width * 0.82f, size.height * 0.68f), delayMillis = 250)
            up()
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            val pose = readCameraPoseOnGl(glView)
            (3..5).any { kotlin.math.abs(pose[it] - beforeDrag[it]) > 0.1 }
        }
    }

    private fun performViewCubeFace(faceLabelRes: Int) {
        val activity = compose.activity
        val label = activity.getString(
            R.string.ViewCubeShowFace,
            activity.getString(faceLabelRes),
        )
        val action = requireNotNull(
            compose.onNodeWithTag("workspace-view-cube")
                .fetchSemanticsNode().config[SemanticsActions.CustomActions]
                .firstOrNull { it.label == label },
        ) {
                "View cube should expose accessibility action '$label'"
        }
        compose.runOnIdle { action.action() }
    }

    @Test
    fun modelMenuDuplicateActionAppliesRequestedCopyCountToNativeModel() {
        val activity = compose.activity
        val context = activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(context.cacheDir, "menu-duplicate-action-test.stl")
        context.assets.open("models/xyz_cube.stl").use { source ->
            modelFile.outputStream().use(source::copyTo)
        }
        val loadDone = CountDownLatch(1)
        var loaded: Model? = null
        var loadError: Exception? = null
        bed.loadModel(modelFile, false, 1, object : BedFragment.ModelLoadCallback {
            override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                loaded = loadedModel
                loadDone.countDown()
            }

            override fun onFailed(error: Exception) {
                loadError = error
                loadDone.countDown()
            }
        })
        assertTrue("temporary STL should load for the menu action", loadDone.await(60, TimeUnit.SECONDS))
        assertEquals("temporary STL should load without error: $loadError", 1, requireNotNull(loaded).objectsCount)

        val viewport = requireNotNull(bed.glView)
        viewport.selectModel(0, Runnable {})
        instrumentationWaitForGlIdle()
        compose.onNodeWithContentDescription(activity.getString(R.string.MenuModel))
            .assertIsDisplayed().performClick()
        revealMenuAction(activity, R.string.ModelMenuDuplicate)
        compose.onNodeWithText(activity.getString(R.string.ModelMenuDuplicateShort))
            .assertIsDisplayed().assertIsEnabled().performClick()
        val duplicateCount = compose.onNodeWithTag("duplicate-model-count").assertIsDisplayed()
        duplicateCount.performTextClearance()
        duplicateCount.performTextInput("2")
        compose.onNodeWithTag("duplicate-model-confirm").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            requireNotNull(bed.currentModel).objectsCount == 3
        }
        instrumentationWaitForGlIdle()
        assertEquals("two requested copies should be added to the original", 3,
            requireNotNull(bed.currentModel).objectsCount)
        assertFalse("successful duplication should close its dialog", bed.isWorkspaceMessageDialogAttached())
        selectAllObjectsScope()
        revealMenuAction(activity, R.string.ModelMenuSelectAll)
        compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuSelectAll))
            .assertIsDisplayed().assertIsEnabled().performClick()
        val selectedCount = CountDownLatch(1)
        val selectedObjects = AtomicReference<Int>()
        viewport.queueEvent {
            selectedObjects.set(viewport.renderer.selectedObjectsCount)
            selectedCount.countDown()
        }
        assertTrue("GL thread should report the select-all result", selectedCount.await(10, TimeUnit.SECONDS))
        assertEquals("Select All should select all three native models", 3, requireNotNull(selectedObjects.get()))
        modelFile.delete()
    }

    @Test
    fun modelMenuCenterOnBedRunsNativeCenteringForSelectedObject() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-center-on-bed-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})

            // Move only this disposable fixture to a point clearly away from common printer bed
            // centers before using the real menu action. The previous +73/-41 offset happened
            // to land exactly at the Kobra 3 bed center, making the test pass/fail depend on
            // fixture placement instead of the Center on Bed action.
            glView.queueEvent {
                model.translate(0, 40.0, 30.0, 0.0)
                glView.requestRender()
            }
            instrumentationWaitForGlIdle()
            val translationBefore = readTranslationOnGl(glView, 0)
            val activeBed = glView.renderer.bed
            val bedMin = activeBed?.getVolumeMin()
            val bedMax = activeBed?.getVolumeMax()
            val bedCenter = if (bedMin != null && bedMax != null) bedMin.center(bedMax) else null

            openModelMenu(activity)
            revealMenuAction(activity, R.string.ModelMenuCenterOnBed)
            // Keep the lazy grid's scroll target deterministic at large text scales where one
            // action can occupy nearly the entire visible page.
            scrollMenuToIndex(3)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.ModelMenuCenterOnBedShort))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val centerAction = compose.onNodeWithText(activity.getString(R.string.ModelMenuCenterOnBedShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled()
            centerAction.performClick()

            val centered = runCatching {
                compose.waitUntil(timeoutMillis = 10_000) {
                    val after = readTranslationOnGl(glView, 0)
                    kotlin.math.abs(after.x - translationBefore.x) > 1.0 ||
                        kotlin.math.abs(after.y - translationBefore.y) > 1.0
                }
            }.isSuccess
            val translationAfter = readTranslationOnGl(glView, 0)
            assertTrue(
                "Center on Bed did not update native placement; " +
                    "before=(${translationBefore.x},${translationBefore.y},${translationBefore.z}), " +
                    "after=(${translationAfter.x},${translationAfter.y},${translationAfter.z}), " +
                    "bedValid=${activeBed?.isValid()}, bedCenter=${bedCenter?.let { "(${it.x},${it.y},${it.z})" }}, " +
                    "selected=${readSelectedObjectOnGl(glView)}, " +
                    "selectionCount=${readSelectedObjectsCountOnGl(glView)}, " +
                    "bounds=${readBoundsOnGl(glView, 0)}",
                centered,
            )
            assertTrue(
                "Center on Bed must alter the selected disposable model's native placement",
                kotlin.math.abs(translationAfter.x - translationBefore.x) > 1.0 ||
                    kotlin.math.abs(translationAfter.y - translationBefore.y) > 1.0,
            )
            assertEquals("centering must not add or remove models", 1, readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuFlattenToggleEntersAndLeavesRendererModeWithoutEditingModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-flatten-toggle-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            assertEquals("the disposable model must be selected before centering", 0,
                readSelectedObjectOnGl(glView))
            instrumentationWaitForGlIdle()
            val triangleCount = readTriangleCountOnGl(glView, 0)

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(6)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.ModelMenuFlattenShort))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val flatten = compose.onNodeWithText(activity.getString(R.string.ModelMenuFlattenShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled()
            flatten.performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                readRendererBooleanOnGl(glView, "isInFlattenMode")
            }
            assertTrue("Flatten action should activate the renderer's flatten interaction mode",
                readRendererBooleanOnGl(glView, "isInFlattenMode"))
            assertEquals("entering Flatten mode must not edit the mesh", triangleCount,
                readTriangleCountOnGl(glView, 0))

            compose.onNodeWithText(activity.getString(R.string.ModelMenuFlattenShort))
                .performScrollTo().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                !readRendererBooleanOnGl(glView, "isInFlattenMode")
            }
            assertFalse("second Flatten action should leave the renderer interaction mode",
                readRendererBooleanOnGl(glView, "isInFlattenMode"))
            assertEquals("leaving Flatten mode must preserve the mesh", triangleCount,
                readTriangleCountOnGl(glView, 0))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuAutoOrientActionsRunForAllObjectsAndSelectedObject() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-auto-orient-test.stl")
        try {
            activity.assets.open("models/pyramid.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            val duplicateDone = CountDownLatch(1)
            glView.duplicateSelectedModel(1, Runnable { duplicateDone.countDown() })
            assertTrue("second temporary object should finish duplicating", duplicateDone.await(15, TimeUnit.SECONDS))
            instrumentationWaitForGlIdle()
            assertEquals(2, readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.updateModel() }

            openModelMenu(activity)
            selectAllObjectsScope()
            val beforeOrientAll = model.geometryRevision
            revealMenuAction(activity, R.string.ModelMenuOrientAll)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuOrientAllShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { model.geometryRevision >= beforeOrientAll + 2 }
            assertEquals("Orient All should process both workspace objects", beforeOrientAll + 2,
                model.geometryRevision)

            selectSingleObjectScope()
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            scrollMenuToIndex(5)
            revealMenuAction(activity, R.string.ModelMenuAutoOrientSelected)
            val beforeOrientSelected = model.geometryRevision
            val beforeSelectedOrientBounds = readBoundsOnGl(glView, 0)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuAutoOrientSelected))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { model.geometryRevision > beforeOrientSelected }
            assertEquals("Auto Orient Selected should process only the selected object", beforeOrientSelected + 1,
                model.geometryRevision)
            assertEquals("orientation actions must preserve the object count", 2, readObjectCountOnGl(glView))
            val afterSelectedOrientBounds = readBoundsOnGl(glView, 0)
            compose.runOnUiThread { bed.undoModelEdit() }
            assertEquals("Undo after Auto Orient Selected must restore the previous bounds",
                beforeSelectedOrientBounds, readBoundsOnGl(glView, 0))
            compose.runOnUiThread { bed.redoModelEdit() }
            assertEquals("Redo after Auto Orient Selected must restore the oriented bounds",
                afterSelectedOrientBounds, readBoundsOnGl(glView, 0))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuArrangeBringsOffBedObjectBackIntoWorkspace() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-arrange-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            glView.queueEvent {
                model.translate(0, 2_000.0, 2_000.0, 0.0)
                glView.requestRender()
            }
            instrumentationWaitForGlIdle()
            val before = readTranslationOnGl(glView, 0)
            assertTrue("test object should begin clearly outside a typical build volume",
                before.x > 1_000.0 && before.y > 1_000.0)
            compose.runOnUiThread { bed.updateModel() }

            openModelMenu(activity)
            selectAllObjectsScope()
            compose.onNodeWithText(activity.getString(R.string.ModelMenuArrangeShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 15_000) {
                val after = readTranslationOnGl(glView, 0)
                kotlin.math.abs(after.x - before.x) > 10.0 || kotlin.math.abs(after.y - before.y) > 10.0
            }
            val after = readTranslationOnGl(glView, 0)
            assertTrue("Arrange should change the off-bed XY placement",
                kotlin.math.abs(after.x - before.x) > 10.0 || kotlin.math.abs(after.y - before.y) > 10.0)
            assertEquals("Arrange must preserve the single temporary object", 1, readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.undoModelEdit() }
            val afterUndo = readTranslationOnGl(glView, 0)
            assertEquals("Undo after Arrange must restore the exact pre-arrange X", before.x, afterUndo.x, 0.01)
            assertEquals("Undo after Arrange must restore the exact pre-arrange Y", before.y, afterUndo.y, 0.01)
            compose.runOnUiThread { bed.redoModelEdit() }
            val afterRedo = readTranslationOnGl(glView, 0)
            assertEquals("Redo after Arrange must restore the arranged X", after.x, afterRedo.x, 0.01)
            assertEquals("Redo after Arrange must restore the arranged Y", after.y, afterRedo.y, 0.01)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuFillBedCreatesCopiesWithinRendererObjectLimit() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-fill-bed-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            scrollMenuToIndex(4)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuFillBedShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

            compose.waitUntil(timeoutMillis = 30_000) { readObjectCountOnGl(glView) > 1 }
            val resultingCount = readObjectCountOnGl(glView)
            assertTrue("Fill Bed should add at least one copy of the disposable test model", resultingCount > 1)
            assertTrue("Fill Bed must respect the renderer's hard object cap", resultingCount <= 256)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuMoveToPlateTransfersSelectedObjectBetweenWorkspacePlates() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-move-to-plate-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            assertEquals(1, bed.plateCount)

            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "1"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuAddPlate))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.plateCount == 2 && bed.currentPlateIndex == 1
            }
            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlatePickerSelectDescription, "1"),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.currentPlateIndex == 0 && readObjectCountOnGl(glView) == 1
            }
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            revealMenuAction(activity, R.string.ModelMenuMoveToPlate)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuMoveToPlateShort))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithText(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 15_000) {
                bed.currentPlateIndex == 1 && readObjectCountOnGl(glView) == 1
            }
            assertEquals("moving the selected object should keep the workspace's two plates", 2, bed.plateCount)

            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlatePickerSelectDescription, "1"),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.currentPlateIndex == 0 && readObjectCountOnGl(glView) == 0
            }
            assertEquals("the moved object must no longer exist on the source plate", 0,
                readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuDeleteAllCancelAndBackPreserveTheLoadedModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-delete-all-cancel-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            assertEquals(1, readObjectCountOnGl(glView))
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuModel))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.ModelMenuObjectsShort))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            selectAllObjectsScope()
            revealMenuAction(activity, R.string.ModelMenuDeleteAll)

            fun assertDeleteAllConfirmationShown() {
                compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
                compose.waitUntil(timeoutMillis = 5_000) {
                    compose.onAllNodesWithText(activity.getString(R.string.ModelDeleteAllConfirm))
                        .fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithText(activity.getString(R.string.ModelDeleteAllConfirm)).assertIsDisplayed()
            }

            val deleteAll = compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteAll))
                .assertIsDisplayed().assertIsEnabled()
            deleteAll.performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                bed.isWorkspaceMessageDialogAttached() || readObjectCountOnGl(glView) == 0
            }
            assertTrue(
                "Delete all should open a confirmation without mutating the model; " +
                    "objects=${readObjectCountOnGl(glView)}, " +
                    "selectedScope=${compose.onNodeWithTag("model-menu-scope-all").fetchSemanticsNode().config}",
                bed.isWorkspaceMessageDialogAttached(),
            )
            assertDeleteAllConfirmationShown()
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).assertIsDisplayed().performClick()
            compose.waitForIdle()
            assertFalse("cancel must dismiss the workspace confirmation", bed.isWorkspaceMessageDialogAttached())
            assertEquals("cancel must not delete the loaded model", 1, readObjectCountOnGl(glView))

            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteAll)).performClick()
            assertDeleteAllConfirmationShown()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            assertFalse("Back must dismiss the workspace confirmation", bed.isWorkspaceMessageDialogAttached())
            assertEquals("Back must not delete the loaded model", 1, readObjectCountOnGl(glView))

            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteAll)).performClick()
            assertDeleteAllConfirmationShown()
            compose.onNode(
                hasText(activity.getString(R.string.ModelMenuDeleteAll)) and hasClickAction(),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
            val modelRemoved = CountDownLatch(1)
            val rendererIsEmpty = AtomicReference<Boolean>()
            glView.queueEvent {
                rendererIsEmpty.set(glView.renderer.model == null)
                modelRemoved.countDown()
            }
            assertTrue("GL thread should confirm delete-all completion", modelRemoved.await(10, TimeUnit.SECONDS))
            assertTrue("confirm should remove the model from the native renderer", requireNotNull(rendererIsEmpty.get()))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuDeleteSelectedCancelBackAndConfirmUseTheNativeSelection() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-delete-selected-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            assertEquals("the disposable selected model should be loaded", 1, readObjectCountOnGl(glView))
            val controller = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            val modelSlot = WorkspaceDestination.MODEL.ordinal
            val shouldNavigate = controller.javaClass.getMethod("getSelectedDestinationSlot")
                .invoke(controller) != modelSlot
            val compactPortrait = usesCompactPortraitLayout(activity)
            val shouldExpand = compactPortrait && !(controller.javaClass.getMethod("getToolPanelExpanded")
                .invoke(controller) as Boolean)
            if (shouldNavigate || shouldExpand) {
                compose.onNodeWithContentDescription(activity.getString(R.string.MenuModel))
                    .assertIsDisplayed().performClick()
            }
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.ModelMenuObjectsShort))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            scrollMenuToIndex(7)
            val deleteSelected = compose.onNodeWithContentDescription(
                activity.getString(R.string.ModelMenuDeleteSelected),
            ).performScrollTo().assertIsDisplayed().assertIsEnabled()

            deleteSelected.performClick()
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteConfirm)).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).performClick()
            compose.waitForIdle()
            assertFalse("cancel should dismiss the selected-model confirmation", bed.isWorkspaceMessageDialogAttached())
            assertEquals("cancel should preserve the selected model", 1, readObjectCountOnGl(glView))

            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteSelected))
                .performScrollTo().performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteConfirm)).assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            assertFalse("Back should dismiss the selected-model confirmation", bed.isWorkspaceMessageDialogAttached())
            assertEquals("Back should preserve the selected model", 1, readObjectCountOnGl(glView))

            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteSelected)).performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteConfirm)).assertIsDisplayed()
            compose.onNode(
                hasText(activity.getString(R.string.ModelMenuDeleteSelected)) and hasClickAction(),
            ).performClick()
            compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
            val removed = CountDownLatch(1)
            val rendererIsEmpty = AtomicReference<Boolean>()
            glView.queueEvent {
                rendererIsEmpty.set(glView.renderer.model == null)
                removed.countDown()
            }
            assertTrue("GL thread should confirm selected-model deletion", removed.await(10, TimeUnit.SECONDS))
            assertTrue("confirm should delete only the selected model", requireNotNull(rendererIsEmpty.get()))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun editScaleActionCancelsWithoutMutationAndAppliesOnlyAfterConfirmation() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-scale-action-test.stl")
        val previousLinked = Prefs.isScaleLinked()
        val previousMillimeters = Prefs.isScaleInputInMM()
        Prefs.setScaleLinked(false)
        Prefs.setScaleInputInMM(false)
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loadDone = CountDownLatch(1)
            val loaded = AtomicReference<Model?>()
            val loadError = AtomicReference<Exception?>()
            bed.loadModel(modelFile, false, 1, object : BedFragment.ModelLoadCallback {
                override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                    loaded.set(loadedModel)
                    loadDone.countDown()
                }

                override fun onFailed(error: Exception) {
                    loadError.set(error)
                    loadDone.countDown()
                }
            })
            assertTrue("temporary scale model should finish loading", loadDone.await(60, TimeUnit.SECONDS))
            val model = requireNotNull(loaded.get()) { "temporary scale model failed: ${loadError.get()}" }
            assertEquals(1, model.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(12)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformScale))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription("X, X =", substring = true).assertIsDisplayed()
            val original = readScaleOnGl(glView, 0)

            compose.onNodeWithContentDescription("X, X =", substring = true).performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.ScaleManualPercent))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val cancelledScaleField = compose.onNodeWithText(activity.getString(R.string.ScaleManualPercent))
            cancelledScaleField.performTextClearance()
            cancelledScaleField.performTextInput("150")
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).performClick()
            compose.waitForIdle()
            val afterCancel = readScaleOnGl(glView, 0)
            assertEquals("cancelling the manual editor must preserve X", original.x, afterCancel.x, 0.0001)
            assertEquals("cancelling the manual editor must preserve Y", original.y, afterCancel.y, 0.0001)
            assertEquals("cancelling the manual editor must preserve Z", original.z, afterCancel.z, 0.0001)

            compose.onNodeWithContentDescription("X, X =", substring = true).performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.ScaleManualPercent))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val confirmedScaleField = compose.onNodeWithText(activity.getString(R.string.ScaleManualPercent))
            confirmedScaleField.performTextClearance()
            confirmedScaleField.performTextInput("125")
            compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
            val afterApply = readScaleOnGl(glView, 0)
            assertEquals("confirming X=125% should scale only X", original.x * 1.25, afterApply.x, 0.002)
            assertEquals("unlinked X scaling should preserve Y", original.y, afterApply.y, 0.002)
            assertEquals("unlinked X scaling should preserve Z", original.z, afterApply.z, 0.002)

            compose.runOnUiThread { bed.undoModelEdit() }
            val afterUndo = readScaleOnGl(glView, 0)
            assertEquals("Undo should restore the pre-scale model", original.x, afterUndo.x, 0.002)
            compose.runOnUiThread { bed.redoModelEdit() }
            val afterRedo = readScaleOnGl(glView, 0)
            assertEquals("Redo should restore the confirmed scale", original.x * 1.25, afterRedo.x, 0.002)

            val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            assertTrue("workspace host should consume Back while the scale tool is active", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 5_000) {
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null
            }
            assertEquals("Android Back should clear the active scale tool", null,
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController))
            assertEquals("closing the scale tool should preserve the Model destination",
                WorkspaceDestination.MODEL.ordinal,
                workspaceController.javaClass.getMethod("getSelectedDestinationSlot").invoke(workspaceController))
            assertEquals("closing the tool must keep the committed scale", original.x * 1.25,
                readScaleOnGl(glView, 0).x, 0.002)

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(9)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelContextResetScale))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitForIdle()
            val afterReset = readScaleOnGl(glView, 0)
            assertEquals("Reset Scale should restore the original native X scale", original.x,
                afterReset.x, 0.002)
            compose.runOnUiThread { bed.undoModelEdit() }
            assertEquals("Undo should restore the explicitly confirmed scale after reset", original.x * 1.25,
                readScaleOnGl(glView, 0).x, 0.002)
            compose.runOnUiThread { bed.redoModelEdit() }
            assertEquals("Redo should restore the reset scale", original.x,
                readScaleOnGl(glView, 0).x, 0.002)
        } finally {
            Prefs.setScaleLinked(previousLinked)
            Prefs.setScaleInputInMM(previousMillimeters)
            modelFile.delete()
        }
    }

    @Test
    fun mirrorPanelReflectsSelectedObjectOnEachAxisAndRecordsUndoRedo() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-mirror-action-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            assertEquals(1, model.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val initialBounds = readExactBoundsOnGl(glView, 0).single()
            fun center(bounds: DoubleArray, axis: Int) = (bounds[axis] + bounds[axis + 3]) / 2.0
            val initialCenter = (0..2).map { center(initialBounds, it) }

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(12)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformScale))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("scale-mirror-x").assertIsDisplayed()
            compose.onNodeWithTag("scale-mirror-y").assertIsDisplayed()
            compose.onNodeWithTag("scale-mirror-z").assertIsDisplayed()

            val undoHistory = BedFragment::class.java.getDeclaredField("undoHistory")
                .apply { isAccessible = true }.get(bed) as java.util.ArrayDeque<*>
            val initialUndoSize = undoHistory.size
            compose.onNodeWithTag("scale-mirror-x").performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                hasExpectedLinearTransform(glView, 0, doubleArrayOf(-1.0, 1.0, 1.0)) &&
                    undoHistory.size > initialUndoSize
            }
            val afterXBounds = readExactBoundsOnGl(glView, 0).single()
            (0..2).forEach { axis -> assertEquals("X reflection keeps world center", initialCenter[axis], center(afterXBounds, axis), 0.01) }

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                hasExpectedLinearTransform(glView, 0, doubleArrayOf(1.0, 1.0, 1.0))
            }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                hasExpectedLinearTransform(glView, 0, doubleArrayOf(-1.0, 1.0, 1.0))
            }

            compose.onNodeWithTag("scale-mirror-y").performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                hasExpectedLinearTransform(glView, 0, doubleArrayOf(-1.0, -1.0, 1.0))
            }
            compose.onNodeWithTag("scale-mirror-z").performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                hasExpectedLinearTransform(glView, 0, doubleArrayOf(-1.0, -1.0, -1.0))
            }
            assertTrue("mirrored object must remain renderable", readTriangleCountOnGl(glView, 0) > 0)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun editPositionActionIsUndoableAndRedoableAfterConfirmation() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-position-action-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val original = readTranslationOnGl(glView, 0)

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(10)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarMove))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText("X =", substring = true).assertIsDisplayed().performClick()
            val positionField = compose.onNodeWithText(activity.getString(R.string.OrientationManualValue))
            positionField.assertIsDisplayed()
            positionField.performTextClearance()
            positionField.performTextInput("12.5")
            compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
            val moved = readTranslationOnGl(glView, 0)
            assertEquals("confirming position X should move the native object", 12.5, moved.x, 0.01)
            assertEquals("position editing should preserve Y", original.y, moved.y, 0.01)

            compose.runOnUiThread { bed.undoModelEdit() }
            val afterUndo = readTranslationOnGl(glView, 0)
            assertEquals("Undo should restore the original X position", original.x, afterUndo.x, 0.01)
            assertEquals("Undo should restore the original Y position", original.y, afterUndo.y, 0.01)
            compose.runOnUiThread { bed.redoModelEdit() }
            assertEquals("Redo should restore the confirmed X position", 12.5,
                readTranslationOnGl(glView, 0).x, 0.01)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun editPositionZIsVisibleCancelableAndUndoable() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-position-z-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val original = readTranslationOnGl(glView, 0)

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(10)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarMove))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText("Z =", substring = true).assertIsDisplayed().performClick()
            val positionField = compose.onNodeWithText(activity.getString(R.string.OrientationManualValue))
            positionField.assertIsDisplayed()
            positionField.performTextClearance()
            positionField.performTextInput("18.75")
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).performClick()
            assertEquals("Cancelling the Z editor must preserve native Z", original.z,
                readTranslationOnGl(glView, 0).z, 0.01)

            compose.onNodeWithText("Z =", substring = true).assertIsDisplayed().performClick()
            positionField.assertIsDisplayed()
            positionField.performTextClearance()
            positionField.performTextInput("18.75")
            compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
            val moved = readTranslationOnGl(glView, 0)
            assertEquals("Confirming position Z should move the native object", 18.75, moved.z, 0.01)
            assertEquals("Z editing must preserve X", original.x, moved.x, 0.01)
            assertEquals("Z editing must preserve Y", original.y, moved.y, 0.01)

            compose.runOnUiThread { bed.undoModelEdit() }
            assertEquals("Undo should restore the original Z position", original.z,
                readTranslationOnGl(glView, 0).z, 0.01)
            compose.runOnUiThread { bed.redoModelEdit() }
            assertEquals("Redo should restore the confirmed Z position", 18.75,
                readTranslationOnGl(glView, 0).z, 0.01)

            compose.onNodeWithContentDescription("Z")
                .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(24f) }
            val rulerMoved = readTranslationOnGl(glView, 0)
            assertEquals("The Z ruler must preview and commit a Z translation", 24.0, rulerMoved.z, 0.01)
            assertEquals("The Z ruler must not change X", original.x, rulerMoved.x, 0.01)
            assertEquals("The Z ruler must not change Y", original.y, rulerMoved.y, 0.01)
            compose.runOnUiThread { bed.undoModelEdit() }
            assertEquals("Undo should restore the previous Z after ruler movement", 18.75,
                readTranslationOnGl(glView, 0).z, 0.01)
            compose.runOnUiThread { bed.redoModelEdit() }
            assertEquals("Redo should restore the ruler-selected Z", 24.0,
                readTranslationOnGl(glView, 0).z, 0.01)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun positionEditorMovesEverySelectedObjectTogetherOnXYZAndKeepsUndoRedo() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "menu-position-selected-group-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val nativeModel = requireNotNull(glView.renderer.model)
                val min = Vec3d()
                val max = Vec3d()
                nativeModel.getBoundingBoxExact(0, min, max)
                nativeModel.addObject(nativeModel, 0)
                nativeModel.translate(1, (max.x - min.x) * 2.0, 0.0, 0.0)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("two selected objects should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val before = (0..1).map { readTranslationOnGl(glView, it) }

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(10)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarMove))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

            fun enterAxisValue(axis: String, value: Double, confirm: Boolean) {
                compose.onNodeWithText("$axis =", substring = true).assertIsDisplayed().performClick()
                val field = compose.onNodeWithText(activity.getString(R.string.OrientationManualValue))
                field.assertIsDisplayed().performTextClearance()
                field.performTextInput(value.toString())
                compose.onNodeWithText(activity.getString(if (confirm) android.R.string.ok else android.R.string.cancel))
                    .assertIsDisplayed().performClick()
                compose.waitForIdle()
            }

            enterAxisValue("X", before[0].x + 11.0, confirm = false)
            assertEquals("cancelling the active-object position field must preserve the group",
                before[0].x, readTranslationOnGl(glView, 0).x, 0.01)
            assertEquals("cancelling must not move the second selected object",
                before[1].x, readTranslationOnGl(glView, 1).x, 0.01)

            enterAxisValue("X", before[0].x + 11.0, confirm = true)
            enterAxisValue("Y", before[0].y + 13.0, confirm = true)
            enterAxisValue("Z", before[0].z + 17.0, confirm = true)
            val after = (0..1).map { readTranslationOnGl(glView, it) }
            for (index in 0..1) {
                assertEquals("X entry should apply the same group delta to object $index",
                    before[index].x + 11.0, after[index].x, 0.02)
                assertEquals("Y entry should apply the same group delta to object $index",
                    before[index].y + 13.0, after[index].y, 0.02)
                assertEquals("Z entry should apply the same group delta to object $index",
                    before[index].z + 17.0, after[index].z, 0.02)
            }
            assertEquals("group move must preserve X spacing", before[1].x - before[0].x,
                after[1].x - after[0].x, 0.02)

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                (0..1).all { kotlin.math.abs(readTranslationOnGl(glView, it).z - before[it].z) < 0.02 }
            }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                (0..1).all { kotlin.math.abs(readTranslationOnGl(glView, it).z - after[it].z) < 0.02 }
            }
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun editMeasureMenuActionOpensComposeModeAndAndroidBackClosesIt() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)

        assertFalse("measurement mode should start disabled", glView.renderer.isInMeasureMode)
        openEditMenu(activity)
        revealMenuAction(activity, R.string.MenuToolbarMeasure)
        compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarMeasure))
            .assertIsDisplayed()
            .performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == "measure-mode" &&
                compose.onAllNodesWithText(activity.getString(R.string.MeasureTitle))
                    .fetchSemanticsNodes().isNotEmpty() && glView.renderer.isInMeasureMode
        }
        compose.onNodeWithText(activity.getString(R.string.MeasureDistance)).assertIsDisplayed()
        assertSame("opening a GL tool must retain the workspace renderer view", glView, bed.glView)

        activity.onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(timeoutMillis = 5_000) {
            workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null &&
                !glView.renderer.isInMeasureMode
        }
            assertEquals("Back should leave the user in the Edit destination", WorkspaceDestination.EDIT.ordinal,
            workspaceController.javaClass.getMethod("getSelectedDestinationSlot").invoke(workspaceController))
        assertSame("closing the tool must retain GLView", glView, bed.glView)
        compose.onNodeWithText(activity.getString(R.string.MeasureTitle)).assertDoesNotExist()
    }

    @Test
    fun editRotationActionIsUndoableAndResetChangesNativeModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-rotation-action-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            assertEquals("test object should be selected before rotation", 0, readSelectedObjectOnGl(glView))

            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.MenuToolbarRotate)
            val rotateAction = compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarRotate))
            if (!usesCompactPortraitLayout(activity)) rotateAction.performScrollTo()
            val rotateNode = rotateAction.fetchSemanticsNode()
            val rotateVisibility = runCatching { rotateAction.assertIsDisplayed() }.exceptionOrNull()
            assertTrue(
                "Rotate action should be displayed after paging; " +
                    "bounds=${rotateNode.boundsInWindow}, root=${rotateNode.boundsInRoot}, " +
                    "panel=${compose.onNodeWithTag("workspace-menu-panel").fetchSemanticsNode().boundsInWindow}, " +
                    "grid=${compose.onNodeWithTag("workspace-menu-actions-grid").fetchSemanticsNode().boundsInWindow}, " +
                    "semantics=${rotateNode.config}, visibility=$rotateVisibility",
                rotateVisibility == null,
            )
            rotateAction.performClick()
            compose.onNodeWithText("X =", substring = true).assertIsDisplayed().performClick()
            val previewField = compose.onNodeWithText(activity.getString(R.string.OrientationManualValue))
            previewField.performTextClearance()
            previewField.performTextInput("45")
            compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
            assertEquals("rotation editing should remain a preview until Apply", 0.0,
                readRotationOnGl(glView, 0).x, 0.002)
            val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            assertTrue("Workspace host should consume Back while the rotation tool is active", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 5_000) {
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null
            }
            assertEquals("Back should discard the uncommitted rotation preview", 0.0,
                readRotationOnGl(glView, 0).x, 0.002)
            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.MenuToolbarRotate)
            val applyRotateAction = compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarRotate))
            if (!usesCompactPortraitLayout(activity)) applyRotateAction.performScrollTo()
            applyRotateAction.assertIsDisplayed().performClick()
            compose.onNodeWithText("X =", substring = true).assertIsDisplayed().performClick()
            val rotationField = compose.onNodeWithText(activity.getString(R.string.OrientationManualValue))
            rotationField.assertIsDisplayed()
            rotationField.performTextClearance()
            rotationField.performTextInput("90")
            compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
            compose.onNodeWithText(activity.getString(R.string.MenuOrientationRotationApply))
                .assertIsDisplayed().performClick()
            val rotated = readRotationOnGl(glView, 0)
            assertEquals("confirmed X rotation should reach the native model", Math.PI / 2, rotated.x, 0.002)

            assertTrue("Back should close the rotation tool before history actions", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 5_000) {
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null
            }
            openEditMenu(activity)
            revealMenuAction(activity, R.string.ModelUndo)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelUndo))
                .assertIsDisplayed().performClick()
            assertEquals("Undo should restore the original X rotation", 0.0,
                readRotationOnGl(glView, 0).x, 0.002)
            revealMenuAction(activity, R.string.ModelRedo)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelRedo))
                .assertIsDisplayed().performClick()
            assertEquals("Redo should restore the confirmed X rotation", Math.PI / 2,
                readRotationOnGl(glView, 0).x, 0.002)

            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            assertEquals("test object should remain selected before reset", 0, readSelectedObjectOnGl(glView))
            assertEquals("history actions should leave no active tool", null,
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController))
            openSelectedObjectMenu(activity)
            val snackbarControllerField = BedFragment::class.java.getDeclaredField("snackbarController")
                .apply { isAccessible = true }
            val snackbarController = snackbarControllerField.get(bed)
            snackbarController.javaClass.getMethod("clear").invoke(snackbarController)
            compose.waitForIdle()
            revealMenuAction(activity, R.string.ModelContextResetRotation)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelContextResetRotation))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitForIdle()
            val snackbarEntries = snackbarController.javaClass.getMethod("getEntries").invoke(snackbarController) as List<*>
            val resetSnackbarShown = snackbarEntries.any { entry ->
                val snackbar = entry?.javaClass?.getMethod("getSnackbar")?.invoke(entry)
                snackbar?.javaClass?.getField("title")?.get(snackbar) ==
                    activity.getString(R.string.ModelContextResetRotationDone)
            }
            assertTrue("Reset Rotation action should execute its completion callback", resetSnackbarShown)
            val afterReset = readRotationOnGl(glView, 0)
            assertEquals("Reset Rotation should clear the native model rotation", 0.0, afterReset.x, 0.002)
            openEditMenu(activity)
            revealMenuAction(activity, R.string.ModelUndo)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelUndo))
                .assertIsDisplayed().performClick()
            assertEquals("Undo should restore rotation after Reset Rotation", Math.PI / 2,
                readRotationOnGl(glView, 0).x, 0.002)
            revealMenuAction(activity, R.string.ModelRedo)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelRedo))
                .assertIsDisplayed().performClick()
            assertEquals("Redo should reapply Reset Rotation", 0.0, readRotationOnGl(glView, 0).x, 0.002)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun rotationEditorRotatesSelectedGroupAsOneAssemblyAndSupportsUndoRedo() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "menu-group-rotation-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val model = requireNotNull(glView.renderer.model)
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(0, min, max)
                val spacing = (max.x - min.x) * 2.5
                model.addObject(model, 0)
                model.translate(1, spacing, 0.0, 0.0)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("selected group should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val before = readExactBoundsOnGl(glView, 0, 1)
            fun center(bounds: DoubleArray, axis: Int) = (bounds[axis] + bounds[axis + 3]) / 2.0
            val beforeDx = center(before[1], 0) - center(before[0], 0)
            val beforeDy = center(before[1], 1) - center(before[0], 1)

            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.MenuToolbarRotate)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarRotate))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText("Z =", substring = true).assertIsDisplayed().performClick()
            val angleField = compose.onNodeWithText(activity.getString(R.string.OrientationManualValue))
            angleField.performTextClearance()
            angleField.performTextInput("90")
            compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
            compose.onNodeWithText(activity.getString(R.string.MenuOrientationRotationApply))
                .assertIsDisplayed().performClick()

            compose.waitUntil(timeoutMillis = 10_000) {
                kotlin.math.abs(readRotationOnGl(glView, 0).z - Math.PI / 2) < 0.01 &&
                    kotlin.math.abs(readRotationOnGl(glView, 1).z - Math.PI / 2) < 0.01
            }
            val after = readExactBoundsOnGl(glView, 0, 1)
            val afterDx = center(after[1], 0) - center(after[0], 0)
            val afterDy = center(after[1], 1) - center(after[0], 1)
            assertEquals("group rotation should rotate the selected layout around its common pivot", -beforeDy, afterDx, 0.1)
            assertEquals("group rotation should retain spacing as the rotated Y offset", beforeDx, afterDy, 0.1)

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                kotlin.math.abs(readRotationOnGl(glView, 0).z) < 0.01 &&
                    kotlin.math.abs(readRotationOnGl(glView, 1).z) < 0.01
            }
            val undone = readExactBoundsOnGl(glView, 0, 1)
            assertEquals("Undo restores the original first-object center", center(before[0], 0), center(undone[0], 0), 0.1)
            assertEquals("Undo restores the original second-object center", center(before[1], 0), center(undone[1], 0), 0.1)
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                kotlin.math.abs(readRotationOnGl(glView, 1).z - Math.PI / 2) < 0.01
            }
            val redone = readExactBoundsOnGl(glView, 0, 1)
            assertEquals("Redo restores the rotated group layout", center(after[1], 1) - center(after[0], 1),
                center(redone[1], 1) - center(redone[0], 1), 0.1)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun scaleEditorScalesSelectedGroupGeometryAndSpacingTogetherWithUndoRedo() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "menu-group-scale-test.stl")
        val previousLinked = Prefs.isScaleLinked()
        val previousMillimeters = Prefs.isScaleInputInMM()
        Prefs.setScaleLinked(false)
        Prefs.setScaleInputInMM(false)
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val model = requireNotNull(glView.renderer.model)
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(0, min, max)
                val spacing = (max.x - min.x) * 2.5
                model.addObject(model, 0)
                model.translate(1, spacing, 0.0, 0.0)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("selected group should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val before = readExactBoundsOnGl(glView, 0, 1)
            fun width(bounds: DoubleArray) = bounds[3] - bounds[0]
            fun centerX(bounds: DoubleArray) = (bounds[0] + bounds[3]) / 2.0
            val originalWidth = width(before[0])
            val originalSpacing = centerX(before[1]) - centerX(before[0])

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(12)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformScale))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription("X, X =", substring = true).assertIsDisplayed().performClick()
            val scaleField = compose.onNodeWithText(activity.getString(R.string.ScaleManualPercent))
            scaleField.performTextClearance()
            scaleField.performTextInput("150")
            compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()

            compose.waitUntil(timeoutMillis = 10_000) {
                kotlin.math.abs(width(readExactBoundsOnGl(glView, 0, 1)[0]) - originalWidth * 1.5) < 0.1
            }
            val after = readExactBoundsOnGl(glView, 0, 1)
            assertEquals("each selected object's geometry should scale by the shared X factor",
                originalWidth * 1.5, width(after[0]), 0.1)
            assertEquals("the second selected object's geometry should scale by the same factor",
                originalWidth * 1.5, width(after[1]), 0.1)
            assertEquals("group spacing should scale about the common pivot",
                originalSpacing * 1.5, centerX(after[1]) - centerX(after[0]), 0.1)

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                kotlin.math.abs(width(readExactBoundsOnGl(glView, 0, 1)[0]) - originalWidth) < 0.1
            }
            val undone = readExactBoundsOnGl(glView, 0, 1)
            assertEquals("Undo restores both selected object sizes", originalWidth, width(undone[1]), 0.1)
            assertEquals("Undo restores their original spacing", originalSpacing,
                centerX(undone[1]) - centerX(undone[0]), 0.1)
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                kotlin.math.abs(width(readExactBoundsOnGl(glView, 0, 1)[1]) - originalWidth * 1.5) < 0.1
            }
        } finally {
            Prefs.setScaleLinked(previousLinked)
            Prefs.setScaleInputInMM(previousMillimeters)
            modelFile.delete()
        }
    }

    @Test
    fun editCutAndVariableLayerActionsOpenAndCancelWithoutChangingModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-cut-action-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(14)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformCut))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.CutApply))
                .performScrollTo().assertIsDisplayed().assertIsEnabled()
            val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            assertEquals("Cut should be the active workspace tool",
                "ru.ytkab0bp.slicebeam.components.bed_menu.TransformCutMenu",
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController))
            assertEquals("opening Cut must not mutate the model", 1, readObjectCountOnGl(glView))
            assertTrue("Cut preview should be visible while the tool is open", readRendererBooleanOnGl(glView, "cutPlaneVisible"))

            assertTrue("Back should cancel and close the Cut tool", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 5_000) {
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null
            }
            assertFalse("Back should hide the uncommitted cut preview", readRendererBooleanOnGl(glView, "cutPlaneVisible"))
            assertEquals("cancelling Cut must preserve the object", 1, readObjectCountOnGl(glView))

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(16)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarVariableLayerHeight))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.VariableLayerTitle)).assertIsDisplayed()
            assertTrue("Variable Layer action should activate its workspace mode", bed.isVariableLayerHeightMode)
            compose.onNodeWithText(activity.getString(R.string.ActionDone)).assertIsDisplayed().performClick()
            compose.waitForIdle()
            assertFalse("Done should close Variable Layer mode", bed.isVariableLayerHeightMode)

            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            openSelectedObjectMenu(activity)
            scrollMenuToIndex(15)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarSplit))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                hasSnackbarTitle(bed, R.string.MenuToolbarSplitNothing)
            }
            assertEquals("Split on a single connected test mesh should preserve its object", 1,
                readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun applyingCutRunsOnGlThreadAndSplitsTheSelectedModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-cut-apply-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            assertEquals(1, model.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(14)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformCut))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.CutApply))
                .performScrollTo().assertIsDisplayed().performClick()

            compose.waitUntil(timeoutMillis = 30_000) {
                readObjectCountOnGl(glView) == 2 &&
                    !readRendererBooleanOnGl(glView, "cutPlaneVisible")
            }
            assertEquals("applying a cut should split the model into two native objects", 2,
                readObjectCountOnGl(glView))
            assertFalse("the cut preview should close after applying",
                readRendererBooleanOnGl(glView, "cutPlaneVisible"))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun cutConnectorTapCreatesDiscardablePreviewWithoutMutatingModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-cut-connector-cancel-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(14)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformCut))
                .performScrollTo().assertIsDisplayed().performClick()
            val cutReady = runCatching {
                compose.waitUntil(timeoutMillis = 10_000) {
                    runCatching {
                        compose.onNodeWithText(activity.getString(R.string.CutApply)).assertIsEnabled()
                    }.isSuccess
                }
            }.isSuccess
            if (!cutReady) {
                val menus = BedFragment::class.java.getDeclaredField("menuMap")
                    .apply { isAccessible = true }.get(bed) as android.util.SparseArray<*>
                val cutMenu = (0 until menus.size()).mapNotNull(menus::valueAt)
                    .firstOrNull { it.javaClass.simpleName == "TransformCutMenu" }
                val cutObject = cutMenu?.javaClass?.getDeclaredField("cutObject")
                    ?.apply { isAccessible = true }?.getInt(cutMenu)
                val ready = cutMenu?.javaClass?.getDeclaredField("cutReady")
                    ?.apply { isAccessible = true }?.get(cutMenu)
                    ?.let { it.javaClass.getMethod("getValue").invoke(it) }
                throw AssertionError(
                    "Cut tool did not become ready; menu=${cutMenu?.javaClass?.name}, " +
                        "cutObject=$cutObject, cutReady=$ready, " +
                        "rendererSelected=${readRendererIntOnGl(glView, "selectedObject")}, " +
                        "modelObjects=${readObjectCountOnGl(glView)}",
                )
            }
            assertTrue("the GL viewport must stay measurable while the action menu is open", glView.height > 0)
            compose.onNodeWithText(activity.getString(R.string.CutAddConnector))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                readRendererBooleanOnGl(glView, "cutConnectorPlacement")
            }

            val connectorPoint = findUncoveredPreciseSurfacePoint(glView, bed)
            tapGlViewAt(glView, connectorPoint.first, connectorPoint.second)
            compose.waitUntil(timeoutMillis = 10_000) {
                readCutConnectorCountOnGl(glView, 0) == 1
            }
            assertEquals("placing a cut connector should only create a preview", 1,
                readObjectCountOnGl(glView))

            assertTrue("Back should close the cut tool", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 10_000) {
                !readRendererBooleanOnGl(glView, "cutPlaneVisible") &&
                    readCutConnectorCountOnGl(glView, 0) == 0
            }
            assertEquals("cancelling the connector preview must preserve the model", 1,
                readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun editSplitActionSeparatesDisconnectedMeshComponentsIntoNativeObjects() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-split-disconnected-mesh-${System.nanoTime()}.stl")
        try {
            writeTwoDisconnectedCubesStl(activity, modelFile)
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals("the importer should keep the compound STL as one selectable model", 1,
                loaded.objectsCount)
            val importedTriangleCount = loaded.getTriangleCount(0)
            assertTrue("both components should contain valid mesh triangles",
                importedTriangleCount > 0 && importedTriangleCount % 2 == 0)
            val trianglesPerComponent = importedTriangleCount / 2

            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            openSelectedObjectMenu(activity)
            scrollMenuToIndex(15)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarSplit))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

            compose.waitUntil(timeoutMillis = 30_000) { readObjectCountOnGl(glView) == 2 }
            assertEquals("native split should create exactly two independent model objects", 2,
                readObjectCountOnGl(glView))
            assertEquals("first split object should preserve its triangles", trianglesPerComponent,
                readTriangleCountOnGl(glView, 0))
            assertEquals("second split object should preserve its triangles", trianglesPerComponent,
                readTriangleCountOnGl(glView, 1))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun editEmbossActionEntersComposeGlModeAndBackExitsWithoutMeshMutation() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-emboss-action-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val trianglesBefore = readTriangleCountOnGl(glView, 0)

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(21)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarEmboss))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.EmbossTitle)).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(R.string.EmbossInstruction)).assertIsDisplayed()

            val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            assertEquals("Emboss should be the active workspace tool", "emboss-mode",
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController))
            assertTrue("Opening the menu action should activate renderer emboss mode",
                readRendererBooleanOnGl(glView, "isInEmbossMode"))
            assertEquals("Entering emboss mode must not mutate the mesh", trianglesBefore,
                readTriangleCountOnGl(glView, 0))

            assertTrue("Android Back should close the active emboss tool", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 5_000) {
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null
            }
            assertFalse("Back must leave renderer emboss mode", readRendererBooleanOnGl(glView, "isInEmbossMode"))
            assertFalse("Back must clear the active emboss tool", bed.isEmbossMode)
            assertEquals("Cancelling the emboss panel must preserve the mesh", trianglesBefore,
                readTriangleCountOnGl(glView, 0))
            assertTrue("Closing the tool must retain the existing GLView", bed.glView === glView)
        } finally {
            if (bed.isEmbossMode) bed.onBackPressed()
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuCanCreateStandaloneTextDirectlyOnTheBuildPlate() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        assertEquals("the test starts with an empty build plate", 0, readObjectCountOnGl(glView))

        openModelMenu(activity)
        selectAllObjectsScope()
        compose.onNodeWithContentDescription(activity.getString(R.string.TextModelCreateSeparate))
            .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

        compose.onNodeWithTag("emboss-standalone-mode").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithText(activity.getString(R.string.TextModelPlacementInstruction)).assertIsDisplayed()
        assertTrue("standalone text placement must activate emboss click handling",
            readRendererBooleanOnGl(glView, "isInEmbossMode"))
        assertTrue("standalone placement must allow bed hits without selecting a model",
            readRendererBooleanOnGl(glView, "isStandaloneTextPlacementMode"))

        val hit = AtomicReference<FloatArray>()
        val hitReady = CountDownLatch(1)
        glView.queueEvent {
            val renderer = glView.renderer
            val raycastBed = GLRenderer::class.java.getDeclaredMethod(
                "raycastBed", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType,
            ).apply { isAccessible = true }
            val xFractions = floatArrayOf(.5f, .35f, .65f, .2f, .8f)
            val yFractions = floatArrayOf(.5f, .35f, .65f, .2f, .8f)
            for (yf in yFractions) {
                for (xf in xFractions) {
                    val x = renderer.viewportWidth * xf
                    val y = renderer.viewportHeight * yf
                    if (raycastBed.invoke(renderer, x, y) != null) {
                        hit.set(floatArrayOf(x, y))
                        break
                    }
                }
                if (hit.get() != null) break
            }
            hitReady.countDown()
        }
        assertTrue("renderer should find a visible point on the empty build plate", hitReady.await(10, TimeUnit.SECONDS))
        val bedPoint = requireNotNull(hit.get())
        val clickHandled = AtomicReference<Boolean>()
        val clickReady = CountDownLatch(1)
        glView.queueEvent {
            clickHandled.set(glView.renderer.onClick(bedPoint[0], bedPoint[1]))
            clickReady.countDown()
        }
        assertTrue("GL click should finish", clickReady.await(10, TimeUnit.SECONDS))
        assertTrue("build-plate click should be consumed by standalone placement", requireNotNull(clickHandled.get()))
        compose.waitUntil(timeoutMillis = 30_000) { readObjectCountOnGl(glView) == 1 }
        assertEquals("clicking the plate should create one independent printable object", 1,
            readObjectCountOnGl(glView))
        assertTrue("generated text must be real printable geometry", readTriangleCountOnGl(glView, 0) > 0)
        assertEquals("created text should become selected", 0, glView.renderer.selectedObject)
        val textTriangleCount = readTriangleCountOnGl(glView, 0)
        val textBounds = readExactBoundsOnGl(glView, 0).single()

        bed.onBackPressed()
        compose.waitForIdle()
        assertFalse("exiting the tool should clear standalone placement mode",
            readRendererBooleanOnGl(glView, "isStandaloneTextPlacementMode"))

        compose.runOnUiThread { bed.undoModelEdit() }
        compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 0 }
        assertEquals("Undo should remove only the generated text object", 0, readObjectCountOnGl(glView))
        compose.runOnUiThread { bed.redoModelEdit() }
        compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
        assertEquals("Redo should restore the printable text mesh", textTriangleCount,
            readTriangleCountOnGl(glView, 0))
        val redoneTextBounds = readExactBoundsOnGl(glView, 0).single()
        textBounds.forEachIndexed { axis, expected ->
            assertEquals("Redo should preserve generated text bounds on axis $axis", expected,
                redoneTextBounds[axis], 0.02)
        }
    }

    @Test
    fun editPaintActionsOpenTheirGlModesAndBackCancelsWithoutMeshMutation() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-paint-actions-test.stl")
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        Prefs.setStoredFilamentSlots(listOf(
            FilamentSlot(0xFFCC3333.toInt(), "UI Paint Base"),
            FilamentSlot(0xFF3366CC.toInt(), "UI Paint Accent"),
        ))
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            assertEquals(1, model.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val trianglesBefore = readTriangleCountOnGl(glView, 0)
            assertFalse("The fixture starts without paint overrides", readModelHasPaintOnGl(glView, 0))

            val paintActions = listOf(
                Triple(R.string.MenuToolbarSupportPaint, GLRenderer.PAINT_MODE_SUPPORT, 17),
                Triple(R.string.MenuToolbarSeamPaint, GLRenderer.PAINT_MODE_SEAM, 18),
                Triple(R.string.MenuToolbarFuzzyPaint, GLRenderer.PAINT_MODE_FUZZY, 19),
                Triple(R.string.MenuToolbarColorPaint, GLRenderer.PAINT_MODE_COLOR, 20),
            )
            paintActions.forEach { (labelRes, expectedMode, index) ->
                openSelectedObjectMenu(activity)
                scrollMenuToIndex(index)
                compose.onNodeWithContentDescription(activity.getString(labelRes))
                    .performScrollTo().assertIsDisplayed().performClick()
                compose.onNodeWithText(activity.getString(R.string.PaintWith)).assertIsDisplayed()
                compose.waitUntil(timeoutMillis = 10_000) {
                    bed.isPaintMode && readRendererBooleanOnGl(glView, "isPaintMode")
                }
                assertEquals("The selected menu action must activate its matching GL paint mode",
                    expectedMode, readRendererIntOnGl(glView, "paintModeKind"))
                assertFalse("Opening or cancelling paint must not add model paint",
                    readModelHasPaintOnGl(glView, 0))

                assertTrue("Android Back should cancel the active paint tool", bed.onBackPressed())
                compose.waitUntil(timeoutMillis = 10_000) { !bed.isPaintMode }
                compose.waitUntil(timeoutMillis = 10_000) {
                    !readRendererBooleanOnGl(glView, "isPaintMode")
                }
                assertFalse("Cancellation should preserve the original unpainted mesh",
                    readModelHasPaintOnGl(glView, 0))
                assertEquals("Cancellation should preserve mesh topology", trianglesBefore,
                    readTriangleCountOnGl(glView, 0))
                assertTrue("Paint tool exit should retain the renderer view", bed.glView === glView)
            }

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(20)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarColorPaint))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.isPaintMode && readRendererBooleanOnGl(glView, "isPaintMode")
            }
            assertTrue("the GL viewport must stay measurable while painting", glView.height > 0)
            compose.onNodeWithText(activity.getString(R.string.PaintBaseFilament)).assertIsDisplayed()
            compose.onAllNodesWithContentDescription(
                activity.getString(R.string.FilamentPaletteSlotDescription, 2, "UI Paint Accent")
            ).get(0).assertIsDisplayed().performClick()
            assertArrayEquals("a base-filament choice remains a draft until confirmed", intArrayOf(1),
                readUsedFilamentsOnGl(glView, 0))
            compose.onNodeWithTag("workspace-mode-panel").performTouchInput { swipeUp() }
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { !bed.isPaintMode }
            instrumentationWaitForGlIdle()
            assertArrayEquals(
                "cancelling a paint session must restore the model's original base filament",
                intArrayOf(1), readUsedFilamentsOnGl(glView, 0),
            )

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(20)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarColorPaint))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.isPaintMode && readRendererBooleanOnGl(glView, "isPaintMode")
            }
            compose.onNodeWithText(activity.getString(R.string.PaintBaseFilament)).assertIsDisplayed()
            compose.onAllNodesWithContentDescription(
                activity.getString(R.string.FilamentPaletteSlotDescription, 2, "UI Paint Accent")
            ).get(0).assertIsDisplayed().performClick()
            assertArrayEquals("choosing a base filament must not mutate the model before Done", intArrayOf(1),
                readUsedFilamentsOnGl(glView, 0))
            compose.onNodeWithTag("workspace-mode-panel").performTouchInput { swipeUp() }
            compose.onNodeWithText(activity.getString(R.string.ActionDone))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { !bed.isPaintMode }
            instrumentationWaitForGlIdle()
            assertArrayEquals("the confirmed color-panel base-filament action should update the native model",
                intArrayOf(2), readUsedFilamentsOnGl(glView, 0))
            assertFalse("changing the base filament should not fabricate facet paint",
                readModelHasPaintOnGl(glView, 0))
            assertEquals("changing the base filament must preserve mesh topology", trianglesBefore,
                readTriangleCountOnGl(glView, 0))
        } finally {
            if (bed.isPaintMode) bed.onBackPressed()
            modelFile.delete()
            Prefs.setStoredFilamentSlots(previousSlots)
        }
    }

    @Test
    fun colorPaintPanelAcceptsRealCanvasTouchAndUndoRestoresFacetAssignments() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-paint-touch-test.stl")
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        Prefs.setStoredFilamentSlots(listOf(
            FilamentSlot(0xFFCC3333.toInt(), "Touch Paint Base"),
            FilamentSlot(0xFF3366CC.toInt(), "Touch Paint Accent"),
        ))
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            assertFalse("paint touch fixture starts without facet assignments", readModelHasPaintOnGl(glView, 0))
            openSelectedObjectMenu(activity)
            scrollMenuToIndex(20)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarColorPaint))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.isPaintMode && readRendererBooleanOnGl(glView, "isPaintMode")
            }
            revealPaintFilamentSwatch(activity, 2, "Touch Paint Accent", occurrence = 1)
                .performClick()

            val point = findUncoveredPreciseSurfacePoint(glView, bed)
            injectCanvasStroke(glView, point.first, point.second)
            val previewTriangles = readPaintPreviewTriangleCountOnGl(glView, 2)
            assertTrue("an injected touch on a precise GL surface hit must create a paint preview; point=$point",
                previewTriangles > 0)
            assertFalse("painting remains a preview until Done", readModelHasPaintOnGl(glView, 0))

            compose.onNodeWithTag("workspace-mode-panel").performTouchInput { swipeUp() }
            compose.onNodeWithText(activity.getString(R.string.ActionDone))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { !bed.isPaintMode }
            compose.waitUntil(timeoutMillis = 10_000) { readModelHasPaintOnGl(glView, 0) }
            val usedAfterPaint = readUsedFilamentsOnGl(glView, 0)
            assertTrue("the actual canvas stroke must assign the selected second filament: ${usedAfterPaint.contentToString()}",
                usedAfterPaint.contains(2))
            assertTrue("the committed color brush must produce painted facets",
                readPaintMaxFilamentOnGl(glView, 0) >= 2)

            openEditMenu(activity)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelUndo))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { !readModelHasPaintOnGl(glView, 0) }
            assertArrayEquals("Undo restores the original base filament", intArrayOf(1), readUsedFilamentsOnGl(glView, 0))
            if (usesCompactPortraitLayout(activity)) {
                compose.onNodeWithTag("workspace-menu-actions-grid").performScrollToIndex(3)
            }
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelRedo))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { readModelHasPaintOnGl(glView, 0) }
            assertTrue("Redo restores the committed painted filament", readUsedFilamentsOnGl(glView, 0).contains(2))
        } finally {
            if (bed.isPaintMode) bed.onBackPressed()
            modelFile.delete()
            Prefs.setStoredFilamentSlots(previousSlots)
        }
    }

    @Test
    fun editSimplifyRequiresConfirmationAndCanBeUndoneAndRedone() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-simplify-action-test.stl")
        try {
            activity.assets.open("models/sphere.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val originalTriangles = readTriangleCountOnGl(glView, 0)
            assertTrue("sphere fixture must be large enough for simplification", originalTriangles > 4)

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(13)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelSimplify))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).assertIsDisplayed().performClick()
            assertEquals("cancelling simplify must preserve the model", originalTriangles,
                readTriangleCountOnGl(glView, 0))

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(13)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelSimplify))
                .performScrollTo().assertIsDisplayed().performClick()
            val startAction = compose.onAllNodesWithText(activity.getString(R.string.ModelSimplifyStart))
            val startNodeIndex = startAction.fetchSemanticsNodes().lastIndex
            assertTrue("simplify confirmation button should be present", startNodeIndex >= 0)
            startAction.get(startNodeIndex).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 30_000) {
                readTriangleCountOnGl(glView, 0) < originalTriangles
            }
            val simplifiedTriangles = readTriangleCountOnGl(glView, 0)
            assertTrue("simplify should produce a smaller native mesh", simplifiedTriangles in 4 until originalTriangles)

            compose.runOnUiThread { bed.undoModelEdit() }
            assertEquals("Undo should restore the imported mesh", originalTriangles,
                readTriangleCountOnGl(requireNotNull(bed.glView), 0))
            compose.runOnUiThread { bed.redoModelEdit() }
            assertEquals("Redo should restore the simplified mesh", simplifiedTriangles,
                readTriangleCountOnGl(requireNotNull(bed.glView), 0))
        } finally {
            modelFile.delete()
        }
    }

    private fun loadSingleTestModel(bed: BedFragment, modelFile: File): Model {
        val testGlView = requireNotNull(bed.glView)
        compose.waitUntil(timeoutMillis = 10_000) {
            testGlView.isAttachedToWindow && testGlView.width > 0 && testGlView.height > 0 &&
                testGlView.renderer.bed != null
        }
        val done = CountDownLatch(1)
        val loaded = AtomicReference<Model?>()
        val error = AtomicReference<Exception?>()
        bed.loadModel(modelFile, false, 1, object : BedFragment.ModelLoadCallback {
            override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                loaded.set(loadedModel)
                done.countDown()
            }

            override fun onFailed(failure: Exception) {
                error.set(failure)
                done.countDown()
            }
        })
        assertTrue("temporary test model should finish loading", done.await(60, TimeUnit.SECONDS))
        return requireNotNull(loaded.get()) { "temporary model failed: ${error.get()}" }
    }

    private fun awaitPreSliceDialog(bed: BedFragment, tag: String) {
        val waitFailure = runCatching {
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }
        }.exceptionOrNull() ?: return
        val controller = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)
        val prepared = BedFragment::class.java.getDeclaredField("preparedFilamentModel")
            .apply { isAccessible = true }.get(bed)
        val visibleTags = listOf("pre-slice-title", "pre-slice-confirm", "workspace-mode-panel")
            .associateWith { name -> compose.onAllNodesWithTag(name).fetchSemanticsNodes().size }
        val descriptions = compose.onAllNodes(
            androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().mapNotNull { node ->
            runCatching { node.config[SemanticsProperties.ContentDescription].joinToString() }.getOrNull()
        }
        throw AssertionError(
            "Pre-slice UI did not appear: target=$tag activePanel=${controller.javaClass
                .getMethod("getActivePanel").invoke(controller)} hasSlice=${bed.hasSliceResult()} " +
                "preparedCurrentModel=${prepared === bed.currentModel} tags=$visibleTags " +
                "descriptions=$descriptions",
            waitFailure,
        )
    }

    private fun awaitWorkspaceDialogTag(bed: BedFragment, tag: String) {
        val waitFailure = runCatching {
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }
        }.exceptionOrNull() ?: return
        val controller = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)
        val activePanel = controller.javaClass.getMethod("getActivePanel").invoke(controller)
        val activeContent = controller.javaClass.getMethod("getActivePanelContent").invoke(controller)
        val tags = listOf("slice-info-title", "slice-layers-title", "slice-layers-range-title")
            .associateWith { name -> compose.onAllNodesWithTag(name).fetchSemanticsNodes().size }
        val descriptions = compose.onAllNodes(
            androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().mapNotNull { node ->
            runCatching { node.config[SemanticsProperties.ContentDescription].joinToString() }.getOrNull()
        }
        throw AssertionError(
            "Workspace dialog '$tag' did not appear: activePanel=$activePanel " +
                "hasContent=${activeContent != null} tags=$tags descriptions=$descriptions",
            waitFailure,
        )
    }

    private fun waitForWorkspaceMenuGrid() {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("workspace-menu-actions-grid").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Creates a standard binary STL containing two closed copies separated by 30 mm on X. */
    private fun writeTwoDisconnectedCubesStl(activity: MainActivity, destination: File) {
        val source = activity.assets.open("models/xyz_cube.stl").use { it.readBytes() }
        assertTrue("bundled STL must have a binary STL header", source.size >= 84)
        val triangleCount = ByteBuffer.wrap(source, 80, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertTrue("bundled STL triangle count must match its binary payload", triangleCount > 0 &&
            source.size == 84 + triangleCount * 50)

        val output = ByteBuffer.allocate(84 + triangleCount * 2 * 50).order(ByteOrder.LITTLE_ENDIAN)
        output.put(source, 0, 80)
        output.putInt(triangleCount * 2)
        output.put(source, 84, triangleCount * 50)
        repeat(triangleCount) { triangleIndex ->
            val triangleOffset = 84 + triangleIndex * 50
            val translated = source.copyOfRange(triangleOffset, triangleOffset + 50)
            val record = ByteBuffer.wrap(translated).order(ByteOrder.LITTLE_ENDIAN)
            listOf(12, 24, 36).forEach { vertexXOffset ->
                record.putFloat(vertexXOffset, record.getFloat(vertexXOffset) + 30f)
            }
            output.put(translated)
        }
        destination.writeBytes(output.array())
    }

    private fun scrollMenuToIndex(index: Int) {
        // Compact portrait menus use a two-row LazyHorizontalGrid. Wide and landscape
        // layouts use a vertically scrollable action column, where individual actions
        // are brought into view with performScrollTo() at the call site.
        if (usesCompactPortraitLayout(compose.activity)) {
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
        }
    }

    /** Uses the visible horizontal action paging, matching how users reach off-screen menu tiles. */
    private fun revealMenuAction(activity: MainActivity, titleRes: Int) {
        val title = activity.getString(titleRes)
        if (!usesCompactPortraitLayout(activity)) {
            compose.onNodeWithContentDescription(title).performScrollTo().assertIsDisplayed()
            return
        }
        val tileTag = "workspace-menu-action-$titleRes"
        repeat(24) {
            val action = compose.onAllNodesWithContentDescription(title)
                .fetchSemanticsNodes().firstOrNull()
            val tile = compose.onAllNodesWithTag(tileTag).fetchSemanticsNodes().firstOrNull()
            val panel = compose.onAllNodesWithTag("workspace-menu-panel")
                .fetchSemanticsNodes().firstOrNull()
            val pagingAffordanceVisible = compose.onAllNodesWithContentDescription(
                activity.getString(R.string.WorkspaceMoreTitle),
            ).fetchSemanticsNodes().isNotEmpty()
            val visiblePanelRight = panel?.boundsInWindow?.right?.minus(
                if (pagingAffordanceVisible) 52f * activity.resources.displayMetrics.density else 0f,
            ) ?: Float.NEGATIVE_INFINITY
            val geometryLooksVisible = action != null && tile != null && panel != null &&
                tile.boundsInWindow.left >= panel.boundsInWindow.left &&
                tile.boundsInWindow.top >= panel.boundsInWindow.top &&
                tile.boundsInWindow.width >= 48f * activity.resources.displayMetrics.density &&
                tile.boundsInWindow.height >= 48f * activity.resources.displayMetrics.density &&
                // Reserve space only while the paging affordance is actually present. When a
                // menu fits on one page, the final action may use the panel's right-hand area.
                tile.boundsInWindow.right <= visiblePanelRight &&
                tile.boundsInWindow.bottom <= panel.boundsInWindow.bottom
            if (geometryLooksVisible && runCatching {
                    compose.onNodeWithContentDescription(title).assertIsDisplayed()
                }.isSuccess
            ) return
            val grid = compose.onNodeWithTag("workspace-menu-actions-grid")
            val pageDescription = activity.getString(R.string.WorkspaceMoreTitle)
            if (compose.onAllNodesWithContentDescription(pageDescription)
                    .fetchSemanticsNodes().isNotEmpty()
            ) {
                // Use the same page-sized accessible control exposed to users. Small synthetic
                // swipes can stop inside a large-font tile and leave the target clipped behind
                // the fixed paging affordance.
                compose.onNodeWithContentDescription(pageDescription).performClick()
            } else {
                val gridWidth = grid.fetchSemanticsNode().size.width.toFloat()
                grid.performTouchInput {
                    swipeLeft(startX = gridWidth * 0.65f, endX = gridWidth * 0.35f)
                }
            }
            compose.waitForIdle()
        }
        val action = compose.onAllNodesWithContentDescription(title)
            .fetchSemanticsNodes().firstOrNull()
        val tile = compose.onAllNodesWithTag(tileTag).fetchSemanticsNodes().firstOrNull()
        val panel = compose.onAllNodesWithTag("workspace-menu-panel")
            .fetchSemanticsNodes().firstOrNull()
        val descriptions = compose.onAllNodes(
            androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().mapNotNull { node ->
            runCatching { node.config[SemanticsProperties.ContentDescription].joinToString() }.getOrNull()
        }
        throw AssertionError(
            "Menu action '$title' did not become fully visible inside its panel; " +
            "action=${action?.boundsInWindow}, tile=${tile?.boundsInWindow}, panel=${panel?.boundsInWindow}, " +
                "grid=${compose.onAllNodesWithTag("workspace-menu-actions-grid").fetchSemanticsNodes().size}, " +
                "visible descriptions=$descriptions",
        )
    }

    private fun revealPaintFilamentSwatch(
        activity: MainActivity,
        index: Int,
        material: String,
        occurrence: Int,
    ): androidx.compose.ui.test.SemanticsNodeInteraction {
        val description = activity.getString(R.string.FilamentPaletteSlotDescription, index, material)
        val swatches = compose.onAllNodesWithContentDescription(description)
        compose.waitUntil(timeoutMillis = 5_000) { swatches.fetchSemanticsNodes().size > occurrence }
        repeat(6) {
            val target = swatches.get(occurrence)
            if (runCatching { target.assertIsDisplayed() }.isSuccess) return target
            compose.onNodeWithTag("workspace-mode-panel")
                .performTouchInput { swipeUp(startY = height * 0.8f, endY = height * 0.3f) }
            compose.waitForIdle()
        }
        swatches.get(occurrence).assertIsDisplayed()
        return swatches.get(occurrence)
    }

    /** Injects a screen-coordinate tap so integration tests cover the real window hit path. */
    private fun tapSemanticsNodeOnDevice(node: androidx.compose.ui.test.SemanticsNodeInteraction) {
        val boundsInWindow = node.fetchSemanticsNode().boundsInWindow
        val decorLocation = IntArray(2)
        compose.activity.window.decorView.getLocationOnScreen(decorLocation)
        val x = decorLocation[0] + boundsInWindow.center.x
        val y = decorLocation[1] + boundsInWindow.center.y
        lastDeviceTapDiagnostic = "window=$boundsInWindow decor=${decorLocation.toList()} screen=($x,$y) " +
            "click=${node.fetchSemanticsNode().config.contains(SemanticsActions.OnClick)}"
        val downTime = SystemClock.uptimeMillis()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(downTime, downTime + 80, MotionEvent.ACTION_UP, x, y, 0)
        try {
            check(automation.injectInputEvent(down, true)) { "Device rejected ACTION_DOWN at ($x, $y)" }
            check(automation.injectInputEvent(up, true)) { "Device rejected ACTION_UP at ($x, $y)" }
        } finally {
            down.recycle()
            up.recycle()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun openWorkspaceMenu(
        activity: MainActivity,
        destination: WorkspaceDestination,
        labelRes: Int,
    ) {
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val controller = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)
        val destinationSlot = destination.ordinal
        val compactPortrait = usesCompactPortraitLayout(activity)
        compose.waitUntil(timeoutMillis = 5_000) {
            !(controller.javaClass.getMethod("isNavigationTransitionInProgress").invoke(controller) as Boolean)
        }
        val needsSelection = controller.javaClass.getMethod("getSelectedDestinationSlot")
            .invoke(controller) != destinationSlot
        val needsExpansion = compactPortrait &&
            !(controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) as Boolean)
        if (needsSelection || needsExpansion) {
            val destinationNode = compose.onNodeWithContentDescription(activity.getString(labelRes))
            val destinationVisible = runCatching {
                destinationNode.assertIsDisplayed()
                true
            }.getOrDefault(false)
            if (!compactPortrait && !destinationVisible) {
                destinationNode.performScrollTo()
            }
            destinationNode.assertIsDisplayed().performClick()
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            controller.javaClass.getMethod("getSelectedDestinationSlot").invoke(controller) == destinationSlot &&
                (!compactPortrait || controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) as Boolean) &&
                !(controller.javaClass.getMethod("isNavigationTransitionInProgress").invoke(controller) as Boolean)
        }
        compose.waitForIdle()
    }

    private fun openEditMenu(activity: MainActivity) {
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val controller = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)
        compose.waitUntil(timeoutMillis = 5_000) {
            !(controller.javaClass.getMethod("isNavigationTransitionInProgress").invoke(controller) as Boolean)
        }
        val editSlot = WorkspaceDestination.EDIT.ordinal
        val shouldNavigate = controller.javaClass.getMethod("getSelectedDestinationSlot")
            .invoke(controller) != editSlot
        val compactPortrait = usesCompactPortraitLayout(activity)
        val shouldExpand = compactPortrait && !(controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) as Boolean)
        if (shouldNavigate || shouldExpand) {
            val destinationNode = compose.onNodeWithContentDescription(
                activity.getString(R.string.MenuToolbarNavigation),
            )
            if (!compactPortrait) destinationNode.performScrollTo()
            destinationNode.assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                controller.javaClass.getMethod("getSelectedDestinationSlot").invoke(controller) == editSlot &&
                    (!compactPortrait || controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) == true) &&
                    !(controller.javaClass.getMethod("isNavigationTransitionInProgress").invoke(controller) as Boolean)
            }
        }
        if (compactPortrait) scrollMenuToIndex(1)
    }

    private fun openModelMenu(activity: MainActivity) {
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val controller = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)
        compose.waitUntil(timeoutMillis = 5_000) {
            !(controller.javaClass.getMethod("isNavigationTransitionInProgress").invoke(controller) as Boolean)
        }
        val modelSlot = WorkspaceDestination.MODEL.ordinal
        val shouldNavigate = controller.javaClass.getMethod("getSelectedDestinationSlot")
            .invoke(controller) != modelSlot
        val compactPortrait = usesCompactPortraitLayout(activity)
        val shouldExpand = compactPortrait && !(controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) as Boolean)
        if (shouldNavigate || shouldExpand) {
            val destinationNode = compose.onNodeWithContentDescription(activity.getString(R.string.MenuModel))
            if (!compactPortrait) destinationNode.performScrollTo()
            destinationNode.assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                controller.javaClass.getMethod("getSelectedDestinationSlot").invoke(controller) == modelSlot &&
                    (!compactPortrait || controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) == true) &&
                    !(controller.javaClass.getMethod("isNavigationTransitionInProgress").invoke(controller) as Boolean)
            }
        }
    }

    private fun usesCompactPortraitLayout(activity: MainActivity): Boolean {
        val configuration = activity.resources.configuration
        return AdaptiveNavigationPolicy.usesCompactPortraitLayout(
            configuration.screenWidthDp,
            configuration.screenHeightDp,
            configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT,
        )
    }

    private fun selectAllObjectsScope() {
        compose.onNodeWithTag("model-menu-scope-all").performClick()
        compose.waitForIdle()
    }

    private fun selectSingleObjectScope() {
        compose.onNodeWithTag("model-menu-scope-object").performClick()
        compose.waitForIdle()
    }

    private fun openSelectedObjectMenu(activity: MainActivity) {
        openModelMenu(activity)
        selectSingleObjectScope()
    }

    @Test
    fun modelObjectsMenuAssignmentOverridesImportedThreeMfMaterial() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val file = File(activity.cacheDir, "menu-filament-assignment-test.3mf")
        InstrumentationRegistry.getInstrumentation().context.assets.open("models/two-color-project.3mf").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        val testSlots = listOf(
            FilamentSlot(0xFFCC3333.toInt(), "UI Test PLA"),
            FilamentSlot(0xFF3366CC.toInt(), "UI Test PETG"),
            FilamentSlot(0xFF33AA66.toInt(), "UI Test ABS"),
            FilamentSlot(0xFFFFCC22.toInt(), "UI Test TPU"),
            FilamentSlot(0xFF9933CC.toInt(), "UI Test Slot 5"),
            FilamentSlot(0xFFFF6633.toInt(), "UI Test Slot 6"),
            FilamentSlot(0xFF33CCCC.toInt(), "UI Test Slot 7"),
            FilamentSlot(0xFFF2F2F2.toInt(), "UI Test Slot 8"),
        )
        Prefs.setStoredFilamentSlots(testSlots)
        SliceBeam.INSTANCE = activity.applicationContext as SliceBeam
        Prefs.init(activity.applicationContext as SliceBeam)
        TemporaryProjectSettings.clear()
        val previousConfig = SliceBeam.CONFIG
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigFile = configFile.takeIf { it.isFile }?.readBytes()
        val prefs = requireNotNull(Prefs.getPrefs())
        val snapshotKeys = listOf("mmu_snapshot_printer", "mmu_snapshot")
        val previousSnapshots = snapshotKeys.associateWith { prefs.getString(it, null) }
        val output = File(activity.cacheDir, "menu-multi-object-filament-${System.nanoTime()}.gcode")
        var sliceResult: GCodeProcessorResult? = null
        try {
            val loadedLatch = CountDownLatch(1)
            var loaded: Model? = null
            var loadFailure: Exception? = null
            bed.loadModel(file, true, 1, object : BedFragment.ModelLoadCallback {
                override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                    loaded = loadedModel
                    loadedLatch.countDown()
                }

                override fun onFailed(error: Exception) {
                    loadFailure = error
                    loadedLatch.countDown()
                }
            })
            assertTrue("color 3MF fixture should load", loadedLatch.await(60, TimeUnit.SECONDS))
            assertEquals("color fixture load failure: $loadFailure", 2, requireNotNull(loaded).objectsCount)
            assertTrue("fixture begins with imported tool 1", loaded!!.getUsedFilaments(0).contentEquals(intArrayOf(1)))

            fun assignObject(index: Int, slot: Int) {
                openModelMenu(activity)
                compose.onNodeWithText(activity.getString(R.string.ModelMenuObjectsShort))
                    .assertIsDisplayed().performClick()
                compose.onAllNodesWithText(activity.getString(R.string.ObjectActionAssignFilamentShort))[index]
                    .assertIsDisplayed().performClick()
                val targetLabel = activity.getString(
                    R.string.ModelContextFilamentSlot,
                    slot,
                    testSlots[slot].type,
                    String.format(java.util.Locale.ROOT, "#%06X", testSlots[slot].color and 0xFFFFFF),
                )
                compose.onNodeWithTag("model-choice-dialog-options")
                    .performScrollToNode(hasText(targetLabel))
                compose.onNodeWithText(targetLabel).assertIsDisplayed().performClick()
                compose.waitUntil(timeoutMillis = 10_000) {
                    bed.currentModel?.getUsedFilaments(index)?.contentEquals(intArrayOf(slot + 1)) == true
                }
            }

            // Assign different own slots through the real object-list UI, one object at a time.
            assignObject(index = 0, slot = 6)
            assertArrayEquals("assigning object 1 must leave object 2 on its imported tool",
                intArrayOf(2), requireNotNull(bed.currentModel).getUsedFilaments(1))
            assignObject(index = 1, slot = 3)
            val assignedModel = requireNotNull(bed.currentModel)
            assertArrayEquals("object 1 should independently use own T6", intArrayOf(7), assignedModel.getUsedFilaments(0))
            assertArrayEquals("object 2 should independently use own T3", intArrayOf(4), assignedModel.getUsedFilaments(1))
            assertFalse("uniform reassignment removes old per-volume paint overrides",
                assignedModel.hasPaint(0))

            // Slice the same renderer model and check both the G-code tools and its declared palette.
            activity.assets.open("orca_profiles/Anycubic.ini").use { SliceBeam.CONFIG = Slic3rConfigWrapper(it) }
            SliceBeam.requireConfig().presets = ConfigObject("presets").apply {
                put("printer", "Anycubic Kobra 3 0.4 nozzle")
                put("print", "0.16mm Standard")
                put("filament", "Anycubic Generic PLA")
            }
            val editor = prefs.edit()
            snapshotKeys.forEach(editor::remove)
            editor.commit()
            SliceBeam.genCurrentConfig(assignedModel)
            val expectedPalette = testSlots.take(7).joinToString(";") {
                String.format(java.util.Locale.ROOT, "#%06X", it.color and 0xFFFFFF)
            }
            val generatedConfig = configFile.readText()
            assertTrue("slice config must include all seven slots through assigned T6",
                Regex("(?m)^filament_colour = ${Regex.escape(expectedPalette)}$").containsMatchIn(generatedConfig))
            assertFalse("bundled Anycubic Generic PLA start G-code must not contain unsupported, commented placeholders",
                generatedConfig.contains("filament_start_gcode = ") &&
                    Regex("(?m)^filament_start_gcode = .*\\{if").containsMatchIn(generatedConfig))
            sliceResult = assignedModel.slice(configFile.absolutePath, output.absolutePath, null)
            assertTrue("the multicolor object slice should create non-empty G-code", output.isFile && output.length() > 0)
            val gcode = output.readText()
            val emittedTools = Regex("(?m)^T(\\d+)(?:\\s|$)").findAll(gcode)
                .map { it.groupValues[1].toInt() }.toSet()
            assertTrue("G-code must emit object 1's assigned T6, tools=$emittedTools", 6 in emittedTools)
            assertTrue("G-code must emit object 2's assigned T3, tools=$emittedTools", 3 in emittedTools)
            assertTrue("G-code preview palette must match all seven required slot colors",
                Regex("(?m)^; filament_colour = ${Regex.escape(expectedPalette)}$").containsMatchIn(gcode))
        } finally {
            sliceResult?.release()
            output.delete()
            val editor = prefs.edit()
            previousSnapshots.forEach { (key, value) -> if (value == null) editor.remove(key) else editor.putString(key, value) }
            editor.commit()
            SliceBeam.CONFIG = previousConfig
            if (previousConfigFile == null) configFile.delete() else configFile.writeBytes(previousConfigFile)
            TemporaryProjectSettings.clear()
            Prefs.setStoredFilamentSlots(previousSlots)
            file.delete()
        }
    }

    @Test
    fun preSliceCanChooseDifferentSlotsForEachObject() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val file = File(activity.cacheDir, "pre-slice-per-object-assignment.3mf")
        InstrumentationRegistry.getInstrumentation().context.assets.open("models/two-color-project.3mf").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        val testSlots = listOf(
            FilamentSlot(0xFFCC3333.toInt(), "Test PLA"),
            FilamentSlot(0xFF3366CC.toInt(), "Test PETG"),
            FilamentSlot(0xFF33AA66.toInt(), "Test ABS"),
            FilamentSlot(0xFFFFCC22.toInt(), "Test TPU"),
        )
        Prefs.setStoredFilamentSlots(testSlots)
        try {
            val loadedLatch = CountDownLatch(1)
            val loadFailure = AtomicReference<Exception?>()
            bed.loadModel(file, true, 1, object : BedFragment.ModelLoadCallback {
                override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                    loadedLatch.countDown()
                }

                override fun onFailed(error: Exception) {
                    loadFailure.set(error)
                    loadedLatch.countDown()
                }
            })
            assertTrue("two-color project import should finish", loadedLatch.await(60, TimeUnit.SECONDS))
            assertEquals("two-color project import failure: ${loadFailure.get()}", 2,
                requireNotNull(bed.currentModel).objectsCount)

            val selectedSlots = AtomicReference<IntArray?>()
            PreSliceFilamentDialog.show(
                bed,
                requireNotNull(bed.currentModel),
                PreSliceFilamentDialog.Callback { selectedSlots.set(it) },
            )
            compose.onNodeWithTag("pre-slice-title").assertIsDisplayed()
            compose.onNodeWithText(activity.getString(R.string.PreSliceIndividualMode))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag("pre-slice-object-slot-0").fetchSemanticsNodes().isNotEmpty()
            }

            // Object 1 -> slot 4/T3, object 2 -> slot 2/T1.
            compose.onNodeWithTag("pre-slice-object-slot-0").performScrollTo().performClick()
            compose.onNodeWithTag("pre-slice-object-0-filament-3").performScrollTo().performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("pre-slice-object-slot-1").performScrollTo().performClick()
            compose.onNodeWithTag("pre-slice-object-1-filament-1").performScrollTo().performClick()
            compose.onNodeWithTag("pre-slice-confirm").performClick()
            assertArrayEquals("first object maps to slot 4", intArrayOf(3, 1), selectedSlots.get())
            assertArrayEquals("choosing assignments is staged until the user confirms the slice",
                intArrayOf(1), requireNotNull(bed.currentModel).getUsedFilaments(0))
        } finally {
            Prefs.setStoredFilamentSlots(previousSlots)
            file.delete()
        }
    }

    private fun instrumentationWaitForGlIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SystemClock.sleep(250L)
    }

    private fun readGCodeOptionOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, option: Int): Boolean {
        val finished = CountDownLatch(1)
        val visible = AtomicReference<Boolean>()
        glView.queueEvent {
            visible.set(requireNotNull(glView.renderer.viewer).isOptionVisible(option))
            finished.countDown()
        }
        assertTrue("GL thread did not return G-code option visibility", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(visible.get())
    }

    private fun readScaleOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Vec3d {
        val finished = CountDownLatch(1)
        val scale = AtomicReference<Vec3d?>()
        val failure = AtomicReference<Throwable?>()
        glView.queueEvent {
            try {
                val model = requireNotNull(glView.renderer.model)
                check(model.pointer != 0L) { "Renderer returned a released native model" }
                check(index in 0 until model.objectsCount) {
                    "Renderer model has ${model.objectsCount} objects; requested scale for index $index"
                }
                scale.set(Vec3d().also { model.getScale(index, it) })
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                finished.countDown()
            }
        }
        assertTrue("GL thread did not return model scale", finished.await(10, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("Could not read model scale on GL thread", it) }
        return requireNotNull(scale.get())
    }

    private fun readTranslationOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Vec3d =
        readVectorOnGl(glView, index) { model, vector -> model.getTranslation(index, vector) }

    private fun readRotationOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Vec3d =
        readVectorOnGl(glView, index) { model, vector -> model.getRotation(index, vector) }

    private fun readSelectedObjectOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView): Int {
        val finished = CountDownLatch(1)
        val selected = AtomicReference<Int>()
        glView.queueEvent {
            selected.set(glView.renderer.selectedObject)
            finished.countDown()
        }
        assertTrue("GL thread did not return selected object", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(selected.get())
    }

    private fun readSelectedObjectsCountOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView): Int {
        val finished = CountDownLatch(1)
        val selected = AtomicReference<Int>()
        glView.queueEvent {
            selected.set(glView.renderer.selectedObjectsCount)
            finished.countDown()
        }
        assertTrue("GL thread did not return selected object count", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(selected.get())
    }

    private fun readSelectedIndicesOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView): IntArray {
        val finished = CountDownLatch(1)
        val selected = AtomicReference<IntArray>()
        glView.queueEvent {
            selected.set(glView.renderer.selectedObjectIndices())
            finished.countDown()
        }
        assertTrue("GL thread did not return selected object indices", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(selected.get())
    }

    private fun readExactBoundsOnGl(
        glView: ru.ytkab0bp.slicebeam.view.GLView,
        vararg indices: Int,
    ): List<DoubleArray> {
        val finished = CountDownLatch(1)
        val bounds = AtomicReference<List<DoubleArray>>()
        glView.queueEvent {
            val model = requireNotNull(glView.renderer.model)
            bounds.set(indices.map { index ->
                val min = ru.ytkab0bp.slicebeam.utils.Vec3d()
                val max = ru.ytkab0bp.slicebeam.utils.Vec3d()
                model.getBoundingBoxExact(index, min, max)
                doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z)
            })
            finished.countDown()
        }
        assertTrue("GL thread did not return exact bounds", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(bounds.get())
    }

    private fun readBoundsOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): String {
        val finished = CountDownLatch(1)
        val bounds = AtomicReference<String>()
        glView.queueEvent {
            val min = ru.ytkab0bp.slicebeam.utils.Vec3d()
            val max = ru.ytkab0bp.slicebeam.utils.Vec3d()
            glView.renderer.model?.getBoundingBoxExact(index, min, max)
            bounds.set("min=(${min.x},${min.y},${min.z}), max=(${max.x},${max.y},${max.z})")
            finished.countDown()
        }
        assertTrue("GL thread did not return model bounds", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(bounds.get())
    }

    private fun readObjectCountOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView): Int {
        val finished = CountDownLatch(1)
        val count = AtomicReference<Int>()
        glView.queueEvent {
            count.set(requireNotNull(glView.renderer.model).objectsCount)
            finished.countDown()
        }
        assertTrue("GL thread did not return model object count", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(count.get())
    }

    private fun readCutConnectorCountOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Int {
        val finished = CountDownLatch(1)
        val count = AtomicReference<Int>()
        glView.queueEvent {
            count.set(glView.renderer.getCutConnectorCount(index))
            finished.countDown()
        }
        assertTrue("GL thread did not return cut connector count", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(count.get())
    }

    private fun tapGlViewCenter(glView: ru.ytkab0bp.slicebeam.view.GLView) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val downTime = SystemClock.uptimeMillis()
            val x = glView.width / 2f
            val y = glView.height / 2f
            val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
            val up = MotionEvent.obtain(downTime, downTime + 80L, MotionEvent.ACTION_UP, x, y, 0)
            try {
                glView.dispatchTouchEvent(down)
                glView.dispatchTouchEvent(up)
            } finally {
                down.recycle()
                up.recycle()
            }
        }
    }

    private fun readTriangleCountOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Int {
        val finished = CountDownLatch(1)
        val count = AtomicReference<Int>()
        glView.queueEvent {
            count.set(requireNotNull(glView.renderer.model).getTriangleCount(index))
            finished.countDown()
        }
        assertTrue("GL thread did not return native triangle count", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(count.get())
    }

    private fun readRendererBooleanOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, fieldName: String): Boolean {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Boolean>()
        glView.queueEvent {
            val field = glView.renderer.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }
            value.set(field.getBoolean(glView.renderer))
            finished.countDown()
        }
        assertTrue("GL thread did not return renderer state $fieldName", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(value.get())
    }

    private fun readProjectionMatrixOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView): DoubleArray {
        val finished = CountDownLatch(1)
        val matrix = AtomicReference<DoubleArray>()
        glView.queueEvent {
            matrix.set(glView.renderer.projectionMatrix.clone())
            finished.countDown()
        }
        assertTrue("GL thread did not return the projection matrix", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(matrix.get())
    }

    private fun readCameraPoseOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView): DoubleArray {
        val finished = CountDownLatch(1)
        val pose = AtomicReference<DoubleArray>()
        glView.queueEvent {
            val camera = glView.renderer.camera
            pose.set(doubleArrayOf(
                camera.origin.x, camera.origin.y, camera.origin.z,
                camera.position.x, camera.position.y, camera.position.z,
            ))
            finished.countDown()
        }
        assertTrue("GL thread did not return the camera pose", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(pose.get())
    }

    private fun readRendererIntOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, fieldName: String): Int {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Int>()
        glView.queueEvent {
            val field = glView.renderer.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }
            value.set(field.getInt(glView.renderer))
            finished.countDown()
        }
        assertTrue("GL thread did not return renderer state $fieldName", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(value.get())
    }

    private fun readModelHasPaintOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Boolean {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Boolean>()
        glView.queueEvent {
            value.set(requireNotNull(glView.renderer.model).hasPaint(index))
            finished.countDown()
        }
        assertTrue("GL thread did not return model paint state", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(value.get())
    }

    private fun readUsedFilamentsOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): IntArray {
        val finished = CountDownLatch(1)
        val value = AtomicReference<IntArray>()
        glView.queueEvent {
            value.set(requireNotNull(glView.renderer.model).getUsedFilaments(index))
            finished.countDown()
        }
        assertTrue("GL thread did not return used filament assignments", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(value.get())
    }

    private fun readPaintMaxFilamentOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Int {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Int>()
        glView.queueEvent {
            value.set(requireNotNull(glView.renderer.model).paintMaxFilament(index))
            finished.countDown()
        }
        assertTrue("GL thread did not return maximum painted filament", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(value.get())
    }

    private fun readPaintPreviewTriangleCountOnGl(
        glView: ru.ytkab0bp.slicebeam.view.GLView,
        filament: Int,
    ): Int {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Int>()
        val failure = AtomicReference<Throwable?>()
        glView.queueEvent {
            var overlay: GLModel? = null
            try {
                val renderer = glView.renderer
                val session = GLRenderer::class.java.getDeclaredField("paintSession")
                    .apply { isAccessible = true }.get(renderer) as? PaintSession
                if (session == null) error("Paint session ended before preview could be inspected")
                overlay = GLModel()
                value.set(session.buildOverlay(overlay, filament))
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                overlay?.release()
                finished.countDown()
            }
        }
        assertTrue("GL thread did not return paint preview triangles", finished.await(10, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("Could not inspect active paint preview", it) }
        return requireNotNull(value.get())
    }

    /** Finds an exact mesh hit in the visible portion of the GL view, outside the paint sheet. */
    private fun findUncoveredPreciseSurfacePoint(
        glView: ru.ytkab0bp.slicebeam.view.GLView,
        bed: BedFragment,
    ): Pair<Float, Float> {
        val modePanelBounds = compose.onAllNodesWithTag("workspace-mode-panel")
            .fetchSemanticsNodes().firstOrNull()?.boundsInRoot
        val menuPanelBounds = compose.onAllNodesWithTag("workspace-menu-panel")
            .fetchSemanticsNodes().firstOrNull()?.boundsInRoot
        val glLocation = IntArray(2)
        val composeLocation = IntArray(2)
        compose.runOnUiThread {
            glView.getLocationOnScreen(glLocation)
            requireNotNull(bed.overlayLayout).getLocationOnScreen(composeLocation)
        }
        val result = AtomicReference<Pair<Float, Float>?>(null)
        val finished = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        glView.queueEvent {
            try {
                val renderer = glView.renderer
                @Suppress("UNCHECKED_CAST")
                val models = GLRenderer::class.java.getDeclaredField("glModels")
                    .apply { isAccessible = true }.get(renderer) as ArrayList<GLModel>
                val mesh = models.firstOrNull() ?: error("Paint mode did not initialize the selected GL mesh")
                val hits = ArrayList<GLModel.HitResult>()
                val renderScale = Prefs.getRenderScale()
                val centerX = renderer.viewportWidth / 2
                val centerY = renderer.viewportHeight / 2
                // Search around the projected center first, where the loaded cube is framed.
                for (radius in listOf(0, 16, 32, 48, 64, 96, 128, 176)) {
                    val offsets = if (radius == 0) listOf(0 to 0) else buildList {
                        for (offset in -radius..radius step 8) {
                            add(offset to -radius)
                            add(offset to radius)
                            add(-radius to offset)
                            add(radius to offset)
                        }
                    }
                    for ((dx, dy) in offsets) {
                        val x = centerX + dx
                        val y = centerY + dy
                        if (x !in 0 until renderer.viewportWidth || y !in 0 until renderer.viewportHeight) continue
                        val localX = x / renderScale
                        val localY = y / renderScale
                        val rootX = glLocation[0] + localX - composeLocation[0]
                        val rootY = glLocation[1] + localY - composeLocation[1]
                        if (modePanelBounds?.contains(Offset(rootX, rootY)) == true ||
                            menuPanelBounds?.contains(Offset(rootX, rootY)) == true
                        ) continue
                        mesh.raycaster.raycast(renderer, hits, x.toFloat(), y.toFloat())
                        if (hits.any { !it.approximate }) {
                            result.set((glLocation[0] + localX) to (glLocation[1] + localY))
                            return@queueEvent
                        }
                    }
                }
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                finished.countDown()
            }
        }
        assertTrue("GL thread did not locate a visible exact surface hit", finished.await(15, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("Could not locate visible paint target", it) }
        return requireNotNull(result.get()) {
            "No precise model surface point was reachable outside workspace panels; " +
                "modePanel=$modePanelBounds, menuPanel=$menuPanelBounds"
        }
    }

    private fun tapGlViewAt(glView: ru.ytkab0bp.slicebeam.view.GLView, screenX: Float, screenY: Float) {
        val canvasLocation = IntArray(2)
        glView.getLocationOnScreen(canvasLocation)
        assertTrue("connector test point must be visible inside the GL canvas",
            screenX >= canvasLocation[0] && screenX < canvasLocation[0] + glView.width &&
                screenY >= canvasLocation[1] && screenY < canvasLocation[1] + glView.height)
        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(
            downTime, downTime, MotionEvent.ACTION_DOWN,
            screenX - canvasLocation[0], screenY - canvasLocation[1], 0,
        ).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
        val up = MotionEvent.obtain(
            downTime, downTime + 80L, MotionEvent.ACTION_UP,
            screenX - canvasLocation[0], screenY - canvasLocation[1], 0,
        ).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
        try {
            var downHandled = false
            var upHandled = false
            compose.runOnUiThread {
                downHandled = glView.dispatchTouchEvent(down)
                upHandled = glView.dispatchTouchEvent(up)
            }
            assertTrue("the GL canvas rejected the connector touch", downHandled && upHandled)
        } finally {
            down.recycle()
            up.recycle()
        }
    }

    private fun injectCanvasStroke(glView: ru.ytkab0bp.slicebeam.view.GLView, screenX: Float, screenY: Float) {
        val canvasLocation = IntArray(2)
        glView.getLocationOnScreen(canvasLocation)
        assertTrue(
            "paint test point must be inside the GL canvas; " +
                "screen=($screenX,$screenY), canvasOrigin=(${canvasLocation[0]},${canvasLocation[1]}), " +
                "canvas=${glView.width}x${glView.height}, renderScale=${Prefs.getRenderScale()}",
            screenX >= canvasLocation[0] && screenX < canvasLocation[0] + glView.width &&
                screenY >= canvasLocation[1] && screenY < canvasLocation[1] + glView.height)
        val downTime = SystemClock.uptimeMillis()
        fun inject(action: Int, deltaX: Float, deltaY: Float, eventTime: Long) {
            val event = MotionEvent.obtain(
                downTime, eventTime, action,
                screenX - canvasLocation[0] + deltaX,
                screenY - canvasLocation[1] + deltaY,
                0,
            ).apply {
                source = InputDevice.SOURCE_TOUCHSCREEN
            }
            try {
                var handled = false
                compose.runOnUiThread { handled = glView.dispatchTouchEvent(event) }
                assertTrue("the GL canvas rejected a touch event at the selected surface point", handled)
            } finally {
                event.recycle()
            }
        }
        inject(MotionEvent.ACTION_DOWN, 0f, 0f, downTime)
        repeat(5) { step ->
            SystemClock.sleep(24L)
            inject(MotionEvent.ACTION_MOVE, step * 2f, step * 2f, SystemClock.uptimeMillis())
        }
        SystemClock.sleep(24L)
        inject(MotionEvent.ACTION_UP, 10f, 10f, SystemClock.uptimeMillis())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun hasSnackbarTitle(bed: BedFragment, titleRes: Int): Boolean {
        val snackbarController = BedFragment::class.java.getDeclaredField("snackbarController")
            .apply { isAccessible = true }.get(bed)
        val entries = snackbarController.javaClass.getMethod("getEntries").invoke(snackbarController) as List<*>
        val expectedTitle = compose.activity.getString(titleRes)
        return entries.any { entry ->
            val snackbar = entry?.javaClass?.getMethod("getSnackbar")?.invoke(entry)
            snackbar?.javaClass?.getField("title")?.get(snackbar) == expectedTitle
        }
    }

    private fun readVectorOnGl(
        glView: ru.ytkab0bp.slicebeam.view.GLView,
        index: Int,
        read: (Model, Vec3d) -> Unit,
    ): Vec3d {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Vec3d?>()
        val failure = AtomicReference<Throwable?>()
        glView.queueEvent {
            try {
                val model = requireNotNull(glView.renderer.model)
                check(model.pointer != 0L) { "Renderer returned a released native model" }
                check(index in 0 until model.objectsCount) {
                    "Renderer model has ${model.objectsCount} objects; requested vector for index $index"
                }
                value.set(Vec3d().also { read(model, it) })
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                finished.countDown()
            }
        }
        assertTrue("GL thread did not return model vector", finished.await(10, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("Could not read native model vector on GL thread", it) }
        return requireNotNull(value.get())
    }

    private fun hasExpectedLinearTransform(
        glView: ru.ytkab0bp.slicebeam.view.GLView,
        index: Int,
        expectedDiagonal: DoubleArray,
    ): Boolean {
        val rotation = readVectorOnGl(glView, index) { model, vector -> model.getRotation(index, vector) }
        val scale = readVectorOnGl(glView, index) { model, vector -> model.getScale(index, vector) }
        val mirror = readVectorOnGl(glView, index) { model, vector -> model.getMirror(index, vector) }
        val sx = scale.x * mirror.x; val sy = scale.y * mirror.y; val sz = scale.z * mirror.z
        val cx = kotlin.math.cos(rotation.x); val cy = kotlin.math.cos(rotation.y); val cz = kotlin.math.cos(rotation.z)
        val sinX = kotlin.math.sin(rotation.x); val sinY = kotlin.math.sin(rotation.y); val sinZ = kotlin.math.sin(rotation.z)
        val matrix = arrayOf(
            doubleArrayOf(cz * cy * sx, (cz * sinY * sinX - sinZ * cx) * sy, (cz * sinY * cx + sinZ * sinX) * sz),
            doubleArrayOf(sinZ * cy * sx, (sinZ * sinY * sinX + cz * cx) * sy, (sinZ * sinY * cx - cz * sinX) * sz),
            doubleArrayOf(-sinY * sx, cy * sinX * sy, cy * cx * sz),
        )
        for (row in 0..2) for (column in 0..2) {
            val expected = if (row == column) expectedDiagonal[row] else 0.0
            if (kotlin.math.abs(matrix[row][column] - expected) > 1e-4) return false
        }
        return true
    }

    private fun createInitializedActivityRule() = run {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        // Menu customizations are intentionally persisted by the app. Give this broad
        // integration class a stable default order so a prior customization test run
        // cannot hide actions that these tests expect in their baseline menu.
        context.getSharedPreferences("workspace_menu_customization", 0).edit().clear().commit()
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
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
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
