package ru.ytkab0bp.slicebeam.utils

/** Maps slicer tools through the selected physical gate and Klipper's tool-to-gate map. */
object AnycubicAceRoutingPolicy {
    data class Routing(
        val toolCommands: IntArray,
        val gates: IntArray,
        val unavailableTool: Int?,
        val sharedGateTool: Int? = null,
    )

    @JvmStatic
    @JvmOverloads
    fun resolve(
        toolCount: Int,
        usedTools: IntArray,
        loadedGates: IntArray,
        toolToGate: IntArray,
        selectedGates: IntArray? = null,
    ): Routing {
        val count = toolCount.coerceAtLeast(1)
        val validLoadedGates = loadedGates.filter { gate -> toolToGate.indexOf(gate) >= 0 }.distinct()
        val fallbackGate = validLoadedGates.firstOrNull() ?: -1
        val requestedGates = IntArray(count) { tool ->
            selectedGates?.getOrNull(tool) ?: toolToGate.getOrNull(tool) ?: -1
        }
        val gates = IntArray(count) { tool ->
            val requested = requestedGates[tool]
            if (toolToGate.indexOf(requested) >= 0) requested else fallbackGate
        }
        val commands = IntArray(count) { tool -> toolToGate.indexOf(gates[tool]) }
        val unavailable = usedTools.firstOrNull { tool ->
            tool !in 0 until count || (requestedGates.getOrNull(tool) ?: -1) !in loadedGates ||
                commands.getOrNull(tool) == -1
        }
        val shared = MmuGateAssignmentPolicy.firstSharedGateTool(usedTools, gates)
        return Routing(commands, gates, unavailable, shared)
    }
}
