package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelImportPackingOrderTest {
    @Test
    fun placesLargestFootprintsFirstAndKeepsEqualAndInvalidEntriesStable() {
        assertEquals(
            listOf(2, 0, 1, 3, 4),
            ModelImportPackingOrder.largestFirst(listOf(100.0, 100.0, 400.0, Double.NaN, 0.0)),
        )
    }
}
