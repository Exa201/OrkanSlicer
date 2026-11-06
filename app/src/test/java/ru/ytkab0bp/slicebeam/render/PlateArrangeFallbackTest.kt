package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateArrangeFallbackTest {
    @Test fun normalSpacingSuccessDoesNotRunCompactOrRestore() {
        var compactCalls = 0
        var restoreCalls = 0
        val arranged = PlateArrangeFallback.run(
            arrangeWithNormalSpacing = { true },
            restoreOriginalPositions = { restoreCalls++ },
            arrangeCompactly = { compactCalls++; true },
        )

        assertTrue(arranged)
        assertEquals(0, compactCalls)
        assertEquals(0, restoreCalls)
    }

    @Test fun compactSpacingRetriesAfterRestoringFailedNormalAttempt() {
        var attempts = 0
        var restoreCalls = 0
        val arranged = PlateArrangeFallback.run(
            arrangeWithNormalSpacing = { attempts++; false },
            restoreOriginalPositions = { restoreCalls++ },
            arrangeCompactly = { attempts++; true },
        )

        assertTrue(arranged)
        assertEquals(2, attempts)
        assertEquals(1, restoreCalls)
    }

    @Test fun completeFailureRestoresOriginalPositionsAfterBothAttempts() {
        var restoreCalls = 0
        val arranged = PlateArrangeFallback.run(
            arrangeWithNormalSpacing = { false },
            restoreOriginalPositions = { restoreCalls++ },
            arrangeCompactly = { false },
        )

        assertFalse(arranged)
        assertEquals(2, restoreCalls)
    }
}
