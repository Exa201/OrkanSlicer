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
            model.addObject(model, 0)
            model.addObject(model, 0)
            model.translate(1, 30.0, -10.0, 8.0)
            model.translate(2, 65.0, 12.0, 14.0)
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
