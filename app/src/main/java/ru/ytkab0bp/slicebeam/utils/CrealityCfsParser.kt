package ru.ytkab0bp.slicebeam.utils

import org.json.JSONArray
import org.json.JSONObject

/** Adapts Creality's Moonraker `box` status into the gate arrays used by the MMU UI. */
internal object CrealityCfsParser {
    private const val GATE_COUNT = 16
    private val materialIds = mapOf(
        "000001" to "PLA", "000002" to "ABS", "000003" to "PETG", "000004" to "TPU",
        "000005" to "ASA", "000006" to "PA", "000007" to "PC", "000008" to "PLA-CF",
        "000009" to "PETG-CF", "000010" to "PA-CF",
    )

    /** Returns null when this Moonraker response does not contain a usable Creality CFS box. */
    fun toMmuResponse(response: JSONObject): JSONObject? {
        val box = response.optJSONObject("result")?.optJSONObject("status")?.optJSONObject("box") ?: return null
        val sameMaterialTypes = HashMap<String, String>()
        box.optJSONArray("same_material")?.let { entries ->
            for (index in 0 until entries.length()) {
                val entry = entries.optJSONArray(index) ?: continue
                if (entry.length() < 4) continue
                val id = entry.optString(0).trim()
                val type = entry.optString(3).trim()
                if (id.isNotEmpty() && type.isNotEmpty() && type != "null" && !sameMaterialTypes.containsKey(id)) {
                    sameMaterialTypes[id] = type
                }
            }
        }

        val colors = MutableList(GATE_COUNT) { "" }
        val materials = MutableList(GATE_COUNT) { "" }
        val status = MutableList(GATE_COUNT) { 0 }
        var foundConnectedTray = false
        for (trayNumber in 1..4) {
            val tray = box.optJSONObject("T$trayNumber") ?: continue
            if (!tray.optString("state").equals("connect", ignoreCase = true)) continue
            foundConnectedTray = true
            for (lane in 0..3) {
                val gate = (trayNumber - 1) * 4 + lane
                val materialId = tray.optJSONArray("material_type")?.optString(lane).orEmpty().trim()
                val type = materialIds[materialId] ?: sameMaterialTypes[materialId]
                val rawColor = tray.optJSONArray("color_value")?.optString(lane).orEmpty().trim()
                val color = normalizeColor(rawColor)
                if (type != null) materials[gate] = type
                else if (materialId.isNotEmpty() && materialId != "-1" && color.isNotEmpty()) materials[gate] = "unknown"
                if (color.isNotEmpty()) colors[gate] = color
                if (materials[gate].isNotEmpty() || colors[gate].isNotEmpty()) status[gate] = 1
            }
        }
        if (!foundConnectedTray || status.none { it > 0 }) return null

        val mmu = JSONObject()
            .put("enabled", true)
            .put("ttg_map", JSONArray((0 until GATE_COUNT).toList()))
            .put("gate_status", JSONArray(status))
            .put("gate_material", JSONArray(materials))
            .put("gate_color", JSONArray(colors))
        return JSONObject().put("result", JSONObject().put("status", JSONObject().put("mmu", mmu)))
    }

    private fun normalizeColor(raw: String): String {
        var value = raw.removePrefix("#").removePrefix("0x").removePrefix("0X")
        if (value.length == 7 && value[0] == '0') value = value.drop(1)
        if (value.length == 8) value = value.take(6)
        return value.takeIf { it.length == 6 && it.all { digit -> digit in '0'..'9' || digit.lowercaseChar() in 'a'..'f' } }
            ?.uppercase() ?: ""
    }
}
