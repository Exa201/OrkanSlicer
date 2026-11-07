package ru.ytkab0bp.slicebeam.config

/** Removes the PA command accidentally shipped in the stock Kobra 3 V2 start G-code. */
object Kobra3V2ProfileMigration {
    private val m900Line = Regex("(?im)^([\\t ]*M900\\b[^\\r\\n]*)(?:\\r?\\n|$)")
    private val legacyK = Regex("(?i)(?:^|[\\t ])K[\\t ]*0?\\.0510*(?=[\\t ;]|$)")
    private val toolIndex = Regex("(?i)(?:^|[\\t ])T[\\t ]*(\\d+)(?=[\\t ;]|$)")

    private const val STOCK_PROFILE_TITLE = "Anycubic Kobra 3 v2 0.4 nozzle - Stock"

    /** Stored profiles can be renamed, so identify V2 variants by their machine model as well. */
    @JvmStatic
    fun isKobra3V2Profile(title: String?, printerModel: String?): Boolean =
        title.equals(STOCK_PROFILE_TITLE, ignoreCase = true) ||
            printerModel?.contains("Kobra 3 V2", ignoreCase = true) == true

    @JvmStatic
    fun removeLegacyPressureAdvance(startGcode: String): String =
        m900Line.replace(startGcode) { match ->
            val command = match.groupValues[1]
            val targetTool = toolIndex.find(command)?.groupValues?.get(1)?.toIntOrNull()
            if (legacyK.containsMatchIn(command) && (targetTool == null || targetTool == 0)) "" else match.value
        }
}
