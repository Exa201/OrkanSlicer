package ru.ytkab0bp.slicebeam

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertArrayEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.os.SystemClock
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import ru.ytkab0bp.slicebeam.components.bed_menu.FileMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.FileMenuObjectActions
import ru.ytkab0bp.slicebeam.components.bed_menu.ListBedMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraPreset
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraPresetGeometry
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraVector
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceDestination
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Reproduces the first Project tap after a cold workspace launch. */
@RunWith(AndroidJUnit4::class)
class InitialProjectMenuDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

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

        compose.onNodeWithContentDescription(activity.getString(R.string.MenuProject))
            .assertIsDisplayed()
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(activity.getString(R.string.WorkspaceAddModelAction))
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithContentDescription(activity.getString(R.string.MenuProject)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(R.string.WorkspaceAddModelAction)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(R.string.MenuFileExport3mf)).assertIsDisplayed()
        assertTrue("the first Project tap should open its action panel",
            controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) as Boolean)
    }

    @Test
    fun toolsMenuProfileExportActionOpensComposeFlowAndAndroidBackDismissesIt() {
        val activity = compose.activity
        compose.onNodeWithContentDescription(activity.getString(R.string.MenuOptions))
            .assertIsDisplayed()
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(activity.getString(R.string.MenuFileExportProfiles))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(activity.getString(R.string.MenuFileExportProfiles))
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
    fun cameraMenuActionsUpdateProjectionAndCameraControlPreference() {
        val activity = compose.activity
        val previousProjection = Prefs.isOrthoProjectionEnabled()
        val previousControlMode = Prefs.getCameraControlMode()
        try {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuCamera))
                .assertIsDisplayed()
                .performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuCameraTop))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            val projectionAction = compose.onNodeWithContentDescription(
                activity.getString(R.string.MenuCameraOrtho),
            ).assertIsDisplayed()
            projectionAction.performClick()
            compose.waitForIdle()
            assertEquals("camera projection tile should update the persisted preference",
                !previousProjection, Prefs.isOrthoProjectionEnabled())

            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(8)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuCameraControlMode))
                .assertIsDisplayed()
                .performClick()
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
            assertEquals("camera control dialog selection should update its preference",
                targetMode, Prefs.getCameraControlMode())
        } finally {
            Prefs.setOrthoProjectionEnabled(previousProjection)
            Prefs.setCameraControlMode(previousControlMode)
        }
    }

    @Test
    fun cameraPresetTilesUpdateTheRendererPoseForEveryPreset() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        compose.onNodeWithContentDescription(activity.getString(R.string.MenuCamera))
            .assertIsDisplayed()
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(activity.getString(R.string.MenuCameraTop))
                .fetchSemanticsNodes().isNotEmpty()
        }

        val camera = requireNotNull(bed.glView).renderer.camera
        val modelBed = requireNotNull(bed.glView).renderer.bed!!
        val min = requireNotNull(modelBed.getVolumeMin())
        val max = requireNotNull(modelBed.getVolumeMax())
        val minVector = CameraVector(min.x, min.y, min.z)
        val maxVector = CameraVector(max.x, max.y, max.z)
        val presets = listOf(
            Triple(0, R.string.MenuCameraIsometric, CameraPreset.ISOMETRIC),
            Triple(2, R.string.MenuCameraTop, CameraPreset.TOP),
            Triple(3, R.string.MenuCameraBottom, CameraPreset.BOTTOM),
            Triple(4, R.string.MenuCameraFront, CameraPreset.FRONT),
            Triple(5, R.string.MenuCameraBack, CameraPreset.BACK),
            Triple(6, R.string.MenuCameraLeft, CameraPreset.LEFT),
            Triple(7, R.string.MenuCameraRight, CameraPreset.RIGHT),
        )
        presets.forEach { (index, presetLabel, preset) ->
            val target = CameraPresetGeometry.resolve(preset, minVector, maxVector)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
            compose.onNodeWithContentDescription(activity.getString(presetLabel))
                .assertIsDisplayed()
                .performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                kotlin.math.abs(camera.position.x - target.position.x) < 0.1 &&
                    kotlin.math.abs(camera.position.y - target.position.y) < 0.1 &&
                    kotlin.math.abs(camera.position.z - target.position.z) < 0.1 &&
                    kotlin.math.abs(camera.origin.x - target.origin.x) < 0.1 &&
                    kotlin.math.abs(camera.origin.y - target.origin.y) < 0.1 &&
                    kotlin.math.abs(camera.origin.z - target.origin.z) < 0.1
            }
            // Let the renderer's spring animation settle before requesting the next preset.
            SystemClock.sleep(900L)
            compose.waitForIdle()
            assertTrue(
                "${activity.getString(presetLabel)} must apply its configured renderer camera pose",
                kotlin.math.abs(camera.position.x - target.position.x) < 0.1 &&
                    kotlin.math.abs(camera.position.y - target.position.y) < 0.1 &&
                    kotlin.math.abs(camera.position.z - target.position.z) < 0.1,
            )
        }
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
        FileMenuObjectActions(bed).showDuplicateDialog(viewport)
        val duplicateCount = compose.onNodeWithTag("duplicate-model-count").assertIsDisplayed()
        duplicateCount.performTextClearance()
        duplicateCount.performTextInput("2")
        compose.onNodeWithTag("duplicate-model-confirm").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            requireNotNull(bed.currentModel).objectsCount == 3
        }
        assertEquals("two requested copies should be added to the original", 3,
            requireNotNull(bed.currentModel).objectsCount)
        assertFalse("successful duplication should close its dialog", bed.isWorkspaceMessageDialogAttached())
        modelFile.delete()
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
        )
        Prefs.setStoredFilamentSlots(testSlots)
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

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuModel)).performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelMenuObjectsShort)).assertIsDisplayed().performClick()
            compose.onAllNodesWithText(activity.getString(R.string.ObjectActionAssignFilamentShort))[0]
                .assertIsDisplayed().performClick()
            val targetLabel = activity.getString(
                R.string.ModelContextFilamentSlot,
                2,
                "UI Test ABS",
                String.format(java.util.Locale.ROOT, "#%06X", testSlots[2].color and 0xFFFFFF),
            )
            compose.onNodeWithText(targetLabel).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.currentModel?.getUsedFilaments(0)?.contentEquals(intArrayOf(3)) == true
            }
            assertArrayEquals("explicit Model-menu T2 assignment replaces imported 3MF T0",
                intArrayOf(3), requireNotNull(bed.currentModel).getUsedFilaments(0))
            assertArrayEquals("unselected second object keeps its imported T1 assignment",
                intArrayOf(2), requireNotNull(bed.currentModel).getUsedFilaments(1))
            assertFalse("uniform reassignment removes old per-volume paint overrides",
                requireNotNull(bed.currentModel).hasPaint(0))
        } finally {
            Prefs.setStoredFilamentSlots(previousSlots)
            file.delete()
        }
    }

    private fun instrumentationWaitForGlIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SystemClock.sleep(250L)
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
