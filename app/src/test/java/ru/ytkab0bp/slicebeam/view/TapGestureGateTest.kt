package ru.ytkab0bp.slicebeam.view

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapGestureGateTest {
    @Test
    fun oneDownAndUpDispatchesAtMostOneClick() {
        val gate = TapGestureGate()

        gate.begin(pointerCount = 1)

        assertTrue(gate.finish(pointerCount = 1, moved = false, cancelled = false))
        assertFalse(gate.finish(pointerCount = 1, moved = false, cancelled = false))
    }

    @Test
    fun cancelledAndDraggedSequencesCannotDispatchOnRelease() {
        val gate = TapGestureGate()

        gate.begin(pointerCount = 1)
        assertFalse(gate.finish(pointerCount = 1, moved = false, cancelled = true))
        assertFalse(gate.finish(pointerCount = 1, moved = false, cancelled = false))

        gate.begin(pointerCount = 1)
        assertTrue(gate.finish(pointerCount = 1, moved = false, cancelled = false))

        gate.begin(pointerCount = 1)
        assertFalse(gate.finish(pointerCount = 1, moved = true, cancelled = false))
    }

    @Test
    fun secondPointerInvalidatesTheTap() {
        val gate = TapGestureGate()

        gate.begin(pointerCount = 1)
        gate.begin(pointerCount = 2)

        assertFalse(gate.finish(pointerCount = 1, moved = false, cancelled = false))
    }

    @Test
    fun slowCumulativeMovementBeyondTouchSlopIsNotTreatedAsATap() {
        val gate = TapGestureGate()
        val touchSlop = 10

        gate.begin(pointerCount = 1, x = 100f, y = 100f)

        // Every incremental event is below slop, while the total displacement is not.
        assertFalse(gate.movedBeyondTouchSlop(x = 106f, y = 100f, touchSlop = touchSlop))
        assertTrue(gate.movedBeyondTouchSlop(x = 112f, y = 100f, touchSlop = touchSlop))
        assertFalse(
            gate.finish(
                pointerCount = 1,
                moved = gate.movedBeyondTouchSlop(x = 112f, y = 100f, touchSlop = touchSlop),
                cancelled = false,
            ),
        )
    }

    @Test
    fun longHoldOnTheBuildPlateDoesNotTurnIntoATapWhenReleased() {
        val gate = TapGestureGate()
        val longPressTimeoutMs = 500L

        gate.begin(pointerCount = 1, x = 80f, y = 120f, downTimeMs = 1_000L)

        assertFalse(
            gate.finish(
                pointerCount = 1,
                moved = false,
                cancelled = false,
                eventTimeMs = 1_500L,
                x = 80f,
                y = 120f,
                maxTapDurationMs = longPressTimeoutMs,
            ),
        )
    }

    @Test
    fun deliberateShortTapRemainsAcceptedWithDownTimeTracking() {
        val gate = TapGestureGate()

        gate.begin(pointerCount = 1, x = 80f, y = 120f, downTimeMs = 1_000L)

        assertTrue(
            gate.finish(
                pointerCount = 1,
                moved = false,
                cancelled = false,
                eventTimeMs = 1_180L,
                x = 80f,
                y = 120f,
                maxTapDurationMs = 500L,
            ),
        )
    }

    @Test
    fun smallFingerJitterStillCountsAsATap() {
        val gate = TapGestureGate()
        gate.begin(pointerCount = 1, x = 50f, y = 75f)

        assertFalse(gate.movedBeyondTouchSlop(x = 54f, y = 79f, touchSlop = 10))
        assertTrue(gate.finish(pointerCount = 1, moved = false, cancelled = false))
    }

    @Test
    fun rapidDuplicateTapInTheSameSpotIsCoalesced() {
        val gate = TapGestureGate()

        gate.begin(pointerCount = 1, x = 40f, y = 60f)
        assertTrue(
            gate.finish(
                pointerCount = 1,
                moved = false,
                cancelled = false,
                eventTimeMs = 1_000,
                x = 40f,
                y = 60f,
                duplicateRadiusPx = 20f,
            ),
        )

        gate.begin(pointerCount = 1, x = 41f, y = 61f)
        assertFalse(
            gate.finish(
                pointerCount = 1,
                moved = false,
                cancelled = false,
                eventTimeMs = 1_100,
                x = 41f,
                y = 61f,
                duplicateRadiusPx = 20f,
            ),
        )
    }

    @Test
    fun separateOrLaterTapsAreStillAccepted() {
        val gate = TapGestureGate()

        gate.begin(pointerCount = 1, x = 40f, y = 60f)
        assertTrue(gate.finish(pointerCount = 1, moved = false, cancelled = false, eventTimeMs = 1_000, x = 40f, y = 60f, duplicateRadiusPx = 20f))

        gate.begin(pointerCount = 1, x = 80f, y = 60f)
        assertTrue(gate.finish(pointerCount = 1, moved = false, cancelled = false, eventTimeMs = 1_100, x = 80f, y = 60f, duplicateRadiusPx = 20f))

        gate.begin(pointerCount = 1, x = 40f, y = 60f)
        assertTrue(gate.finish(pointerCount = 1, moved = false, cancelled = false, eventTimeMs = 1_500, x = 40f, y = 60f, duplicateRadiusPx = 20f))
    }
}
