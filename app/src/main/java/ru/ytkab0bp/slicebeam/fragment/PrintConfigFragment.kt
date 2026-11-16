package ru.ytkab0bp.slicebeam.fragment

import android.widget.Toast
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef
import ru.ytkab0bp.slicebeam.slic3r.Slic3rUtils

/** Process-profile screen with printer filtering and purge controls. Filament routing lives in the workspace palette. */
class PrintConfigFragment : ProfileListFragment() {
    private var compatItems: List<ProfileListItem>? = null
    private var lastPrinter: String? = null
    private var lastUid = 0
    private var currentConfig: ConfigObject? = null

    override fun getProfileListType(): Int = ConfigObject.PROFILE_LIST_PRINT

    override fun useTabs(): Boolean = true

    override fun useAccordionCategories(): Boolean = true

    override fun showFilamentPalette(): Boolean = false

    override fun onCreate() {
        super.onCreate()
        SliceBeam.ensurePrinterScopedProcess()
        onResetConfig()
    }

    override fun onResume() {
        SliceBeam.ensurePrinterScopedProcess()
        super.onResume()
    }

    override fun getItems(filter: Boolean): List<ProfileListItem> {
        val profiles = SliceBeam.requireConfig().printConfigs
        if (!filter) return profiles.map { it as ProfileListItem }

        val printer = SliceBeam.requireConfig().presets!!.get("printer")
        if (lastPrinter == printer && compatItems != null && lastUid == SliceBeam.CONFIG_UID) {
            return compatItems.orEmpty()
        }

        val printerConfig = SliceBeam.requireConfig().findPrinter(printer)
        val model = printerConfig?.get("printer_model")
        var nozzle = printerConfig?.get("printer_variant")
        if (printerConfig != null && nozzle == null) {
            nozzle = Slic3rUtils.firstNozzleDiameter(printerConfig.get("nozzle_diameter"))
        }
        val checker = printerConfig?.let { Slic3rUtils.ConfigChecker(it.serialize()) }
        val seenTitles = HashSet<String>()
        val compatible = ArrayList<ProfileListItem>(profiles.size)

        try {
            for (profile in profiles) {
                if (Slic3rUtils.isPrinterCompatible(
                        profile.title,
                        profile.get("compatible_printers").orEmpty(),
                        profile.get("compatible_printers_condition").orEmpty(),
                        printer,
                        model,
                        nozzle,
                        checker,
                    ) && Slic3rUtils.layerHeightFitsNozzle(profile.get("layer_height"), nozzle)
                    && Slic3rUtils.lineWidthFitsNozzle(profile.get("line_width"), nozzle)
                    && seenTitles.add(profile.title.orEmpty())
                ) {
                    // Bundled profiles can be duplicated for every printer; show each name once.
                    compatible.add(profile)
                }
            }
        } finally {
            checker?.release()
        }

        lastPrinter = printer
        lastUid = SliceBeam.CONFIG_UID
        compatItems = compatible
        return compatible
    }

    override fun getConfigItems(): List<ProfileOptionElement> =
        OrcaPrintSettingsBuilder.build(this)

    private fun optionValue(key: String): String {
        val definition = PrintConfigDef.getInstance().options[key]
        val profile = requireNotNull(currentConfig)
        var value = ProfileSettingsRepository.valueFor(getProfileListType(), profile, key, true)
        if (value == null && definition != null) value = definition.defaultValue
        return value ?: ""
    }

    internal fun currentSettingValue(key: String): String = optionValue(key)

    internal fun refreshConditionalSettings() = onUpdateConfigItems()

    override fun cloneCurrentProfile() {
        val current = currentConfig ?: return
        val copy = ConfigObject(
            SliceBeam.requireInstance().getString(R.string.SettingsProfileCopy, current.title),
        )
        copy.profileListType = ConfigObject.PROFILE_LIST_PRINT
        copy.values.putAll(current.values)
        val config = SliceBeam.requireConfig()
        val presets = config.presets ?: return
        val previousProcess = presets["print"]
        config.printConfigs.add(copy)
        presets.put("print", copy.title)
        if (!saveConfigOrNotify()) {
            config.printConfigs.remove(copy)
            if (previousProcess == null) presets.remove("print") else presets.put("print", previousProcess)
            return
        }

        currentConfig = ConfigObject(copy)
        setProfileDropdownTitle(getCurrentConfig()?.title)
        compatItems = null
    }

    override fun deleteCurrentProfile() {
        val current = currentConfig ?: return
        compatItems = null
        val profiles = SliceBeam.requireConfig().printConfigs
        if (profiles.size <= 1) {
            getContext()?.let { Toast.makeText(it, R.string.ProfileDeleteLastBlocked, Toast.LENGTH_LONG).show() }
            return
        }
        val config = SliceBeam.requireConfig()
        val stored = config.findPrint(current.title) ?: return
        val removedIndex = profiles.indexOf(stored)
        if (removedIndex < 0) return
        profiles.removeAt(removedIndex)
        val firstProfile = getItems(false).firstOrNull() ?: run {
            profiles.add(removedIndex, stored)
            return
        }
        if (!selectItem(firstProfile)) {
            profiles.add(removedIndex, stored)
            currentConfig = ConfigObject(stored)
            setProfileDropdownTitle(stored.title)
            compatItems = null
            return
        }
        setProfileDropdownTitle(getCurrentConfig()?.title)
    }

    override fun onApplyConfig(title: String): Boolean {
        val current = currentConfig ?: return false
        compatItems = null
        val config = SliceBeam.requireConfig()
        val stored = config.findPrint(current.title) ?: return false
        val previousStored = ConfigObject(stored)
        val presets = config.presets ?: return false
        val previousProcess = presets["print"]
        stored.setTitle(title)
        stored.values.clear()
        stored.values.putAll(current.values)
        presets.put("print", title)
        if (!saveConfigOrNotify()) {
            stored.setTitle(previousStored.title)
            stored.values.clear()
            stored.values.putAll(previousStored.values)
            if (previousProcess == null) presets.remove("print") else presets.put("print", previousProcess)
            return false
        }
        current.setTitle(title)
        setProfileDropdownTitle(title)
        return true
    }

    override fun onResetConfig() {
        val profile = SliceBeam.requireConfig().findPrint(SliceBeam.requireConfig().presets!!.get("print"))
        if (profile != null) {
            currentConfig = ConfigObject(profile)
        } else {
            val custom = ConfigObject(SliceBeam.requireInstance().getString(R.string.IntroCustomProfileName))
            currentConfig = custom
            SliceBeam.requireConfig().printConfigs.add(ConfigObject(custom))
            saveConfigOrNotify()
        }
    }

    override fun getCurrentConfig(): ConfigObject? = currentConfig

    override val title: Int = R.string.SlotPrintConfigTooltip

    override fun selectItem(item: ProfileListItem): Boolean {
        if (!SliceBeam.applyProfileSelection(process = item.title)) {
            getContext()?.let { Toast.makeText(it, R.string.ConfigSaveFailed, Toast.LENGTH_LONG).show() }
            return false
        }
        onResetConfig()
        compatItems = null
        return true
    }

}
