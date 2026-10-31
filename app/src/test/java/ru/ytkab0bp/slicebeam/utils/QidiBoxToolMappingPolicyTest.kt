package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QidiBoxToolMappingPolicyTest {
    @Test fun unmappedToolsDoNotOverwritePrinterVariables() {
        val script = QidiBoxToolMappingPolicy.buildSaveVariablesScript(
            intArrayOf(2, -1, 15), intArrayOf(0, 1, 2),
        )
        assertEquals(
            "SAVE_VARIABLE VARIABLE=value_t0 VALUE=\"'slot2'\"\n" +
                "SAVE_VARIABLE VARIABLE=value_t2 VALUE=\"'slot15'\"",
            script,
        )
    }

    @Test fun allowsTheSamePhysicalBoxSlotForMoreThanOneGcodeTool() {
        val script = QidiBoxToolMappingPolicy.buildSaveVariablesScript(
            intArrayOf(4, 4), intArrayOf(0, 1),
        )
        assertEquals(2, script.lines().size)
        assertTrue(script.lines().all { it.contains("slot4") })
    }

    @Test fun mappingRoundTripsAndInvalidValuesBecomeUnmapped() {
        val encoded = QidiBoxToolMappingPolicy.encode(intArrayOf(0, 15, -1, 99))
        assertEquals("0,15,-1,-1", encoded)
        assertArrayEquals(intArrayOf(0, 15, -1, -1), QidiBoxToolMappingPolicy.decode(encoded, 4))
        assertArrayEquals(intArrayOf(-1, -1, -1), QidiBoxToolMappingPolicy.decode("x,16", 3))
    }

    @Test fun mappingIdentityIsStableButPrinterScoped() {
        val key = QidiBoxToolMappingPolicy.preferenceKey("Qidi Q2", "http://printer.local/")
        assertEquals(key, QidiBoxToolMappingPolicy.preferenceKey(" qidi q2 ", "http://PRINTER.local"))
        assertNotEquals(key, QidiBoxToolMappingPolicy.preferenceKey("Qidi Q2", "http://other.local"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidActiveToolIsRejectedInsteadOfSilentlyMapped() {
        QidiBoxToolMappingPolicy.buildSaveVariablesScript(intArrayOf(0), intArrayOf(16))
    }
}
