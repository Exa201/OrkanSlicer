package ru.ytkab0bp.slicebeam.utils

import java.util.regex.Pattern

/** Small deterministic helpers for routing slicer tools to physical MMU tools. */
object GCodeToolMapper {
    private val TOOL_LINE = Pattern.compile("^(T)([0-9]{1,2})(\\s*(?:;.*)?)$")

    /** Rewrite a stand-alone G-code T command; unrelated lines are returned unchanged. */
    @JvmStatic
    fun remapToolLine(line: String, outputTools: IntArray): String {
        val matcher = TOOL_LINE.matcher(line)
        if (!matcher.matches()) return line
        val inputTool = matcher.group(2).toInt()
        if (inputTool >= outputTools.size) {
            throw IllegalArgumentException("No ACE slot selected for T$inputTool")
        }
        return matcher.group(1) + outputTools[inputTool] + matcher.group(3)
    }

    /** Return the tool number for a stand-alone T command, or -1 for an unrelated line. */
    @JvmStatic
    fun parseToolCommand(line: String): Int {
        val end = toolCommandEnd(line)
        if (end < 0) return -1
        var digitsEnd = 1
        while (digitsEnd < line.length && Character.isDigit(line[digitsEnd])) digitsEnd++
        return line.substring(1, digitsEnd).toInt()
    }

    /** Fast check for Orca's per-filament comment without compiling a regex for every G-code line. */
    @JvmStatic
    fun isFilamentMapLine(line: String): Boolean {
        var i = 0
        while (i < line.length && Character.isWhitespace(line[i])) i++
        if (i >= line.length || line[i++] != ';') return false
        while (i < line.length && Character.isWhitespace(line[i])) i++
        if (!line.regionMatches(i, "filament", 0, 8, ignoreCase = true)) return false
        i += 8
        while (i < line.length && Character.isWhitespace(line[i])) i++
        return i < line.length && line[i] == ':'
    }

    private fun toolCommandEnd(line: String): Int {
        if (line.length < 2 || (line[0] != 'T' && line[0] != 't') || !Character.isDigit(line[1])) return -1
        var end = 2
        if (end < line.length && Character.isDigit(line[end])) end++
        while (end < line.length && Character.isWhitespace(line[end])) end++
        if (end < line.length && line[end] != ';') return -1
        return end
    }

    @JvmStatic
    fun formatGateList(zeroBasedGates: IntArray): String {
        val value = StringBuilder()
        for (gate in zeroBasedGates) {
            if (value.isNotEmpty()) value.append(',')
            value.append(gate + 1)
        }
        return value.toString()
    }
}
