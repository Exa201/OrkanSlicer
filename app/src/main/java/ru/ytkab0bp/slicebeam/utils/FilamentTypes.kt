package ru.ytkab0bp.slicebeam.utils

/** Filament material types, mirroring the engine's MaterialType database. Common ones listed first. */
object FilamentTypes {
    @JvmField
    val ALL = arrayOf(
        // Common
        "PLA", "PLA-CF", "PETG", "PETG-CF", "ABS", "ASA", "TPU", "PA", "PC", "PVA", "HIPS", "PVB",
        // Engineering / specialty
        "ABS-CF", "ABS-GF", "ASA-CF", "ASA-GF", "ASA-AERO", "PLA-AERO",
        "PA-CF", "PA-GF", "PA6", "PA6-CF", "PA6-GF", "PA11", "PA11-CF", "PA11-GF",
        "PA12", "PA12-CF", "PA12-GF", "PAHT", "PAHT-CF", "PAHT-GF",
        "PC-ABS", "PC-CF", "PC-PBT", "PCTG", "PET", "PET-CF", "PET-GF", "PETG-GF",
        "PP", "PP-CF", "PP-GF", "PPA-CF", "PPA-GF", "PPS", "PPS-CF", "PPSU", "PSU",
        "PE", "PE-CF", "PE-GF", "PCL", "PHA", "PI", "POM",
        "PEI-1010", "PEI-1010-CF", "PEI-1010-GF", "PEI-9085", "PEI-9085-CF", "PEI-9085-GF",
        "PEEK", "PEEK-CF", "PEEK-GF", "PEKK", "PEKK-CF", "PES",
        "PVDF", "SBS", "TPI", "BVOH", "CoPE", "EVA", "FLEX"
    )

    /**
     * Maps common printer-side aliases to the material family Orca uses for compatibility.
     * Keep specialty variants (for example CF/GF) distinct so an alias never makes an
     * incompatible spool look safe to assign.
     */
    @JvmStatic
    fun compatibilityFamily(type: String?): String? {
        val normalized = type?.trim()?.replace(Regex("\\s+"), " ")?.uppercase(java.util.Locale.ROOT)
            ?.takeIf { it.isNotEmpty() } ?: return null
        return when (normalized) {
            "PLA+", "PLA PRO" -> "PLA"
            "PLA MATTE", "PLA-MATTE" -> "PLA MATTE"
            "PLA SILK", "PLA-SILK" -> "PLA SILK"
            "PLA HIGH SPEED", "PLA-HS", "PLA HS" -> "PLA HIGH SPEED"
            "ABS+" -> "ABS"
            "PETG+", "PET" -> "PETG"
            "PETG HF", "PETG-HF" -> "PETG HF"
            "NYLON" -> "PA"
            "PPA" -> "PPA-CF"
            "COPE" -> "COPE"
            else -> normalized
        }
    }
}
