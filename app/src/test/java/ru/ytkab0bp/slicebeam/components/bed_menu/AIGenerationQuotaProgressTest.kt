package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Test

class AIGenerationQuotaProgressTest {
    @Test
    fun remainingQuotaIsDisplayedAsAFraction() {
        assertEquals(0.7f, aiGenerationQuotaProgress(total = 10, generated = 3), 0.0001f)
    }

    @Test
    fun exhaustedOrExceededQuotaDoesNotProduceNegativeProgress() {
        assertEquals(0f, aiGenerationQuotaProgress(total = 10, generated = 12), 0f)
    }

    @Test
    fun zeroQuotaDoesNotProduceNanOrInfinity() {
        assertEquals(0f, aiGenerationQuotaProgress(total = 0, generated = 0), 0f)
    }
}
