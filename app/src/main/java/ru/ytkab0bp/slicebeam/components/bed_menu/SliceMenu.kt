package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.io.File
import java.io.IOException

/** Coordinates the Slice menu, printer preparation and Kotlin upload controller. */
class SliceMenu : ListBedMenu() {
    private var portrait = true
    private val composeActions = mutableStateOf<List<BedComposeMenuAction>>(emptyList())

    override fun onCreateItems(portrait: Boolean): List<BedMenuItem> = emptyList()

    @Composable
    override fun WorkspaceContent(portrait: Boolean) {
        LaunchedEffect(this, portrait) { refreshItems(portrait) }
        CustomizableBedMenuActionsContent("slice", composeActions.value, portrait)
    }

    private fun createActions(): List<BedComposeMenuAction> {
        val bed = fragment ?: return emptyList()
        if (!bed.hasSliceResult()) {
            val hasModel = bed.currentModel?.objectsCount?.let { it > 0 } == true
            return SliceMenuItems.createBeforeSlice(bed, hasModel)
        }
        return SliceMenuItems.createAfterSlice(bed, this) { type, host, apiKey, printer, print ->
            refreshMmuAndPrepare(type, host, apiKey, printer, print)
        }
    }

    fun refreshItems(portrait: Boolean) {
        this.portrait = portrait
        composeActions.value = createActions()
    }

    fun refreshInfoItems() {
        composeActions.value = createActions()
    }

    private fun refreshMmuAndPrepare(
        type: String,
        host: String,
        apiKey: String?,
        printer: ConfigObject,
        print: Boolean,
    ) {
        if (SlicePrintPreparationDialog.isAnycubicKobra(printer)) {
            MmuFilamentSync.synchronize(printer) { _, error ->
                if (error != null) {
                    val context = fragment?.getContext()
                    if (context != null) {
                        fragment?.showWorkspaceMessageDialog(
                            title = context.getString(R.string.MmuSyncFailed),
                            message = context.getString(R.string.MmuAssignmentSyncFailed),
                            confirmLabel = context.getString(android.R.string.ok),
                        )
                    }
                    return@synchronize
                }
                scanToolCountAndPrepare(type, host, apiKey, printer, print)
            }
        } else {
            scanToolCountAndPrepare(type, host, apiKey, printer, print)
        }
    }

    private fun scanToolCountAndPrepare(
        type: String,
        host: String,
        apiKey: String?,
        printer: ConfigObject,
        print: Boolean,
    ) {
        val bed = fragment ?: return
        val gcode: File = BedFragment.tempGCodePath
        IOUtils.IO_POOL.execute {
            val preflight = try {
                SlicePreparationTools.readPreflight(gcode)
            } catch (_: IOException) {
                SliceUploadController.showPreparationFailure(bed)
                return@execute
            }
            val sourceFilamentTypes = try {
                SlicePreparationTools.readFilamentTypes(gcode)
            } catch (_: IOException) {
                SliceUploadController.showPreparationFailure(bed)
                return@execute
            }
            val toolUsage = preflight.toolUsage
            val pressureAdvance = preflight.pressureAdvance
            val activeTools = toolUsage.used
            val toolCount = maxOf(1, (activeTools.maxOrNull() ?: 0) + 1)
            ViewUtils.postOnMainThread {
                if (bed.getContext() != null) {
                    SlicePrintPreparationDialog.show(
                        bed,
                        printer,
                        print,
                        toolCount,
                        activeTools,
                        toolUsage.firstAppearance,
                        sourceFilamentTypes,
                        pressureAdvance,
                        this::activatePrinterProfile,
                        { updatedPrinter ->
                            refreshMmuAndPrepare(
                                updatedPrinter.get("host_type") ?: "octoprint",
                                updatedPrinter.get("print_host").orEmpty(),
                                updatedPrinter.get("printhost_apikey"),
                                updatedPrinter,
                                print,
                            )
                        },
                    ) { sendType, sendHost, sendApiKey, targetPrinter, shouldPrint ->
                        SliceUploadController.upload(
                            bed,
                            sendType,
                            sendHost,
                            sendApiKey,
                            shouldPrint,
                            targetPrinter,
                            activeTools,
                        )
                    }
                }
            }
        }
    }

    private fun activatePrinterProfile(printer: ConfigObject): Boolean {
        val context = fragment?.getContext()
        if (!SliceBeam.applyProfileSelection(printer = printer.title)) {
            if (context != null) {
                android.widget.Toast.makeText(context, R.string.ConfigSaveFailed, android.widget.Toast.LENGTH_LONG).show()
            }
            return false
        }
        fragment?.refreshProfileStatus()
        return true
    }

}
