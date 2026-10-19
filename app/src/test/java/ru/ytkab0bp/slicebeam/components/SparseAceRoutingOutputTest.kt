package ru.ytkab0bp.slicebeam.components

import java.io.File
import org.junit.Assert.*
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.AnycubicGCodeProcessor

/** Empty ACE slots keep their index; later tools are never shifted onto a different gate. */
class SparseAceRoutingOutputTest {
    @Test fun laterToolKeepsItsSlotAfterAnEmptyGateAndKeepsAllLayers() {
        val source = File.createTempFile("ace-gap-source", ".gcode")
        val output = File.createTempFile("ace-gap-output", ".gcode")
        try {
            source.writeText(
                "; HEADER_BLOCK_START\n" +
                    "; filament: 1,2,3\n" +
                    "; filament_colour = #111111;#222222;#333333\n" +
                    "; filament_type = PETG;PETG;PETG\n" +
                    "; HEADER_BLOCK_END\n" +
                    ";LAYER_CHANGE\nT0\nG1 X1 E1\n" +
                    ";LAYER_CHANGE\nT2\nG1 X3 E3\n" +
                    "M400\n",
            )
            val scan = AnycubicGCodeProcessor.scan(source)
            assertEquals(2, scan.maxTool)
            assertTrue(AnycubicGCodeProcessor.rewrite(source, output, scan,
                AnycubicGCodeProcessor.Options().apply {
                    remappedTools = intArrayOf(0, 1, 2)
                    gates = intArrayOf(0, 1, 2)
                    colors = "#111111;#19C8A0;#333333"
                    materials = "PETG;PLA;PETG"
                }))
            val lines = output.readLines()
            assertEquals(listOf("T0", "T2"), lines.filter { it.matches(Regex("T[0-9]+")) })
            assertTrue(lines.contains("; filament: 1,2,3"))
            assertTrue(lines.contains("; filament_colour = #111111;#19C8A0;#333333"))
            assertEquals(2, lines.count { it == ";LAYER_CHANGE" })
            assertTrue(lines.contains("G1 X3 E3"))
            assertEquals("M400", lines.last())
        } finally {
            source.delete()
            output.delete()
        }
    }

    @Test fun selectedBlueAndWhiteGatesDriveToolCommandsAndGcodePalette() {
        val source = File.createTempFile("ace-color-source", ".gcode")
        val output = File.createTempFile("ace-color-output", ".gcode")
        try {
            source.writeText(
                "; HEADER_BLOCK_START\n" +
                    "; filament: 1,2\n" +
                    "; filament_colour = #212721;#FFFFFF\n" +
                    "; filament_type = PETG;PETG\n" +
                    "; HEADER_BLOCK_END\nT0\nG1 X1 E1\nT1\nG1 X2 E2\nM400\n",
            )
            val scan = AnycubicGCodeProcessor.scan(source)
            val routing = ru.ytkab0bp.slicebeam.utils.AnycubicAceRoutingPolicy.resolve(
                toolCount = 2,
                usedTools = intArrayOf(0, 1),
                loadedGates = intArrayOf(0, 1, 2, 3),
                toolToGate = intArrayOf(0, 1, 2, 3),
                selectedGates = intArrayOf(1, 3),
            )
            assertNull(routing.unavailableTool)
            val physical = listOf(
                ru.ytkab0bp.slicebeam.utils.FilamentSlot(0xFFFFFF, "PETG"),
                ru.ytkab0bp.slicebeam.utils.FilamentSlot(0x0047BB, "PLA"),
            )
            val outputTools = routing.toolCommands
            val paletteColors = AnycubicGCodeProcessor.formatMappedPalette(outputTools, physical, true)
            val paletteTypes = AnycubicGCodeProcessor.formatMappedPalette(outputTools, physical, false)
            assertTrue(AnycubicGCodeProcessor.rewrite(source, output, scan,
                AnycubicGCodeProcessor.Options().apply {
                    remappedTools = outputTools
                    gates = routing.gates
                    colors = paletteColors
                    materials = paletteTypes
                }))

            val lines = output.readLines()
            assertEquals(listOf("T1", "T3"), lines.filter { it.matches(Regex("T[0-9]+")) })
            assertTrue(lines.contains("; filament: 2,4"))
            assertTrue(lines.contains("; filament_colour = #19C8A0;#FFFFFF;#19C8A0;#0047BB"))
            assertTrue(lines.contains("; filament_type = PLA;PETG;PLA;PLA"))
        } finally {
            source.delete()
            output.delete()
        }
    }
}
