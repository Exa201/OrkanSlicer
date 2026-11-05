package ru.ytkab0bp.slicebeam.ui.settings

/** Visibility rules for process settings whose usefulness depends on other profile values. */
object OrcaConditionalSettingsPolicy {
    @JvmStatic
    fun showSection(sectionTitle: String, showLightningOptions: Boolean): Boolean =
        sectionTitle != "Lightning infill" || showLightningOptions

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
}
