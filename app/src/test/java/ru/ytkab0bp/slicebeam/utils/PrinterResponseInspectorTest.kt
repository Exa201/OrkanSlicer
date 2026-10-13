package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrinterResponseInspectorTest {
    private fun bytes(text: String) = text.toByteArray(Charsets.UTF_8)

    @Test fun moonrakerResultIsNotAnEmbeddedError() {
        assertNull(PrinterResponseInspector.embeddedError(bytes("""{"result":{"item":{"path":"test.gcode"}}}""")))
        assertNull(PrinterResponseInspector.embeddedError(bytes("""{"result":"ok","code":0}""")))
        assertNull(PrinterResponseInspector.embeddedError(bytes("""{"result":"ok","error":0}""")))
    }

    @Test fun httpSuccessWithErrorObjectDoesNotMeanPrintStarted() {
        val detail = PrinterResponseInspector.embeddedError(bytes(
            """{"error":{"code":10011703,"message":"unknown filament in extruder"}}"""))
        assertEquals("unknown filament in extruder (Code 10011703)", detail)
    }

    @Test fun topLevelDeviceErrorCodeIsReported() {
        val detail = PrinterResponseInspector.embeddedError(bytes(
            """{"code":10011725,"message":"The device cannot parse the file"}"""))
        assertEquals("The device cannot parse the file (Code 10011725)", detail)
    }

    @Test fun stringErrorAndMalformedFailureBodiesRemainReadable() {
        assertEquals("Unable to start", PrinterResponseInspector.embeddedError(bytes("""{"error":"Unable to start"}""")))
        assertEquals("Bad gateway", PrinterResponseInspector.failureDetail(bytes("Bad gateway"), "fallback"))
        assertEquals("fallback", PrinterResponseInspector.failureDetail(null, "fallback"))
    }

    @Test fun longDeviceResponsesAreBounded() {
        val detail = PrinterResponseInspector.failureDetail(bytes("x".repeat(5000)), "fallback")
        assertEquals(500, detail.length)
        assertFalse(detail.contains("fallback"))
        assertTrue(detail.all { it == 'x' })
    }
}
