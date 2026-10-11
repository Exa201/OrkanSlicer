package ru.ytkab0bp.slicebeam.fragment

/** Card boundaries used by the Compose profile list to group category rows and their settings. */
object ProfileSettingsRowSections {
    data class Decoration(
        val startsSection: Boolean,
        val endsSection: Boolean,
        val accentColor: Int,
    )

    @JvmStatic
    fun decorate(rows: List<ProfileOptionWrapper>): List<Decoration> {
        if (rows.isEmpty()) return emptyList()
        val result = MutableList(rows.size) { Decoration(false, false, 0) }
        var sectionStart = 0
        for (index in 1..rows.size) {
            val nextStartsSection = index < rows.size && isSectionHeader(rows[index])
            if (index == rows.size || nextStartsSection) {
                val accentColor = rows[sectionStart].color
                for (rowIndex in sectionStart..index - 1) {
                    result[rowIndex] = Decoration(
                        startsSection = rowIndex == sectionStart,
                        endsSection = rowIndex == index - 1,
                        accentColor = accentColor,
                    )
                }
                sectionStart = index
            }
        }
        return result
    }

    private fun isSectionHeader(row: ProfileOptionWrapper): Boolean =
        row.title != null || row.hasSpecialType()
}
