package ru.ytkab0bp.slicebeam

import android.app.Activity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupProfilePickerResultTest {
    @Test
    fun importsUriWhenPickerReturnsSuccessfulDocument() {
        assertTrue(
            SetupProfilePickerResult.shouldImport(
                Activity.RESULT_OK,
                Activity.RESULT_OK,
                hasUri = true,
            ),
        )
    }

    @Test
    fun ignoresCanceledAndIncompleteResults() {
        assertFalse(
            SetupProfilePickerResult.shouldImport(
                Activity.RESULT_CANCELED,
                Activity.RESULT_OK,
                hasUri = true,
            ),
        )
        assertFalse(
            SetupProfilePickerResult.shouldImport(
                Activity.RESULT_OK,
                Activity.RESULT_OK,
                hasUri = false,
            ),
        )
    }
}
