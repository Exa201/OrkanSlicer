package ru.ytkab0bp.slicebeam.components

import java.io.File
import org.junit.Assert.*
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.AnycubicGCodeProcessor

/** The second physical ACE gate can be empty without losing a model color or its last layer. */
class SparseAceRoutingOutputTest {
    @Test fun threeModelColorsRouteAroundEmptySecondGateAndKeepAllLayers() {
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
                    ";LAYER_CHANGE\nT1\nG1 X2 E2\n" +
                    ";LAYER_CHANGE\nT2\nG1 X3 E3\n" +
                    "M400\n",
            )
            val scan = AnycubicGCodeProcessor.scan(source)
            assertEquals(2, scan.maxTool)
            assertTrue(AnycubicGCodeProcessor.rewrite(source, output, scan,
                AnycubicGCodeProcessor.Options().apply {
                    remappedTools = intArrayOf(0, 2, 3)
                    gates = intArrayOf(0, 2, 3)
                    colors = "#111111;#19C8A0;#222222;#333333"
                    materials = "PETG;PLA;PETG;PETG"
                }))
            val lines = output.readLines()
            assertEquals(listOf("T0", "T2", "T3"), lines.filter { it.matches(Regex("T[0-9]+")) })
            assertTrue(lines.contains("; filament: 1,3,4"))
            assertTrue(lines.contains("; filament_colour = #111111;#19C8A0;#222222;#333333"))
            assertEquals(3, lines.count { it == ";LAYER_CHANGE" })
            assertTrue(lines.contains("G1 X3 E3"))
            assertEquals("M400", lines.last())
        } finally {
            source.delete()
            output.delete()
        }
    }
}
