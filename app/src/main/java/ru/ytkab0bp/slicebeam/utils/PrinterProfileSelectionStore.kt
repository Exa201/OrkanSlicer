package ru.ytkab0bp.slicebeam.utils

import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** Stores the last selected print and filament presets for each saved printer profile. */
object PrinterProfileSelectionStore {
    private const val PRINT_PREFIX = "printer_profile_selection.print."
    private const val FILAMENT_PREFIX = "printer_profile_selection.filament."

    data class Selection(val process: String?, val filament: String?)

    @JvmStatic
    fun read(printerTitle: String): Selection? {
        val preferences = Prefs.getPrefs() ?: return null
        val suffix = profileKey(printerTitle)
        val process = preferences.getString(PRINT_PREFIX + suffix, null)
        val filament = preferences.getString(FILAMENT_PREFIX + suffix, null)
        return if (process == null && filament == null) null else Selection(process, filament)
    }

    @JvmStatic
    fun write(printerTitle: String, process: String?, filament: String?): Boolean {
        val preferences = Prefs.getPrefs() ?: return false
        val suffix = profileKey(printerTitle)
        val editor: SharedPreferences.Editor = preferences.edit()
        if (process == null) editor.remove(PRINT_PREFIX + suffix)
        else editor.putString(PRINT_PREFIX + suffix, process)
        if (filament == null) editor.remove(FILAMENT_PREFIX + suffix)
        else editor.putString(FILAMENT_PREFIX + suffix, filament)
        return editor.commit()
    }

    private fun profileKey(printerTitle: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(printerTitle.toByteArray(StandardCharsets.UTF_8))
        return buildString(digest.size * 2) {
            digest.forEach { byte ->
                val value = byte.toInt() and 0xFF
                append(HEX[value ushr 4])
                append(HEX[value and 0x0F])
            }
        }
    }

    private const val HEX = "0123456789abcdef"
}
