package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GCodeResultReplacementTest {
    @Test
    fun replacingViewerDetachesOldResultBeforeReleasingItsNativePointer() {
        val events = mutableListOf<String>()
        val previous = RecordingResult("previous", events)
        val next = RecordingResult("next", events)

        replaceGCodeResult(previous, next) {
            events += "viewer-installed-next"
        }

        assertEquals(listOf("viewer-installed-next", "released-previous"), events)
        assertTrue(previous.released)
        assertFalse(next.released)
    }

    @Test
    fun clearingViewerReleasesItsResultOnlyAfterTheViewerIsCleared() {
        val events = mutableListOf<String>()
        val previous = RecordingResult("previous", events)

        replaceGCodeResult(previous, null) {
            events += "viewer-cleared"
        }

        assertEquals(listOf("viewer-cleared", "released-previous"), events)
        assertTrue(previous.released)
    }

    @Test
    fun retainingSameResultDoesNotReleaseIt() {
        val events = mutableListOf<String>()
        val current = RecordingResult("current", events)

        replaceGCodeResult(current, current) {
            events += "viewer-reused"
        }

        assertEquals(listOf("viewer-reused"), events)
        assertFalse(current.released)
    }

    @Test
    fun failedViewerReplacementKeepsPreviousResultAlive() {
        val events = mutableListOf<String>()
        val previous = RecordingResult("previous", events)
        val failure = IllegalStateException("viewer setup failed")

        try {
            replaceGCodeResult(previous, RecordingResult("next", events)) {
                events += "viewer-install-failed"
                throw failure
            }
            throw AssertionError("replacement should propagate the viewer failure")
        } catch (actual: IllegalStateException) {
            assertEquals(failure, actual)
        }

        assertEquals(listOf("viewer-install-failed"), events)
        assertFalse(previous.released)
    }

    private class RecordingResult(
        private val label: String,
        private val events: MutableList<String>,
    ) : GCodeProcessorResult(0L) {
        var released = false
            private set

        override fun release() {
            if (!released) {
                released = true
                events += "released-$label"
            }
        }
    }
}
