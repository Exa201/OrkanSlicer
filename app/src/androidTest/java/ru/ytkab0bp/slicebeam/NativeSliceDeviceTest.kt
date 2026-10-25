package ru.ytkab0bp.slicebeam

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult
import ru.ytkab0bp.slicebeam.slic3r.BooleanOperation
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.slic3r.ObjectBounds3D
import ru.ytkab0bp.slicebeam.slic3r.ObjectDistributionAxis
import ru.ytkab0bp.slicebeam.slic3r.ObjectGroupDistribution
import ru.ytkab0bp.slicebeam.slic3r.ObjectPlacement
import ru.ytkab0bp.slicebeam.utils.Vec3d
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

/** Runs an ARM64 native slice in an isolated app id, away from production app data. */
@RunWith(AndroidJUnit4::class)
class NativeSliceDeviceTest {
    @Test
    fun invalidTransformAndSimplifyRequestsPreserveNativeModel() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyAsset(context, "models/sphere.stl", "invalid-transform-edits.stl")
        val model = Model(modelFile)
        try {
            val originalTriangles = model.getTriangleCount(0)
            val originalVolume = model.getObjectVolume(0)
            val min = Vec3d()
            val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val originalBounds = doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z)

            model.translate(-1, 10.0, 0.0, 0.0)
            model.translate(0, Double.NaN, 0.0, 0.0)
            model.scale(model.objectsCount, 2.0, 2.0, 2.0)
            model.scale(0, 1.0, Double.POSITIVE_INFINITY, 1.0)
            model.rotate(-1, 0.0, 0.0, 1.0)
            model.rotate(0, 0.0, Double.NaN, 0.0)
            model.ensureOnBed(model.objectsCount)
            model.resetRotation(-1)
            model.autoOrient(model.objectsCount)
            model.translate(Double.NaN, 0.0, 0.0)
            model.simplify(-1, 0.5f)
            assertEquals("invalid simplify ratio is rejected", 0, model.simplify(0, Float.NaN))
            assertEquals("out-of-range simplify ratio is rejected", 0, model.simplify(0, 1.1f))
            assertEquals("invalid object triangle query is safe", 0, model.getTriangleCount(model.objectsCount))
            assertEquals("invalid object volume query is safe", 0.0, model.getObjectVolume(-1), 0.0)
            assertFalse("invalid object paint query is safe", model.hasPaint(model.objectsCount))
            assertEquals("invalid object paint-tool query is safe", 0, model.paintMaxFilament(-1))
            model.getBoundingBoxExact(model.objectsCount, min, max)
            assertArrayEquals("invalid exact bounds return an empty box", DoubleArray(6),
                doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z), 0.0)
            model.getBoundingBoxApprox(-1, min, max)
            assertArrayEquals("invalid approximate bounds return an empty box", DoubleArray(6),
                doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z), 0.0)
            val overlay = ru.ytkab0bp.slicebeam.slic3r.GLModel()
            try {
                assertEquals("invalid object paint overlay is safe", 0,
                    model.buildPaintOverlay(model.objectsCount, overlay, 1))
            } finally {
                overlay.release()
            }

            assertEquals(1, model.objectsCount)
            assertEquals(originalTriangles, model.getTriangleCount(0))
            assertEquals(originalVolume, model.getObjectVolume(0), 1e-6)
            model.getBoundingBoxExact(0, min, max)
            doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z).forEachIndexed { axis, value ->
                assertEquals("invalid edits preserve bounds on axis $axis", originalBounds[axis], value, 1e-6)
            }
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test
    fun invalidDuplicateDeleteAndSplitRequestsPreserveNativeModel() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "invalid-object-index-edits.stl")
        val model = Model(modelFile)
        try {
            val originalTriangles = model.getTriangleCount(0)
            val originalVolume = model.getObjectVolume(0)
            val originalBounds = DoubleArray(6).also { bounds ->
                val min = Vec3d()
                val max = Vec3d()
                model.getBoundingBoxExact(0, min, max)
                bounds[0] = min.x; bounds[1] = min.y; bounds[2] = min.z
                bounds[3] = max.x; bounds[4] = max.y; bounds[5] = max.z
            }

            assertEquals("linked duplication rejects a missing object", -1,
                model.duplicateInstance(model.objectsCount, 0, Vec3d(10.0, 0.0, 0.0)))
            assertEquals("linked duplication rejects a negative source instance", -1,
                model.duplicateInstance(0, -1, Vec3d(10.0, 0.0, 0.0)))
            model.deleteObject(model.objectsCount)
            assertEquals("split rejects a negative object index", 0, model.split(-1))

            assertEquals("invalid edits preserve the object count", 1, model.objectsCount)
            assertEquals("invalid edits preserve the mesh", originalTriangles, model.getTriangleCount(0))
            assertEquals("invalid edits preserve solid volume", originalVolume, model.getObjectVolume(0), 1e-6)
            val min = Vec3d()
            val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val after = doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z)
            originalBounds.forEachIndexed { axis, value ->
                assertEquals("invalid edits preserve bounds on axis $axis", value, after[axis], 1e-6)
            }
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test
    fun invalidJoinBooleanAndTextRequestsLeaveModelUntouched() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "invalid-model-edit-requests.stl")
        val fontFile = copyAsset(context, "font/roboto_medium.ttf", "invalid-model-edit-font.ttf")
        val model = Model(modelFile)
        try {
            model.addObject(model, 0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            val originalCount = model.objectsCount
            val originalFilaments = (0 until originalCount).map { model.getUsedFilaments(it) }
            val min = Vec3d()
            val max = Vec3d()
            val originalBounds = (0 until originalCount).map { index ->
                model.getBoundingBoxExact(index, min, max)
                doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z)
            }

            assertEquals("duplicate join indices are rejected", -1,
                model.combineObjectsAsParts(intArrayOf(0, 0)))
            assertEquals("out-of-range Boolean indices are rejected", -1,
                model.booleanObjects(intArrayOf(0, originalCount), BooleanOperation.UNION))
            assertEquals("blank standalone text is rejected", -1, model.createTextObject(
                fontFile.absolutePath, "  ", 12f, 1.2f,
                Vec3d(90.0, 90.0, 0.0), Vec3d(0.0, 0.0, 1.0),
            ))
            assertEquals("an invalid surface target is rejected", -1, model.createTextObject(
                fontFile.absolutePath, "Label", 12f, 1.2f,
                Vec3d(90.0, 90.0, 0.0), Vec3d(0.0, 0.0, 1.0), targetObjectIndex = originalCount,
            ))

            assertEquals("invalid model edits must not add, remove, or combine objects", originalCount,
                model.objectsCount)
            (0 until originalCount).forEach { index ->
                assertArrayEquals("invalid edits preserve object $index filament assignments",
                    originalFilaments[index], model.getUsedFilaments(index))
                model.getBoundingBoxExact(index, min, max)
                val after = doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z)
                originalBounds[index].forEachIndexed { axis, value ->
                    assertEquals("invalid edits preserve object $index bounds on axis $axis", value,
                        after[axis], 0.001)
                }
            }
        } finally {
            model.release()
            modelFile.delete()
            fontFile.delete()
        }
    }

    @Test
    fun copyingAnObjectFromEmptyOrReleasedModelIsIgnoredSafely() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val targetFile = copyCube(context, "safe-invalid-object-copy-target.stl")
        val sourceFile = copyCube(context, "safe-invalid-object-copy-source.stl")
        val target = Model(targetFile)
        val empty = Model()
        val released = Model(sourceFile)
        try {
            empty.addObject(empty, 0)
            assertEquals("an empty model must not self-copy an unavailable object", 0, empty.objectsCount)

            target.addObject(empty, 0)
            assertEquals("an empty source must not append a native object", 1, target.objectsCount)

            released.release()
            target.addObject(released, 0)
            assertEquals("a released source must not append a native object", 1, target.objectsCount)

            target.addObject(target, target.objectsCount)
            assertEquals("an out-of-range index must not append a native object", 1, target.objectsCount)
        } finally {
            target.release()
            empty.release()
            released.release()
            targetFile.delete()
            sourceFile.delete()
        }
    }

    @Test
    fun xAndYDistributedObjectsRemainOnBedAndProduceExtrusionGcode() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "distributed-group-slice.stl")
        val model = Model(modelFile)
        try {
            val min = Vec3d()
            val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val width = max.x - min.x
            val depth = max.y - min.y
            model.addObject(model, 0)
            model.addObject(model, 0)
            model.translate(1, width * 2.0, depth * 2.0, 0.0)
            model.translate(2, width * 6.0, depth * 6.0, 0.0)

            for (axis in listOf(ObjectDistributionAxis.X, ObjectDistributionAxis.Y)) {
                val bounds = linkedMapOf<Int, ObjectBounds3D>()
                for (index in 0 until model.objectsCount) {
                    model.getBoundingBoxExact(index, min, max)
                    bounds[index] = ObjectBounds3D(min.x, min.y, min.z, max.x, max.y, max.z)
                }
                val deltas = requireNotNull(ObjectGroupDistribution.plan(bounds, axis))
                deltas.forEach { (index, delta) -> model.translate(index, delta.x, delta.y, delta.z) }
                val dimension = if (axis == ObjectDistributionAxis.X) 0 else 1
                val gapA = run {
                    model.getBoundingBoxExact(0, min, max); val endFirst = if (dimension == 0) max.x else max.y
                    model.getBoundingBoxExact(1, min, max); val startMiddle = if (dimension == 0) min.x else min.y
                    startMiddle - endFirst
                }
                val gapB = run {
                    model.getBoundingBoxExact(1, min, max); val endMiddle = if (dimension == 0) max.x else max.y
                    model.getBoundingBoxExact(2, min, max); val startLast = if (dimension == 0) min.x else min.y
                    startLast - endMiddle
                }
                assertEquals("equal clear gaps along $axis", gapA, gapB, 1e-6)
                for (index in 0 until model.objectsCount) {
                    model.getBoundingBoxExact(index, min, max)
                    assertTrue("XY distribution must keep object $index on the bed", min.z >= -0.05)
                }
            }

            val gcode = sliceToGcode(context, model, "distributed-group-slice.gcode")
            assertHasExtrusion(gcode)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test
    fun splitMultipartObjectPreservesEachFilamentAndStillSlices() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "split-multipart-slice.stl")
        val model = Model(modelFile)
        try {
            model.addObject(model, 0)
            model.translate(1, 30.0, 0.0, 0.0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            assertEquals(0, model.combineObjectsAsParts(intArrayOf(0, 1)))
            assertEquals(1, model.objectsCount)
            assertEquals(listOf(1, 2), model.getUsedFilaments(0).toList())

            assertEquals("Split should report both restored source bodies", 2, model.split(0))
            assertEquals("Split should restore two separately editable objects", 2, model.objectsCount)
            assertEquals(setOf(1, 2), (0 until model.objectsCount).flatMap { model.getUsedFilaments(it).toList() }.toSet())

            val gcode = sliceToGcode(context, model, "split-multipart-slice.gcode")
            assertHasExtrusion(gcode)
            assertTrue("the split T1 body must still reach G-code", Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test
    fun simplifiedMeshRemainsSolidAndProducesExtrusionGcode() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyAsset(context, "models/sphere.stl", "simplified-sphere-slice.stl")
        val model = Model(modelFile)
        try {
            val originalTriangles = model.getTriangleCount(0)
            val originalVolume = model.getObjectVolume(0)
            assertTrue("sphere fixture should contain enough triangles to simplify", originalTriangles > 8)

            val simplifiedTriangles = model.simplify(0, 0.5f)
            assertTrue("simplification should reduce triangle count", simplifiedTriangles in 4 until originalTriangles)
            assertTrue("simplification should retain a positive solid volume", model.getObjectVolume(0) > 0.0)
            assertTrue("the simplified mesh should remain a substantial part of its original volume",
                model.getObjectVolume(0) / originalVolume in 0.7..1.3)

            val gcode = sliceToGcode(context, model, "simplified-sphere-slice.gcode")
            assertHasExtrusion(gcode)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test
    fun autoOrientedModelRemainsOnBedAndProducesExtrusionGcode() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyAsset(context, "models/pyramid.stl", "auto-oriented-pyramid-slice.stl")
        val model = Model(modelFile)
        try {
            model.rotate(0, Math.toRadians(23.0), Math.toRadians(31.0), Math.toRadians(17.0))
            model.autoOrient(0)
            val min = ru.ytkab0bp.slicebeam.utils.Vec3d()
            val max = ru.ytkab0bp.slicebeam.utils.Vec3d()
            model.getBoundingBoxExact(0, min, max)
            assertTrue("auto-orient should leave a finite, positive-height object", min.z.isFinite() && max.z > min.z)
            assertTrue("auto-oriented model should remain on or above the build plate", min.z >= -0.05)
            assertTrue("auto-orient should preserve the printable solid", model.getObjectVolume(0) > 0.0)

            val gcode = sliceToGcode(context, model, "auto-oriented-pyramid-slice.gcode")
            assertHasExtrusion(gcode)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

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
            model.scale(1, 1.3, 0.8, 1.2)
            model.rotate(1, 0.35, -0.22, 0.48)
            model.translate(1, 50.0, 50.0, 0.0)
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
    fun combiningLinkedCopiesWithAnotherObjectMaterializesAllInstancesAndStillSlices() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "combine-linked-copies.stl")
        val sourceFile = copyCube(context, "combine-linked-source.stl")
        val projectFile = File(context.cacheDir, "combine-linked-roundtrip.3mf")
        val outputFile = File(context.cacheDir, "combine-linked-roundtrip.gcode")
        val model = Model(modelFile)
        val linkedSource = Model(sourceFile)
        var restored: Model? = null
        var sliceResult: GCodeProcessorResult? = null
        try {
            val initialMin = Vec3d(); val initialMax = Vec3d()
            model.getBoundingBoxExact(0, initialMin, initialMax)
            val width = initialMax.x - initialMin.x
            val trianglesPerCopy = model.getTriangleCount(0)
            assertTrue("the linked copy should be created", model.duplicateInstance(
                0, 0, Vec3d(width + 7.0, 0.0, 0.0),
            ) >= 0)
            linkedSource.setUniformFilament(0, 2)
            model.addObject(linkedSource, 0)
            model.translate(1, width * 2.0 + 14.0, 0.0, 0.0)
            model.setUniformFilament(0, 1)

            fun combinedWorldBounds(source: Model, index: Int): Pair<DoubleArray, DoubleArray> {
                val min = Vec3d(); val max = Vec3d()
                source.getBoundingBoxExact(index, min, max)
                return doubleArrayOf(min.x, min.y, min.z) to doubleArrayOf(max.x, max.y, max.z)
            }
            val sourceBounds = listOf(combinedWorldBounds(model, 0), combinedWorldBounds(model, 1))
            val expectedMin = DoubleArray(3) { axis -> sourceBounds.minOf { it.first[axis] } }
            val expectedMax = DoubleArray(3) { axis -> sourceBounds.maxOf { it.second[axis] } }

            assertEquals("linked object and separately placed object should combine", 0,
                model.combineObjectsAsParts(intArrayOf(0, 1)))
            assertEquals("all three world copies should become one printable object", 1, model.objectsCount)
            assertEquals("the linked copy must be materialized as its own part",
                trianglesPerCopy * 3, model.getTriangleCount(0))
            assertEquals("each source tool assignment should survive grouping", listOf(1, 2),
                model.getUsedFilaments(0).toList())
            val joinedBounds = combinedWorldBounds(model, 0)
            for (axis in 0..2) {
                assertEquals("joined world minimum on axis $axis", expectedMin[axis], joinedBounds.first[axis], 0.02)
                assertEquals("joined world maximum on axis $axis", expectedMax[axis], joinedBounds.second[axis], 0.02)
            }

            SliceBeam.genCurrentConfig(model)
            model.export3mf(SliceBeam.getCurrentConfigFile().absolutePath, projectFile.absolutePath)
            restored = Model(projectFile, 1)
            assertEquals("the grouped project should reload as one object", 1, restored.objectsCount)
            assertEquals("both materials should survive the 3MF round trip", listOf(1, 2),
                restored.getUsedFilaments(0).toList())
            val restoredBounds = combinedWorldBounds(restored, 0)
            for (axis in 0..2) {
                assertEquals("reloaded world minimum on axis $axis", expectedMin[axis], restoredBounds.first[axis], 0.02)
                assertEquals("reloaded world maximum on axis $axis", expectedMax[axis], restoredBounds.second[axis], 0.02)
            }
            sliceResult = restored.slice(SliceBeam.getCurrentConfigFile().absolutePath, outputFile.absolutePath, null)
            val gcode = outputFile.readText()
            assertHasExtrusion(gcode)
            assertTrue("the original target should emit T0", Regex("(?m)^T0(?:\\s|$)").containsMatchIn(gcode))
            assertTrue("the joined source should emit T1", Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            sliceResult?.release()
            restored?.release()
            linkedSource.release()
            model.release()
            modelFile.delete()
            sourceFile.delete()
            projectFile.delete()
            outputFile.delete()
        }
    }

    @Test
    fun uniformReassignmentOfMultipart3mfSurvivesReloadAndSlicesWithNewSlot() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceFile = copyCube(context, "multipart-reassignment-source.stl")
        val originalProject = File(context.cacheDir, "multipart-reassignment-original.3mf")
        val remappedProject = File(context.cacheDir, "multipart-reassignment-remapped.3mf")
        val output = File(context.cacheDir, "multipart-reassignment-remapped.gcode")
        val model = Model(sourceFile)
        var originalReload: Model? = null
        var remappedReload: Model? = null
        var result: GCodeProcessorResult? = null
        try {
            model.addObject(model, 0)
            model.translate(1, 30.0, 0.0, 0.0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            assertEquals(0, model.combineObjectsAsParts(intArrayOf(0, 1)))
            assertEquals("fixture starts as one object with two material volumes", listOf(1, 2),
                model.getUsedFilaments(0).toList())

            SliceBeam.genCurrentConfig(model)
            model.export3mf(SliceBeam.getCurrentConfigFile().absolutePath, originalProject.absolutePath)
            originalReload = Model(originalProject, 1)
            assertEquals("the source 3MF must reload both material assignments", listOf(1, 2),
                originalReload.getUsedFilaments(0).toList())

            // This is the same explicit uniform reassignment used by the model-to-filament UI.
            originalReload.setUniformFilament(0, 2)
            assertEquals("choosing slot 2 must replace the imported per-volume slot 1", listOf(2),
                originalReload.getUsedFilaments(0).toList())
            SliceBeam.genCurrentConfig(originalReload)
            originalReload.export3mf(SliceBeam.getCurrentConfigFile().absolutePath, remappedProject.absolutePath)
            remappedReload = Model(remappedProject, 1)
            assertEquals("the chosen slot must survive the remapped 3MF round trip", listOf(2),
                remappedReload.getUsedFilaments(0).toList())

            SliceBeam.genCurrentConfig(remappedReload)
            result = remappedReload.slice(SliceBeam.getCurrentConfigFile().absolutePath, output.absolutePath, null)
            val gcode = output.readText()
            assertHasExtrusion(gcode)
            assertTrue("the chosen second slot must reach G-code as T1",
                Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            result?.release()
            remappedReload?.release()
            originalReload?.release()
            model.release()
            sourceFile.delete()
            originalProject.delete()
            remappedProject.delete()
            output.delete()
        }
    }

    @Test
    fun perSourceColorRemappingSurvives3mfReloadAndEmitsMappedTools() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceFile = copyCube(context, "per-color-remap-source.stl")
        val firstRoundTrip = File(context.cacheDir, "per-color-remap-first.3mf")
        val sharedRoundTrip = File(context.cacheDir, "per-color-remap-shared.3mf")
        val firstOutput = File(context.cacheDir, "per-color-remap-first.gcode")
        val sharedOutput = File(context.cacheDir, "per-color-remap-shared.gcode")
        val model = Model(sourceFile)
        var firstReload: Model? = null
        var sharedReload: Model? = null
        var firstResult: GCodeProcessorResult? = null
        var sharedResult: GCodeProcessorResult? = null
        try {
            model.addObject(model, 0)
            model.addObject(model, 0)
            model.translate(1, 30.0, 0.0, 0.0)
            model.translate(2, 60.0, 0.0, 0.0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            model.setUniformFilament(2, 3)
            assertEquals(0, model.combineObjectsAsParts(intArrayOf(0, 1, 2)))
            assertEquals(listOf(1, 2, 3), model.getUsedFilaments(0).toList())

            model.remapFilaments(intArrayOf(1, 2, 3), intArrayOf(2, 3, 3))
            assertEquals("each 3MF material must map independently, including two sharing a target",
                listOf(2, 3), model.getUsedFilaments(0).toList())
            SliceBeam.genCurrentConfig(model)
            model.export3mf(SliceBeam.getCurrentConfigFile().absolutePath, firstRoundTrip.absolutePath)
            firstReload = Model(firstRoundTrip, 1)
            assertEquals("source color assignments must survive 3MF reload", listOf(2, 3),
                firstReload.getUsedFilaments(0).toList())
            firstResult = firstReload.slice(SliceBeam.getCurrentConfigFile().absolutePath,
                firstOutput.absolutePath, null)
            val firstGcode = firstOutput.readText()
            assertHasExtrusion(firstGcode)
            assertTrue("mapped source colors must use output tools T1 and T2",
                Regex("(?m)^T1(?:\\s|$)").containsMatchIn(firstGcode) &&
                    Regex("(?m)^T2(?:\\s|$)").containsMatchIn(firstGcode))
            assertFalse("unmapped T0 must not be emitted", Regex("(?m)^T0(?:\\s|$)").containsMatchIn(firstGcode))

            firstReload.remapFilaments(intArrayOf(2, 3), intArrayOf(2, 2))
            assertEquals("different model colors may intentionally share one filament slot", listOf(2),
                firstReload.getUsedFilaments(0).toList())
            SliceBeam.genCurrentConfig(firstReload)
            firstReload.export3mf(SliceBeam.getCurrentConfigFile().absolutePath, sharedRoundTrip.absolutePath)
            sharedReload = Model(sharedRoundTrip, 1)
            assertEquals(listOf(2), sharedReload.getUsedFilaments(0).toList())
            sharedResult = sharedReload.slice(SliceBeam.getCurrentConfigFile().absolutePath,
                sharedOutput.absolutePath, null)
            val sharedGcode = sharedOutput.readText()
            assertHasExtrusion(sharedGcode)
            assertTrue("the shared assignment must slice with target tool T1",
                Regex("(?m)^T1(?:\\s|$)").containsMatchIn(sharedGcode))
            assertFalse("the shared assignment must not retain target tool T2",
                Regex("(?m)^T2(?:\\s|$)").containsMatchIn(sharedGcode))
        } finally {
            firstResult?.release()
            sharedResult?.release()
            firstReload?.release()
            sharedReload?.release()
            model.release()
            sourceFile.delete()
            firstRoundTrip.delete()
            sharedRoundTrip.delete()
            firstOutput.delete()
            sharedOutput.delete()
        }
    }

    @Test
    fun kromacutColorFixtureCanMapBothSourceColorsToOneSlotAndSliceAfterReload() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val fixture = File(context.cacheDir, "kromacut-color-fixture-input.3mf")
        val fixturePipe = instrumentation.uiAutomation.executeShellCommand("cat /data/local/tmp/kromacut.3mf")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(fixturePipe).use { input ->
            fixture.outputStream().use { output -> input.copyTo(output) }
        }
        val fixtureAvailable = fixture.length() > 1_000_000L
        if (!fixtureAvailable) fixture.delete()
        assumeTrue(
            "Push the user-provided KromaCut fixture to /data/local/tmp/kromacut.3mf to run this integration test",
            fixtureAvailable,
        )
        val remappedProject = File(context.cacheDir, "kromacut-shared-filament.3mf")
        val originalGcodeFile = File(context.cacheDir, "kromacut-original-colors.gcode")
        val gcodeFile = File(context.cacheDir, "kromacut-shared-filament.gcode")
        val model = Model(fixture, 1)
        var reloaded: Model? = null
        var originalSliceResult: GCodeProcessorResult? = null
        var sliceResult: GCodeProcessorResult? = null
        try {
            assertEquals("KromaCut's two colored layers should import as one assembled object", 1, model.objectsCount)
            assertEquals("the source project should retain both imported colors", listOf(1, 2),
                model.getUsedFilaments(0).toList())
            assertEquals("the first source color should use its first logical filament", 1,
                model.getVolumeExtruder(0, 0))
            assertEquals("the second source color should use its second logical filament", 2,
                model.getVolumeExtruder(0, 1))

            SliceBeam.genCurrentConfig(model)
            originalSliceResult = model.slice(
                SliceBeam.getCurrentConfigFile().absolutePath,
                originalGcodeFile.absolutePath,
                null,
            )
            val originalGcode = originalGcodeFile.readText()
            assertHasExtrusion(originalGcode)
            assertTrue("the original two-color KromaCut slice must switch to its second tool",
                Regex("(?m)^T1(?:\\s|$)").containsMatchIn(originalGcode))
            assertFalse("the original KromaCut project must not reference a third tool",
                Regex("(?m)^T2(?:\\s|$)").containsMatchIn(originalGcode))

            // A user may intentionally print both source colors with one physical slot.
            model.remapFilaments(intArrayOf(1, 2), intArrayOf(2, 2))
            assertEquals("both source colors should now share the chosen logical filament", listOf(2),
                model.getUsedFilaments(0).toList())
            assertEquals(2, model.getVolumeExtruder(0, 0))
            assertEquals(2, model.getVolumeExtruder(0, 1))

            SliceBeam.genCurrentConfig(model)
            model.export3mf(SliceBeam.getCurrentConfigFile().absolutePath, remappedProject.absolutePath)
            reloaded = Model(remappedProject, 1)
            assertEquals("the chosen filament must survive the 3MF round trip", listOf(2),
                reloaded.getUsedFilaments(0).toList())
            assertEquals(2, reloaded.getVolumeExtruder(0, 0))
            assertEquals(2, reloaded.getVolumeExtruder(0, 1))

            SliceBeam.genCurrentConfig(reloaded)
            sliceResult = reloaded.slice(SliceBeam.getCurrentConfigFile().absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertHasExtrusion(gcode)
            assertTrue("the selected slot 2 must be emitted as G-code tool T1",
                Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
            assertFalse("the original slot 1 must not reappear after slicing",
                Regex("(?m)^T0(?:\\s|$)").containsMatchIn(gcode))
            assertFalse("unassigned slot 3 must not be emitted",
                Regex("(?m)^T2(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            originalSliceResult?.release()
            sliceResult?.release()
            reloaded?.release()
            model.release()
            fixture.delete()
            remappedProject.delete()
            originalGcodeFile.delete()
            gcodeFile.delete()
            SliceBeam.getCurrentConfigFile().delete()
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
            val trianglesPerCopy = model.getTriangleCount(0)
            model.addObject(model, 0)
            model.addObject(model, 0)
            model.translate(1, 30.0, -10.0, 8.0)
            model.translate(2, 65.0, 12.0, 14.0)
            assertTrue("the moving group should include a linked copy", model.duplicateInstance(
                1, 0, Vec3d(24.0, 6.0, 0.0),
            ) >= 0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            model.setUniformFilament(2, 3)

            fun bounds(index: Int): ObjectBounds3D {
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(index, min, max)
                return ObjectBounds3D(min.x, min.y, min.z, max.x, max.y, max.z)
            }
            val target = bounds(0)
            val movingGroup = requireNotNull(ObjectPlacement.enclosing(listOf(bounds(1), bounds(2))))
            val surfacePoint = Vec3d(
                (target.minX + target.maxX) / 2.0,
                (target.minY + target.maxY) / 2.0,
                target.maxZ,
            )
            val delta = requireNotNull(ObjectPlacement.onSurface(movingGroup, surfacePoint))
            model.translate(1, delta.x, delta.y, delta.z)
            model.translate(2, delta.x, delta.y, delta.z)

            val placed = requireNotNull(ObjectPlacement.enclosing(listOf(bounds(1), bounds(2))))
            assertEquals(target.maxZ, placed.minZ, 0.001)
            // Orca rejects separate ModelObjects whose first printable layer is above
            // the bed. A stacked assembly must become multipart geometry before slicing.
            assertEquals(0, model.combineObjectsAsParts(intArrayOf(0, 1, 2)))
            assertEquals(1, model.objectsCount)
            assertEquals("the placed assembly should materialize all four printable copies",
                trianglesPerCopy * 4, model.getTriangleCount(0))
            assertEquals("surface placement assembly must retain target and placed-part tools",
                listOf(1, 2, 3), model.getUsedFilaments(0).toList())
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
                listOf(1, 2, 3), restored.getUsedFilaments(0).toList())
            val gcode = sliceToGcode(context, restored, "placed-group-slice.gcode")
            assertHasExtrusion(gcode)
            assertTrue("the placed assembly's second material must reach G-code",
                Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
            assertTrue("the placed assembly's third material must reach G-code",
                Regex("(?m)^T2(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            restored?.release()
            model.release()
            modelFile.delete()
            projectFile.delete()
        }
    }

    @Test
    fun sideSurfacePlacedObjectsRemainPrintableAfterThreeMfReload() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "placed-side-surface.stl")
        val projectFile = File(context.cacheDir, "placed-side-surface-roundtrip.3mf")
        val model = Model(modelFile)
        var restored: Model? = null
        try {
            model.addObject(model, 0)
            model.translate(1, 40.0, 0.0, 0.0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)

            fun bounds(index: Int): ObjectBounds3D {
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(index, min, max)
                return ObjectBounds3D(min.x, min.y, min.z, max.x, max.y, max.z)
            }
            val target = bounds(0)
            val moving = bounds(1)
            val point = Vec3d(
                target.maxX,
                (target.minY + target.maxY) / 2.0,
                (target.minZ + target.maxZ) / 2.0,
            )
            val delta = requireNotNull(ObjectPlacement.onSurface(moving, point, Vec3d(1.0, 0.0, 0.0)))
            model.translate(1, delta.x, delta.y, delta.z)
            val placed = bounds(1)
            assertEquals("the part should touch the selected side face", target.maxX, placed.minX, 0.001)
            assertEquals("the part should center on the tapped face point",
                (target.minY + target.maxY) / 2.0, (placed.minY + placed.maxY) / 2.0, 0.001)
            assertEquals("side placement should retain bed contact", target.minZ, placed.minZ, 0.001)

            assertEquals("side-by-side pieces should become one multipart printable object",
                0, model.combineObjectsAsParts(intArrayOf(0, 1)))
            assertEquals(listOf(1, 2), model.getUsedFilaments(0).toList())
            val exportConfig = SliceBeam.getCurrentConfigFile()
            try {
                model.export3mf(exportConfig.absolutePath, projectFile.absolutePath)
            } finally {
                exportConfig.delete()
            }
            restored = Model(projectFile, 1)
            assertEquals(1, restored.objectsCount)
            assertEquals("3MF reload must preserve both side-joined materials", listOf(1, 2),
                restored.getUsedFilaments(0).toList())
            val gcode = sliceToGcode(context, restored, "placed-side-surface.gcode")
            assertHasExtrusion(gcode)
            assertTrue("the side-placed second material must reach G-code",
                Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            restored?.release()
            model.release()
            modelFile.delete()
            projectFile.delete()
        }
    }

    @Test
    fun standaloneTextCanBePlacedJoinedAndSlicedAsItsOwnFilamentPart() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cubeFile = copyCube(context, "text-side-join-cube.stl")
        val fontFile = copyAsset(context, "font/roboto_medium.ttf", "text-side-join-font.ttf")
        val projectFile = File(context.cacheDir, "text-side-join-roundtrip.3mf")
        val model = Model(cubeFile)
        var restored: Model? = null
        var generatedConfig: File? = null
        try {
            val textIndex = model.createTextObject(
                fontFile.absolutePath, "A", 6f, 1.2f,
                Vec3d(100.0, 100.0, 0.0), Vec3d(0.0, 0.0, 1.0),
            )
            assertEquals("standalone text should be a separate editable object", 1, textIndex)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(textIndex, 2)

            fun bounds(index: Int): ObjectBounds3D {
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(index, min, max)
                return ObjectBounds3D(min.x, min.y, min.z, max.x, max.y, max.z)
            }
            val target = bounds(0)
            val text = bounds(textIndex)
            val point = Vec3d(
                target.maxX,
                (target.minY + target.maxY) / 2.0,
                (target.minZ + target.maxZ) / 2.0,
            )
            val delta = requireNotNull(ObjectPlacement.onSurface(text, point, Vec3d(1.0, 0.0, 0.0)))
            model.translate(textIndex, delta.x, delta.y, delta.z)
            val placedText = bounds(textIndex)
            assertEquals("text should meet the chosen side face", target.maxX, placedText.minX, 0.001)
            assertEquals("text should be centered at the picked face location", point.y,
                (placedText.minY + placedText.maxY) / 2.0, 0.001)

            assertEquals("text and model should become one multipart object", 0,
                model.combineObjectsAsParts(intArrayOf(0, textIndex)))
            assertEquals("text and base material assignments should both survive joining", listOf(1, 2),
                model.getUsedFilaments(0).toList())

            SliceBeam.genCurrentConfig(model)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            model.export3mf(generatedConfig.absolutePath, projectFile.absolutePath)
            generatedConfig.delete()
            generatedConfig = null
            restored = Model(projectFile, 1)
            assertEquals(1, restored.objectsCount)
            assertEquals(listOf(1, 2), restored.getUsedFilaments(0).toList())
            val gcode = sliceToGcode(context, restored, "text-side-join.gcode")
            assertHasExtrusion(gcode)
            assertTrue("base material should be emitted", Regex("(?m)^T0(?:\\s|$)").containsMatchIn(gcode))
            assertTrue("joined standalone text material should be emitted", Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            restored?.release()
            model.release()
            cubeFile.delete()
            fontFile.delete()
            projectFile.delete()
            generatedConfig?.delete()
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
    fun booleanOnMultipartBaseUsesItsFirstPartMaterialAndSlices() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "boolean-multipart-base.stl")
        val projectFile = File(context.cacheDir, "boolean-multipart-base-roundtrip.3mf")
        val model = Model(modelFile)
        var restored: Model? = null
        try {
            model.addObject(model, 0)
            model.addObject(model, 0)
            // The first base volume is deliberately not the lowest used filament.
            model.setUniformFilament(0, 2)
            model.setUniformFilament(1, 1)
            model.setUniformFilament(2, 3)
            model.translate(1, 50.0, 0.0, 0.0)
            model.translate(2, 55.0, 0.0, 0.0)

            assertEquals("the target should combine into a two-material multipart object", 0,
                model.combineObjectsAsParts(intArrayOf(0, 1)))
            assertEquals(listOf(1, 2), model.getUsedFilaments(0).toList())
            assertEquals("the cutter remains a separate third object", 2, model.objectsCount)

            assertEquals("Boolean union should accept the disconnected multipart base", 0,
                model.booleanObjects(0, 1, BooleanOperation.UNION))
            assertEquals("Boolean uses the first base volume's material, even when it is not the lowest slot",
                listOf(2), model.getUsedFilaments(0).toList())
            assertTrue("multipart-base Boolean output remains printable", model.getObjectVolume(0) > 0.0)

            SliceBeam.genCurrentConfig(model)
            val exportConfig = SliceBeam.getCurrentConfigFile()
            try {
                model.export3mf(exportConfig.absolutePath, projectFile.absolutePath)
            } finally {
                exportConfig.delete()
            }
            restored = Model(projectFile, 1)
            assertEquals("the CSG output survives 3MF reload", 1, restored.objectsCount)
            assertEquals("the documented first base-part material survives reload", listOf(2),
                restored.getUsedFilaments(0).toList())
            val gcode = sliceToGcode(context, restored, "boolean-multipart-base.gcode")
            assertHasExtrusion(gcode)
            assertTrue("the first base volume's filament should use T1",
                Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
            assertFalse("other base-part and cutter tools must not be emitted",
                Regex("(?m)^(?:T0|T2)(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            restored?.release()
            model.release()
            modelFile.delete()
            projectFile.delete()
        }
    }

    @Test
    fun booleanUnionConsumesEverySelectedToolAndSlicesTheCombinedResult() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "boolean-multiple-tools.stl")
        val projectFile = File(context.cacheDir, "boolean-multiple-tools.3mf")
        val model = Model(modelFile)
        var restored: Model? = null
        var generatedConfig: File? = null
        try {
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val width = max.x - min.x
            val sourceVolume = model.getObjectVolume(0)
            assertTrue("fixture must have a finite printable volume", sourceVolume > 0.0)

            model.addObject(model, 0)
            model.addObject(model, 0)
            model.translate(1, width / 2.0, 0.0, 0.0)
            model.translate(2, width, 0.0, 0.0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            model.setUniformFilament(2, 3)

            assertEquals("one target and both selected tools should be unioned", 0,
                model.booleanObjects(intArrayOf(0, 1, 2), BooleanOperation.UNION))
            assertEquals("every selected tool should be consumed", 1, model.objectsCount)
            assertEquals("Boolean result should keep only the target object's material", listOf(1),
                model.getUsedFilaments(0).toList())
            assertEquals("three overlapping cubes should form a two-cube-wide solid", 2.0,
                model.getObjectVolume(0) / sourceVolume, 0.04)

            SliceBeam.genCurrentConfig(model)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            model.export3mf(generatedConfig.absolutePath, projectFile.absolutePath)
            generatedConfig.delete()
            generatedConfig = null
            restored = Model(projectFile, 1)
            assertEquals(1, restored.objectsCount)
            assertEquals(listOf(1), restored.getUsedFilaments(0).toList())
            val gcode = sliceToGcode(context, restored, "boolean-multiple-tools.gcode")
            assertHasExtrusion(gcode)
            val toolChanges = Regex("(?m)^T\\d+.*$").findAll(gcode).map { it.value }.distinct().toList()
            // A one-tool slice can use T0 implicitly without a redundant T0 command.
            assertFalse("consumed tool materials must not leak into G-code; tools=$toolChanges",
                Regex("(?m)^(?:T1|T2)(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            restored?.release()
            model.release()
            modelFile.delete()
            projectFile.delete()
            generatedConfig?.delete()
        }
    }

    @Test
    fun booleanDifferenceAndIntersectionSurviveThreeMfReloadAndStillSlice() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = copyCube(context, "boolean-operations-roundtrip.stl")
        try {
            listOf(BooleanOperation.DIFFERENCE, BooleanOperation.INTERSECTION).forEach { operation ->
                val projectFile = File(context.cacheDir, "boolean-${operation.name.lowercase()}-roundtrip.3mf")
                val model = Model(modelFile)
                var restored: Model? = null
                try {
                    val min = Vec3d(); val max = Vec3d()
                    model.getBoundingBoxExact(0, min, max)
                    val sourceVolume = model.getObjectVolume(0)
                    model.addObject(model, 0)
                    model.translate(1, (max.x - min.x) / 2.0, 0.0, 0.0)
                    model.setUniformFilament(0, 1)
                    model.setUniformFilament(1, 2)

                    val result = model.booleanObjects(0, 1, operation)
                    assertEquals("$operation should replace both operands", 0, result)
                    assertEquals("$operation should leave one printable object", 1, model.objectsCount)
                    assertEquals("$operation should produce the expected half-volume overlap", 0.5,
                        model.getObjectVolume(result) / sourceVolume, 0.03)
                    assertEquals("the Boolean output should use the target object's material",
                        listOf(1), model.getUsedFilaments(result).toList())

                    SliceBeam.genCurrentConfig(model)
                    val exportConfig = SliceBeam.getCurrentConfigFile()
                    try {
                        model.export3mf(exportConfig.absolutePath, projectFile.absolutePath)
                    } finally {
                        exportConfig.delete()
                    }
                    val modelXml = ZipFile(projectFile).use { archive ->
                        archive.getInputStream(archive.getEntry("3D/3dmodel.model"))
                            .bufferedReader().use { it.readText() }
                    }
                    assertFalse(
                        "$operation's generated mesh must not retain stale source object metadata",
                        listOf("key=\"source_file\"", "key=\"source_object_id\"", "key=\"source_volume_id\"")
                            .any(modelXml::contains),
                    )
                    restored = Model(projectFile, 1)
                    assertEquals("$operation result should survive 3MF reload", 1, restored.objectsCount)
                    assertEquals("$operation reload must not restore the removed cutter's material",
                        listOf(1), restored.getUsedFilaments(0).toList())
                    assertEquals("$operation geometry volume should survive reload", 0.5,
                        restored.getObjectVolume(0) / sourceVolume, 0.03)

                    val gcode = sliceToGcode(context, restored, "boolean-${operation.name.lowercase()}-roundtrip.gcode")
                    assertHasExtrusion(gcode)
                    assertFalse("$operation G-code must not use the removed cutter's T1",
                        Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
                } finally {
                    restored?.release()
                    model.release()
                    projectFile.delete()
                }
            }
        } finally {
            modelFile.delete()
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
    fun suppliedMulticolorOmaSvgRoundTripsAndProducesExtrusionGcode() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val svgFile = File(context.cacheDir, "supplied-oma-heart-slice.svg")
        InstrumentationRegistry.getInstrumentation().context.assets
            .open("models/Beste_Oma_Kind_Herz_3D_Druck.svg").use { input ->
                svgFile.outputStream().use(input::copyTo)
            }
        val projectFile = File(context.cacheDir, "supplied-oma-heart-roundtrip.3mf")
        val gcodeFile = File(context.cacheDir, "supplied-oma-heart.gcode")
        var model: Model? = null
        var restored: Model? = null
        var generatedConfig: File? = null
        var sliceResult: GCodeProcessorResult? = null
        try {
            model = Model(svgFile)
            assertEquals("the source SVG should load as one workspace model", 1, model.objectsCount)
            assertTrue("the source SVG should contain printable mesh facets", model.getTriangleCount(0) > 0)
            assertTrue("the source SVG should create a closed printable body", model.getObjectVolume(0) > 0.0)
            assertEquals("the SVG's red, white, and black fill groups should map to three tools",
                listOf(1, 2, 3), model.getUsedFilaments(0).toList())

            SliceBeam.genCurrentConfig(model)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            model.export3mf(generatedConfig.absolutePath, projectFile.absolutePath)
            generatedConfig.delete()
            generatedConfig = null

            restored = Model(projectFile, 1)
            assertEquals("the exported project should retain the SVG model", 1, restored.objectsCount)
            assertTrue("the 3MF round trip should retain printable geometry", restored.getTriangleCount(0) > 0)
            assertEquals("the SVG's filament assignments should survive the project round trip",
                model.getUsedFilaments(0).toList(), restored.getUsedFilaments(0).toList())
            assertEquals("the 3MF round trip must preserve all three SVG color tools",
                listOf(1, 2, 3), restored.getUsedFilaments(0).toList())

            SliceBeam.genCurrentConfig(restored)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            sliceResult = restored.slice(generatedConfig.absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertHasExtrusion(gcode)
            for (tool in 0..2) {
                assertTrue("the multicolor SVG should emit tool T$tool", Regex("(?m)^T$tool(?:\\s|$)").containsMatchIn(gcode))
            }
            Log.i("NativeSliceDeviceTest", "PASS supplied multicolor SVG G-code bytes=${gcodeFile.length()}, " +
                "facets=${restored.getTriangleCount(0)}, filaments=${restored.getUsedFilaments(0).toList()}")
        } finally {
            sliceResult?.release()
            model?.release()
            restored?.release()
            svgFile.delete()
            projectFile.delete()
            gcodeFile.delete()
            generatedConfig?.delete()
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
    fun booleanUnionHandlesLinkedTargetAndCutterCopiesAndStillSlices() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val targetFile = copyCube(context, "boolean-linked-target.stl")
        val cutterFile = copyCube(context, "boolean-linked-cutter.stl")
        val outputFile = File(context.cacheDir, "boolean-linked-copies.gcode")
        val model = Model(targetFile)
        val cutter = Model(cutterFile)
        var sliceResult: GCodeProcessorResult? = null
        try {
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val width = max.x - min.x
            val oneCubeVolume = model.getObjectVolume(0)
            assertTrue("the target should have a second linked placement", model.duplicateInstance(
                0, 0, Vec3d(width * 2.0, 0.0, 0.0),
            ) >= 0)
            cutter.translate(0, width * 0.5, 0.0, 0.0)
            assertTrue("the cutter should have a second linked placement", cutter.duplicateInstance(
                0, 0, Vec3d(width * 2.0, 0.0, 0.0),
            ) >= 0)
            model.addObject(cutter, 0)

            val targetMin = Vec3d(); val targetMax = Vec3d()
            val cutterMin = Vec3d(); val cutterMax = Vec3d()
            model.getBoundingBoxExact(0, targetMin, targetMax)
            model.getBoundingBoxExact(1, cutterMin, cutterMax)
            val expectedMinX = minOf(targetMin.x, cutterMin.x)
            val expectedMaxX = maxOf(targetMax.x, cutterMax.x)

            val result = model.booleanObjects(0, 1, BooleanOperation.UNION)
            assertEquals("all selected linked copies should be combined", 0, result)
            assertEquals("Boolean output should be one printable object", 1, model.objectsCount)
            assertEquals("two half-overlapping pairs should preserve the combined volume",
                oneCubeVolume * 3.0, model.getObjectVolume(result), oneCubeVolume * 0.03)
            val resultMin = Vec3d(); val resultMax = Vec3d()
            model.getBoundingBoxExact(result, resultMin, resultMax)
            assertEquals("Boolean result should retain all linked-copy bounds", expectedMinX,
                resultMin.x, 0.03)
            assertEquals("Boolean result should retain all linked-copy bounds", expectedMaxX,
                resultMax.x, 0.03)

            SliceBeam.genCurrentConfig(model)
            val configFile = SliceBeam.getCurrentConfigFile()
            sliceResult = model.slice(configFile.absolutePath, outputFile.absolutePath, null)
            val gcode = outputFile.readText()
            assertTrue("the linked-copy Boolean result should slice", gcode.isNotBlank())
            assertHasExtrusion(gcode)
        } finally {
            sliceResult?.release()
            cutter.release()
            model.release()
            targetFile.delete()
            cutterFile.delete()
            outputFile.delete()
        }
    }

    @Test
    fun booleanDifferenceAndIntersectionApplyAcrossLinkedCopiesAndStillSlice() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val targetCube = copyCube(context, "boolean-linked-pair-target.stl")
        val cutterCube = copyCube(context, "boolean-linked-pair-cutter.stl")
        try {
            for (operation in listOf(BooleanOperation.DIFFERENCE, BooleanOperation.INTERSECTION)) {
                val suffix = if (operation == BooleanOperation.DIFFERENCE) "difference" else "intersection"
                val projectFile = File(context.cacheDir, "boolean-linked-$suffix.3mf")
                val gcodeFile = File(context.cacheDir, "boolean-linked-$suffix.gcode")
                val model = Model(targetCube)
                val cutter = Model(cutterCube)
                var restored: Model? = null
                var sliceResult: GCodeProcessorResult? = null
                try {
                    val min = Vec3d(); val max = Vec3d()
                    model.getBoundingBoxExact(0, min, max)
                    val width = max.x - min.x
                    val oneCubeVolume = model.getObjectVolume(0)
                    assertTrue("target should contain two linked copies", model.duplicateInstance(
                        0, 0, Vec3d(width * 2.0, 0.0, 0.0),
                    ) >= 0)
                    cutter.translate(0, width * 0.5, 0.0, 0.0)
                    assertTrue("cutter should contain two corresponding linked copies", cutter.duplicateInstance(
                        0, 0, Vec3d(width * 2.0, 0.0, 0.0),
                    ) >= 0)
                    model.setUniformFilament(0, 1)
                    cutter.setUniformFilament(0, 2)
                    model.addObject(cutter, 0)

                    val result = model.booleanObjects(0, 1, operation)
                    assertEquals("$suffix should apply to both linked pairs", 0, result)
                    assertEquals("$suffix should replace the two inputs with one result", 1, model.objectsCount)
                    assertEquals("$suffix should keep one half of each target copy",
                        oneCubeVolume, model.getObjectVolume(result), oneCubeVolume * 0.03)
                    val resultMin = Vec3d(); val resultMax = Vec3d()
                    model.getBoundingBoxExact(result, resultMin, resultMax)
                    val resultSpan = resultMax.x - resultMin.x
                    assertTrue("$suffix should preserve both distant result components; x span=$resultSpan",
                        resultSpan >= width * 2.0 && resultSpan <= width * 3.5)
                    assertEquals("Boolean output should keep the target material", listOf(1),
                        model.getUsedFilaments(result).toList())

                    SliceBeam.genCurrentConfig(model)
                    val exportConfig = SliceBeam.getCurrentConfigFile()
                    try {
                        model.export3mf(exportConfig.absolutePath, projectFile.absolutePath)
                    } finally {
                        exportConfig.delete()
                    }
                    restored = Model(projectFile, 1)
                    assertEquals("$suffix 3MF reload should contain one result", 1, restored.objectsCount)
                    assertEquals("$suffix 3MF reload should retain the target material", listOf(1),
                        restored.getUsedFilaments(0).toList())
                    SliceBeam.genCurrentConfig(restored)
                    val sliceConfig = SliceBeam.getCurrentConfigFile()
                    try {
                        sliceResult = restored.slice(sliceConfig.absolutePath, gcodeFile.absolutePath, null)
                    } finally {
                        sliceConfig.delete()
                    }
                    val gcode = gcodeFile.readText()
                    assertHasExtrusion(gcode)
                    assertFalse("$suffix result should not use cutter T1", Regex("(?m)^T1(?:\\s|$)")
                        .containsMatchIn(gcode))
                } finally {
                    sliceResult?.release()
                    restored?.release()
                    cutter.release()
                    model.release()
                    projectFile.delete()
                    gcodeFile.delete()
                }
            }
        } finally {
            targetCube.delete()
            cutterCube.delete()
        }
    }

    @Test
    fun standaloneRaisedAndEngravedTextSurviveThreeMfAndSliceWithTheirFilaments() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cubeFile = copyCube(context, "text-edit-workflow-cube.stl")
        val fontFile = copyAsset(context, "font/roboto_medium.ttf", "text-edit-workflow-font.ttf")
        val projectFile = File(context.cacheDir, "text-edit-workflow.3mf")
        val gcodeFile = File(context.cacheDir, "text-edit-workflow.gcode")
        var model: Model? = null
        var restored: Model? = null
        var result: GCodeProcessorResult? = null
        var generatedConfig: File? = null
        try {
            model = Model(cubeFile)
            val originalTriangles = model.getTriangleCount(0)
            val originalVolume = model.getObjectVolume(0)
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val centerX = (min.x + max.x) / 2.0
            val centerY = (min.y + max.y) / 2.0
            val originalTopZ = max.z

            // Exercise the two in-place text modes on the selected cube's top face.
            model.embossText(
                0, fontFile.absolutePath, "A", 4f, 0.8f, 0,
                Vec3d(centerX - 4.0, centerY, originalTopZ),
                Vec3d(0.0, 0.0, 1.0),
            )
            val raisedTriangles = model.getTriangleCount(0)
            val raisedVolume = model.getObjectVolume(0)
            assertTrue("raised text should add native geometry", raisedTriangles > originalTriangles)
            assertTrue("raised text should add printable solid volume", raisedVolume > originalVolume)

            model.embossText(
                0, fontFile.absolutePath, "B", 4f, 0.8f, 1,
                Vec3d(centerX + 4.0, centerY, originalTopZ),
                Vec3d(0.0, 0.0, 1.0),
            )
            assertEquals("in-place text edits must not create extra model objects", 1, model.objectsCount)

            // A separate label is a second printable object and keeps an independent tool assignment.
            val standaloneIndex = model.createTextObject(
                fontFile.absolutePath, "C", 6f, 1f,
                Vec3d(100.0, 100.0, 0.0), Vec3d(0.0, 0.0, 1.0),
            )
            assertEquals(1, standaloneIndex)
            model.setUniformFilament(standaloneIndex, 2)
            assertTrue("standalone text should contain printable triangles", model.getTriangleCount(standaloneIndex) > 0)

            SliceBeam.genCurrentConfig(model)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            model.export3mf(generatedConfig.absolutePath, projectFile.absolutePath)
            generatedConfig.delete()
            generatedConfig = null
            val modelXml = ZipFile(projectFile).use { archive ->
                val configEntry = archive.getEntry("Metadata/model_settings.config")
                    ?: archive.getEntry("Metadata/Slic3r_PE_model.config")
                    ?: error("3MF model config is missing; entries: ${archive.entries().asSequence().map { it.name }.toList()}")
                archive.getInputStream(configEntry)
                    .bufferedReader().use { it.readText() }
            }
            assertTrue("engraving must be serialized as a negative 3MF volume",
                modelXml.contains("negative_part"))

            restored = Model(projectFile, 1)
            assertEquals("raised/engraved base plus standalone text should remain two objects", 2, restored.objectsCount)
            assertTrue("3MF reload should preserve the modified base geometry",
                restored.getTriangleCount(0) > originalTriangles)
            assertTrue("3MF reload should preserve the standalone text mesh", restored.getTriangleCount(1) > 0)
            assertEquals("standalone text should retain its assigned tool", listOf(2),
                restored.getUsedFilaments(1).toList())

            SliceBeam.genCurrentConfig(restored)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            result = restored.slice(generatedConfig.absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertHasExtrusion(gcode)
            assertTrue("base model with raised and engraved text should use target T0",
                Regex("(?m)^T0(?:\\s|$)").containsMatchIn(gcode))
            assertTrue("separate text should use its assigned T1",
                Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
            assertFalse("no unrelated filament should be emitted",
                Regex("(?m)^T2(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            result?.release()
            restored?.release()
            model?.release()
            cubeFile.delete()
            fontFile.delete()
            projectFile.delete()
            gcodeFile.delete()
            generatedConfig?.delete()
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
            val secondTextIndex = model.createTextObject(
                fontFile.absolutePath,
                "3D",
                18f,
                1.2f,
                Vec3d(90.0, 125.0, 0.0),
                Vec3d(0.0, 0.0, 1.0),
            )
            assertEquals("a second label should remain a separate object", 1, secondTextIndex)
            assertTrue("the second standalone text should contain printable triangles",
                model.getTriangleCount(secondTextIndex) > 0)
            model.setUniformFilament(textIndex, 1)
            model.setUniformFilament(secondTextIndex, 2)

            SliceBeam.genCurrentConfig(model)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            model.export3mf(generatedConfig.absolutePath, projectFile.absolutePath)
            generatedConfig.delete()
            generatedConfig = null
            restored = Model(projectFile, 1)
            assertEquals("both standalone labels should remain separate after 3MF reload", 2, restored.objectsCount)
            assertEquals("first text label should survive project reload", "ORKAN", restored.getObjectName(0))
            assertEquals("second text label should survive project reload", "3D", restored.getObjectName(1))
            assertEquals("the first text should keep its own filament", listOf(1),
                restored.getUsedFilaments(0).toList())
            assertEquals("the second text should keep its own filament", listOf(2),
                restored.getUsedFilaments(1).toList())
            assertTrue("reloaded first text should remain printable", restored.getTriangleCount(0) > 0)
            assertTrue("reloaded second text should remain printable", restored.getTriangleCount(1) > 0)

            SliceBeam.genCurrentConfig(restored)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            sliceResult = restored.slice(generatedConfig.absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertTrue("native slicer should emit G-code for standalone text", gcode.isNotBlank())
            assertTrue("text G-code should contain deposited moves", Regex("(?m)^G1 .*\\bE[0-9.-]+").containsMatchIn(gcode))
            assertTrue("first text should use T0", Regex("(?m)^T0(?:\\s|$)").containsMatchIn(gcode))
            assertTrue("second text should use T1", Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
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
    fun booleanTextPlacementAndMultipartJoinKeepBothFilamentsThroughThreeMfAndSlice() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cubeFile = copyCube(context, "boolean-text-placement-cube.stl")
        val fontFile = copyAsset(context, "font/roboto_medium.ttf", "boolean-text-placement-font.ttf")
        val projectFile = File(context.cacheDir, "boolean-text-placement-project.3mf")
        val gcodeFile = File(context.cacheDir, "boolean-text-placement.gcode")
        var model: Model? = null
        var restored: Model? = null
        var result: GCodeProcessorResult? = null
        var generatedConfig: File? = null
        try {
            model = Model(cubeFile)
            model.addObject(model, 0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            val baseMin = Vec3d(); val baseMax = Vec3d()
            model.getBoundingBoxExact(0, baseMin, baseMax)
            model.translate(1, (baseMax.x - baseMin.x) / 2.0, 0.0, 0.0)

            val booleanIndex = model.booleanObjects(0, 1, BooleanOperation.UNION)
            assertEquals("Boolean union should replace the overlapping solids", 0, booleanIndex)
            assertEquals("Boolean result should retain target material", listOf(1), model.getUsedFilaments(0).toList())
            assertTrue("Boolean union should create a printable solid", model.getObjectVolume(0) > 0.0)

            val textIndex = model.createTextObject(
                fontFile.absolutePath, "A", 8f, 1.2f,
                Vec3d(80.0, 80.0, 0.0), Vec3d(0.0, 0.0, 1.0),
            )
            assertEquals("standalone text should be a distinct object", 1, textIndex)
            model.setUniformFilament(textIndex, 2)
            val textMin = Vec3d(); val textMax = Vec3d()
            model.getBoundingBoxExact(textIndex, textMin, textMax)
            val targetMin = Vec3d(); val targetMax = Vec3d()
            model.getBoundingBoxExact(0, targetMin, targetMax)
            val textBounds = ObjectBounds3D(textMin.x, textMin.y, textMin.z, textMax.x, textMax.y, textMax.z)
            val targetBounds = ObjectBounds3D(targetMin.x, targetMin.y, targetMin.z, targetMax.x, targetMax.y, targetMax.z)
            val delta = requireNotNull(ObjectPlacement.onTop(textBounds, targetBounds))
            model.translate(textIndex, delta.x, delta.y, delta.z)

            assertEquals("text should join the printable target as a separate material part", 0,
                model.combineObjectsAsParts(intArrayOf(0, textIndex)))
            assertEquals(1, model.objectsCount)
            assertTrue("joined Boolean body and text retain both assigned materials",
                model.getUsedFilaments(0).toSet().containsAll(setOf(1, 2)))

            SliceBeam.genCurrentConfig(model)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            model.export3mf(generatedConfig.absolutePath, projectFile.absolutePath)
            generatedConfig.delete()
            generatedConfig = null
            restored = Model(projectFile, 1)
            assertEquals("round-trip keeps the joined assembly as one printable object", 1, restored.objectsCount)
            assertTrue("round-trip retains body and text material assignments",
                restored.getUsedFilaments(0).toSet().containsAll(setOf(1, 2)))
            assertTrue("round-trip retains printable geometry", restored.getObjectVolume(0) > 0.0)

            SliceBeam.genCurrentConfig(restored)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            result = restored.slice(generatedConfig.absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertHasExtrusion(gcode)
            assertTrue("the Boolean body material should use extruder T0", Regex("(?m)^T0(?:\\s|$)").containsMatchIn(gcode))
            assertTrue("the placed text material should use extruder T1", Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
        } finally {
            result?.release()
            restored?.release()
            model?.release()
            cubeFile.delete()
            fontFile.delete()
            projectFile.delete()
            gcodeFile.delete()
            generatedConfig?.delete()
        }
    }

    @Test
    fun curvedSurfaceTextSurvivesThreeMfReloadAndProducesExtrusionGcode() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cylinderFile = createCylinderFixture("curved-text-slice-cylinder.stl", 25.0, 40.0, 96)
        val fontFile = copyAsset(context, "font/roboto_medium.ttf", "curved-text-slice-font.ttf")
        val projectFile = File(context.cacheDir, "curved-text-surface-project.3mf")
        val gcodeFile = File(context.cacheDir, "curved-text-surface.gcode")
        var model: Model? = null
        var restored: Model? = null
        var result: GCodeProcessorResult? = null
        var generatedConfig: File? = null
        try {
            model = Model(cylinderFile)
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val cylinderTriangles = model.getTriangleCount(0)
            val textIndex = model.createTextObject(
                fontFile.absolutePath,
                "ORCA",
                10f,
                1.2f,
                Vec3d(max.x, (min.y + max.y) / 2.0, (min.z + max.z) / 2.0),
                Vec3d(1.0, 0.0, 0.0),
                targetObjectIndex = 0,
            )
            assertEquals("surface-conforming text should join the target printable object", 0, textIndex)
            assertEquals("the text operation must not add a floating object", 1, model.objectsCount)
            assertTrue(model.getTriangleCount(0) - cylinderTriangles > cylinderTriangles)

            SliceBeam.genCurrentConfig(model)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            model.export3mf(generatedConfig.absolutePath, projectFile.absolutePath)
            generatedConfig.delete()
            generatedConfig = null
            restored = Model(projectFile, 1)
            assertEquals("surface text stays a part of its printable target", 1, restored.objectsCount)
            assertTrue("the conforming text volume must survive 3MF reload",
                restored.getTriangleCount(0) - cylinderTriangles > cylinderTriangles)

            SliceBeam.genCurrentConfig(restored)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            result = restored.slice(generatedConfig.absolutePath, gcodeFile.absolutePath, null)
            assertHasExtrusion(gcodeFile.readText())
            Log.i("NativeSliceDeviceTest", "PASS curved surface text G-code bytes=${gcodeFile.length()}")
        } finally {
            result?.release()
            restored?.release()
            model?.release()
            cylinderFile.delete()
            fontFile.delete()
            projectFile.delete()
            gcodeFile.delete()
            generatedConfig?.delete()
        }
    }

    @Test
    fun curvedTextInheritsTheTappedMultipartVolumeFilament() = withKobraProfile {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cylinderFile = createCylinderFixture("curved-text-filament-cylinder.stl", 25.0, 30.0, 96)
        val fontFile = copyAsset(context, "font/roboto_medium.ttf", "curved-text-filament-font.ttf")
        val projectFile = File(context.cacheDir, "curved-text-filament-project.3mf")
        var model: Model? = null
        var restored: Model? = null
        var generatedConfig: File? = null
        try {
            model = Model(cylinderFile)
            model.addObject(model, 0)
            model.translate(1, 60.0, 0.0, 0.0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            assertEquals(0, model.combineObjectsAsParts(intArrayOf(0, 1)))
            assertEquals(listOf(1, 2), model.getUsedFilaments(0).toList())

            val textIndex = model.createTextObject(
                fontFile.absolutePath,
                "ORCA",
                8f,
                1.0f,
                Vec3d(85.0, 0.0, 15.0),
                Vec3d(1.0, 0.0, 0.0),
                targetObjectIndex = 0,
            )
            assertEquals("curved text remains part of the tapped multipart object", 0, textIndex)

            SliceBeam.genCurrentConfig(model)
            generatedConfig = SliceBeam.getCurrentConfigFile()
            model.export3mf(generatedConfig.absolutePath, projectFile.absolutePath)
            generatedConfig.delete()
            generatedConfig = null

            val volumeFilamentTwoEntries = ZipFile(projectFile).use { archive ->
                val entry = archive.getEntry("Metadata/Slic3r_PE_model.config")
                val metadata = requireNotNull(entry).let {
                    archive.getInputStream(it).bufferedReader().use { reader -> reader.readText() }
                }
                Regex("type=\\\"volume\\\" key=\\\"extruder\\\" value=\\\"2\\\"")
                    .findAll(metadata).count()
            }
            assertEquals("the source part and its surface text must both use filament 2", 2, volumeFilamentTwoEntries)

            restored = Model(projectFile, 1)
            assertEquals(listOf(1, 2), restored.getUsedFilaments(0).toList())
            val gcodeFile = File(context.cacheDir, "curved-text-filament-project.gcode")
            try {
                val gcode = sliceToGcode(context, restored, gcodeFile.name)
                assertTrue("the tapped part's filament must reach emitted tool changes",
                    Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
                assertHasExtrusion(gcode)
            } finally {
                gcodeFile.delete()
            }
        } finally {
            model?.release()
            restored?.release()
            cylinderFile.delete()
            fontFile.delete()
            projectFile.delete()
            generatedConfig?.delete()
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

    private fun copyAsset(context: Context, assetPath: String, name: String): File =
        File(context.cacheDir, name).also { target ->
            context.assets.open(assetPath).use { input -> target.outputStream().use(input::copyTo) }
        }

    private fun createCylinderFixture(name: String, radius: Double, height: Double, segments: Int): File {
        val target = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, name)
        fun point(angle: Double, z: Double) = doubleArrayOf(radius * kotlin.math.cos(angle), radius * kotlin.math.sin(angle), z)
        fun vertexLine(point: DoubleArray) = "vertex ${point[0]} ${point[1]} ${point[2]}\n"
        val stl = StringBuilder("solid curved_text_cylinder\n")
        for (index in 0 until segments) {
            val a0 = 2.0 * Math.PI * index / segments
            val a1 = 2.0 * Math.PI * (index + 1) / segments
            val b0 = point(a0, 0.0); val b1 = point(a1, 0.0)
            val t0 = point(a0, height); val t1 = point(a1, height)
            val bottomCenter = doubleArrayOf(0.0, 0.0, 0.0)
            val topCenter = doubleArrayOf(0.0, 0.0, height)
            listOf(
                arrayOf(b0, b1, t1), arrayOf(b0, t1, t0),
                arrayOf(bottomCenter, b1, b0), arrayOf(topCenter, t0, t1),
            ).forEach { triangle ->
                stl.append("facet normal 0 0 0\nouter loop\n")
                triangle.forEach { stl.append(vertexLine(it)) }
                stl.append("endloop\nendfacet\n")
            }
        }
        stl.append("endsolid curved_text_cylinder\n")
        target.writeText(stl.toString())
        return target
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
