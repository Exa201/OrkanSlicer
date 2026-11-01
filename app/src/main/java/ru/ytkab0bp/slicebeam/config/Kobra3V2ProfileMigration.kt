package ru.ytkab0bp.slicebeam.config

/** Removes the PA command accidentally shipped in the stock Kobra 3 V2 start G-code. */
object Kobra3V2ProfileMigration {
    private val legacyPressureAdvanceLine = Regex(
        "(?im)^[\\t ]*M900[\\t ]+K0\\.051[\\t ]*(?:;[^\\r\\n]*)?(?:\\r?\\n|$)",
    )

    @JvmStatic
    fun removeLegacyPressureAdvance(startGcode: String): String =
        legacyPressureAdvanceLine.replace(startGcode, "")
}
