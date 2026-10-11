package ru.ytkab0bp.slicebeam.utils

import java.util.Locale

/** Pure conversion helpers for preferences that also need deterministic local tests. */
internal object PrefsValueCodec {
    private const val DEFAULT_FILAMENT_COLOR = 0xFF1AC5A2.toInt()

    fun parseFilamentSlots(raw: String?): MutableList<FilamentSlot> {
        val slots = ArrayList<FilamentSlot>()
        if (raw.isNullOrEmpty()) {
            slots.add(FilamentSlot(DEFAULT_FILAMENT_COLOR, "PLA"))
            return slots
        }

        for (part in raw.split(',')) {
            if (part.isEmpty()) continue
            var type = "PLA"
            var hex = part
            val separator = part.indexOf('|')
            if (separator >= 0) {
                type = part.substring(0, separator)
                hex = part.substring(separator + 1)
            }
            val color = try {
                hex.trim().toLong(16).toInt() or 0xFF000000.toInt()
            } catch (_: NumberFormatException) {
                DEFAULT_FILAMENT_COLOR
            }
            slots.add(FilamentSlot(color, type))
            if (slots.size >= Prefs.MAX_FILAMENT_COLORS) break
        }
        if (slots.isEmpty()) slots.add(FilamentSlot(DEFAULT_FILAMENT_COLOR, "PLA"))
        return slots
    }

    fun serializeFilamentSlots(slots: List<FilamentSlot>): String = buildString {
        slots.take(Prefs.MAX_FILAMENT_COLORS).forEachIndexed { index, slot ->
            if (index > 0) append(',')
            append(slot.type)
                .append('|')
                .append(String.format(Locale.getDefault(), "%06X", slot.color and 0xFFFFFF))
        }
    }

    fun themeMode(ordinal: Int): Prefs.ThemeMode =
        Prefs.ThemeMode.values().getOrNull(ordinal) ?: Prefs.ThemeMode.SYSTEM
}
