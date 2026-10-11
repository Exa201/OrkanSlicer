package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CableGrommetFormLogicTest {
    @Test fun defaultGrommetDimensionsAreValid() = assertNull(CableGrommetFormLogic.validate(CableGrommetFormLogic.defaults()))

    @Test fun flangeMustClearTheCableTubeAndWall() {
        val values = CableGrommetFormLogic.defaults().copy(flange = "30", diameter = "12", clearance = "1.5", wall = "6")
        assertSame(CableGrommetIssue.FLANGE, CableGrommetFormLogic.validate(values))
    }

    @Test fun decimalCommaIsAcceptedAndMissingMeasurementsFail() {
        assertNull(CableGrommetFormLogic.validate(CableGrommetFormLogic.defaults().copy(clearance = "0,4")))
        assertSame(CableGrommetIssue.TUBE_LENGTH,
            CableGrommetFormLogic.validate(CableGrommetFormLogic.defaults().copy(tubeLength = "")))
    }
}
