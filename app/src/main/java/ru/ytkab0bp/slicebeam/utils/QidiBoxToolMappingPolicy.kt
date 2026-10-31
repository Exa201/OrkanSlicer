package ru.ytkab0bp.slicebeam.utils

import java.security.MessageDigest
import java.util.Locale

/** Safe adaptation of Orca 2.5's Qidi-agent tool-to-Box SAVE_VARIABLE mapping. */
object QidiBoxToolMappingPolicy {
    const val SLOT_COUNT = 16
    const val TOOL_COUNT = 16

    fun defaults(toolCount: Int): IntArray = IntArray(toolCount.coerceIn(0, TOOL_COUNT)) { UNMAPPED }

    fun encode(mapping: IntArray): String = mapping.take(TOOL_COUNT)
        .map { if (it in 0 until SLOT_COUNT) it else UNMAPPED }
        .joinToString(",")

    fun decode(value: String?, toolCount: Int): IntArray {
        val count = toolCount.coerceIn(0, TOOL_COUNT)
        val result = defaults(count)
        if (value.isNullOrBlank()) return result
        value.split(',').take(count).forEachIndexed { index, token ->
            val slot = token.toIntOrNull() ?: UNMAPPED
            result[index] = if (slot in 0 until SLOT_COUNT) slot else UNMAPPED
        }
        return result
    }

    /** Empty means keep the printer's existing variable; repeated physical slots are allowed. */
    fun buildSaveVariablesScript(mapping: IntArray, activeTools: IntArray): String {
        val tools = activeTools.distinct().sorted()
        require(tools.all { it in 0 until TOOL_COUNT }) { "Qidi Box mapping supports tools T0–T15." }
        return tools.mapNotNull { tool ->
            val slot = mapping.getOrNull(tool) ?: UNMAPPED
            if (slot == UNMAPPED) null
            else {
                require(slot in 0 until SLOT_COUNT) { "Qidi Box slots must be in the range 0–15." }
                "SAVE_VARIABLE VARIABLE=value_t$tool VALUE=\"'slot$slot'\""
            }
        }.joinToString("\n")
    }

    /** A printer's mapping must not leak into another printer or profile using a different host. */
    fun preferenceKey(printerTitle: String, host: String): String {
        val identity = "${printerTitle.trim().lowercase(Locale.ROOT)}|${host.trim().trimEnd('/').lowercase(Locale.ROOT)}"
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(Locale.ROOT, it) }
        return "qidi_box_tool_mapping_$hex"
    }

    private const val UNMAPPED = -1
}
