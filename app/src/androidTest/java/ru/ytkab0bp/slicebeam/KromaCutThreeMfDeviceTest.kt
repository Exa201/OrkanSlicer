package ru.ytkab0bp.slicebeam

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.config.ThreeMfImportSelection
import ru.ytkab0bp.slicebeam.config.ThreeMfProjectSettingsReader
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult
import ru.ytkab0bp.slicebeam.slic3r.GLModel
import ru.ytkab0bp.slicebeam.slic3r.LargeModelPick
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef
import ru.ytkab0bp.slicebeam.slic3r.PaintSession
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.render.GLRenderer
import ru.ytkab0bp.slicebeam.ui.settings.OrcaFilamentSettingsCatalog
import ru.ytkab0bp.slicebeam.ui.settings.OrcaProcessSettingsCatalog
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.Vec3d
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync
import ru.ytkab0bp.slicebeam.utils.SliceFilamentColorResolver
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

@RunWith(AndroidJUnit4::class)
class KromaCutThreeMfDeviceTest {
    @After fun clearTemporarySettings() {
        TemporaryProjectSettings.clear()
        ProfileSettingsRepository.clearAllDrafts()
    }

    private fun sourceFile(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File(context.getExternalFilesDir(null), "kromacut-user-sample.3mf")
    }

    private fun projectSettings(file: File) = ThreeMfProjectSettingsReader.read(
        file,
        PrintConfigDef.getInstance().options,
        Slic3rConfigWrapper.PRINT_CONFIG_KEYS.toSet() + OrcaProcessSettingsCatalog.categories()
            .flatMap { it.sections }.flatMap { it.keys },
        Slic3rConfigWrapper.FILAMENT_CONFIG_KEYS.toSet() + OrcaFilamentSettingsCatalog.categories()
            .flatMap { it.sections }.flatMap { it.keys },
    )

    @Test fun allImportChoiceCombinationsRemainIndependentForTheProvidedProject() {
        val source = sourceFile()
        assumeTrue("Push the supplied KromaCut 3MF to the app external files test directory", source.isFile)
        val fileSettings = projectSettings(source)
        assertEquals("The KromaCut project contains two color/material slots", 2, fileSettings.filaments.size)
        assertEquals("The project print profile contains a distinctive layer height", "0.24", fileSettings.process["layer_height"])

        // Synthetic list only: this validates the import-choice mapping and does not change
        // the user's printer, ACE capacity, saved profile, or hardware slot configuration.
        val ownSlots = listOf(FilamentSlot(0x112233, "PLA"), FilamentSlot(0x445566, "PETG"))
        for (usePrint in listOf(false, true)) {
            for (useFilament in listOf(false, true)) {
                for (usePrinter in listOf(false, true)) {
                    val selected = requireNotNull(
                        ThreeMfImportSelection.select(fileSettings, usePrint, useFilament, ownSlots, usePrinter),
                    )
                    assertEquals("print selection=$usePrint", usePrint, selected.printSettingsImported)
                    assertEquals("filament selection=$useFilament", useFilament, selected.filamentSettingsImported)
                    assertEquals("printer selection=$usePrinter", usePrinter, selected.printerSettingsImported)
                    assertEquals("print-only values are gated independently", usePrint, selected.process.containsKey("layer_height"))
                    assertEquals("bed choice is gated independently", usePrinter && fileSettings.process.containsKey("curr_bed_type"),
                        selected.process.containsKey("curr_bed_type"))
                    if (!useFilament) {
                        assertTrue("declining file filament settings must retain only the project's visual colors",
                            selected.filaments.all { it.keys == setOf("filament_colour", "filament_type") })
                    }
                    TemporaryProjectSettings.activate(selected)
                    val effective = ConfigObject().apply {
                        put("layer_height", "0.16")
                        put("curr_bed_type", "Cool Plate")
                        put("nozzle_temperature", "205,210")
                        put("filament_colour", "#112233;#445566")
                        put("filament_type", "PETG;ABS")
                    }
                    TemporaryProjectSettings.applyTo(effective, fileSettings.filaments.size)
                    assertEquals("only selected print settings should replace the current process value",
                        if (usePrint) "0.24" else "0.16", effective["layer_height"])
                    assertEquals("a missing file bed setting must never invent or replace a bed choice",
                        "Cool Plate", effective["curr_bed_type"])
                    assertEquals("visual part colors are preserved regardless of profile choices",
                        fileSettings.filaments.map { it.getValue("filament_colour") }.joinToString(";"), effective["filament_colour"])
                    assertEquals("only selected file filament settings should replace current material types",
                        if (useFilament) fileSettings.filaments.map { it.getValue("filament_type") }.joinToString(";")
                        else "PLA;PETG", effective["filament_type"])
                    assertEquals("this file has no embedded temperatures, so current filament temperatures stay active",
                        "205,210", effective["nozzle_temperature"])
                }
            }
        }
    }

    @Test fun detailedKromaCutCanBePaintedWithoutChangingItsOriginalMesh() {
        val source = sourceFile()
        assumeTrue("Push the supplied KromaCut 3MF to the app external files test directory", source.isFile)
        val model = Model(source, 1)
        var paint: PaintSession? = null
        val overlay = GLModel()
        try {
            assertEquals("the supplied detailed KromaCut mesh should be exercised", 165_264,
                model.getTriangleCount(0))
            assertFalse("KromaCut coloring must not require destructive mesh simplification",
                LargeModelPick.needsPreciseSurfaceSimplification(model.getTriangleCount(0)))

            val min = Vec3d()
            val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            paint = PaintSession(model, 0, GLRenderer.PAINT_MODE_COLOR)
            assertTrue("the detailed original mesh should initialize a color paint session", paint.isValid())
            paint.height(min.z + (max.z - min.z) * 0.25, min.z + (max.z - min.z) * 0.75, 2)
            assertTrue("the live overlay must contain painted faces before confirmation",
                paint.buildOverlay(overlay, 2) > 0)
            paint.commit()

            assertEquals("painting must preserve the exact original triangle count", 165_264,
                model.getTriangleCount(0))
            assertTrue("the committed original mesh must retain the selected filament", model.hasPaint(0))
            assertTrue(model.getUsedFilaments(0).contains(2))
        } finally {
            paint?.end()
            overlay.release()
            model.release()
        }
    }

    @Test fun providedProjectSlicesWithOwnSettingsAndUniformObjectReassignment() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val source = sourceFile()
        assumeTrue("Push the supplied KromaCut 3MF to the app external files test directory", source.isFile)

        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        val previousConfig = SliceBeam.CONFIG
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigFile = configFile.takeIf { it.isFile }?.readBytes()
        val model = Model(source, 1)
        val outputs = ArrayList<File>()
        try {
            context.assets.open("orca_profiles/Anycubic.ini").use { SliceBeam.CONFIG = Slic3rConfigWrapper(it) }
            SliceBeam.requireConfig().presets = ConfigObject("presets").apply {
                put("printer", "Anycubic Kobra 3 0.4 nozzle")
                put("print", "0.16mm Standard")
                put("filament", "Anycubic Generic PLA")
            }
            val savedProfiles = SliceBeam.requireConfig().serialize()

            val settings = projectSettings(source)
            assertEquals(2, settings.filaments.size)
            assertEquals(1, model.objectsCount)
            assertEquals(2, model.requiredFilamentCount)
            assertArrayEquals(intArrayOf(1, 2), model.getUsedFilaments(0))
            val baseline = SliceBeam.buildCurrentConfigObject(model.requiredFilamentCount)
            assertNotEquals("the current profile must differ from the embedded 3MF print value", settings.process["layer_height"], baseline["layer_height"])

            val ownSlots = Prefs.getStoredFilamentSlots()
            val colorsOnly = requireNotNull(ThreeMfImportSelection.select(settings, false, false, ownSlots, false))
            TemporaryProjectSettings.activate(colorsOnly)
            val ownSettingsConfig = SliceBeam.buildCurrentConfigObject(model.requiredFilamentCount)
            assertEquals(baseline["layer_height"], ownSettingsConfig["layer_height"])
            assertEquals(baseline["nozzle_temperature"], ownSettingsConfig["nozzle_temperature"])
            assertEquals(settings.filaments.map { it.getValue("filament_colour") }.joinToString(";"), ownSettingsConfig["filament_colour"])
            assertEquals("choosing no embedded settings leaves saved profiles unchanged", savedProfiles,
                SliceBeam.requireConfig().serialize())

            sliceAndVerify(model, ownSettingsConfig, "own-settings-two-color")?.let(outputs::add)

            val editedPalette = requireNotNull(TemporaryProjectSettings.slots())
            editedPalette[0].color = 0xFF00AA55.toInt()
            TemporaryProjectSettings.setSlots(editedPalette)
            val recoloredConfig = SliceBeam.buildCurrentConfigObject(model.requiredFilamentCount)
            assertEquals("#00AA55;#0C0C0B", recoloredConfig["filament_colour"])
            sliceAndVerify(model, recoloredConfig, "edited-palette-two-color")?.let(outputs::add)

            val min = Vec3d()
            val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val paint = PaintSession(model, 0, GLRenderer.PAINT_MODE_COLOR)
            try {
                assertTrue("paint session should initialize for the imported KromaCut object", paint.isValid())
                paint.height(min.z + (max.z - min.z) * 0.25, min.z + (max.z - min.z) * 0.75, 2)
                paint.commit()
            } finally {
                paint.end()
            }
            assertTrue("paint operation should retain the second assigned filament", model.getUsedFilaments(0).contains(2))
            sliceAndVerify(model, recoloredConfig, "painted-two-color")?.let(outputs::add)

            val printOnly = requireNotNull(ThreeMfImportSelection.select(settings, true, false, ownSlots, false))
            TemporaryProjectSettings.activate(printOnly)
            val printOnlyConfig = SliceBeam.buildCurrentConfigObject(model.requiredFilamentCount)
            assertEquals("0.24", printOnlyConfig["layer_height"])
            assertEquals(baseline["nozzle_temperature"], printOnlyConfig["nozzle_temperature"])
            sliceAndVerify(model, printOnlyConfig, "print-only-two-color")?.let(outputs::add)

            val filamentOnly = requireNotNull(ThreeMfImportSelection.select(settings, false, true, ownSlots, false))
            TemporaryProjectSettings.activate(filamentOnly)
            val filamentOnlyConfig = SliceBeam.buildCurrentConfigObject(model.requiredFilamentCount)
            assertEquals(baseline["layer_height"], filamentOnlyConfig["layer_height"])
            assertEquals(settings.filaments.map { it.getValue("filament_colour") }.joinToString(";"), filamentOnlyConfig["filament_colour"])
            sliceAndVerify(model, filamentOnlyConfig, "filament-only-two-color")?.let(outputs::add)

            val printAndFilament = requireNotNull(ThreeMfImportSelection.select(settings, true, true, ownSlots, false))
            TemporaryProjectSettings.activate(printAndFilament)
            val bothSettingsConfig = SliceBeam.buildCurrentConfigObject(model.requiredFilamentCount)
            assertEquals("0.24", bothSettingsConfig["layer_height"])
            sliceAndVerify(model, bothSettingsConfig, "print-and-filament-two-color")?.let(outputs::add)

            // This is the exact model operation called by the object's explicit filament picker.
            TemporaryProjectSettings.activate(colorsOnly)
            model.setUniformFilament(0, 2)
            assertArrayEquals(intArrayOf(2), model.getUsedFilaments(0))
            assertFalse("uniform reassignment must remove imported per-part color assignments", model.hasPaint(0))
            val reassignedConfig = SliceBeam.buildCurrentConfigObject(model.requiredFilamentCount)
            sliceAndVerify(model, reassignedConfig, "uniform-slot-two")?.let(outputs::add)

            assertEquals("temporary project settings must not mutate installed profile data", savedProfiles,
                SliceBeam.requireConfig().serialize())
        } finally {
            model.release()
            outputs.forEach { it.delete() }
            SliceBeam.CONFIG = previousConfig
            if (previousConfigFile == null) configFile.delete() else configFile.writeBytes(previousConfigFile)
        }
    }

    @Test fun selectedBlueAndWhiteAceGatesAreUsedByTheRealKromaCutSlice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val source = sourceFile()
        assumeTrue("Push the supplied KromaCut 3MF to the app external files test directory", source.isFile)

        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        Prefs.init(context.applicationContext as SliceBeam)
        val prefs = requireNotNull(Prefs.getPrefs())
        val snapshotKeys = listOf("mmu_snapshot_printer", "mmu_snapshot")
        val oldSnapshot = snapshotKeys.associateWith { prefs.getString(it, null) }
        val previousConfig = SliceBeam.CONFIG
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigFile = configFile.takeIf { it.isFile }?.readBytes()
        val model = Model(source, 1)
        val outputs = ArrayList<File>()
        try {
            context.assets.open("orca_profiles/Anycubic.ini").use { SliceBeam.CONFIG = Slic3rConfigWrapper(it) }
            SliceBeam.requireConfig().presets = ConfigObject("presets").apply {
                put("printer", "Anycubic Kobra 3 0.4 nozzle")
                put("print", "0.16mm Standard")
                put("filament", "Anycubic Generic PLA")
            }
            val printer = requireNotNull(SliceBeam.requireConfig().findPrinter("Anycubic Kobra 3 0.4 nozzle"))
            val title = printer.title.orEmpty()
            val fakeMmu = JSONObject().put(
                "result", JSONObject().put(
                    "status", JSONObject().put(
                        "mmu", JSONObject()
                            .put("enabled", true)
                            .put("ttg_map", JSONArray(listOf(0, 1, 2, 3)))
                            .put("gate_status", JSONArray(listOf(1, 1, 1, 1)))
                            .put("gate_material", JSONArray(listOf("PETG", "PETG", "PETG", "PLA")))
                            .put("gate_color", JSONArray(listOf("212721", "FFFFFF", "00BA4B", "0047BB"))),
                    ),
                ),
            )
            prefs.edit().putString("mmu_snapshot_printer", title)
                .putString("mmu_snapshot", fakeMmu.toString()).commit()

            val fileSettings = projectSettings(source)
            assertEquals(2, fileSettings.filaments.size)
            val importedColors = requireNotNull(ThreeMfImportSelection.select(
                fileSettings, false, false, Prefs.getStoredFilamentSlots(), false,
            ))
            TemporaryProjectSettings.activate(importedColors)
            val originalMaterialValues = SliceBeam.buildCurrentConfigObject(model.requiredFilamentCount)["filament_type"]
            // Exact mapping from the reported device flow: source T0 -> white ACE2, T1 -> blue ACE4.
            MmuFilamentSync.setSelectedGates(title, intArrayOf(1, 3))
            val mappedConfig = SliceBeam.buildCurrentConfigObject(model.requiredFilamentCount)
            assertEquals("#FFFFFF;#0047BB", mappedConfig["filament_colour"])
            assertEquals("gate color routing must not change slicer material/profile values",
                originalMaterialValues, mappedConfig["filament_type"])

            sliceAndVerify(model, mappedConfig, "selected-blue-white-ace-gates")?.let(outputs::add)
            val gcode = requireNotNull(outputs.singleOrNull())
            val body = gcode.readText()
            assertTrue("slicer must retain both material tools in the real G-code", Regex("(?m)^T0(?:\\s|$)").containsMatchIn(body))
            assertTrue("slicer must retain both material tools in the real G-code", Regex("(?m)^T1(?:\\s|$)").containsMatchIn(body))
            assertTrue("slicer G-code preview palette must show the selected white and blue gates",
                body.contains("; filament_colour = #FFFFFF;#0047BB"))
        } finally {
            model.release()
            outputs.forEach { it.delete() }
            TemporaryProjectSettings.clear()
            val editor = prefs.edit()
            oldSnapshot.forEach { (key, value) -> if (value == null) editor.remove(key) else editor.putString(key, value) }
            editor.commit()
            SliceBeam.CONFIG = previousConfig
            if (previousConfigFile == null) configFile.delete() else configFile.writeBytes(previousConfigFile)
        }
    }

    private fun sliceAndVerify(model: Model, config: ConfigObject, label: String): File? {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SliceBeam.genCurrentConfig(model)
        val generatedConfig = SliceBeam.getCurrentConfigFile().readText()
        assertConfigValue(generatedConfig, "layer_height", config["layer_height"])
        assertConfigValue(generatedConfig, "filament_colour", config["filament_colour"])
        val output = File(context.cacheDir, "kromacut-${UUID.randomUUID()}-$label.gcode")
        var result: GCodeProcessorResult? = null
        try {
            result = model.slice(SliceBeam.getCurrentConfigFile().absolutePath, output.absolutePath, null)
            // The 3D G-code viewer renders from this result palette, so verify the preview
            // uses the exact colors passed into native slicing for this slice.
            val expectedPreviewPalette = SliceFilamentColorResolver.resolve(
                config["filament_colour"], model.requiredFilamentCount, Prefs.getFilamentPalette(),
            )
            assertArrayEquals(
                "the 3D G-code preview must render with the same effective colors native slicing received",
                expectedPreviewPalette,
                result.previewToolColors,
            )
            assertTrue("slice should produce a non-empty G-code file", output.isFile && output.length() > 0)
            val content = output.readText()
            val used = model.getUsedFilaments(0).map { it - 1 }.toSet()
            val emitted = Regex("(?m)^T(\\d+)(?:\\s|$)").findAll(content).map { it.groupValues[1].toInt() }.toSet()
            assertTrue("all assigned material tools should occur in G-code; used=$used emitted=$emitted", emitted.containsAll(used))
            val advertised = Regex("(?m)^; total layer number: (\\d+)$").find(content)?.groupValues?.get(1)?.toInt()
            val actual = Regex("(?m)^;LAYER_CHANGE$").findAll(content).count()
            assertTrue("G-code must advertise a positive layer count", (advertised ?: 0) > 0)
            assertEquals("G-code layer count must match the emitted layers", advertised, actual)
            assertTrue("G-code configuration block must be complete", content.contains("CONFIG_BLOCK_END"))
            assertTrue("G-code must identify this slicer build", content.contains("; generated by ") && content.contains(BuildConfig.VERSION_NAME))
            config["filament_colour"]?.let { palette ->
                val emittedPalette = Regex("(?m)^; filament_colour = (.*)$").find(content)?.groupValues?.get(1)
                assertEquals(
                    "G-code must use the selected/imported palette, not stale 3MF overrides",
                    palette, emittedPalette,
                )
            }
            return output
        } catch (failure: Throwable) {
            output.delete()
            throw failure
        } finally {
            result?.release()
        }
    }

    private fun assertConfigValue(config: String, key: String, expected: String?) {
        if (expected == null) return
        val line = Regex("(?m)^${Regex.escape(key)} = (.*)$").find(config)?.groupValues?.get(1)
        assertEquals("generated slice config should use the selected $key value", expected, line)
    }
}
