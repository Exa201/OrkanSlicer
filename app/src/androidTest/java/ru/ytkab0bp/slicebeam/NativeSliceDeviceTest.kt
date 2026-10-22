package ru.ytkab0bp.slicebeam

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult
import ru.ytkab0bp.slicebeam.slic3r.BooleanOperation
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.slic3r.ObjectBounds3D
import ru.ytkab0bp.slicebeam.slic3r.ObjectPlacement
import ru.ytkab0bp.slicebeam.utils.Vec3d
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

/** Runs an ARM64 native slice in an isolated app id, away from production app data. */
@RunWith(AndroidJUnit4::class)
class NativeSliceDeviceTest {
    @Test
    fun separateObjectsAssignedToDifferentFilamentsEmitToolChange() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "separate-filaments-slice.stl")
        val model = Model(modelFile)
        try {
            model.addObject(model, 0)
            model.translate(1, 30.0, 0.0, 0.0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            assertEquals(listOf(1), model.getUsedFilaments(0).toList())
            assertEquals(listOf(2), model.getUsedFilaments(1).toList())

            val gcode = sliceToGcode(context, model, "separate-filaments-slice.gcode")

            assertHasExtrusion(gcode)
            assertTrue("the second object's T1 assignment must reach emitted G-code", Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test
    fun combinedMultipartObjectKeepsBothFilamentsThroughSlicing() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "combined-filaments-slice.stl")
        val roundTripFile = File(context.cacheDir, "combined-filaments-roundtrip.3mf")
        val model = Model(modelFile)
        var restored: Model? = null
        try {
            model.addObject(model, 0)
            model.translate(1, 30.0, 0.0, 0.0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)

            assertEquals(0, model.combineObjectsAsParts(intArrayOf(0, 1)))
            assertEquals("both part assignments must remain on the combined object", listOf(1, 2), model.getUsedFilaments(0).toList())

            SliceBeam.genCurrentConfig(model)
            model.export3mf(SliceBeam.getCurrentConfigFile().absolutePath, roundTripFile.absolutePath)
            ZipFile(roundTripFile).use { archive ->
                val entry = archive.getEntry("Metadata/Slic3r_PE_model.config")
                val metadata = entry?.let { archive.getInputStream(it).bufferedReader().use { reader -> reader.readText() } }
                assertTrue("second part's filament must be present in the 3MF volume metadata",
                    metadata.orEmpty().contains("type=\"volume\" key=\"extruder\" value=\"2\""))
            }
            val roundTrip = Model(roundTripFile, 1).also { restored = it }
            assertEquals(1, roundTrip.objectsCount)
            assertEquals("both part assignments must survive 3MF save/reload", listOf(1, 2), roundTrip.getUsedFilaments(0).toList())

            val gcode = sliceToGcode(context, roundTrip, "combined-filaments-slice.gcode")

            assertHasExtrusion(gcode)
            assertTrue("the second part's T1 assignment must reach emitted G-code", Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            restored?.release()
            model.release()
            modelFile.delete()
            roundTripFile.delete()
        }
    }

    @Test
    fun placedObjectGroupRemainsPrintableAfterSurfacePlacement() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "placed-group-slice.stl")
        val projectFile = File(context.cacheDir, "placed-group-roundtrip.3mf")
        val model = Model(modelFile)
        var restored: Model? = null
        try {
            model.addObject(model, 0)
            model.addObject(model, 0)
            model.translate(1, 30.0, -10.0, 8.0)
            model.translate(2, 65.0, 12.0, 14.0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            model.setUniformFilament(2, 2)

            fun bounds(index: Int): ObjectBounds3D {
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(index, min, max)
                return ObjectBounds3D(min.x, min.y, min.z, max.x, max.y, max.z)
            }
            val target = bounds(0)
            val movingGroup = requireNotNull(ObjectPlacement.enclosing(listOf(bounds(1), bounds(2))))
            val delta = requireNotNull(ObjectPlacement.onTop(movingGroup, target))
            model.translate(1, delta.x, delta.y, delta.z)
            model.translate(2, delta.x, delta.y, delta.z)

            val placed = requireNotNull(ObjectPlacement.enclosing(listOf(bounds(1), bounds(2))))
            assertEquals(target.maxZ, placed.minZ, 0.001)
            // Orca rejects separate ModelObjects whose first printable layer is above
            // the bed. A stacked assembly must become multipart geometry before slicing.
            assertEquals(0, model.combineObjectsAsParts(intArrayOf(0, 1, 2)))
            assertEquals(1, model.objectsCount)
            assertEquals("surface placement assembly must retain target and placed-part tools",
                listOf(1, 2), model.getUsedFilaments(0).toList())
            SliceBeam.genCurrentConfig(model)
            val exportConfig = SliceBeam.getCurrentConfigFile()
            try {
                model.export3mf(exportConfig.absolutePath, projectFile.absolutePath)
            } finally {
                exportConfig.delete()
            }
            restored = Model(projectFile, 1)
            assertEquals("surface-placed multipart object must survive project reload", 1, restored.objectsCount)
            assertEquals("surface placement must retain the target and placed-part tools after reload",
                listOf(1, 2), restored.getUsedFilaments(0).toList())
            val gcode = sliceToGcode(context, restored, "placed-group-slice.gcode")
            assertHasExtrusion(gcode)
            assertTrue("the placed assembly's second material must reach G-code",
                Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            restored?.release()
            model.release()
            modelFile.delete()
            projectFile.delete()
        }
    }

    @Test
    fun booleanResultUsesTargetObjectsFilamentAndStillSlices() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "boolean-filament-slice.stl")
        val projectFile = File(context.cacheDir, "boolean-filament-roundtrip.3mf")
        val model = Model(modelFile)
        var restored: Model? = null
        try {
            model.addObject(model, 0)
            model.translate(1, 10.0, 0.0, 0.0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)

            assertEquals(0, model.booleanObjects(0, 1, BooleanOperation.UNION))
            assertEquals("a single Boolean solid should keep the target material", listOf(1), model.getUsedFilaments(0).toList())

            SliceBeam.genCurrentConfig(model)
            val exportConfig = SliceBeam.getCurrentConfigFile()
            try {
                model.export3mf(exportConfig.absolutePath, projectFile.absolutePath)
            } finally {
                exportConfig.delete()
            }
            restored = Model(projectFile, 1)
            assertEquals("the Boolean result should survive project reload", 1, restored.objectsCount)
            assertEquals("reload must keep the target material and omit the removed cutter material",
                listOf(1), restored.getUsedFilaments(0).toList())
            val gcode = sliceToGcode(context, restored, "boolean-filament-slice.gcode")

            assertHasExtrusion(gcode)
            assertFalse("the removed Boolean tool's T1 must not remain assigned", Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            restored?.release()
            model.release()
            modelFile.delete()
            projectFile.delete()
        }
    }

    @Test
    fun importedSvgSolidProducesExtrusionGcode() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)

        val previousConfig = SliceBeam.CONFIG
        context.assets.open("orca_profiles/Anycubic.ini").use { SliceBeam.CONFIG = Slic3rConfigWrapper(it) }
        val config = requireNotNull(SliceBeam.CONFIG)
        config.presets = ConfigObject("presets").apply {
            put("printer", requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle")).title)
            put("print", requireNotNull(config.findPrint("0.16mm Standard")).title)
            put("filament", requireNotNull(config.findFilament("Anycubic Generic PLA")).title)
        }

        val svgFile = File(context.cacheDir, "native-svg-slice-test.svg")
        val gcodeFile = File(context.cacheDir, "native-svg-slice-test.gcode")
        var model: Model? = null
        var sliceResult: GCodeProcessorResult? = null
        var generatedConfig: File? = null
        try {
            svgFile.writeText(
                """<svg xmlns="http://www.w3.org/2000/svg" width="24mm" height="18mm" viewBox="0 0 24 18">
                    <path id="frame" fill="#202020" d="M1 1 H23 V17 H1 Z M7 6 H17 V12 H7 Z"/>
                </svg>""".trimIndent(),
            )
            model = Model(svgFile)
            assertEquals("SVG import must expose printable mesh geometry", 1, model.objectsCount)
            assertTrue(model.getTriangleCount(0) > 0)
            assertTrue(model.getObjectVolume(0) > 0.0)

            SliceBeam.genCurrentConfig(model)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            sliceResult = model.slice(generatedConfig.absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertTrue("Imported SVG should produce non-empty G-code", gcode.isNotBlank())
            assertTrue("Imported SVG should produce deposited toolpaths", Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
            Log.i("NativeSliceDeviceTest", "PASS SVG G-code bytes=${gcodeFile.length()}")
        } finally {
            sliceResult?.release()
            model?.release()
            svgFile.delete()
            gcodeFile.delete()
            generatedConfig?.delete()
            SliceBeam.CONFIG = previousConfig
        }
    }

    @Test
    fun disjointBooleanUnionKeepsItsInstanceBoundsAndProducesGcode() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)

        val previousConfig = SliceBeam.CONFIG
        context.assets.open("orca_profiles/Anycubic.ini").use { SliceBeam.CONFIG = Slic3rConfigWrapper(it) }
        val config = requireNotNull(SliceBeam.CONFIG)
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", process.title)
            put("filament", filament.title)
        }

        val modelFile = File(context.cacheDir, "boolean-disjoint-slice.stl")
        val gcodeFile = File(context.cacheDir, "boolean-disjoint-slice.gcode")
        var model: Model? = null
        var sliceResult: GCodeProcessorResult? = null
        var generatedConfig: File? = null
        try {
            context.assets.open("models/xyz_cube.stl").use { input -> modelFile.outputStream().use(input::copyTo) }
            model = Model(modelFile)
            val initialMin = Vec3d(); val initialMax = Vec3d()
            model.getBoundingBoxExact(0, initialMin, initialMax)
            model.addObject(model, 0)
            model.translate(1, (initialMax.x - initialMin.x) * 1.5, 0.0, 0.0)

            val result = model.booleanObjects(0, 1, BooleanOperation.UNION)
            assertEquals("Boolean union should leave one valid model object", 0, result)
            val resultMin = Vec3d(); val resultMax = Vec3d()
            model.getBoundingBoxExact(result, resultMin, resultMax)
            assertEquals((initialMax.x - initialMin.x) * 2.5, resultMax.x - resultMin.x, 0.02)

            SliceBeam.genCurrentConfig(model)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            sliceResult = model.slice(generatedConfig.absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertTrue("Native slicer should emit G-code for the disjoint Boolean result", gcode.isNotBlank())
            assertTrue("G-code should include at least one deposited move", Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
            Log.i("NativeSliceDeviceTest", "PASS disjoint Boolean G-code bytes=${gcodeFile.length()}")
        } finally {
            sliceResult?.release()
            model?.release()
            modelFile.delete()
            gcodeFile.delete()
            generatedConfig?.delete()
            SliceBeam.CONFIG = previousConfig
        }
    }

    @Test
    fun standalonePrintableTextProducesExtrusionGcode() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)

        val previousConfig = SliceBeam.CONFIG
        context.assets.open("orca_profiles/Anycubic.ini").use { SliceBeam.CONFIG = Slic3rConfigWrapper(it) }
        val config = requireNotNull(SliceBeam.CONFIG)
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", process.title)
            put("filament", filament.title)
        }

        val fontFile = File(context.cacheDir, "standalone-text-slice-font.ttf")
        val projectFile = File(context.cacheDir, "standalone-text-roundtrip.3mf")
        val gcodeFile = File(context.cacheDir, "standalone-text-slice.gcode")
        var model: Model? = null
        var restored: Model? = null
        var sliceResult: GCodeProcessorResult? = null
        var generatedConfig: File? = null
        try {
            context.assets.open("font/roboto_medium.ttf").use { input -> fontFile.outputStream().use(input::copyTo) }
            model = Model()
            val textIndex = model.createTextObject(
                fontFile.absolutePath,
                "ORKAN",
                18f,
                1.2f,
                Vec3d(90.0, 90.0, 0.0),
                Vec3d(0.0, 0.0, 1.0),
            )
            assertEquals(0, textIndex)
            assertTrue("standalone text should contain printable triangles", model.getTriangleCount(textIndex) > 0)

            SliceBeam.genCurrentConfig(model)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            model.export3mf(generatedConfig.absolutePath, projectFile.absolutePath)
            generatedConfig.delete()
            generatedConfig = null
            restored = Model(projectFile, 1)
            assertEquals("standalone text should remain a separate object after 3MF reload", 1, restored.objectsCount)
            assertEquals("text label should survive project reload", "ORKAN", restored.getObjectName(0))
            assertTrue("reloaded text should remain a printable mesh", restored.getTriangleCount(0) > 0)

            SliceBeam.genCurrentConfig(restored)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            sliceResult = restored.slice(generatedConfig.absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertTrue("native slicer should emit G-code for standalone text", gcode.isNotBlank())
            assertTrue("text G-code should contain deposited moves", Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
            Log.i("NativeSliceDeviceTest", "PASS standalone text G-code bytes=${gcodeFile.length()}")
        } finally {
            sliceResult?.release()
            restored?.release()
            model?.release()
            fontFile.delete()
            projectFile.delete()
            gcodeFile.delete()
            generatedConfig?.delete()
            SliceBeam.CONFIG = previousConfig
        }
    }

    @Test
    fun bundledKobraProfileSlicesBoxLocally() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)

        context.assets.open("orca_profiles/Anycubic.ini").use { input ->
            SliceBeam.CONFIG = Slic3rConfigWrapper(input)
        }
        val config = requireNotNull(SliceBeam.CONFIG)
        val printer: ConfigObject? = config.findPrinter("Anycubic Kobra 3 0.4 nozzle")
        val process: ConfigObject? = config.findPrint("0.16mm Standard")
        val filament: ConfigObject? = config.findFilament("Anycubic Generic PLA")
        assertNotNull("Bundled Kobra 3 printer preset", printer)
        assertNotNull("Bundled print preset", process)
        assertNotNull("Bundled PLA filament preset", filament)

        val selectedPresets = ConfigObject("presets")
        config.presets = selectedPresets
        selectedPresets.put("printer", requireNotNull(printer).title)
        selectedPresets.put("print", requireNotNull(process).title)
        selectedPresets.put("filament", requireNotNull(filament).title)

        val modelFile = File(context.cacheDir, "native-slice-device-test.stl")
        val gcodeFile = File(context.cacheDir, "native-slice-device-test.gcode")
        var model: Model? = null
        var result: GCodeProcessorResult? = null
        try {
            context.assets.open("models/box.stl").use { input ->
                FileOutputStream(modelFile).use { output -> input.copyTo(output) }
            }
            model = Model(modelFile)
            assertTrue("The bundled STL must load as one object", model.objectsCount == 1)
            SliceBeam.genCurrentConfig(model)
            result = model.slice(
                SliceBeam.getCurrentConfigFile().absolutePath,
                gcodeFile.absolutePath,
                null,
            )
            assertTrue(
                "Native Orca/libslic3r must produce non-empty G-code",
                gcodeFile.isFile && gcodeFile.length() > 0,
            )
            Log.i("NativeSliceDeviceTest", "PASS local G-code bytes=${gcodeFile.length()}")
        } finally {
            result?.release()
            model?.release()
            modelFile.delete()
            SliceBeam.getCurrentConfigFile().delete()
            gcodeFile.delete()
        }
    }

    @Test
    fun bridgeSettingsReachNativeConfigAndGenerateBridgeGcode() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)

        context.assets.open("orca_profiles/Anycubic.ini").use { input ->
            SliceBeam.CONFIG = Slic3rConfigWrapper(input)
        }
        val config = requireNotNull(SliceBeam.CONFIG)
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        process.put("bridge_line_width", "0.35")
        process.put("bridge_flow", "1.05")
        process.put("bridge_angle", "37")
        process.put("internal_bridge_angle", "61")
        process.put("relative_bridge_angle", "1")
        filament.put("enable_pressure_advance", "1")
        filament.put("pressure_advance", "0.042")

        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", process.title)
            put("filament", filament.title)
        }

        val modelFile = File(context.cacheDir, "bridge-settings-device-test.stl")
        val gcodeFile = File(context.cacheDir, "bridge-settings-device-test.gcode")
        var model: Model? = null
        var result: GCodeProcessorResult? = null
        try {
            context.assets.open("models/OrcaCube_v2.stl").use { input ->
                FileOutputStream(modelFile).use { output -> input.copyTo(output) }
            }
            model = Model(modelFile)
            assertTrue("The Orca Cube fixture must load", model.objectsCount == 1)
            SliceBeam.genCurrentConfig(model)
            val nativeConfig = SliceBeam.getCurrentConfigFile().readText()
            assertTrue(nativeConfig.contains("bridge_line_width = 0.35"))
            assertTrue(nativeConfig.contains("bridge_flow = 1.05"))
            assertTrue(nativeConfig.contains("bridge_angle = 37"))
            assertTrue(nativeConfig.contains("internal_bridge_angle = 61"))
            assertTrue(nativeConfig.contains("relative_bridge_angle = 1"))
            assertTrue(nativeConfig.contains("enable_pressure_advance = 1"))
            assertTrue(nativeConfig.contains("pressure_advance = 0.042"))

            result = model.slice(
                SliceBeam.getCurrentConfigFile().absolutePath,
                gcodeFile.absolutePath,
                null,
            )
            assertTrue("Native Orca/libslic3r must produce bridge G-code",
                gcodeFile.isFile && gcodeFile.length() > 0)
            val gcode = gcodeFile.readText()
            assertTrue("The Orca Cube slice must contain a bridge role", gcode.contains("TYPE:Bridge"))
            assertTrue("A staged filament pressure-advance value must reach Klipper G-code",
                gcode.contains("SET_PRESSURE_ADVANCE ADVANCE=0.042"))
            Log.i("NativeSliceDeviceTest", "PASS bridge G-code bytes=${gcodeFile.length()}")
        } finally {
            result?.release()
            model?.release()
            modelFile.delete()
            SliceBeam.getCurrentConfigFile().delete()
            gcodeFile.delete()
        }
    }

    @Test
    fun selectedBedSurfaceTemperatureReachesNativeGcode() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)

        context.assets.open("orca_profiles/Anycubic.ini").use { input ->
            SliceBeam.CONFIG = Slic3rConfigWrapper(input)
        }
        val config = requireNotNull(SliceBeam.CONFIG)
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        printer.put("curr_bed_type", "Textured PEI Plate")
        printer.put("support_multi_bed_types", "1")
        printer.put("machine_start_gcode", "M140 S[bed_temperature_initial_layer_single]\\nM190 S[bed_temperature_initial_layer_single]")
        filament.put("hot_plate_temp_initial_layer", "81")
        filament.put("hot_plate_temp", "80")
        filament.put("textured_plate_temp_initial_layer", "62")
        filament.put("textured_plate_temp", "61")
        filament.put("cool_plate_temp_initial_layer", "43")
        filament.put("cool_plate_temp", "42")
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", process.title)
            put("filament", filament.title)
        }

        val modelFile = File(context.cacheDir, "bed-surface-temperature-device-test.stl")
        val gcodeFile = File(context.cacheDir, "bed-surface-temperature-device-test.gcode")
        var model: Model? = null
        var result: GCodeProcessorResult? = null
        try {
            context.assets.open("models/box.stl").use { input -> modelFile.outputStream().use { input.copyTo(it) } }
            model = Model(modelFile)
            SliceBeam.genCurrentConfig(model)
            val nativeConfig = SliceBeam.getCurrentConfigFile().readText()
            assertTrue(nativeConfig.contains("curr_bed_type = Textured PEI Plate"))
            assertTrue(nativeConfig.contains("textured_plate_temp_initial_layer = 62"))
            assertTrue(nativeConfig.contains("hot_plate_temp_initial_layer = 81"))

            result = model.slice(SliceBeam.getCurrentConfigFile().absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertTrue("G-code must heat the selected Textured PEI surface at 62 C", gcode.contains("M140 S62"))
            assertTrue("G-code must not substitute the high-temperature value", !gcode.contains("M140 S81"))
            Log.i("NativeSliceDeviceTest", "PASS selected bed-surface G-code bytes=${gcodeFile.length()}")
        } finally {
            result?.release()
            model?.release()
            modelFile.delete()
            SliceBeam.getCurrentConfigFile().delete()
            gcodeFile.delete()
        }
    }

    private inline fun withKobraProfile(block: () -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        val previousConfig = SliceBeam.CONFIG
        try {
            context.assets.open("orca_profiles/Anycubic.ini").use { SliceBeam.CONFIG = Slic3rConfigWrapper(it) }
            val config = requireNotNull(SliceBeam.CONFIG)
            config.presets = ConfigObject("presets").apply {
                put("printer", requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle")).title)
                put("print", requireNotNull(config.findPrint("0.16mm Standard")).title)
                put("filament", requireNotNull(config.findFilament("Anycubic Generic PLA")).title)
            }
            block()
        } finally {
            SliceBeam.CONFIG = previousConfig
        }
    }

    private fun copyCube(context: Context, name: String): File = File(context.cacheDir, name).also { target ->
        context.assets.open("models/xyz_cube.stl").use { input -> target.outputStream().use(input::copyTo) }
    }

    private fun sliceToGcode(context: Context, model: Model, name: String): String {
        val gcodeFile = File(context.cacheDir, name)
        var generatedConfig: File? = null
        var result: GCodeProcessorResult? = null
        try {
            SliceBeam.genCurrentConfig(model)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            result = model.slice(generatedConfig.absolutePath, gcodeFile.absolutePath, null)
            return gcodeFile.readText()
        } finally {
            result?.release()
            gcodeFile.delete()
            generatedConfig?.delete()
        }
    }

    private fun assertHasExtrusion(gcode: String) {
        assertTrue("Slicing should produce non-empty G-code", gcode.isNotBlank())
        assertTrue("Slicing should produce deposited moves", Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
    }
}
