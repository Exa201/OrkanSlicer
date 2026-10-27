package ru.ytkab0bp.slicebeam.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaintInputCoalescerTest {
    @Test
    fun rapidMovesKeepOnlyLatestPendingPointAndScheduleOneDrain() {
        val coalescer = PaintInputCoalescer()

        assertTrue(coalescer.submit(1f, 2f))
        repeat(100) { index ->
            assertFalse(coalescer.submit(index.toFloat(), (index * 2).toFloat()))
        }

        assertEquals(PaintInputCoalescer.Point(99f, 198f), coalescer.takePending())
        assertFalse(coalescer.finishDrain())
        assertTrue("a later gesture must be able to schedule another GL drain", coalescer.submit(3f, 4f))
    }

    @Test
    fun flushReturnsTheLastPointBeforeStrokeCompletionAndResetsScheduling() {
        val coalescer = PaintInputCoalescer()
        assertTrue(coalescer.submit(1f, 1f))
        assertFalse(coalescer.submit(8f, 9f))

        assertEquals(PaintInputCoalescer.Point(8f, 9f), coalescer.flush())
        assertTrue("input after flush must queue a fresh drain", coalescer.submit(10f, 11f))
        assertEquals(PaintInputCoalescer.Point(10f, 11f), coalescer.takePending())
        assertFalse(coalescer.finishDrain())
    }
}
