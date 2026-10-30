package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationTransformPolicyTest {
    @Test
    fun positionRulerChangesOnlyItsOwnAxisAndPreservesFractionalCoordinates() {
        val current = OrientationVector3(1.5, 2.5, 3.5)
        val values = OrientationAxisValues(12, -7, 18)
        assertEquals(
            OrientationVector3(12.0, 2.5, 3.5),
            OrientationTransformPolicy.positionFromRuler(current, values, OrientationAxis.X),
        )
        assertEquals(
            OrientationVector3(1.5, -7.0, 3.5),
            OrientationTransformPolicy.positionFromRuler(current, values, OrientationAxis.Y),
        )
        assertEquals(
            OrientationVector3(1.5, 2.5, 18.0),
            OrientationTransformPolicy.positionFromRuler(current, values, OrientationAxis.Z),
        )
    }

    @Test
    fun updatesOnlyTheRequestedPositionOrRotationAxis() {
        val initial = OrientationAxisValues(-12, 24, 90)

        assertEquals(OrientationAxisValues(45, 24, 90), OrientationTransformPolicy.setAxis(initial, OrientationAxis.X, 45))
        assertEquals(OrientationAxisValues(-12, -30, 90), OrientationTransformPolicy.setAxis(initial, OrientationAxis.Y, -30))
        assertEquals(OrientationAxisValues(-12, 24, 180), OrientationTransformPolicy.setAxis(initial, OrientationAxis.Z, 180))
    }

    @Test
    fun rotationNormalizationHandlesWholeTurnsAndNonFiniteValues() {
        assertEquals(90.0, OrientationTransformPolicy.normalizedDegrees(450.0), 0.0)
        assertEquals(-90.0, OrientationTransformPolicy.normalizedDegrees(-450.0), 0.0)
        assertEquals(0.0, OrientationTransformPolicy.normalizedDegrees(Double.NaN), 0.0)
        assertEquals(OrientationVector3(10.0, -20.0, 0.0), OrientationTransformPolicy.normalizedDegrees(OrientationVector3(370.0, -380.0, Double.POSITIVE_INFINITY)))
    }
}
