package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.*
import org.junit.Test

class ThreeMfValueCompatibilityTest {
    @Test fun knownBambuValuesHaveEquivalentOrcaMeanings() {
        assertEquals("rectilinear", ThreeMfValueCompatibility.normalize("internal_solid_infill_pattern", "zig-zag"))
        assertEquals("rectilinear", ThreeMfValueCompatibility.normalize("ironing_pattern", "zig-zag"))
        assertEquals("ensure_all", ThreeMfValueCompatibility.normalize("ensure_vertical_shell_thickness", "enabled"))
        assertEquals("none", ThreeMfValueCompatibility.normalize("ensure_vertical_shell_thickness", "disabled"))
        assertEquals("0", ThreeMfValueCompatibility.normalize("tree_support_wall_count", "-1"))
        assertEquals("-3", ThreeMfValueCompatibility.normalize("tree_support_wall_count", "-3"))
    }
    @Test fun pairedNozzleValuesResolveToTheLogicalFilamentRatherThanASecondColor() {
        val values = listOf("210", "220", "230", "240", "250", "260")
        val self = listOf("1", "1", "2", "2", "3", "3")
        val variants = List(3) { listOf("Standard", "High Flow") }.flatten()
        assertEquals(listOf("210", "230", "250"), (0..2).map {
            ThreeMfValueCompatibility.filamentValue(values, it, 3, self, variants, null)
        })
        assertEquals(listOf("220", "240", "260"), (0..2).map {
            ThreeMfValueCompatibility.filamentValue(values, it, 3, self, variants, "High Flow")
        })
    }
    @Test fun nozzlePairsCanBeGroupedWithoutSelfIndicesAndSingletonValuesRepeat() {
        assertEquals("230", ThreeMfValueCompatibility.filamentValue(listOf("210", "220", "230", "240"), 1, 2, emptyList(), emptyList(), null))
        assertEquals("1.75", ThreeMfValueCompatibility.filamentValue(listOf("1.75"), 2, 3, emptyList(), emptyList(), null))
        assertEquals("blue", ThreeMfValueCompatibility.filamentValue(listOf("gray", "purple", "blue"), 2, 3, emptyList(), emptyList(), null))
    }
}
