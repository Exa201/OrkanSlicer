package ru.ytkab0bp.slicebeam

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelImportPlateCountPolicyTest {
    @Test
    fun nonThreeMfModelsAlwaysLoadAsOnePlate() {
        assertEquals(1, ModelImportPlateCountPolicy.resolve(false, 0))
        assertEquals(1, ModelImportPlateCountPolicy.resolve(false, 4))
    }

    @Test
    fun threeMfUsesDetectedPlateCountWhenValid() {
        assertEquals(1, ModelImportPlateCountPolicy.resolve(true, 1))
        assertEquals(6, ModelImportPlateCountPolicy.resolve(true, 6))
    }

    @Test
    fun malformedThreeMfFallsBackToOnePlate() {
        assertEquals(1, ModelImportPlateCountPolicy.resolve(true, 0))
        assertEquals(1, ModelImportPlateCountPolicy.resolve(true, -2))
    }
}
