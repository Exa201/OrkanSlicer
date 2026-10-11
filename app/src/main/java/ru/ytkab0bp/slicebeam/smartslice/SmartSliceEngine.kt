package ru.ytkab0bp.slicebeam.smartslice

import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef
import java.util.Locale

/** Deterministic rules layered over Orca profiles. It does not replace slicing or validation. */
object SmartSliceEngine {
    @JvmStatic
    fun analyze(model: Model?, intent: SmartSliceIntent): SmartSliceResult =
        analyze(GeometryAnalysis.analyze(model), intent, SliceBeam.buildCurrentConfigObject())

    /** Runs the rule pass from immutable geometry and profile snapshots away from the UI and GL threads. */
    @JvmStatic
    fun analyze(g: GeometryAnalysis, intent: SmartSliceIntent, cfg: ConfigObject): SmartSliceResult {
        val result = SmartSliceResult(g)
        result.nozzleMm = number(first(cfg.get("nozzle_diameter")), 0.4)
        val material = value(cfg.get("filament_type"), "PLA").uppercase(Locale.ROOT)

        var layer = intent.requestedLayerMm ?: when (intent.priority) {
            SmartSliceIntent.Priority.QUALITY -> result.nozzleMm * 0.30
            SmartSliceIntent.Priority.FAST -> result.nozzleMm * 0.65
            else -> result.nozzleMm * 0.50
        }
        if (intent.purpose == SmartSliceIntent.Purpose.MINIATURE || g.verySmall) {
            layer = Math.min(layer, result.nozzleMm * 0.30)
        }
        layer = clamp(layer, Math.max(0.06, result.nozzleMm * 0.20), result.nozzleMm * 0.75)
        result.layerMm = round(layer, 2)
        result.reasons.add(
            String.format(
                Locale.GERMANY,
                "Die Schichthöhe %.2f mm berücksichtigt die Düse mit %.2f mm und die gewählte Priorität %s.",
                result.layerMm,
                result.nozzleMm,
                priorityLabel(intent.priority),
            ),
        )

        var lineWidth = number(cfg.get("line_width"), result.nozzleMm * 1.05)
        if (lineWidth <= 0.0) lineWidth = result.nozzleMm * 1.05
        val requestedWall = intent.requestedWallMm ?: if (
            intent.purpose == SmartSliceIntent.Purpose.FUNCTIONAL ||
            intent.purpose == SmartSliceIntent.Purpose.MECHANICAL
        ) {
            result.nozzleMm * 5.2
        } else {
            result.nozzleMm * 3.2
        }
        var walls = Math.ceil(requestedWall / lineWidth).toInt()
        if (intent.priority == SmartSliceIntent.Priority.STRENGTH) walls++
        result.walls = clamp(
            walls.toDouble(),
            if (intent.purpose == SmartSliceIntent.Purpose.VASE) 1.0 else 2.0,
            8.0,
        ).toInt()

        when {
            intent.purpose == SmartSliceIntent.Purpose.VASE -> {
                result.infillPercent = 0
                result.infillPattern = "line"
                result.walls = 1
                put(result, "spiral_mode", "1")
                put(result, "top_shell_layers", "0")
                result.reasons.add("Der Vasenmodus erzeugt eine durchgehende Außenkontur ohne Infill und Deckschichten.")
            }
            intent.purpose == SmartSliceIntent.Purpose.FUNCTIONAL ||
                intent.purpose == SmartSliceIntent.Purpose.MECHANICAL ||
                intent.priority == SmartSliceIntent.Priority.STRENGTH -> {
                result.infillPercent = if (intent.priority == SmartSliceIntent.Priority.STRENGTH) 28 else 22
                result.infillPattern = "gyroid"
                result.reasons.add("Zusätzliche Perimeter tragen Lasten meist effizienter als extrem hohes Infill.")
            }
            intent.purpose == SmartSliceIntent.Purpose.MINIATURE -> {
                result.infillPercent = 10
                result.infillPattern = "gyroid"
                result.reasons.add("Feine Layer und langsame Außenwände priorisieren sichtbare Details.")
            }
            else -> {
                result.infillPercent = if (intent.priority == SmartSliceIntent.Priority.FAST) 10 else 15
                result.infillPattern = "gyroid"
                result.reasons.add("Moderates Infill begrenzt Zeit und Material, während die Wände die Außenfläche tragen.")
            }
        }
        result.reasons.add(
            String.format(
                Locale.GERMANY,
                "%d Wandlinien ergeben bei etwa %.2f mm Linienbreite rund %.2f mm Wandstärke.",
                result.walls,
                lineWidth,
                result.walls * lineWidth,
            ),
        )

        put(result, "layer_height", decimal(result.layerMm))
        put(result, "wall_loops", result.walls.toString())
        put(result, "sparse_infill_density", "${result.infillPercent}%")
        put(result, "sparse_infill_pattern", result.infillPattern!!)
        put(result, "wall_generator", "arachne")
        put(result, "detect_thin_wall", "1")
        put(result, "extra_perimeters_on_overhangs", "1")

        val visual = intent.purpose == SmartSliceIntent.Purpose.DECORATION ||
            intent.purpose == SmartSliceIntent.Purpose.MINIATURE
        val support = g.tall || visual
        result.support = if (!support) "nur bei Bedarf" else if (visual) "Tree / nur Druckbett" else "Auto"
        put(result, "enable_support", if (support) "1" else "0")
        if (support) {
            put(result, "support_type", if (visual) "tree(auto)" else "normal(auto)")
            put(result, "support_on_build_plate_only", if (visual) "1" else "0")
            put(result, "support_interface_top_layers", if (visual) "3" else "2")
            result.reasons.add(
                if (visual) "Tree-Support vom Druckbett reduziert Kontaktspuren auf sichtbaren Flächen."
                else "Support bleibt auf druckkritische Geometrie begrenzt.",
            )
        } else {
            result.reasons.add("Für diesen Zweck und das erkannte Seitenverhältnis sind keine zusätzlichen Stützen vorgesehen.")
        }

        result.seam = if (visual) "Rückseite" else "nächste geeignete Kante"
        put(result, "seam_position", if (visual) "back" else "aligned")
        if (visual) {
            put(result, "outer_wall_speed", if (intent.purpose == SmartSliceIntent.Purpose.MINIATURE) "30" else "40")
            put(result, "top_surface_speed", "35")
            result.reasons.add("Die Naht wird mit Orcas Seam-Strategie von den bevorzugten Sichtflächen weggelegt.")
        } else {
            result.reasons.add("Die Naht wird ausgerichtet, damit die Außenkontur konsistent bleibt.")
        }

        var shellLayers = Math.max(3, Math.ceil(0.8 / result.layerMm).toInt())
        if (intent.purpose == SmartSliceIntent.Purpose.FUNCTIONAL ||
            intent.purpose == SmartSliceIntent.Purpose.MECHANICAL
        ) {
            shellLayers = Math.max(shellLayers, 5)
        }
        // The vase branch already forces zero top shells; do not overwrite it with the general shell rule.
        if (intent.purpose != SmartSliceIntent.Purpose.VASE) {
            put(result, "top_shell_layers", shellLayers.toString())
        }
        put(result, "bottom_shell_layers", shellLayers.toString())
        put(result, "brim_type", if (g.slender || g.wide) "auto_brim" else "no_brim")
        result.reasons.add("$shellLayers obere und untere Schichten sorgen für geschlossene Deck- und Bodenflächen.")
        if (g.slender || g.wide) {
            result.reasons.add("Die erkannte schlanke oder breite Grundform aktiviert Orcas Auto-Brim als zusätzliche Haftreserve.")
        } else {
            result.reasons.add("Die Geometrie benötigt nach der schnellen Formprüfung keinen zusätzlichen Brim.")
        }

        when {
            material.contains("PETG") -> {
                put(result, "bridge_flow", "0.9")
                put(result, "bridge_speed", "25")
            }
            material.contains("TPU") -> {
                put(result, "bridge_flow", "1.0")
                put(result, "bridge_speed", "15")
            }
            material.contains("ABS") || material.contains("ASA") -> {
                put(result, "bridge_flow", "0.95")
                put(result, "bridge_speed", "25")
            }
            else -> {
                put(result, "bridge_flow", "0.95")
                put(result, "bridge_speed", "30")
            }
        }
        result.reasons.add("Brückenfluss und -geschwindigkeit sind auf das aktive Material $material abgestimmt.")

        if (intent.purpose == SmartSliceIntent.Purpose.PRECISION ||
            intent.priority == SmartSliceIntent.Priority.DIMENSIONAL
        ) {
            put(result, "outer_wall_speed", "35")
            put(result, "elefant_foot_compensation", "0.15")
            result.reasons.add("Kontrollierte Außenwandgeschwindigkeit verbessert die Wiederholbarkeit maßkritischer Flächen.")
        }
        if (g.verySmall && result.nozzleMm >= 0.4) {
            result.warnings.add("Sehr kleine Geometrie erkannt. Für feinste Details kann eine 0,2-mm-Düse geeigneter sein; die Hardware wird nicht automatisch geändert.")
        }
        if (g.objectCount == 0) result.warnings.add("Es befindet sich kein analysierbares Objekt auf der aktiven Platte.")

        // Only hand options understood by this exact embedded Orca core to the live profile, and
        // report only effective changes rather than presenting unchanged defaults as decisions.
        val changes = result.changes.entries.iterator()
        while (changes.hasNext()) {
            val change = changes.next()
            if (!PrintConfigDef.getInstance().options.containsKey(change.key) || change.value == cfg.get(change.key)) {
                changes.remove()
            }
        }
        return result
    }

    @JvmStatic
    fun apply(result: SmartSliceResult) {
        ProfileSettingsRepository.setPendingValues(ConfigObject.PROFILE_LIST_PRINT, result.changes)
        SliceBeam.CONFIG_UID++
    }

    private fun put(result: SmartSliceResult, key: String, value: String) {
        result.changes[key] = value
    }

    private fun first(text: String?): String? =
        text?.let { java.util.regex.Pattern.compile("[,;]").split(it)[0].trim() }

    private fun value(value: String?, fallback: String): String =
        if (value == null || value.trim().isEmpty()) fallback else value.trim()

    private fun number(text: String?, fallback: Double): Double = try {
        java.lang.Double.parseDouble(value(text, "").replace("%", ""))
    } catch (_: Exception) {
        fallback
    }

    private fun clamp(value: Double, min: Double, max: Double): Double = Math.max(min, Math.min(value, max))

    private fun round(value: Double, places: Int): Double {
        val factor = Math.pow(10.0, places.toDouble())
        return Math.round(value * factor) / factor
    }

    private fun decimal(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun priorityLabel(priority: SmartSliceIntent.Priority): String = when (priority) {
        SmartSliceIntent.Priority.QUALITY -> "Qualität"
        SmartSliceIntent.Priority.FAST -> "Geschwindigkeit"
        SmartSliceIntent.Priority.STRENGTH -> "Stabilität"
        SmartSliceIntent.Priority.DIMENSIONAL -> "Maßhaltigkeit"
        else -> "Ausgewogen"
    }
}
