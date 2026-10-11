package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CableClipFormLogicTest {
    @Test fun defaultMountingClipIsValid() = assertNull(CableClipFormLogic.validate(CableClipFormLogic.defaults()))

    @Test fun mouthMustRemainNarrowerThanTheCableAndAtLeastFortyPercentWide() {
        assertSame(CableClipIssue.MOUTH,
            CableClipFormLogic.validate(CableClipFormLogic.defaults().copy(mouth = "6")))
        assertSame(CableClipIssue.MOUTH,
            CableClipFormLogic.validate(CableClipFormLogic.defaults().copy(mouth = "2")))
    }

    @Test fun mountDimensionsAreOptionalWhenMountTabIsDisabled() {
        val values = CableClipFormLogic.defaults().copy(withMount = false, tabLength = "bad", screwHole = "bad")
        assertNull(CableClipFormLogic.validate(values))
    }

    @Test fun decimalCommaAndMissingDimensionsAreHandled() {
        assertNull(CableClipFormLogic.validate(CableClipFormLogic.defaults().copy(clearance = "0,4")))
        assertSame(CableClipIssue.DIAMETER,
            CableClipFormLogic.validate(CableClipFormLogic.defaults().copy(diameter = "")))
    }
}
