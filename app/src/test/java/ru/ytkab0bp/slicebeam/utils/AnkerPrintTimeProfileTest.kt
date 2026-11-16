package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AnkerPrintTimeProfileTest {
    private fun ankerPrinterProfiles(): List<Pair<String, Map<String, String>>> {
        val profile = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .map { File(it, "app/src/main/assets/orca_profiles/Anker.ini") }
            .firstOrNull(File::isFile)
        assertTrue("Bundled Anker profile asset must be available from the project checkout", profile != null)

        return requireNotNull(profile).readText()
            .split(Regex("(?=^\\[)", RegexOption.MULTILINE))
            .filter { it.startsWith("[printer:Anker ") }
            .map { section ->
                val name = section.lineSequence().first()
                val values = section.lineSequence()
                    .filter { '=' in it }
                    .associate { line ->
                        val (key, value) = line.split('=', limit = 2)
                        key.trim() to value.trim()
                    }
                name to values
            }
    }

    @Test
    fun everyBundledAnkerPrinterProfileReportsTotalPrintTime() {
        val profiles = ankerPrinterProfiles()
        assertEquals("Expected all bundled Anker M5, M5C and all-metal nozzle variants", 12, profiles.size)
        profiles.forEach { (name, values) ->
            val fileStartGcode = values["file_start_gcode"].orEmpty()
            assertTrue("$name must expose Orca's total print-time placeholder", fileStartGcode.contains("{print_time_total_sec}"))
            assertFalse("$name must not expose the incomplete print-time placeholder", fileStartGcode.contains("{print_time_sec}"))
        }
    }
}
