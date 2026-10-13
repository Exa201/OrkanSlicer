package ru.ytkab0bp.slicebeam.slic3r

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrcaModelBridgeTest {
    @Test
    fun validatesPathPlateAndReadabilityBeforeCallingNativeCode() {
        assertEquals(
            "Es wurde kein Dateipfad zum Modell angegeben.",
            OrcaModelBridge.validateRequest(" ", 0, true, true),
        )
        assertEquals(
            "Die ausgewählte Druckplatte ist ungültig.",
            OrcaModelBridge.validateRequest("model.3mf", -1, true, true),
        )
        assertNotNull(OrcaModelBridge.validateRequest("missing.3mf", 0, false, false))
        assertNotNull(OrcaModelBridge.validateRequest("model.3mf", 0, true, false))
        assertNull(OrcaModelBridge.validateRequest("model.3mf", 1, true, true))
    }

    @Test
    fun invalidPathsReturnAReadableResultWithoutInitializingNativeLoader() {
        val result = OrcaModelBridge.load("", 0)

        assertFalse(result.isSuccess())
        assertEquals("Es wurde kein Dateipfad zum Modell angegeben.", result.errorMessage)
        assertTrue(result.cause is Slic3rRuntimeError)
    }

    @Test
    fun baseNameHandlesBothPathSeparatorsAndKeepsDotFilesIntact() {
        assertEquals("model", OrcaModelBridge.baseName("/cache/project/model.3mf"))
        assertEquals("part.one", OrcaModelBridge.baseName("C:\\models\\part.one.stl"))
        assertEquals(".profile", OrcaModelBridge.baseName(".profile"))
    }

    @Test
    fun nativeFailuresDoNotExposeLinkerDetails() {
        assertEquals(
            "Der native Orca-Kern konnte das Modell nicht laden. Prüfe die Gerätearchitektur und die mitgelieferten Bibliotheken.",
            OrcaModelBridge.errorMessage(UnsatisfiedLinkError("missing libprivate.so")),
        )
        assertEquals(
            "file is corrupt",
            OrcaModelBridge.errorMessage(Slic3rRuntimeError("file is corrupt")),
        )
    }

    @Test
    fun emptyAfterRepairHasAnActionableMessage() {
        val message = OrcaModelBridge.errorMessage(Slic3rRuntimeError("STL_NO_CONNECTED_FACETS"))
        assertTrue(message.contains("keine zusammenhängenden"))
        assertTrue(message.contains("Desktop-Slicer"))
    }

    @Test
    fun emptyRegularFilesPassTheFilesystemPreflight() {
        val emptyFile = File.createTempFile("orca-model-preflight", ".stl")
        try {
            assertNull(OrcaModelBridge.validateRequest(
                emptyFile.absolutePath, 0, emptyFile.isFile, emptyFile.canRead(),
            ))
        } finally {
            emptyFile.delete()
        }
    }
}
