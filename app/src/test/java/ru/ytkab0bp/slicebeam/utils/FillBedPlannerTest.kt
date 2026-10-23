package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FillBedPlannerTest {
    @Test
    fun estimatesOnlyObjectsThatFitTheBed() {
        assertEquals(4, FillBedPlanner.estimatedCapacity(220.0, 220.0, 100.0, 100.0, 6.0))
        assertEquals(0, FillBedPlanner.estimatedCapacity(220.0, 220.0, 221.0, 100.0, 6.0))
        assertEquals(0, FillBedPlanner.estimatedCapacity(220.0, 220.0, 100.0, 221.0, 6.0))
    }

    @Test
    fun invalidDimensionsDoNotCreateSpeculativeCapacity() {
        assertEquals(0, FillBedPlanner.estimatedCapacity(220.0, 220.0, 0.0, 20.0, 6.0))
        assertEquals(0, FillBedPlanner.estimatedCapacity(Double.NaN, 220.0, 20.0, 20.0, 6.0))
        assertEquals(0, FillBedPlanner.estimatedCapacity(220.0, 220.0, 20.0, 20.0, -1.0))
    }
}
