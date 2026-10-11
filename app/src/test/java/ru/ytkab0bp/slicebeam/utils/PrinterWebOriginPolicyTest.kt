package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrinterWebOriginPolicyTest {
    @Test
    fun resolvesMoonrakerAddressToPrinterDashboardAndKeepsNormalWebHost() {
        assertEquals("http://192.168.1.100/", PrinterWebOriginPolicy.dashboardUrl("192.168.1.100:7125"))
        assertEquals("http://192.168.1.100/", PrinterWebOriginPolicy.dashboardUrl("http://192.168.1.100:7125"))
        assertEquals("http://192.168.1.100", PrinterWebOriginPolicy.dashboardUrl("192.168.1.100"))
        assertNull(PrinterWebOriginPolicy.dashboardUrl("  "))
    }

    @Test
    fun allowsPrinterOriginAndBlocksCrossOriginNavigation() {
        val dashboard = PrinterWebOriginPolicy.dashboardUrl("192.168.1.100:7125")
        assertTrue(PrinterWebOriginPolicy.allows(dashboard, "http://192.168.1.100/web/"))
        assertFalse(PrinterWebOriginPolicy.allows(dashboard, "http://example.com/"))
        assertFalse(PrinterWebOriginPolicy.allows(dashboard, "https://192.168.1.100/"))
        assertFalse(PrinterWebOriginPolicy.allows(dashboard, "http://192.168.1.100:7125/"))
    }
}
