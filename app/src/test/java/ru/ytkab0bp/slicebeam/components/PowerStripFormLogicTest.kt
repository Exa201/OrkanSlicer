package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PowerStripFormLogicTest {
    @Test fun defaultTrayDimensionsAreValid() = assertNull(PowerStripFormLogic.validate(PowerStripFormLogic.defaults()))

    @Test fun cableNotchMustFitTheClearWidth() {
        val values = PowerStripFormLogic.defaults().copy(width = "30", clearance = "0.5", notch = "22")
        assertSame(PowerStripIssue.NOTCH, PowerStripFormLogic.validate(values))
    }

    @Test fun invalidAndLocalizedDecimalInputsAreHandled() {
        assertNull(PowerStripFormLogic.validate(PowerStripFormLogic.defaults().copy(clearance = "2,5")))
        assertSame(PowerStripIssue.LENGTH,
            PowerStripFormLogic.validate(PowerStripFormLogic.defaults().copy(length = "bad")))
    }
}
