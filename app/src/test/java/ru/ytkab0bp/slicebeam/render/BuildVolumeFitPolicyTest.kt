package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildVolumeFitPolicyTest {
    @Test
    fun shrinksOnlyWhenAnObjectExceedsTheBuildVolume() {
        assertEquals(
            0.4999,
            requireNotNull(BuildVolumeFitPolicy.uniformScale(200.0, 100.0, 50.0, 100.0, 100.0, 100.0)),
            1e-9,
        )
        assertEquals(
            1.0,
            requireNotNull(BuildVolumeFitPolicy.uniformScale(20.0, 30.0, 40.0, 100.0, 100.0, 100.0)),
            0.0,
        )
    }

    @Test
    fun limitingAxisDeterminesUniformScaleAndKeepsClearance() {
        assertEquals(
            0.49975,
            requireNotNull(BuildVolumeFitPolicy.uniformScale(40.0, 80.0, 20.0, 100.0, 40.0, 100.0)),
            1e-9,
        )
    }

    @Test
    fun circularBedFitsTheObjectFootprintCircumcircleAndHeight() {
        assertEquals(
            (185.0 - 0.02) / kotlin.math.hypot(300.0, 300.0).let { it / 2.0 },
            requireNotNull(BuildVolumeFitPolicy.uniformCircleScale(300.0, 300.0, 100.0, 185.0, 250.0)),
            1e-9,
        )
        assertEquals(
            1.0,
            requireNotNull(BuildVolumeFitPolicy.uniformCircleScale(40.0, 30.0, 50.0, 185.0, 250.0)),
            0.0,
        )
        assertEquals(
            (250.0 - 0.02) / 300.0,
            requireNotNull(BuildVolumeFitPolicy.uniformCircleScale(120.0, 120.0, 300.0, 100.0, 250.0)),
            1e-9,
        )
    }

    @Test
    fun circularFitRejectsInvalidDimensionsAndRadius() {
        assertNull(BuildVolumeFitPolicy.uniformCircleScale(0.0, 10.0, 10.0, 100.0, 100.0))
        assertNull(BuildVolumeFitPolicy.uniformCircleScale(10.0, 10.0, 10.0, Double.NaN, 100.0))
        assertNull(BuildVolumeFitPolicy.uniformCircleScale(10.0, 10.0, 10.0, 0.01, 100.0))
    }

    @Test
    fun circularFitPlanProvidesRendererScaleAndOffsetBedTarget() {
        val plan = requireNotNull(
            BuildVolumeFitPolicy.circularFitPlan(
                objectMinX = -125.0,
                objectMinY = -125.0,
                objectMinZ = 0.0,
                objectMaxX = 125.0,
                objectMaxY = 125.0,
                objectMaxZ = 250.0,
                bedCenterX = 17.0,
                bedCenterY = -11.0,
                bedRadius = 100.0,
                bedMinZ = 0.0,
                bedMaxZ = 180.0,
            ),
        )
        assertEquals((100.0 - 0.02) / kotlin.math.hypot(250.0, 250.0).let { it / 2.0 },
            plan.uniformScale, 1e-9)
        assertEquals("renderer target uses the native circle center X", 17.0, plan.targetCenterX, 0.0)
        assertEquals("renderer target uses the native circle center Y", -11.0, plan.targetCenterY, 0.0)
        assertEquals("renderer target keeps the object on the bed", 0.0, plan.targetMinZ, 0.0)
        assertNull(
            BuildVolumeFitPolicy.circularFitPlan(
                0.0, 0.0, 0.0, 10.0, 10.0, 10.0,
                Double.NaN, 0.0, 100.0, 0.0, 100.0,
            ),
        )
    }

    @Test
    fun rejectsDegenerateOrInvalidDimensions() {
        assertNull(BuildVolumeFitPolicy.uniformScale(0.0, 2.0, 3.0, 100.0, 100.0, 100.0))
        assertNull(BuildVolumeFitPolicy.uniformScale(2.0, Double.NaN, 3.0, 100.0, 100.0, 100.0))
        assertNull(BuildVolumeFitPolicy.uniformScale(2.0, 3.0, 4.0, 100.0, 100.0, 100.0, -1.0))
        assertNull(BuildVolumeFitPolicy.uniformScale(2.0, 3.0, 4.0, 100.0, 100.0, 100.0, Double.NaN))
    }
}
