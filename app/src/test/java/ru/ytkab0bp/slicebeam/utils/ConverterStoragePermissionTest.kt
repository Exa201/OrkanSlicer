package ru.ytkab0bp.slicebeam.utils

import android.content.Intent
import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConverterStoragePermissionTest {
    @Test
    fun selectedTreeMustIncludeWriteGrant() {
        assertTrue(
            ConverterStorage.hasWritablePersistableGrant(Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
        )
        assertFalse(
            ConverterStorage.hasWritablePersistableGrant(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
        assertFalse(ConverterStorage.hasWritablePersistableGrant(0))
    }

    @Test
    fun revokedProviderPermissionIsFoundThroughWrappedCause() {
        val failure = IOException("Could not open provider", SecurityException("Permission Denial"))

        assertTrue(ConverterStorage.isPermissionFailure(failure))
        assertFalse(ConverterStorage.isPermissionFailure(IOException("Disk full")))
    }
}
