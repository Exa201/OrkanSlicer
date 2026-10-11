package ru.ytkab0bp.slicebeam.config

/** Equivalent Bambu/legacy values in the option vocabulary used by our Orca core. */
object ThreeMfValueCompatibility {
    fun normalize(key: String, value: String): String = when {
        key in setOf("sparse_infill_pattern", "top_surface_pattern", "bottom_surface_pattern",
            "internal_solid_infill_pattern", "ironing_pattern", "support_ironing_pattern") && value == "zig-zag" -> "rectilinear"
        key in setOf("sparse_infill_pattern", "top_surface_pattern", "bottom_surface_pattern",
            "internal_solid_infill_pattern", "ironing_pattern", "support_ironing_pattern") && value == "monotoniclines" -> "monotonicline"
        key == "ensure_vertical_shell_thickness" && value in setOf("enabled", "1") -> "ensure_all"
        key == "ensure_vertical_shell_thickness" && value == "disabled" -> "none"
        key == "ensure_vertical_shell_thickness" && value == "0" -> "ensure_moderate"
        key == "tree_support_wall_count" && value == "-1" -> "0"
        else -> value
    }

    /** H2C stores several nozzle variants per logical filament, identified by self_index. */
    fun filamentValue(values: List<String>, index: Int, filamentCount: Int,
                      selfIndices: List<String>, variants: List<String>, preferredVariant: String?): String {
        if (values.size <= filamentCount) return values.getOrNull(index) ?: values.last()
        val candidates = if (selfIndices.size == values.size) selfIndices.indices.filter {
            selfIndices[it].toIntOrNull() == index + 1
        } else if (filamentCount > 0 && values.size % filamentCount == 0) {
            val stride = values.size / filamentCount
            (index * stride until (index + 1) * stride).toList()
        } else emptyList()
        val selected = candidates.firstOrNull { preferredVariant != null && variants.getOrNull(it) == preferredVariant }
            ?: candidates.firstOrNull() ?: index.coerceAtMost(values.lastIndex)
        return values[selected]
    }
}
