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

    @Test
    fun acceptsOrcaFooterProgressCountOffByOneWhenHeaderAndLayerMarkersAgree() {
        val file = gcodeFile(
            """; total layer number: 2
                |; max_z_height: 0.40
                |;LAYER_CHANGE
                |;Z:0.20
                |G1 X1 E1
                |;LAYER_CHANGE
                |;Z:0.40
                |G1 X2 E1
                |M73 P100 R0
                |; EXECUTABLE_BLOCK_END
                |; total layers count = 3
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
    fun acceptsModelMaxHeightAboveFinalEmittedLayerWhenAllLayersAreComplete() {
        val contents = buildString {
            appendLine("; total layer number: 74")
            appendLine("; max_z_height: 15.10")
            repeat(74) { index ->
                appendLine(";LAYER_CHANGE")
                appendLine(";Z:${"%.1f".format(java.util.Locale.ROOT, 0.3 + index * 0.2)}")
                appendLine("G1 X$index E1")
            }
            appendLine("M73 P100 R0")
            appendLine("; EXECUTABLE_BLOCK_END")
            appendLine("; total layers count = 75")
        }
        val file = gcodeFile(
            contents,
        )
        try {
            val result = GCodeIntegrityValidator.validate(file)
            assertTrue(result.details, result.isValid())
            assertEquals(74, result.expectedLayers)
            assertEquals(74, result.actualLayers)
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsOrcaFooterMismatchWhenDeclaredLayerMarkerIsMissing() {
        val file = gcodeFile(
            """; total layer number: 2
                |; max_z_height: 0.40
                |;LAYER_CHANGE
                |;Z:0.20
                |G1 X1 E1
                |M73 P100 R0
                |; EXECUTABLE_BLOCK_END
                |; total layers count = 3
                |""".trimMargin(),
        )
        try {
            val result = GCodeIntegrityValidator.validate(file)
            assertEquals(GCodeIntegrityValidator.Failure.LAYER_COUNT_MISMATCH, result.failure)
            assertEquals(1, result.actualLayers)
        } finally {
            file.delete()
        }
    }

    private fun gcodeFile(contents: String): File = File.createTempFile("gcode-integrity", ".gcode").apply {
        writeText(contents)
    }
}
