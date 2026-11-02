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

    @Test fun automaticViewportPolicyUsesKnownWeakGlRendererWithoutGuessingUnknownHardware() {
        assertTrue(DevicePerformancePolicy.isKnownLowEndRenderer("Adreno (TM) 530"))
        assertTrue(DevicePerformancePolicy.isKnownLowEndRenderer("Mali-G57"))
        assertTrue(DevicePerformancePolicy.isKnownLowEndRenderer("Google SwiftShader"))
        assertFalse(DevicePerformancePolicy.isKnownLowEndRenderer("Adreno (TM) 740"))
        assertFalse(DevicePerformancePolicy.isKnownLowEndRenderer("Unknown GPU"))
        assertFalse(DevicePerformancePolicy.isKnownLowEndRenderer(null))
        assertEquals(0.65f, DevicePerformancePolicy.defaultRenderScale(false, "Mali-G57"))
        assertEquals(0.7f, DevicePerformancePolicy.defaultRenderScale(true, "Mali-G57"))
        assertEquals(1f, DevicePerformancePolicy.defaultRenderScale(false, "Adreno (TM) 740"))
        assertEquals(0.65f, DevicePerformancePolicy.renderScale(false, 0, "Google SwiftShader"))
        assertEquals(1f, DevicePerformancePolicy.renderScale(false, 100, "Google SwiftShader"))
        assertTrue(DevicePerformancePolicy.isLowPerformanceDevice(false, "Adreno 530"))
        assertFalse(DevicePerformancePolicy.isLowPerformanceDevice(false, "Adreno 740"))
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

    @Test fun viewportFrameRateDefaultsLowerOnWeakGpusAndHonorsSupportedCaps() {
        assertEquals(30, DevicePerformancePolicy.defaultViewportFpsCap(true))
        assertEquals(30, DevicePerformancePolicy.defaultViewportFpsCap(false, "Mali-G57"))
        assertEquals(60, DevicePerformancePolicy.defaultViewportFpsCap(false, "Adreno 740"))
        assertEquals(30, DevicePerformancePolicy.viewportFpsCap(true, 0))
        assertEquals(45, DevicePerformancePolicy.viewportFpsCap(true, 45))
        assertEquals(60, DevicePerformancePolicy.viewportFpsCap(false, 29))
    }

    @Test fun viewportFrameLimiterCoalescesRedrawsWithoutBlockingChanges() {
        assertEquals(0L, DevicePerformancePolicy.frameRequestDelayMs(100L, 100L, 30))
        val nextFrame = DevicePerformancePolicy.nextAllowedFrameAtMs(100L, 30)
        assertEquals(33L, nextFrame - 100L)
        assertEquals(23L, DevicePerformancePolicy.frameRequestDelayMs(110L, nextFrame, 30))
        assertEquals(0L, DevicePerformancePolicy.frameRequestDelayMs(140L, nextFrame, 30))
        assertEquals(0L, DevicePerformancePolicy.frameRequestDelayMs(100L, 200L, 0))
    }

    @Test fun inactivePlatesCanUseCheapPreviewWithoutChangingTheActiveModel() {
        assertTrue(DevicePerformancePolicy.useSimpleInactivePlatePreview(true, false))
        assertTrue(DevicePerformancePolicy.useSimpleInactivePlatePreview(false, true))
        assertTrue(DevicePerformancePolicy.useSimpleInactivePlatePreview(false, false, true))
        assertFalse(DevicePerformancePolicy.useSimpleInactivePlatePreview(false, false))
        assertEquals(35, DevicePerformancePolicy.initialLayerBlockSize(false, true))
        assertEquals(180_000, DevicePerformancePolicy.activePreviewTriangleBudget(false, false, true))
    }

    @Test fun activePreviewHasABoundedMeshOnlyInPerformanceOrLowRamMode() {
        assertEquals(120_000, DevicePerformancePolicy.activePreviewTriangleBudget(false, true))
        assertEquals(120_000, DevicePerformancePolicy.activePreviewTriangleBudget(true, true))
        assertEquals(250_000, DevicePerformancePolicy.activePreviewTriangleBudget(true, false))
        assertNull(DevicePerformancePolicy.activePreviewTriangleBudget(false, false))
    }
}
