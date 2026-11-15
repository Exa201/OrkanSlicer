package ru.ytkab0bp.slicebeam.components.bed_menu

/** Search keeps original model indices so row actions and grouped selection target the real objects. */
internal object ModelObjectSearchPolicy {
    fun matchingIndices(rows: List<ModelObjectRow>, query: String): List<Int> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return rows.indices.toList()
        return rows.indices.filter { index ->
            val row = rows[index]
            row.name.contains(normalized, ignoreCase = true) ||
                row.material.contains(normalized, ignoreCase = true) ||
                row.parts.any { part ->
                    part.label.contains(normalized, ignoreCase = true) || part.material.contains(normalized, ignoreCase = true)
                }
        }
    }
}
