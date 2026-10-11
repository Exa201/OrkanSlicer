package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.config.ConfigObject
import java.io.File
import java.util.Base64
import java.util.zip.ZipFile

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
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=",
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
                val storedPreview = zip.getInputStream(zip.getEntry("Metadata/plate_1.png")).use { it.readBytes() }
                assertTrue(storedPreview.contentEquals(thumbnail))
            }
        } finally {
            source.delete()
            packageFile.delete()
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

    private fun qidiQ2() = ConfigObject("Qidi Q2 0.4 nozzle").apply {
        put("printer_model", "Q2")
        put("nozzle_diameter", "0.4")
    }
}
