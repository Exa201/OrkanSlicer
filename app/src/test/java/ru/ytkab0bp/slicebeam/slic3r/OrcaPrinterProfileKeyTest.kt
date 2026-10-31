package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertTrue
import org.junit.Test

class OrcaPrinterProfileKeyTest {
    @Test
    fun physicalExtruderMapIsIncludedInPrinterProfilePersistenceAndImport() {
        assertTrue(
            "Orca physical extruder mapping must survive printer-profile save/import",
            "physical_extruder_map" in Slic3rConfigWrapper.PRINTER_CONFIG_KEYS,
        )
    }
}
