package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.config.ConfigObject
import java.io.File
import java.util.Base64
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class QidiGCode3mfPackagerTest {
    @Test
    fun createsSinglePlateQidiPackageWithPreviewFilamentDataAndMatchingChecksum() {
        val printer = qidiQ2()
        val source = File.createTempFile("qidi-q2", ".gcode")
        val packageFile = File.createTempFile("qidi-q2", ".gcode.3mf")
        try {
            source.writeText(
                """; total layer number: 2
                    |; max_z_height: 0.40
                    |; estimated printing time (normal mode) = 1h 2m 3s
                    |; nozzle_diameter = 0.4
                    |; layer_height = 0.2
                    |; filament_type = PLA,PETG
                    |; filament_colour = #FF0000,#00AAFF
                    |; filament_map = 1,2
                    |; filament used [mm] = 1000,500
                    |; total filament used [g] = 5
                    |EXCLUDE_OBJECT_DEFINE NAME=clamp POLYGON=[[10,20],[30,20],[30,40],[10,40],[10,20]]
                    |T0
                    |T1
                    |""".trimMargin(),
            )
            val thumbnail = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=",
            )
            val summary = QidiGCode3mfPackager.create(
                sourceGcode = source,
                destination = packageFile,
                previewPng = thumbnail,
                printer = printer,
                filamentSlots = listOf(FilamentSlot(0xFFFF0000.toInt(), "PLA"), FilamentSlot(0xFF00AAFF.toInt(), "PETG")),
                clientVersion = "test-version",
                displayName = "clamp.gcode.3mf",
            )

            assertEquals(2, summary.layers)
            assertEquals(3723, summary.durationSeconds)
            assertEquals(2, summary.filamentCount)
            assertEquals("clamp.gcode.3mf", QidiGCode3mfPackager.uploadName("clamp.gcode"))
            assertTrue(QidiGCode3mfPackager.verify(packageFile))
            ZipFile(packageFile).use { zip ->
                assertEquals(16, zip.entries().asSequence().count())
                val requiredEntries = listOf(
                    "[Content_Types].xml",
                    "_rels/.rels",
                    "3D/3dmodel.model",
                    "Metadata/plate_1.gcode",
                    "Metadata/plate_1.gcode.md5",
                    "Metadata/plate_1.png",
                    "Metadata/pick_1.png",
                    "Metadata/model_settings.config",
                    "Metadata/project_settings.config",
                    "Metadata/slice_info.config",
                    "Metadata/filament_sequence.json",
                )
                requiredEntries.forEach { assertTrue("Missing package entry $it", zip.getEntry(it) != null) }
                val storedGcode = zip.getInputStream(zip.getEntry("Metadata/plate_1.gcode")).use { it.readBytes() }
                assertTrue("The package must preserve the slicer's toolpath byte-for-byte", storedGcode.contentEquals(source.readBytes()))
                val sliceInfo = zip.getInputStream(zip.getEntry("Metadata/slice_info.config")).bufferedReader().use { it.readText() }
                assertTrue(sliceInfo.contains("key=\"printer_model_id\" value=\"Q2\""))
                assertTrue(sliceInfo.contains("key=\"filament_maps\" value=\"1 2\""))
                assertTrue(sliceInfo.contains("type=\"PLA\" color=\"#FF0000\""))
                assertTrue(sliceInfo.contains("name=\"clamp\""))
                val relationships = zip.getInputStream(zip.getEntry("_rels/.rels")).bufferedReader().use { it.readText() }
                assertTrue(relationships.contains("http://schemas.qiditech.com/package/2021/cover-thumbnail-middle"))
                assertTrue(relationships.contains("http://schemas.qiditech.com/package/2021/cover-thumbnail-small"))
                val storedPreview = zip.getInputStream(zip.getEntry("Metadata/plate_1.png")).use { it.readBytes() }
                assertTrue(storedPreview.contentEquals(thumbnail))
            }
        } finally {
            source.delete()
            packageFile.delete()
        }
    }

    @Test
    fun includesFullSettingsForQidiPrintDetailsIncludingSettingsAfterFormerLimit() {
        val source = File.createTempFile("qidi-full-settings", ".gcode")
        val target = File.createTempFile("qidi-full-settings", ".gcode.3mf")
        val png = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=",
        )
        try {
            val settings = buildString {
                appendLine("; total layer number: 1")
                repeat(300) { appendLine("; test_setting_$it = value_$it") }
                appendLine("; layer_height = 0.16")
                appendLine("; first_layer_height = 0.24")
                appendLine("; outer_wall_speed = 120")
                appendLine("; travel_speed = 500")
                appendLine("; filament_type = PLA,PETG")
                appendLine("; filament_colour = #112233,#AABBCC")
                appendLine("; filament_map = 1,2")
                appendLine("T0")
                appendLine("T1")
            }
            source.writeText(settings)
            QidiGCode3mfPackager.create(source, target, png, qidiQ2(), emptyList(), "test", "details")
            ZipFile(target).use { zip ->
                val projectSettings = zip.getInputStream(zip.getEntry("Metadata/project_settings.config"))
                    .bufferedReader().use { it.readText() }
                assertTrue(projectSettings.contains("\"layer_height\": \"0.16\""))
                assertTrue(projectSettings.contains("\"first_layer_height\": \"0.24\""))
                assertTrue(projectSettings.contains("\"outer_wall_speed\": \"120\""))
                assertTrue(projectSettings.contains("\"travel_speed\": \"500\""))
                assertTrue(projectSettings.contains("\"test_setting_299\": \"value_299\""))
                assertTrue(projectSettings.contains("\"filament_colour\": [\"#112233\", \"#AABBCC\"]"))
            }
        } finally {
            source.delete()
            target.delete()
        }
    }

    @Test
    fun requiresARealPreviewAndDoesNotClassifyQ2CAsQ2() {
        assertFalse(QidiGCode3mfPackager.isQidiQ2(ConfigObject("Qidi Q2C 0.4 nozzle").apply { put("printer_model", "Q2C") }))
        assertTrue(QidiGCode3mfPackager.isQidiQ2(qidiQ2()))
        val source = File.createTempFile("qidi-no-preview", ".gcode")
        val target = File.createTempFile("qidi-no-preview", ".gcode.3mf")
        try {
            source.writeText("; total layer number: 1\nT0\n")
            try {
                QidiGCode3mfPackager.create(source, target, null, qidiQ2(), emptyList(), "test", "print.gcode.3mf")
                throw AssertionError("A missing preview must not create a package with a blank/fake picture")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("preview"))
            }
            assertFalse(target.length() > 0L)
        } finally {
            source.delete()
            target.delete()
        }
    }

    @Test
    fun rejectsTruncatedOrCorruptedPngPreviewsBeforePackaging() {
        val source = File.createTempFile("qidi-bad-preview", ".gcode")
        val target = File.createTempFile("qidi-bad-preview", ".gcode.3mf")
        try {
            source.writeText("; total layer number: 1\nT0\n")
            val png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=",
            )
            val corrupt = png.clone().also { it[45] = (it[45].toInt() xor 1).toByte() }
            for (preview in listOf(png.copyOf(24), corrupt)) {
                try {
                    QidiGCode3mfPackager.create(source, target, preview, qidiQ2(), emptyList(), "test", "print.gcode.3mf")
                    throw AssertionError("A broken PNG must not be accepted as a printer preview")
                } catch (expected: IllegalArgumentException) {
                    assertTrue(expected.message.orEmpty().contains("preview"))
                }
                assertEquals(0L, target.length())
            }
        } finally {
            source.delete()
            target.delete()
        }
    }

    @Test
    fun rejectsExcessiveToolNumbersBeforeAllocatingFilamentMetadata() {
        val source = File.createTempFile("qidi-tool-limit", ".gcode")
        val target = File.createTempFile("qidi-tool-limit", ".gcode.3mf")
        val png = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=",
        )
        try {
            for (tool in listOf("T16", "T999999999999999999999999")) {
                source.writeText("; total layer number: 1\n$tool\n")
                try {
                    QidiGCode3mfPackager.create(source, target, png, qidiQ2(), emptyList(), "test", "part")
                    throw AssertionError("Accepted an unsupported tool: $tool")
                } catch (expected: IllegalArgumentException) {
                    assertTrue(expected.message.orEmpty().contains("tool number"))
                }
            }
        } finally {
            source.delete()
            target.delete()
        }
    }

    @Test
    fun rejectsAnInvalidSecondaryPreviewInsideTheCompletedPackage() {
        val source = File.createTempFile("qidi-preview-verify", ".gcode")
        val target = File.createTempFile("qidi-preview-verify", ".gcode.3mf")
        val damaged = File.createTempFile("qidi-preview-damaged", ".gcode.3mf")
        val png = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=",
        )
        try {
            source.writeText("; total layer number: 1\nT0\n")
            QidiGCode3mfPackager.create(source, target, png, qidiQ2(), emptyList(), "test", "part")
            ZipFile(target).use { zip ->
                ZipOutputStream(damaged.outputStream()).use { output ->
                    zip.entries().asSequence().forEach { entry ->
                        output.putNextEntry(ZipEntry(entry.name))
                        if (entry.name == "Metadata/top_1.png") output.write(byteArrayOf(1, 2, 3))
                        else zip.getInputStream(entry).use { it.copyTo(output) }
                        output.closeEntry()
                    }
                }
            }
            assertFalse(QidiGCode3mfPackager.verify(damaged))
        } finally {
            source.delete()
            target.delete()
            damaged.delete()
        }
    }

    private fun qidiQ2() = ConfigObject("Qidi Q2 0.4 nozzle").apply {
        put("printer_model", "Q2")
        put("nozzle_diameter", "0.4")
    }
}
