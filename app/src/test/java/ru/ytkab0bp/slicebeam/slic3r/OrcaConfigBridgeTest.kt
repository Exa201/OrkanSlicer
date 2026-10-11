package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Test

class OrcaConfigBridgeTest {
    @Test
    fun reportsNativeLibraryFailuresInGermanWithoutLeakingLinkerDetails() {
        val message = OrcaConfigBridge.errorMessage(UnsatisfiedLinkError("missing libprivate.so"))

        assertEquals(
            "Der native Orca-Kern konnte nicht geladen werden. Prüfe die Gerätearchitektur und die mitgelieferten Bibliotheken.",
            message,
        )
    }

    @Test
    fun reportsSchemaFailuresWithUsefulContext() {
        val message = OrcaConfigBridge.errorMessage(IllegalStateException("invalid schema"))

        assertEquals("Das Orca-Einstellungsschema konnte nicht geladen werden: invalid schema", message)
    }
}
