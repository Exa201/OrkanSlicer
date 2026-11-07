package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ModelEditOperationTest {
    @Test
    fun successfulNativeEditReturnsItsResult() {
        assertEquals(3, ModelEditOperation.runSafely(failureResult = MODEL_EDIT_OPERATION_FAILED) { 3 })
    }

    @Test
    fun nativeEditExceptionBecomesFailureResultAndIsReported() {
        val failure = IllegalStateException("native backup failed")
        var reported: Exception? = null

        val result = ModelEditOperation.runSafely(
            failureResult = MODEL_EDIT_OPERATION_FAILED,
            onFailure = { reported = it },
        ) { throw failure }

        assertEquals(MODEL_EDIT_OPERATION_FAILED, result)
        assertSame(failure, reported)
    }
}
