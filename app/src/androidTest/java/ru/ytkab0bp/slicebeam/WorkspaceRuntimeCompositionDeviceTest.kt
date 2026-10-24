package ru.ytkab0bp.slicebeam

import android.content.Intent
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.render.GLRenderer
import ru.ytkab0bp.slicebeam.slic3r.GLModel
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.view.GLView
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Uses the platform window recomposer, without ComposeTestRule's activity idling clock. */
@RunWith(AndroidJUnit4::class)
class WorkspaceRuntimeCompositionDeviceTest {
    @Test
    fun selectedModelCanOpenStandaloneTextPanelInPlatformWorkspaceComposition() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val application = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = application
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

        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        ) as MainActivity
        val modelFile = File(activity.cacheDir, "platform-recomposer-text-panel.stl")
        try {
            context.assets.open("models/xyz_cube.stl").use { input ->
                modelFile.outputStream().use(input::copyTo)
            }

            val bed = awaitReadyWorkspace(activity)
            val model = loadSingleModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.setUniformFilament(0, 1)
                glView.renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("model selection should settle on the GL thread", prepared.await(10, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { bed.updateModel() }

            instrumentation.runOnMainSync { bed.enterEmbossMode(standaloneText = true) }
            assertTrue("standalone text should register its visible tool panel",
                awaitWorkspaceTag(activity, "emboss-standalone-mode", 5_000))
        } finally {
            instrumentation.runOnMainSync {
                (runCatching { activity.navigationDelegate.getCurrentFragment() as? BedFragment }.getOrNull())
                    ?.dismissWorkspaceToolPanel()
                activity.finish()
            }
            modelFile.delete()
        }
    }

    @Test
    fun standaloneTextCanFollowModelSurfaceAndUndoRedoInPlatformComposition() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val application = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = application
        Prefs.init(application)
        Prefs.getPrefs()?.edit()?.remove("crash")?.putBoolean("converter_storage_first_prompt", true)?.commit()
        Prefs.setLastCommit()
        SliceBeam.hasUpdateInfo = false
        SliceBeam.clearLiveDiffs()

        val config = context.assets.open("orca_profiles/Anycubic.ini").use(::Slic3rConfigWrapper)
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title); put("print", process.title); put("filament", filament.title)
        }
        SliceBeam.CONFIG = config

        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        ) as MainActivity
        val modelFile = File(activity.cacheDir, "platform-surface-text-undo-redo.stl")
        try {
            context.assets.open("models/xyz_cube.stl").use { input -> modelFile.outputStream().use(input::copyTo) }
            val bed = awaitReadyWorkspace(activity)
            val model = loadSingleModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.setUniformFilament(0, 1)
                glView.renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("model selection should settle on GL", prepared.await(10, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { bed.updateModel() }
            val originalTriangles = triangleCount(glView)

            instrumentation.runOnMainSync { bed.enterEmbossMode(standaloneText = true) }
            awaitTag(activity, "emboss-standalone-mode")
            clickTag(activity, "emboss-follow-surface")
            awaitSelected(activity, "emboss-follow-surface")
            setText(activity, "emboss-text-input", "A")
            setText(activity, "emboss-font-size", "1")

            val hit = findUpwardSurfacePoint(glView)
            tap(glView, hit.first, hit.second)
            awaitCondition(30_000) { triangleCount(glView) > originalTriangles }
            assertEquals("text fitted to a surface must be part of the target model", 1, model.objectsCount)
            val editedTriangles = triangleCount(glView)
            assertTrue("surface text must add native triangles", editedTriangles > originalTriangles)
            assertTrue("surface text must inherit the selected model's filament",
                usedFilaments(glView).contains(1))

            instrumentation.runOnMainSync { bed.onBackPressed() }
            awaitCondition(10_000) { !rendererBoolean(glView, "isStandaloneTextPlacementMode") }
            instrumentation.runOnMainSync { bed.undoModelEdit() }
            awaitCondition(15_000) { triangleCount(glView) == originalTriangles }
            instrumentation.runOnMainSync { bed.redoModelEdit() }
            awaitCondition(15_000) { triangleCount(glView) == editedTriangles }
        } finally {
            instrumentation.runOnMainSync {
                (runCatching { activity.navigationDelegate.getCurrentFragment() as? BedFragment }.getOrNull())
                    ?.let { if (it.isEmbossMode) it.onBackPressed() }
                activity.finish()
            }
            modelFile.delete()
        }
    }

    @Test
    fun standaloneSurfaceTextTargetsTheTappedObjectAndInheritsItsFilament() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val application = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = application
        Prefs.init(application)
        Prefs.getPrefs()?.edit()?.remove("crash")?.putBoolean("converter_storage_first_prompt", true)?.commit()
        Prefs.setLastCommit()
        SliceBeam.hasUpdateInfo = false
        SliceBeam.clearLiveDiffs()

        val previousConfig = SliceBeam.CONFIG
        val config = context.assets.open("orca_profiles/Anycubic.ini").use(::Slic3rConfigWrapper)
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title); put("print", process.title); put("filament", filament.title)
        }
        SliceBeam.CONFIG = config

        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        ) as MainActivity
        val modelFile = File(activity.cacheDir, "surface-text-second-object-filament.stl")
        val projectFile = File(activity.cacheDir, "surface-text-second-object-filament.3mf")
        val gcodeFile = File(activity.cacheDir, "surface-text-second-object-filament.gcode")
        var sliceResult: ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult? = null
        var generatedConfig: File? = null
        var slicedModel: Model? = null
        try {
            context.assets.open("models/xyz_cube.stl").use { input -> modelFile.outputStream().use(input::copyTo) }
            val bed = awaitReadyWorkspace(activity)
            val model = loadSingleModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            val prepared = CountDownLatch(1)
            glView.queueEvent {
                model.addObject(model, 0)
                model.translate(1, 35.0, 0.0, 0.0)
                model.setUniformFilament(0, 1)
                model.setUniformFilament(1, 2)
                glView.renderer.invalidateGlModel(0)
                glView.renderer.invalidateGlModel(1)
                glView.renderer.setSelectedObject(0)
                glView.requestRender()
                prepared.countDown()
            }
            assertTrue("both objects should be prepared on GL", prepared.await(10, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { bed.updateModel() }
            val firstObjectTriangles = triangleCount(glView, 0)
            val targetObjectTriangles = triangleCount(glView, 1)

            instrumentation.runOnMainSync { bed.enterEmbossMode(standaloneText = true) }
            awaitTag(activity, "emboss-standalone-mode")
            clickTag(activity, "emboss-follow-surface")
            awaitSelected(activity, "emboss-follow-surface")
            setText(activity, "emboss-text-input", "A")
            setText(activity, "emboss-font-size", "1")

            val hit = findUpwardSurfacePoint(glView, objectIndex = 1)
            tap(glView, hit.first, hit.second)
            awaitCondition(30_000) { triangleCount(glView, 1) > targetObjectTriangles }
            assertEquals("surface text should remain attached to its intended object", 2, model.objectsCount)
            assertEquals("the non-target object must not receive the text mesh", firstObjectTriangles,
                triangleCount(glView, 0))
            assertTrue("the target object's filament assignment must be retained",
                usedFilaments(glView, 1).contains(2))

            instrumentation.runOnMainSync { bed.onBackPressed() }
            awaitCondition(10_000) { !rendererBoolean(glView, "isStandaloneTextPlacementMode") }
            instrumentation.runOnMainSync { bed.undoModelEdit() }
            awaitCondition(15_000) { triangleCount(glView, 1) == targetObjectTriangles }
            instrumentation.runOnMainSync { bed.redoModelEdit() }
            awaitCondition(15_000) { triangleCount(glView, 1) > targetObjectTriangles }

            // Exercise the real slicer after the GL surface edit and make sure the
            // saved, reloaded project retains the target's second filament in emitted G-code.
            val exportReady = CountDownLatch(1)
            val exportFailure = AtomicReference<Throwable?>()
            glView.queueEvent {
                try {
                    // Undo/redo swaps the renderer's native Model pointer and releases the prior
                    // wrapper. Export the live renderer model, never the pre-undo reference.
                    val liveModel = requireNotNull(glView.renderer.model)
                    SliceBeam.genCurrentConfig(liveModel)
                    generatedConfig = SliceBeam.getCurrentConfigFile()
                    liveModel.export3mf(requireNotNull(generatedConfig).absolutePath, projectFile.absolutePath)
                } catch (error: Throwable) {
                    exportFailure.set(error)
                } finally {
                    exportReady.countDown()
                }
            }
            assertTrue("the GL-owned edited model should finish its 3MF export", exportReady.await(60, TimeUnit.SECONDS))
            exportFailure.get()?.let { throw AssertionError("surface-edited model export failed", it) }
            assertTrue(
                "surface-edited model should produce a project file at ${projectFile.absolutePath}; " +
                    "parentExists=${projectFile.parentFile?.isDirectory}; " +
                    "parentFiles=${projectFile.parentFile?.list()?.joinToString()}; " +
                    "configExists=${generatedConfig?.isFile}",
                projectFile.isFile && projectFile.length() > 0L,
            )
            generatedConfig?.delete()
            generatedConfig = null
            slicedModel = Model(projectFile, 1)
            assertEquals("both original objects should survive project save/load", 2, slicedModel.objectsCount)
            assertTrue("surface text should survive project save/load on its target",
                slicedModel.getTriangleCount(1) > targetObjectTriangles)
            assertTrue("the saved target object must keep its second filament",
                slicedModel.getUsedFilaments(1).contains(2))
            SliceBeam.genCurrentConfig(slicedModel)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            sliceResult = slicedModel.slice(generatedConfig.absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertTrue("surface-edited target should emit extrusion", Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
            assertTrue("surface text must retain the tapped object's T1 assignment",
                Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            sliceResult?.release()
            slicedModel?.release()
            generatedConfig?.delete()
            projectFile.delete()
            gcodeFile.delete()
            SliceBeam.CONFIG = previousConfig
            instrumentation.runOnMainSync {
                (runCatching { activity.navigationDelegate.getCurrentFragment() as? BedFragment }.getOrNull())
                    ?.let { if (it.isEmbossMode) it.onBackPressed() }
                activity.finish()
            }
            modelFile.delete()
        }
    }

    @Test
    fun standaloneSurfaceTextFollowsCurvedGlTargetThroughUndoRedoAndSlicing() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val application = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = application
        Prefs.init(application)
        Prefs.getPrefs()?.edit()?.remove("crash")?.putBoolean("converter_storage_first_prompt", true)?.commit()
        Prefs.setLastCommit()
        SliceBeam.hasUpdateInfo = false
        SliceBeam.clearLiveDiffs()

        val previousConfig = SliceBeam.CONFIG
        val config = context.assets.open("orca_profiles/Anycubic.ini").use(::Slic3rConfigWrapper)
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title); put("print", process.title); put("filament", filament.title)
        }
        SliceBeam.CONFIG = config

        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        ) as MainActivity
        val modelFile = createCylinderFixture(activity.cacheDir, "standalone-curved-gl-target.stl", 25.0, 30.0, 96)
        val projectFile = File(activity.cacheDir, "standalone-curved-gl-target.3mf")
        val gcodeFile = File(activity.cacheDir, "standalone-curved-gl-target.gcode")
        var generatedConfig: File? = null
        var slicedModel: Model? = null
        var sliceResult: ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult? = null
        try {
            val bed = awaitReadyWorkspace(activity)
            loadSingleModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            onGl(glView) { view ->
                requireNotNull(view.renderer.model).setUniformFilament(0, 2)
                view.renderer.invalidateGlModel(0)
                view.requestRender()
            }
            instrumentation.runOnMainSync { bed.updateModel() }
            val originalTriangles = triangleCount(glView, 0)

            instrumentation.runOnMainSync { bed.enterEmbossMode(standaloneText = true) }
            awaitTag(activity, "emboss-standalone-mode")
            clickTag(activity, "emboss-follow-surface")
            awaitSelected(activity, "emboss-follow-surface")
            setText(activity, "emboss-text-input", "A")
            setText(activity, "emboss-font-size", "1")

            val hit = findCurvedSurfacePoint(glView, objectIndex = 0)
            tap(glView, hit.first, hit.second)
            awaitCondition(30_000) { triangleCount(glView, 0) > originalTriangles }
            assertEquals("curved surface text stays part of its existing target object", 1,
                requireNotNull(bed.currentModel).objectsCount)
            assertTrue("the curved target's filament assignment should be retained",
                usedFilaments(glView, 0).contains(2))

            instrumentation.runOnMainSync { bed.onBackPressed() }
            awaitCondition(10_000) { !rendererBoolean(glView, "isStandaloneTextPlacementMode") }
            instrumentation.runOnMainSync { bed.undoModelEdit() }
            awaitCondition(15_000) { triangleCount(glView, 0) == originalTriangles }
            instrumentation.runOnMainSync { bed.redoModelEdit() }
            awaitCondition(15_000) { triangleCount(glView, 0) > originalTriangles }

            val exportReady = CountDownLatch(1)
            val exportFailure = AtomicReference<Throwable?>()
            glView.queueEvent {
                try {
                    val liveModel = requireNotNull(glView.renderer.model)
                    SliceBeam.genCurrentConfig(liveModel)
                    generatedConfig = SliceBeam.getCurrentConfigFile()
                    liveModel.export3mf(requireNotNull(generatedConfig).absolutePath, projectFile.absolutePath)
                } catch (error: Throwable) {
                    exportFailure.set(error)
                } finally {
                    exportReady.countDown()
                }
            }
            assertTrue("curved GL-edited model export should finish", exportReady.await(60, TimeUnit.SECONDS))
            exportFailure.get()?.let { throw AssertionError("curved GL-edited model export failed", it) }
            assertTrue("curved GL-edited model should create a 3MF", projectFile.isFile && projectFile.length() > 0L)
            generatedConfig?.delete()
            generatedConfig = null

            slicedModel = Model(projectFile, 1)
            assertEquals("3MF reload should keep the curved model as one multipart object", 1, slicedModel.objectsCount)
            assertTrue("surface-following text should survive the 3MF reload",
                slicedModel.getTriangleCount(0) > originalTriangles)
            assertTrue("surface-following text should retain the target material",
                slicedModel.getUsedFilaments(0).contains(2))
            SliceBeam.genCurrentConfig(slicedModel)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            sliceResult = slicedModel.slice(generatedConfig.absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertTrue("curved target should generate extrusion", Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
            assertTrue("curved surface text should use the inherited T1 material",
                Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            sliceResult?.release()
            slicedModel?.release()
            generatedConfig?.delete()
            projectFile.delete()
            gcodeFile.delete()
            SliceBeam.CONFIG = previousConfig
            instrumentation.runOnMainSync {
                (runCatching { activity.navigationDelegate.getCurrentFragment() as? BedFragment }.getOrNull())
                    ?.let { if (it.isEmbossMode) it.onBackPressed() }
                activity.finish()
            }
            modelFile.delete()
        }
    }

    private fun awaitReadyWorkspace(activity: MainActivity): BedFragment {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + 30_000
        while (SystemClock.uptimeMillis() < deadline) {
            val ready = AtomicReference<BedFragment?>()
            instrumentation.runOnMainSync {
                val bed = runCatching { activity.navigationDelegate.getCurrentFragment() as? BedFragment }.getOrNull()
                if (bed?.glView?.isAttachedToWindow == true && bed.glView?.renderer?.bed != null &&
                    bed.getView()?.isAttachedToWindow == true
                ) ready.set(bed)
            }
            ready.get()?.let { return it }
            SystemClock.sleep(50)
        }
        throw AssertionError("workspace BedFragment and GL renderer did not become ready")
    }

    private fun loadSingleModel(bed: BedFragment, file: File): Model {
        val finished = CountDownLatch(1)
        val loaded = AtomicReference<Model?>()
        val failure = AtomicReference<Exception?>()
        bed.loadModel(file, false, 1, object : BedFragment.ModelLoadCallback {
            override fun onLoaded(model: Model, firstNewObject: Int, addedObjects: Int) {
                loaded.set(model)
                finished.countDown()
            }

            override fun onFailed(error: Exception) {
                failure.set(error)
                finished.countDown()
            }
        })
        assertTrue("test cube should load before opening the tool", finished.await(60, TimeUnit.SECONDS))
        return loaded.get() ?: throw AssertionError("test cube failed to load", failure.get())
    }

    private fun awaitWorkspaceTag(activity: MainActivity, tag: String, timeoutMillis: Long): Boolean {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            val found = AtomicReference(false)
            instrumentation.runOnMainSync {
                val bed = runCatching { activity.navigationDelegate.getCurrentFragment() as? BedFragment }.getOrNull()
                val root = bed?.getView()
                if (root != null) {
                    val composeRoots = mutableListOf<View>()
                    fun find(view: View) {
                        if (view.javaClass.name == "androidx.compose.ui.platform.AndroidComposeView") {
                            composeRoots += view
                            return
                        }
                        if (view is ViewGroup) for (index in 0 until view.childCount) find(view.getChildAt(index))
                    }
                    find(root)
                    found.set(composeRoots.any { composeRoot ->
                        val owner = composeRoot.javaClass.getMethod("getSemanticsOwner").invoke(composeRoot) as SemanticsOwner
                        containsTag(owner.unmergedRootSemanticsNode, tag)
                    })
                }
            }
            if (found.get()) return true
            SystemClock.sleep(50)
        }
        return false
    }

    private fun semanticsNode(activity: MainActivity, tag: String): SemanticsNode {
        val result = AtomicReference<SemanticsNode?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val bed = activity.navigationDelegate.getCurrentFragment() as? BedFragment
            val roots = mutableListOf<View>()
            fun visit(view: View) {
                if (view.javaClass.name == "androidx.compose.ui.platform.AndroidComposeView") {
                    roots += view
                } else if (view is ViewGroup) {
                    for (index in 0 until view.childCount) visit(view.getChildAt(index))
                }
            }
            bed?.getView()?.let(::visit)
            result.set(roots.asSequence().map { it.javaClass.getMethod("getSemanticsOwner").invoke(it) as SemanticsOwner }
                .mapNotNull { owner -> findNode(owner.unmergedRootSemanticsNode, tag) }.firstOrNull())
        }
        return requireNotNull(result.get()) { "Workspace semantics tag '$tag' was not found" }
    }

    private fun awaitTag(activity: MainActivity, tag: String, timeoutMillis: Long = 5_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            if (runCatching { semanticsNode(activity, tag) }.isSuccess) return
            SystemClock.sleep(50)
        }
        semanticsNode(activity, tag)
    }

    private fun findNode(node: SemanticsNode, tag: String): SemanticsNode? {
        if (node.config.contains(SemanticsProperties.TestTag) && node.config[SemanticsProperties.TestTag] == tag) return node
        node.children.forEach { findNode(it, tag)?.let { found -> return found } }
        return null
    }

    private fun clickTag(activity: MainActivity, tag: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val click = requireNotNull(semanticsNode(activity, tag).config[SemanticsActions.OnClick].action)
        instrumentation.runOnMainSync {
            assertTrue("click should be accepted for $tag", click.invoke())
        }
    }

    private fun setText(activity: MainActivity, tag: String, text: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val action = requireNotNull(semanticsNode(activity, tag).config[SemanticsActions.SetText].action)
        instrumentation.runOnMainSync {
            assertTrue("text input should accept $tag", action.invoke(AnnotatedString(text)))
        }
    }

    private fun awaitSelected(activity: MainActivity, tag: String, timeoutMillis: Long = 5_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            val selected = runCatching {
                val node = semanticsNode(activity, tag)
                node.config.contains(SemanticsProperties.Selected) && node.config[SemanticsProperties.Selected]
            }.getOrDefault(false)
            if (selected) return
            SystemClock.sleep(50)
        }
        throw AssertionError("Workspace semantics tag '$tag' never became selected")
    }

    private fun findUpwardSurfacePoint(glView: GLView, objectIndex: Int = 0): Pair<Float, Float> {
        val location = IntArray(2)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { glView.getLocationOnScreen(location) }
        val point = AtomicReference<Pair<Float, Float>?>()
        val done = CountDownLatch(1)
        glView.queueEvent {
            try {
                val renderer = glView.renderer
                @Suppress("UNCHECKED_CAST")
                val models = GLRenderer::class.java.getDeclaredField("glModels").apply { isAccessible = true }
                    .get(renderer) as ArrayList<GLModel>
                val mesh = requireNotNull(models.getOrNull(objectIndex)) {
                    "GL mesh $objectIndex should exist for the requested target object"
                }
                val hits = ArrayList<GLModel.HitResult>()
                val scale = Prefs.getRenderScale()
                val cx = renderer.viewportWidth / 2
                val cy = renderer.viewportHeight / 2
                for (radius in listOf(0, 16, 32, 48, 64, 96, 128, 176)) {
                    val offsets = if (radius == 0) listOf(0 to 0) else buildList {
                        for (offset in -radius..radius step 8) {
                            add(offset to -radius); add(offset to radius); add(-radius to offset); add(radius to offset)
                        }
                    }
                    for ((dx, dy) in offsets) {
                        val x = cx + dx; val y = cy + dy
                        if (x !in 0 until renderer.viewportWidth || y !in 0 until renderer.viewportHeight) continue
                        hits.clear()
                        mesh.raycaster.raycast(renderer, hits, x.toFloat(), y.toFloat())
                        if (hits.any { !it.approximate && it.normal.z > 0.7 }) {
                            point.set((location[0] + x / scale) to (location[1] + y / scale))
                            return@queueEvent
                        }
                    }
                }
            } finally { done.countDown() }
        }
        assertTrue("GL thread should locate an upward exact surface hit", done.await(15, TimeUnit.SECONDS))
        return requireNotNull(point.get()) { "No accessible upward model surface was found" }
    }

    private fun findCurvedSurfacePoint(glView: GLView, objectIndex: Int): Pair<Float, Float> {
        val location = IntArray(2)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { glView.getLocationOnScreen(location) }
        val point = AtomicReference<Pair<Float, Float>?>()
        val done = CountDownLatch(1)
        glView.queueEvent {
            try {
                val renderer = glView.renderer
                @Suppress("UNCHECKED_CAST")
                val models = GLRenderer::class.java.getDeclaredField("glModels").apply { isAccessible = true }
                    .get(renderer) as ArrayList<GLModel>
                val mesh = requireNotNull(models.getOrNull(objectIndex))
                val hits = ArrayList<GLModel.HitResult>()
                val scale = Prefs.getRenderScale()
                val step = 8
                for (y in step until renderer.viewportHeight step step) {
                    for (x in step until renderer.viewportWidth step step) {
                        if (renderer.raycastObjectIndex(x.toFloat(), y.toFloat()) != objectIndex) continue
                        hits.clear()
                        mesh.raycaster.raycast(renderer, hits, x.toFloat(), y.toFloat())
                        val hit = hits.firstOrNull { !it.approximate } ?: continue
                        if (kotlin.math.abs(hit.normal.z) >= 0.65) continue
                        point.set((location[0] + x / scale) to (location[1] + y / scale))
                        return@queueEvent
                    }
                }
            } finally { done.countDown() }
        }
        assertTrue("GL thread should locate a precise curved-side hit", done.await(20, TimeUnit.SECONDS))
        return requireNotNull(point.get()) { "No visible curved-side hit with a non-upward normal was found" }
    }

    private fun createCylinderFixture(directory: File, name: String, radius: Double, height: Double, segments: Int): File {
        val target = File(directory, name)
        fun point(angle: Double, z: Double) = doubleArrayOf(radius * kotlin.math.cos(angle), radius * kotlin.math.sin(angle), z)
        fun vertex(point: DoubleArray) = "vertex " + point[0] + " " + point[1] + " " + point[2] + "\n"
        val stl = StringBuilder("solid curved_surface_text\n")
        for (index in 0 until segments) {
            val a0 = 2.0 * Math.PI * index / segments
            val a1 = 2.0 * Math.PI * (index + 1) / segments
            val b0 = point(a0, 0.0); val b1 = point(a1, 0.0)
            val t0 = point(a0, height); val t1 = point(a1, height)
            val bottom = doubleArrayOf(0.0, 0.0, 0.0)
            val top = doubleArrayOf(0.0, 0.0, height)
            listOf(arrayOf(b0, b1, t1), arrayOf(b0, t1, t0),
                arrayOf(bottom, b1, b0), arrayOf(top, t0, t1)).forEach { triangle ->
                stl.append("facet normal 0 0 0\nouter loop\n")
                triangle.forEach { stl.append(vertex(it)) }
                stl.append("endloop\nendfacet\n")
            }
        }
        stl.append("endsolid curved_surface_text\n")
        target.writeText(stl.toString())
        return target
    }

    private fun tap(glView: GLView, screenX: Float, screenY: Float) {
        val location = IntArray(2)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { glView.getLocationOnScreen(location) }
        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN,
            screenX - location[0], screenY - location[1], 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
        val up = MotionEvent.obtain(downTime, downTime + 80, MotionEvent.ACTION_UP,
            screenX - location[0], screenY - location[1], 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
        try {
            instrumentation.runOnMainSync { glView.dispatchTouchEvent(down); glView.dispatchTouchEvent(up) }
        } finally { down.recycle(); up.recycle() }
    }

    private fun triangleCount(glView: GLView, objectIndex: Int = 0): Int =
        onGl(glView) { requireNotNull(it.renderer.model).getTriangleCount(objectIndex) }
    private fun usedFilaments(glView: GLView, objectIndex: Int = 0): IntArray =
        onGl(glView) { requireNotNull(it.renderer.model).getUsedFilaments(objectIndex) }
    private fun rendererBoolean(glView: GLView, name: String): Boolean = onGl(glView) {
        GLRenderer::class.java.getDeclaredField(name).apply { isAccessible = true }.getBoolean(it.renderer)
    }

    private fun <T> onGl(glView: GLView, action: (GLView) -> T): T {
        val result = AtomicReference<T?>(); val done = CountDownLatch(1)
        glView.queueEvent { try { result.set(action(glView)) } finally { done.countDown() } }
        assertTrue("GL operation should complete", done.await(15, TimeUnit.SECONDS))
        return requireNotNull(result.get())
    }

    private fun awaitCondition(timeoutMillis: Long, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(25)
        }
        assertTrue("condition did not become true before timeout", condition())
    }

    private fun containsTag(node: SemanticsNode, tag: String): Boolean {
        if (node.config.contains(SemanticsProperties.TestTag) && node.config[SemanticsProperties.TestTag] == tag) {
            return true
        }
        return node.children.any { containsTag(it, tag) }
    }
}
