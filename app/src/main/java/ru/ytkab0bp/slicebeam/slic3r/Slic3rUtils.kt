package ru.ytkab0bp.slicebeam.slic3r

import java.util.ArrayList
import java.util.regex.Pattern
import ru.ytkab0bp.slicebeam.utils.DebugUtils
import ru.ytkab0bp.slicebeam.utils.Vec3d

open class Slic3rUtils {
    companion object {
        // Matches a nozzle size embedded in a preset / printer name, e.g. "... 0.4 nozzle" -> "0.4".
        private val NOZZLE_PATTERN: Pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*nozzle")

        /** Extract the nozzle size token from a preset/printer name, or null if it has none. */
        @JvmStatic
        fun extractNozzle(name: String?): String? {
            if (name.isNullOrEmpty()) return null
            val matcher = NOZZLE_PATTERN.matcher(name)
            return if (matcher.find()) matcher.group(1) else null
        }

        /** Remove a trailing " <size> nozzle" suffix from a printer name, leaving its model portion. */
        @JvmStatic
        fun stripNozzleSuffix(name: String?): String? {
            if (name.isNullOrEmpty()) return name
            val matcher = NOZZLE_PATTERN.matcher(name)
            return if (matcher.find() && matcher.end() >= name.length - 1) {
                name.substring(0, matcher.start()).trim()
            } else name
        }

        /** The first nozzle diameter from a possibly multi-extruder nozzle_diameter value. */
        @JvmStatic
        fun firstNozzleDiameter(nozzleDiameter: String?): String? {
            if (nozzleDiameter.isNullOrEmpty()) return null
            return nozzleDiameter.split(Regex("[;,]"), limit = 0).first().trim()
        }

        /** Orca rejects slicing when layer height exceeds nozzle diameter. */
        @JvmStatic
        fun layerHeightFitsNozzle(layerHeight: String?, nozzleDiameter: String?): Boolean {
            if (layerHeight.isNullOrEmpty() || nozzleDiameter.isNullOrEmpty()) return true
            return try {
                layerHeight.toFloat() <= nozzleDiameter.toFloat() + 1e-4f
            } catch (_: NumberFormatException) {
                true
            }
        }

        /** Match a preset's line width to the active nozzle; zero means auto/derived. */
        @JvmStatic
        fun lineWidthFitsNozzle(lineWidth: String?, nozzleDiameter: String?): Boolean {
            if (lineWidth.isNullOrEmpty() || nozzleDiameter.isNullOrEmpty()) return true
            return try {
                val width = lineWidth.toFloat()
                val nozzle = nozzleDiameter.toFloat()
                if (width <= 0 || nozzle <= 0) true
                else width >= nozzle * 0.65f - 1e-4f && width <= nozzle * 1.3f + 1e-4f
            } catch (_: NumberFormatException) {
                true
            }
        }

        private fun nozzleEquals(a: String?, b: String?): Boolean {
            if (a == null || b == null) return false
            return try {
                kotlin.math.abs(a.toFloat() - b.toFloat()) < 1e-4f
            } catch (_: NumberFormatException) {
                a == b
            }
        }

        /** Check explicit preset compatibility, model siblings, conditions and declared nozzle size. */
        @JvmStatic
        fun isPrinterCompatible(
            presetName: String?,
            compatibleList: String?,
            condition: String?,
            activePrinterName: String?,
            activeModel: String?,
            activeNozzle: String?,
            checker: ConfigChecker?
        ): Boolean {
            val list = parseStringList(compatibleList)
            val printerOk = if (list.isEmpty()) {
                checker == null || checker.checkCompatibility(condition)
            } else {
                list.any { entry ->
                    entry == activePrinterName ||
                        (!activeModel.isNullOrEmpty() && stripNozzleSuffix(entry) == activeModel)
                }
            }
            if (!printerOk) return false
            if (!activeNozzle.isNullOrEmpty()) {
                val presetNozzle = extractNozzle(presetName)
                if (presetNozzle != null && !nozzleEquals(presetNozzle, activeNozzle)) return false
            }
            return true
        }

        /** Parse Orca's semicolon-separated ConfigOptionStrings representation. */
        @JvmStatic
        fun parseStringList(serialized: String?): List<String> {
            val out = ArrayList<String>()
            if (serialized.isNullOrEmpty()) return out

            val input = serialized
            val current = StringBuilder()
            var inQuotes = false
            var quoted = false
            var hasToken = false
            var index = 0
            while (index < input.length) {
                val character = input[index]
                if (inQuotes) {
                    if (character == '\\' && index + 1 < input.length) {
                        index++
                        current.append(input[index])
                    } else if (character == '"') {
                        inQuotes = false
                    } else {
                        current.append(character)
                    }
                } else if (character == '"') {
                    inQuotes = true
                    quoted = true
                    hasToken = true
                } else if (character == ';') {
                    out.add(if (quoted) current.toString() else current.toString().trim())
                    current.setLength(0)
                    quoted = false
                    hasToken = false
                } else {
                    current.append(character)
                    hasToken = true
                }
                index++
            }
            if (hasToken || current.isNotEmpty()) {
                out.add(if (quoted) current.toString() else current.toString().trim())
            }
            return out
        }

        /** Orca preset compatibility, preferring an explicit compatible-list when present. */
        @JvmStatic
        fun isCompatible(
            compatibleList: String?,
            condition: String?,
            activeName: String?,
            checker: ConfigChecker?
        ): Boolean {
            if (!compatibleList.isNullOrEmpty()) {
                return parseStringList(compatibleList).any { it == activeName }
            }
            return checker == null || checker.checkCompatibility(condition)
        }

        @JvmStatic
        fun calcViewNormalMatrix(viewMatrix: DoubleArray, worldMatrix: DoubleArray, normalMatrix: DoubleArray) {
            DebugUtils.assertTrue(viewMatrix.size == 16)
            DebugUtils.assertTrue(worldMatrix.size == 16)
            DebugUtils.assertTrue(normalMatrix.size == 12)
            Native.utils_calc_view_normal_matrix(viewMatrix, worldMatrix, normalMatrix)
        }

        @JvmStatic
        fun unproject(
            viewMatrix: DoubleArray,
            projectionMatrix: DoubleArray,
            screenWidth: Int,
            screenHeight: Int,
            x: Double,
            y: Double
        ): Vec3d {
            DebugUtils.assertTrue(viewMatrix.size == 16)
            DebugUtils.assertTrue(projectionMatrix.size == 16)
            val point = Native.utils_unproject(viewMatrix, projectionMatrix, screenWidth, screenHeight, x, y)
            return Vec3d(point[0], point[1], point[2])
        }
    }

    class ConfigChecker(config: String?) {
        private val pointer: Long = Native.utils_config_create(config ?: "")

        fun checkCompatibility(condition: String?): Boolean {
            if (condition.isNullOrEmpty()) return true
            return Native.utils_config_check_compatibility(pointer, condition)
        }

        @Throws(Slic3rRuntimeError::class)
        fun eval(condition: String?): String? = Native.utils_config_eval(pointer, condition ?: "")

        fun release() {
            Native.utils_config_release(pointer)
        }
    }
}
