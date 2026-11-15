package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelObjectSearchPolicyTest {
    private val rows = listOf(
        ModelObjectRow("Housing", 0, "PLA", 0xff112233.toInt(), true, true),
        ModelObjectRow("Bracket", 1, "PETG", 0xff445566.toInt(), false, true,
            listOf(ModelObjectPartRow("Insert", 2, "TPU", 0xff778899.toInt()))),
        ModelObjectRow("Cover", 0, "ABS", 0xffaabbcc.toInt(), false, true),
    )

    @Test fun emptyQueryReturnsEveryOriginalIndex() {
        assertEquals(listOf(0, 1, 2), ModelObjectSearchPolicy.matchingIndices(rows, "  "))
    }

    @Test fun objectNameAndMaterialSearchKeepOriginalIndices() {
        assertEquals(listOf(2), ModelObjectSearchPolicy.matchingIndices(rows, "cover"))
        assertEquals(listOf(1), ModelObjectSearchPolicy.matchingIndices(rows, "petg"))
    }

    @Test fun childPartsCanMatchWithoutReindexingOtherObjects() {
        assertEquals(listOf(1), ModelObjectSearchPolicy.matchingIndices(rows, "insert"))
        assertEquals(listOf(1), ModelObjectSearchPolicy.matchingIndices(rows, "tpu"))
    }
}
