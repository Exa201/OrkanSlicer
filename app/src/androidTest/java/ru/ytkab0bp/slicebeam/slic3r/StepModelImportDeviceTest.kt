package ru.ytkab0bp.slicebeam.slic3r

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import ru.ytkab0bp.slicebeam.utils.Vec3d
import java.io.File

@RunWith(AndroidJUnit4::class)
class StepModelImportDeviceTest {
    @Test fun importsStepAndStpAsRenderableTriangleMeshesWithSelectedMeshingOptions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File(context.cacheDir, "step-cube-fixture-source.step")
        InstrumentationRegistry.getInstrumentation().context.assets.open("step_cube_ccby4.step").use { input ->
            source.outputStream().use { output -> input.copyTo(output) }
        }

        try {
            for (extension in listOf("step", "stp")) {
                val file = File(context.cacheDir, "step-cube-fixture.$extension")
                source.copyTo(file, overwrite = true)
                val progress = mutableListOf<Float>()
                val model = Model(file, 0, StepImportOptions(0.01, 0.5, splitCompound = false), ImportProgressCallback {
                    progress += it
                    true
                })
                try {
                    assertEquals(".$extension should produce one solid object", 1, model.objectsCount)
                    assertTrue("STEP conversion should create triangles", model.getTriangleCount(0) > 0)
                    val min = Vec3d()
                    val max = Vec3d()
                    model.getBoundingBoxExact(0, min, max)
                    assertEquals(10.0, max.x - min.x, 0.05)
                    assertEquals(10.0, max.y - min.y, 0.05)
                    assertEquals(10.0, max.z - min.z, 0.05)
                    assertTrue("STEP progress stays normalized", progress.all { it in 0f..1f })
                } finally {
                    model.release()
                    file.delete()
                }
            }

            // Splitting a non-compound STEP solid must remain a valid, renderable single object.
            val splitModel = Model(source, 0, StepImportOptions(0.003, 0.5, splitCompound = true))
            try {
                assertEquals(1, splitModel.objectsCount)
                assertTrue(splitModel.getTriangleCount(0) > 0)
            } finally {
                splitModel.release()
            }
        } finally {
            source.delete()
        }
    }
}
