package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GCodeIntegrityValidatorTest {
    @Test
    fun acceptsOneNumberedTransitionBetweenTwoDeclaredLayers() {
        val file = gcodeFile(
            """; total layer number: 2
                |; max_z_height: 0.40
                |;LAYER_CHANGE
                |;Z:0.20
                |G1 X1 E1
                |; BEFORE_LAYER_CHANGE 0 @ 0.20mm
                |;LAYER_CHANGE
                |;Z:0.40
                |G1 X2 E1
                |; EXECUTABLE_BLOCK_END
                |""".trimMargin(),
        )
        try {
            val result = GCodeIntegrityValidator.validate(file)
            assertTrue(result.details, result.isValid())
            assertEquals(2, result.expectedLayers)
            assertEquals(2, result.actualLayers)
        } finally {
            file.delete()
        }
    }

    @Test
    fun stillRejectsTooFewNumberedTransitionsWithAccurateFailure() {
        val file = gcodeFile(
            """; total layer number: 3
                |; max_z_height: 0.60
                |;LAYER_CHANGE
                |;Z:0.20
                |G1 X1 E1
                |; BEFORE_LAYER_CHANGE 0 @ 0.20mm
                |;LAYER_CHANGE
                |;Z:0.40
                |G1 X2 E1
                |;LAYER_CHANGE
                |;Z:0.60
                |G1 X3 E1
                |; EXECUTABLE_BLOCK_END
                |""".trimMargin(),
        )
        try {
            val result = GCodeIntegrityValidator.validate(file)
            assertEquals(GCodeIntegrityValidator.Failure.NUMBERING_GAP, result.failure)
            assertEquals(3, result.actualLayers)
        } finally {
            file.delete()
        }
    }

    private fun gcodeFile(contents: String): File = File.createTempFile("gcode-integrity", ".gcode").apply {
        writeText(contents)
    }
}
