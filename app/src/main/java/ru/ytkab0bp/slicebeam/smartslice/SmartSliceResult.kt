package ru.ytkab0bp.slicebeam.smartslice

import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.Locale

class SmartSliceResult internal constructor(@JvmField val geometry: GeometryAnalysis) {
    @JvmField val changes: LinkedHashMap<String, String> = LinkedHashMap()
    @JvmField val reasons: MutableList<String> = ArrayList()
    @JvmField val warnings: MutableList<String> = ArrayList()
    @JvmField var nozzleMm: Double = 0.0
    @JvmField var layerMm: Double = 0.0
    @JvmField var walls: Int = 0
    @JvmField var infillPercent: Int = 0
    @JvmField var infillPattern: String? = null
    @JvmField var support: String? = null
    @JvmField var seam: String? = null

    fun summary(): String {
        val out = StringBuilder()
        out.append(
            String.format(
                Locale.GERMANY,
                "Modell: %.1f × %.1f × %.1f mm · %d Objekt(e)\n",
                geometry.widthMm,
                geometry.depthMm,
                geometry.heightMm,
                geometry.objectCount
            )
        )
        out.append(String.format(Locale.GERMANY, "Düse: %.2f mm · Schichthöhe: %.2f mm\n", nozzleMm, layerMm))
        out.append("Wände: ").append(walls).append(" · Infill: ").append(infillPercent).append(" % ")
            .append(infillPattern).append('\n')
        out.append("Support: ").append(support).append(" · Naht: ").append(seam).append("\n\n")
        out.append("Warum diese Einstellungen?\n")
        for (reason in reasons) out.append("• ").append(reason).append('\n')
        if (warnings.isNotEmpty()) {
            out.append("\nHinweise\n")
            for (warning in warnings) out.append("• ").append(warning).append('\n')
        }
        out.append("\nSmart Slice passt ").append(changes.size).append(" Einstellungen nachvollziehbar an.")
        return out.toString()
    }

    fun changeSummary(before: Map<String, String>?): String {
        val out = StringBuilder()
        out.append("Konkrete Änderungen\n")
        if (changes.isEmpty()) {
            return out.append("Die aktuellen Orca-Einstellungen passen bereits zu dieser Auswahl.\n").toString()
        }
        for ((key, newValue) in changes) {
            val oldValue = before?.get(key)
            out.append(displayName(key)).append(": ")
                .append(if (oldValue == null) "–" else displayValue(key, oldValue))
                .append(" → ").append(displayValue(key, newValue)).append('\n')
        }
        return out.toString()
    }

    private companion object {
        fun displayName(key: String): String = when (key) {
            "layer_height" -> "Schichthöhe"
            "wall_loops" -> "Wandlinien"
            "sparse_infill_density" -> "Fülldichte"
            "sparse_infill_pattern" -> "Füllmuster"
            "wall_generator" -> "Wandgenerator"
            "detect_thin_wall" -> "Dünne Wände erkennen"
            "extra_perimeters_on_overhangs" -> "Zusätzliche Wände an Überhängen"
            "spiral_mode" -> "Spiralvasenmodus"
            "top_shell_layers" -> "Obere Schichten"
            "bottom_shell_layers" -> "Untere Schichten"
            "enable_support" -> "Stützen"
            "support_type" -> "Stütztyp"
            "support_on_build_plate_only" -> "Stützen nur vom Druckbett"
            "support_interface_top_layers" -> "Stütz-Kontaktschichten"
            "seam_position" -> "Nahtposition"
            "outer_wall_speed" -> "Außenwandgeschwindigkeit"
            "top_surface_speed" -> "Deckflächen-Geschwindigkeit"
            "brim_type" -> "Druckbettrand"
            "bridge_flow" -> "Brückenfluss"
            "bridge_speed" -> "Brückengeschwindigkeit"
            "elefant_foot_compensation" -> "Elefantenfußausgleich"
            else -> key.replace('_', ' ')
        }

        fun displayValue(key: String, value: String?): String {
            if (value == null) return "–"
            return when (key) {
                "layer_height", "elefant_foot_compensation" -> value.replace('.', ',') + " mm"
                "outer_wall_speed", "top_surface_speed", "bridge_speed" -> value + " mm/s"
                "sparse_infill_density" -> if (value.endsWith("%")) value else value + "%"
                "wall_loops", "top_shell_layers", "bottom_shell_layers", "support_interface_top_layers" -> value + " Schichten"
                "detect_thin_wall", "extra_perimeters_on_overhangs", "spiral_mode", "enable_support",
                "support_on_build_plate_only" -> if (value == "1") "Ein" else "Aus"
                "sparse_infill_pattern" -> when (value) {
                    "gyroid" -> "Gyroid"
                    "line" -> "Linien"
                    else -> value
                }
                "support_type" -> when {
                    value.contains("tree") -> "Tree (automatisch)"
                    value.contains("normal") -> "Standard (automatisch)"
                    else -> value
                }
                "seam_position" -> when (value) {
                    "back" -> "Rückseite"
                    "aligned" -> "Ausgerichtet"
                    else -> value
                }
                "brim_type" -> when (value) {
                    "auto_brim" -> "Automatisch"
                    "no_brim" -> "Keiner"
                    else -> value
                }
                else -> value
            }
        }
    }
}
