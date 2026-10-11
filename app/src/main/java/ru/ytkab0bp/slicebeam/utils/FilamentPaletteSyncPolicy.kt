package ru.ytkab0bp.slicebeam.utils

/** Automatic refresh preserves configured tools; an explicit sync adopts the available printer slots. */
object FilamentPaletteSyncPolicy {
    data class SyncedPalette(val slots: List<FilamentSlot>, val gates: IntArray)

    fun replaceFromPrinter(
        printerGates: List<FilamentSlot?>,
        toolToGate: IntArray,
    ): SyncedPalette {
        val gates = toolToGate.take(Prefs.MAX_FILAMENT_COLORS)
            .distinct().filter { printerGates.getOrNull(it) != null }.sorted()
        require(gates.isNotEmpty()) { "No occupied filament slots with material and color are available" }
        return SyncedPalette(gates.map { requireNotNull(printerGates[it]) }, gates.toIntArray())
    }

    @JvmStatic
    fun updateExisting(
        configured: List<FilamentSlot>,
        printerTools: List<FilamentSlot?>
    ): List<FilamentSlot> {
        val result = ArrayList(configured)
        val count = minOf(result.size, printerTools.size)
        for (tool in 0 until count) {
            val live = printerTools[tool]
            if (live != null) result[tool] = live
        }
        return result
    }
}
