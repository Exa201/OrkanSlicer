package ru.ytkab0bp.slicebeam.slic3r

import ru.ytkab0bp.slicebeam.utils.Prefs

/** Mesh tessellation settings used only while importing STEP/STP geometry. */
data class StepImportOptions(
    val linearDeflection: Double = DEFAULT_LINEAR_DEFLECTION,
    val angularDeflection: Double = DEFAULT_ANGULAR_DEFLECTION,
    val splitCompound: Boolean = false,
) {
    fun validate(): String? = when {
        !linearDeflection.isFinite() || linearDeflection !in MIN_LINEAR_DEFLECTION..MAX_LINEAR_DEFLECTION ->
            "Die lineare Abweichung muss zwischen 0,0001 und 1 mm liegen."
        !angularDeflection.isFinite() || angularDeflection !in MIN_ANGULAR_DEFLECTION..MAX_ANGULAR_DEFLECTION ->
            "Die Winkelschrittweite muss zwischen 0,01 und 1,5 rad liegen."
        else -> null
    }

    companion object {
        const val DEFAULT_LINEAR_DEFLECTION = 0.003
        const val DEFAULT_ANGULAR_DEFLECTION = 0.5
        const val MIN_LINEAR_DEFLECTION = 0.0001
        const val MAX_LINEAR_DEFLECTION = 1.0
        const val MIN_ANGULAR_DEFLECTION = 0.01
        const val MAX_ANGULAR_DEFLECTION = 1.5

        private const val PREF_LINEAR = "step_linear_deflection"
        private const val PREF_ANGULAR = "step_angular_deflection"
        private const val PREF_SPLIT = "step_split_compound"

        @JvmStatic
        fun isStepFile(fileName: String): Boolean {
            val lower = fileName.lowercase(java.util.Locale.ROOT)
            return lower.endsWith(".step") || lower.endsWith(".stp")
        }

        @JvmStatic
        fun loadDefaults(): StepImportOptions {
            val prefs = Prefs.getPrefs()
            val candidate = StepImportOptions(
                prefs?.getString(PREF_LINEAR, null)?.replace(',', '.')?.toDoubleOrNull() ?: DEFAULT_LINEAR_DEFLECTION,
                prefs?.getString(PREF_ANGULAR, null)?.replace(',', '.')?.toDoubleOrNull() ?: DEFAULT_ANGULAR_DEFLECTION,
                prefs?.getBoolean(PREF_SPLIT, false) ?: false,
            )
            return if (candidate.validate() == null) candidate else StepImportOptions()
        }

        @JvmStatic
        fun saveDefaults(options: StepImportOptions) {
            require(options.validate() == null)
            Prefs.getPrefs()?.edit()
                ?.putString(PREF_LINEAR, options.linearDeflection.toString())
                ?.putString(PREF_ANGULAR, options.angularDeflection.toString())
                ?.putBoolean(PREF_SPLIT, options.splitCompound)
                ?.apply()
        }
    }
}
