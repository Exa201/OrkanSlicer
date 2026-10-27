package ru.ytkab0bp.slicebeam

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ThreeMfProjectSettingsReader
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.GLModel
import ru.ytkab0bp.slicebeam.slic3r.PaintSession
import ru.ytkab0bp.slicebeam.render.GLRenderer
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult
import ru.ytkab0bp.slicebeam.ui.settings.OrcaFilamentSettingsCatalog
import ru.ytkab0bp.slicebeam.ui.settings.OrcaProcessSettingsCatalog
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.io.File

/** A generated 3MF fixture keeps this import regression runnable without a user's model. */
@RunWith(AndroidJUnit4::class)
class TwoColorProjectDeviceTest {
    @Test fun livePaintPreviewKeepsImportedColorsAcrossAllPartsOfACombinedObject() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "two-color-live-paint-parts.3mf")
        InstrumentationRegistry.getInstrumentation().context.assets.open("models/two-color-project.3mf").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        var model: Model? = null
        val liveOverlay = GLModel()
        val committedOverlay = GLModel()
        try {
            model = Model(file, 1)
            assertEquals(0, model.combineObjectsAsParts(intArrayOf(0, 1)))
            assertEquals(1, model.objectsCount)
            assertArrayEquals("the combined 3MF assembly must retain both imported tools",
                intArrayOf(1, 2), model.getUsedFilaments(0))

            val session = PaintSession(model, 0, GLRenderer.PAINT_MODE_COLOR)
            try {
                assertTrue("the combined multicolor object should open a live paint session", session.isValid())
                val liveTriangles = session.buildOverlay(liveOverlay, 2)
                assertTrue(
                    "live preview must retain the untouched second volume's imported color; " +
                        "it must not disappear until the paint session is confirmed",
                    liveTriangles > 0,
                )
                val committedTriangles = model.buildPaintOverlay(0, committedOverlay, 2)
                assertEquals("live and committed overlays must cover the same second-volume facets",
                    committedTriangles, liveTriangles)
            } finally {
                session.end()
            }
        } finally {
            liveOverlay.release()
            committedOverlay.release()
            model?.release()
            file.delete()
        }
    }

    @Test fun explicitUniformObjectFilamentChoiceReplacesThreeMfPartAssignment() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "two-color-assignment.3mf")
        InstrumentationRegistry.getInstrumentation().context.assets.open("models/two-color-project.3mf").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        var model: Model? = null
        try {
            model = Model(file, 1)
            assertArrayEquals(intArrayOf(1), model.getUsedFilaments(0))
            assertArrayEquals(intArrayOf(2), model.getUsedFilaments(1))

            // This is the native operation used by the explicit object-slot UI action.
            model.setUniformFilament(0, 2)

            assertEquals(2, model.getExtruder(0))
            assertArrayEquals("The 3MF's old part material must no longer win", intArrayOf(2), model.getUsedFilaments(0))
            assertFalse("Uniform assignment removes the object's imported paint overrides", model.hasPaint(0))
            assertArrayEquals("The other object remains unchanged", intArrayOf(2), model.getUsedFilaments(1))
        } finally {
            model?.release()
            file.delete()
        }
    }

    @Test fun paintedFilamentAssignmentsSurviveMultipartJoinThreeMfRoundTripAndSlice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        val previousConfig = SliceBeam.CONFIG
        val source = File(context.cacheDir, "two-color-paint-source.3mf")
        val exported = File(context.cacheDir, "two-color-painted-roundtrip.3mf")
        val gcodeFile = File(context.cacheDir, "two-color-painted-roundtrip.gcode")
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigFile = configFile.takeIf { it.isFile }?.readBytes()
        var original: Model? = null
        var restored: Model? = null
        var sliceResult: GCodeProcessorResult? = null
        Prefs.init(context.applicationContext as android.app.Application)
        context.assets.open("orca_profiles/Anycubic.ini").use { SliceBeam.CONFIG = Slic3rConfigWrapper(it) }
        SliceBeam.requireConfig().presets = ConfigObject("presets").apply {
            put("printer", "Anycubic Kobra 3 0.4 nozzle")
            put("print", "0.16mm Standard")
            put("filament", "Anycubic Generic PLA")
        }
        InstrumentationRegistry.getInstrumentation().context.assets.open("models/two-color-project.3mf").use { input ->
            source.outputStream().use { input.copyTo(it) }
        }
        try {
            original = Model(source, 1)
            assertFalse("fixture object should begin without facet painting", original.hasPaint(0))
            val session = PaintSession(original, 0, GLRenderer.PAINT_MODE_COLOR)
            try {
                assertTrue("paint session should initialize for the bundled model", session.isValid())
                // Paint a band intersecting the cube's actual surface. The fixture has a 6 mm
                // cube with triangles at z=0 and z=6; using the full bounding-box range can
                // leave horizontal facets on a boundary and produce no selector assignments.
                session.height(2.0, 4.0, 2)
                session.commit()
            } finally {
                session.end()
            }
            assertTrue("painting should be stored in the model", original.hasPaint(0))
            val usedFilaments = original.getUsedFilaments(0)
            assertTrue("painting should assign filament 2", usedFilaments.contains(2))
            val paintedFacets = original.paintMaxFilament(0)
            assertTrue("committed painting should contain filament 2 facets (max=$paintedFacets)", paintedFacets >= 2)

            // Give the separate source object the same base tool so any T1 in the final G-code
            // can only come from the painted facets copied with the joined target volume.
            original.setUniformFilament(1, 1)
            assertEquals("joining a painted object with a second part should be atomic", 0,
                original.combineObjectsAsParts(intArrayOf(0, 1)))
            assertEquals(1, original.objectsCount)
            assertArrayEquals("painted and base material assignments should coexist after joining",
                intArrayOf(1, 2), original.getUsedFilaments(0))

            SliceBeam.genCurrentConfig(original)
            original.export3mf(SliceBeam.getCurrentConfigFile().absolutePath, exported.absolutePath)
            assertTrue("export should produce a 3MF project", exported.isFile && exported.length() > 0)
            val exportHasPaint = java.util.zip.ZipFile(exported).use { zip ->
                val modelXml = zip.getInputStream(zip.getEntry("3D/3dmodel.model")).bufferedReader().use { it.readText() }
                modelXml.contains("slic3rpe:mmu_segmentation=")
            }
            assertTrue("exported 3MF should contain facet paint attributes", exportHasPaint)
            restored = Model(exported, 1)
            val paintedObjects = (0 until restored.objectsCount).filter { restored.hasPaint(it) }
            assertTrue(
                "facet paint should survive export/import; objects=${restored.objectsCount}, " +
                    "painted=$paintedObjects, used=${(0 until restored.objectsCount).map { restored.getUsedFilaments(it).contentToString() }}, " +
                    "max=${(0 until restored.objectsCount).map { restored.paintMaxFilament(it) }}",
                paintedObjects.isNotEmpty(),
            )
            assertArrayEquals(usedFilaments, restored.getUsedFilaments(0))

            // The second source part is assigned to T0, so T1 in the generated G-code can only
            // come from the painted facets carried through the multipart join and 3MF reload.
            assertEquals(1, restored.objectsCount)
            assertArrayEquals(intArrayOf(1, 2), restored.getUsedFilaments(0))
            SliceBeam.genCurrentConfig(restored)
            sliceResult = restored.slice(
                SliceBeam.getCurrentConfigFile().absolutePath,
                gcodeFile.absolutePath,
                null,
            )
            val gcode = gcodeFile.readText()
            assertTrue("the reloaded painted facets must emit a T1 tool change", Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
            assertTrue("the painted project should still produce extrusion moves", Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
        } finally {
            sliceResult?.release()
            restored?.release()
            original?.release()
            SliceBeam.CONFIG = previousConfig
            if (previousConfigFile == null) configFile.delete()
            else configFile.writeBytes(previousConfigFile)
            source.delete()
            exported.delete()
            gcodeFile.delete()
        }
    }

    @Test fun importsBothObjectsAndFilamentsWithoutPersistingProjectValues() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        val file = File(context.cacheDir, "two-color-project.3mf")
        InstrumentationRegistry.getInstrumentation().context.assets.open("models/two-color-project.3mf").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val savedConfig = SliceBeam.getConfigFile().takeIf { it.exists() }?.readBytes()
        val previousConfig = SliceBeam.CONFIG
        var model: Model? = null
        try {
            val processKeys = Slic3rConfigWrapper.PRINT_CONFIG_KEYS.toSet() +
                OrcaProcessSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys }
            val filamentKeys = Slic3rConfigWrapper.FILAMENT_CONFIG_KEYS.toSet() +
                OrcaFilamentSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys }
            val settings = ThreeMfProjectSettingsReader.read(
                file, PrintConfigDef.getInstance().options, processKeys, filamentKeys,
            )
            assertEquals(2, settings.filaments.size)
            assertTrue(settings.invalidValues.isEmpty())
            model = Model(file, 1)
            assertEquals(2, model.objectsCount)
            assertEquals(2, model.requiredFilamentCount)
            assertArrayEquals(intArrayOf(1), model.getUsedFilaments(0))
            assertArrayEquals(intArrayOf(2), model.getUsedFilaments(1))
            TemporaryProjectSettings.activate(settings)
            assertEquals(2, TemporaryProjectSettings.slots()?.size)
            assertTrue("3MF printer profile must not replace the active one", SliceBeam.CONFIG === previousConfig)
            assertArrayEquals(savedConfig, SliceBeam.getConfigFile().takeIf { it.exists() }?.readBytes())
        } finally {
            TemporaryProjectSettings.clear()
            model?.release()
            file.delete()
        }
    }
}
