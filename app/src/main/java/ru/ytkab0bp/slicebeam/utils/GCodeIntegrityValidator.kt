package ru.ytkab0bp.slicebeam.utils

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.Locale
import java.util.regex.Pattern

/** Checks that a sliced G-code has every declared layer and a completion marker after printing. */
object GCodeIntegrityValidator {
    private val LAYER_TOTAL = Pattern.compile("(?i)^;\\s*(?:total layer number|total layers count)\\s*[:=]\\s*(\\d+)\\s*$")
    private val NUMBERED_LAYER = Pattern.compile("(?i)^; BEFORE_LAYER_CHANGE (\\d+) @ ([0-9]+(?:\\.[0-9]+)?)mm\\s*$")
    private val EXTRUSION = Pattern.compile("(?:^|\\s)E([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))(?:\\s|$)")
    private val MOTION_COMMAND = Pattern.compile("(?i)^G[0-3](?:\\s|$).*$")
    private val LAYER_COMMAND = Pattern.compile("(?i)^;LAYER:\\s*\\d+\\s*$")
    private val COMPLETION_COMMAND = Pattern.compile("(?i)^M(?:2|30)(?:\\s|$).*")
    private val M73_COMPLETION = Pattern.compile("(?i)^M73\\s+P100(?:\\s|$).*")
    private val END_PRINT_COMMAND = Pattern.compile("(?i)^(?:END_PRINT|PRINT_END)(?:\\s|$).*")

    enum class Failure {
        NONE,
        INVALID_FILE,
        MISSING_LAYER_COUNT,
        LAYER_COUNT_MISMATCH,
        NUMBERING_GAP,
        LAST_LAYER_HAS_NO_EXTRUSION,
        END_BEFORE_LAST_LAYER,
        MISSING_COMPLETION_MARKER,
        READ_ERROR
    }

    class Result private constructor(
        @JvmField val failure: Failure,
        @JvmField val expectedLayers: Int,
        @JvmField val actualLayers: Int,
        @JvmField val details: String
    ) {
        fun isValid(): Boolean = failure == Failure.NONE

        companion object {
            internal fun create(failure: Failure, expectedLayers: Int, actualLayers: Int, details: String): Result =
                Result(failure, expectedLayers, actualLayers, details)
        }
    }

    /** Incremental validator so package builders can validate during their metadata scan. */
    class StreamingValidator {
        private var declaredLayers = -1
        private var declaredLayersAtEnd = -1
        private var layerMarkers = 0
        private var expectedNumber = 0
        private var lastLayerHasExtrusion = false
        private var sawNumberedLayers = false
        private var completionAfterLastLayer = false
        private var completionBeforeFinalLayer = false
        private var foundAnyCompletion = false
        private var numberingFailure: Result? = null

        fun acceptLine(line: String) {
            val trimmed = line.trim()
            val count = LAYER_TOTAL.matcher(trimmed)
            if (count.matches()) {
                val parsed = count.group(1)?.toIntOrNull() ?: throw NumberFormatException("Invalid declared layer count")
                if (trimmed.lowercase(Locale.ROOT).contains("total layer number")) declaredLayers = parsed
                else declaredLayersAtEnd = parsed
            }
            if (trimmed.equals(";LAYER_CHANGE", ignoreCase = true) || LAYER_COMMAND.matcher(trimmed).matches()) {
                layerMarkers++
                lastLayerHasExtrusion = false
            }
            val numbered = NUMBERED_LAYER.matcher(trimmed)
            if (numbered.matches()) {
                sawNumberedLayers = true
                val number = numbered.group(1)?.toIntOrNull() ?: throw NumberFormatException("Invalid numbered layer")
                if (number != expectedNumber && numberingFailure == null) {
                    numberingFailure = result(
                        Failure.NUMBERING_GAP, declaredLayers, layerMarkers,
                        "Layer numbering jumps from ${expectedNumber - 1} to $number."
                    )
                }
                expectedNumber++
            }
            if (layerMarkers > 0 && hasPositiveExtrusion(trimmed)) lastLayerHasExtrusion = true
            if (isCompletionMarker(trimmed)) {
                foundAnyCompletion = true
                if (layerMarkers == 0 || (declaredLayers > 0 && layerMarkers < declaredLayers)) completionBeforeFinalLayer = true
                else completionAfterLastLayer = true
            }
        }

        fun finish(): Result {
            numberingFailure?.let { return it }
            val expected = if (declaredLayers > 0) declaredLayers else declaredLayersAtEnd
            if (expected <= 0) return result(Failure.MISSING_LAYER_COUNT, expected, layerMarkers, "No valid total layer count was found in the sliced file.")
            if (layerMarkers != expected) return result(
                Failure.LAYER_COUNT_MISMATCH, expected, layerMarkers,
                "The file declares $expected layers but contains $layerMarkers layer starts."
            )
            if (sawNumberedLayers && expectedNumber != expected && expectedNumber != expected - 1) return result(
                Failure.NUMBERING_GAP, expected, layerMarkers,
                "Expected $expected layer numbers or $expected - 1 numbered transitions, but found $expectedNumber."
            )
            if (completionBeforeFinalLayer) return result(Failure.END_BEFORE_LAST_LAYER, expected, layerMarkers, "A print-completion command occurs before the final declared layer.")
            if (!lastLayerHasExtrusion) return result(Failure.LAST_LAYER_HAS_NO_EXTRUSION, expected, layerMarkers, "The final layer has no positive extrusion move.")
            if (!foundAnyCompletion || !completionAfterLastLayer) return result(Failure.MISSING_COMPLETION_MARKER, expected, layerMarkers, "No recognized print-completion marker follows the final layer.")
            return result(Failure.NONE, expected, layerMarkers, "All declared layers and the print completion are present.")
        }

        fun readError(error: IOException): Result = result(Failure.READ_ERROR, declaredLayers, layerMarkers, error.toString())
    }

    @JvmStatic
    fun validate(file: File?): Result {
        if (file == null || !file.isFile || file.length() == 0L) {
            return result(Failure.INVALID_FILE, 0, 0, "The sliced file is missing or empty.")
        }
        val validator = StreamingValidator()
        try {
            BufferedReader(FileReader(file), 64 * 1024).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    validator.acceptLine(line)
                }
            }
        } catch (error: IOException) {
            return validator.readError(error)
        } catch (error: NumberFormatException) {
            return result(Failure.READ_ERROR, 0, 0, error.toString())
        }
        return validator.finish()
    }

    private fun hasPositiveExtrusion(line: String): Boolean {
        val comment = line.indexOf(';')
        val command = if (comment >= 0) line.substring(0, comment).trim() else line
        if (!MOTION_COMMAND.matcher(command).matches()) return false
        val extrusion = EXTRUSION.matcher(command)
        while (extrusion.find()) {
            try {
                if ((extrusion.group(1)?.toDoubleOrNull() ?: 0.0) > 0.0) return true
            } catch (_: NumberFormatException) {
                // Ignore malformed extrusion values just as the previous Java implementation did.
            }
        }
        return false
    }

    private fun isCompletionMarker(line: String): Boolean =
        line.equals("; EXECUTABLE_BLOCK_END", ignoreCase = true)
                || COMPLETION_COMMAND.matcher(line).matches()
                || END_PRINT_COMMAND.matcher(line).matches()
                || M73_COMPLETION.matcher(line).matches()

    private fun result(failure: Failure, expected: Int, actual: Int, details: String): Result =
        Result.create(failure, expected, actual, details)
}
