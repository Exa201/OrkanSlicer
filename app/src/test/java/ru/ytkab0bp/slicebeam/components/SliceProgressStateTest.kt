package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.events.SlicingProgressEvent

class SliceProgressStateTest {
    @Test
    fun absentMessagePreservesStatusAndOnlyUpdatesBoundedProgress() {
        val initial = SliceProgressState(statusMessage = "Preparing")

        val updated = initial.apply(SlicingProgressEvent(138, null)) { error("No message to localize") }

        assertEquals("Preparing", updated.statusMessage)
        assertEquals(100, updated.progress)
        assertTrue(updated.messages.isEmpty())
        assertTrue(updated.completed)
    }

    @Test
    fun localizesMessagesAndSuppressesRepeatedAdjacentSteps() {
        val initial = SliceProgressState(statusMessage = "Preparing")
        val first = initial.apply(SlicingProgressEvent(15, "Generating perimeters")) { "Walls" }
        val repeated = first.apply(SlicingProgressEvent(25, "Generating perimeters")) { "Walls" }

        assertEquals("Walls", repeated.statusMessage)
        assertEquals(listOf("Walls"), repeated.messages)
        assertEquals(25, repeated.progress)
    }

    @Test
    fun repeatedStepAfterAnotherStepIsRecordedAgain() {
        val initial = SliceProgressState(statusMessage = "Preparing")
        val walls = initial.apply(SlicingProgressEvent(20, "perimeters")) { "Walls" }
        val infill = walls.apply(SlicingProgressEvent(60, "infill")) { "Infill" }
        val wallsAgain = infill.apply(SlicingProgressEvent(85, "perimeters")) { "Walls" }

        assertEquals(listOf("Walls", "Infill", "Walls"), wallsAgain.messages)
    }

    @Test
    fun marksCompletionOnlyWhenNativeProgressReachesOneHundred() {
        val initial = SliceProgressState(statusMessage = "Preparing")

        assertFalse(initial.apply(SlicingProgressEvent(99, null)) { "" }.completed)
        assertTrue(initial.apply(SlicingProgressEvent(100, null)) { "" }.completed)
    }
}
