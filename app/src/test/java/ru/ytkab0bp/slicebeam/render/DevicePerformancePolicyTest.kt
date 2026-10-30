package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePerformancePolicyTest {
    @Test fun touchCoordinatesFollowTheActualGlSurfaceRatherThanDeviceRamPolicy() {
        assertEquals(500f, DevicePerformancePolicy.mapTouchCoordinate(500f, 1000, 1000))
        assertEquals(350f, DevicePerformancePolicy.mapTouchCoordinate(500f, 1000, 700))
        assertEquals(500f, DevicePerformancePolicy.mapTouchCoordinate(500f, 0, 700))
    }

    @Test fun weakDevicesUseFewerPixelsAndShorterInitialPreview() {
        assertEquals(0.7f, DevicePerformancePolicy.defaultRenderScale(true))
        assertEquals(25, DevicePerformancePolicy.initialLayerBlockSize(true))
        assertEquals(1f, DevicePerformancePolicy.defaultRenderScale(false))
        assertEquals(50, DevicePerformancePolicy.initialLayerBlockSize(false))
    }

    @Test fun inactivePlatesCanUseCheapPreviewWithoutChangingTheActiveModel() {
        assertTrue(DevicePerformancePolicy.useSimpleInactivePlatePreview(true, false))
        assertTrue(DevicePerformancePolicy.useSimpleInactivePlatePreview(false, true))
        assertFalse(DevicePerformancePolicy.useSimpleInactivePlatePreview(false, false))
    }

    @Test fun activePreviewHasABoundedMeshOnlyInPerformanceOrLowRamMode() {
        assertEquals(120_000, DevicePerformancePolicy.activePreviewTriangleBudget(false, true))
        assertEquals(120_000, DevicePerformancePolicy.activePreviewTriangleBudget(true, true))
        assertEquals(250_000, DevicePerformancePolicy.activePreviewTriangleBudget(true, false))
        assertNull(DevicePerformancePolicy.activePreviewTriangleBudget(false, false))
    }
}
