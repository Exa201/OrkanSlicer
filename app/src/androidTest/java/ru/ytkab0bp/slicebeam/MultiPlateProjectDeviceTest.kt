package ru.ytkab0bp.slicebeam

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.slic3r.Model
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Exercises the native per-plate 3MF reader with different colors on two build plates. */
@RunWith(AndroidJUnit4::class)
class MultiPlateProjectDeviceTest {
    @Test fun loadsEachPlateAndItsOwnColorFromTheSameProject() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        val source = File(context.cacheDir, "two-color-source.3mf")
        val project = File(context.cacheDir, "two-plate-two-color.3mf")
        instrumentation.context.assets.open("models/two-color-project.3mf").use { input ->
            source.outputStream().use { input.copyTo(it) }
        }
        var first: Model? = null
        var second: Model? = null
        try {
            ZipFile(source).use { original ->
                ZipOutputStream(project.outputStream()).use { output ->
                    original.entries().asSequence().forEach { entry ->
                        val bytes = original.getInputStream(entry).use { it.readBytes() }
                        val content = if (entry.name == "Metadata/model_settings.config") {
                            val plates = """<plate><metadata key="plater_id" value="1"/><model_instance><metadata key="object_id" value="1"/><metadata key="instance_id" value="0"/></model_instance></plate><plate><metadata key="plater_id" value="2"/><model_instance><metadata key="object_id" value="2"/><metadata key="instance_id" value="0"/></model_instance></plate>"""
                            bytes.toString(Charsets.UTF_8).replace("</config>", "$plates</config>")
                                .toByteArray(Charsets.UTF_8)
                        } else bytes
                        output.putNextEntry(ZipEntry(entry.name))
                        output.write(content)
                        output.closeEntry()
                    }
                }
            }
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
}
