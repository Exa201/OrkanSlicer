package ru.ytkab0bp.slicebeam

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.slic3r.GLModel
import ru.ytkab0bp.slicebeam.slic3r.BooleanOperation
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.ModelMirrorAxis
import ru.ytkab0bp.slicebeam.slic3r.Native
import ru.ytkab0bp.slicebeam.slic3r.ObjectBounds3D
import ru.ytkab0bp.slicebeam.slic3r.ObjectPlacement
import ru.ytkab0bp.slicebeam.components.FastenerFormLogic
import ru.ytkab0bp.slicebeam.components.bed_menu.ModelBooleanDialog
import ru.ytkab0bp.slicebeam.utils.FastenerStlWriter
import ru.ytkab0bp.slicebeam.utils.FastenerStandards
import ru.ytkab0bp.slicebeam.utils.Vec3d
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ModelEditingNativeDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun releasedModelRejectsTextJoinAndBooleanRequestsBeforeNativeCalls() {
        val model = Model()
        model.release()

        assertTrue("released model must reject multipart join", model.combineObjectsAsParts(intArrayOf(0, 1)) < 0)
        assertTrue("released model must reject Boolean operation", model.booleanObjects(0, 1, BooleanOperation.UNION) < 0)
        assertTrue("released model must reject standalone text creation", model.createTextObject(
            "font.ttf", "Label", 10f, 1f, Vec3d(0.0, 0.0, 0.0), Vec3d(0.0, 0.0, 1.0),
        ) < 0)
    }

    @Test fun mirrorXyZKeepsObjectCenterMeshAndCanBeToggledBack() {
        val modelFile = copyAsset("models/xyz_cube.stl", "mirror-axes-cube.stl")
        val model = Model(modelFile)
        try {
            val triangles = model.getTriangleCount(0)
            val volume = model.getObjectVolume(0)
            val beforeMin = Vec3d(); val beforeMax = Vec3d()
            model.getBoundingBoxExact(0, beforeMin, beforeMax)
            val centerBefore = doubleArrayOf(
                (beforeMin.x + beforeMax.x) / 2.0,
                (beforeMin.y + beforeMax.y) / 2.0,
                (beforeMin.z + beforeMax.z) / 2.0,
            )

            for ((axis, component) in listOf(
                ModelMirrorAxis.X to 0,
                ModelMirrorAxis.Y to 1,
                ModelMirrorAxis.Z to 2,
            )) {
                model.mirror(0, axis)
                val expected = doubleArrayOf(1.0, 1.0, 1.0).also { it[component] = -1.0 }
                assertLinearTransform(model, expected)
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(0, min, max)
                val centerAfter = listOf((min.x + max.x) / 2.0, (min.y + max.y) / 2.0, (min.z + max.z) / 2.0)
                centerBefore.indices.forEach { coordinate ->
                    assertEquals("$axis mirror should preserve the object's center", centerBefore[coordinate], centerAfter[coordinate], 0.001)
                }
                assertEquals("$axis mirror must retain the printable mesh", triangles, model.getTriangleCount(0))
                assertEquals("$axis mirror must retain the solid volume", volume, model.getObjectVolume(0), 0.001)

                model.mirror(0, axis)
                assertLinearTransform(model, doubleArrayOf(1.0, 1.0, 1.0))
            }
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    private fun assertLinearTransform(model: Model, expectedDiagonal: DoubleArray) {
        val rotation = Vec3d(); val scale = Vec3d(); val mirror = Vec3d()
        model.getRotation(0, rotation)
        model.getScale(0, scale)
        model.getMirror(0, mirror)
        val sx = scale.x * mirror.x; val sy = scale.y * mirror.y; val sz = scale.z * mirror.z
        val cx = kotlin.math.cos(rotation.x); val cy = kotlin.math.cos(rotation.y); val cz = kotlin.math.cos(rotation.z)
        val sinX = kotlin.math.sin(rotation.x); val sinY = kotlin.math.sin(rotation.y); val sinZ = kotlin.math.sin(rotation.z)
        val matrix = arrayOf(
            doubleArrayOf(cz * cy * sx, (cz * sinY * sinX - sinZ * cx) * sy, (cz * sinY * cx + sinZ * sinX) * sz),
            doubleArrayOf(sinZ * cy * sx, (sinZ * sinY * sinX + cz * cx) * sy, (sinZ * sinY * cx - cz * sinX) * sz),
            doubleArrayOf(-sinY * sx, cy * sinX * sy, cy * cx * sz),
        )
        for (row in 0..2) for (column in 0..2) {
            val expected = if (row == column) expectedDiagonal[row] else 0.0
            assertEquals("mirrored transform matrix[$row,$column]", expected, matrix[row][column], 1e-5)
        }
    }

    @Test fun combineSelectedMeshesKeepsPlacementAndPerVolumeFilamentAssignments() {
        val modelFile = copyAsset("models/xyz_cube.stl", "combine-cube.stl")
        val model = Model(modelFile)
        try {
            assertEquals(1, model.objectsCount)
            model.addObject(model, 0)
            assertEquals(2, model.objectsCount)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            model.translate(1, 30.0, 0.0, 0.0)
            val firstTriangles = model.getTriangleCount(0)
            val secondTriangles = model.getTriangleCount(1)
            val expectedFilaments = (model.getUsedFilaments(0) + model.getUsedFilaments(1)).toSet()

            assertEquals(0, model.combineObjectsAsParts(intArrayOf(0, 1)))
            assertEquals(1, model.objectsCount)
            assertEquals(firstTriangles + secondTriangles, model.getTriangleCount(0))
            assertTrue("combined part materials should remain represented", model.getUsedFilaments(0).toSet().containsAll(expectedFilaments))
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            assertTrue("both source placements should be retained", max.x - min.x > 30.0)
        } finally {
            model.release()
        }
    }

    @Test fun combiningRotatedNonUniformlyScaledObjectPreservesWorldBounds() {
        val modelFile = copyAsset("models/xyz_cube.stl", "combine-transformed-cube.stl")
        val model = Model(modelFile)
        try {
            model.addObject(model, 0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            model.scale(1, 1.4, 0.7, 1.2)
            model.rotate(1, 0.35, -0.22, 0.48)
            model.translate(1, 31.0, -17.0, 9.0)

            fun bounds(index: Int): Pair<DoubleArray, DoubleArray> {
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(index, min, max)
                return doubleArrayOf(min.x, min.y, min.z) to doubleArrayOf(max.x, max.y, max.z)
            }
            val sourceBounds = listOf(bounds(0), bounds(1))
            val expectedMin = DoubleArray(3) { axis -> sourceBounds.minOf { it.first[axis] } }
            val expectedMax = DoubleArray(3) { axis -> sourceBounds.maxOf { it.second[axis] } }
            val expectedTriangles = model.getTriangleCount(0) + model.getTriangleCount(1)

            assertEquals(0, model.combineObjectsAsParts(intArrayOf(0, 1)))
            assertEquals(1, model.objectsCount)
            assertEquals(expectedTriangles, model.getTriangleCount(0))
            assertTrue("both source filament assignments must remain on the joined part",
                model.getUsedFilaments(0).toSet().containsAll(setOf(1, 2)))
            val joined = bounds(0)
            for (axis in 0..2) {
                assertEquals("joined minimum on axis $axis", expectedMin[axis], joined.first[axis], 0.01)
                assertEquals("joined maximum on axis $axis", expectedMax[axis], joined.second[axis], 0.01)
            }
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun rejectedObjectCombinesLeaveGeometryPlacementAndMaterialsUnchanged() {
        val modelFile = copyAsset("models/xyz_cube.stl", "combine-invalid-input-cube.stl")
        val model = Model(modelFile)
        try {
            model.addObject(model, 0)
            model.setUniformFilament(0, 1)
            model.setUniformFilament(1, 2)
            model.translate(1, 37.0, -12.0, 8.0)

            data class Snapshot(
                val triangles: Int,
                val volume: Double,
                val filaments: List<Int>,
                val min: List<Double>,
                val max: List<Double>,
            )
            fun snapshot(index: Int): Snapshot {
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(index, min, max)
                return Snapshot(
                    model.getTriangleCount(index),
                    model.getObjectVolume(index),
                    model.getUsedFilaments(index).toList(),
                    listOf(min.x, min.y, min.z),
                    listOf(max.x, max.y, max.z),
                )
            }

            val before = listOf(snapshot(0), snapshot(1))
            val invalidRequests = listOf(
                intArrayOf(0),
                intArrayOf(0, 1, 1),
                intArrayOf(0, 2),
                intArrayOf(-1, 1),
            )
            invalidRequests.forEach { indices ->
                assertTrue("invalid combine request must be rejected", model.combineObjectsAsParts(indices) < 0)
                assertEquals("rejected request must preserve both source objects", 2, model.objectsCount)
                assertEquals("target geometry/material/placement must remain unchanged", before[0], snapshot(0))
                assertEquals("source geometry/material/placement must remain unchanged", before[1], snapshot(1))
            }
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun placingOnePrintableObjectOnAnotherAlignsCentersAndTouchesSurface() {
        val modelFile = copyAsset("models/xyz_cube.stl", "place-cube.stl")
        val model = Model(modelFile)
        try {
            model.addObject(model, 0)
            model.translate(1, 28.0, -11.0, 9.0)
            fun bounds(index: Int): ObjectBounds3D {
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(index, min, max)
                return ObjectBounds3D(min.x, min.y, min.z, max.x, max.y, max.z)
            }
            val before = bounds(1)
            val target = bounds(0)
            val delta = ObjectPlacement.onTop(before, target, clearanceMm = 0.0)
            assertTrue("placement should be calculable for valid printable meshes", delta != null)
            model.translate(1, delta!!.x, delta.y, delta.z)
            val placed = bounds(1)
            assertEquals((target.minX + target.maxX) / 2.0, (placed.minX + placed.maxX) / 2.0, 0.001)
            assertEquals((target.minY + target.maxY) / 2.0, (placed.minY + placed.maxY) / 2.0, 0.001)
            assertEquals(target.maxZ, placed.minZ, 0.001)
        } finally {
            model.release()
        }
    }

    @Test fun failedCutRollsBackOnlyConnectorsAddedForThatCut() {
        val modelFile = copyAsset("models/xyz_cube.stl", "cut-connector-rollback.stl")
        val model = Model(modelFile)
        try {
            val originalTriangles = model.getTriangleCount(0)
            val originalVolume = model.getObjectVolume(0)
            model.addConnector(0, Vec3d(0.0, 0.0, 5.0), 2f, 5f, 0)
            val connectorsBeforeCut = model.getCutConnectorCount(0)
            assertEquals("the fixture should contain one pre-existing connector", 1, connectorsBeforeCut)

            model.addConnector(0, Vec3d(1.0, 1.0, 5.0), 2f, 5f, 1)
            assertEquals(2, model.getCutConnectorCount(0))

            val applied = model.cutPreservingConnectorsOnFailure(
                objectIndex = 0,
                connectorCountBeforeCommit = connectorsBeforeCut,
                zHeight = 0.0,
                rotX = 0.0,
                rotY = 0.0,
                keepUpper = false,
                keepLower = false,
            )

            assertTrue("a cut with neither resulting side selected should fail", !applied)
            assertEquals("rollback must preserve connectors that existed before this cut", connectorsBeforeCut,
                model.getCutConnectorCount(0))
            assertEquals("failed cut must preserve source triangles", originalTriangles, model.getTriangleCount(0))
            assertEquals("failed cut must preserve source volume", originalVolume, model.getObjectVolume(0), 0.001)
            assertEquals(1, model.objectsCount)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun placingSelectedObjectGroupOnAnotherKeepsGroupSpacingAndTouchesSurface() {
        val modelFile = copyAsset("models/xyz_cube.stl", "place-group-cube.stl")
        val model = Model(modelFile)
        try {
            model.addObject(model, 0)
            model.addObject(model, 0)
            model.translate(1, 30.0, -10.0, 8.0)
            model.translate(2, 65.0, 12.0, 14.0)
            fun bounds(index: Int): ObjectBounds3D {
                val min = Vec3d(); val max = Vec3d()
                model.getBoundingBoxExact(index, min, max)
                return ObjectBounds3D(min.x, min.y, min.z, max.x, max.y, max.z)
            }
            val firstBefore = bounds(1)
            val secondBefore = bounds(2)
            val movingGroup = requireNotNull(ObjectPlacement.enclosing(listOf(firstBefore, secondBefore)))
            val target = bounds(0)
            val delta = requireNotNull(ObjectPlacement.onTop(movingGroup, target))

            model.translate(1, delta.x, delta.y, delta.z)
            model.translate(2, delta.x, delta.y, delta.z)

            val firstAfter = bounds(1)
            val secondAfter = bounds(2)
            val placedGroup = requireNotNull(ObjectPlacement.enclosing(listOf(firstAfter, secondAfter)))
            assertEquals((target.minX + target.maxX) / 2.0, (placedGroup.minX + placedGroup.maxX) / 2.0, 0.001)
            assertEquals((target.minY + target.maxY) / 2.0, (placedGroup.minY + placedGroup.maxY) / 2.0, 0.001)
            assertEquals(target.maxZ, placedGroup.minZ, 0.001)
            assertEquals(
                (secondBefore.minX + secondBefore.maxX) / 2.0 - (firstBefore.minX + firstBefore.maxX) / 2.0,
                (secondAfter.minX + secondAfter.maxX) / 2.0 - (firstAfter.minX + firstAfter.maxX) / 2.0,
                0.001,
            )
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun standaloneTextCreatesIndependentPrintableMeshAtPickedSurface() {
        val modelFile = copyAsset("models/xyz_cube.stl", "text-base-cube.stl")
        val fontFile = copyAsset("font/roboto_medium.ttf", "text-model-font.ttf")
        val model = Model(modelFile)
        try {
            val before = model.objectsCount
            val index = model.createTextObject(
                fontFile.absolutePath,
                "ORCA",
                10f,
                1.2f,
                Vec3d(20.0, 15.0, 12.0),
                Vec3d(0.0, 0.0, 1.0),
            )
            assertEquals(before, index)
            assertEquals(before + 1, model.objectsCount)
            assertTrue("text must be a real mesh, not just an embossed modifier", model.getTriangleCount(index) > 0)
            assertEquals("ORCA", model.getObjectName(index))
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(index, min, max)
            assertTrue(min.z >= 11.99 && min.z <= 12.01)
            assertTrue(max.z - min.z > 1.0 && max.z - min.z < 1.5)
        } finally {
            model.release()
        }
    }

    @Test fun standaloneTextCanConformToCurvedCylinderAndRemainsPrintable() {
        val modelFile = createCylinderFixture("text-curved-cylinder.stl", radius = 25f, height = 30f, segments = 96)
        val fontFile = copyAsset("font/roboto_medium.ttf", "text-curved-surface-font.ttf")
        val model = Model(modelFile)
        try {
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val position = Vec3d(max.x, (min.y + max.y) / 2.0, (min.z + max.z) / 2.0)
            val flatIndex = model.createTextObject(
                fontFile.absolutePath, "ORCA", 10f, 1.2f, position, Vec3d(1.0, 0.0, 0.0),
            )
            assertEquals(1, flatIndex)
            val flatTriangles = model.getTriangleCount(flatIndex)
            val cylinderTriangles = model.getTriangleCount(0)
            val curvedIndex = model.createTextObject(
                fontFile.absolutePath, "ORCA", 10f, 1.2f, position, Vec3d(1.0, 0.0, 0.0),
                targetObjectIndex = 0,
            )
            assertEquals("curved text should be added as a part of the target printable object", 0, curvedIndex)
            assertEquals("surface text must not introduce a floating plate object", 2, model.objectsCount)
            assertTrue("surface following must refine the mesh before bending it",
                model.getTriangleCount(curvedIndex) - cylinderTriangles > flatTriangles)
            assertTrue("the target and curved text must remain a closed printable solid", model.getObjectVolume(curvedIndex) > 0.0)
            val curvedMin = Vec3d(); val curvedMax = Vec3d()
            model.getBoundingBoxExact(curvedIndex, curvedMin, curvedMax)
            assertTrue("text should protrude from the curved cylinder surface", curvedMax.x > max.x + 1.0)
            assertTrue("conforming text remains attached to the surface rather than floating", curvedMin.z <= min.z + 0.01)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun svgImportCreatesStandalonePrintableGeometry() {
        val svgFile = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "model-editing-printable-shape.svg",
        )
        svgFile.writeText(
            """<svg xmlns="http://www.w3.org/2000/svg" width="24mm" height="18mm" viewBox="0 0 24 18">
                <path id="frame" fill="#202020" d="M1 1 H23 V17 H1 Z M7 6 H17 V12 H7 Z"/>
            </svg>""".trimIndent(),
        )
        val model = Model(svgFile)
        try {
            assertEquals("SVG import should create one printable object", 1, model.objectsCount)
            assertTrue("SVG import should create a non-empty triangle mesh", model.getTriangleCount(0) > 0)
            assertTrue("SVG import should create a closed solid with positive volume", model.getObjectVolume(0) > 0.0)
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            assertTrue("imported SVG should have a printable width", max.x - min.x > 1.0)
            assertTrue("imported SVG should have a printable height", max.y - min.y > 1.0)
            assertTrue("imported SVG should be extruded into a 3D body", max.z - min.z > 1.0)
        } finally {
            model.release()
            svgFile.delete()
        }
    }

    @Test fun nativeBooleanUnionCreatesOneClosedObjectWithCombinedVolume() = assertBooleanResult(
        BooleanOperation.UNION,
        expectedVolumeFactor = 1.5,
    )

    @Test fun nativeBooleanDifferenceSubtractsTheOverlappingHalf() = assertBooleanResult(
        BooleanOperation.DIFFERENCE,
        expectedVolumeFactor = 0.5,
    )

    @Test fun nativeBooleanIntersectionKeepsOnlyTheOverlappingHalf() = assertBooleanResult(
        BooleanOperation.INTERSECTION,
        expectedVolumeFactor = 0.5,
    )

    @Test fun nativeBooleanUnionKeepsDisjointClosedSolidsAsOneModelObject() {
        val modelFile = copyAsset("models/xyz_cube.stl", "boolean-disjoint-union.stl")
        val model = Model(modelFile)
        try {
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val sourceVolume = model.getObjectVolume(0)
            model.addObject(model, 0)
            model.translate(1, (max.x - min.x) * 1.5, 0.0, 0.0)

            val result = model.booleanObjects(0, 1, BooleanOperation.UNION)

            assertEquals(0, result)
            assertEquals(1, model.objectsCount)
            assertEquals(2.0, model.getObjectVolume(result) / sourceVolume, 0.02)
            val resultMin = Vec3d(); val resultMax = Vec3d()
            model.getBoundingBoxExact(result, resultMin, resultMax)
            assertEquals((max.y - min.y), resultMax.y - resultMin.y, 0.02)
            assertEquals((max.z - min.z), resultMax.z - resultMin.z, 0.02)
            assertEquals((max.x - min.x) * 2.5, resultMax.x - resultMin.x, 0.02)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun nativeBooleanUnionMergesSolidsTouchingAcrossOneCoplanarFace() {
        val modelFile = createCubeFixture("boolean-face-touching-union.stl", 20f)
        val model = Model(modelFile)
        try {
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val width = max.x - min.x
            val sourceVolume = model.getObjectVolume(0)
            model.addObject(model, 0)
            // The second cube shares the target's +X face exactly: no gap and no overlap.
            model.translate(1, width, 0.0, 0.0)

            val result = model.booleanObjects(0, 1, BooleanOperation.UNION)

            assertEquals("face-touching closed solids should union into one object", 0, result)
            assertEquals(1, model.objectsCount)
            assertEquals("coplanar shared faces must not lose or duplicate volume", 2.0,
                model.getObjectVolume(result) / sourceVolume, 0.002)
            val resultMin = Vec3d(); val resultMax = Vec3d()
            model.getBoundingBoxExact(result, resultMin, resultMax)
            assertEquals(width * 2.0, resultMax.x - resultMin.x, 0.002)
            assertEquals(max.y - min.y, resultMax.y - resultMin.y, 0.002)
            assertEquals(max.z - min.z, resultMax.z - resultMin.z, 0.002)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun nativeBooleanUnionWithVertexTouchIsValidOrLeavesBothInputsUntouched() {
        val modelFile = createCubeFixture("boolean-vertex-touching-union.stl", 20f)
        val model = Model(modelFile)
        try {
            val targetMin = Vec3d(); val targetMax = Vec3d()
            model.getBoundingBoxExact(0, targetMin, targetMax)
            val side = targetMax.x - targetMin.x
            val sourceVolume = model.getObjectVolume(0)
            val sourceTriangles = model.getTriangleCount(0)
            model.addObject(model, 0)
            // The cubes meet at exactly one vertex. Some mesh solvers reject this
            // non-manifold boundary; rejection must not consume or alter either input.
            model.translate(1, side, side, side)
            val result = model.booleanObjects(0, 1, BooleanOperation.UNION)

            if (result >= 0) {
                assertEquals("a successful vertex-touch union should yield one valid object", 1,
                    model.objectsCount)
                assertEquals("a successful union must preserve both volumes", 2.0,
                    model.getObjectVolume(result) / sourceVolume, 0.002)
                assertTrue("a successful union must retain triangles", model.getTriangleCount(result) > 0)
                val resultMin = Vec3d(); val resultMax = Vec3d()
                model.getBoundingBoxExact(result, resultMin, resultMax)
                assertEquals(side * 2.0, resultMax.x - resultMin.x, 0.002)
                assertEquals(side * 2.0, resultMax.y - resultMin.y, 0.002)
                assertEquals(side * 2.0, resultMax.z - resultMin.z, 0.002)
            } else {
                assertEquals("rejected non-manifold union must preserve both source objects", 2,
                    model.objectsCount)
                for (index in 0..1) {
                    assertEquals(sourceVolume, model.getObjectVolume(index), sourceVolume * 0.002)
                    assertEquals(sourceTriangles, model.getTriangleCount(index))
                    val min = Vec3d(); val max = Vec3d()
                    model.getBoundingBoxExact(index, min, max)
                    assertEquals(side, max.x - min.x, 0.002)
                    assertEquals(side, max.y - min.y, 0.002)
                    assertEquals(side, max.z - min.z, 0.002)
                }
            }
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun nativeBooleanDifferenceWithDisjointCutterNeverMutatesTargetGeometry() {
        val modelFile = createCubeFixture("boolean-disjoint-difference.stl", 20f)
        val model = Model(modelFile)
        try {
            val targetMin = Vec3d(); val targetMax = Vec3d()
            model.getBoundingBoxExact(0, targetMin, targetMax)
            val targetVolume = model.getObjectVolume(0)
            val targetTriangles = model.getTriangleCount(0)
            model.addObject(model, 0)
            // Keep a positive gap so the cutter cannot intersect even through tolerance.
            model.translate(1, (targetMax.x - targetMin.x) * 2.0, 0.0, 0.0)
            val cutterVolume = model.getObjectVolume(1)
            val cutterTriangles = model.getTriangleCount(1)

            val result = model.booleanObjects(0, 1, BooleanOperation.DIFFERENCE)

            if (result >= 0) {
                assertEquals("a valid empty subtraction should leave only the unchanged target", 1,
                    model.objectsCount)
                assertEquals(targetVolume, model.getObjectVolume(result), targetVolume * 0.002)
                assertEquals(targetTriangles, model.getTriangleCount(result))
            } else {
                assertEquals("a rejected subtraction must preserve both input objects", 2,
                    model.objectsCount)
                assertEquals(targetVolume, model.getObjectVolume(0), targetVolume * 0.002)
                assertEquals(targetTriangles, model.getTriangleCount(0))
                assertEquals(cutterVolume, model.getObjectVolume(1), cutterVolume * 0.002)
                assertEquals(cutterTriangles, model.getTriangleCount(1))
            }
            val preservedMin = Vec3d(); val preservedMax = Vec3d()
            model.getBoundingBoxExact(if (result >= 0) result else 0, preservedMin, preservedMax)
            assertEquals(targetMin.x, preservedMin.x, 0.002)
            assertEquals(targetMax.x, preservedMax.x, 0.002)
            assertEquals(targetMin.y, preservedMin.y, 0.002)
            assertEquals(targetMax.y, preservedMax.y, 0.002)
            assertEquals(targetMin.z, preservedMin.z, 0.002)
            assertEquals(targetMax.z, preservedMax.z, 0.002)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun nativeBooleanUnionSupportsThreeSelectedObjectsAndPreservesWorldBounds() {
        val modelFile = copyAsset("models/xyz_cube.stl", "boolean-three-object-union.stl")
        val model = Model(modelFile)
        try {
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val width = max.x - min.x
            val sourceVolume = model.getObjectVolume(0)
            model.addObject(model, 0)
            model.addObject(model, 0)
            model.translate(1, width * 0.5, 0.0, 0.0)
            model.translate(2, -width * 0.5, 0.0, 0.0)

            val result = model.booleanObjects(intArrayOf(0, 1, 2), BooleanOperation.UNION)

            assertEquals("the output should replace all three source solids", 0, result)
            assertEquals(1, model.objectsCount)
            assertEquals(2.0, model.getObjectVolume(result) / sourceVolume, 0.03)
            val resultMin = Vec3d(); val resultMax = Vec3d()
            model.getBoundingBoxExact(result, resultMin, resultMax)
            assertEquals(width * 2.0, resultMax.x - resultMin.x, 0.02)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun nativeBooleanDifferenceAppliesTwoSeparateSelectedCutters() {
        val modelFile = copyAsset("models/xyz_cube.stl", "boolean-two-cutter-difference.stl")
        val model = Model(modelFile)
        try {
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val width = max.x - min.x
            val centerX = (min.x + max.x) / 2.0
            val centerY = (min.y + max.y) / 2.0
            val centerZ = (min.z + max.z) / 2.0
            val sourceVolume = model.getObjectVolume(0)
            model.addObject(model, 0)
            model.addObject(model, 0)
            for (index in 1..2) {
                model.scale(index, 0.3, 0.3, 0.3)
                val cutterMin = Vec3d(); val cutterMax = Vec3d()
                model.getBoundingBoxExact(index, cutterMin, cutterMax)
                val cutterCenterX = (cutterMin.x + cutterMax.x) / 2.0
                val cutterCenterY = (cutterMin.y + cutterMax.y) / 2.0
                val cutterCenterZ = (cutterMin.z + cutterMax.z) / 2.0
                val offsetX = if (index == 1) -width * 0.2 else width * 0.2
                model.translate(index, centerX + offsetX - cutterCenterX, centerY - cutterCenterY, centerZ - cutterCenterZ)
            }

            val result = model.booleanObjects(intArrayOf(0, 1, 2), BooleanOperation.DIFFERENCE)

            assertEquals("two-cutter difference should replace the three source objects", 0, result)
            assertEquals(1, model.objectsCount)
            assertEquals("two disjoint 30% cutters remove 5.4% of the base volume", 0.946,
                model.getObjectVolume(result) / sourceVolume, 0.015)
            assertTrue(model.getTriangleCount(result) > 0)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun nativeBooleanIntersectionKeepsSharedVolumeAcrossThreeObjects() {
        val modelFile = copyAsset("models/xyz_cube.stl", "boolean-three-object-intersection.stl")
        val model = Model(modelFile)
        try {
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val width = max.x - min.x
            val sourceVolume = model.getObjectVolume(0)
            model.addObject(model, 0)
            model.addObject(model, 0)
            model.translate(1, width * 0.2, width * 0.2, width * 0.2)
            model.translate(2, -width * 0.2, -width * 0.2, -width * 0.2)

            val result = model.booleanObjects(intArrayOf(0, 1, 2), BooleanOperation.INTERSECTION)

            assertEquals("three-solid intersection should replace all source objects", 0, result)
            assertEquals(1, model.objectsCount)
            assertEquals(0.216, model.getObjectVolume(result) / sourceVolume, 0.02)
            val resultMin = Vec3d(); val resultMax = Vec3d()
            model.getBoundingBoxExact(result, resultMin, resultMax)
            assertEquals(width * 0.6, resultMax.x - resultMin.x, 0.02)
            assertEquals(width * 0.6, resultMax.y - resultMin.y, 0.02)
            assertEquals(width * 0.6, resultMax.z - resultMin.z, 0.02)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun nativeBooleanIntersectionHandlesThreeSolidsWithCoplanarFaces() {
        val modelFile = createCubeFixture("boolean-coplanar-three-object-intersection.stl", 20f)
        val model = Model(modelFile)
        try {
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val width = max.x - min.x
            model.addObject(model, 0)
            model.addObject(model, 0)
            model.translate(1, width * 0.2, 0.0, 0.0)
            model.translate(2, -width * 0.2, 0.0, 0.0)

            val result = model.booleanObjects(intArrayOf(0, 1, 2), BooleanOperation.INTERSECTION)

            assertEquals("three-body intersection with shared coplanar faces should succeed", 0, result)
            assertEquals(1, model.objectsCount)
            val resultMin = Vec3d(); val resultMax = Vec3d()
            model.getBoundingBoxExact(result, resultMin, resultMax)
            assertEquals(width * 0.6, resultMax.x - resultMin.x, 0.02)
            assertEquals(width, resultMax.y - resultMin.y, 0.02)
            assertEquals(width, resultMax.z - resultMin.z, 0.02)
            assertEquals("coplanar three-solid overlap should have exact 0.6 cube volume", 0.6,
                model.getObjectVolume(result) / (width * width * width), 0.002)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun failedMultiObjectBooleanLeavesEveryInputUntouched() {
        val modelFile = copyAsset("models/xyz_cube.stl", "boolean-three-object-failure.stl")
        val model = Model(modelFile)
        try {
            model.addObject(model, 0)
            model.addObject(model, 0)
            model.translate(1, 10.0, 0.0, 0.0)
            model.translate(2, 40.0, 0.0, 0.0)
            val volumes = (0 until model.objectsCount).map(model::getObjectVolume)

            assertTrue(model.booleanObjects(intArrayOf(0, 1, 2), BooleanOperation.INTERSECTION) < 0)

            assertEquals(3, model.objectsCount)
            volumes.forEachIndexed { index, volume -> assertEquals(volume, model.getObjectVolume(index), 0.01) }
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun nativeBooleanDifferenceKeepsAnEnclosedCavityAndExpectedSolidVolume() {
        val modelFile = copyAsset("models/xyz_cube.stl", "boolean-contained-difference.stl")
        val model = Model(modelFile)
        try {
            val targetMin = Vec3d(); val targetMax = Vec3d()
            model.getBoundingBoxExact(0, targetMin, targetMax)
            val sourceVolume = model.getObjectVolume(0)
            model.addObject(model, 0)
            model.scale(1, 0.5, 0.5, 0.5)
            val toolMin = Vec3d(); val toolMax = Vec3d()
            model.getBoundingBoxExact(1, toolMin, toolMax)
            model.translate(
                1,
                (targetMin.x + targetMax.x - toolMin.x - toolMax.x) / 2.0,
                (targetMin.y + targetMax.y - toolMin.y - toolMax.y) / 2.0,
                (targetMin.z + targetMax.z - toolMin.z - toolMax.z) / 2.0,
            )

            val result = model.booleanObjects(0, 1, BooleanOperation.DIFFERENCE)

            assertEquals(0, result)
            assertEquals(1, model.objectsCount)
            assertEquals(0.875, model.getObjectVolume(result) / sourceVolume, 0.02)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun booleanDialogOffersUnionDifferenceAndIntersectionAsSeparateActions() {
        val selected = AtomicReference<BooleanOperation?>(null)
        compose.setContent {
            BeamComposeTheme {
                ModelBooleanDialog(
                    targetName = "Base object",
                    toolNames = listOf("Tool A", "Tool B"),
                    onDismiss = {},
                    onChoose = selected::set,
                )
            }
        }

        compose.onNodeWithTag("model-boolean-target").assertIsDisplayed()
        compose.onNodeWithTag("model-boolean-tools").assertIsDisplayed()
        compose.onNodeWithText("Base object", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Tool A", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Tool B", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("model-boolean-union").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(BooleanOperation.UNION, selected.get()) }
        compose.onNodeWithTag("model-boolean-difference").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(BooleanOperation.DIFFERENCE, selected.get()) }
        compose.onNodeWithTag("model-boolean-intersection").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(BooleanOperation.INTERSECTION, selected.get()) }
    }

    private fun assertBooleanResult(operation: BooleanOperation, expectedVolumeFactor: Double) {
        val modelFile = copyAsset("models/xyz_cube.stl", "boolean-${operation.name.lowercase()}.stl")
        val model = Model(modelFile)
        try {
            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val sourceVolume = model.getObjectVolume(0)
            assertTrue("fixture cube must have finite positive volume", sourceVolume > 0.0)
            model.addObject(model, 0)
            model.translate(1, (max.x - min.x) / 2.0, 0.0, 0.0)

            val resultIndex = model.booleanObjects(0, 1, operation)

            assertEquals("boolean replaces both source objects only after success", 0, resultIndex)
            assertEquals(1, model.objectsCount)
            val resultVolume = model.getObjectVolume(resultIndex)
            assertEquals(expectedVolumeFactor, resultVolume / sourceVolume, 0.02)
            assertTrue("boolean output must be a non-empty printable mesh", model.getTriangleCount(resultIndex) > 0)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun invalidBooleanRequestLeavesBothObjectsUnchanged() {
        val modelFile = copyAsset("models/xyz_cube.stl", "boolean-invalid.stl")
        val model = Model(modelFile)
        try {
            model.addObject(model, 0)
            model.translate(1, 100.0, 0.0, 0.0)
            val firstVolume = model.getObjectVolume(0)
            val secondVolume = model.getObjectVolume(1)
            assertTrue(model.booleanObjects(0, 1, BooleanOperation.DIFFERENCE) < 0)
            assertTrue(model.booleanObjects(0, 1, BooleanOperation.INTERSECTION) < 0)
            assertTrue(model.booleanObjects(intArrayOf(0, 1, 1), BooleanOperation.UNION) < 0)
            assertEquals(2, model.objectsCount)
            assertEquals(firstVolume, model.getObjectVolume(0), 0.01)
            assertEquals(secondVolume, model.getObjectVolume(1), 0.01)
        } finally {
            model.release()
            modelFile.delete()
        }
    }

    @Test fun standardM12BoltCanBuildNativeRaycastDataAndBePicked() {
        val values = FastenerFormLogic.defaults(size = FastenerStandards.count() - 1)
        assertEquals(null, FastenerFormLogic.validate(values))
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "m12-raycast-regression.stl")
        val written = FastenerStlWriter.writeBolt(
            file,
            FastenerStandards.diameter(values.size),
            FastenerFormLogic.parse(values.second),
            FastenerFormLogic.parse(values.first),
            FastenerFormLogic.parse(values.fourth),
            FastenerFormLogic.parse(values.third),
        )
        val model = Model(file)
        val glModel = GLModel()
        try {
            assertTrue("generated STL should retain its screw triangles", model.getTriangleCount(0) > 0)
            assertEquals(written, model.getTriangleCount(0))
            glModel.initFrom(model, 0)
            val started = android.os.SystemClock.elapsedRealtime()
            Native.glmodel_init_raycast_data(glModel.pointer)
            val elapsed = android.os.SystemClock.elapsedRealtime() - started

            val min = Vec3d(); val max = Vec3d()
            model.getBoundingBoxExact(0, min, max)
            val hits = Native.glmodel_raycast_closest_hit(
                glModel.pointer,
                doubleArrayOf((min.x + max.x) / 2.0, (min.y + max.y) / 2.0, max.z + 10.0),
                doubleArrayOf(0.0, 0.0, -1.0),
            )
            android.util.Log.i("FastenerRaycastRegression", "M12 facets=${written}, raycastIndexMs=${elapsed}")
            assertTrue("ray through the bolt axis should hit the generated model", hits.size >= 6)
        } finally {
            glModel.release()
            model.release()
            file.delete()
        }
    }

    @Test fun nativeRaycastCacheSurvivesConcurrentMeshReinitialization() {
        val modelFile = copyAsset("models/xyz_cube.stl", "raycast-concurrency-cube.stl")
        val model = Model(modelFile)
        val glModel = GLModel()
        val start = CountDownLatch(1)
        val done = CountDownLatch(2)
        val stop = AtomicBoolean(false)
        val failure = AtomicReference<Throwable?>()
        var reader: Thread? = null
        var writer: Thread? = null
        try {
            glModel.initFrom(model, 0)
            reader = Thread {
                try {
                    start.await()
                    repeat(60) {
                        if (stop.get()) return@Thread
                        Native.glmodel_init_raycast_data(glModel.pointer)
                        val hits = Native.glmodel_raycast_closest_hit(
                            glModel.pointer,
                            doubleArrayOf(10.0, 10.0, 30.0),
                            doubleArrayOf(0.0, 0.0, -1.0),
                        )
                        assertEquals("raycast result must contain complete position/normal tuples", 0, hits.size % 6)
                    }
                } catch (error: Throwable) {
                    failure.compareAndSet(null, error)
                } finally {
                    done.countDown()
                }
            }
            writer = Thread {
                try {
                    start.await()
                    repeat(30) { index ->
                        if (stop.get()) return@Thread
                        glModel.initFrom(model, 0)
                        if (index % 5 == 0) glModel.reset()
                    }
                } catch (error: Throwable) {
                    failure.compareAndSet(null, error)
                } finally {
                    done.countDown()
                }
            }
            reader.start()
            writer.start()
            start.countDown()
            assertTrue("concurrent raycast/cache rebuild should finish", done.await(30, TimeUnit.SECONDS))
            failure.get()?.let { throw AssertionError("concurrent raycast/cache rebuild failed", it) }
        } finally {
            stop.set(true)
            start.countDown()
            reader?.join(5_000)
            writer?.join(5_000)
            val workersStopped = reader?.isAlive != true && writer?.isAlive != true
            if (workersStopped) {
                glModel.release()
                model.release()
                modelFile.delete()
            } else {
                throw AssertionError("raycast stress workers did not stop; native models kept alive to avoid use-after-free")
            }
        }
    }

    private fun copyAsset(asset: String, name: String): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File(context.cacheDir, name).also { file ->
            context.assets.open(asset).use { input -> file.outputStream().use(input::copyTo) }
        }
    }

    private fun createCubeFixture(name: String, side: Float): File {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, name)
        val vertices = arrayOf(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(side, 0f, 0f),
            floatArrayOf(side, side, 0f), floatArrayOf(0f, side, 0f),
            floatArrayOf(0f, 0f, side), floatArrayOf(side, 0f, side),
            floatArrayOf(side, side, side), floatArrayOf(0f, side, side),
        )
        val triangles = arrayOf(
            intArrayOf(0, 2, 1), intArrayOf(0, 3, 2),
            intArrayOf(4, 5, 6), intArrayOf(4, 6, 7),
            intArrayOf(0, 1, 5), intArrayOf(0, 5, 4),
            intArrayOf(3, 7, 6), intArrayOf(3, 6, 2),
            intArrayOf(0, 4, 7), intArrayOf(0, 7, 3),
            intArrayOf(1, 2, 6), intArrayOf(1, 6, 5),
        )
        val stl = ByteBuffer.allocate(84 + triangles.size * 50).order(ByteOrder.LITTLE_ENDIAN)
        stl.position(80)
        stl.putInt(triangles.size)
        triangles.forEach { triangle ->
            repeat(3) { stl.putFloat(0f) }
            triangle.forEach { vertex -> vertices[vertex].forEach { stl.putFloat(it) } }
            stl.putShort(0)
        }
        file.writeBytes(stl.array())
        return file
    }

    private fun createCylinderFixture(name: String, radius: Float, height: Float, segments: Int): File {
        val triangles = ArrayList<Array<FloatArray>>(segments * 4)
        fun point(angle: Double, z: Float) = floatArrayOf(
            radius * kotlin.math.cos(angle).toFloat(),
            radius * kotlin.math.sin(angle).toFloat(),
            z,
        )
        val bottomCenter = floatArrayOf(0f, 0f, 0f)
        val topCenter = floatArrayOf(0f, 0f, height)
        for (index in 0 until segments) {
            val a0 = 2.0 * Math.PI * index / segments
            val a1 = 2.0 * Math.PI * (index + 1) / segments
            val bottom0 = point(a0, 0f); val bottom1 = point(a1, 0f)
            val top0 = point(a0, height); val top1 = point(a1, height)
            triangles += arrayOf(bottom0, bottom1, top1)
            triangles += arrayOf(bottom0, top1, top0)
            triangles += arrayOf(bottomCenter, bottom1, bottom0)
            triangles += arrayOf(topCenter, top0, top1)
        }
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, name)
        val stl = ByteBuffer.allocate(84 + triangles.size * 50).order(ByteOrder.LITTLE_ENDIAN)
        stl.position(80)
        stl.putInt(triangles.size)
        triangles.forEach { triangle ->
            repeat(3) { stl.putFloat(0f) }
            triangle.forEach { vertex -> vertex.forEach(stl::putFloat) }
            stl.putShort(0)
        }
        file.writeBytes(stl.array())
        return file
    }
}
