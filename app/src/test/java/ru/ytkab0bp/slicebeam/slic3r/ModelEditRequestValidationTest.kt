package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.Vec3d

class ModelEditRequestValidationTest {
    @Test fun joiningAndBooleanRequestsRequireDistinctInRangeObjects() {
        assertTrue(ModelEditRequestValidation.hasDistinctObjectIndices(intArrayOf(0, 2), 3))
        assertTrue(ModelEditRequestValidation.hasDistinctObjectIndices(intArrayOf(0, 1, 2), 3))
        assertFalse(ModelEditRequestValidation.hasDistinctObjectIndices(intArrayOf(), 3))
        assertFalse(ModelEditRequestValidation.hasDistinctObjectIndices(intArrayOf(1), 3))
        assertFalse(ModelEditRequestValidation.hasDistinctObjectIndices(intArrayOf(0, 0), 3))
        assertFalse(ModelEditRequestValidation.hasDistinctObjectIndices(intArrayOf(-1, 1), 3))
        assertFalse(ModelEditRequestValidation.hasDistinctObjectIndices(intArrayOf(0, 3), 3))
    }

    @Test fun textRequestAcceptsStandaloneAndSurfaceAttachedPlacement() {
        assertTrue(ModelEditRequestValidation.isValidTextRequest(
            "font.ttf", "Label", 10f, 1.2f,
            Vec3d(1.0, 2.0, 3.0), Vec3d(0.0, 0.0, 1.0), -1, 2,
        ))
        assertTrue(ModelEditRequestValidation.isValidTextRequest(
            "font.ttf", "Label", 10f, 1.2f,
            Vec3d(1.0, 2.0, 3.0), Vec3d(0.0, 0.0, 2.0), 1, 2,
        ))
    }

    @Test fun textRequestRejectsInvalidDimensionsVectorsAndTarget() {
        fun valid(
            font: String = "font.ttf",
            text: String = "Label",
            size: Float = 10f,
            depth: Float = 1.2f,
            position: Vec3d = Vec3d(1.0, 2.0, 3.0),
            normal: Vec3d = Vec3d(0.0, 0.0, 1.0),
            target: Int = -1,
        ) = ModelEditRequestValidation.isValidTextRequest(
            font, text, size, depth, position, normal, target, objectCount = 2,
        )

        assertFalse(valid(font = "  "))
        assertFalse(valid(text = "  "))
        assertFalse(valid(size = Float.NaN))
        assertFalse(valid(size = Float.POSITIVE_INFINITY))
        assertFalse(valid(size = 0f))
        assertFalse(valid(depth = -1f))
        assertFalse(valid(position = Vec3d(Double.NaN, 0.0, 0.0)))
        assertFalse(valid(normal = Vec3d(0.0, 0.0, 0.0)))
        assertFalse(valid(normal = Vec3d(Double.POSITIVE_INFINITY, 0.0, 1.0)))
        assertFalse(valid(target = -2))
        assertFalse(valid(target = 2))
    }
}
