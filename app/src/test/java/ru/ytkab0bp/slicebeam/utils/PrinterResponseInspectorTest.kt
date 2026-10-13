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

    @Test fun zeroErrorFieldDoesNotHideTopLevelDeviceError() {
        for (neutralError in listOf("0", "false", "\"\"", "{\"code\":0,\"message\":\"ok\"}")) {
            val detail = PrinterResponseInspector.embeddedError(bytes(
                """{"error":$neutralError,"code":10011725,"message":"The device cannot parse the file"}"""))
            assertEquals("The device cannot parse the file (Code 10011725)", detail)
        }
    }

    @Test fun zeroErrorObjectWithoutDeviceCodeIsSuccess() {
        assertNull(PrinterResponseInspector.embeddedError(bytes(
            """{"error":{"code":0,"message":"ok"},"result":{"filename":"test.gcode"}}""")))
    }

    @Test fun explicitRejectionIsNotACompletedUpload() {
        assertEquals("Printer busy", PrinterResponseInspector.embeddedError(bytes(
            """{"success":false,"message":"Printer busy","code":0}""")))
        assertEquals("Der Drucker hat den Auftrag abgelehnt.", PrinterResponseInspector.embeddedError(bytes(
            """{"success":false,"result":null}""")))
        assertNull(PrinterResponseInspector.embeddedError(bytes("""{"success":true,"result":"ok"}""")))
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

    @Test fun htmlFromPrinterApiIsExplainedWithoutClaimingPrintStarted() {
        val fallback = "Keine Upload-Bestätigung"
        assertTrue(PrinterResponseInspector.missingAcknowledgement(
            bytes("<html><body><center>200 OK</center></body></html>"), fallback).contains("Webseite"))
        assertEquals(fallback, PrinterResponseInspector.missingAcknowledgement(bytes("{}"), fallback))
    }
}
