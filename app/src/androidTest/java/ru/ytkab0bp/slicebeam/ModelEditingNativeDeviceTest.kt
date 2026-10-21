package ru.ytkab0bp.slicebeam

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.slic3r.GLModel
import ru.ytkab0bp.slicebeam.slic3r.BooleanOperation
import ru.ytkab0bp.slicebeam.slic3r.Model
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

@RunWith(AndroidJUnit4::class)
class ModelEditingNativeDeviceTest {
    @get:Rule val compose = createComposeRule()

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
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var selected: BooleanOperation? = null
        compose.setContent {
            BeamComposeTheme {
                ModelBooleanDialog(onDismiss = {}, onChoose = { selected = it })
            }
        }

        compose.onNodeWithText(context.getString(R.string.ModelBooleanUnion)).performClick()
        compose.runOnIdle { assertEquals(BooleanOperation.UNION, selected) }
        compose.onNodeWithText(context.getString(R.string.ModelBooleanDifference)).performClick()
        compose.runOnIdle { assertEquals(BooleanOperation.DIFFERENCE, selected) }
        compose.onNodeWithText(context.getString(R.string.ModelBooleanIntersection)).performClick()
        compose.runOnIdle { assertEquals(BooleanOperation.INTERSECTION, selected) }
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
}
