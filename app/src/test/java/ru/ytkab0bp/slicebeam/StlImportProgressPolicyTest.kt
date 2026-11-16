package ru.ytkab0bp.slicebeam

import org.junit.Assert.assertEquals
import org.junit.Test

class StlImportProgressPolicyTest {
    @Test
    fun mapsNativeFractionToBoundedReadProgressAndPreparation() {
        assertEquals(0, StlImportProgressPolicy.stepForFraction(-0.2f))
        assertEquals(4, StlImportProgressPolicy.stepForFraction(0.24f))
        assertEquals(9, StlImportProgressPolicy.stepForFraction(0.49f))
        assertEquals(10, StlImportProgressPolicy.stepForFraction(0.5f))
        assertEquals(10, StlImportProgressPolicy.stepForFraction(1f))
        assertEquals(10, StlImportProgressPolicy.stepForFraction(2f))
    }
}
