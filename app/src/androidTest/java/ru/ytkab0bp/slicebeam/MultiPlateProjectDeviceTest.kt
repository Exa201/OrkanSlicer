package ru.ytkab0bp.slicebeam

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Vec3d
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Exercises the native per-plate 3MF reader with different colors on two build plates. */
@RunWith(AndroidJUnit4::class)
class MultiPlateProjectDeviceTest {
    @Test fun eachPlateModelSurvivesNativeThreeMfExportAndReload() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val application = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = application
        Prefs.init(application)
        val prefs = requireNotNull(Prefs.getPrefs())
        val previousPalette = prefs.getString("filament_palette", null)
        val previousSyncedPrinter = prefs.getString("mmu_synced_printer", null)
        val previousConfig = SliceBeam.CONFIG
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigBytes = configFile.takeIf { it.isFile }?.readBytes()
        context.assets.open("orca_profiles/Anycubic.ini").use { SliceBeam.CONFIG = Slic3rConfigWrapper(it) }
        SliceBeam.CONFIG!!.presets = ConfigObject("presets").apply {
            put("printer", "Anycubic Kobra 3 0.4 nozzle")
            put("print", "0.16mm Standard")
            put("filament", "Anycubic Generic PLA")
        }
        Prefs.setMmuSyncedPrinter(null)
        Prefs.setStoredFilamentSlots(listOf(
            FilamentSlot(0xFFCC3333.toInt(), "PLA"),
            FilamentSlot(0xFF3366CC.toInt(), "PETG"),
        ))

        try {
            listOf(1, 3, 6, 10, 20).forEach { plateCount ->
                val source = File(context.cacheDir, "plate-roundtrip-source-$plateCount.3mf")
                val project = File(context.cacheDir, "plate-roundtrip-$plateCount.3mf")
                createTwoPlateProject(source, project, plateCount)
                try {
                    for (plateId in 1..plateCount) {
                        val exported = File(context.cacheDir, "plate-roundtrip-$plateCount-$plateId.3mf")
                        var original: Model? = null
                        var restored: Model? = null
                        try {
                            original = Model(project, plateId)
                            assertEquals("plate $plateId/$plateCount model count", 1, original.objectsCount)
                            val expectedTool = if (plateId % 2 == 1) 1 else 2
                            assertTrue(original.getUsedFilaments(0).contentEquals(intArrayOf(expectedTool)))

                            // Make object placement identifiable in the exported project and compare
                            // native model state after a real export, release, and native reload.
                            original.translate(0, plateId * 3.0, plateId * -2.0, 0.0)
                            val expectedPosition = Vec3d().also { original.getTranslation(0, it) }
                            SliceBeam.genCurrentConfig(original)
                            original.export3mf(configFile.absolutePath, exported.absolutePath)
                            original.release()
                            original = null

                            assertTrue("plate $plateId/$plateCount export missing", exported.isFile && exported.length() > 0)
                            restored = Model(exported, 1)
                            assertEquals("reloaded plate $plateId/$plateCount model count", 1, restored.objectsCount)
                            val restoredTools = restored.getUsedFilaments(0)
                            // Native single-plate export compacts unused tool indices. The
                            // separate two-color project roundtrip covers exact multi-tool ids.
                            assertTrue("reloaded plate $plateId/$plateCount has no filament assignment",
                                restoredTools.isNotEmpty())
                            val actualPosition = Vec3d().also { restored.getTranslation(0, it) }
                            assertEquals("plate $plateId/$plateCount x placement", expectedPosition.x, actualPosition.x, 0.01)
                            assertEquals("plate $plateId/$plateCount y placement", expectedPosition.y, actualPosition.y, 0.01)
                            assertEquals("plate $plateId/$plateCount z placement", expectedPosition.z, actualPosition.z, 0.01)
                        } finally {
                            restored?.release()
                            original?.release()
                            exported.delete()
                        }
                    }
                } finally {
                    source.delete()
                    project.delete()
                }
            }
        } finally {
            SliceBeam.CONFIG = previousConfig
            if (previousConfigBytes == null) configFile.delete() else configFile.writeBytes(previousConfigBytes)
            prefs.edit().also { editor ->
                if (previousPalette == null) editor.remove("filament_palette")
                else editor.putString("filament_palette", previousPalette)
                if (previousSyncedPrinter == null) editor.remove("mmu_synced_printer")
                else editor.putString("mmu_synced_printer", previousSyncedPrinter)
            }.commit()
        }
    }

    @Test fun nativeProjectLoadsEveryPlateForSmallAndLargeProjects() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        val counts = listOf(1, 3, 6, 10, 20)
        counts.forEach { plateCount ->
            val source = File(context.cacheDir, "plate-stress-source-$plateCount.3mf")
            val project = File(context.cacheDir, "plate-stress-$plateCount.3mf")
            createTwoPlateProject(source, project, plateCount)
            try {
                ZipFile(project).use { zip ->
                    val settings = zip.getInputStream(zip.getEntry("Metadata/model_settings.config"))
                        .bufferedReader().use { it.readText() }
                    assertEquals("detected plate count for $plateCount", plateCount,
                        ThreeMfPlateCountDetector.detect(settings,
                            zip.entries().asSequence().map { it.name }.toList()))
                }
                for (plateId in 1..plateCount) {
                    val model = Model(project, plateId)
                    try {
                        assertEquals("plate $plateId/$plateCount should retain one object", 1, model.objectsCount)
                        val expectedTool = if (plateId % 2 == 1) 1 else 2
                        assertTrue("plate $plateId/$plateCount should retain tool $expectedTool",
                            model.getUsedFilaments(0).contentEquals(intArrayOf(expectedTool)))
                    } finally {
                        model.release()
                    }
                }
            } finally {
                source.delete()
                project.delete()
            }
        }
    }

    @Test fun loadsEachPlateAndItsOwnColorFromTheSameProject() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        val source = File(context.cacheDir, "two-color-source.3mf")
        val project = File(context.cacheDir, "two-plate-two-color.3mf")
        createTwoPlateProject(source, project)
        var first: Model? = null
        var second: Model? = null
        try {
            ZipFile(project).use { zip ->
                val settings = zip.getInputStream(zip.getEntry("Metadata/model_settings.config"))
                    .bufferedReader().use { it.readText() }
                assertEquals(2, ThreeMfPlateCountDetector.detect(settings,
                    zip.entries().asSequence().map { it.name }.toList()))
            }
            first = Model(project, 1)
            second = Model(project, 2)
            assertEquals(1, first.objectsCount)
            assertEquals(1, second.objectsCount)
            assertTrue(first.getUsedFilaments(0).contentEquals(intArrayOf(1)))
            assertTrue(second.getUsedFilaments(0).contentEquals(intArrayOf(2)))
        } finally {
            first?.release()
            second?.release()
            source.delete()
            project.delete()
        }
    }

    @Test fun workspaceLoadsTheSecondPlateWithoutLosingTheFirst() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val application = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = application
        Prefs.init(application)
        Prefs.getPrefs()?.edit()?.remove("crash")
            ?.putBoolean("converter_storage_first_prompt", true)?.commit()
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

        val source = File(context.cacheDir, "two-color-workspace-source.3mf")
        val project = File(context.cacheDir, "two-plate-workspace.3mf")
        createTwoPlateProject(source, project)
        val firstLoaded = CountDownLatch(1)
        val secondLoaded = CountDownLatch(1)
        val loadFailure = AtomicReference<Exception?>()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                bed.loadModel(project, true, 2, object : BedFragment.ModelLoadCallback {
                    override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                        firstLoaded.countDown()
                    }
                    override fun onFailed(error: Exception) {
                        loadFailure.set(error)
                        firstLoaded.countDown()
                    }
                })
            }
            assertTrue("First plate did not finish loading", firstLoaded.await(60, TimeUnit.SECONDS))
            loadFailure.get()?.let { throw AssertionError("First plate failed", it) }
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                assertEquals(2, bed.plateCount)
                assertEquals(1, bed.currentModel?.objectsCount)
                assertTrue(bed.currentModel!!.getUsedFilaments(0).contentEquals(intArrayOf(1)))
                val switch = BedFragment::class.java.getDeclaredMethod("switchPlate",
                    Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, BedFragment.ModelLoadCallback::class.java)
                    .apply { isAccessible = true }
                switch.invoke(bed, 1, true, BedFragment.ModelLoadCallback { _, _, _ -> secondLoaded.countDown() })
            }
            assertTrue("Second plate did not finish loading", secondLoaded.await(60, TimeUnit.SECONDS))
            scenario.onActivity { activity ->
                val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
                assertEquals(2, bed.plateCount)
                assertEquals(1, bed.currentModel?.objectsCount)
                assertTrue(bed.currentModel!!.getUsedFilaments(0).contentEquals(intArrayOf(2)))
            }
        } finally {
            scenario.close()
            source.delete()
            project.delete()
        }
    }

    private fun createTwoPlateProject(source: File, project: File, plateCount: Int = 2) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.context.assets.open("models/two-color-project.3mf").use { input ->
            source.outputStream().use { input.copyTo(it) }
        }
        ZipFile(source).use { original ->
            ZipOutputStream(project.outputStream()).use { output ->
                original.entries().asSequence().forEach { entry ->
                    val bytes = original.getInputStream(entry).use { it.readBytes() }
                    val content = if (entry.name == "Metadata/model_settings.config") {
                        val plates = (1..plateCount).joinToString("") { plateId ->
                            val objectId = if (plateId % 2 == 1) 1 else 2
                            """<plate><metadata key="plater_id" value="$plateId"/><model_instance><metadata key="object_id" value="$objectId"/><metadata key="instance_id" value="0"/></model_instance></plate>"""
                        }
                        bytes.toString(Charsets.UTF_8).replace("</config>", "$plates</config>")
                            .toByteArray(Charsets.UTF_8)
                    } else bytes
                    output.putNextEntry(ZipEntry(entry.name))
                    output.write(content)
                    output.closeEntry()
                }
            }
        }
    }
}
