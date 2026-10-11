package ru.ytkab0bp.slicebeam.fragment

import android.content.Context
import android.widget.Toast
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.BeamAlertDialogBuilder
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef
import ru.ytkab0bp.slicebeam.slic3r.Slic3rUtils
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Process-profile screen with printer filtering, purge controls and the filament palette. */
class PrintConfigFragment : ProfileListFragment() {
    private var compatItems: List<ProfileListItem>? = null
    private var lastPrinter: String? = null
    private var lastUid = 0
    private var currentConfig: ConfigObject? = null
    private var toolsThemeRevision by mutableIntStateOf(0)
    private var filamentSyncing by mutableStateOf(false)

    override fun getProfileListType(): Int = ConfigObject.PROFILE_LIST_PRINT

    override fun useTabs(): Boolean = true

    override fun useAccordionCategories(): Boolean = true

    override fun showFilamentPalette(): Boolean = true

    override fun onCreate() {
        super.onCreate()
        SliceBeam.ensurePrinterScopedProcess()
        onResetConfig()
    }

    override fun onApplyTheme() {
        super.onApplyTheme()
        toolsThemeRevision++
    }

    override fun onResume() {
        SliceBeam.ensurePrinterScopedProcess()
        super.onResume()
        // Refresh printer-dependent controls when returning from another profile screen.
        toolsThemeRevision++
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

    @androidx.compose.runtime.Composable
    override fun BelowFilamentPaletteContent(modifier: Modifier) {
        val context = LocalContext.current
        val revision = toolsThemeRevision
        val printer = SliceBeam.CONFIG?.let { config ->
            config.presets?.get("printer")?.let(config::findPrinter)
        }
        val canSync = PrintConfigToolsLogic.canSyncFilaments(printer?.get("print_host"))
        key(revision) {
            PrintConfigToolsBar(
                modifier = modifier,
                canSyncFilaments = canSync,
                isSyncing = filamentSyncing,
                onPrimeTower = { showPrimeTowerPositionDialog(context) },
                onFlushVolumes = { showFlushVolumesDialog(context) },
                onSyncFilaments = { printer?.let(::syncMmuFilaments) },
            )
        }
    }

    private fun syncMmuFilaments(printer: ConfigObject) {
        if (filamentSyncing) return
        filamentSyncing = true
        MmuFilamentSync.synchronizeAll(printer) { updated, error ->
            filamentSyncing = false
            if (error != null) {
                showMmuSyncError(error)
            } else {
                filamentPaletteController?.reload()
                getContext()?.let { context ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.MmuSyncSuccess, updated),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private fun showMmuSyncError(detail: String) {
        val context = getContext() ?: return
        BeamAlertDialogBuilder(context)
            .setTitle(R.string.MmuSyncFailed)
            .setMessage(detail)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun optionValue(key: String): String {
        val definition = PrintConfigDef.getInstance().options[key]
        val profile = requireNotNull(currentConfig)
        var value = ProfileSettingsRepository.valueFor(getProfileListType(), profile, key, true)
        if (value == null && definition != null) value = definition.defaultValue
        return value ?: ""
    }

    private fun applyOptionValue(key: String, value: String) {
        val definition = PrintConfigDef.getInstance().options[key]
        if (definition != null) {
            updateConfigField(definition, -1, value)
        } else {
            val profile = requireNotNull(currentConfig)
            ProfileSettingsRepository.setPendingValue(
                getProfileListType(), key, profile.get(key), value,
            )
        }
        onUpdateConfigItems()
    }

    private fun showFlushVolumesDialog(ctx: Context) {
        val matrix = PrintConfigToolsLogic.flushMatrix(
            serialized = optionValue("flush_volumes_matrix"),
            slotCount = Prefs.getFilamentSlots().size,
            maxSlots = Prefs.MAX_FILAMENT_COLORS,
        )
        val cells = mutableStateListOf<String>().apply { addAll(matrix.cells) }
        val error = mutableStateOf(false)
        val composeView = ComposeView(ctx).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
        val dialog = BeamAlertDialogBuilder(ctx)
            .setTitle(R.string.FlushVolumes)
            .setView(composeView)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        composeView.setContent {
            BeamComposeTheme {
                FlushVolumesEditor(
                    matrix = matrix,
                    cells = cells.toList(),
                    error = error.value,
                    onCellChanged = { index, value ->
                        if (index in cells.indices) cells[index] = value
                        error.value = false
                    },
                )
            }
        }
        dialog.setOnShowListener {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            styleDialogButtons(dialog)
        }
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val serialized = PrintConfigToolsLogic.serializeFlushMatrix(matrix.side, cells.toList())
            if (serialized == null) {
                error.value = true
                return@setOnClickListener
            }
            applyOptionValue("flush_volumes_matrix", serialized)
            Toast.makeText(ctx, R.string.PrintConfigFlushVolumesUpdated, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    private fun showPrimeTowerPositionDialog(ctx: Context) {
        val positions = PrintConfigToolsLogic.towerPositions(
            optionValue("wipe_tower_x"),
            optionValue("wipe_tower_y"),
        )
        val xValues = mutableStateListOf<String>().apply { addAll(positions.x) }
        val yValues = mutableStateListOf<String>().apply { addAll(positions.y) }
        val error = mutableStateOf(false)
        val composeView = ComposeView(ctx).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
        val dialog = BeamAlertDialogBuilder(ctx)
            .setTitle(R.string.PrimeTowerPosition)
            .setView(composeView)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        composeView.setContent {
            BeamComposeTheme {
                PrimeTowerPositionEditor(
                    positions = positions,
                    xValues = xValues.toList(),
                    yValues = yValues.toList(),
                    error = error.value,
                    onXChanged = { index, value ->
                        if (index in xValues.indices) xValues[index] = value
                        error.value = false
                    },
                    onYChanged = { index, value ->
                        if (index in yValues.indices) yValues[index] = value
                        error.value = false
                    },
                )
            }
        }
        dialog.setOnShowListener {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            styleDialogButtons(dialog)
        }
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val serialized = PrintConfigToolsLogic.serializeTowerPositions(xValues.toList(), yValues.toList())
            if (serialized == null) {
                error.value = true
                return@setOnClickListener
            }
            applyOptionValue("wipe_tower_x", serialized.first)
            applyOptionValue("wipe_tower_y", serialized.second)
            dialog.dismiss()
        }
    }

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

    companion object {
        private fun styleDialogButtons(dialog: AlertDialog) {
            val accent = ThemesRepo.getColor(android.R.attr.colorAccent)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(accent)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(accent)
        }
    }
}
