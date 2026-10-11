package ru.ytkab0bp.slicebeam.fragment

import android.util.Log
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef
import java.util.regex.Pattern

/** Keeps profile option reads and indexed edits consistent with Orca's stored value formats. */
object ProfileConfigValueController {
    private const val TAG = "ProfileListFragment"

    @JvmStatic
    fun valueFor(
        profileType: Int,
        profile: ConfigObject,
        key: String,
        guiType: ConfigOptionDef.GUIType?,
        defaultValue: String?,
        index: Int,
        ignorePending: Boolean,
    ): String? {
        var value = ProfileSettingsRepository.valueFor(profileType, profile, key, !ignorePending)
        if (index != -1 && value != null) {
            val parts = Pattern.compile(separator(guiType)).split(value, -1)
            try {
                value = parts[index]
            } catch (error: ArrayIndexOutOfBoundsException) {
                Log.w(TAG, "Failed to parse profile option '$key'", error)
            }
        }
        return value ?: if (key == "host_type") "octoprint" else defaultValue
    }

    @JvmStatic
    fun stageValue(
        profileType: Int,
        profile: ConfigObject,
        key: String,
        guiType: ConfigOptionDef.GUIType?,
        index: Int,
        editedValue: String,
        defaultValue: String? = null,
    ) {
        val baseValue = valueFor(
            profileType, profile, key, guiType, null, index = -1, ignorePending = true,
        ) ?: defaultValue
        val currentValue = valueFor(
            profileType, profile, key, guiType, defaultValue, index = -1, ignorePending = false,
        ) ?: baseValue
        val value = if (index == -1) {
            editedValue
        } else {
            val delimiter = separator(guiType)
            val parts = Pattern.compile(delimiter).split(currentValue.orEmpty(), -1).toMutableList()
            val defaults = defaultValue
                ?.let { Pattern.compile(delimiter).split(it, -1).toList() }
                .orEmpty()
            while (parts.size <= index) {
                val next = parts.size
                val fallback = defaults.getOrNull(next)
                    ?: defaults.lastOrNull()
                    ?: parts.lastOrNull()?.takeIf { it.isNotEmpty() }
                    ?: ""
                parts.add(fallback)
            }
            parts[index] = editedValue
            parts.joinToString(delimiter)
        }
        ProfileSettingsRepository.setPendingValue(profileType, key, baseValue, value)
    }

    private fun separator(guiType: ConfigOptionDef.GUIType?): String =
        if (guiType == ConfigOptionDef.GUIType.COLOR) ";" else ","
}
