package ru.ytkab0bp.slicebeam.slic3r

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Test

class Slic3rLocalizationTest {
    @Test
    fun parsesGermanEntriesMultilineValuesAndEndOfFileEntry() {
        val localization = parse(
            """# German sample
msgid "Main Extruder"
msgstr "Hauptextruder"

msgid "Long "
"source"
msgstr "Mehrzeilig "
"übersetzt\nund vollständig"

msgid "No translation"
msgstr ""

msgid "Brand"
msgstr "PrusaSlicer und slic3r"

msgid "Escapes"
msgstr "Zitat: \"Hallo\"; Pfad C:\\Temp"

msgid "Last entry"
msgstr "Dateiende"
"""
        )

        assertEquals("Hauptextruder", localization.get("Main Extruder"))
        assertEquals("Mehrzeilig übersetzt\nund vollständig", localization.get("Long source"))
        assertEquals("No translation", localization.get("No translation"))
        assertEquals("OrkanSlicer und OrkanSlicer", localization.get("Brand"))
        assertEquals("Zitat: \"Hallo\"; Pfad C:\\Temp", localization.get("Escapes"))
        assertEquals("Dateiende", localization.get("Last entry"))
    }

    @Test
    fun missingKeysFallBackToTheSourceAndNormalizeLegacyProductNames() {
        val localization = parse("")

        assertEquals("Nicht übersetzt", localization.get("Nicht übersetzt"))
        assertEquals("OrkanSlicer", localization.get("PrusaSlicer"))
    }

    private fun parse(po: String): Slic3rLocalization = Slic3rLocalization.fromPo(
        ByteArrayInputStream(po.toByteArray(StandardCharsets.UTF_8))
    )
}
