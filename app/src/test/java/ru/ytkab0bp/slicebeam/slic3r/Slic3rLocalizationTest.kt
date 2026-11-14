package ru.ytkab0bp.slicebeam.slic3r

import java.io.ByteArrayInputStream
import java.io.File
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

    @Test
    fun parsesPluralSourceAndUsesTranslatedPluralFormForPluralLookups() {
        val localization = parse(
            """msgid "%1${'$'}d connector is out of cut contour"
msgid_plural "%1${'$'}d connectors are out of cut contour"
msgstr[0] "%1${'$'}d Verbinder ist außerhalb der Schnittkontur"
msgstr[1] "%1${'$'}d Verbinder sind außerhalb der Schnittkontur"

"""
        )

        assertEquals("%1${'$'}d Verbinder ist außerhalb der Schnittkontur", localization.get("%1${'$'}d connector is out of cut contour"))
        assertEquals("%1${'$'}d Verbinder sind außerhalb der Schnittkontur", localization.get("%1${'$'}d connectors are out of cut contour"))
    }

    @Test
    fun actualGermanCatalogResolvesOrcaPluralSourceMessages() {
        val fromModule = File("src/main/assets/localization/de.po")
        val catalog = if (fromModule.isFile) fromModule else File("app/src/main/assets/localization/de.po")
        val localization = catalog.inputStream().use(Slic3rLocalization::fromPo)

        assertEquals(
            "%1${'$'}d Verbinder sind außerhalb des Schnittkontur",
            localization.get("%1${'$'}d connectors are out of cut contour"),
        )
    }

    @Test
    fun actualGermanCatalogIncludesTheMobileBridgeWidthExplanation() {
        val fromModule = File("src/main/assets/localization/de.po")
        val catalog = if (fromModule.isFile) fromModule else File("app/src/main/assets/localization/de.po")
        val localization = catalog.inputStream().use(Slic3rLocalization::fromPo)

        assertEquals("Brückenlinienbreite", localization.get("Bridge line width"))
        assertEquals(
            "Brückenlinienbreite in mm oder Prozent des Düsendurchmessers. Höchstens so breit wie die Düse. Bei 0 wird die Breite der inneren Vollfüllung verwendet.",
            localization.get("Bridge line width in mm or % of the nozzle diameter. Maximum: nozzle diameter. 0 uses the internal solid infill width."),
        )
    }

    private fun parse(po: String): Slic3rLocalization = Slic3rLocalization.fromPo(
        ByteArrayInputStream(po.toByteArray(StandardCharsets.UTF_8))
    )
}
