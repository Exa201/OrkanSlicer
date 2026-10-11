package ru.ytkab0bp.slicebeam.ui.settings

import java.util.Locale

/** Applies the beginner, standard, expert, favorites, and text-search rules for Orca options. */
object ProfileSettingsFilter {
    private val simpleKeys = setOf(
        "layer_height", "initial_layer_print_height", "wall_loops", "top_shell_layers",
        "bottom_shell_layers", "sparse_infill_density", "sparse_infill_pattern",
        "enable_support", "support_type", "support_style", "brim_type", "brim_width",
        "outer_wall_speed", "inner_wall_speed", "sparse_infill_speed", "initial_layer_speed",
    )
    private val expertMarkers = listOf(
        "gcode", "thumbnails", "filename_format", "compatible_", "notes", "fuzzy_skin",
        "wipe_tower", "flush_", "ooze_prevention", "skirt_height", "draft_shield",
    )

    /**
     * Returns whether an option matches the current screen filters. The localized search text is
     * assembled by the host so Orca metadata and translations remain owned by the existing layer.
     */
    @JvmStatic
    fun matches(
        key: String?,
        visibleInAdvanced: Boolean,
        hasDefinition: Boolean,
        isFavorite: Boolean,
        favoritesOnly: Boolean,
        settingsLevel: Int,
        query: String,
        searchableText: String,
    ): Boolean {
        val normalizedQuery = OrcaProfileGroupLocalizer.expandGermanSearch(
            query.trim().lowercase(Locale.ROOT),
        ).trim()

        if (!hasDefinition) {
            return (settingsLevel == EXPERT_LEVEL ||
                (settingsLevel >= STANDARD_LEVEL && visibleInAdvanced)) && normalizedQuery.isEmpty()
        }
        if (favoritesOnly && !isFavorite) return false
        if (settingsLevel == SIMPLE_LEVEL && (key == null || key !in simpleKeys)) return false
        if (settingsLevel == STANDARD_LEVEL && key != null && key != "flush_into_infill" && expertMarkers.any { marker -> key.contains(marker) }) return false
        if (normalizedQuery.isEmpty()) return true

        val searchable = searchableText.lowercase(Locale.ROOT)
        return normalizedQuery.split(WHITESPACE).filter { it.isNotBlank() }.all { searchable.contains(it) }
    }

    private const val SIMPLE_LEVEL = 0
    private const val STANDARD_LEVEL = 1
    private const val EXPERT_LEVEL = 2
    private val WHITESPACE = Regex("\\s+")
}
