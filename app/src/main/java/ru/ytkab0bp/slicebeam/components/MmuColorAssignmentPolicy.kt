package ru.ytkab0bp.slicebeam.components

/** Suggestions never replace an explicit user choice or change model geometry. */
internal object MmuColorAssignmentPolicy {
    data class Slot(val gate: Int, val color: Int?, val material: String?)
    fun materialMismatch(source: String?, target: String?): Boolean =
        !source.isNullOrBlank() && !target.isNullOrBlank() && !source.trim().equals(target.trim(), true)

    fun suggest(color: Int?, material: String?, preferredGate: Int, slots: List<Slot>): Int {
        require(slots.isNotEmpty())
        val compatible = slots.filter { !material.isNullOrBlank() && it.material.equals(material, true) }
        val candidates = compatible.ifEmpty { slots }
        val colored = candidates.filter { color != null && it.color != null }
        if (colored.isNotEmpty()) return colored.minWith(compareBy<Slot> {
            distance(requireNotNull(color), requireNotNull(it.color))
        }.thenBy { if (it.gate == preferredGate) 0 else 1 }.thenBy { it.gate }).gate
        return candidates.firstOrNull { it.gate == preferredGate }?.gate ?: candidates.first().gate
    }

    private fun distance(a: Int, b: Int): Int = listOf(0, 8, 16).sumOf { shift ->
        val difference = ((a shr shift) and 255) - ((b shr shift) and 255)
        difference * difference
    }
}
