package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.config.ThreeMfProjectSettings
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper

class QuickSliceProfileTransactionTest {
    @Test fun importedProjectQuickValuesStayTemporaryAndDoNotCreateASavedPreset() {
        val config = sampleConfig()
        val before = config.serialize()
        TemporaryProjectSettings.activate(ThreeMfProjectSettings("project.3mf", mapOf("layer_height" to "0.2"), emptyList(), emptyMap()))
        try {
            val result = QuickSliceProfileTransaction.apply(config,
                QuickSliceProfileTransaction.Values("0.24", "35", supports = true, brim = false),
            ) { error("Temporary project must never be persisted") }
            assertEquals(QuickSliceProfileTransaction.Result.SAVED, result)
            assertEquals(before, config.serialize())
            val effective = ConfigObject()
            TemporaryProjectSettings.applyTo(effective)
            assertEquals("0.24", effective["layer_height"])
            assertEquals("35%", effective["sparse_infill_density"])
            assertEquals("1", effective["enable_support"])
        } finally { TemporaryProjectSettings.clear() }
    }
    @Test
    fun successfulSaveCanBeReloadedWithAllQuickValues() {
        val config = sampleConfig()
        lateinit var savedText: String

        val result = QuickSliceProfileTransaction.apply(
            config,
            QuickSliceProfileTransaction.Values("0.16", "22", supports = true, brim = true),
        ) {
            savedText = config.serialize()
            true
        }

        assertEquals(QuickSliceProfileTransaction.Result.SAVED, result)
        val reloaded = Slic3rConfigWrapper(savedText.byteInputStream())
        assertEquals(QuickSliceProfileTransaction.PROFILE_NAME, reloaded.presets?.get("print"))
        val quick = reloaded.findPrint(QuickSliceProfileTransaction.PROFILE_NAME)
        assertEquals("0.16", quick?.get("layer_height"))
        assertEquals("22%", quick?.get("fill_density"))
        assertEquals("1", quick?.get("support_material"))
        assertEquals("5", quick?.get("brim_width"))
        assertEquals("outer_only", quick?.get("brim_type"))
    }

    @Test
    fun failedSaveRestoresExistingQuickProfileAndActivePrintSelection() {
        val config = sampleConfig().apply {
            printConfigs.add(ConfigObject(QuickSliceProfileTransaction.PROFILE_NAME).apply {
                profileListType = ConfigObject.PROFILE_LIST_PRINT
                put("layer_height", "0.12")
                put("fill_density", "10%")
            })
        }

        val result = QuickSliceProfileTransaction.apply(
            config,
            QuickSliceProfileTransaction.Values("0.24", "35", supports = false, brim = false),
        ) { false }

        assertEquals(QuickSliceProfileTransaction.Result.SAVE_FAILED, result)
        assertEquals("Balanced", config.presets?.get("print"))
        assertEquals(2, config.printConfigs.size)
        val quick = config.findPrint(QuickSliceProfileTransaction.PROFILE_NAME)
        assertEquals("0.12", quick?.get("layer_height"))
        assertEquals("10%", quick?.get("fill_density"))
    }

    @Test
    fun missingActivePrintProfileDoesNotMutateConfiguration() {
        val config = sampleConfig().apply { presets?.put("print", "missing") }
        val before = config.serialize()

        val result = QuickSliceProfileTransaction.apply(
            config,
            QuickSliceProfileTransaction.Values("0.24", "35", supports = false, brim = false),
        ) { error("Persistence must not be called without a base profile") }

        assertEquals(QuickSliceProfileTransaction.Result.PRINT_PROFILE_MISSING, result)
        assertEquals(before, config.serialize())
        assertFalse(config.printConfigs.any { it.title == QuickSliceProfileTransaction.PROFILE_NAME })
    }

    private fun sampleConfig() = Slic3rConfigWrapper().apply {
        printConfigs.add(ConfigObject("Balanced").apply {
            profileListType = ConfigObject.PROFILE_LIST_PRINT
            put("layer_height", "0.20")
            put("fill_density", "15%")
        })
        presets = ConfigObject().apply {
            put("print", "Balanced")
        }
    }
}
