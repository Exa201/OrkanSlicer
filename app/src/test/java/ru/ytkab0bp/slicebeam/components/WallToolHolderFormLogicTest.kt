package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class WallToolHolderFormLogicTest {
    @Test fun defaultHolderIsValidAndCountUsesWholeNumbers() {
        assertNull(WallToolHolderFormLogic.validate(WallToolHolderFormLogic.defaults()))
        assertEquals(-1, WallToolHolderFormLogic.parseCount("4.5"))
    }

    @Test fun numberOfHoldersIsLimitedToTwoThroughSix() {
        assertSame(WallToolHolderIssue.COUNT,
            WallToolHolderFormLogic.validate(WallToolHolderFormLogic.defaults().copy(count = "1")))
        assertSame(WallToolHolderIssue.COUNT,
            WallToolHolderFormLogic.validate(WallToolHolderFormLogic.defaults().copy(count = "seven")))
    }

    @Test fun dimensionalValuesAcceptDecimalCommaAndRejectOutOfRange() {
        assertNull(WallToolHolderFormLogic.validate(WallToolHolderFormLogic.defaults().copy(clearance = "0,6")))
        assertSame(WallToolHolderIssue.GUIDE_HEIGHT,
            WallToolHolderFormLogic.validate(WallToolHolderFormLogic.defaults().copy(guideHeight = "36")))
    }
}
