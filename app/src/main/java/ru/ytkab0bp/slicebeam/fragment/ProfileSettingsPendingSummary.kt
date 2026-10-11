package ru.ytkab0bp.slicebeam.fragment

import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef
import ru.ytkab0bp.slicebeam.slic3r.Slic3rLocalization

/** Builds the save dialog's change summary from the same staged profile values used by slicing. */
object ProfileSettingsPendingSummary {
    @JvmStatic
    fun build(profileType: Int, profile: ConfigObject): String {
        val summary = StringBuilder()
        ProfileSettingsRepository.pendingEntries(profileType).forEach { (key, _) ->
            val definition = PrintConfigDef.getInstance().options[key] ?: return@forEach
            if (summary.isNotEmpty()) summary.append('\n')
            summary.append(Slic3rLocalization.getString(definition.getFullLabel()))
                .append(" - ")
                .append(
                    ProfileConfigValueController.valueFor(
                        profileType,
                        profile,
                        definition.key,
                        definition.guiType,
                        definition.defaultValue,
                        -1,
                        false,
                    ),
                )
        }
        return summary.toString()
    }
}
