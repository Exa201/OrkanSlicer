package ru.ytkab0bp.slicebeam.utils

/** Pure assignment rules for keeping slicer tools on distinct physical ACE gates. */
object MmuGateAssignmentPolicy {
    /** Resolves logical slicer tools to their chosen physical gates, with firmware defaults. */
    @JvmStatic
    fun gatesForTools(activeTools: IntArray, selectedGates: IntArray?, defaultGates: IntArray): IntArray =
        IntArray(activeTools.size) { index ->
            val tool = activeTools[index]
            selectedGates?.getOrNull(tool) ?: defaultGates.getOrNull(tool) ?: -1
        }

    /**
     * Keeps the requested gate on [tool], moves any previous occupant to the tool's former
     * gate, then repairs remaining duplicates when enough gates are available.
     */
    @JvmStatic
    fun selectGate(mapping: IntArray, tool: Int, gate: Int, availableGates: IntArray): IntArray {
        require(tool in mapping.indices) { "Unknown slicer tool T$tool" }
        require(gate in availableGates) { "ACE gate ${gate + 1} is unavailable" }
        val result = mapping.copyOf()
        val previousGate = result[tool]
        result[tool] = gate
        result.indices.firstOrNull { it != tool && result[it] == gate }?.let { occupant ->
            result[occupant] = previousGate
        }
        return normalize(result, availableGates, tool)
    }

    /** Repairs duplicate or stale mappings without changing a valid selected tool's gate. */
    @JvmStatic
    fun normalize(mapping: IntArray, availableGates: IntArray, preferredTool: Int = -1): IntArray {
        if (availableGates.isEmpty()) return mapping.copyOf()
        val result = mapping.copyOf()
        val order = buildList {
            if (preferredTool in result.indices) add(preferredTool)
            result.indices.filter { it != preferredTool }.forEach(::add)
        }
        val claimed = HashSet<Int>()
        for (index in order) {
            val current = result[index]
            if (current in availableGates && claimed.add(current)) continue
            val replacement = availableGates.firstOrNull { it !in claimed }
            if (replacement != null) {
                result[index] = replacement
                claimed.add(replacement)
            } else if (current !in availableGates) {
                result[index] = availableGates.first()
            }
        }
        return result
    }

    @JvmStatic
    fun firstSharedGateTool(activeTools: IntArray, toolToGate: IntArray): Int? {
        val seen = HashSet<Int>()
        return activeTools.firstOrNull { tool ->
            val gate = toolToGate.getOrNull(tool) ?: return@firstOrNull true
            !seen.add(gate)
        }
    }

    /** Returns visual filament colors in slicer-tool order, falling back for unknown gates. */
    @JvmStatic
    fun colorsForGates(
        sourceColors: List<Int>,
        gates: IntArray?,
        physicalGateColors: Map<Int, Int>,
        toolCount: Int,
    ): List<Int> = List(toolCount.coerceAtLeast(0)) { tool ->
        val source = sourceColors.getOrElse(tool) { sourceColors.lastOrNull() ?: 0xFF19C8A0.toInt() }
        gates?.getOrNull(tool)?.let(physicalGateColors::get) ?: source
    }
}
