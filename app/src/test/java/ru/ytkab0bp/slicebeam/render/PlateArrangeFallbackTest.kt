package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateArrangeFallbackTest {
    @Test fun normalSpacingSuccessDoesNotRunCompactOrRestore() {
        var compactCalls = 0
        var minimumGapCalls = 0
        var restoreCalls = 0
        val arranged = PlateArrangeFallback.run(
            arrangeWithNormalSpacing = { true },
            restoreOriginalPositions = { restoreCalls++ },
            arrangeCompactly = { compactCalls++; true },
            arrangeWithMinimumGap = { minimumGapCalls++; true },
        )

        assertTrue(arranged)
        assertEquals(0, compactCalls)
        assertEquals(0, minimumGapCalls)
        assertEquals(0, restoreCalls)
    }

    @Test fun compactSpacingRetriesAfterRestoringFailedNormalAttempt() {
        var attempts = 0
        var minimumGapCalls = 0
        var restoreCalls = 0
        val arranged = PlateArrangeFallback.run(
            arrangeWithNormalSpacing = { attempts++; false },
            restoreOriginalPositions = { restoreCalls++ },
            arrangeCompactly = { attempts++; true },
            arrangeWithMinimumGap = { minimumGapCalls++; true },
        )

        assertTrue(arranged)
        assertEquals(2, attempts)
        assertEquals(0, minimumGapCalls)
        assertEquals(1, restoreCalls)
    }

    @Test fun minimumPositiveGapIsTriedAfterNormalAndCompactSpacingFail() {
        var attempts = 0
        var restoreCalls = 0
        val arranged = PlateArrangeFallback.run(
            arrangeWithNormalSpacing = { attempts++; false },
            restoreOriginalPositions = { restoreCalls++ },
            arrangeCompactly = { attempts++; false },
            arrangeWithMinimumGap = { attempts++; true },
        )

        assertTrue(arranged)
        assertEquals(3, attempts)
        assertEquals(2, restoreCalls)
    }

    @Test fun completeFailureRestoresOriginalPositionsAfterAllAttempts() {
        var restoreCalls = 0
        val arranged = PlateArrangeFallback.run(
            arrangeWithNormalSpacing = { false },
            restoreOriginalPositions = { restoreCalls++ },
            arrangeCompactly = { false },
            arrangeWithMinimumGap = { false },
        )

        assertFalse(arranged)
        assertEquals(3, restoreCalls)
    }
}
