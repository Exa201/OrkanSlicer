package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PlanterPotFormLogicTest {
    @Test
    fun standardPlanterAndSaucerDimensionsAreAccepted() {
        assertNull(PlanterPotFormLogic.validate(PlanterPotFormLogic.defaults()))
    }

    @Test
    fun decimalCommaIsParsedAndMissingNumbersAreRejected() {
        assertEquals(2.4f, PlanterPotFormLogic.parse(" 2,4 "))
        assertSame(
            PlanterPotIssue.WALL,
            PlanterPotFormLogic.validate(PlanterPotFormLogic.defaults().copy(wall = "")),
        )
    }

    @Test
    fun drainMustFitInsideTheUsableFloor() {
        val invalid = PlanterPotFormLogic.defaults().copy(bottomDiameter = "30", wall = "5", drain = "20")
        assertSame(PlanterPotIssue.DRAIN, PlanterPotFormLogic.validate(invalid))
    }

    @Test
    fun disabledSaucerDoesNotBlockGenerationOnSaucerOnlyFields() {
        val values = PlanterPotFormLogic.defaults().copy(
            withSaucer = false,
            saucerClearance = "invalid",
            saucerWall = "invalid",
            saucerHeight = "invalid",
            saucerBase = "invalid",
        )
        assertNull(PlanterPotFormLogic.validate(values))
    }

    @Test
    fun everySaucerDimensionIsCheckedWhenSaucerIsEnabled() {
        assertSame(PlanterPotIssue.SAUCER_CLEARANCE,
            PlanterPotFormLogic.validate(PlanterPotFormLogic.defaults().copy(saucerClearance = "0")))
        assertSame(PlanterPotIssue.SAUCER_WALL,
            PlanterPotFormLogic.validate(PlanterPotFormLogic.defaults().copy(saucerWall = "7")))
        assertSame(PlanterPotIssue.SAUCER_HEIGHT,
            PlanterPotFormLogic.validate(PlanterPotFormLogic.defaults().copy(saucerHeight = "3")))
        assertSame(PlanterPotIssue.SAUCER_BASE,
            PlanterPotFormLogic.validate(PlanterPotFormLogic.defaults().copy(saucerBase = "6")))
    }
}
