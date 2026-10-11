package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PhoneStandFormLogicTest {
    @Test
    fun defaultStandDimensionsAreAccepted() {
        assertNull(PhoneStandFormLogic.validate(PhoneStandFormLogic.defaults()))
    }

    @Test
    fun decimalCommaIsSupportedAndNonNumericInputIsRejected() {
        assertEquals(16.5f, PhoneStandFormLogic.parse("16,5"))
        assertSame(
            PhoneStandIssue.WIDTH,
            PhoneStandFormLogic.validate(PhoneStandFormLogic.defaults().copy(width = "phone")),
        )
    }

    @Test
    fun cableChannelsMustFitInsideTheirOpeningAndBase() {
        assertSame(
            PhoneStandIssue.CABLE_GAP,
            PhoneStandFormLogic.validate(PhoneStandFormLogic.defaults().copy(width = "55", cableGap = "31")),
        )
        assertSame(
            PhoneStandIssue.CABLE_DEPTH,
            PhoneStandFormLogic.validate(PhoneStandFormLogic.defaults().copy(depth = "65", cableDepth = "50")),
        )
    }

    @Test
    fun tooSteepSupportCombinationIsRejected() {
        val values = PhoneStandFormLogic.defaults().copy(
            tilt = "35",
            depth = "65",
            backHeight = "140",
        )
        assertSame(PhoneStandIssue.SUPPORT_CLEARANCE, PhoneStandFormLogic.validate(values))
    }
}
