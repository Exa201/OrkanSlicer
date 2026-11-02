package ru.ytkab0bp.slicebeam.utils

/** Automatic refresh preserves configured tools; an explicit sync keeps printer slot indices stable. */
object FilamentPaletteSyncPolicy {
    data class SyncedPalette(val slots: List<FilamentSlot>, val gates: IntArray)

    private const val EMPTY_SLOT_COLOR = 0xFF808080.toInt()

    fun replaceFromPrinter(
        printerGates: List<FilamentSlot?>,
        toolToGate: IntArray,
        configuredSlots: List<FilamentSlot> = emptyList(),
    ): SyncedPalette {
        val mappings = toolToGate.take(Prefs.MAX_FILAMENT_COLORS)
        require(mappings.isNotEmpty()) { "No printer tool-to-slot mappings are available" }
        require(mappings.indices.all { mappings[it] == it }) {
            "Printer maps tools to different ACE slots; OrkanSlicer keeps T0→slot 1, T1→slot 2, and so on"
        }
        val slotCount = minOf(printerGates.size, mappings.size)
        require((0 until slotCount).any { gate ->
            val printer = printerGates[gate]
            printer != null && (!printer.unknownLoadedSpool || configuredSlots.getOrNull(gate)?.type?.isNotBlank() == true)
        }) {
            "No occupied filament slots with material and color are available"
        }

        // Keep empty physical positions in the list. A placeholder carries no material and is
        // rejected by the existing upload preflight if its matching T tool is used.
        val slots = List(slotCount) { gate ->
            val printer = printerGates[gate]
            when {
                printer == null -> FilamentSlot(EMPTY_SLOT_COLOR, null)
                printer.unknownLoadedSpool -> configuredSlots.getOrNull(gate)
                    ?.takeIf { !it.type.isNullOrBlank() }
                    ?: FilamentSlot(printer.color, null)
                else -> printer
            }
        }
        return SyncedPalette(slots, IntArray(slotCount) { it })
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
            if (live != null && !live.unknownLoadedSpool) result[tool] = live
        }
        return result
    }
}
