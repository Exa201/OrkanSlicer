package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PlantTagsFormLogicTest {
    @Test fun defaultFourMarkerSetIsValidAndKeepsTheFourNames() {
        val values = PlantTagsFormLogic.defaults()
        assertEquals(listOf("Tomate", "Basilikum", "Petersilie", "Minze"), PlantTagsFormLogic.labels(values.names))
        assertNull(PlantTagsFormLogic.validate(values))
    }

    @Test fun oneToEightNonEmptyLabelsAreRequired() {
        assertSame(PlantTagsIssue.COUNT,
            PlantTagsFormLogic.validate(PlantTagsFormLogic.defaults().copy(names = " \n\t")))
        assertSame(PlantTagsIssue.COUNT,
            PlantTagsFormLogic.validate(PlantTagsFormLogic.defaults().copy(names = (1..9).joinToString("\n") { "P$it" })))
    }

    @Test fun labelLengthAndPrintBedFootprintAreChecked() {
        assertSame(PlantTagsIssue.NAME_LENGTH,
            PlantTagsFormLogic.validate(PlantTagsFormLogic.defaults().copy(names = "A".repeat(25))))
        assertSame(PlantTagsIssue.PLATE_SIZE,
            PlantTagsFormLogic.validate(PlantTagsFormLogic.defaults().copy(width = "100")))
    }

    @Test fun decimalCommasAndOptionalHoleAreSupported() {
        val values = PlantTagsFormLogic.defaults().copy(width = "70,5", raised = "1,2", hole = "0")
        assertNull(PlantTagsFormLogic.validate(values))
        assertSame(PlantTagsIssue.HOLE,
            PlantTagsFormLogic.validate(values.copy(hole = "13")))
    }
}
