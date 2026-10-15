package ru.ytkab0bp.slicebeam.utils

import android.text.TextUtils
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.message.BasicHeader
import org.json.JSONException
import org.json.JSONObject
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import java.util.ArrayList

/** Reads live Moonraker MMU slots and preserves their physical gate assignment. */
class MmuFilamentSync private constructor() {
    fun interface Callback {
        fun onResult(updated: Int, error: String?)
    }

    companion object {
        data class PhysicalGate(val index: Int, val printerTool: Int, val occupied: Boolean, val filament: FilamentSlot?)
        data class AssignedGate(val modelTool: Int, val gate: Int, val printerTool: Int,
                                val material: String?, val color: Int?)
        private const val SNAPSHOT_PRINTER = "mmu_snapshot_printer"
        private const val SNAPSHOT = "mmu_snapshot"
        private const val CHOICE_PRINTER = "mmu_choice_printer"
        private const val CHOICE_GATES = "mmu_choice_gates"

        @JvmStatic
        fun synchronize(printer: ConfigObject, callback: Callback) {
            synchronizeInternal(printer, false, callback)
        }

        /** User-requested sync: replace the count, materials, colors and gate assignments together. */
        @JvmStatic
        fun synchronizeAll(printer: ConfigObject, callback: Callback) {
            synchronizeInternal(printer, true, callback)
        }

        private fun synchronizeInternal(printer: ConfigObject, replacePalette: Boolean, callback: Callback) {
            var host = printer.get("print_host")?.trim() ?: ""
            if (TextUtils.isEmpty(host)) {
                callback.onResult(0, message(R.string.PrinterWebHostMissing))
                return
            }

            host = host.trim()
            if (!host.startsWith("http://") && !host.startsWith("https://")) host = "http://$host"
            while (host.endsWith("/")) host = host.dropLast(1)
            val apiKey = printer.get("printhost_apikey") ?: ""
            val headers: Array<Header> = if (TextUtils.isEmpty(apiKey)) emptyArray()
                else arrayOf(BasicHeader("X-Api-Key", apiKey))

            val client = AsyncHttpClient().apply { setTimeout(15_000) }
            client.get(SliceBeam.requireInstance(), "$host/printer/objects/query?mmu", headers, null,
                object : AsyncHttpResponseHandler() {
                    override fun onSuccess(statusCode: Int, responseHeaders: Array<Header>?, body: ByteArray?) {
                        val updated = try {
                            if (body == null || body.isEmpty()) throw JSONException(message(R.string.MmuSyncEmptyResponse))
                            val responseText = String(body, Charsets.UTF_8)
                            val response = JSONObject(responseText)
                            val mmu = response.getJSONObject("result").getJSONObject("status").getJSONObject("mmu")
                            if (!mmu.optBoolean("enabled", false)) throw JSONException(message(R.string.MmuSyncNotAvailable))
                            val remote = fromMoonraker(response)
                            val printerGates = gateSlots(mmu)
                            val prefs = Prefs.getPrefs()
                                ?: throw IllegalStateException("Preferences are not initialized")
                            val current = Prefs.getFilamentSlots()
                            val updatedSlots: List<FilamentSlot>
                            val count: Int
                            if (replacePalette) {
                                val mapping = mmu.getJSONArray("ttg_map")
                                val toolToGate = IntArray(minOf(mapping.length(), Prefs.MAX_FILAMENT_COLORS)) {
                                    mapping.optInt(it, -1)
                                }
                                val status = mmu.optJSONArray("gate_status")
                                val incompleteGate = toolToGate.firstOrNull { gate ->
                                    gate >= 0 && status != null && status.optInt(gate, 0) > 0
                                        && printerGates.getOrNull(gate) == null
                                }
                                if (incompleteGate != null) throw JSONException(
                                    SliceBeam.requireInstance().getString(R.string.MmuSyncIncompleteSlot, incompleteGate + 1),
                                )
                                if (toolToGate.none { printerGates.getOrNull(it) != null }) {
                                    throw JSONException(message(R.string.MmuAssignmentNoLoadedSlots))
                                }
                                val synced = FilamentPaletteSyncPolicy.replaceFromPrinter(printerGates, toolToGate)
                                updatedSlots = synced.slots
                                count = updatedSlots.size
                                setSelectedGates(printer.title.orEmpty(), synced.gates)
                            } else {
                                // Once assigned, logical T1 may refer to physical gate 3 rather than gate 2.
                                val choices = getSelectedGates(printer.title.orEmpty(), current.size)
                                val liveTools = choices?.map { printerGates.getOrNull(it) } ?: remote
                                updatedSlots = FilamentPaletteSyncPolicy.updateExisting(current, liveTools)
                                count = liveTools.take(current.size).count { it != null }
                            }
                            if (replacePalette || !TemporaryProjectSettings.active) Prefs.setFilamentSlots(updatedSlots)
                            Prefs.setMmuSyncedPrinter(printer.title)
                            prefs.edit()
                                .putString(SNAPSHOT_PRINTER, printer.title)
                                .putString(SNAPSHOT, responseText)
                                .apply()
                            count
                        } catch (error: Exception) {
                            callback.onResult(0, error.message ?: message(R.string.MmuSyncFailed))
                            return
                        }
                        callback.onResult(updated, null)
                    }

                    override fun onFailure(
                        statusCode: Int,
                        responseHeaders: Array<Header>?,
                        body: ByteArray?,
                        error: Throwable?,
                    ) {
                        val detail = if (body != null && body.isNotEmpty()) {
                            String(body, Charsets.UTF_8)
                        } else {
                            error?.message?.takeIf { it.isNotBlank() } ?: "HTTP $statusCode"
                        }
                        callback.onResult(0, detail)
                    }
                })
        }

        private fun message(resource: Int): String = SliceBeam.requireInstance().getString(resource)

        @JvmStatic
        fun getSnapshot(printerTitle: String): JSONObject? {
            val prefs = Prefs.getPrefs()
            if (prefs == null || printerTitle != prefs.getString(SNAPSHOT_PRINTER, null)) return null
            return try {
                JSONObject(prefs.getString(SNAPSHOT, "") ?: "")
                    .getJSONObject("result").getJSONObject("status").getJSONObject("mmu")
            } catch (_: Exception) {
                null
            }
        }

        @JvmStatic
        fun getSelectedGates(printerTitle: String, toolCount: Int): IntArray? {
            if (TemporaryProjectSettings.active) return TemporaryProjectSettings.selectedGates(printerTitle, toolCount)
            val prefs = Prefs.getPrefs() ?: return null
            val storedPrinter = prefs.getString(CHOICE_PRINTER, null)
            val raw = prefs.getString(CHOICE_GATES, null)
            if (printerTitle != storedPrinter || TextUtils.isEmpty(raw)) return null
            val gateText = raw ?: return null
            val parts = gateText.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            // A one-color model only needs the first choice of a larger synced palette.
            if (toolCount < 1 || parts.size < toolCount) return null
            return try {
                IntArray(toolCount) { index -> parts[index].toInt() }
            } catch (_: NumberFormatException) {
                null
            }
        }

        @JvmStatic
        fun setSelectedGates(printerTitle: String, gates: IntArray) {
            if (TemporaryProjectSettings.active) {
                TemporaryProjectSettings.setSelectedGates(printerTitle, gates)
                return
            }
            val value = gates.joinToString(",")
            Prefs.getPrefs()?.edit()
                ?.putString(CHOICE_PRINTER, printerTitle)
                ?.putString(CHOICE_GATES, value)
                ?.apply()
        }

        /** Removing a logical filament also removes its physical choice, so later tools stay aligned. */
        fun removeSelectedGate(index: Int, previousCount: Int) {
            if (TemporaryProjectSettings.active) {
                TemporaryProjectSettings.removeSelectedGate(index)
                return
            }
            val prefs = Prefs.getPrefs() ?: return
            val printer = prefs.getString(CHOICE_PRINTER, null) ?: return
            val gates = getSelectedGates(printer, previousCount) ?: return
            if (index !in gates.indices) return
            setSelectedGates(printer, gates.filterIndexed { tool, _ -> tool != index }.toIntArray())
        }

        @JvmStatic
        fun loadedGates(printerTitle: String): IntArray {
            val status = getSnapshot(printerTitle)?.optJSONArray("gate_status") ?: return IntArray(0)
            val loaded = ArrayList<Int>()
            for (gate in 0 until status.length()) if (status.optInt(gate, 0) > 0) loaded.add(gate)
            return loaded.toIntArray()
        }

        /** The physical ACE view includes empty gates; never compress it into the slicer's tool palette. */
        @JvmStatic
        fun physicalGates(printerTitle: String): List<PhysicalGate> {
            val mmu = getSnapshot(printerTitle) ?: return emptyList()
            val status = mmu.optJSONArray("gate_status") ?: return emptyList()
            val mapping = mmu.optJSONArray("ttg_map")
            val filaments = try { gateSlots(mmu) } catch (_: JSONException) { emptyList() }
            return List(minOf(status.length(), Prefs.MAX_FILAMENT_COLORS)) { gate ->
                val tool = (0 until (mapping?.length() ?: 0)).firstOrNull {
                    mapping?.optInt(it, -1) == gate
                } ?: -1
                PhysicalGate(gate, tool, status.optInt(gate, 0) > 0, filaments.getOrNull(gate))
            }
        }

        @JvmStatic
        fun defaultGateForTool(printerTitle: String, tool: Int): Int {
            val mapping = getSnapshot(printerTitle)?.optJSONArray("ttg_map") ?: return -1
            return mapping.optInt(tool, -1)
        }

        @JvmStatic
        fun toolForGate(printerTitle: String, gate: Int): Int {
            val mapping = getSnapshot(printerTitle)?.optJSONArray("ttg_map") ?: return -1
            for (tool in 0 until mapping.length()) if (mapping.optInt(tool, -1) == gate) return tool
            return -1
        }

        /** Compare only gates used by this sliced job before and after the final printer refresh. */
        fun assignedGates(printerTitle: String, activeTools: IntArray): List<AssignedGate>? {
            val count = (activeTools.maxOrNull() ?: 0) + 1
            val selected = getSelectedGates(printerTitle, count) ?: return null
            if (getSnapshot(printerTitle) == null) return null
            return activeTools.map { tool ->
                val gate = selected.getOrNull(tool) ?: return null
                val filament = gateSlot(printerTitle, gate)
                AssignedGate(tool, gate, toolForGate(printerTitle, gate),
                    filament?.type, filament?.color)
            }
        }

        @JvmStatic
        fun gateLabel(printerTitle: String, gate: Int): String {
            val mmu = getSnapshot(printerTitle)
            val materials = mmu?.optJSONArray("gate_material")
            val colors = mmu?.optJSONArray("gate_color")
            val statuses = mmu?.optJSONArray("gate_status")
            if (materials == null || colors == null || statuses == null || gate < 0 || gate >= statuses.length()) {
                return "ACE ${gate + 1}"
            }
            if (statuses.optInt(gate, 0) <= 0) return "ACE ${gate + 1} · leer"
            val material = materials.optString(gate, "Filament")
            var rawColor = colors.optString(gate, "")
            if (rawColor.length >= 6) rawColor = "#${rawColor.substring(0, 6)}"
            return "ACE ${gate + 1} · $material" +
                (if (TextUtils.isEmpty(rawColor)) "" else " · $rawColor")
        }

        /** Physical gate material/color, independent of the imported project's palette. */
        fun gateSlot(printerTitle: String, gate: Int): FilamentSlot? = try {
            getSnapshot(printerTitle)?.let { gateSlots(it).getOrNull(gate) }
        } catch (_: Exception) { null }

        @JvmStatic
        @Throws(JSONException::class)
        fun fromMoonraker(response: JSONObject): List<FilamentSlot?> {
            val mmu = response.getJSONObject("result").getJSONObject("status").getJSONObject("mmu")
            if (!mmu.optBoolean("enabled", false)) throw JSONException("MMU is disabled")
            val mapping = mmu.getJSONArray("ttg_map")
            if (mapping.length() == 0) throw JSONException("No MMU tool mappings available")
            val gates = gateSlots(mmu)
            return List(minOf(mapping.length(), Prefs.MAX_FILAMENT_COLORS)) { tool ->
                gates.getOrNull(mapping.optInt(tool, -1))
            }
        }

        private fun gateSlots(mmu: JSONObject): List<FilamentSlot?> {
            val colors = mmu.getJSONArray("gate_color")
            val materials = mmu.getJSONArray("gate_material")
            val gateStatus = mmu.optJSONArray("gate_status")
            val slots = ArrayList<FilamentSlot?>()
            for (gate in 0 until minOf(colors.length(), materials.length())) {
                if (gateStatus != null && gateStatus.optInt(gate, 0) <= 0) {
                    slots.add(null)
                    continue
                }
                val material = materials.optString(gate, "").trim()
                var color = colors.optString(gate, "").trim()
                if (material.isEmpty() || material.equals("unknown", ignoreCase = true) || color.isEmpty()
                ) {
                    slots.add(null)
                    continue
                }
                if (color.startsWith("#")) color = color.substring(1)
                if (color.length != 6 && color.length != 8) {
                    slots.add(null)
                    continue
                }
                if (!color.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                    slots.add(null)
                    continue
                }
                try {
                    val rgb = color.substring(0, 6).toLong(16).toInt()
                    slots.add(FilamentSlot(rgb, material))
                } catch (_: NumberFormatException) {
                    slots.add(null)
                }
            }
            return slots
        }
    }
}
