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

    @Test fun viewportQualityOverrideIsClampedToSupportedChoicesAndAutomaticFollowsDevice() {
        assertEquals(0.7f, DevicePerformancePolicy.renderScale(true, 0))
        assertEquals(1f, DevicePerformancePolicy.renderScale(false, 0))
        assertEquals(0.5f, DevicePerformancePolicy.renderScale(false, 50))
        assertEquals(0.65f, DevicePerformancePolicy.renderScale(true, 65))
        assertEquals(0.8f, DevicePerformancePolicy.renderScale(false, 80))
        assertEquals(1f, DevicePerformancePolicy.renderScale(true, 100))
        assertEquals(0.7f, DevicePerformancePolicy.renderScale(true, 55))
        assertEquals(1f, DevicePerformancePolicy.renderScale(false, 55))
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
