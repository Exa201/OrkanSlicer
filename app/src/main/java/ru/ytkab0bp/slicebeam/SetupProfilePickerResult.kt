package ru.ytkab0bp.slicebeam

import android.app.Activity
/** Filters canceled or incomplete document-picker results before profile import begins. */
internal object SetupProfilePickerResult {
    fun shouldImport(resultCode: Int, successResultCode: Int, hasUri: Boolean): Boolean =
        resultCode == successResultCode && hasUri
}
