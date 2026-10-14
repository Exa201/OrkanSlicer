package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoonrakerResponseInspectorTest {
    private fun bytes(value: String) = value.toByteArray(Charsets.UTF_8)

    @Test fun standardUploadReturnsTheActualStoredPath() {
        val acknowledgement = MoonrakerResponseInspector.uploadedFile(bytes(
            """{"item":{"root":"gcodes","path":"subdir/model-1.gcode"},"action":"create_file","print_started":false,"print_queued":false}"""))
        assertEquals("subdir/model-1.gcode", acknowledgement?.path)
        assertEquals("subdir/model-1.gcode", MoonrakerResponseInspector.uploadedFile(bytes(
            """{"item":{"root":"gcodes","path":"subdir/model-1.gcode"}}"""), "model-1.gcode")?.path)
        assertFalse(acknowledgement!!.printStarted)
        assertFalse(acknowledgement.printQueued)
        assertEquals("My model.gcode", MoonrakerResponseInspector.uploadedFile(bytes(
            """{"item":{"root":"gcodes","path":"My model.gcode"},"action":"create_file"}"""))?.path)
    }

    @Test fun wrappedUploadAndExistingPrintStateAreRecognized() {
        val started = MoonrakerResponseInspector.uploadedFile(bytes(
            """{"result":{"item":{"root":"gcodes","path":"part.gcode.3mf"},"print_started":true}}"""))
        val queued = MoonrakerResponseInspector.uploadedFile(bytes(
            """{"item":{"root":"gcodes","path":"part.gcode"},"print_queued":true,"action":"create_file"}"""))
        assertTrue(started!!.printStarted)
        assertTrue(queued!!.printQueued)
    }

    @Test fun invalidUploadAcknowledgementCannotStartAnotherFile() {
        for (payload in listOf("", "{}", "not json",
            """{"result":"ok"}""",
            """{"item":{"root":"config","path":"part.gcode"}}""",
            """{"item":{"root":"gcodes","path":"../other.gcode"}}""",
            """{"item":{"root":"gcodes","path":"/other.gcode"}}""",
            """{"item":{"root":"gcodes","path":"other.gcode"},"action":"delete_file"}""")) {
            assertNull("Accepted $payload", MoonrakerResponseInspector.uploadedFile(bytes(payload)))
        }
    }

    @Test fun acknowledgementForAnotherFileCannotBeStarted() {
        val reply = bytes("""{"item":{"root":"gcodes","path":"other.gcode"},"action":"create_file"}""")
        assertNull(MoonrakerResponseInspector.uploadedFile(reply, "wanted.gcode"))
    }

    @Test fun printStartRequiresAnExplicitOkResult() {
        assertTrue(MoonrakerResponseInspector.printStartConfirmed(bytes("""{"result":"ok"}""")))
        assertTrue(MoonrakerResponseInspector.printStartConfirmed(bytes("\"ok\"")))
        for (payload in listOf("", "{}", "not json", """{"result":null}""",
            """{"result":"busy"}""", """{"error":{"code":123,"message":"busy"}}""")) {
            assertFalse("Accepted $payload", MoonrakerResponseInspector.printStartConfirmed(bytes(payload)))
        }
    }
}
