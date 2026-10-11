package ru.ytkab0bp.slicebeam.components

import java.io.File
import org.junit.Assert.*
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.AnycubicGCodeProcessor

class SinglePhysicalSlotOutputTest {
    @Test fun implicitFirstToolIsExplicitlyRoutedToSlotFourWithoutDroppingLayers() {
        val source = File.createTempFile("slot-source", ".gcode")
        val output = File.createTempFile("slot-output", ".gcode")
        try {
            source.writeText("; HEADER_BLOCK_START\n; HEADER_BLOCK_END\nG28\n;LAYER_CHANGE\nG1 X10 E1\n;LAYER_CHANGE\nG1 X20 E2\nM400\n")
            assertFalse(AnycubicGCodeProcessor.scan(source).hasToolSelection)
            AnycubicGCodeProcessor.rewrite(source, output, AnycubicGCodeProcessor.scan(source), AnycubicGCodeProcessor.Options().apply {
                remappedTools = intArrayOf(3)
                gates = intArrayOf(3)
                versionName = "test"
            })
            val result = output.readText()
            assertTrue("Slot four must explicitly select printer tool T3", result.lines().contains("T3"))
            assertTrue("Slot selection must precede extrusion", result.indexOf("T3\n") < result.indexOf("G1 X10 E1"))
            assertTrue(result.contains("; filament: 4"))
            assertEquals(2, result.lines().count { it == ";LAYER_CHANGE" })
            assertTrue(result.contains("M400"))
        } finally { source.delete(); output.delete() }
    }
    @Test fun canonicalFanCommandSelectsTheSlotOnceBeforeFirstRetraction() {
        val source = File.createTempFile("slot-source", ".gcode")
        val output = File.createTempFile("slot-output", ".gcode")
        try {
            source.writeText("; HEADER_BLOCK_END\nG28\nM106 S0\n;LAYER_CHANGE\nG1 E-1 F3600\nG1 X10 E1\nM400\n")
            assertTrue(AnycubicGCodeProcessor.rewrite(source, output, AnycubicGCodeProcessor.scan(source), AnycubicGCodeProcessor.Options().apply {
                remappedTools = intArrayOf(1)
                gates = intArrayOf(1)
            }))
            val result = output.readText()
            assertEquals(1, result.lines().count { it == "T1" })
            assertTrue(result.indexOf("T1\n") < result.indexOf("M106 S0"))
        } finally { source.delete(); output.delete() }
    }

    @Test fun extrusionMentionedOnlyInACommentDoesNotActivateTheSlot() {
        val source = File.createTempFile("slot-source", ".gcode")
        val output = File.createTempFile("slot-output", ".gcode")
        try {
            source.writeText("; HEADER_BLOCK_END\nG1 X10 ; E1 is only a comment\nG1 E-1 F3600\nM400\n")
            AnycubicGCodeProcessor.rewrite(source, output, AnycubicGCodeProcessor.scan(source), AnycubicGCodeProcessor.Options().apply {
                remappedTools = intArrayOf(2)
                gates = intArrayOf(2)
            })
            val result = output.readText()
            assertEquals(1, result.lines().count { it == "T2" })
            assertTrue(result.indexOf("T2\n") > result.indexOf("G1 X10"))
            assertTrue(result.indexOf("T2\n") < result.indexOf("G1 E-1"))
        } finally { source.delete(); output.delete() }
    }
}
