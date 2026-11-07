package ru.ytkab0bp.slicebeam.ui.settings

import ru.ytkab0bp.slicebeam.slic3r.Slic3rLocalization

/** Shared German labels for profile tabs and collapsible groups. */
object OrcaProfileGroupLocalizer {
    private val germanEntries = listOf(
        "General" to "Allgemein",
        "Size and coordinates" to "Größe und Koordinaten",
        "Bed surface" to "Druckoberfläche",
        "Capabilities" to "Funktionen",
        "Custom G-code" to "Eigener G-Code",
        "Machine limits" to "Maschinengrenzen",
        "Physical Printer" to "Druckerverbindung",
        "Print Host upload" to "Druckserver und Upload",
        "Acceleration limits" to "Beschleunigungsgrenzen",
        "Maximum speeds" to "Maximale Geschwindigkeiten",
        "Jerk limits" to "Ruckgrenzen",
        "Minimum feedrates" to "Mindestvorschübe",
        "Layer height limits" to "Schichthöhenbegrenzung",
        "Position (for multi-extruder printers)" to "Position (Mehrfach-Extruder)",
        "Retraction for cutting (long retraction when cut)" to "Rückzug beim Filamentschnitt",
        "Retraction when tool is disabled (advanced settings for multi-extruder setups)" to "Rückzug bei inaktivem Werkzeug",
        "Filament Overrides" to "Filamentkorrekturen",
        "Dependencies" to "Profilkompatibilität",
        "Quality" to "Qualität",
        "Strength" to "Stabilität",
        "Layers and perimeters" to "Schichten und Wandungen",
        "Top/bottom layers" to "Obere / untere Schichten",
        "Skirt and brim" to "Haftung, Skirt und Brim",
        "Support material" to "Stützstrukturen",
        "Multiple Extruders" to "Mehrere Extruder",
        "Output options" to "Ausgabeoptionen",
        "Speed" to "Geschwindigkeit",
        "Support" to "Stützstrukturen",
        "Support flow" to "Stützfluss",
        "Cooling" to "Kühlung",
        "Multimaterial" to "Mehrmaterial",
        "Others" to "Weitere Einstellungen",
        "Filament profiles" to "Filamentprofile",
        "Layer height" to "Schichthöhe",
        "Line width" to "Linienbreite",
        "Seam" to "Naht",
        "Scarf joint seam" to "Schrägnaht",
        "Precision" to "Präzision",
        "Ironing" to "Glätten",
        "Wall generator" to "Wandgenerator",
        "Walls and surfaces" to "Wände und Oberflächen",
        "Overhangs" to "Überhänge",
        "Bridging" to "Brücken",
        "Walls" to "Wände",
        "Cooling and fan" to "Kühlung und Lüfter",
        "Infill" to "Füllung",
        "Flush into infill" to "Reinigung in der Füllung",
        "Lightning infill" to "Blitzfüllung",
        "Flow ratio" to "Flussverhältnis",
        "Wall flow ratio" to "Flussverhältnis der Wände",
        "Solid and infill flow ratio" to "Flussverhältnis der Füllung",
        "Internal bridges" to "Innere Brücken",
        "Advanced" to "Erweitert",
        "Acceleration" to "Beschleunigung",
        "Overhang speed" to "Überhanggeschwindigkeit",
        "Jerk (XY)" to "Ruck (XY)",
        "Junction deviation" to "Eckenabweichung",
        "Pressure advance" to "Druckvorschub",
        "Resonance avoidance" to "Resonanzvermeidung",
        "Raft" to "Raft",
        "Tree supports" to "Baumstützen",
        "Adhesion · skirt" to "Haftung · Skirt",
        "Adhesion · brim" to "Haftung · Brim",
        "Brim" to "Haftkante (Brim)",
        "Filament & temperature" to "Filament & Temperatur",
        "Support filament" to "Stützfilament",
        "Support ironing" to "Stützen glätten",
        "Ooze prevention" to "Fadenbildung vermeiden",
        "Prime tower" to "Reinigungsturm",
        "Flushing" to "Reinigung",
        "Filament for Features" to "Filament für Druckbereiche",
        "Beam interlocking" to "Mehrmaterial-Verzahnung",
        "Skirt" to "Skirt / Umrandung",
        "Fuzzy Skin" to "Raue Oberfläche",
        "Special mode" to "Sondermodus",
        "G-code output" to "G-Code-Ausgabe",
        "Notes" to "Notizen",
        "Profile dependencies" to "Profilkompatibilität",
        "Filament" to "Filament",
        "Temperature" to "Temperatur",
        "Smooth cool plate" to "Glatte kühle Druckplatte",
        "Engineering plate" to "Engineering-Druckplatte",
        "High temperature plate" to "Hochtemperatur-Druckplatte",
        "Textured PEI plate" to "Strukturierte PEI-Druckplatte",
        "Textured cool plate" to "Strukturierte kühle Druckplatte",
        "SuperTack plate" to "SuperTack-Druckplatte",
        "Nozzle" to "Düse",
        "Chamber" to "Druckkammer",
        "Enable" to "Aktivierung",
        "Fan speed" to "Lüftergeschwindigkeit",
        "Additional / auxiliary fan" to "Zusätzlicher Lüfter",
        "Fan kickstart / speedup" to "Lüfterstart",
        "Air filtration" to "Luftfilter",
        "Dynamic fan speeds" to "Dynamische Lüftersteuerung",
        "Cooling thresholds" to "Kühlgrenzen",
        "Filament properties" to "Filamenteigenschaften",
        "Filament change" to "Filamentwechsel",
        "Print speed override" to "Druckgeschwindigkeitskorrektur",
        "Shrinkage compensation" to "Schrumpfungsausgleich",
        "Wipe tower parameters" to "Reinigungsturm-Parameter",
        "Toolchange parameters with single extruder MM printers" to "Werkzeugwechsel bei Single-Extruder-MM-Druckern",
        "Toolchange parameters with multi extruder MM printers" to "Werkzeugwechsel bei Mehrfach-Extruder-MM-Druckern",
        "Travel lift" to "Z-Hub bei Bewegung",
        "Retraction" to "Rückzug",
        "Retraction when tool is disabled" to "Rückzug bei inaktivem Werkzeug",
        "Start G-code" to "Start-G-Code",
        "End G-code" to "End-G-Code",
    )
    private val germanLabels = germanEntries.toMap()

    /** Keeps the historical screen behavior: English headings stay as-is, German uses a shared map. */
    @JvmStatic
    fun mobileGroup(value: String?, language: String): String {
        if (value == null) return ""
        if (!language.equals("de", ignoreCase = true)) return value
        return germanLabels[value] ?: Slic3rLocalization.getString(value)
    }

    /** Filament groups use Orca translations in other languages and keep unknown German labels raw. */
    @JvmStatic
    fun filamentGroup(value: String, language: String): String {
        if (!language.equals("de", ignoreCase = true)) return Slic3rLocalization.getString(value)
        return germanLabels[value] ?: value
    }

    @JvmStatic
    fun expandGermanSearch(query: String): String = query
        .replace("reinigung in der füllung", "flush into infill")
        .replace("reinigung in der fuellung", "flush into infill")
        .replace("schichthöhe", "layer height")
        .replace("schichthoehe", "layer height")
        .replace("erste schicht", "initial layer")
        .replace("stütz", "support")
        .replace("stuetz", "support")
        .replace("füll", "infill")
        .replace("fuell", "infill")
        .replace("reinigung", "flush")
        .replace("wand", "wall")
        .replace("naht", "seam")
        .replace("geschwindigkeit", "speed")
        .replace("temperatur", "temperature")
        .replace("kühl", "cooling")
        .replace("kuehl", "cooling")
        .replace("rückzug", "retract")
        .replace("rueckzug", "retract")

    @JvmStatic
    fun hasUniqueGermanKeys(): Boolean = germanEntries.size == germanLabels.size

    @JvmStatic
    fun germanKeys(): List<String> = germanEntries.map { it.first }
}
