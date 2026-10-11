package ru.ytkab0bp.slicebeam

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ThreeMfProjectSettingsReader
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.ui.settings.OrcaFilamentSettingsCatalog
import ru.ytkab0bp.slicebeam.ui.settings.OrcaProcessSettingsCatalog
import java.io.File

/** A generated 3MF fixture keeps this import regression runnable without a user's model. */
@RunWith(AndroidJUnit4::class)
class TwoColorProjectDeviceTest {
    @Test fun importsBothObjectsAndFilamentsWithoutPersistingProjectValues() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        val file = File(context.cacheDir, "two-color-project.3mf")
        InstrumentationRegistry.getInstrumentation().context.assets.open("models/two-color-project.3mf").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val savedConfig = SliceBeam.getConfigFile().takeIf { it.exists() }?.readBytes()
        val previousConfig = SliceBeam.CONFIG
        var model: Model? = null
        try {
            val processKeys = Slic3rConfigWrapper.PRINT_CONFIG_KEYS.toSet() +
                OrcaProcessSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys }
            val filamentKeys = Slic3rConfigWrapper.FILAMENT_CONFIG_KEYS.toSet() +
                OrcaFilamentSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys }
            val settings = ThreeMfProjectSettingsReader.read(
                file, PrintConfigDef.getInstance().options, processKeys, filamentKeys,
            )
            assertEquals(2, settings.filaments.size)
            assertTrue(settings.invalidValues.isEmpty())
            model = Model(file, 1)
            assertEquals(2, model.objectsCount)
            assertEquals(2, model.requiredFilamentCount)
            assertArrayEquals(intArrayOf(1), model.getUsedFilaments(0))
            assertArrayEquals(intArrayOf(2), model.getUsedFilaments(1))
            TemporaryProjectSettings.activate(settings)
            assertEquals(2, TemporaryProjectSettings.slots()?.size)
            assertTrue("3MF printer profile must not replace the active one", SliceBeam.CONFIG === previousConfig)
            assertArrayEquals(savedConfig, SliceBeam.getConfigFile().takeIf { it.exists() }?.readBytes())
        } finally {
            TemporaryProjectSettings.clear()
            model?.release()
            file.delete()
        }
    }
}
