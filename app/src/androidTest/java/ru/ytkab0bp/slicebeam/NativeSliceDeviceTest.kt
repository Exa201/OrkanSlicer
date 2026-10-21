package ru.ytkab0bp.slicebeam

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult
import ru.ytkab0bp.slicebeam.slic3r.BooleanOperation
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.Vec3d
import java.io.File
import java.io.FileOutputStream

/** Runs an ARM64 native slice in an isolated app id, away from production app data. */
@RunWith(AndroidJUnit4::class)
class NativeSliceDeviceTest {
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
        val gcodeFile = File(context.cacheDir, "standalone-text-slice.gcode")
        var model: Model? = null
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
            sliceResult = model.slice(generatedConfig.absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertTrue("native slicer should emit G-code for standalone text", gcode.isNotBlank())
            assertTrue("text G-code should contain deposited moves", Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
            Log.i("NativeSliceDeviceTest", "PASS standalone text G-code bytes=${gcodeFile.length()}")
        } finally {
            sliceResult?.release()
            model?.release()
            fontFile.delete()
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
}
