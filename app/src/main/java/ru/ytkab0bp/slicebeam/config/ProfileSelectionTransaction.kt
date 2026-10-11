package ru.ytkab0bp.slicebeam.config

import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper

/** Persists profile edits and a requested selection as one recoverable configuration change. */
object ProfileSelectionTransaction {
    @JvmStatic
    fun apply(
        config: Slic3rConfigWrapper,
        printer: String?,
        filament: String?,
        process: String?,
        synchronizeCompatible: () -> Boolean,
        persist: () -> Boolean,
    ): Boolean {
        val presets = config.presets ?: return false
        if (printer != null && config.findPrinter(printer) == null) return false
        if (filament != null && config.findFilament(filament) == null) return false
        if (process != null && config.findPrint(process) == null) return false

        val snapshot = ProfileConfigSnapshot(config)
        val committedDraftTypes = LinkedHashSet<Int>()
        try {
            // Save edits against the profiles that were active before switching. This prevents a
            // printer change from silently discarding staged edits or applying them to new profiles.
            val activeProfiles = listOf(
                ConfigObject.PROFILE_LIST_PRINTER to config.findPrinter(presets["printer"]),
                ConfigObject.PROFILE_LIST_PRINT to config.findPrint(presets["print"]),
                ConfigObject.PROFILE_LIST_FILAMENT to config.findFilament(presets["filament"]),
            )
            for ((profileType, profile) in activeProfiles) {
                if (profile != null && ProfileSettingsRepository.pendingCountFor(profileType) > 0) {
                    ProfileSettingsRepository.applyPendingFor(profileType, profile)
                    committedDraftTypes.add(profileType)
                }
            }

            var changed = committedDraftTypes.isNotEmpty()
            if (printer != null && presets["printer"] != printer) {
                presets.put("printer", printer)
                changed = true
            }
            if (filament != null && presets["filament"] != filament) {
                presets.put("filament", filament)
                changed = true
            }
            if (process != null && presets["print"] != process) {
                presets.put("print", process)
                changed = true
            }
            changed = synchronizeCompatible() || changed

            // An unchanged selection must never clear pending edits. If there were edits, the
            // committedDraftTypes set makes this a persisted transaction above.
            if (!changed) return true
            if (!persist()) {
                snapshot.restore()
                return false
            }
            committedDraftTypes.forEach(ProfileSettingsRepository::clearDraft)
            return true
        } catch (_: Exception) {
            snapshot.restore()
            return false
        }
    }
}
