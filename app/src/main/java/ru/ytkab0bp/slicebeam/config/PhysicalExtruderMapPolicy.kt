package ru.ytkab0bp.slicebeam.config

/** Orca maps each logical filament slot to a zero-based physical toolhead. */
object PhysicalExtruderMapPolicy {
    fun identity(logicalExtruderCount: Int): String =
        (0 until logicalExtruderCount.coerceAtLeast(1)).joinToString(",") { it.toString() }

    /** Returns a mapping only when it has one valid physical-head index per logical slot. */
    fun parse(value: String, logicalExtruderCount: Int): IntArray? {
        if (logicalExtruderCount < 1) return null
        val entries = value.split(',').map(String::trim)
        if (entries.size != logicalExtruderCount) return null
        val mapping = entries.map { it.toIntOrNull() ?: return null }
        if (mapping.any { it !in 0 until logicalExtruderCount }) return null
        return mapping.toIntArray()
    }

    fun isValid(value: String, logicalExtruderCount: Int): Boolean =
        parse(value, logicalExtruderCount) != null
}
