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

    @Test
    fun everySelectableOrcaLanguageHasAReadableCatalog() {
        val assetDirectory = sequenceOf(
            File("src/main/assets/localization"),
            File("app/src/main/assets/localization"),
        ).first(File::isDirectory)

        assertEquals(23, OrcaLanguage.all.size)
        assertEquals(OrcaLanguage.all.size, OrcaLanguage.all.map { it.tag.lowercase() }.toSet().size)
        OrcaLanguage.all.forEach { language ->
            val catalog = File(assetDirectory, "${language.assetKey}.po")
            check(catalog.isFile && catalog.length() > 0L) {
                "Missing Orca localization catalog for ${language.tag}: ${catalog.path}"
            }
            val localization = catalog.inputStream().use(Slic3rLocalization::fromPo)
            check(localization.get("Printer").isNotBlank()) {
                "Orca localization catalog for ${language.tag} did not load"
            }
        }
        assertEquals("Deutsch", OrcaLanguage.byTag("de").nativeName)
        assertEquals("Português (Brasil)", OrcaLanguage.byTag("pt-BR").nativeName)
        assertEquals("简体中文", OrcaLanguage.byTag("zh-CN").nativeName)
        assertEquals("Deutsch", OrcaLanguage.byTag("unsupported").nativeName)
    }

    private fun parse(po: String): Slic3rLocalization = Slic3rLocalization.fromPo(
        ByteArrayInputStream(po.toByteArray(StandardCharsets.UTF_8))
    )
}
