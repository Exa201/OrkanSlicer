package ru.ytkab0bp.slicebeam

import android.app.Application
import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.content.IntentFilter
import android.content.ClipData
import android.net.Uri
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
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
import androidx.compose.ui.test.performTextReplacement
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
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
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
import ru.ytkab0bp.slicebeam.components.bed_menu.FileMenuObjectActions
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
    fun externallyOpenedDetailedMulticolorSvgLoadsThroughWorkspaceImportPath() {
        val activity = compose.activity
        val source = File(activity.cacheDir, "external-multicolor-${System.nanoTime()}.svg")
        try {
            InstrumentationRegistry.getInstrumentation().context.assets
                .open("models/Beste_Oma_Kind_Herz_3D_Druck.svg").use { input ->
                source.outputStream().use(input::copyTo)
            }
            @Suppress("DEPRECATION")
            val provider = activity.packageManager.getPackageInfo(
                activity.packageName,
                PackageManager.GET_PROVIDERS,
            ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
            val authority = requireNotNull(provider.authority).substringBefore(';')
            val uri = FileProvider.getUriForFile(activity, authority, source)

            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod("handleExternalModelOpen", Intent::class.java)
                    .apply { isAccessible = true }
                    .invoke(activity, Intent(Intent.ACTION_VIEW).setData(uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }

            compose.waitUntil(timeoutMillis = 60_000) {
                val model = (activity.navigationDelegate.getCurrentFragment() as? BedFragment)?.currentModel
                model != null && model.objectsCount == 1 && model.getTriangleCount(0) > 0
            }
            val model = requireNotNull((activity.navigationDelegate.getCurrentFragment() as BedFragment).currentModel)
            assertTrue("Imported SVG should render with its original printable width",
                model.boundingBoxApproxMax.x - model.boundingBoxApproxMin.x > 100.0)
            assertTrue("Opening an SVG must not delete the selected source document", source.isFile)
        } finally {
            source.delete()
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
    fun modelMenuSelectionActionsFollowGlOwnedModelAndSelectionState() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-menu-gl-state-test.stl")
        try {
            openModelMenu(activity)
            revealMenuAction(activity, R.string.ModelMenuDuplicate)
            val duplicate = compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDuplicate))
                .performScrollTo()
            duplicate.assertIsDisplayed().assertIsNotEnabled()

            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            compose.runOnUiThread { glView.selectModel(0, Runnable {}) }
            instrumentationWaitForGlIdle()
            compose.waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDuplicate))
                        .fetchSemanticsNode().config[SemanticsProperties.Disabled]
                }.getOrNull() == null
            }
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDuplicate))
                .assertIsEnabled()

            compose.runOnUiThread { glView.selectModel(-1, Runnable {}) }
            instrumentationWaitForGlIdle()
            compose.waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    SemanticsProperties.Disabled in compose.onNodeWithContentDescription(
                        activity.getString(R.string.ModelMenuDuplicate),
                    ).fetchSemanticsNode().config
                }.getOrDefault(false)
            }
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDuplicate))
                .assertIsNotEnabled()

            selectAllObjectsScope()
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuArrange))
                .assertIsDisplayed().assertIsEnabled()
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuBooleanUnionRunsForAllThreeSelectedObjects() {
        runModelMenuBooleanRoundTrip(
            operationLabel = R.string.ModelBooleanUnion,
            fileName = "model-menu-three-boolean-test.stl",
            // The bundled STL cube's faceted mesh yields a 2.027 volume ratio for this
            // overlap arrangement even though its measured union bounds are exactly 2x.
            expectedVolumeRatio = 2.027,
            expectedBoundsWidthRatio = 2.0,
        ) { model, _, width ->
            model.translate(1, width * 0.5, 0.0, 0.0)
            model.translate(2, width, 0.0, 0.0)
        }
    }

    @Test
    fun booleanUiUsesPrimarySelectedObjectAsTargetMaterial() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "boolean-primary-target-material-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val renderer = glView.renderer
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(0, min, max)
                val width = max.x - min.x
                model.addObject(model, 0)
                model.translate(1, width * 0.5, 0.0, 0.0)
                model.setUniformFilament(0, 1)
                model.setUniformFilament(1, 2)
                renderer.setSelectedObject(1)
                renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("Boolean sources should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            selectAllObjectsScope()
            revealMenuAction(activity, R.string.ModelMenuBoolean)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuBoolean))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelBooleanUnion), useUnmergedTree = true)
                .performScrollTo().assertIsDisplayed().performClick()

            compose.waitUntil(timeoutMillis = 30_000) { readObjectCountOnGl(glView) == 1 }
            assertArrayEquals(
                "the primary selected object (index 1) must supply the Boolean result material",
                intArrayOf(2), readUsedFilamentsOnGl(glView, 0),
            )
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 2 }
            assertArrayEquals("Undo should restore both source material assignments", intArrayOf(1),
                readUsedFilamentsOnGl(glView, 0))
            assertArrayEquals(intArrayOf(2), readUsedFilamentsOnGl(glView, 1))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            assertArrayEquals("Redo should retain the primary target's material", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuBooleanDifferenceAppliesMultipleCuttersAndSupportsUndoRedo() {
        runModelMenuBooleanRoundTrip(
            operationLabel = R.string.ModelBooleanDifference,
            fileName = "model-menu-boolean-difference-two-cutters.stl",
            expectedVolumeRatio = 0.946,
        ) { model, _, width ->
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val centerX = (min.x + max.x) / 2.0
            val centerY = (min.y + max.y) / 2.0
            val centerZ = (min.z + max.z) / 2.0
            for (index in 1..2) {
                model.scale(index, 0.3, 0.3, 0.3)
                val cutterMin = Vec3d(); val cutterMax = Vec3d()
                model.getBoundingBoxExact(index, cutterMin, cutterMax)
                val offsetX = if (index == 1) -width * 0.2 else width * 0.2
                model.translate(
                    index,
                    centerX + offsetX - (cutterMin.x + cutterMax.x) / 2.0,
                    centerY - (cutterMin.y + cutterMax.y) / 2.0,
                    centerZ - (cutterMin.z + cutterMax.z) / 2.0,
                )
            }
        }
    }

    @Test
    fun modelMenuBooleanIntersectionCombinesThreeObjectsAndSupportsUndoRedo() {
        runModelMenuBooleanRoundTrip(
            operationLabel = R.string.ModelBooleanIntersection,
            fileName = "model-menu-boolean-intersection-three-objects.stl",
            expectedVolumeRatio = 0.36,
        ) { model, _, width ->
            // Keep all three solids' lower faces at the plate plane. Their 0.6-width XY
            // overlap yields a 0.36 volume intersection that is directly sliceable.
            model.translate(1, width * 0.2, width * 0.2, 0.0)
            model.translate(2, -width * 0.2, -width * 0.2, 0.0)
        }
    }

    private fun runModelMenuBooleanRoundTrip(
        operationLabel: Int,
        fileName: String,
        expectedVolumeRatio: Double,
        expectedBoundsWidthRatio: Double? = null,
        configure: (Model, GLRenderer, Double) -> Unit,
    ) {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, fileName)
        val projectFile = File(activity.cacheDir, fileName.removeSuffix(".stl") + ".3mf")
        val gcodeFile = File(activity.cacheDir, fileName.removeSuffix(".stl") + ".gcode")
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        val previousConfig = SliceBeam.CONFIG
        val previousInstance = SliceBeam.INSTANCE
        SliceBeam.INSTANCE = activity.applicationContext as SliceBeam
        Prefs.init(activity.applicationContext as SliceBeam)
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigFile = configFile.takeIf { it.isFile }?.readBytes()
        val prefs = requireNotNull(Prefs.getPrefs())
        val snapshotKeys = listOf("mmu_snapshot_printer", "mmu_snapshot")
        val previousSnapshots = snapshotKeys.associateWith { prefs.getString(it, null) }
        var roundTrippedModel: Model? = null
        var sliceResult: GCodeProcessorResult? = null
        try {
            Prefs.setStoredFilamentSlots(listOf(
                FilamentSlot(0xFF303030.toInt(), "Boolean target T0"),
                FilamentSlot(0xFF3366CC.toInt(), "Boolean cutter T1"),
                FilamentSlot(0xFFF2F2F2.toInt(), "Boolean cutter T2"),
            ))
            TemporaryProjectSettings.clear()
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val sourceVolume = AtomicReference<Double>()
            val sourceWidth = AtomicReference<Double>()
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val renderer = glView.renderer
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(0, min, max)
                val width = max.x - min.x
                sourceVolume.set(model.getObjectVolume(0))
                sourceWidth.set(width)
                model.addObject(model, 0)
                model.addObject(model, 0)
                model.setUniformFilament(0, 1)
                model.setUniformFilament(1, 2)
                model.setUniformFilament(2, 3)
                configure(model, renderer, width)
                renderer.selectAllObjects()
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("Boolean sources should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            selectAllObjectsScope()
            revealMenuAction(activity, R.string.ModelMenuBoolean)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuBoolean))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithText(activity.getString(operationLabel), useUnmergedTree = true)
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 30_000) { readObjectCountOnGl(glView) == 1 }

            fun volumeOnGl(): Double {
                val value = AtomicReference<Double>()
                val finished = CountDownLatch(1)
                glView.queueEvent {
                    value.set(requireNotNull(glView.renderer.model).getObjectVolume(0))
                    finished.countDown()
                }
                assertTrue("Boolean result volume should be read on the GL thread", finished.await(10, TimeUnit.SECONDS))
                return requireNotNull(value.get())
            }

            assertEquals("the selected Boolean action should produce the expected solid volume",
                expectedVolumeRatio, volumeOnGl() / requireNotNull(sourceVolume.get()), 0.025)
            assertArrayEquals("Boolean output should keep the target material and discard cutter assignments",
                intArrayOf(1), readUsedFilamentsOnGl(glView, 0))
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 3 }
            assertEquals("Undo should restore all Boolean source objects", 3, readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            assertEquals("Redo should retain the chosen Boolean solid volume", expectedVolumeRatio,
                volumeOnGl() / requireNotNull(sourceVolume.get()), 0.025)
            assertArrayEquals("Redo should keep the target material assignment", intArrayOf(1),
                readUsedFilamentsOnGl(glView, 0))
            expectedBoundsWidthRatio?.let { expectedRatio ->
                val resultBounds = readExactBoundsOnGl(glView, 0).single()
                assertEquals("Boolean union should include all three selected solids",
                    expectedRatio * requireNotNull(sourceWidth.get()), resultBounds[3] - resultBounds[0], 0.03)
            }

            val exportReady = CountDownLatch(1)
            val exportFailure = AtomicReference<Throwable?>()
            glView.queueEvent {
                try {
                    val resultModel = requireNotNull(glView.renderer.model)
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
                    check(clearSnapshots.commit())
                    SliceBeam.genCurrentConfig(resultModel)
                    resultModel.export3mf(configFile.absolutePath, projectFile.absolutePath)
                } catch (error: Throwable) {
                    exportFailure.set(error)
                } finally {
                    exportReady.countDown()
                }
            }
            assertTrue("Boolean result should export after the UI action", exportReady.await(60, TimeUnit.SECONDS))
            exportFailure.get()?.let { throw AssertionError("Boolean result 3MF export failed", it) }
            assertTrue("Boolean result should create a non-empty 3MF", projectFile.isFile && projectFile.length() > 0L)
            roundTrippedModel = Model(projectFile, 1)
            assertEquals("Boolean result should remain one object after reload", 1, roundTrippedModel!!.objectsCount)
            assertArrayEquals("3MF reload should keep only the target filament", intArrayOf(1),
                roundTrippedModel!!.getUsedFilaments(0))
            SliceBeam.genCurrentConfig(roundTrippedModel!!)
            sliceResult = roundTrippedModel!!.slice(configFile.absolutePath, gcodeFile.absolutePath, null)
            assertTrue("reloaded Boolean result should produce G-code", gcodeFile.isFile && gcodeFile.length() > 0L)
            val gcode = gcodeFile.readText()
            assertTrue("reloaded Boolean result should contain deposited extrusion",
                Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
            val emittedTools = Regex("(?m)^T(\\d+)(?:\\s|$)").findAll(gcode)
                .map { it.groupValues[1].toInt() }.toSet()
            // Orca may omit an explicit T0 when only the default first tool remains active.
            // The 3MF assertion above proves that tool; any explicit selection must still be T0.
            assertTrue("Boolean output may select only target T0; found $emittedTools",
                emittedTools.all { it == 0 })
            assertFalse("removed cutter T1 must not be used by the Boolean result", 1 in emittedTools)
            assertFalse("removed cutter T2 must not be used by the Boolean result", 2 in emittedTools)
        } finally {
            sliceResult?.release()
            roundTrippedModel?.release()
            gcodeFile.delete()
            projectFile.delete()
            val restoreSnapshots = prefs.edit()
            previousSnapshots.forEach { (key, value) ->
                if (value == null) restoreSnapshots.remove(key) else restoreSnapshots.putString(key, value)
            }
            restoreSnapshots.commit()
            SliceBeam.CONFIG = previousConfig
            SliceBeam.INSTANCE = previousInstance
            if (previousConfigFile == null) configFile.delete() else configFile.writeBytes(previousConfigFile)
            TemporaryProjectSettings.clear()
            Prefs.setStoredFilamentSlots(previousSlots)
            modelFile.delete()
        }
    }

    @Test
    fun booleanDialogRejectsAStaleObjectSelectionWithoutChangingTheModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "boolean-stale-selection-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val current = requireNotNull(glView.renderer.model)
                current.addObject(current, 0)
                current.translate(1, 12.0, 0.0, 0.0)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("Boolean fixtures should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val beforeBounds = readExactBoundsOnGl(glView, 0, 1)
            val beforeRevision = model.geometryRevision

            openModelMenu(activity)
            selectAllObjectsScope()
            revealMenuAction(activity, R.string.ModelMenuBoolean)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuBoolean))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelBooleanUnion)).assertIsDisplayed()

            val changedSelection = CountDownLatch(1)
            glView.queueEvent {
                glView.renderer.setSelectedObject(1)
                glView.requestRender()
                changedSelection.countDown()
            }
            assertTrue("selection change should complete on the GL thread", changedSelection.await(10, TimeUnit.SECONDS))
            compose.onNodeWithText(activity.getString(R.string.ModelBooleanUnion)).performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelBooleanSelectionChanged) }

            assertEquals("a stale Boolean dialog must not replace either source object", 2,
                readObjectCountOnGl(glView))
            assertEquals("stale selection rejection must not mutate native geometry", beforeRevision,
                model.geometryRevision)
            beforeBounds.zip(readExactBoundsOnGl(glView, 0, 1)).forEachIndexed { index, (before, after) ->
                assertArrayEquals("stale Boolean selection must preserve object $index", before, after, 0.001)
            }
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun booleanDialogCancelAndAndroidBackPreserveModelGeometryMaterialsAndSelection() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "boolean-cancel-preserves-model-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val current = requireNotNull(glView.renderer.model)
                current.addObject(current, 0)
                current.translate(1, 12.0, 4.0, 3.0)
                current.setUniformFilament(0, 2)
                current.setUniformFilament(1, 3)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("Boolean sources should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            val beforeBounds = readExactBoundsOnGl(glView, 0, 1)
            val beforeFilaments = (0..1).map { readUsedFilamentsOnGl(glView, it).toList() }
            val beforeRevision = model.geometryRevision
            val selectedIndices = intArrayOf(0, 1)
            val cancelLabel = activity.getString(android.R.string.cancel)
            val unionLabel = activity.getString(R.string.ModelBooleanUnion)

            fun openBooleanDialog() {
                openModelMenu(activity)
                selectAllObjectsScope()
                revealMenuAction(activity, R.string.ModelMenuBoolean)
                compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuBoolean))
                    .assertIsDisplayed().assertIsEnabled().performClick()
                compose.onNodeWithText(unionLabel).assertIsDisplayed()
            }

            fun assertSourcesUnchanged() {
                assertEquals("cancel must retain both source objects", 2, readObjectCountOnGl(glView))
                assertEquals("cancel must not mutate model geometry", beforeRevision, model.geometryRevision)
                beforeBounds.zip(readExactBoundsOnGl(glView, 0, 1)).forEachIndexed { index, (before, after) ->
                    assertArrayEquals("cancel must preserve object $index bounds", before, after, 0.001)
                }
                assertEquals("cancel must retain source material assignments", beforeFilaments,
                    (0..1).map { readUsedFilamentsOnGl(glView, it).toList() })
                assertArrayEquals("cancel must retain the source selection", selectedIndices,
                    readSelectedIndicesOnGl(glView))
            }

            openBooleanDialog()
            compose.onNodeWithText(cancelLabel).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(unionLabel).fetchSemanticsNodes().isEmpty()
            }
            assertSourcesUnchanged()

            openBooleanDialog()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(unionLabel).fetchSemanticsNodes().isEmpty()
            }
            assertSourcesUnchanged()
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun objectListAssignmentAndDuplicateDialogsRejectStaleSelections() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "object-actions-stale-selection-test.stl")
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        val testSlots = listOf(
            FilamentSlot(0xFFCC3333.toInt(), "Stale Guard PLA"),
            FilamentSlot(0xFF3366CC.toInt(), "Stale Guard PETG"),
        )
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.translate(1, 16.0, 0.0, 0.0)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("object action fixtures should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            Prefs.setStoredFilamentSlots(testSlots)
            val actions = FileMenuObjectActions(bed)

            // Selecting a row from an object-list snapshot must not overwrite a newer selection.
            compose.runOnUiThread { actions.showObjectsDialog(glView) }
            compose.onNodeWithTag("model-object-row-0").assertIsDisplayed()
            val selectedOne = CountDownLatch(1)
            glView.queueEvent {
                glView.renderer.setSelectedObject(1)
                selectedOne.countDown()
            }
            assertTrue("object selection change should finish on GL", selectedOne.await(10, TimeUnit.SECONDS))
            compose.onNodeWithTag("model-object-row-0").performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }
            assertEquals("stale object-list selection must preserve the newer selection", 1, readSelectedObjectOnGl(glView))

            // Assigning an individual object's filament is also bound to the opening selection.
            clearWorkspaceSnackbars(bed)
            compose.runOnUiThread { actions.showObjectsDialog(glView) }
            compose.onAllNodesWithText(activity.getString(R.string.ObjectActionAssignFilamentShort))[0]
                .assertIsDisplayed().performClick()
            compose.onNodeWithTag("model-choice-option-0").assertIsDisplayed()
            val selectedZero = CountDownLatch(1)
            glView.queueEvent {
                glView.renderer.setSelectedObject(0)
                selectedZero.countDown()
            }
            assertTrue("second object selection change should finish on GL", selectedZero.await(10, TimeUnit.SECONDS))
            compose.onNodeWithTag("model-choice-option-0").performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }
            assertTrue("stale filament assignment must leave object 1 unchanged",
                model.getUsedFilaments(0).contentEquals(intArrayOf(1)))

            // A delayed duplicate confirmation must never duplicate the replacement selection.
            clearWorkspaceSnackbars(bed)
            compose.runOnUiThread { actions.showDuplicateDialog(glView) }
            compose.onNodeWithTag("duplicate-model-confirm").assertIsDisplayed()
            val selectedForDuplicate = CountDownLatch(1)
            glView.queueEvent {
                glView.renderer.setSelectedObject(1)
                selectedForDuplicate.countDown()
            }
            assertTrue("duplicate selection change should finish on GL", selectedForDuplicate.await(10, TimeUnit.SECONDS))
            compose.onNodeWithTag("duplicate-model-confirm").performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }
            assertEquals("stale duplicate confirmation must not add objects", 2, readObjectCountOnGl(glView))
            assertEquals("stale duplicate confirmation must preserve newer selection", 1, readSelectedObjectOnGl(glView))
        } finally {
            Prefs.setStoredFilamentSlots(previousSlots)
            modelFile.delete()
        }
    }

    @Test
    fun objectListAndDuplicateConfirmationStillApplyToTheirCapturedSelection() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "object-actions-current-selection-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val selectInitialObject = CountDownLatch(1)
            glView.queueEvent {
                glView.renderer.setSelectedObject(0)
                selectInitialObject.countDown()
            }
            assertTrue("initial object selection should finish on GL", selectInitialObject.await(10, TimeUnit.SECONDS))
            val actions = FileMenuObjectActions(bed)

            compose.runOnUiThread { actions.showObjectsDialog(glView) }
            compose.onNodeWithTag("model-object-row-0").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { readSelectedObjectOnGl(glView) == 0 }

            compose.runOnUiThread { actions.showDuplicateDialog(glView) }
            compose.onNodeWithTag("duplicate-model-confirm").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 30_000) { readObjectCountOnGl(glView) == 2 }
            assertEquals("the current confirmed selection should be duplicated once", 2, readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun selectedObjectActionDialogRejectsStaleSplitAndDelete() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "selected-object-actions-stale-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.translate(1, 18.0, 0.0, 0.0)
                glView.renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("two selected-action test objects should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            val actions = FileMenuObjectActions(bed)

            compose.runOnUiThread { actions.showSelectedObjectActions(glView) }
            compose.onNodeWithTag("model-choice-option-2").assertIsDisplayed()
            val changedForSplit = CountDownLatch(1)
            glView.queueEvent {
                glView.renderer.setSelectedObject(1)
                changedForSplit.countDown()
            }
            assertTrue("selection change before Split must complete on GL", changedForSplit.await(10, TimeUnit.SECONDS))
            compose.onNodeWithTag("model-choice-option-2").performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }
            assertEquals("stale Split must keep both objects", 2, readObjectCountOnGl(glView))
            assertEquals("stale Split must keep the replacement selection", 1, readSelectedObjectOnGl(glView))

            clearWorkspaceSnackbars(bed)
            compose.runOnUiThread { actions.showSelectedObjectActions(glView) }
            compose.onNodeWithTag("model-choice-option-3").assertIsDisplayed()
            val changedForDelete = CountDownLatch(1)
            glView.queueEvent {
                glView.renderer.setSelectedObject(0)
                changedForDelete.countDown()
            }
            assertTrue("selection change before Delete must complete on GL", changedForDelete.await(10, TimeUnit.SECONDS))
            compose.onNodeWithTag("model-choice-option-3").performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }
            assertEquals("stale Delete must keep both objects", 2, readObjectCountOnGl(glView))
            assertEquals("stale Delete must keep the replacement selection", 0, readSelectedObjectOnGl(glView))

            clearWorkspaceSnackbars(bed)
            compose.runOnUiThread { actions.showSelectedObjectActions(glView) }
            compose.onNodeWithTag("model-choice-option-3").performClick()
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            assertEquals("confirmed Delete should remove the captured object", 1, readObjectCountOnGl(glView))
            assertEquals("confirmed Delete should clear the selected object", -1, readSelectedObjectOnGl(glView))
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 2 }
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun selectedObjectActionMoveRejectsStaleSelectionBeforeChangingPlate() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "selected-object-actions-stale-move-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.translate(1, 18.0, 0.0, 0.0)
                glView.renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("two selected-action test objects should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }

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
            compose.waitUntil(timeoutMillis = 10_000) { bed.currentPlateIndex == 0 && readObjectCountOnGl(glView) == 2 }
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            compose.runOnUiThread { FileMenuObjectActions(bed).showSelectedObjectActions(glView) }
            compose.onNodeWithTag("model-choice-option-1").assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.PlateNumber, "2")).assertIsDisplayed()

            val changedSelection = CountDownLatch(1)
            glView.queueEvent {
                glView.renderer.setSelectedObject(1)
                changedSelection.countDown()
            }
            assertTrue("selection change before plate confirmation must complete on GL", changedSelection.await(10, TimeUnit.SECONDS))
            compose.onNodeWithText(activity.getString(R.string.PlateNumber, "2")).performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }
            assertEquals("stale move must stay on the source plate", 0, bed.currentPlateIndex)
            assertEquals("stale move must preserve both source objects", 2, readObjectCountOnGl(glView))

            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "1"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlatePickerSelectDescription, "2"),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { bed.currentPlateIndex == 1 }
            assertEquals("stale move must leave target plate empty", 0, readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun placeOnObjectDialogRejectsChangedSelectionWithoutMovingOrCombiningObjects() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "place-object-stale-selection-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.translate(1, 18.0, 0.0, 0.0)
                glView.renderer.setSelectedObject(1)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("placement objects should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val boundsBefore = readExactBoundsOnGl(glView, 0, 1)
            val revisionBefore = model.geometryRevision

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.ModelMenuPlaceOnObject)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuPlaceOnObject))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithTag("model-choice-option-0").assertIsDisplayed()

            val changedSelection = CountDownLatch(1)
            glView.queueEvent {
                glView.renderer.setSelectedObject(0)
                changedSelection.countDown()
            }
            assertTrue("selection change should complete on GL", changedSelection.await(10, TimeUnit.SECONDS))
            compose.onNodeWithTag("model-choice-option-0").performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }

            assertEquals("stale placement must keep both source objects", 2, readObjectCountOnGl(glView))
            assertEquals("stale placement must not mutate geometry", revisionBefore, model.geometryRevision)
            boundsBefore.zip(readExactBoundsOnGl(glView, 0, 1)).forEachIndexed { index, (before, after) ->
                assertArrayEquals("object $index must stay at its original bounds", before, after, 0.001)
            }
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun surfacePlacementCancelsWhenSelectionChangesBeforeThePick() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "surface-placement-stale-selection-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.translate(1, 24.0, 0.0, 0.0)
                glView.renderer.setSelectedObject(1)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("surface-placement objects should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val boundsBefore = readExactBoundsOnGl(glView, 0, 1)
            val revisionBefore = model.geometryRevision

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.ModelMenuPlaceOnSurface)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuPlaceOnSurface))
                .assertIsEnabled().performClick()
            compose.onNodeWithTag("model-choice-option-0").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { glView.renderer.isSurfacePlacementActive }

            val changedSelection = CountDownLatch(1)
            glView.queueEvent {
                glView.renderer.setSelectedObject(0)
                glView.renderer.onClick(100f, 100f)
                glView.requestRender()
                changedSelection.countDown()
            }
            assertTrue("stale surface-pick should be rejected on GL", changedSelection.await(10, TimeUnit.SECONDS))
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }

            assertFalse("stale surface placement must leave pick mode", glView.renderer.isSurfacePlacementActive)
            assertEquals("stale surface placement must preserve source objects", 2, readObjectCountOnGl(glView))
            assertEquals("stale surface placement must not mutate geometry", revisionBefore, model.geometryRevision)
            boundsBefore.zip(readExactBoundsOnGl(glView, 0, 1)).forEachIndexed { index, (before, after) ->
                assertArrayEquals("object $index must stay at its original bounds", before, after, 0.001)
            }
        } finally {
            if (glView.renderer.isSurfacePlacementActive) bed.onBackPressed()
            modelFile.delete()
        }
    }

    @Test
    fun alignmentAndDistributionDialogsRejectStaleModelSelections() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "group-dialog-stale-selection-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.addObject(model, 0)
                model.translate(1, 18.0, 0.0, 0.0)
                model.translate(2, 36.0, 0.0, 0.0)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1, 2))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("group-action objects should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.ModelMenuAlignSelected)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuAlignSelected))
                .assertIsEnabled().performClick()
            compose.onNodeWithTag("model-choice-option-0").assertIsDisplayed()
            val alignmentRevision = model.geometryRevision
            val alignmentBounds = readExactBoundsOnGl(glView, 0, 1, 2)

            val changeAlignmentSelection = CountDownLatch(1)
            glView.queueEvent {
                glView.renderer.setSelectedObject(2)
                changeAlignmentSelection.countDown()
            }
            assertTrue("alignment selection change should finish on GL", changeAlignmentSelection.await(10, TimeUnit.SECONDS))
            compose.onNodeWithTag("model-choice-option-0").performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }
            assertEquals("stale alignment must not change geometry", alignmentRevision, model.geometryRevision)
            alignmentBounds.zip(readExactBoundsOnGl(glView, 0, 1, 2)).forEachIndexed { index, (before, after) ->
                assertArrayEquals("alignment must preserve object $index", before, after, 0.001)
            }

            val restoreSelection = CountDownLatch(1)
            glView.queueEvent {
                glView.renderer.setSelectedObjects(intArrayOf(0, 1, 2))
                restoreSelection.countDown()
            }
            assertTrue("distribution selection should be restored on GL", restoreSelection.await(10, TimeUnit.SECONDS))
            compose.waitForIdle()
            revealMenuAction(activity, R.string.ModelMenuDistributeSelected)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDistributeSelected))
                .assertIsEnabled().performClick()
            compose.onNodeWithTag("model-choice-option-0").assertIsDisplayed()
            val distributionRevision = model.geometryRevision
            val distributionBounds = readExactBoundsOnGl(glView, 0, 1, 2)

            val changeDistributionGeometry = CountDownLatch(1)
            glView.queueEvent {
                model.translate(0, 0.0, 1.0, 0.0)
                changeDistributionGeometry.countDown()
            }
            assertTrue("geometry change should finish on GL", changeDistributionGeometry.await(10, TimeUnit.SECONDS))
            compose.onNodeWithTag("model-choice-option-0").performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }
            assertEquals("stale distribution must not add a second geometry change",
                distributionRevision + 1, model.geometryRevision)
            distributionBounds.zip(readExactBoundsOnGl(glView, 0, 1, 2)).forEachIndexed { index, (before, after) ->
                val expected = before.copyOf().also { if (index == 0) it[1] += 1.0; if (index == 0) it[4] += 1.0 }
                assertArrayEquals("distribution must preserve the externally moved object $index", expected, after, 0.001)
            }
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun combineConfirmationRejectsChangedSelectionWithoutCombiningObjects() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "combine-stale-confirmation-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.addObject(model, 0)
                model.translate(1, 15.0, 0.0, 0.0)
                model.translate(2, 30.0, 0.0, 0.0)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("combine fixtures should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val revisionBefore = model.geometryRevision
            val boundsBefore = readExactBoundsOnGl(glView, 0, 1, 2)

            openModelMenu(activity)
            selectAllObjectsScope()
            revealMenuAction(activity, R.string.ModelMenuCombineSelected)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuCombineSelected))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }

            val changed = CountDownLatch(1)
            glView.selectModels(intArrayOf(0, 2)) { changed.countDown() }
            assertTrue("selection change should finish on GL", changed.await(10, TimeUnit.SECONDS))
            compose.onNode(hasText(activity.getString(R.string.ModelMenuCombineSelected)) and hasClickAction())
                .performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }

            assertEquals("stale confirmation must preserve every source object", 3, readObjectCountOnGl(glView))
            assertEquals("stale confirmation must not mutate geometry", revisionBefore, model.geometryRevision)
            boundsBefore.zip(readExactBoundsOnGl(glView, 0, 1, 2)).forEachIndexed { index, (before, after) ->
                assertArrayEquals("stale confirmation must preserve object $index", before, after, 0.001)
            }
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun deleteSelectedConfirmationRejectsChangedSelectionWithoutDeletingObjects() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "delete-selection-stale-confirmation-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.translate(1, 15.0, 0.0, 0.0)
                glView.renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("delete fixtures should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val revisionBefore = model.geometryRevision
            val boundsBefore = readExactBoundsOnGl(glView, 0, 1)

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.ModelMenuDeleteSelected)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteSelected))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }

            val changed = CountDownLatch(1)
            glView.selectModels(intArrayOf(1)) { changed.countDown() }
            assertTrue("selection change should finish on GL", changed.await(10, TimeUnit.SECONDS))
            compose.onNode(hasText(activity.getString(R.string.ModelMenuDeleteSelected)) and hasClickAction())
                .performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }

            assertEquals("stale confirmation must preserve both objects", 2, readObjectCountOnGl(glView))
            assertEquals("stale confirmation must not mutate geometry", revisionBefore, model.geometryRevision)
            boundsBefore.zip(readExactBoundsOnGl(glView, 0, 1)).forEachIndexed { index, (before, after) ->
                assertArrayEquals("stale confirmation must preserve object $index", before, after, 0.001)
            }
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun deleteAllConfirmationRejectsChangedModelWithoutDeletingIt() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "delete-all-stale-confirmation-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            compose.runOnUiThread { bed.updateModel() }
            openModelMenu(activity)
            selectAllObjectsScope()
            revealMenuAction(activity, R.string.ModelMenuDeleteAll)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteAll))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }

            val mutated = CountDownLatch(1)
            glView.queueEvent {
                model.translate(0, 7.0, 0.0, 0.0)
                glView.requestRender()
                mutated.countDown()
            }
            assertTrue("test geometry change should finish on GL", mutated.await(10, TimeUnit.SECONDS))
            val revisionAfterTestMutation = model.geometryRevision
            compose.onNode(hasText(activity.getString(R.string.ModelMenuDeleteAll)) and hasClickAction())
                .performClick()
            compose.waitUntil(timeoutMillis = 10_000) { hasSnackbarTitle(bed, R.string.ModelSelectionChanged) }

            assertSame("stale delete-all must retain the same model", model, glView.renderer.model)
            assertEquals("stale delete-all must preserve the model object", 1, readObjectCountOnGl(glView))
            assertEquals("only the deliberate test mutation should change geometry", revisionAfterTestMutation,
                model.geometryRevision)
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
            assertArrayEquals("the source object should remain selected before opening the object list",
                intArrayOf(0), readSelectedIndicesOnGl(glView))

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
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                beforeCenter.zip(readExactBoundsOnGl(glView, 0, 1, 2, 3))
                    .all { (before, current) -> before.contentEquals(current) }
            }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                centeredBounds.zip(readExactBoundsOnGl(glView, 0, 1, 2, 3))
                    .all { (centered, current) -> centered.contentEquals(current) }
            }

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
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                centeredBounds.zip(readExactBoundsOnGl(glView, 0, 1, 2, 3))
                    .all { (centered, current) -> centered.contentEquals(current) }
            }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                droppedBounds.zip(readExactBoundsOnGl(glView, 0, 1, 2, 3))
                    .all { (dropped, current) -> dropped.contentEquals(current) }
            }
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun singleObjectBatchDuplicateCreatesRequestedCopiesWithUndoRedo() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "single-object-batch-duplicate-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.setUniformFilament(0, 2)
                glView.renderer.setSelectedObject(0)
                prepared.countDown()
            }
            assertTrue("single object should be selected on GL thread", prepared.await(10, TimeUnit.SECONDS))
            val added = AtomicReference<Int>()
            val completed = CountDownLatch(1)
            glView.duplicateSelectedModels(3) { copies ->
                added.set(copies)
                if (copies > 0) bed.updateModel()
                completed.countDown()
            }
            assertTrue("batch duplicate should complete", completed.await(20, TimeUnit.SECONDS))
            assertEquals("the requested number of copies should be created in one transaction", 3, added.get())
            assertEquals(4, readObjectCountOnGl(glView))
            (0..3).forEach { index ->
                assertEquals("each copy preserves the assigned source filament", listOf(2),
                    readUsedFilamentsOnGl(glView, index).toList())
            }

            compose.runOnUiThread { bed.undoModelEdit() }
            instrumentationWaitForGlIdle()
            assertEquals("undo removes the whole duplicate batch atomically", 1, readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.redoModelEdit() }
            instrumentationWaitForGlIdle()
            assertEquals("redo restores the complete batch", 4, readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun failedGroupDuplicateRestoresAllObjectsAndFilamentAssignments() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "group-duplicate-no-room-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val extra = Model()
                try {
                    extra.addObject(model, 0)
                    model.addObject(extra, 0)
                } finally {
                    extra.release()
                }
                model.translate(1, 18.0, 11.0, 6.0)
                model.scale(1, 40.0, 40.0, 1.0)
                model.setUniformFilament(0, 2)
                model.setUniformFilament(1, 3)
                glView.renderer.invalidateGlModel(0)
                glView.renderer.invalidateGlModel(1)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("unarrangeable group should be prepared on GL thread", prepared.await(10, TimeUnit.SECONDS))
            instrumentationWaitForGlIdle()
            val beforeBounds = (0..1).map { readExactBoundsOnGl(glView, it).single() }
            val beforeFilaments = (0..1).map { readUsedFilamentsOnGl(glView, it).toList() }
            val copies = AtomicReference<Int>()
            val done = CountDownLatch(1)
            glView.queueEvent {
                copies.set(glView.renderer.duplicateSelectedObjects(2))
                glView.requestRender()
                done.countDown()
            }
            assertTrue("group duplicate should return from GL thread", done.await(15, TimeUnit.SECONDS))
            assertEquals("an unarrangeable group batch must create no partial copies", 0, copies.get())
            assertEquals(2, readObjectCountOnGl(glView))
            (0..1).forEach { index ->
                assertArrayEquals("group rollback must restore object $index", beforeBounds[index],
                    readExactBoundsOnGl(glView, index).single(), 0.03)
                assertEquals("group rollback must preserve object $index filament", beforeFilaments[index],
                    readUsedFilamentsOnGl(glView, index).toList())
            }
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun duplicateBatchDoesNotKeepPartialCopiesWhenArrangeCannotFitTheModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-menu-duplicate-no-room-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.scale(0, 40.0, 40.0, 1.0)
                glView.renderer.invalidateGlModel(0)
                glView.renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("oversized source should be prepared on GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val before = readExactBoundsOnGl(glView, 0).single()

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.ModelMenuDuplicate)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuDuplicateShort))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithTag("duplicate-model-count").performTextClearance()
            compose.onNodeWithTag("duplicate-model-count").performTextInput("3")
            compose.onNodeWithTag("duplicate-model-confirm").performClick()

            instrumentationWaitForGlIdle()
            assertEquals("a failed batch must roll back every requested copy", 1, readObjectCountOnGl(glView))
            assertArrayEquals("the failed operation must preserve the source geometry and position",
                before, readExactBoundsOnGl(glView, 0).single(), 0.03)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun failedContextDuplicateKeepsRedoHistoryIntact() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-context-duplicate-redo-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            val duplicateCount = AtomicReference<Int>()
            val duplicated = CountDownLatch(1)
            glView.duplicateSelectedModelWithResult { count ->
                duplicateCount.set(count)
                if (count > 0) bed.updateModel()
                duplicated.countDown()
            }
            assertTrue("successful duplicate should finish", duplicated.await(15, TimeUnit.SECONDS))
            assertEquals(1, duplicateCount.get())
            assertEquals(2, readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.undoModelEdit() }
            instrumentationWaitForGlIdle()
            assertEquals("undo should restore the original model", 1, readObjectCountOnGl(glView))

            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val current = requireNotNull(glView.renderer.model)
                current.scale(0, 40.0, 40.0, 1.0)
                glView.renderer.invalidateGlModel(0)
                glView.renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("oversized model should be prepared", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.showSelectedModelContextMenu() }
            compose.onNodeWithText(activity.getString(R.string.ModelContextDuplicate))
                .assertIsDisplayed().performClick()
            instrumentationWaitForGlIdle()
            Thread.sleep(300)
            assertEquals("failed duplicate must not add an object", 1, readObjectCountOnGl(glView))

            compose.runOnUiThread { bed.redoModelEdit() }
            instrumentationWaitForGlIdle()
            assertEquals("failed duplicate must not clear the previous redo snapshot", 2,
                readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun fillBedRejectsOversizedObjectWithoutChangingItsPlacement() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-menu-fill-bed-no-room-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.scale(0, 40.0, 40.0, 1.0)
                glView.renderer.invalidateGlModel(0)
                glView.renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("oversized source should be prepared on GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val before = readExactBoundsOnGl(glView, 0).single()

            openModelMenu(activity)
            scrollMenuToIndex(4)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuFillBedShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

            compose.waitUntil(timeoutMillis = 30_000) { !glView.isFillBedInProgress }
            instrumentationWaitForGlIdle()
            assertEquals("an oversized object must not produce speculative copies", 1, readObjectCountOnGl(glView))
            assertArrayEquals("a rejected fill action must preserve the source geometry and position",
                before, readExactBoundsOnGl(glView, 0).single(), 0.03)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun fillBedCopiesPreserveFilamentsAndDistinctPlacementsWithUndoRedo() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-menu-fill-bed-positive-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val selectionReady = CountDownLatch(1)
            glView.queueEvent {
                model.setUniformFilament(0, 2)
                glView.renderer.setSelectedObject(0)
                glView.requestRender()
                selectionReady.countDown()
            }
            assertTrue("source object should be selected on the GL thread", selectionReady.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            assertEquals("fixture must start with one object", 1, readObjectCountOnGl(glView))

            openModelMenu(activity)
            scrollMenuToIndex(4)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuFillBedShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

            compose.waitUntil(timeoutMillis = 60_000) { !glView.isFillBedInProgress }
            instrumentationWaitForGlIdle()
            val filledCount = readObjectCountOnGl(glView)
            assertTrue("fill-bed must use remaining printable plate area", filledCount > 1)
            (0 until filledCount).forEach { index ->
                assertArrayEquals("copy $index must preserve the source filament assignment",
                    intArrayOf(2), readUsedFilamentsOnGl(glView, index))
            }
            val uniquePlacements = (0 until filledCount).map { index ->
                val translation = readTranslationOnGl(glView, index)
                listOf(translation.x, translation.y, translation.z)
            }.distinct()
            assertEquals("every copy should be arranged at a distinct position", filledCount,
                uniquePlacements.size)

            compose.runOnUiThread { bed.undoModelEdit() }
            instrumentationWaitForGlIdle()
            assertEquals("Undo must restore the single source object", 1, readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.redoModelEdit() }
            instrumentationWaitForGlIdle()
            assertEquals("Redo must restore the completed plate arrangement", filledCount,
                readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun fillBedArrangeFailureRollsBackCopiesAndExistingPositions() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-menu-fill-bed-arrange-rollback-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val blocker = Model()
                try {
                    blocker.addObject(model, 0)
                    model.addObject(blocker, 0)
                } finally {
                    blocker.release()
                }
                model.scale(1, 40.0, 40.0, 1.0)
                glView.renderer.invalidateGlModel(0)
                glView.renderer.invalidateGlModel(1)
                glView.renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("crowded plate should be prepared on GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val before = (0..1).map { readExactBoundsOnGl(glView, it).single() }

            openModelMenu(activity)
            scrollMenuToIndex(4)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuFillBedShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

            compose.waitUntil(timeoutMillis = 30_000) { !glView.isFillBedInProgress }
            instrumentationWaitForGlIdle()
            assertEquals("a failed native arrange must not leave speculative copies", 2, readObjectCountOnGl(glView))
            (0..1).forEach { index ->
                assertArrayEquals("failed arrange must restore object $index", before[index],
                    readExactBoundsOnGl(glView, index).single(), 0.03)
            }
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun arrangeFailureRestoresEveryObjectAndDoesNotCommitAChangedLayout() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "model-arrange-rollback-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val extra = Model()
                try {
                    extra.addObject(model, 0)
                    model.addObject(extra, 0)
                } finally {
                    extra.release()
                }
                model.translate(0, 29.0, 17.0, 4.0)
                model.translate(1, -24.0, -13.0, 9.0)
                model.scale(1, 40.0, 40.0, 1.0)
                glView.renderer.invalidateGlModel(0)
                glView.renderer.invalidateGlModel(1)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("unarrangeable plate should be prepared on GL thread", prepared.await(10, TimeUnit.SECONDS))
            instrumentationWaitForGlIdle()
            val before = (0..1).map { readExactBoundsOnGl(glView, it).single() }
            val arranged = AtomicReference<Boolean>()
            val done = CountDownLatch(1)
            glView.queueEvent {
                arranged.set(glView.renderer.arrangeObjects())
                glView.requestRender()
                done.countDown()
            }
            assertTrue("arrange result should return from GL thread", done.await(10, TimeUnit.SECONDS))
            assertEquals("oversized blocker should reject the full arrangement", false, arranged.get())
            assertEquals(2, readObjectCountOnGl(glView))
            (0..1).forEach { index ->
                assertArrayEquals("failed arrange must keep object $index unchanged", before[index],
                    readExactBoundsOnGl(glView, index).single(), 0.03)
            }
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
        val roundTripFile = File(activity.cacheDir, "model-menu-group-combine-test.3mf")
        val gcodeFile = File(activity.cacheDir, "model-menu-group-combine-test.gcode")
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        Prefs.setStoredFilamentSlots(listOf(
            FilamentSlot(0xFF303030.toInt(), "Join T0"),
            FilamentSlot(0xFF3366CC.toInt(), "Join T1"),
            FilamentSlot(0xFFF2F2F2.toInt(), "Join T2"),
        ))
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
            glView.queueEvent {
                val renderer = glView.renderer
                val model = requireNotNull(renderer.model)
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(0, min, max)
                val partWidth = max.x - min.x
                model.addObject(model, 0)
                model.translate(1, partWidth * 1.4, 0.0, 0.0)
                model.duplicateInstance(1, 0, Vec3d(partWidth * 1.4, 0.0, 0.0))
                model.addObject(model, 0)
                model.translate(2, partWidth * 4.2, 0.0, 0.0)
                model.setUniformFilament(0, 1)
                model.setUniformFilament(1, 2)
                model.setUniformFilament(2, 3)
                renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("multipart test objects should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            assertEquals("three selected source objects should exist before opening the list", 3,
                readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            selectSingleObjectScope()
            revealMenuAction(activity, R.string.WorkspaceObjects)
            compose.onNodeWithContentDescription(activity.getString(R.string.WorkspaceObjects))
                .assertIsDisplayed().performClick()
            compose.onNodeWithTag("model-objects-group-selection-toggle").performClick()
            compose.onNodeWithTag("model-object-row-1").performScrollTo().performClick()
            compose.onNodeWithTag("model-object-row-2").performScrollTo().performClick()
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
            assertArrayEquals("combining the selected group preserves all source materials",
                intArrayOf(1, 2, 3), requireNotNull(combinedFilaments.get()))

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 3 }
            assertEquals("Undo should restore the three separate source objects", 3,
                readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            assertArrayEquals("Redo should restore all multipart filament assignments", intArrayOf(1, 2, 3),
                readUsedFilamentsOnGl(glView, 0))

            // Verify split history before slicing allocates native processing memory. The app
            // intentionally disables snapshots above its native-heap safety threshold.
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.MenuToolbarSplit)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarSplit))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 15_000) { readObjectCountOnGl(glView) == 4 }
            // Split posts updateModel() to Main, which then queues its history snapshot on GL.
            // Wait for that GL-owned write before inspecting the snapshot; reading the deque on
            // Main immediately after the native split races the posted history update.
            compose.waitUntil(timeoutMillis = 10_000) {
                readLastUndoSnapshotObjectCountOnGl(bed, glView) == 1
            }
            assertEquals("split undo snapshot should restore the multipart joined object", 1,
                readLastUndoSnapshotObjectCountOnGl(bed, glView))
            assertEquals("splitting a multipart object should preserve each source filament", listOf(1, 2, 2, 3),
                (0 until 4).map { index -> readUsedFilamentsOnGl(glView, index).single() })
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            assertArrayEquals("Undo split must restore the joined multipart assembly", intArrayOf(1, 2, 3),
                readUsedFilamentsOnGl(glView, 0))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 4 }
            assertEquals("Redo split must restore all independent source bodies", listOf(1, 2, 2, 3),
                (0 until 4).map { index -> readUsedFilamentsOnGl(glView, index).single() })
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }

            val exportReady = CountDownLatch(1)
            val exportFailure = AtomicReference<Throwable?>()
            glView.queueEvent {
                try {
                    val model = requireNotNull(glView.renderer.model)
                    assertEquals("the joined parts should remain a single renderer object", 1, model.objectsCount)
                    assertArrayEquals("the joined object should expose all three part materials", intArrayOf(1, 2, 3),
                        model.getUsedFilaments(0))
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
                    check(clearSnapshots.commit())
                    SliceBeam.genCurrentConfig(model)
                    model.export3mf(configFile.absolutePath, roundTripFile.absolutePath)
                } catch (error: Throwable) {
                    exportFailure.set(error)
                } finally {
                    exportReady.countDown()
                }
            }
            assertTrue("joined multipart object should export from the active renderer",
                exportReady.await(60, TimeUnit.SECONDS))
            exportFailure.get()?.let { throw AssertionError("joined object 3MF export failed", it) }
            assertTrue("joined object should produce a non-empty 3MF project",
                roundTripFile.isFile && roundTripFile.length() > 0L)
            roundTrippedModel = Model(roundTripFile, 1)
            assertEquals("the joined parts should remain one object after 3MF reload", 1,
                roundTrippedModel!!.objectsCount)
            assertArrayEquals("all joined part filaments should survive 3MF reload", intArrayOf(1, 2, 3),
                roundTrippedModel!!.getUsedFilaments(0))
            SliceBeam.genCurrentConfig(roundTrippedModel!!)
            sliceResult = roundTrippedModel!!.slice(configFile.absolutePath, gcodeFile.absolutePath, null)
            assertTrue("the joined object should generate G-code", gcodeFile.isFile && gcodeFile.length() > 0L)
            val gcode = gcodeFile.readText()
            assertTrue("joined parts should produce deposited extrusion",
                Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
            val emittedTools = Regex("(?m)^T(\\d+)(?:\\s|$)").findAll(gcode)
                .map { it.groupValues[1].toInt() }.toSet()
            assertTrue("T0 from the first joined part should be used, tools=$emittedTools", 0 in emittedTools)
            assertTrue("T1 from the second joined part should be used, tools=$emittedTools", 1 in emittedTools)
            assertTrue("T2 from the third joined part should be used, tools=$emittedTools", 2 in emittedTools)
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
    fun projectExportAvailabilityTracksModelLoadedOnGlThread() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "project-export-gl-state-test.stl")
        try {
            openWorkspaceMenu(activity, WorkspaceDestination.PROJECT, R.string.MenuProject)
            revealMenuAction(activity, R.string.MenuFileExport3mf)
            val export = compose.onNodeWithContentDescription(activity.getString(R.string.MenuFileExport3mf))
            export.assertIsDisplayed().assertIsNotEnabled()

            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            compose.waitUntil(timeoutMillis = 5_000) {
                runCatching { export.assertIsEnabled() }.isSuccess
            }
            export.assertIsEnabled()
        } finally {
            modelFile.delete()
        }
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

            compose.waitUntil(timeoutMillis = 5_000) { capturedIntent.get() != null }
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
    fun projectMenuWaitsForAnAllocatedRendererBedToBecomeValidBeforeOpeningPicker() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val requestCode = MainActivity.REQUEST_CODE_OPEN_FILE
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }

        val originalRendererBed = AtomicReference<ru.ytkab0bp.slicebeam.slic3r.Bed3D?>()
        val invalidRendererBed = ru.ytkab0bp.slicebeam.slic3r.Bed3D()
        val bedReplaced = CountDownLatch(1)
        glView.queueEvent {
            originalRendererBed.set(glView.renderer.bed)
            glView.renderer.bed = invalidRendererBed
            bedReplaced.countDown()
        }
        try {
            assertTrue("the test must install an allocated but unconfigured GL bed",
                bedReplaced.await(10, TimeUnit.SECONDS))
            openWorkspaceMenu(activity, WorkspaceDestination.PROJECT, R.string.MenuProject)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.WorkspaceAddModelAction))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.WorkspaceAddModelAction))
                .assertIsDisplayed().performClick()

            // Allow the first GL readiness check to observe the invalid non-null Bed3D.
            activity.window.decorView.postDelayed({
                glView.queueEvent { glView.renderer.bed = originalRendererBed.get() }
            }, 350L)

            compose.waitUntil(timeoutMillis = 5_000) { capturedIntent.get() != null }
            assertEquals("the picker should open once the existing Bed3D becomes valid",
                Intent.ACTION_OPEN_DOCUMENT, requireNotNull(capturedIntent.get()).action)
        } finally {
            launchers[requestCode] = previousLauncher
            val restored = CountDownLatch(1)
            glView.queueEvent {
                originalRendererBed.get()?.let { glView.renderer.bed = it }
                invalidRendererBed.release()
                restored.countDown()
            }
            assertTrue("the test must restore the production bed", restored.await(10, TimeUnit.SECONDS))
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
            // FileMenu validates the print bed from the renderer's GL thread before it launches
            // the SAF picker, so the registered result launcher is intentionally asynchronous.
            compose.waitUntil(timeoutMillis = 5_000) { capturedIntent.get() != null }
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
    fun projectMenuImportsSelectedStepAfterConfirmingMeshingOptions() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val inputFile = File(activity.cacheDir, "menu-step-import-${System.nanoTime()}.step")
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
            compose.waitUntil(timeoutMillis = 5_000) { capturedIntent.get() != null }
            val request = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, request.action)
            assertEquals("*/*", request.type)

            val inputUri = FileProvider.getUriForFile(activity, authority, inputFile)
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_OK, Intent().setData(inputUri))
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag("step-import-confirm").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals("STEP geometry must wait for explicit meshing confirmation", 0,
                bed.currentModel?.objectsCount ?: 0)
            compose.onNodeWithTag("step-linear-deflection").performScrollTo().performTextReplacement("0.01")
            compose.onNodeWithTag("step-angular-deflection").performScrollTo().performTextReplacement("0.7")
            compose.onNodeWithTag("step-import-confirm").performClick()

            compose.waitUntil(timeoutMillis = 90_000) {
                val model = bed.currentModel
                model != null && model.objectsCount == 1 && model.getTriangleCount(0) > 0
            }
            assertTrue("The selected STEP must be copied into app-owned cache before native import",
                cachedImport.isFile && cachedImport.length() > 0L)
            assertTrue("The imported STEP must produce renderable triangles",
                requireNotNull(bed.currentModel).getTriangleCount(0) > 0)
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
            compose.waitUntil(timeoutMillis = 10_000) { capturedIntent.get() != null }
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
            compose.onNodeWithTag("step-angular-deflection").performScrollTo().assertIsDisplayed()
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
    fun selectedStepReturnedByDocumentPickerRestoresWorkspaceBeforeShowingOptions() {
        val activity = compose.activity
        val file = File(activity.cacheDir, "step-return-to-workspace-${System.nanoTime()}.step")
        val cachedImport = File(SliceBeam.getModelCacheDir(), file.name)
        try {
            InstrumentationRegistry.getInstrumentation().context.assets
                .open("step_cube_ccby4.step").use { source -> file.outputStream().use(source::copyTo) }
            val provider = activity.packageManager.getPackageInfo(
                activity.packageName,
                PackageManager.GET_PROVIDERS,
            ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
            val authority = requireNotNull(provider.authority).substringBefore(';')
            val uri = FileProvider.getUriForFile(activity, authority, file)

            activity.runOnUiThread {
                (activity.navigationDelegate as ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate)
                    .switchToSlot(ru.ytkab0bp.slicebeam.navigation.MobileNavigationPolicy.APP_SLOT)
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                activity.navigationDelegate.getCurrentFragment() !is BedFragment &&
                    activity.navigationDelegate.getCurrentFragment().getView()?.isAttachedToWindow == true
            }

            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(
                        activity,
                        MainActivity.REQUEST_CODE_OPEN_FILE,
                        Activity.RESULT_OK,
                        Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                    )
            }

            compose.waitUntil(timeoutMillis = 15_000) {
                activity.navigationDelegate.getCurrentFragment() is BedFragment &&
                    compose.onAllNodesWithTag("step-import-confirm").fetchSemanticsNodes().isNotEmpty()
            }
            val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
            assertEquals("STEP must not be loaded before the meshing dialog is confirmed",
                0, bed.currentModel?.objectsCount ?: 0)
            compose.onNodeWithTag("step-linear-deflection").assertIsDisplayed()
            compose.onNodeWithTag("step-angular-deflection").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("step-import-confirm").performClick()

            compose.waitUntil(timeoutMillis = 90_000) {
                val model = bed.currentModel
                model != null && model.objectsCount == 1 && model.getTriangleCount(0) > 0
            }
            assertTrue("The STEP returned from SAF must render after options are confirmed",
                requireNotNull(bed.currentModel).getTriangleCount(0) > 0)
        } finally {
            file.delete()
            cachedImport.delete()
        }
    }

    @Test
    fun readableStepFileUriImportsFromOwnedCacheAndNeverDeletesTheSource() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val source = File(activity.cacheDir, "readable-step-uri-${System.nanoTime()}.step")
        val cached = File(SliceBeam.getModelCacheDir(), source.name)
        try {
            InstrumentationRegistry.getInstrumentation().context.assets
                .open("step_cube_ccby4.step").use { input ->
                source.outputStream().use(input::copyTo)
            }
            val openStep = {
                activity.runOnUiThread {
                    MainActivity::class.java.getDeclaredMethod("loadFile", Uri::class.java)
                        .apply { isAccessible = true }
                        .invoke(activity, Uri.fromFile(source))
                }
                compose.waitUntil(timeoutMillis = 10_000) {
                    compose.onAllNodesWithTag("step-import-confirm").fetchSemanticsNodes().isNotEmpty()
                }
            }

            // Cancel proves cleanup only targets the app-owned copy, never the selected file URI.
            openStep()
            assertTrue("The original source must still be present while options are shown", source.isFile)
            assertTrue("STEP import should operate on an app-owned cache copy", cached.isFile)
            compose.onNodeWithTag("step-import-cancel").performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag("step-import-confirm").fetchSemanticsNodes().isEmpty()
            }
            assertTrue("Cancelling STEP import must preserve the original file", source.isFile)
            assertFalse("The cancelled app-owned copy should be cleaned up", cached.exists())

            // Confirm also imports through the cached copy while preserving the user's source.
            openStep()
            compose.onNodeWithTag("step-import-confirm").performClick()
            compose.waitUntil(timeoutMillis = 90_000) {
                val model = bed.currentModel
                model != null && model.objectsCount == 1 && model.getTriangleCount(0) > 0
            }
            assertTrue("Successful import must leave the original file untouched", source.isFile)
            assertTrue("The native importer should receive the cached STEP file", cached.isFile)
        } finally {
            source.delete()
            cached.delete()
        }
    }

    @Test
    fun externalStepDocumentUriShowsMeshingOptionsBeforeLoadingRenderableModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val file = File(activity.cacheDir, "external-step-view-${System.nanoTime()}.step")
        try {
            // STEP files opened from Android must work even if the app was last left on a global
            // page. External imports are routed back to the workspace, which owns the renderer
            // and the STEP settings dialog host.
            activity.runOnUiThread {
                (activity.navigationDelegate as ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate)
                    .switchToSlot(ru.ytkab0bp.slicebeam.navigation.MobileNavigationPolicy.APP_SLOT)
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                activity.navigationDelegate.getCurrentFragment() !is BedFragment &&
                    activity.navigationDelegate.getCurrentFragment().getView()?.isAttachedToWindow == true
            }
            InstrumentationRegistry.getInstrumentation().context.assets
                .open("step_cube_ccby4.step").use { source -> file.outputStream().use(source::copyTo) }
            val provider = activity.packageManager.getPackageInfo(
                activity.packageName,
                PackageManager.GET_PROVIDERS,
            ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
            val authority = requireNotNull(provider.authority).substringBefore(';')
            val uri = FileProvider.getUriForFile(activity, authority, file)
            // Exercise both external actions through the same production dispatcher used by
            // onCreate/onNewIntent. Cancel VIEW first to prove no geometry is loaded prematurely.
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod("handleExternalModelOpen", Intent::class.java)
                    .apply { isAccessible = true }
                    .invoke(activity, Intent(Intent.ACTION_VIEW).setData(uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag("step-import-confirm").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals("External STEP open must wait for the user's meshing choice",
                0, bed.currentModel?.objectsCount ?: 0)
            compose.onNodeWithTag("step-linear-deflection").assertIsDisplayed()
            compose.onNodeWithTag("step-angular-deflection").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("step-import-cancel").performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag("step-import-confirm").fetchSemanticsNodes().isEmpty()
            }
            assertEquals("Cancel must not load the source geometry", 0, bed.currentModel?.objectsCount ?: 0)

            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod("handleExternalModelOpen", Intent::class.java)
                    .apply { isAccessible = true }
                    .invoke(activity, Intent(Intent.ACTION_SEND).apply {
                        type = "model/step"
                        // Reproduce file providers that omit EXTRA_STREAM and expose the
                        // shared STEP URI only through ClipData.
                        clipData = ClipData.newRawUri("step model", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
            }
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag("step-import-confirm").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("step-import-confirm").performClick()

            compose.waitUntil(timeoutMillis = 90_000) {
                val model = bed.currentModel
                model != null && model.objectsCount == 1 && model.getTriangleCount(0) > 0
            }
            assertTrue("The externally opened STEP must become a renderable mesh",
                requireNotNull(bed.currentModel).getTriangleCount(0) > 0)
        } finally {
            file.delete()
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
            compose.waitUntil(timeoutMillis = 5_000) { capturedIntent.get() != null }
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
            // Compose semantics dispatch is sufficient here and avoids reconnecting UiAutomation
            // during instrumentation on Android 16, which can abort the entire test process.
            exportAction.performClick()
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
                        "root=$rootBounds",
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
            layersAction.performClick()
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceToolPanelAttached("slice-layer-preview") }
            compose.onNodeWithTag("slice-layers-panel").assertIsDisplayed()
            compose.onNodeWithTag("workspace-docked-tool-panel").assertIsDisplayed()
            val rendererBounds = compose.onNodeWithTag("workspace-renderer-area").fetchSemanticsNode().boundsInRoot
            val controlsBounds = compose.onNodeWithTag("workspace-docked-tool-panel").fetchSemanticsNode().boundsInRoot
            val separatedHorizontally = rendererBounds.right <= controlsBounds.left + 1f ||
                controlsBounds.right <= rendererBounds.left + 1f
            val separatedVertically = rendererBounds.bottom <= controlsBounds.top + 1f ||
                controlsBounds.bottom <= rendererBounds.top + 1f
            assertTrue(
                "The layer controls must not cover the build plate renderer; " +
                    "renderer=$rendererBounds controls=$controlsBounds",
                separatedHorizontally || separatedVertically,
            )
            val layerTitleBounds = compose.onNodeWithTag("slice-layers-title")
                .fetchSemanticsNode().boundsInRoot
            assertTrue(
                "The layer controls title must be inside its visible dock; " +
                    "title=$layerTitleBounds controls=$controlsBounds",
                layerTitleBounds.width > 0f && layerTitleBounds.height > 0f &&
                    layerTitleBounds.center.x in controlsBounds.left..controlsBounds.right &&
                    layerTitleBounds.center.y in controlsBounds.top..controlsBounds.bottom,
            )
            compose.onNodeWithTag("slice-layers-range-title").assertIsDisplayed()
            compose.onNodeWithTag("slice-preview-size-25").assertIsDisplayed().performClick()
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
            exportAction.performClick()
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
        viewport.queueEvent {
            requireNotNull(viewport.renderer.model).setUniformFilament(0, 2)
            viewport.requestRender()
        }
        instrumentationWaitForGlIdle()
        assertArrayEquals("source filament assignment should apply on the GL thread", intArrayOf(2),
            readUsedFilamentsOnGl(viewport, 0))
        compose.runOnUiThread { bed.updateModel() }
        instrumentationWaitForGlIdle()
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
        for (index in 0 until 3) {
            assertArrayEquals("duplicate $index should retain the source material", intArrayOf(2),
                readUsedFilamentsOnGl(viewport, index))
        }
        compose.runOnUiThread { bed.undoModelEdit() }
        compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(viewport) == 1 }
        assertArrayEquals("Undo duplication should restore the assigned source material", intArrayOf(2),
            readUsedFilamentsOnGl(viewport, 0))
        compose.runOnUiThread { bed.redoModelEdit() }
        compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(viewport) == 3 }
        for (index in 0 until 3) {
            assertArrayEquals("Redo duplication should restore material on copy $index", intArrayOf(2),
                readUsedFilamentsOnGl(viewport, index))
        }
        assertCurrentWorkspaceModelRoundTripsAndSlices("duplicate")
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
                model.setUniformFilament(0, 2)
                glView.requestRender()
            }
            instrumentationWaitForGlIdle()
            // Record the controlled starting placement so Undo returns here rather than to the
            // STL fixture's initial position.
            compose.runOnUiThread { bed.updateModel() }
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
            assertArrayEquals("Center on Bed must preserve the assigned filament", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                val restored = readTranslationOnGl(glView, 0)
                kotlin.math.abs(restored.x - translationBefore.x) < 0.01 &&
                    kotlin.math.abs(restored.y - translationBefore.y) < 0.01
            }
            assertArrayEquals("Undo Center on Bed must preserve the assigned filament", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                val restored = readTranslationOnGl(glView, 0)
                kotlin.math.abs(restored.x - translationAfter.x) < 0.01 &&
                    kotlin.math.abs(restored.y - translationAfter.y) < 0.01
            }
            assertArrayEquals("Redo Center on Bed must preserve the assigned filament", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
            assertCurrentWorkspaceModelRoundTripsAndSlices("center-on-bed")
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
    fun flatteningAnAngledFaceIsUndoableAndRedoable() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-flatten-history-test.stl")
        try {
            activity.assets.open("models/pyramid.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openSelectedObjectMenu(activity)
            scrollMenuToIndex(6)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuFlattenShort))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { readRendererBooleanOnGl(glView, "isInFlattenMode") }
            val rotationBefore = readRotationOnGl(glView, 0)

            val applied = AtomicReference(false)
            val completed = CountDownLatch(1)
            val failure = AtomicReference<Throwable?>()
            glView.queueEvent {
                try {
                    val renderer = glView.renderer
                    val flattenModeField = GLRenderer::class.java.getDeclaredField("isInFlattenMode")
                        .apply { isAccessible = true }
                    val planesField = GLRenderer::class.java.getDeclaredField("flattenPlanes")
                        .apply { isAccessible = true }
                    @Suppress("UNCHECKED_CAST")
                    val planes = planesField.get(renderer) as List<GLModel>
                    check(planes.isNotEmpty()) { "Flatten mode did not produce any face planes" }
                    val hits = ArrayList<GLModel.HitResult>()
                    planes.forEach { plane ->
                        plane.raycaster.raycast(
                            renderer, hits,
                            renderer.viewportWidth / 2f,
                            renderer.viewportHeight / 2f,
                        )
                    }
                    check(flattenModeField.getBoolean(renderer)) {
                        "raycast smoke check unexpectedly left Flatten mode"
                    }
                    // Plane 1 is a sloped face on the asymmetric pyramid fixture. The
                    // renderer's production tap path calls this same commit after picking.
                    applied.set(renderer.applyFlattenPlane(1))
                } catch (error: Throwable) {
                    failure.set(error)
                } finally {
                    completed.countDown()
                }
            }
            assertTrue("GL thread did not finish face-plane hit search", completed.await(30, TimeUnit.SECONDS))
            failure.get()?.let { throw AssertionError("Could not apply flatten rotation", it) }
            assertTrue("applying the angled plane should commit flatten rotation", applied.get())
            compose.waitUntil(timeoutMillis = 10_000) { readModelHistorySize(bed, "undoHistory") > 0 }
            val rotationAfter = readRotationOnGl(glView, 0)
            assertTrue("flattening should change the native object rotation",
                kotlin.math.abs(rotationAfter.x - rotationBefore.x) > 1e-4 ||
                    kotlin.math.abs(rotationAfter.y - rotationBefore.y) > 1e-4 ||
                    kotlin.math.abs(rotationAfter.z - rotationBefore.z) > 1e-4)

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                val rotation = readRotationOnGl(glView, 0)
                kotlin.math.abs(rotation.x - rotationBefore.x) < 1e-4 &&
                    kotlin.math.abs(rotation.y - rotationBefore.y) < 1e-4 &&
                    kotlin.math.abs(rotation.z - rotationBefore.z) < 1e-4
            }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                val rotation = readRotationOnGl(glView, 0)
                kotlin.math.abs(rotation.x - rotationAfter.x) < 1e-4 &&
                    kotlin.math.abs(rotation.y - rotationAfter.y) < 1e-4 &&
                    kotlin.math.abs(rotation.z - rotationAfter.z) < 1e-4
            }
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
            glView.selectModels(intArrayOf(0, 1)) { bed.updateModel() }
            instrumentationWaitForGlIdle()
            scrollMenuToIndex(5)
            revealMenuAction(activity, R.string.ModelMenuAutoOrientSelected)
            val beforeOrientSelected = model.geometryRevision
            val beforeSelectedOrientBounds = readExactBoundsOnGl(glView, 0, 1)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuAutoOrientSelected))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { model.geometryRevision >= beforeOrientSelected + 2 }
            assertTrue("Auto Orient Selected should update the selected group",
                model.geometryRevision >= beforeOrientSelected + 2)
            assertEquals("orientation actions must preserve the object count", 2, readObjectCountOnGl(glView))
            val afterSelectedOrientBounds = readExactBoundsOnGl(glView, 0, 1)
            beforeSelectedOrientBounds.zip(afterSelectedOrientBounds).forEachIndexed { index, (before, after) ->
                for (axis in 0..2) {
                    assertEquals("auto-orient should preserve object $index center on axis $axis",
                        (before[axis] + before[axis + 3]) / 2.0,
                        (after[axis] + after[axis + 3]) / 2.0, 0.02)
                }
            }
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                beforeSelectedOrientBounds.zip(readExactBoundsOnGl(glView, 0, 1))
                    .all { (before, current) -> before.contentEquals(current) }
            }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                afterSelectedOrientBounds.zip(readExactBoundsOnGl(glView, 0, 1))
                    .all { (after, current) -> after.contentEquals(current) }
            }
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
                model.setUniformFilament(0, 2)
                model.translate(0, 2_000.0, 2_000.0, 0.0)
                glView.requestRender()
            }
            instrumentationWaitForGlIdle()
            compose.runOnUiThread { bed.updateModel() }
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
            assertArrayEquals("Arrange must preserve the object's assigned filament", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                val restored = readTranslationOnGl(glView, 0)
                kotlin.math.abs(restored.x - before.x) < 0.01 && kotlin.math.abs(restored.y - before.y) < 0.01
            }
            val afterUndo = readTranslationOnGl(glView, 0)
            assertEquals("Undo after Arrange must restore the exact pre-arrange X", before.x, afterUndo.x, 0.01)
            assertEquals("Undo after Arrange must restore the exact pre-arrange Y", before.y, afterUndo.y, 0.01)
            assertArrayEquals("Undo Arrange must preserve the object's assigned filament", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                val restored = readTranslationOnGl(glView, 0)
                kotlin.math.abs(restored.x - after.x) < 0.01 && kotlin.math.abs(restored.y - after.y) < 0.01
            }
            val afterRedo = readTranslationOnGl(glView, 0)
            assertEquals("Redo after Arrange must restore the arranged X", after.x, afterRedo.x, 0.01)
            assertEquals("Redo after Arrange must restore the arranged Y", after.y, afterRedo.y, 0.01)
            assertArrayEquals("Redo Arrange must preserve the object's assigned filament", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
            assertCurrentWorkspaceModelRoundTripsAndSlices("arrange")
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
            val model = loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            glView.queueEvent {
                model.setUniformFilament(0, 2)
                glView.requestRender()
            }
            instrumentationWaitForGlIdle()
            compose.runOnUiThread { bed.updateModel() }

            openModelMenu(activity)
            scrollMenuToIndex(4)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuFillBedShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

            compose.waitUntil(timeoutMillis = 30_000) { readObjectCountOnGl(glView) > 1 }
            val resultingCount = readObjectCountOnGl(glView)
            assertTrue("Fill Bed should add at least one copy of the disposable test model", resultingCount > 1)
            assertTrue("Fill Bed must respect the renderer's hard object cap", resultingCount <= 256)
            for (index in 0 until resultingCount) {
                assertArrayEquals("Fill Bed copy $index must inherit the source filament", intArrayOf(2),
                    readUsedFilamentsOnGl(glView, index))
            }
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            assertArrayEquals("Undo Fill Bed must restore the source filament", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == resultingCount }
            for (index in 0 until resultingCount) {
                assertArrayEquals("Redo Fill Bed copy $index must retain its filament", intArrayOf(2),
                    readUsedFilamentsOnGl(glView, index))
            }
            // Filling a small cube plate creates dozens of printable instances. Round-trip the
            // complete generated project here; slice validation is covered by the focused
            // single-object and multi-object editing tests, avoiding a minutes-long stress slice.
            assertCurrentWorkspaceModelRoundTripsAndSlices("fill-bed", sliceModel = false)
        } finally {
            modelFile.delete()
        }
    }

    /** Export the actual post-action renderer model, reload the 3MF, and slice that reloaded model. */
    private fun assertCurrentWorkspaceModelRoundTripsAndSlices(
        name: String,
        sliceModel: Boolean = true,
        prepareForSlice: (Model) -> Unit = {},
    ) {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val projectFile = File(activity.cacheDir, "placement-$name-${System.nanoTime()}.3mf")
        val gcodeFile = File(activity.cacheDir, "placement-$name-${System.nanoTime()}.gcode")
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        val previousConfig = SliceBeam.CONFIG
        val previousInstance = SliceBeam.INSTANCE
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigFile = configFile.takeIf { it.isFile }?.readBytes()
        val prefs = requireNotNull(Prefs.getPrefs())
        val snapshotKeys = listOf("mmu_snapshot_printer", "mmu_snapshot")
        val previousSnapshots = snapshotKeys.associateWith { prefs.getString(it, null) }
        val objectCount = readObjectCountOnGl(glView)
        val expectedPositions = (0 until objectCount).map { readTranslationOnGl(glView, it) }
        val expectedFilaments = (0 until objectCount).map { readUsedFilamentsOnGl(glView, it) }
        var restoredModel: Model? = null
        var sliceResult: GCodeProcessorResult? = null
        try {
            assertTrue("$name should leave at least one printable object", objectCount > 0)
            assertTrue("$name should retain at least one assigned material",
                expectedFilaments.all { it.contentEquals(intArrayOf(2)) })
            SliceBeam.INSTANCE = activity.applicationContext as SliceBeam
            Prefs.init(activity.applicationContext as SliceBeam)
            Prefs.setStoredFilamentSlots(listOf(
                FilamentSlot(0xFF303030.toInt(), "Placement T0"),
                FilamentSlot(0xFF3366CC.toInt(), "Placement T1"),
                FilamentSlot(0xFFF2F2F2.toInt(), "Placement T2"),
            ))
            TemporaryProjectSettings.clear()
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
            check(clearSnapshots.commit())

            val exported = CountDownLatch(1)
            val exportFailure = AtomicReference<Throwable?>()
            glView.queueEvent {
                try {
                    val model = requireNotNull(glView.renderer.model)
                    assertEquals("$name renderer object count before export", objectCount, model.objectsCount)
                    SliceBeam.genCurrentConfig(model)
                    model.export3mf(configFile.absolutePath, projectFile.absolutePath)
                } catch (error: Throwable) {
                    exportFailure.set(error)
                } finally {
                    exported.countDown()
                }
            }
            assertTrue("$name renderer export should finish", exported.await(60, TimeUnit.SECONDS))
            exportFailure.get()?.let { throw AssertionError("$name 3MF export failed", it) }
            assertTrue("$name should create a non-empty 3MF", projectFile.isFile && projectFile.length() > 0L)

            restoredModel = Model(projectFile, 1)
            val restored = requireNotNull(restoredModel)
            assertEquals("$name 3MF should retain every object", objectCount, restored.objectsCount)
            for (index in 0 until objectCount) {
                val position = Vec3d()
                restored.getTranslation(index, position)
                assertEquals("$name reloaded object $index X placement", expectedPositions[index].x, position.x, 0.02)
                assertEquals("$name reloaded object $index Y placement", expectedPositions[index].y, position.y, 0.02)
                assertEquals("$name reloaded object $index Z placement", expectedPositions[index].z, position.z, 0.02)
                assertArrayEquals("$name reloaded object $index filament", expectedFilaments[index],
                    restored.getUsedFilaments(index))
            }
            if (sliceModel) {
                prepareForSlice(restored)
                assertTrue("$name should leave a printable sample after preparation", restored.objectsCount > 0)
                SliceBeam.genCurrentConfig(restored)
                sliceResult = restored.slice(configFile.absolutePath, gcodeFile.absolutePath, null)
                assertTrue("$name reloaded model should produce non-empty G-code",
                    gcodeFile.isFile && gcodeFile.length() > 0L)
                val gcode = gcodeFile.readText()
                assertTrue("$name G-code should contain deposited extrusion",
                    Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
                val emittedTools = Regex("(?m)^T(\\d+)(?:\\s|$)").findAll(gcode)
                    .map { it.groupValues[1].toInt() }.toSet()
                assertTrue("$name G-code should use assigned tool T1; found $emittedTools", 1 in emittedTools)
                assertFalse("$name G-code must not use the unassigned T2", 2 in emittedTools)
            }
        } finally {
            sliceResult?.release()
            restoredModel?.release()
            gcodeFile.delete()
            projectFile.delete()
            val restoreSnapshots = prefs.edit()
            previousSnapshots.forEach { (key, value) ->
                if (value == null) restoreSnapshots.remove(key) else restoreSnapshots.putString(key, value)
            }
            restoreSnapshots.commit()
            SliceBeam.CONFIG = previousConfig
            SliceBeam.INSTANCE = previousInstance
            if (previousConfigFile == null) configFile.delete() else configFile.writeBytes(previousConfigFile)
            TemporaryProjectSettings.clear()
            Prefs.setStoredFilamentSlots(previousSlots)
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
    fun modelMenuMoveSelectedGroupToAnotherPlatePreservesObjectsAndFilaments() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-move-selected-group-to-plate-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.addObject(model, 0)
                model.setUniformFilament(0, 2)
                model.setUniformFilament(1, 3)
                model.setUniformFilament(2, 4)
                model.translate(1, 42.0, 0.0, 0.0)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("both objects should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            assertEquals(3, readObjectCountOnGl(glView))
            val sourceBounds = readExactBoundsOnGl(glView, 0, 1)
            val unselectedBounds = readExactBoundsOnGl(glView, 2).single()
            assertArrayEquals(intArrayOf(2), readUsedFilamentsOnGl(glView, 0))
            assertArrayEquals(intArrayOf(3), readUsedFilamentsOnGl(glView, 1))
            assertArrayEquals(intArrayOf(4), readUsedFilamentsOnGl(glView, 2))

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
                bed.currentPlateIndex == 0 && readObjectCountOnGl(glView) == 3
            }
            glView.selectModels(intArrayOf(0, 1))
            compose.waitUntil(timeoutMillis = 5_000) {
                readSelectedIndicesOnGl(glView).contentEquals(intArrayOf(0, 1))
            }

            openModelMenu(activity)
            revealMenuAction(activity, R.string.ModelMenuMoveToPlate)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuMoveToPlateShort))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithText(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 15_000) {
                bed.currentPlateIndex == 1 && readObjectCountOnGl(glView) == 2 &&
                    readSelectedIndicesOnGl(glView).contentEquals(intArrayOf(0, 1))
            }

            assertEquals("moving a group must keep the workspace plate count", 2, bed.plateCount)
            assertArrayEquals("the first moved model keeps its filament", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
            assertArrayEquals("the second moved model keeps its own filament", intArrayOf(3),
                readUsedFilamentsOnGl(glView, 1))
            val targetBounds = readExactBoundsOnGl(glView, 0, 1)
            assertEquals("the transfer must preserve the objects' relative X placement",
                sourceBounds[1][0] - sourceBounds[0][0], targetBounds[1][0] - targetBounds[0][0], 0.03)

            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlatePickerSelectDescription, "1"),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.currentPlateIndex == 0 && readObjectCountOnGl(glView) == 1
            }
            assertArrayEquals("the unselected source-plate object keeps its filament", intArrayOf(4),
                readUsedFilamentsOnGl(glView, 0))
            assertArrayEquals("the unselected source-plate object keeps its position", unselectedBounds,
                readExactBoundsOnGl(glView, 0).single(), 0.03)
            glView.selectModels(intArrayOf(0))
            compose.waitUntil(timeoutMillis = 5_000) {
                readSelectedIndicesOnGl(glView).contentEquals(intArrayOf(0))
            }
            openModelMenu(activity)
            revealMenuAction(activity, R.string.ModelMenuMoveToPlate)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuMoveToPlateShort))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithText(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 15_000) {
                bed.currentPlateIndex == 1 && readObjectCountOnGl(glView) == 3 &&
                    readSelectedIndicesOnGl(glView).contentEquals(intArrayOf(2))
            }
            assertArrayEquals("moving one selected object preserves its filament", intArrayOf(4),
                readUsedFilamentsOnGl(glView, 2))
            assertArrayEquals("moving one selected object preserves its position", unselectedBounds,
                readExactBoundsOnGl(glView, 2).single(), 0.03)
            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlatePickerSelectDescription, "1"),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.currentPlateIndex == 0 && readObjectCountOnGl(glView) == 0
            }
            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "1"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlatePickerSelectDescription, "2"),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.currentPlateIndex == 1 && readObjectCountOnGl(glView) == 3
            }
            assertArrayEquals("both per-object material assignments survive a plate reload", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
            assertArrayEquals("the second material assignment survives a plate reload", intArrayOf(3),
                readUsedFilamentsOnGl(glView, 1))
            assertArrayEquals("the individually moved object survives a plate reload", intArrayOf(4),
                readUsedFilamentsOnGl(glView, 2))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun combiningObjectsOnSecondaryPlateKeepsHistoryAndOtherPlateUnchanged() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-combine-secondary-plate-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            val sourceBounds = readExactBoundsOnGl(glView, 0).single()
            val sourceFilaments = readUsedFilamentsOnGl(glView, 0)

            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "1"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuAddPlate))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.plateCount == 2 && bed.currentPlateIndex == 1
            }

            // Load a controlled model on the secondary plate. A second import creates a separate
            // object while avoiding self-copying a native model after a plate switch.
            loadSingleTestModel(bed, modelFile)
            loadSingleTestModel(bed, modelFile)
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 2 }

            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val model = requireNotNull(glView.renderer.model)
                model.setUniformFilament(0, 1)
                model.translate(1, 38.0, 0.0, 0.0)
                model.setUniformFilament(1, 2)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("secondary plate objects should be prepared on the GL thread",
                prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            assertEquals("both objects should remain on the secondary plate before combine", 2,
                readObjectCountOnGl(glView))

            openModelMenu(activity)
            selectAllObjectsScope()
            revealMenuAction(activity, R.string.ModelMenuCombineSelected)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuCombineSelected))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithTag("workspace-message-confirm").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 15_000) { readObjectCountOnGl(glView) == 1 }
            assertArrayEquals("secondary plate combine retains both source materials", intArrayOf(1, 2),
                readUsedFilamentsOnGl(glView, 0))
            val joinedBounds = readExactBoundsOnGl(glView, 0).single()
            assertTrue("joined parts should preserve the moved component's world extent",
                joinedBounds[3] - joinedBounds[0] > sourceBounds[3] - sourceBounds[0] + 30.0)

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 2 }
            assertArrayEquals("Undo restores the first secondary plate material", intArrayOf(1),
                readUsedFilamentsOnGl(glView, 0))
            assertArrayEquals("Undo restores the second secondary plate material", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 1))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }

            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlatePickerSelectDescription, "1"),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.currentPlateIndex == 0 && readObjectCountOnGl(glView) == 1
            }
            assertArrayEquals("the primary plate's original filament assignment remains unchanged",
                sourceFilaments, readUsedFilamentsOnGl(glView, 0))
            assertArrayEquals("the primary plate's original object bounds remain unchanged",
                sourceBounds, readExactBoundsOnGl(glView, 0).single(), 0.03)

            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "1"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlatePickerSelectDescription, "2"),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.currentPlateIndex == 1 && readObjectCountOnGl(glView) == 1
            }
            assertArrayEquals("the combined assembly and both materials survive returning to plate 2",
                intArrayOf(1, 2), readUsedFilamentsOnGl(glView, 0))
            assertArrayEquals("the combined assembly retains its position after plate switching",
                joinedBounds, readExactBoundsOnGl(glView, 0).single(), 0.03)
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

            compose.runOnUiThread { bed.undoModelEdit() }
            val undoWaitFailure = runCatching {
                compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            }.exceptionOrNull()
            if (undoWaitFailure != null) {
                val disabled = BedFragment::class.java.getDeclaredField("modelHistoryDisabledForMemory")
                    .apply { isAccessible = true }.getBoolean(bed)
                @Suppress("UNCHECKED_CAST")
                val undoHistory = BedFragment::class.java.getDeclaredField("undoHistory")
                    .apply { isAccessible = true }.get(bed) as java.util.ArrayDeque<Model>
                val stableSnapshot = BedFragment::class.java.getDeclaredField("stableModelSnapshot")
                    .apply { isAccessible = true }.get(bed) as? Model
                val diagnostics = "Delete All Undo timed out: historyDisabled=$disabled, " +
                    "undoSnapshots=${undoHistory.size}, stableObjects=${stableSnapshot?.objectsCount}, " +
                    "nativeHeap=${android.os.Debug.getNativeHeapAllocatedSize()}, " +
                    "rendererObjects=${readObjectCountOnGl(glView)}"
                android.util.Log.e("DeleteAllUndoDiag", diagnostics, undoWaitFailure)
                assertTrue(diagnostics, false)
            }
            assertEquals("Undo should restore the object removed by Delete All", 1,
                readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 0 }
            assertEquals("Redo should restore the empty state after Delete All", 0,
                readObjectCountOnGl(glView))
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
            val copiesAdded = CountDownLatch(1)
            val addedCopies = AtomicReference<Int>()
            glView.duplicateSelectedModels(2) {
                addedCopies.set(it)
                bed.updateModel()
                copiesAdded.countDown()
            }
            assertTrue("GL thread should create the two selected test copies", copiesAdded.await(10, TimeUnit.SECONDS))
            assertEquals(2, requireNotNull(addedCopies.get()))
            glView.selectModels(intArrayOf(0, 2))
            instrumentationWaitForGlIdle()
            assertEquals("the disposable test group should contain three objects", 3, readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.updateModel() }
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
            revealMenuAction(activity, R.string.ModelMenuDeleteSelected)
            val deleteSelected = compose.onNodeWithContentDescription(
                activity.getString(R.string.ModelMenuDeleteSelected),
            ).performScrollTo().assertIsDisplayed().assertIsEnabled()

            deleteSelected.performClick()
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteConfirm)).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).performClick()
            compose.waitForIdle()
            assertFalse("cancel should dismiss the selected-model confirmation", bed.isWorkspaceMessageDialogAttached())
            assertEquals("cancel should preserve all objects", 3, readObjectCountOnGl(glView))

            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteSelected))
                .performScrollTo().performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteConfirm)).assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            assertFalse("Back should dismiss the selected-model confirmation", bed.isWorkspaceMessageDialogAttached())
            assertEquals("Back should preserve all objects", 3, readObjectCountOnGl(glView))

            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteSelected)).performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteConfirm)).assertIsDisplayed()
            compose.onNode(
                hasText(activity.getString(R.string.ModelMenuDeleteSelected)) and hasClickAction(),
            ).performClick()
            compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
            val removed = CountDownLatch(1)
            val remainingCount = AtomicReference<Int>()
            glView.queueEvent {
                remainingCount.set(glView.renderer.model?.objectsCount ?: 0)
                removed.countDown()
            }
            assertTrue("GL thread should confirm selected-group deletion", removed.await(10, TimeUnit.SECONDS))
            assertEquals("confirm should delete both selected objects and keep the unselected one",
                1, requireNotNull(remainingCount.get()))

            val undoHistory = BedFragment::class.java.getDeclaredField("undoHistory")
                .apply { isAccessible = true }.get(bed) as java.util.ArrayDeque<*>
            assertTrue("a committed group deletion should be undoable", undoHistory.isNotEmpty())
            assertEquals("undo snapshot should contain the pre-delete group", 3,
                (undoHistory.peekLast() as Model).objectsCount)
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) != 1 }
            assertEquals("undo should restore the deleted selection", 3, readObjectCountOnGl(glView))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }

            // Deleting the final remaining object produces a null renderer model. Undo and Redo
            // must still round-trip between that empty state and the restored object.
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.ModelMenuDeleteSelected)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteSelected))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteConfirm)).assertIsDisplayed()
            compose.onNode(hasText(activity.getString(R.string.ModelMenuDeleteSelected)) and hasClickAction())
                .performClick()
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 0 }
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 0 }
            assertEquals("Redo should delete the final restored object again", 0,
                readObjectCountOnGl(glView))
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
            revealMenuAction(activity, R.string.MenuTransformScale)
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
            revealMenuAction(activity, R.string.ModelContextResetScale)
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
            glView.queueEvent {
                requireNotNull(glView.renderer.model).setUniformFilament(0, 2)
                glView.requestRender()
            }
            instrumentationWaitForGlIdle()
            compose.runOnUiThread { bed.updateModel() }
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val initialBounds = readExactBoundsOnGl(glView, 0).single()
            fun center(bounds: DoubleArray, axis: Int) = (bounds[axis] + bounds[axis + 3]) / 2.0
            val initialCenter = (0..2).map { center(initialBounds, it) }

            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.MenuTransformScale)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformScale))
                .performScrollTo().assertIsDisplayed().performClick()
            // The compact phone panel keeps all three scale axes and the mirror controls in
            // one scroll column. On shorter devices the mirror row starts below the viewport.
            compose.onNodeWithTag("scale-mirror-x").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("scale-mirror-y").assertIsDisplayed()
            compose.onNodeWithTag("scale-mirror-z").assertIsDisplayed()

            val undoHistory = BedFragment::class.java.getDeclaredField("undoHistory")
                .apply { isAccessible = true }.get(bed) as java.util.ArrayDeque<*>
            val initialUndoSize = undoHistory.size
            val mirrorButton = compose.onNodeWithTag("scale-mirror-x")
            compose.waitUntil(timeoutMillis = 10_000) {
                runCatching { mirrorButton.assertIsEnabled() }.isSuccess
            }
            mirrorButton.performScrollTo().assertIsDisplayed().assertIsEnabled().performTouchInput { click() }
            compose.waitUntil(timeoutMillis = 10_000) {
                hasExpectedLinearTransform(glView, 0, doubleArrayOf(-1.0, 1.0, 1.0)) &&
                    undoHistory.size > initialUndoSize
            }
            val afterXBounds = readExactBoundsOnGl(glView, 0).single()
            (0..2).forEach { axis -> assertEquals("X reflection keeps world center", initialCenter[axis], center(afterXBounds, axis), 0.01) }
            assertArrayEquals("X reflection preserves the object's filament", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                hasExpectedLinearTransform(glView, 0, doubleArrayOf(1.0, 1.0, 1.0))
            }
            assertArrayEquals("Undo reflection preserves the object's filament", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                hasExpectedLinearTransform(glView, 0, doubleArrayOf(-1.0, 1.0, 1.0))
            }
            assertArrayEquals("Redo reflection preserves the object's filament", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))

            compose.onNodeWithTag("scale-mirror-y").performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                hasExpectedLinearTransform(glView, 0, doubleArrayOf(-1.0, -1.0, 1.0))
            }
            compose.onNodeWithTag("scale-mirror-z").performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                hasExpectedLinearTransform(glView, 0, doubleArrayOf(-1.0, -1.0, -1.0))
            }
            assertArrayEquals("XYZ reflections preserve the object's filament", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
            assertTrue("mirrored object must remain renderable", readTriangleCountOnGl(glView, 0) > 0)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun mirrorPanelReflectsTheWholeSelectedGroupAndKeepsPlacementAndFilaments() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-mirror-group-test.stl")
        try {
            activity.assets.open("models/pyramid.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.translate(1, 37.0, 19.0, 0.0)
                model.setUniformFilament(0, 1)
                model.setUniformFilament(1, 2)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("both disposable objects should be prepared as a GL selection", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            val initialBounds = readExactBoundsOnGl(glView, 0, 1)
            val initialCenters = initialBounds.map { bounds ->
                (0..2).map { axis -> (bounds[axis] + bounds[axis + 3]) / 2.0 }
            }
            val initialFilaments = (0..1).map { readUsedFilamentsOnGl(glView, it).toList() }
            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.MenuTransformScale)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformScale))
                .performScrollTo().assertIsDisplayed().performClick()

            val undoHistory = BedFragment::class.java.getDeclaredField("undoHistory")
                .apply { isAccessible = true }.get(bed) as java.util.ArrayDeque<*>
            val initialUndoSize = undoHistory.size
            compose.onNodeWithTag("scale-mirror-x").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            val mirrorApplied = runCatching {
                compose.waitUntil(timeoutMillis = 10_000) {
                    (0..1).all { objectIndex ->
                        hasExpectedLinearTransform(glView, objectIndex, doubleArrayOf(-1.0, 1.0, 1.0))
                    }
                }
            }.isSuccess
            val historyRecorded = runCatching {
                compose.waitUntil(timeoutMillis = 10_000) { undoHistory.size > initialUndoSize }
            }.isSuccess
            assertTrue(
                "group X mirror should apply and be undoable; applied=$mirrorApplied, " +
                    "historyRecorded=$historyRecorded, history=${undoHistory.size}/$initialUndoSize, " +
                    "selection=${readSelectedIndicesOnGl(glView).contentToString()}, " +
                    "mirrors=${(0..1).map { index -> readVectorOnGl(glView, index) { model, vector -> model.getMirror(index, vector) }.let { doubleArrayOf(it.x, it.y, it.z).contentToString() } }}, " +
                    "scales=${(0..1).map { index -> readScaleOnGl(glView, index).let { doubleArrayOf(it.x, it.y, it.z).contentToString() } }}, " +
                    "rotations=${(0..1).map { index -> readRotationOnGl(glView, index).let { doubleArrayOf(it.x, it.y, it.z).contentToString() } }}",
                mirrorApplied && historyRecorded,
            )
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                (0..1).all { objectIndex ->
                    hasExpectedLinearTransform(glView, objectIndex, doubleArrayOf(1.0, 1.0, 1.0))
                }
            }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) {
                (0..1).all { objectIndex ->
                    hasExpectedLinearTransform(glView, objectIndex, doubleArrayOf(-1.0, 1.0, 1.0))
                }
            }
            compose.onNodeWithTag("scale-mirror-y").performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                (0..1).all { objectIndex ->
                    hasExpectedLinearTransform(glView, objectIndex, doubleArrayOf(-1.0, -1.0, 1.0))
                }
            }
            compose.onNodeWithTag("scale-mirror-z").performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                (0..1).all { objectIndex ->
                    hasExpectedLinearTransform(glView, objectIndex, doubleArrayOf(-1.0, -1.0, -1.0))
                }
            }

            val finalBounds = readExactBoundsOnGl(glView, 0, 1)
            finalBounds.forEachIndexed { objectIndex, bounds ->
                (0..2).forEach { axis ->
                    assertEquals(
                        "mirroring must retain object $objectIndex's world-space center on axis $axis",
                        initialCenters[objectIndex][axis], (bounds[axis] + bounds[axis + 3]) / 2.0, 0.05,
                    )
                }
            }
            assertEquals("all selected objects must keep their own filament assignment",
                initialFilaments, (0..1).map { readUsedFilamentsOnGl(glView, it).toList() })
            assertEquals("group mirroring must not add or remove objects", 2, readObjectCountOnGl(glView))
            assertTrue("both mirrored objects must remain renderable",
                (0..1).all { readTriangleCountOnGl(glView, it) > 0 })
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
            val afterX = (0..1).map { readTranslationOnGl(glView, it) }
            enterAxisValue("Y", before[0].y + 13.0, confirm = true)
            val afterY = (0..1).map { readTranslationOnGl(glView, it) }
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

            fun assertGroupTranslations(expected: List<Vec3d>, action: String) {
                compose.waitUntil(timeoutMillis = 10_000) {
                    (0..1).all { index ->
                        val actual = readTranslationOnGl(glView, index)
                        kotlin.math.abs(actual.x - expected[index].x) < 0.02 &&
                            kotlin.math.abs(actual.y - expected[index].y) < 0.02 &&
                            kotlin.math.abs(actual.z - expected[index].z) < 0.02
                    }
                }
                (0..1).forEach { index ->
                    val actual = readTranslationOnGl(glView, index)
                    assertEquals("$action should restore object $index X", expected[index].x, actual.x, 0.02)
                    assertEquals("$action should restore object $index Y", expected[index].y, actual.y, 0.02)
                    assertEquals("$action should restore object $index Z", expected[index].z, actual.z, 0.02)
                }
            }

            compose.runOnUiThread { bed.undoModelEdit() }
            assertGroupTranslations(afterY, "Undo Z")
            compose.runOnUiThread { bed.undoModelEdit() }
            assertGroupTranslations(afterX, "Undo Y")
            compose.runOnUiThread { bed.undoModelEdit() }
            assertGroupTranslations(before, "Undo X")

            compose.runOnUiThread { bed.redoModelEdit() }
            assertGroupTranslations(afterX, "Redo X")
            compose.runOnUiThread { bed.redoModelEdit() }
            assertGroupTranslations(afterY, "Redo Y")
            compose.runOnUiThread { bed.redoModelEdit() }
            assertGroupTranslations(after, "Redo Z")
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuAlignsSelectedGroupOnXAndYCentersAndZBaseWithUndoRedo() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "menu-group-align-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val model = requireNotNull(glView.renderer.model)
                model.addObject(model, 0)
                model.translate(1, 43.0, 29.0, 18.0)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("two objects should be prepared for alignment on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            fun bounds() = readExactBoundsOnGl(glView, 0, 1)
            fun centersX(value: List<DoubleArray>) = value.map { (it[0] + it[3]) / 2.0 }
            fun centersY(value: List<DoubleArray>) = value.map { (it[1] + it[4]) / 2.0 }
            fun invokeAlignment(label: Int) {
                openSelectedObjectMenu(activity)
                revealMenuAction(activity, R.string.ModelMenuAlignSelected)
                compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuAlignSelected))
                    .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
                val option = activity.getString(label)
                compose.onNodeWithText(option).assertIsDisplayed().performClick()
                compose.waitUntil(timeoutMillis = 10_000) {
                    val current = bounds()
                    when (label) {
                        R.string.ModelMenuAlignCenterX -> kotlin.math.abs(centersX(current)[0] - centersX(current)[1]) < 0.02
                        R.string.ModelMenuAlignCenterY -> kotlin.math.abs(centersY(current)[0] - centersY(current)[1]) < 0.02
                        else -> kotlin.math.abs(current[0][2] - current[1][2]) < 0.02
                    }
                }
            }

            val initial = bounds()
            invokeAlignment(R.string.ModelMenuAlignCenterX)
            val afterX = bounds()
            assertEquals("X alignment must only change the secondary object's X center",
                centersX(afterX)[0], centersX(afterX)[1], 0.02)
            assertEquals("X alignment must preserve Y", centersY(initial)[1], centersY(afterX)[1], 0.02)
            assertEquals("X alignment must preserve Z", initial[1][2], afterX[1][2], 0.02)
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { bounds().zip(initial).all { (a, b) -> a.contentEquals(b) } }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { kotlin.math.abs(centersX(bounds())[0] - centersX(bounds())[1]) < 0.02 }

            val beforeY = bounds()
            invokeAlignment(R.string.ModelMenuAlignCenterY)
            val afterY = bounds()
            assertEquals("Y alignment must preserve X", centersX(beforeY)[1], centersX(afterY)[1], 0.02)
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { kotlin.math.abs(centersY(bounds())[0] - centersY(bounds())[1]) > 0.02 }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { kotlin.math.abs(centersY(bounds())[0] - centersY(bounds())[1]) < 0.02 }

            val beforeZ = bounds()
            invokeAlignment(R.string.ModelMenuAlignBaseZ)
            val afterZ = bounds()
            assertEquals("Z-base alignment must preserve X", centersX(beforeZ)[1], centersX(afterZ)[1], 0.02)
            assertEquals("Z-base alignment must preserve Y", centersY(beforeZ)[1], centersY(afterZ)[1], 0.02)
            assertEquals("Z-base alignment must align both minimum heights", afterZ[0][2], afterZ[1][2], 0.02)
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { kotlin.math.abs(beforeZ[0][2] - bounds()[0][2]) < 0.02 }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { kotlin.math.abs(bounds()[0][2] - bounds()[1][2]) < 0.02 }
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuDistributesSelectedGroupOnXYZWithUndoRedoAndWarnsForZ() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "menu-group-distribute-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                val model = requireNotNull(glView.renderer.model)
                val min = ru.ytkab0bp.slicebeam.utils.Vec3d()
                val max = ru.ytkab0bp.slicebeam.utils.Vec3d()
                model.getBoundingBoxExact(0, min, max)
                val size = doubleArrayOf(max.x - min.x, max.y - min.y, max.z - min.z)
                model.addObject(model, 0)
                model.addObject(model, 0)
                model.translate(1, size[0] * 2.0, size[1] * 2.0, size[2] * 2.0)
                model.translate(2, size[0] * 6.0, size[1] * 6.0, size[2] * 6.0)
                glView.renderer.setSelectedObjects(intArrayOf(0, 1, 2))
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("three objects should be prepared on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            fun bounds() = readExactBoundsOnGl(glView, 0, 1, 2)
            fun start(item: DoubleArray, axis: Int) = item[axis]
            fun end(item: DoubleArray, axis: Int) = item[axis + 3]
            val axes = listOf(
                R.string.ModelMenuDistributeX to 0,
                R.string.ModelMenuDistributeY to 1,
                R.string.ModelMenuDistributeZ to 2,
            )
            for ((label, axis) in axes) {
                val before = bounds()
                openSelectedObjectMenu(activity)
                revealMenuAction(activity, R.string.ModelMenuDistributeSelected)
                compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDistributeSelected))
                    .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
                compose.onNodeWithText(activity.getString(R.string.ModelMenuDistributeHint)).assertIsDisplayed()
                compose.onNodeWithTag("model-choice-dialog-options").performScrollToIndex(axis)
                compose.onNodeWithText(activity.getString(label))
                    .performScrollTo().assertIsDisplayed().performClick()
                compose.waitUntil(timeoutMillis = 10_000) {
                    val current = bounds()
                    val firstGap = start(current[1], axis) - end(current[0], axis)
                    val secondGap = start(current[2], axis) - end(current[1], axis)
                    kotlin.math.abs(firstGap - secondGap) < 0.02 && firstGap >= -0.02
                }
                val after = bounds()
                assertEquals("distribution must preserve first outer object on axis $axis",
                    start(before[0], axis), start(after[0], axis), 0.02)
                assertEquals("distribution must preserve last outer object on axis $axis",
                    end(before[2], axis), end(after[2], axis), 0.02)
                for (index in 0..2) for (otherAxis in 0..2) {
                    if (otherAxis != axis) {
                        assertEquals("distribution on $axis must preserve axis $otherAxis for object $index",
                            before[index][otherAxis], after[index][otherAxis], 0.02)
                    }
                }
                compose.runOnUiThread { bed.undoModelEdit() }
                compose.waitUntil(timeoutMillis = 10_000) {
                    val current = bounds()
                    current.indices.all { index -> (0..5).all { component ->
                        kotlin.math.abs(current[index][component] - before[index][component]) < 0.02
                    } }
                }
                compose.runOnUiThread { bed.redoModelEdit() }
                compose.waitUntil(timeoutMillis = 10_000) {
                    val current = bounds()
                    val firstGap = start(current[1], axis) - end(current[0], axis)
                    val secondGap = start(current[2], axis) - end(current[1], axis)
                    kotlin.math.abs(firstGap - secondGap) < 0.02
                }
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
            revealMenuAction(activity, R.string.MenuTransformScale)
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
    fun selectedGroupRotationAndScaleSurviveThreeMfReloadAndProduceGcode() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "group-transform-slice-test.stl")
        val projectFile = File(activity.cacheDir, "group-transform-slice-test.3mf")
        val gcodeFile = File(activity.cacheDir, "group-transform-slice-test.gcode")
        SliceBeam.INSTANCE = activity.applicationContext as SliceBeam
        Prefs.init(activity.applicationContext as SliceBeam)
        TemporaryProjectSettings.clear()
        val previousConfig = SliceBeam.CONFIG
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigFile = configFile.takeIf { it.isFile }?.readBytes()
        val prefs = requireNotNull(Prefs.getPrefs())
        val snapshotKeys = listOf("mmu_snapshot_printer", "mmu_snapshot")
        val previousSnapshots = snapshotKeys.associateWith { prefs.getString(it, null) }
        var restoredModel: Model? = null
        var sliceResult: GCodeProcessorResult? = null
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)

            val prepared = CountDownLatch(1)
            val prepareFailure = AtomicReference<Throwable?>()
            glView.queueEvent {
                try {
                    val renderer = glView.renderer
                    val model = requireNotNull(renderer.model)
                    val min = Vec3d(); val max = Vec3d()
                    model.getBoundingBoxExact(0, min, max)
                    val gap = (max.x - min.x) * 2.5
                    model.addObject(model, 0)
                    model.translate(1, gap, 0.0, 0.0)
                    model.setUniformFilament(0, 1)
                    model.setUniformFilament(1, 2)
                    renderer.setSelectedObjects(intArrayOf(0, 1))
                    val rotated = renderer.rotateSelectedObjects(0.0, 0.0, Math.toRadians(37.0))
                    val scaled = rotated && renderer.scaleSelectedObjects(1.2, 1.2, 1.2)
                    check(scaled) { "selected group rotation and scale should apply" }
                    glView.requestRender()
                } catch (error: Throwable) {
                    prepareFailure.set(error)
                } finally {
                    prepared.countDown()
                }
            }
            assertTrue("group transform should complete on the GL thread", prepared.await(30, TimeUnit.SECONDS))
            prepareFailure.get()?.let { throw AssertionError("group transform failed", it) }
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()

            val exportReady = CountDownLatch(1)
            val exportFailure = AtomicReference<Throwable?>()
            glView.queueEvent {
                try {
                    val model = requireNotNull(glView.renderer.model)
                    assertEquals(2, model.objectsCount)
                    assertEquals(listOf(1), model.getUsedFilaments(0).toList())
                    assertEquals(listOf(2), model.getUsedFilaments(1).toList())
                    for (index in 0 until model.objectsCount) {
                        val min = Vec3d(); val max = Vec3d()
                        model.getBoundingBoxExact(index, min, max)
                        assertTrue("transformed object must have finite printable bounds",
                            listOf(min.x, min.y, min.z, max.x, max.y, max.z).all { it.isFinite() } && max.z > min.z)
                    }
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
                    check(clearSnapshots.commit())
                    SliceBeam.genCurrentConfig(model)
                    model.export3mf(configFile.absolutePath, projectFile.absolutePath)
                } catch (error: Throwable) {
                    exportFailure.set(error)
                } finally {
                    exportReady.countDown()
                }
            }
            assertTrue("transformed group should export from the active renderer model",
                exportReady.await(60, TimeUnit.SECONDS))
            exportFailure.get()?.let { throw AssertionError("group transformation 3MF export failed", it) }
            assertTrue("3MF export should create a project", projectFile.isFile && projectFile.length() > 0L)

            restoredModel = Model(projectFile, 1)
            assertEquals("both transformed objects should survive 3MF reload", 2, restoredModel!!.objectsCount)
            assertEquals("first object's filament assignment should survive reload", listOf(1),
                restoredModel!!.getUsedFilaments(0).toList())
            assertEquals("second object's filament assignment should survive reload", listOf(2),
                restoredModel!!.getUsedFilaments(1).toList())
            val firstBounds = DoubleArray(6).also { bounds ->
                val min = Vec3d(); val max = Vec3d()
                restoredModel!!.getBoundingBoxExact(0, min, max)
                bounds[0] = min.x; bounds[1] = min.y; bounds[2] = min.z
                bounds[3] = max.x; bounds[4] = max.y; bounds[5] = max.z
            }
            assertTrue("reloaded group object should remain above the build plate", firstBounds[2] >= -0.05)
            SliceBeam.genCurrentConfig(restoredModel!!)
            sliceResult = restoredModel!!.slice(configFile.absolutePath, gcodeFile.absolutePath, null)
            assertTrue("reloaded transformed group should produce non-empty G-code",
                gcodeFile.isFile && gcodeFile.length() > 0L)
            val gcode = gcodeFile.readText()
            assertTrue("reloaded transformed group should produce deposited extrusion",
                Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
            assertTrue("both object tool assignments should reach G-code",
                Regex("(?m)^T0(?:\\s|$)").containsMatchIn(gcode) &&
                    Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            sliceResult?.release()
            restoredModel?.release()
            gcodeFile.delete()
            projectFile.delete()
            val restoreSnapshots = prefs.edit()
            previousSnapshots.forEach { (key, value) ->
                if (value == null) restoreSnapshots.remove(key) else restoreSnapshots.putString(key, value)
            }
            restoreSnapshots.commit()
            SliceBeam.CONFIG = previousConfig
            if (previousConfigFile == null) configFile.delete() else configFile.writeBytes(previousConfigFile)
            TemporaryProjectSettings.clear()
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
            revealMenuAction(activity, R.string.MenuTransformCut)
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
            revealMenuAction(activity, R.string.MenuToolbarVariableLayerHeight)
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
            revealMenuAction(activity, R.string.MenuToolbarSplit)
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
    fun adaptiveLayerHeightApplyAndClearSupportUndoRedo() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-adaptive-layer-height-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            assertFalse("the fixture should begin with constant layer height",
                readAdaptiveLayerHeightOnGl(glView, 0))

            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.MenuToolbarVariableLayerHeight)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarVariableLayerHeight))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.VariableLayerTitle)).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(R.string.VariableLayerAdaptive))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 30_000) { readAdaptiveLayerHeightOnGl(glView, 0) }

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { !readAdaptiveLayerHeightOnGl(glView, 0) }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readAdaptiveLayerHeightOnGl(glView, 0) }

            compose.onNodeWithText(activity.getString(R.string.ActionClear))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { !readAdaptiveLayerHeightOnGl(glView, 0) }
            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readAdaptiveLayerHeightOnGl(glView, 0) }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { !readAdaptiveLayerHeightOnGl(glView, 0) }
            assertSame("adaptive profile edits should retain the renderer host", glView, bed.glView)
        } finally {
            if (bed.isVariableLayerHeightMode) bed.onBackPressed()
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
            glView.queueEvent {
                requireNotNull(glView.renderer.model).setUniformFilament(0, 2)
                glView.requestRender()
            }
            instrumentationWaitForGlIdle()
            compose.runOnUiThread { bed.updateModel() }
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.MenuTransformCut)
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

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 2 }
            assertArrayEquals("cut half 0 should retain the source material", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 0))
            assertArrayEquals("cut half 1 should retain the source material", intArrayOf(2),
                readUsedFilamentsOnGl(glView, 1))
            assertCurrentWorkspaceModelRoundTripsAndSlices("cut") { cutModel ->
                // The upper cut half is intentionally floating at this horizontal cut plane.
                // Keep the lower, bed-contacting half for a valid G-code check after verifying
                // that both halves survived Undo/Redo and the 3MF round trip above.
                fun lowerZ(index: Int): Double {
                    val min = Vec3d(); val max = Vec3d()
                    cutModel.getBoundingBoxExact(index, min, max)
                    return min.z
                }
                val lowerHalf = if (lowerZ(0) <= lowerZ(1)) 0 else 1
                cutModel.deleteObject(1 - lowerHalf)
            }
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
            revealMenuAction(activity, R.string.MenuTransformCut)
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
    fun cutConnectorEditingStaysBoundToItsObjectAndRejectsStaleCut() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelFile = File(activity.cacheDir, "menu-cut-connector-selection-binding.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.translate(1, 30.0, 0.0, 0.0)
                glView.renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("two cut targets should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            compose.runOnUiThread { bed.updateModel() }
            instrumentationWaitForGlIdle()
            val originalTriangles = intArrayOf(
                readTriangleCountOnGl(glView, 0),
                readTriangleCountOnGl(glView, 1),
            )

            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.MenuTransformCut)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformCut))
                .performScrollTo().assertIsDisplayed().performClick()
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

            // Exercise Undo before checking that later selection changes cannot redirect the preview.
            compose.onNodeWithText(activity.getString(R.string.CutUndoConnector))
                .performScrollTo().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                readCutConnectorCountOnGl(glView, 0) == 0
            }

            // Even if the active selection changes during background connector placement,
            // connector previews remain attached to the object whose plane is being edited.
            glView.selectModel(1, Runnable {})
            instrumentationWaitForGlIdle()
            tapGlViewAt(glView, connectorPoint.first, connectorPoint.second)
            compose.waitUntil(timeoutMillis = 10_000) {
                readCutConnectorCountOnGl(glView, 0) == 1
            }
            assertEquals("selection changes must not redirect connectors to another object", 0,
                readCutConnectorCountOnGl(glView, 1))

            compose.onNodeWithText(activity.getString(R.string.CutApply))
                .performScrollTo().assertIsEnabled().performClick()
            val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            compose.waitUntil(timeoutMillis = 10_000) {
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null &&
                    !readRendererBooleanOnGl(glView, "cutPlaneVisible")
            }
            assertEquals("a stale cut must preserve both source objects", 2, readObjectCountOnGl(glView))
            assertEquals("the changed selection should remain active", 1, readSelectedObjectOnGl(glView))
            assertEquals("a stale cut must not alter source mesh 0", originalTriangles[0],
                readTriangleCountOnGl(glView, 0))
            assertEquals("a stale cut must not alter source mesh 1", originalTriangles[1],
                readTriangleCountOnGl(glView, 1))
            assertEquals("connector previews must be discarded with the stale operation", 0,
                readCutConnectorCountOnGl(glView, 0))
            assertEquals("no connector may leak onto the newly selected model", 0,
                readCutConnectorCountOnGl(glView, 1))
            assertEquals("preview connectors must never be committed to the model", 0,
                model.getCutConnectorCount(0) + model.getCutConnectorCount(1))
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
            glView.queueEvent {
                requireNotNull(glView.renderer.model).setUniformFilament(0, 2)
                glView.requestRender()
            }
            instrumentationWaitForGlIdle()
            compose.runOnUiThread { bed.updateModel() }
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.MenuToolbarSplit)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarSplit))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

            compose.waitUntil(timeoutMillis = 30_000) { readObjectCountOnGl(glView) == 2 }
            assertEquals("native split should create exactly two independent model objects", 2,
                readObjectCountOnGl(glView))
            assertEquals("first split object should preserve its triangles", trianglesPerComponent,
                readTriangleCountOnGl(glView, 0))
            assertEquals("second split object should preserve its triangles", trianglesPerComponent,
                readTriangleCountOnGl(glView, 1))

            compose.runOnUiThread { bed.undoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 1 }
            assertEquals("undo should restore the original compound mesh", importedTriangleCount,
                readTriangleCountOnGl(glView, 0))
            compose.runOnUiThread { bed.redoModelEdit() }
            compose.waitUntil(timeoutMillis = 10_000) { readObjectCountOnGl(glView) == 2 }
            assertEquals("redo should restore both disconnected components", trianglesPerComponent,
                readTriangleCountOnGl(glView, 1))
            for (index in 0 until 2) {
                assertArrayEquals("split component $index should keep the original material", intArrayOf(2),
                    readUsedFilamentsOnGl(glView, index))
            }
            assertCurrentWorkspaceModelRoundTripsAndSlices("split-disconnected")
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
            revealMenuAction(activity, R.string.MenuToolbarEmboss)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarEmboss))
                .performScrollTo().assertIsDisplayed().performClick()
            val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            assertEquals("Emboss should be the active workspace tool", "emboss-mode",
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController))
            assertTrue("The active GL tool must publish its Compose panel content",
                workspaceController.javaClass.getMethod("getActiveToolPanelContent").invoke(workspaceController) != null)
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
        val createTextAction = compose.onNodeWithContentDescription(activity.getString(R.string.TextModelCreateSeparate))
            .performScrollTo().assertIsDisplayed().assertIsEnabled().fetchSemanticsNode()
        val actionCenter = createTextAction.boundsInRoot.center
        val workspaceRoot = requireNotNull(bed.getView())
        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN,
            actionCenter.x, actionCenter.y, 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
        val up = MotionEvent.obtain(downTime, downTime + 80L, MotionEvent.ACTION_UP,
            actionCenter.x, actionCenter.y, 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
        try {
            compose.runOnUiThread {
                workspaceRoot.dispatchTouchEvent(down)
                workspaceRoot.dispatchTouchEvent(up)
            }
        } finally {
            down.recycle()
            up.recycle()
        }
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
        val textCreationDeadline = SystemClock.uptimeMillis() + 30_000L
        var createdObjectCount = readObjectCountOnGl(glView)
        while (createdObjectCount != 1 && SystemClock.uptimeMillis() < textCreationDeadline) {
            // Avoid Compose idle synchronization while the GL renderer continues to draw.
            SystemClock.sleep(25L)
            createdObjectCount = readObjectCountOnGl(glView)
        }
        assertEquals("clicking the plate should create one independent printable object", 1,
            createdObjectCount)
        assertTrue("generated text must be real printable geometry", readTriangleCountOnGl(glView, 0) > 0)
        assertEquals("created text should become selected", 0, glView.renderer.selectedObject)
        val textTriangleCount = readTriangleCountOnGl(glView, 0)
        val textBounds = readExactBoundsOnGl(glView, 0).single()

        bed.onBackPressed()
        assertFalse("exiting the tool should clear standalone placement mode",
            readRendererBooleanOnGl(glView, "isStandaloneTextPlacementMode"))

        val historyDisabledByMemoryGuard = AtomicReference<Boolean>()
        compose.runOnUiThread {
            historyDisabledByMemoryGuard.set(
                BedFragment::class.java.getDeclaredField("modelHistoryDisabledForMemory")
                    .apply { isAccessible = true }.getBoolean(bed),
            )
        }
        compose.runOnUiThread { bed.undoModelEdit() }
        if (requireNotNull(historyDisabledByMemoryGuard.get())) {
            assertEquals("Memory protection should keep the generated object when history is disabled", 1,
                readObjectCountOnGl(glView))
            return
        }
        val undoDeadline = SystemClock.uptimeMillis() + 10_000L
        while (readObjectCountOnGl(glView) != 0 && SystemClock.uptimeMillis() < undoDeadline) {
            SystemClock.sleep(25L)
        }
        assertEquals("Undo should remove only the generated text object", 0, readObjectCountOnGl(glView))
        compose.runOnUiThread { bed.redoModelEdit() }
        val redoDeadline = SystemClock.uptimeMillis() + 10_000L
        while (readObjectCountOnGl(glView) != 1 && SystemClock.uptimeMillis() < redoDeadline) {
            SystemClock.sleep(25L)
        }
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
                R.string.MenuToolbarSupportPaint to GLRenderer.PAINT_MODE_SUPPORT,
                R.string.MenuToolbarSeamPaint to GLRenderer.PAINT_MODE_SEAM,
                R.string.MenuToolbarFuzzyPaint to GLRenderer.PAINT_MODE_FUZZY,
                R.string.MenuToolbarColorPaint to GLRenderer.PAINT_MODE_COLOR,
            )
            paintActions.forEach { (labelRes, expectedMode) ->
                openSelectedObjectMenu(activity)
                revealMenuAction(activity, labelRes)
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
            revealMenuAction(activity, R.string.MenuToolbarColorPaint)
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
            revealMenuAction(activity, R.string.MenuToolbarColorPaint)
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
            revealMenuAction(activity, R.string.MenuToolbarColorPaint)
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
    fun supportSeamAndFuzzyPaintCommitRealCanvasStrokesAndUndoRedo() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-special-paint-touch-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val paintActions = listOf(
                Triple(R.string.MenuToolbarSupportPaint, GLRenderer.PAINT_MODE_SUPPORT, R.string.PaintEnforce),
                Triple(R.string.MenuToolbarSeamPaint, GLRenderer.PAINT_MODE_SEAM, R.string.PaintEnforce),
                Triple(R.string.MenuToolbarFuzzyPaint, GLRenderer.PAINT_MODE_FUZZY, R.string.PaintFuzzy),
            )
            paintActions.forEach { (actionLabel, expectedMode, paintState) ->
                val state = if (paintState == R.string.PaintBlock) 2 else 1
                assertEquals("mode $expectedMode should start with an empty native annotation channel", 0,
                    readCommittedPaintModeTriangleCountOnGl(glView, expectedMode, state))
                openSelectedObjectMenu(activity)
                revealMenuAction(activity, actionLabel)
                compose.onNodeWithContentDescription(activity.getString(actionLabel))
                    .performScrollTo().assertIsDisplayed().performClick()
                compose.waitUntil(timeoutMillis = 10_000) {
                    bed.isPaintMode && readRendererBooleanOnGl(glView, "isPaintMode")
                }
                assertEquals("the menu action must activate its expected GL paint mode",
                    expectedMode, readRendererIntOnGl(glView, "paintModeKind"))
                compose.onNodeWithText(activity.getString(paintState)).assertIsDisplayed().performClick()
                instrumentationWaitForGlIdle()

                val point = findUncoveredPreciseSurfacePoint(glView, bed)
                injectCanvasStroke(glView, point.first, point.second)
                val previewTriangles = readPaintPreviewTriangleCountOnGl(glView, state)
                assertTrue("$expectedMode stroke should create a native preview; point=$point", previewTriangles > 0)
                assertEquals("special paint remains a preview until it is confirmed", 0,
                    readCommittedPaintModeTriangleCountOnGl(glView, expectedMode, state))

                assertTrue("Android Back should cancel a staged mode-$expectedMode paint stroke",
                    bed.onBackPressed())
                compose.waitUntil(timeoutMillis = 10_000) { !bed.isPaintMode }
                assertEquals("cancelling mode-$expectedMode paint must discard the staged stroke", 0,
                    readCommittedPaintModeTriangleCountOnGl(glView, expectedMode, state))

                openSelectedObjectMenu(activity)
                revealMenuAction(activity, actionLabel)
                compose.onNodeWithContentDescription(activity.getString(actionLabel))
                    .performScrollTo().assertIsDisplayed().performClick()
                compose.waitUntil(timeoutMillis = 10_000) {
                    bed.isPaintMode && readRendererBooleanOnGl(glView, "isPaintMode")
                }
                compose.onNodeWithText(activity.getString(paintState)).assertIsDisplayed().performClick()
                instrumentationWaitForGlIdle()
                val confirmPoint = findUncoveredPreciseSurfacePoint(glView, bed)
                injectCanvasStroke(glView, confirmPoint.first, confirmPoint.second)
                assertTrue("mode-$expectedMode should rebuild its preview after reopening",
                    readPaintPreviewTriangleCountOnGl(glView, state) > 0)
                assertEquals("reopening a mode after Back must not restore the cancelled draft", 0,
                    readCommittedPaintModeTriangleCountOnGl(glView, expectedMode, state))

                compose.onNodeWithTag("workspace-mode-panel").performTouchInput { swipeUp() }
                compose.onNodeWithText(activity.getString(R.string.ActionDone))
                    .performScrollTo().assertIsDisplayed().performClick()
                compose.waitUntil(timeoutMillis = 10_000) { !bed.isPaintMode }
                compose.waitUntil(timeoutMillis = 10_000) {
                    readCommittedPaintModeTriangleCountOnGl(glView, expectedMode, state) > 0
                }
                compose.runOnUiThread { bed.undoModelEdit() }
                compose.waitUntil(timeoutMillis = 10_000) {
                    readCommittedPaintModeTriangleCountOnGl(glView, expectedMode, state) == 0
                }
                compose.runOnUiThread { bed.redoModelEdit() }
                compose.waitUntil(timeoutMillis = 10_000) {
                    readCommittedPaintModeTriangleCountOnGl(glView, expectedMode, state) > 0
                }
                compose.runOnUiThread { bed.undoModelEdit() }
                compose.waitUntil(timeoutMillis = 10_000) {
                    readCommittedPaintModeTriangleCountOnGl(glView, expectedMode, state) == 0
                }
            }
            assertTrue("finishing special paint tools should preserve the live renderer", bed.glView === glView)
        } finally {
            if (bed.isPaintMode) bed.onBackPressed()
            modelFile.delete()
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
            revealMenuAction(activity, R.string.ModelSimplify)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelSimplify))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).assertIsDisplayed().performClick()
            assertEquals("cancelling simplify must preserve the model", originalTriangles,
                readTriangleCountOnGl(glView, 0))

            openSelectedObjectMenu(activity)
            revealMenuAction(activity, R.string.ModelSimplify)
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

    @Test
    fun editSimplifyRejectsSelectionChangedAfterTargetCapture() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-simplify-stale-selection-${System.nanoTime()}.stl")
        try {
            activity.assets.open("models/sphere.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            val duplicateDone = CountDownLatch(1)
            glView.duplicateSelectedModel(1, Runnable { duplicateDone.countDown() })
            assertTrue("second test object should finish duplicating", duplicateDone.await(15, TimeUnit.SECONDS))
            instrumentationWaitForGlIdle()
            assertEquals(2, readObjectCountOnGl(glView))
            val trianglesBefore = (0..1).map { readTriangleCountOnGl(glView, it) }

            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val captured = AtomicReference<Pair<ru.ytkab0bp.slicebeam.render.ModelSelectionSnapshot, Int>?>()
            val capturedDone = CountDownLatch(1)
            bed.captureSelectedObjectForSimplification { snapshot, triangleCount ->
                captured.set(snapshot to triangleCount)
                capturedDone.countDown()
            }
            assertTrue("simplify dialog should capture its native target on GL",
                capturedDone.await(10, TimeUnit.SECONDS))
            assertTrue("captured simplify selection should refer to the first object",
                captured.get()!!.first.primaryObject == 0 && captured.get()!!.second == trianglesBefore[0])

            // The delayed confirmation is tied to object 0. Switching selection must not redirect
            // the native edit to object 1, even though both objects are valid and have same mesh.
            val switchedSelection = CountDownLatch(1)
            glView.selectModel(1, Runnable { switchedSelection.countDown() })
            assertTrue("selection should switch while the simplify prompt is open",
                switchedSelection.await(10, TimeUnit.SECONDS))
            assertEquals("the newer selection should be object 1", 1, readSelectedObjectOnGl(glView))
            bed.simplifySelectedObject(requireNotNull(captured.get()).first, 0.5f)
            instrumentationWaitForGlIdle()
            assertEquals("stale simplify confirmation must leave object 0 unchanged",
                trianglesBefore[0], readTriangleCountOnGl(glView, 0))
            assertEquals("stale simplify confirmation must not mutate newly selected object 1",
                trianglesBefore[1], readTriangleCountOnGl(glView, 1))
            assertEquals("stale selection must not change object count", 2, model.objectsCount)
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
        val assignedProject = File(activity.cacheDir, "menu-filament-assignment-roundtrip.3mf")
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
        var roundTrippedModel: Model? = null
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

            // Export the UI-assigned renderer model and verify the per-object slots survive reload
            // before checking the G-code tools and declared palette.
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
            assignedModel.export3mf(configFile.absolutePath, assignedProject.absolutePath)
            assertTrue("per-object filament assignment should export as a non-empty 3MF",
                assignedProject.isFile && assignedProject.length() > 0L)
            roundTrippedModel = Model(assignedProject, 1)
            val slicedModel = requireNotNull(roundTrippedModel)
            assertEquals("the assignment round-trip should keep both model objects", 2, slicedModel.objectsCount)
            assertArrayEquals("the first object's selected T6 should survive project reload", intArrayOf(7),
                slicedModel.getUsedFilaments(0))
            assertArrayEquals("the second object's selected T3 should survive project reload", intArrayOf(4),
                slicedModel.getUsedFilaments(1))
            SliceBeam.genCurrentConfig(slicedModel)
            val expectedPalette = testSlots.take(7).joinToString(";") {
                String.format(java.util.Locale.ROOT, "#%06X", it.color and 0xFFFFFF)
            }
            val generatedConfig = configFile.readText()
            assertTrue("slice config must include all seven slots through assigned T6",
                Regex("(?m)^filament_colour = ${Regex.escape(expectedPalette)}$").containsMatchIn(generatedConfig))
            assertFalse("bundled Anycubic Generic PLA start G-code must not contain unsupported, commented placeholders",
                generatedConfig.contains("filament_start_gcode = ") &&
                    Regex("(?m)^filament_start_gcode = .*\\{if").containsMatchIn(generatedConfig))
            sliceResult = slicedModel.slice(configFile.absolutePath, output.absolutePath, null)
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
            roundTrippedModel?.release()
            output.delete()
            assignedProject.delete()
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
            count.set(glView.renderer.model?.objectsCount ?: 0)
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

    private fun readAdaptiveLayerHeightOnGl(
        glView: ru.ytkab0bp.slicebeam.view.GLView,
        index: Int,
    ): Boolean {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Boolean>()
        glView.queueEvent {
            value.set(requireNotNull(glView.renderer.model).hasAdaptiveLayerHeight(index))
            finished.countDown()
        }
        assertTrue("GL thread did not return adaptive layer-height state", finished.await(10, TimeUnit.SECONDS))
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

    /** Reads a persisted non-color facet channel by opening a disposable native paint session. */
    private fun readCommittedPaintModeTriangleCountOnGl(
        glView: ru.ytkab0bp.slicebeam.view.GLView,
        mode: Int,
        state: Int,
    ): Int {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Int>()
        val failure = AtomicReference<Throwable?>()
        glView.queueEvent {
            var session: PaintSession? = null
            var overlay: GLModel? = null
            try {
                session = PaintSession(requireNotNull(glView.renderer.model), 0, mode)
                if (!session.isValid()) error("Could not inspect committed paint mode $mode")
                overlay = GLModel()
                value.set(session.buildOverlay(overlay, state))
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                overlay?.release()
                session?.end()
                finished.countDown()
            }
        }
        assertTrue("GL thread did not return committed paint mode $mode", finished.await(10, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("Could not inspect committed paint mode $mode", it) }
        return requireNotNull(value.get())
    }

    /** Finds an exact mesh hit in the visible portion of the GL view, outside the paint sheet. */
    private fun findUncoveredPreciseSurfacePoint(
        glView: ru.ytkab0bp.slicebeam.view.GLView,
        bed: BedFragment,
        avoid: Pair<Float, Float>? = null,
        requireUpwardFace: Boolean = false,
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
                        if (avoid != null &&
                            kotlin.math.abs(glLocation[0] + localX - avoid.first) < 20f &&
                            kotlin.math.abs(glLocation[1] + localY - avoid.second) < 20f
                        ) continue
                        if (modePanelBounds?.contains(Offset(rootX, rootY)) == true ||
                            menuPanelBounds?.contains(Offset(rootX, rootY)) == true
                        ) continue
                        mesh.raycaster.raycast(renderer, hits, x.toFloat(), y.toFloat())
                        if (hits.any { !it.approximate && (!requireUpwardFace || it.normal.z > 0.7) }) {
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

    private fun clearWorkspaceSnackbars(bed: BedFragment) {
        val snackbarController = BedFragment::class.java.getDeclaredField("snackbarController")
            .apply { isAccessible = true }.get(bed)
        snackbarController.javaClass.getMethod("clear").invoke(snackbarController)
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

    private fun readModelHistorySize(bed: BedFragment, fieldName: String): Int {
        val history = BedFragment::class.java.getDeclaredField(fieldName).apply { isAccessible = true }
            .get(bed) as java.util.ArrayDeque<*>
        return history.size
    }

    private fun readLastUndoSnapshotObjectCountOnGl(
        bed: BedFragment,
        glView: ru.ytkab0bp.slicebeam.view.GLView,
    ): Int {
        val finished = CountDownLatch(1)
        val objectCount = AtomicReference<Int>()
        glView.queueEvent {
            @Suppress("UNCHECKED_CAST")
            val history = BedFragment::class.java.getDeclaredField("undoHistory")
                .apply { isAccessible = true }.get(bed) as java.util.ArrayDeque<Model>
            objectCount.set(history.peekLast()?.objectsCount ?: 0)
            finished.countDown()
        }
        assertTrue("GL thread did not return the latest undo snapshot", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(objectCount.get())
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

    private fun <T> onInstrumentationMain(block: () -> T): T {
        val result = AtomicReference<Any?>()
        val failure = AtomicReference<Throwable?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            try {
                result.set(block())
            } catch (error: Throwable) {
                failure.set(error)
            }
        }
        failure.get()?.let { throw AssertionError("Workspace semantics operation failed", it) }
        @Suppress("UNCHECKED_CAST")
        return result.get() as T
    }

    private fun workspaceComposeRootViews(activity: MainActivity): List<View> {
        val bed = requireNotNull(activity.navigationDelegate.getCurrentFragment() as? BedFragment)
        val fragmentRoot = requireNotNull(bed.getView())
        val composeRoots = mutableListOf<View>()
        fun findComposeRoots(view: View) {
            if (view.javaClass.name == "androidx.compose.ui.platform.AndroidComposeView") {
                composeRoots += view
                return
            }
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    findComposeRoots(view.getChildAt(index))
                }
            }
        }
        findComposeRoots(fragmentRoot)
        require(composeRoots.isNotEmpty()) {
            "Workspace AndroidComposeView is not attached"
        }
        return composeRoots
    }

    private fun workspaceSemanticsOwners(activity: MainActivity): List<SemanticsOwner> =
        workspaceComposeRootViews(activity).map { composeRoot ->
            composeRoot.javaClass.getMethod("getSemanticsOwner").invoke(composeRoot) as SemanticsOwner
        }

    private fun findWorkspaceSemanticsNode(node: SemanticsNode, tag: String): SemanticsNode? {
        if (semanticsValue(node.config, SemanticsProperties.TestTag) == tag) return node
        node.children.forEach { child -> findWorkspaceSemanticsNode(child, tag)?.let { return it } }
        return null
    }

    private fun <T> semanticsValue(config: SemanticsConfiguration, key: SemanticsPropertyKey<T>): T? =
        if (config.contains(key)) config[key] else null

    private fun readWorkspaceSemanticsNode(activity: MainActivity, tag: String): SemanticsNode =
        onInstrumentationMain {
            val owners = workspaceSemanticsOwners(activity)
            owners.firstNotNullOfOrNull { owner ->
                findWorkspaceSemanticsNode(owner.unmergedRootSemanticsNode, tag)
            } ?: error("Workspace semantics node '$tag' is not present in ${owners.size} Compose roots")
        }

    private fun awaitWorkspaceSemanticsNode(activity: MainActivity, tag: String, timeoutMillis: Long = 5_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        do {
            val found = runCatching { readWorkspaceSemanticsNode(activity, tag) }.isSuccess
            if (found) return
            SystemClock.sleep(50L)
        } while (SystemClock.uptimeMillis() < deadline)
        readWorkspaceSemanticsNode(activity, tag)
    }

    private fun awaitWorkspaceSelected(activity: MainActivity, tag: String, timeoutMillis: Long = 5_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        do {
            val selected = runCatching {
                semanticsValue(readWorkspaceSemanticsNode(activity, tag).config, SemanticsProperties.Selected)
            }.getOrNull() == true
            if (selected) return
            SystemClock.sleep(50L)
        } while (SystemClock.uptimeMillis() < deadline)
        assertTrue("workspace node '$tag' should become selected", runCatching {
            semanticsValue(readWorkspaceSemanticsNode(activity, tag).config, SemanticsProperties.Selected)
        }.getOrNull() == true)
    }

    private fun performWorkspaceClick(activity: MainActivity, tag: String) = onInstrumentationMain {
        val node = requireNotNull(workspaceSemanticsOwners(activity).asSequence()
                .map { it.unmergedRootSemanticsNode }
                .firstNotNullOfOrNull { findWorkspaceSemanticsNode(it, tag) }
        ) { "Workspace click target '$tag' is not present in attached Compose roots" }
        val click = requireNotNull(semanticsValue(node.config, SemanticsActions.OnClick)?.action) {
            "Workspace semantics node '$tag' has no click action"
        }
        assertTrue("Workspace click action '$tag' should be accepted", click.invoke())
    }

    private fun performWorkspaceSetText(activity: MainActivity, tag: String, value: String) =
        onInstrumentationMain {
            val node = requireNotNull(workspaceSemanticsOwners(activity).asSequence()
                .map { it.unmergedRootSemanticsNode }
                .firstNotNullOfOrNull { findWorkspaceSemanticsNode(it, tag) }
            ) { "Workspace text field '$tag' is not present in attached Compose roots" }
            val setText = requireNotNull(semanticsValue(node.config, SemanticsActions.SetText)?.action) {
                "Workspace semantics node '$tag' has no set-text action"
            }
            assertTrue("Workspace text field '$tag' should accept text input",
                setText.invoke(AnnotatedString(value)))
        }

    private fun awaitWorkspaceSemanticsText(activity: MainActivity, expected: String, timeoutMillis: Long = 5_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        do {
            val visible = onInstrumentationMain {
                fun contains(node: SemanticsNode): Boolean {
                    val text = semanticsValue(node.config, SemanticsProperties.Text).orEmpty()
                        .any { it.text == expected }
                    val description = semanticsValue(node.config, SemanticsProperties.ContentDescription).orEmpty()
                        .any { it == expected }
                    return text || description || node.children.any(::contains)
                }
                workspaceSemanticsOwners(activity).any { owner ->
                    contains(owner.unmergedRootSemanticsNode)
                }
            }
            if (visible) return
            SystemClock.sleep(50L)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError("Workspace semantic text '$expected' did not appear")
    }
}
