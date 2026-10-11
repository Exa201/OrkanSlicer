package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraPresetGeometryTest {
    private val min = CameraVector(10.0, 20.0, 5.0)
    private val max = CameraVector(210.0, 170.0, 255.0)
    private val origin = CameraVector(110.0, 95.0, 0.0)

    @Test
    fun isometricViewTargetsTheBuildVolumeCenter() {
        val pose = CameraPresetGeometry.resolve(CameraPreset.ISOMETRIC, min, max)
        assertVector(origin, pose.origin)
        assertVector(CameraVector(-150.0, -165.0, 5.0 + kotlin.math.sqrt(135200.0)), pose.position)
    }

    @Test
    fun topAndBottomViewsClearTheBuildVolume() {
        assertPose(CameraPreset.TOP, CameraVector(110.0, 94.0, 505.0))
        assertPose(CameraPreset.BOTTOM, CameraVector(110.0, 94.0, -245.0))
    }

    @Test
    fun frontAndBackViewsUseTheCorrectBedEdges() {
        assertPose(CameraPreset.FRONT, CameraVector(110.0, -130.0, 0.0))
        assertPose(CameraPreset.BACK, CameraVector(110.0, 320.0, 0.0))
    }

    @Test
    fun leftAndRightViewsUseTheCorrectBedEdges() {
        assertPose(CameraPreset.LEFT, CameraVector(-190.0, 95.0, 0.0))
        assertPose(CameraPreset.RIGHT, CameraVector(410.0, 95.0, 0.0))
    }

    private fun assertPose(preset: CameraPreset, expectedPosition: CameraVector) {
        val pose = CameraPresetGeometry.resolve(preset, min, max)
        assertVector(origin, pose.origin)
        assertVector(expectedPosition, pose.position)
    }

    private fun assertVector(expected: CameraVector, actual: CameraVector) {
        assertEquals(expected.x, actual.x, 1e-6)
        assertEquals(expected.y, actual.y, 1e-6)
        assertEquals(expected.z, actual.z, 1e-6)
    }
}
