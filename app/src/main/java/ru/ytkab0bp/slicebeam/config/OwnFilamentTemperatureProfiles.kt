package ru.ytkab0bp.slicebeam.config

/** Resolve an own preset by material before using its temperatures for an imported 3MF tool. */
object OwnFilamentTemperatureProfiles {
    fun choose(profiles: List<ConfigObject>, assignedTitle: String?, currentTitle: String?, material: String?): ConfigObject? {
        val expected = material?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return null
        fun matches(profile: ConfigObject): Boolean = profile["filament_type"]?.trim()?.uppercase() == expected
        return profiles.firstOrNull { it.title == assignedTitle && matches(it) }
            ?: profiles.firstOrNull { it.title == currentTitle && matches(it) }
            ?: profiles.firstOrNull(::matches)
    }

    fun isTemperatureKey(key: String): Boolean = key.contains("temperature") || key.endsWith("_temp") ||
        key.contains("_temp_")
}
