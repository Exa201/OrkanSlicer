package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class QidiProfileFilenameTest {
    @Test
    fun qidiFilenameUsesTheInitiallySelectedToolInsteadOfAlwaysT0() {
        val profile = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "app/src/main/assets/orca_profiles/Qidi.ini") }
            .firstOrNull(File::isFile)
        assertTrue("Qidi profile asset must be available from the project checkout", profile != null)

        val filenameFormats = requireNotNull(profile).readLines()
            .filter { it.startsWith("output_filename_format =") }
            .filter { it.contains("{filament_type[") }

        assertTrue("Qidi profile should include material-aware output filenames", filenameFormats.isNotEmpty())
        assertFalse(
            "Qidi filenames must not force material slot T0 when another tool starts the print",
            filenameFormats.any { it.contains("{filament_type[0]}") },
        )
        assertTrue(
            "Qidi material-aware filenames should use the initial active tool",
            filenameFormats.all { it.contains("{filament_type[initial_tool]}") },
        )
    }
}
