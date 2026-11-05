package ru.ytkab0bp.slicebeam.ui.settings

/** Visibility rules for process settings whose usefulness depends on other profile values. */
object OrcaConditionalSettingsPolicy {
    @JvmStatic
    fun showSection(
        sectionTitle: String,
        showLightningOptions: Boolean,
        showInfillAnchorOptions: Boolean = true,
    ): Boolean = when (sectionTitle) {
        "Lightning infill" -> showLightningOptions
        "Infill anchors" -> showInfillAnchorOptions
        else -> true
    }

    @JvmStatic
    fun showLightningOptions(sparseInfillDensity: String?, sparseInfillPattern: String?): Boolean {
        val density = sparseInfillDensity
            ?.trim()
            ?.removeSuffix("%")
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?: return false
        return density > 0.0 && sparseInfillPattern?.trim()?.equals("lightning", ignoreCase = true) == true
    }

    @JvmStatic
    fun showInfillAnchorOptions(sparseInfillDensity: String?, sparseInfillPattern: String?): Boolean {
        val density = sparseInfillDensity
            ?.trim()
            ?.removeSuffix("%")
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?: return false
        if (density <= 0.0) return false
        val pattern = sparseInfillPattern?.trim()?.lowercase() ?: return false
        return pattern != "concentric" && pattern != "spiralinset" &&
            pattern != "spiral_inset" && pattern != "spiral inset"
    }
}
